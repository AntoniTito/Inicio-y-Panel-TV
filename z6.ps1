[CmdletBinding(SupportsShouldProcess=$true)]
param(
 [Parameter(Position=0)][ValidateSet('instalar','configurar','diagnosticar','exportar','verificar','restaurar','compilar')][string]$Operacion='diagnosticar',
 [string]$Dispositivo,[string]$Perfil='replica',[string]$AdbPath,[string]$Respaldo,[string]$CarpetaApks,
 [string]$Jdk,[string]$Keystore,[string]$Salida,[string]$EstadoLocal,
 [switch]$Simular,[switch]$RecuperarVersion,[switch]$SoloPanel
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'herramientas\comun.ps1')
if($Operacion -eq 'compilar'){
 if($Simular -or $WhatIfPreference){Write-Output 'SIMULACION: compilar y verificar sin reemplazar entregas.';return}
 & (Join-Path $PSScriptRoot 'herramientas\compilar.ps1') -Jdk $Jdk -Keystore $Keystore -Salida $Salida
 return
}
$profilePath=Join-Path $PSScriptRoot ('perfiles\'+$Perfil+'.json')
if($Perfil -notmatch '^[A-Za-z0-9_-]+$' -or !(Test-Path -LiteralPath $profilePath)){throw 'Unknown profile. Use a name from perfiles.'}
$profile=Read-Z6Json $profilePath
if([int]$profile.schema -ne 1){throw 'Unsupported profile schema'}
if($Simular -or $WhatIfPreference){
 [pscustomobject]@{mode='SIMULACION';operation=$Operacion;device=$Dispositivo;profile=$Perfil;adbExecuted=$false;steps=@('Identificar destino y capacidades','Respaldar estado antes de cambiarlo','Verificar APK y compatibilidad','Instalar y configurar sin copiar identidades','Verificar HOME antes de deshabilitar el anterior','Informar funciones y autorizaciones pendientes');supportedApi=@($profile.validatedApi);recoveryRequested=[bool]$RecuperarVersion}|ConvertTo-Json -Depth 5
 return
}
Connect-Z6 $Dispositivo $AdbPath
$device=Get-Z6DeviceInfo
if(!$EstadoLocal){$EstadoLocal=Join-Path $PSScriptRoot 'privado\dispositivos'}
$deviceDir=Join-Path $EstadoLocal $device.id
$snapshotPath=Join-Path $deviceDir 'original.json'
$warnings=New-Object 'System.Collections.Generic.List[string]'
$steps=New-Object 'System.Collections.Generic.List[string]'
function Add-Limitation([string]$Text){$warnings.Add($Text);Write-Warning $Text}
function Get-PanelDump {Invoke-Z6Shell @('dumpsys','package','local.z6panel') -AllowFailure}
function Get-PanelCode {
 $match=[regex]::Match((Get-PanelDump),'versionCode=(\d+)')
 if($match.Success){return [int]$match.Groups[1].Value};return 0
}
function Get-PanelPath {
 $raw=Invoke-Z6Shell @('pm','path','local.z6panel') -AllowFailure
 $line=@($raw -split "`n"|Where-Object {$_ -match '^package:/'})|Select-Object -First 1
 if($line){return $line.Substring(8).Trim()};return $null
}
function Normalize-Component([string]$Value){$parts=$Value.Split('/');if($parts.Length -ne 2){throw 'Invalid component'};if($parts[1].StartsWith('.')){return $parts[0]+'/'+$parts[0]+$parts[1]};return $Value}
function Test-PanelActive {
 $inspect=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','inspect') -AllowFailure
 return $inspect -match 'json=' -and $inspect -notmatch 'error='
}
function Save-Original {
 if(Test-Path -LiteralPath $snapshotPath){$saved=Read-Z6Json $snapshotPath;if($saved.device.id -ne $device.id){throw 'Backup identity mismatch'};return $saved}
 $homePackage=$device.home.Split('/')[0]
 if($device.home -match 'ResolverActivity'){throw 'Choose a default HOME on Android before installing.'}
 $panelPath=Get-PanelPath
 $settings=[ordered]@{secure=[ordered]@{};global=[ordered]@{}}
 foreach($key in @('enabled_accessibility_services','accessibility_enabled')){$settings.secure[$key]=Get-Z6Setting 'secure' $key}
 foreach($key in @('window_animation_scale','transition_animation_scale','animator_duration_scale')){$settings.global[$key]=Get-Z6Setting 'global' $key}
 $permissions=[ordered]@{};$dump=Get-PanelDump
 foreach($permission in @('android.permission.WRITE_SECURE_SETTINGS','android.permission.ACCESS_FINE_LOCATION','android.permission.ACCESS_BACKGROUND_LOCATION','android.permission.BLUETOOTH_CONNECT','android.permission.BLUETOOTH_SCAN')){$permissions[$permission]=[bool]($dump -match ([regex]::Escape($permission)+': granted=true'))}
 $prefs=$null
 if($panelPath){$prefs=Get-Z6PanelPreferences}
 $snapshot=[ordered]@{schema=1;createdUtc=[DateTime]::UtcNow.ToString('o');device=$device;home=$device.home;homePackage=$homePackage;homeEnabled=(Get-Z6PackageState $homePackage);panelPresent=[bool]$panelPath;panelCode=(Get-PanelCode);preferences=$prefs;settings=$settings;permissions=$permissions}
 if($device.api -ge 31){$op=Invoke-Z6Shell @('cmd','appops','get','local.z6panel','SCHEDULE_EXACT_ALARM') -AllowFailure;$mode=[regex]::Match($op,'SCHEDULE_EXACT_ALARM:\s*(allow|default|ignore|deny)');$snapshot.alarmMode=if($mode.Success){$mode.Groups[1].Value}else{'default'}}
 if($panelPath){New-Item -ItemType Directory -Force -Path $deviceDir|Out-Null;Invoke-Z6Adb -Arguments @('-s',$Dispositivo,'pull',$panelPath,(Join-Path $deviceDir 'panel-anterior.apk'))|Out-Null}
 Write-Z6Json $snapshot $snapshotPath
 return Read-Z6Json $snapshotPath
}
function Write-Report([string]$State){
 Write-Z6Json ([ordered]@{schema=1;timeUtc=[DateTime]::UtcNow.ToString('o');operation=$Operacion;status=$State;device=$device;backup=$snapshotPath;steps=@($steps.ToArray());limitations=@($warnings.ToArray());physicalTests='Not performed by installer: remote, audio, sleep and reboot require physical validation.'}) (Join-Path $deviceDir 'ultimo-informe.json')
}
function Install-ProvidedApps {
 if($SoloPanel){$steps.Add('External applications deferred by request');return}
 foreach($app in $profile.apps){
  $existing=Invoke-Z6Shell @('pm','path',$app.package) -AllowFailure
  if($existing -match '^package:'){continue}
  $apk=if($CarpetaApks){Join-Path $CarpetaApks ($app.package+'.apk')}else{$null}
  if(!$apk -or !(Test-Path -LiteralPath $apk)){Add-Limitation ('Application missing: '+$app.name+'. Supply '+$app.package+'.apk or install it using Android.');continue}
  $aapt=Join-Path $PSScriptRoot '.local\sdk\build-tools\35.0.0\aapt.exe'
  if(!(Test-Path -LiteralPath $aapt)){Add-Limitation ('Cannot validate provided APK without build-tools: '+$app.name);continue}
  $badging=(& $aapt dump badging $apk) -join "`n"
  if($LASTEXITCODE -ne 0 -or $badging -notmatch ("package: name='"+[regex]::Escape($app.package)+"'")){throw 'Provided application package mismatch'}
  Invoke-Z6Adb -Arguments @('-s',$Dispositivo,'install','-r',$apk)|Out-Null
  if((Invoke-Z6Shell @('pm','path',$app.package) -AllowFailure) -notmatch '^package:'){throw 'Application install could not be verified'}
 }
}
function Configure-Panel {
 $permissions=@('android.permission.WRITE_SECURE_SETTINGS')
 if($device.api -ge 31){$permissions+=@('android.permission.BLUETOOTH_CONNECT','android.permission.BLUETOOTH_SCAN')}else{$permissions+=@('android.permission.ACCESS_FINE_LOCATION','android.permission.ACCESS_BACKGROUND_LOCATION')}
 foreach($permission in $permissions){
  $null=Invoke-Z6Shell @('pm','grant','local.z6panel',$permission) -AllowFailure
  if((Get-PanelDump) -notmatch ([regex]::Escape($permission)+': granted=true')){Add-Limitation ('Permission unavailable: '+$permission)}
 }
 if($device.api -ge 31){$null=Invoke-Z6Shell @('cmd','appops','set','local.z6panel','SCHEDULE_EXACT_ALARM','allow') -AllowFailure}
 $enabled=Get-Z6Setting 'secure' 'enabled_accessibility_services'
 $services=@($enabled -split ':'|Where-Object {$_ -and $_ -ne 'null' -and $_ -notmatch '^local\.z6panel/'})
 if($services -notcontains 'local.z6panel/local.z6panel.PanelService'){$services+='local.z6panel/local.z6panel.PanelService'}
 Set-Z6Setting 'secure' 'enabled_accessibility_services' (($services|Select-Object -Unique)-join ':')
 Set-Z6Setting 'secure' 'accessibility_enabled' '1'
 $active=$false
 for($n=0;$n -lt 10;$n++){if(Test-PanelActive){$active=$true;break};Start-Sleep -Milliseconds 500}
 if(!$active){Invoke-Z6Shell @('am','start','-a','android.settings.ACCESSIBILITY_SETTINGS')|Out-Null;throw 'Activate Z6 Panel in Accessibility, then repeat the same command. Original HOME retained.'}
 $current=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((Get-Z6PanelPreferences)))|ConvertFrom-Json
 $merged=[ordered]@{};foreach($p in $current.PSObject.Properties){$merged[$p.Name]=$p.Value}
 foreach($p in $profile.panel.PSObject.Properties){$merged[$p.Name]=$p.Value}
 $encoded=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(($merged|ConvertTo-Json -Depth 20 -Compress)))
 Set-Z6PanelPreferences $encoded
 foreach($p in $profile.global.PSObject.Properties){Set-Z6Setting 'global' $p.Name ([string]$p.Value)}
 $steps.Add('Preferences, accessibility and animation values verified')
 # Local authorization is requested only on the Android-10 transport supported by this APK.
 if($device.properties['service.adb.tcp.port'] -eq '5555' -or $Dispositivo -match '^emulator-\d+$'){
  $opened=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','open','--arg','diagnostics')
  if($opened -notmatch 'status=OK'){throw 'Cannot open helper diagnostics'}
  $repair=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','action','--arg','repair')
  Write-Host 'Authorize the local ADB key on Android if prompted.'
  $ready=$false
  if($repair -notmatch 'error='){
   for($n=0;$n -lt 15;$n++){Start-Sleep -Seconds 2;$ping=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','bridge','--arg','PING') -AllowFailure;if($ping -match 'status=OK'){$ready=$true;break}}
  }
  if($ready){$steps.Add('Helper PING OK; autonomous reboot still requires physical validation')}
  else{Add-Limitation 'Local helper authorization pending. Use Equipo > Diagnostico > Recuperar control.'}
 }else{Add-Limitation 'This APK requires authorized local ADB on port 5555 for autonomous helper startup. Native Android settings remain the fallback; do not force a firmware change.'}
 Invoke-Z6Shell @('cmd','package','set-home-activity','--user','0','local.z6panel/.HomeActivity')|Out-Null
 if((Normalize-Component (Get-Z6Home)) -ne 'local.z6panel/local.z6panel.HomeActivity'){
  # Some TV launchers have higher intent priority than user-selected HOME.
  # Prove the replacement is resumed before a reversible, verified priority change.
  Invoke-Z6Shell @('am','start','-W','-n','local.z6panel/.HomeActivity')|Out-Null
  $proof=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','inspect')
  if($proof -notmatch '"homeVisible":true' -or $original.homePackage -eq 'local.z6panel'){throw 'Default HOME verification failed; previous launcher not disabled'}
  try{
   Set-Z6PackageState $original.homePackage 3
   Invoke-Z6Shell @('cmd','package','set-home-activity','--user','0','local.z6panel/.HomeActivity')|Out-Null
   if((Normalize-Component (Get-Z6Home)) -ne 'local.z6panel/local.z6panel.HomeActivity'){throw 'Default HOME verification failed after priority change'}
   $steps.Add('Vendor HOME priority handled after foreground proof')
  }catch{
   Set-Z6PackageState $original.homePackage $original.homeEnabled
   Invoke-Z6Shell @('cmd','package','set-home-activity','--user','0',$original.home)|Out-Null
   throw
  }
 }
 $steps.Add('Z6 HOME verified')
 if($original.homePackage -ne 'local.z6panel'){Set-Z6PackageState $original.homePackage 3;$steps.Add('Previous HOME disabled reversibly')}
 Invoke-Z6Shell @('am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')|Out-Null
}
try{
 switch($Operacion){
  {$_ -in 'diagnosticar','verificar'} {
   $dump=Get-PanelDump;$ping=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','bridge','--arg','PING') -AllowFailure
   [pscustomobject]@{device=$device;panelCode=(Get-PanelCode);accessibility=(Get-Z6Setting 'secure' 'enabled_accessibility_services');securePermission=[bool]($dump -match 'android.permission.WRITE_SECURE_SETTINGS: granted=true');helper=$ping;compatibility=$(if($device.api -in @($profile.validatedApi)){'Profile-supported API; review device-specific validation'}else{'API not validated for this profile'})}|ConvertTo-Json -Depth 8
  }
  'exportar' {
   $prefs=Get-Z6PanelPreferences
   if(!$Salida){$Salida=Join-Path $deviceDir 'preferencias-exportadas.base64'}
   New-Item -ItemType Directory -Force -Path (Split-Path ([IO.Path]::GetFullPath($Salida)) -Parent)|Out-Null
   $prefs|Set-Content -LiteralPath $Salida -Encoding ASCII
   $steps.Add('Private preference snapshot exported');Write-Report 'exported';Write-Output $Salida
  }
  {$_ -in 'instalar','configurar'} {
   if($device.api -notin @($profile.validatedApi)){throw 'Android API is not validated for this profile. No configuration applied. See docs\COMPATIBILIDAD.md.'}
   $original=Save-Original
   if($Operacion -eq 'instalar'){
    $artifact=Get-Z6Artifact
    $code=Get-PanelCode
    if($code -gt $artifact.code){throw 'Installed version is newer; refusing downgrade or uninstall.'}
    if($code -lt $artifact.code){Invoke-Z6Adb -Arguments @('-s',$Dispositivo,'install','-r',$artifact.path)|Out-Null}
    if((Get-PanelCode) -ne $artifact.code){throw 'Panel version verification failed'}
    $steps.Add('Signed panel release installed or already present')
    Install-ProvidedApps
   }elseif(!(Get-PanelPath)){throw 'Install the panel before configuring it'}
   Configure-Panel
   Write-Report $(if($warnings.Count){'configured-with-limitations'}else{'configured-not-physically-validated'})
   Write-Host ('Backup: '+$snapshotPath)
   Write-Host 'Configuration finished. Review ultimo-informe.json and perform the physical checklist.'
  }
  'restaurar' {
   if($Respaldo){$snapshotPath=[IO.Path]::GetFullPath($Respaldo)}
   if(!(Test-Path -LiteralPath $snapshotPath)){throw 'No original snapshot. Provide -Respaldo from this device.'}
   $original=Read-Z6Json $snapshotPath
   if($original.device.id -ne $device.id){throw 'Backup belongs to a different TV Box; restoration refused'}
   if(Get-PanelPath){$cancel=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','timer_cancel');if($cancel -notmatch 'status=OK'){throw 'Cannot verify timer cancellation; restore aborted before changing HOME'}}
   if($RecuperarVersion){$artifact=Get-Z6Artifact -Recovery -Api $device.api;if((Get-PanelCode) -gt $artifact.code){throw 'Recovery APK is older than installed version. A higher signed recovery code is required.'};Invoke-Z6Adb -Arguments @('-s',$Dispositivo,'install','-r',$artifact.path)|Out-Null;if((Get-PanelCode) -ne $artifact.code){throw 'Recovery APK version verification failed'};$steps.Add(('Recovery code '+$artifact.code+' installed; future updates require a higher code'))}
   Set-Z6PackageState $original.homePackage $original.homeEnabled
   Invoke-Z6Shell @('cmd','package','set-home-activity','--user','0',$original.home)|Out-Null
   if((Normalize-Component (Get-Z6Home)) -ne (Normalize-Component $original.home)){throw 'Original HOME restoration failed'}
   if($original.preferences){Set-Z6PanelPreferences $original.preferences}
   $current=@((Get-Z6Setting 'secure' 'enabled_accessibility_services') -split ':'|Where-Object {$_ -and $_ -ne 'null' -and $_ -notmatch '^local\.z6panel/'})
   $prior=@($original.settings.secure.enabled_accessibility_services -split ':'|Where-Object {$_ -and $_ -ne 'null'})
   $services=@(($current+$prior)|Select-Object -Unique)
   Set-Z6Setting 'secure' 'enabled_accessibility_services' $(if($services.Count){$services -join ':'}else{'null'})
   Set-Z6Setting 'secure' 'accessibility_enabled' $(if($services.Count){'1'}else{$original.settings.secure.accessibility_enabled})
   foreach($p in $original.settings.global.PSObject.Properties){Set-Z6Setting 'global' $p.Name ([string]$p.Value)}
   foreach($p in $original.permissions.PSObject.Properties){if(!$p.Value){$null=Invoke-Z6Shell @('pm','revoke','local.z6panel',$p.Name) -AllowFailure;if((Get-PanelDump) -match ([regex]::Escape($p.Name)+': granted=true')){throw ('Permission restoration failed: '+$p.Name)}}}
   if($device.api -ge 31 -and ($original.PSObject.Properties['alarmMode'] -or !$original.panelPresent)){$mode=if($original.PSObject.Properties['alarmMode']){$original.alarmMode}else{'default'};Invoke-Z6Shell @('cmd','appops','set','local.z6panel','SCHEDULE_EXACT_ALARM',$mode)|Out-Null}
   if(!$original.panelPresent){$helperPids=Invoke-Z6Shell @('pidof','z6panel_bridge') -AllowFailure;foreach($helperPid in ($helperPids -split '\s+')){if($helperPid -match '^\d+$'){Invoke-Z6Shell @('kill',$helperPid)|Out-Null}}}
   Invoke-Z6Shell @('am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')|Out-Null
   $steps.Add('Original HOME and modified settings restored; unrelated accessibility services retained; panel data not uninstalled')
   Write-Report 'restored';Write-Output 'Restored. Panel remains installed; no data uninstall was performed.'
  }
 }
}catch{
 $warnings.Add($_.Exception.Message)
 try{Write-Report 'failed'}catch{Write-Warning 'Could not save failure report'}
 throw
}
