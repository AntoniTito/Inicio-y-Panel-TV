[CmdletBinding(SupportsShouldProcess=$true)]
param([Parameter(Mandatory=$true)][string]$Dispositivo,[string]$AdbPath,[string]$EstadoLocal,[switch]$Restaurar,[switch]$Simular)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
if($Simular -or $WhatIfPreference){Write-Output 'SIMULACION: comprobar galeria nativa y respaldar su estado; sin ejecutar ADB.';return}
Connect-Z6 $Dispositivo $AdbPath
$device=Get-Z6DeviceInfo
if(!$EstadoLocal){$EstadoLocal=Join-Path $script:Z6Root 'privado\dispositivos'}
$snapshot=Join-Path $EstadoLocal ($device.id+'\galeria-original.json')
if($Restaurar){
 if(!(Test-Path -LiteralPath $snapshot)){return}
 $old=Read-Z6Json $snapshot
 if($old.deviceId -ne $device.id -or $old.package -ne 'com.android.gallery3d'){throw 'Gallery backup identity mismatch'}
 if($PSCmdlet.ShouldProcess($Dispositivo,'Restore original native-gallery enabled state')){Set-Z6PackageState $old.package ([int]$old.state)}
 return
}
$package='com.android.gallery3d'
$path=Invoke-Z6Shell @('pm','path',$package) -AllowFailure
if($path -notmatch '^package:'){Write-Output 'Native gallery package not present; use an available viewer or Mis fotos.';return}
$state=Get-Z6PackageState $package
if(!(Test-Path -LiteralPath $snapshot)){Write-Z6Json ([ordered]@{deviceId=$device.id;package=$package;state=$state}) $snapshot}
if($state -in @(2,3,4)){
 if($PSCmdlet.ShouldProcess($Dispositivo,'Enable the installed native image gallery')){Set-Z6PackageState $package 1}
}
Write-Output 'Native gallery is available; verify its remote-control navigation on this firmware.'
