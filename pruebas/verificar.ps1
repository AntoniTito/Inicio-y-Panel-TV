[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. (Join-Path $root 'herramientas\comun.ps1')
$checks=New-Object 'System.Collections.Generic.List[object]'
function Assert([bool]$Condition,[string]$Name){if(!$Condition){throw "FAILED: $Name"};$checks.Add([pscustomobject]@{name=$Name;passed=$true});Write-Host ('OK '+$Name)}
$scripts=@(Get-Item -LiteralPath (Join-Path $root 'z6.ps1'))+@(Get-ChildItem -LiteralPath (Join-Path $root 'herramientas'),(Join-Path $root 'pruebas') -Filter *.ps1 -Recurse)
foreach($file in $scripts){$errors=$null;[Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$null,[ref]$errors)|Out-Null;Assert (!$errors) ('Parse '+$file.Name)}
$release=Get-Z6Artifact;$recovery=Get-Z6Artifact -Recovery
Assert ($release.code -eq 56 -and $recovery.code -eq 57) 'Release and recovery hashes and codes'
$profile=Read-Z6Json (Join-Path $root 'perfiles\replica.json')
Assert ([bool]$profile.panel.overlay_clock.value) 'Reference includes overlay clock'
Assert ($profile.panel.PSObject.Properties.Name -notcontains 'catalog_snapshot' -and $profile.panel.PSObject.Properties.Name -notcontains 'welcome_boot' -and $profile.panel.PSObject.Properties.Name -notcontains 'local_adb_enabled') 'Profile excludes device and lifecycle state'
$test=Join-Path $root ('.local\tests\'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $test|Out-Null
$statePath=Join-Path $test 'fake.json'
$fixture=[ordered]@{code=0;home='vendor.launcher/.Home';helper=$false;packages=[ordered]@{'vendor.launcher'=0};permissions=[ordered]@{};props=[ordered]@{'ro.product.model'='TEST';'ro.product.device'='test';'ro.build.version.release'='10';'ro.build.version.sdk'='29';'ro.build.fingerprint'='test/build';'ro.serialno'='TEST';'service.adb.tcp.port'='5555'};settings=[ordered]@{secure=[ordered]@{android_id='TEST-ID';enabled_accessibility_services='other.app/.Service';accessibility_enabled='1'};global=[ordered]@{window_animation_scale='1';transition_animation_scale='1';animator_duration_scale='1'}};preferences=[ordered]@{display_name=[ordered]@{type='string';value='DESTINATION'}}}
Write-Z6Json $fixture $statePath
$oldFake=$env:Z6_FAKE_STATE;$env:Z6_FAKE_STATE=$statePath
$driver=Join-Path $root 'z6.ps1';$mock=Join-Path $PSScriptRoot 'adb-falso.ps1';$backups=Join-Path $test 'backups'
try{
 $dry=(& $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups -Simular)|ConvertFrom-Json
 Assert (!$dry.adbExecuted -and !(Test-Path -LiteralPath ($statePath+'.calls'))) 'Dry run executes no ADB'
 $whatIf=(& $driver restaurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups -WhatIf)|ConvertFrom-Json
 Assert (!$whatIf.adbExecuted -and !(Test-Path -LiteralPath ($statePath+'.calls'))) 'WhatIf executes no ADB'
 & $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups
 $state=Read-Z6Json $statePath
 Assert ($state.code -eq 56 -and $state.home -eq 'local.z6panel/.HomeActivity') 'Install version and select HOME'
 Assert ($state.packages.'vendor.launcher' -eq 3) 'Disable old HOME only after selection'
 Assert ($state.preferences.display_name.value -eq 'DESTINATION' -and $state.preferences.overlay_clock.value) 'Keep destination personalization and apply clock'
 Assert ($state.usageMode -eq 'allow') 'Initial usage access enables verified native restart'
 $original=@(Get-ChildItem -LiteralPath $backups -Recurse -Filter original.json)[0].FullName
 $hash=(Get-FileHash -LiteralPath $original).Hash
 $state=Read-Z6Json $statePath
 $state.settings.secure.enabled_accessibility_services+=':local.z6panel/local.z6panel.TvNavigationService'
 $state|Add-Member -NotePropertyName tvRecovery -NotePropertyValue ([pscustomobject]@{schema=1;boot=$true;wake=$true;button=$true;channel='TV de prueba'}) -Force
 $state|Add-Member -NotePropertyName remoteActions -NotePropertyValue ([pscustomobject]@{schema=1;actions=@([pscustomobject]@{id='test-shortcut';kind='reopen'});layout=[pscustomobject]@{current='local.z6panel/local.z6panel.RemoteLayoutReceiver/z6remote';previous=$null}}) -Force
 Write-Z6Json $state $statePath
 & $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups
 Assert ((Get-FileHash -LiteralPath $original).Hash -eq $hash) 'Repeated installation preserves first backup'
 Assert ((Read-Z6Json $statePath).settings.secure.enabled_accessibility_services -match 'TvNavigationService') 'Reconfiguration preserves separately authorized TV assistance'
 $state=Read-Z6Json $statePath;$state.settings.secure.enabled_accessibility_services+=':new.app/.Service';Write-Z6Json $state $statePath
 & $driver restaurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups
 $state=Read-Z6Json $statePath
 Assert ($state.home -eq 'vendor.launcher/.Home' -and $state.packages.'vendor.launcher' -eq 0) 'Restore original HOME and enabled state'
 Assert ($state.settings.global.window_animation_scale -eq '1' -and $state.settings.global.animator_duration_scale -eq '1') 'Restore original animation values'
 Assert ($state.settings.secure.enabled_accessibility_services -match 'new.app' -and $state.settings.secure.enabled_accessibility_services -notmatch 'z6panel') 'Keep unrelated accessibility services during rollback'
 Assert (!$state.helper) 'Stop newly introduced helper during restoration'
 Assert $state.tvCancelled 'Rollback cancels TV automation before restoring HOME'
 Assert $state.tvInputStopped 'Rollback synchronously stops the physical-button listener'
 Assert $state.nativeRemoteRestored 'Rollback removes native map before installing an APK without its receiver'
 Assert ($state.usageMode -eq 'default') 'Rollback restores original usage access'
 $buttonBackups=@(Get-ChildItem -LiteralPath $backups -Recurse -Filter 'botones-antes-restaurar-*.base64')
 Assert ($buttonBackups.Count -gt 0) 'Rollback exports multiple buttons privately'
 $tvBackups=@(Get-ChildItem -LiteralPath $backups -Recurse -Filter 'tv-antes-restaurar-*.base64')
 Assert ($tvBackups.Count -gt 0) 'Rollback preserves private TV route outside transferable profiles'
 $tvSaved=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((Get-Content -LiteralPath $tvBackups[0].FullName -Raw)))|ConvertFrom-Json
 Assert ($tvSaved.boot -and $tvSaved.channel -eq 'TV de prueba') 'TV backup contains original switches and channel'
 $state=Read-Z6Json $statePath;$state.preferences|Add-Member -NotePropertyName wallpaper_slideshow_use_all -NotePropertyValue ([pscustomobject]@{type='boolean';value=$false}) -Force;$state.preferences|Add-Member -NotePropertyName wallpaper_slideshow_ids -NotePropertyValue ([pscustomobject]@{type='set';value=@('PHOTO-A','PHOTO-B')}) -Force;$state.preferences|Add-Member -NotePropertyName wallpaper_mode -NotePropertyValue ([pscustomobject]@{type='string';value='slideshow'}) -Force;Write-Z6Json $state $statePath
 & $driver restaurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups -RecuperarVersion
 Assert ((Read-Z6Json $statePath).code -eq 57) 'Signed recovery selection is supported'
 $legacy=Read-Z6Json $statePath;$legacy.code=53;Write-Z6Json $legacy $statePath
 $oldCalls=(Get-Content -LiteralPath ($statePath+'.calls')).Count
 & $driver restaurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups
 $newCalls=@(Get-Content -LiteralPath ($statePath+'.calls')|Select-Object -Skip $oldCalls)
 Assert (-not ($newCalls -match 'tv_export|tv_cancel|tv_remote_stop')) 'Higher-code historical recovery is not mistaken for TV capability'
 $partial=Read-Z6Json $statePath;Assert (!$partial.preferences.wallpaper_slideshow_use_all.value -and @($partial.preferences.wallpaper_slideshow_ids.value).Count -eq 2 -and $partial.preferences.wallpaper_mode.value -eq 'slideshow') 'API29 recovery preserves subset IDs and keeps partial presentation on compatible recovery'
 $foreign=Read-Z6Json $original;$foreign.device.id='DIFFERENT';$foreignPath=Join-Path $test 'foreign.json';Write-Z6Json $foreign $foreignPath
 $failed=$false;try{& $driver restaurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups -Respaldo $foreignPath}catch{$failed=$_.Exception.Message -match 'different TV Box'}
 Assert $failed 'Reject backup belonging to another TV Box'
 $state=Read-Z6Json $statePath;$state.code=57;Write-Z6Json $state $statePath
 $failed=$false;try{& $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups}catch{$failed=$_.Exception.Message -match 'refusing downgrade'}
 Assert ($failed -and (Read-Z6Json $statePath).code -eq 57) 'Reject newer installed version without uninstalling'
 $state=Read-Z6Json $statePath;$state.code=40;$state.home='vendor.launcher/.Home';$state.packages.'vendor.launcher'=0;$state|Add-Member -NotePropertyName failHomeSet -NotePropertyValue $true;Write-Z6Json $state $statePath
 $failed=$false;try{& $driver configurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups}catch{$failed=$_.Exception.Message -match 'HOME verification failed'}
 Assert ($failed -and (Read-Z6Json $statePath).packages.'vendor.launcher' -eq 0) 'Failed HOME selection never disables previous launcher'
 $state=Read-Z6Json $statePath;$state.failHomeSet=$false;Write-Z6Json $state $statePath
 $state=Read-Z6Json $statePath;$state.props.'ro.build.version.sdk'='34';$state.code=0;Write-Z6Json $state $statePath
 $failed=$false;try{& $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups}catch{$failed=$_.Exception.Message -match 'API is not validated'}
 Assert ($failed -and (Read-Z6Json $statePath).code -eq 0) 'Unknown Android version blocked before install/configuration'
 $invalidRoot=Join-Path $test 'corrupt';New-Item -ItemType Directory -Force -Path (Join-Path $invalidRoot 'entregas')|Out-Null
 Copy-Item -LiteralPath (Join-Path $root 'entregas\manifest.json') -Destination (Join-Path $invalidRoot 'entregas\manifest.json')
 [IO.File]::WriteAllText((Join-Path $invalidRoot 'entregas\Z6-Panel-0.19.0.apk'),'CORRUPTED')
 $previousRoot=$script:Z6Root;$script:Z6Root=$invalidRoot
 $failed=$false;try{$null=Get-Z6Artifact}catch{$failed=$_.Exception.Message -match 'hash mismatch'}finally{$script:Z6Root=$previousRoot}
 Assert $failed 'Reject a corrupted signed release'
 $state=Read-Z6Json $statePath;$state.props.'ro.build.version.sdk'='31';$state.props.'ro.build.fingerprint'='test/android12';$state.code=0;$state.home='vendor.launcher/.Home';$state.packages.'vendor.launcher'=0;$state.failHomeSet=$false;$state|Add-Member -NotePropertyName priorityHome -NotePropertyValue $true -Force;$state|Add-Member -NotePropertyName alarmMode -NotePropertyValue 'default' -Force;Write-Z6Json $state $statePath
 & $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups
 $state=Read-Z6Json $statePath
 Assert ($state.home -eq 'local.z6panel/.HomeActivity' -and $state.packages.'vendor.launcher' -eq 3) 'Vendor HOME priority handled with replacement foreground proof'
 Assert ($state.permissions.'android.permission.BLUETOOTH_CONNECT' -and $state.permissions.'android.permission.BLUETOOTH_SCAN') 'Android 12 nearby-device permissions granted'
 Assert ($state.alarmMode -eq 'allow') 'Android 12 exact-alarm access configured'
 Assert ($state.usageMode -eq 'allow') 'Android 12 verified-restart access configured'
 & $driver restaurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups
 $state=Read-Z6Json $statePath
 Assert ($state.alarmMode -eq 'default' -and $state.timerCancelled) 'Rollback cancels timer and restores previous exact-alarm mode'
 Assert ($state.usageMode -eq 'default') 'Android 12 rollback restores original usage mode'
 Assert (!$state.permissions.'android.permission.BLUETOOTH_CONNECT' -and !$state.permissions.'android.permission.BLUETOOTH_SCAN') 'Rollback restores previous nearby-device grants'
 $state=Read-Z6Json $statePath;$state.preferences|Add-Member -NotePropertyName wallpaper_slideshow_use_all -NotePropertyValue ([pscustomobject]@{type='boolean';value=$false}) -Force;$state.preferences|Add-Member -NotePropertyName wallpaper_slideshow_ids -NotePropertyValue ([pscustomobject]@{type='set';value=@('PHOTO-A','PHOTO-B')}) -Force;$state.preferences|Add-Member -NotePropertyName wallpaper_mode -NotePropertyValue ([pscustomobject]@{type='string';value='slideshow'}) -Force;Write-Z6Json $state $statePath
 & $driver restaurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups -RecuperarVersion
 $recovered=Read-Z6Json $statePath
 Assert ($recovered.code -eq 57) 'Android 12 signed recovery selection is supported'
 Assert (!$recovered.preferences.wallpaper_slideshow_use_all.value -and @($recovered.preferences.wallpaper_slideshow_ids.value).Count -eq 2 -and $recovered.preferences.wallpaper_mode.value -eq 'slideshow') 'API31 recovery preserves subset IDs and keeps partial presentation on compatible recovery'
 $api31Recovery=Get-Z6Artifact -Recovery -Api 31
 Assert ($api31Recovery.path -match 'recuperacion-0.18.1-c57') 'API 31 selects the adapted recovery'
 $unsupported=$false;try{Get-Z6Artifact -Recovery -Api 34|Out-Null}catch{$unsupported=$_.Exception.Message -match 'No signed recovery'}
 Assert $unsupported 'Unknown API recovery is refused'
 $state=Read-Z6Json $statePath;$state.props.'ro.build.version.sdk'='29';$state.props.'ro.build.fingerprint'='test/media';$state.props.'ro.serialno'='TEST-MEDIA';$state.code=36;$state.home='vendor.launcher/.Home';$state.packages.'vendor.launcher'=0;$state.priorityHome=$false;$state|Add-Member -NotePropertyName homeForeground -NotePropertyValue $false -Force;$state.packages|Add-Member -NotePropertyName 'com.android.gallery3d' -NotePropertyValue 3 -Force;Write-Z6Json $state $statePath
 & $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups
 $state=Read-Z6Json $statePath
 Assert ($state.code -eq 56 -and !$state.preferences.PSObject.Properties['wallpaper_publish_hold'] -and ((Get-Content -LiteralPath ($statePath+'.calls') -Raw) -match 'wallpaper_backup_manifest')) 'Legacy update backs up media before releasing migration hold'
 Assert ($state.packages.'com.android.gallery3d' -eq 1) 'Installed disabled native gallery is enabled reversibly'
 & $driver restaurar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups -RecuperarVersion
 Assert ((Read-Z6Json $statePath).packages.'com.android.gallery3d' -eq 3) 'Restoration recovers original disabled gallery state'
 $state=Read-Z6Json $statePath;$state.code=36;$state|Add-Member -NotePropertyName photoBackupFails -NotePropertyValue $true -Force;Write-Z6Json $state $statePath
 $failed=$false;try{& $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups|Out-Null}catch{$failed=$true}
 $state=Read-Z6Json $statePath
 Assert ($failed -and $state.code -eq 56 -and $state.preferences.wallpaper_publish_hold.value) 'Failed photo backup keeps the migration held without deleting originals'
 $state.photoBackupFails=$false;Write-Z6Json $state $statePath
 & $driver instalar -Dispositivo TEST -AdbPath $mock -EstadoLocal $backups
 Assert (!(Read-Z6Json $statePath).preferences.PSObject.Properties['wallpaper_publish_hold']) 'Repeated installer resumes photo backup and releases migration safely'
 Write-Z6Json ([ordered]@{timestampUtc=[DateTime]::UtcNow.ToString('o');physicalDeviceContacted=$false;checks=@($checks.ToArray())}) (Join-Path $root '.local\verificacion-asistente.json')
 Write-Output ('Passed: '+$checks.Count+' checks. No physical device contacted.')
}finally{$env:Z6_FAKE_STATE=$oldFake}
