[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Dispositivo,[Parameter(Mandatory=$true)][string]$Salida,[string]$AdbPath)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
if($Dispositivo -notmatch '^[A-Za-z0-9_.:\[\]-]+$'){throw 'Invalid ADB serial'}
$adb=Resolve-Z6Adb $AdbPath
$target=[IO.Path]::GetFullPath($Salida)
New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent)|Out-Null
$psi=New-Object Diagnostics.ProcessStartInfo
$psi.FileName=$adb;$psi.Arguments='-s '+$Dispositivo+' exec-out screencap -p';$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true;$psi.RedirectStandardOutput=$true
$process=[Diagnostics.Process]::Start($psi)
$stream=[IO.File]::Create($target)
try{$process.StandardOutput.BaseStream.CopyTo($stream)}finally{$stream.Dispose()}
$process.WaitForExit()
if($process.ExitCode -ne 0){throw 'ADB screen capture failed'}
Write-Output $target
