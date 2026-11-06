Set-StrictMode -Version 2
$script:Z6Root=Split-Path $PSScriptRoot -Parent
function Write-Z6Json($Value,[string]$Path){
 New-Item -ItemType Directory -Force -Path (Split-Path $Path -Parent)|Out-Null
 $Value|ConvertTo-Json -Depth 30|Set-Content -LiteralPath $Path -Encoding UTF8
}
function Read-Z6Json([string]$Path){Get-Content -LiteralPath $Path -Raw -Encoding UTF8|ConvertFrom-Json}
function Resolve-Z6Adb([string]$Path){
 if($Path){if(!(Test-Path -LiteralPath $Path -PathType Leaf)){throw 'AdbPath does not exist'};return (Resolve-Path -LiteralPath $Path).Path}
 $bundled=Join-Path $script:Z6Root '.local\sdk\platform-tools\adb.exe'
 if(Test-Path -LiteralPath $bundled){return $bundled}
 $found=Get-Command adb -ErrorAction SilentlyContinue
 if($found){return $found.Source}
 throw 'ADB not found. Run herramientas\preparar-adb.ps1 or provide -AdbPath.'
}
function Invoke-Z6Adb {
 param([string[]]$Arguments,[switch]$AllowFailure)
 $previous=$ErrorActionPreference
 try{$ErrorActionPreference='Continue';$output=(& $script:Z6Adb @Arguments 2>&1|ForEach-Object {"$_"}) -join "`n";$code=$LASTEXITCODE}finally{$ErrorActionPreference=$previous}
 if($code -ne 0 -and !$AllowFailure){throw "ADB failed ($code): $output"}
 return $output.Trim()
}
function Invoke-Z6Shell {
 param([string[]]$Arguments,[switch]$AllowFailure)
 Invoke-Z6Adb -Arguments (@('-s',$script:Z6Device,'shell')+$Arguments) -AllowFailure:$AllowFailure
}
function Connect-Z6([string]$Device,[string]$AdbPath){
 if(!$Device -or $Device -notmatch '^[A-Za-z0-9_.:\[\]-]+$'){throw 'Provide an explicit ADB serial or IP:port with -Dispositivo.'}
 $script:Z6Adb=Resolve-Z6Adb $AdbPath;$script:Z6Device=$Device
 if($Device.Contains(':')){Invoke-Z6Adb -Arguments @('connect',$Device)|Out-Host}
 if((Invoke-Z6Adb -Arguments @('-s',$Device,'get-state')) -ne 'device'){throw 'Device is offline or unauthorized. Authorize this PC on the TV.'}
}
function Get-Z6Setting([string]$Space,[string]$Name){Invoke-Z6Shell @('settings','get',$Space,$Name)}
function Set-Z6Setting([string]$Space,[string]$Name,[string]$Value){
 if(!$Value -or $Value -eq 'null'){Invoke-Z6Shell @('settings','delete',$Space,$Name)|Out-Null}
 else{Invoke-Z6Shell @('settings','put',$Space,$Name,$Value)|Out-Null}
 if((Get-Z6Setting $Space $Name) -ne $(if(!$Value){'null'}else{$Value})){throw "Setting verification failed: $Space/$Name"}
}
function Get-Z6Home {
 $text=Invoke-Z6Shell @('cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
 $line=@($text -split "`n"|Where-Object {$_ -match '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$'})|Select-Object -Last 1
 if(!$line){throw 'Cannot identify a recoverable HOME activity. No changes made.'}
 return $line.Trim()
}
function Get-Z6DeviceInfo {
 $props=[ordered]@{}
 foreach($key in @('ro.product.model','ro.product.device','ro.build.version.release','ro.build.version.sdk','ro.build.fingerprint','ro.serialno','service.adb.tcp.port')){$props[$key]=Invoke-Z6Shell @('getprop',$key)}
 $androidId=Get-Z6Setting 'secure' 'android_id'
 $identity=$props['ro.build.fingerprint']+'|'+$props['ro.serialno']+'|'+$androidId
 $sha=[Security.Cryptography.SHA256]::Create()
 try{$id=([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($identity)))).Replace('-','').Substring(0,20)}finally{$sha.Dispose()}
 [pscustomobject]@{id=$id;endpoint=$script:Z6Device;properties=$props;androidId=$androidId;size=(Invoke-Z6Shell @('wm','size'));density=(Invoke-Z6Shell @('wm','density'));home=(Get-Z6Home);api=[int]$props['ro.build.version.sdk']}
}
function Get-Z6PackageState([string]$Package){
 $text=Invoke-Z6Shell @('dumpsys','package',$Package) -AllowFailure
 $state=[regex]::Match($text,'(?s)User 0:.*?enabled=(\d+)')
 if(!$state.Success){throw "Cannot read package state: $Package"}
 return [int]$state.Groups[1].Value
}
function Set-Z6PackageState([string]$Package,[int]$State){
 $operation=switch($State){0{'default-state'}1{'enable'}2{'disable'}3{'disable-user'}4{'disable-until-used'}default{throw 'Invalid enabled state'}}
 Invoke-Z6Shell @('pm',$operation,'--user','0',$Package)|Out-Null
 if((Get-Z6PackageState $Package) -ne $State){throw "Package state verification failed: $Package"}
}
function Get-Z6PanelPreferences {
 $text=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','prefs_export') -AllowFailure
 $match=[regex]::Match($text,'base64=([A-Za-z0-9+/=]+)')
 if(!$match.Success){throw 'Panel preference export failed; no usable backup returned.'}
 $json=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($match.Groups[1].Value))
 $null=$json|ConvertFrom-Json
 return $match.Groups[1].Value
}
function Set-Z6PanelPreferences([string]$Encoded){
 if($Encoded -notmatch '^[A-Za-z0-9+/=]+$' -or $Encoded.Length -gt 262144){throw 'Invalid preferences'}
 $result=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','prefs_restore','--arg',$Encoded)
 if($result -notmatch 'status=OK'){throw "Preference restore failed: $result"}
 $expected=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($Encoded))|ConvertFrom-Json
 $actual=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((Get-Z6PanelPreferences)))|ConvertFrom-Json
 foreach($p in $expected.PSObject.Properties){
  $found=$actual.PSObject.Properties[$p.Name]
  if(!$found -or $p.Value.type -ne $found.Value.type){throw "Preference verification failed: $($p.Name)"}
  if($p.Value.type -eq 'set'){
   $left=@($p.Value.value|Sort-Object);$right=@($found.Value.value|Sort-Object)
   if($left.Count -ne $right.Count -or (($left|ConvertTo-Json -Compress) -ne ($right|ConvertTo-Json -Compress))){throw "Preference set verification failed: $($p.Name)"}
  }elseif(($p.Value.value|ConvertTo-Json -Depth 15 -Compress) -ne ($found.Value.value|ConvertTo-Json -Depth 15 -Compress)){throw "Preference verification failed: $($p.Name)"}
 }
}
function Get-Z6Artifact([switch]$Recovery){
 $manifest=Read-Z6Json (Join-Path $script:Z6Root 'entregas\manifest.json')
 $name=if($Recovery -and $manifest.PSObject.Properties['recovery']){$manifest.recovery}elseif(!$Recovery -and $manifest.PSObject.Properties['current']){$manifest.current}elseif($Recovery){'Z6-Panel-recuperacion-0.10.3.apk'}else{'Z6-Panel-0.10.4.apk'}
 $entry=@($manifest.artifacts|Where-Object {$_.file -eq $name})[0]
 $path=Join-Path $script:Z6Root ('entregas\'+$name)
 if(!(Test-Path -LiteralPath $path)){throw "Missing signed APK: $name. Download the private release into entregas."}
 if((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $entry.sha256){throw "APK hash mismatch: $name"}
 [pscustomobject]@{path=$path;code=[int]$entry.code;sha256=$entry.sha256}
}
