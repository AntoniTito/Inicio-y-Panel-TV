[CmdletBinding(SupportsShouldProcess=$true)]
param([Parameter(Mandatory=$true)][string]$Dispositivo,[string]$AdbPath,[string]$EstadoLocal,[switch]$Restaurar,[switch]$Simular)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
. (Join-Path $PSScriptRoot 'tv-recuperacion.ps1')
if($Simular -or $WhatIfPreference){Write-Output 'SIMULACION: identificar control, respaldar asignaciones y permiso, preparar mapa nativo o restaurarlo. Sin ejecutar ADB.';return}
Connect-Z6 $Dispositivo $AdbPath
$device=Get-Z6DeviceInfo
if($device.api -notin @(29,31)){throw 'Native remote setup is validated only for API 29/31'}
if(!(Test-Z6RemoteActionsSupport)){throw 'Z6 0.19.0 or later is required'}
if(!$EstadoLocal){$EstadoLocal=Join-Path $script:Z6Root 'privado\dispositivos'}
$directory=Join-Path $EstadoLocal $device.id
New-Item -ItemType Directory -Force -Path $directory|Out-Null
$backupPath=Join-Path $directory 'mando-original.json'
$raw=Invoke-Z6Shell @('pm','path','local.z6panel')
$m=[regex]::Match($raw,'package:(/[A-Za-z0-9_./=+~\-]+)')
if(!$m.Success){throw 'Installed APK path unavailable'}
$apk=$m.Groups[1].Value
function Native([string]$Operation){$result=Invoke-Z6Shell @("CLASSPATH='$apk' app_process /system/bin local.z6bridge.NativeRemoteLayout $Operation");try{$value=$result|ConvertFrom-Json}catch{throw 'Invalid native-layout reply'};if($value.status -ne 'OK'){throw ('Remote unavailable or incompatible: '+$value.status)};return $value}
if($Restaurar){
 if(!(Test-Path -LiteralPath $backupPath)){throw 'No original remote backup for this device'}
 $saved=Read-Z6Json $backupPath;if($saved.deviceId -ne $device.id){throw 'Remote backup belongs to another device'}
 $cancel=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('{"op":"cancel"}'))
 $stopped=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_control','--arg',$cancel)
 if($stopped -notmatch 'status=OK'){throw 'Cannot cancel current shortcut before restoring'}
 $stopped=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_remote_stop')
 if($stopped -notmatch 'status=OK'){throw 'Cannot verify listener shutdown'}
 $encoded=Get-Z6RemoteActions
 Restore-Z6NativeRemoteLayout $encoded
 $reply=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_buttons_restore','--arg',$saved.buttons)
 if($reply -notmatch 'status=OK'){throw 'Original shortcuts restoration failed'}
 if($saved.layout.current -eq 'local.z6panel/local.z6panel.RemoteLayoutReceiver/z6remote'){$null=Native 'apply_saved'}
 Invoke-Z6Shell @('cmd','appops','set','local.z6panel','GET_USAGE_STATS',$saved.usageMode)|Out-Null
 Write-Output 'Original button configuration restored.';return
}
if(!(Test-Path -LiteralPath $backupPath)){
 $original=Native 'inspect'
 $op=Invoke-Z6Shell @('cmd','appops','get','local.z6panel','GET_USAGE_STATS') -AllowFailure
 $mode=[regex]::Match($op,'GET_USAGE_STATS:\s*(allow|default|ignore|deny)')
 Write-Z6Json ([ordered]@{deviceId=$device.id;buttons=(Get-Z6RemoteActions);layout=$original;usageMode=$(if($mode.Success){$mode.Groups[1].Value}else{'default'})}) $backupPath
}
$applied=Native 'apply'
$encoded=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(($applied|ConvertTo-Json -Compress)))
$reply=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_native_prepared','--arg',$encoded)
if($reply -notmatch 'status=OK'){Restore-Z6NativeRemoteLayout ([Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes((@{layout=$applied}|ConvertTo-Json -Compress))));throw 'Shortcut registration failed; native map reverted'}
Invoke-Z6Shell @('cmd','appops','set','local.z6panel','GET_USAGE_STATS','allow')|Out-Null
Write-Output 'Netflix and YouTube prepared. Verify each physical button. Other remote models use key learning; no universal map is applied.'
