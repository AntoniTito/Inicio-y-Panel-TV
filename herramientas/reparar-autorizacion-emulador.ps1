[CmdletBinding(SupportsShouldProcess=$true)]
param([Parameter(Mandatory=$true)][string]$Dispositivo,[string]$Jdk,[switch]$Simular)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
if($Simular -or $WhatIfPreference){Write-Output 'SIMULACION: verificar emulador API 31 y renovar SOLO la clave local del panel previamente autorizada. Sin ADB.';return}
Connect-Z6 $Dispositivo ''
if((Invoke-Z6Shell @('getprop','ro.kernel.qemu')) -ne '1' -or (Invoke-Z6Shell @('getprop','ro.build.version.sdk')) -ne '31'){throw 'Only Android-12 emulator supported. Physical devices are refused.'}
$dump=Invoke-Z6Shell @('dumpsys','adb')
$keys=@([regex]::Matches($dump,'([A-Za-z0-9+/]{699}=) ([A-Za-z0-9_.@-]+)'))
$local=@($keys|Where-Object {$_.Groups[2].Value -eq 'Z6Panel@localhost'})|Select-Object -First 1
$other=@($keys|Where-Object {$_.Groups[2].Value -ne 'Z6Panel@localhost'}|ForEach-Object {$_.Groups[1].Value}|Select-Object -Unique)
if(!$local -or !$other.Count){throw 'No previously approved local-panel key and independent host key found. Authorize Z6 once in Android before repair.'}
if(($dump -split 'keystore=')[0].Contains('Z6Panel@localhost')){Write-Output 'Panel key is already in the persistent authorized key file. No repair performed.';return}
$md5=[Security.Cryptography.MD5]::Create()
try{$fingerprint=([BitConverter]::ToString($md5.ComputeHash([Convert]::FromBase64String($local.Groups[1].Value)))).Replace('-',':')}finally{$md5.Dispose()}
if(!$Jdk){if($env:JAVA_HOME){$Jdk=Join-Path $env:JAVA_HOME 'bin'}else{$candidate=Get-ChildItem -Path "$env:ProgramFiles\Java\jdk-21*\bin\javac.exe" -ErrorAction SilentlyContinue|Select-Object -Last 1;if($candidate){$Jdk=$candidate.DirectoryName}}}
if(!$Jdk -or !(Test-Path -LiteralPath (Join-Path $Jdk 'javac.exe'))){throw 'JDK 21 is required for this one-time emulator repair'}
$build=Join-Path $script:Z6Root ('.local\adb-repair\'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path (Join-Path $build 'classes')|Out-Null
$android=Join-Path $script:Z6Root '.local\sdk\platforms\android-35\android.jar'
$d8=Join-Path $script:Z6Root '.local\sdk\build-tools\35.0.0\d8.bat'
& (Join-Path $Jdk 'javac.exe') --release 8 -classpath $android -d (Join-Path $build 'classes') (Join-Path $PSScriptRoot 'android\AdbKeyRepair.java')
if($LASTEXITCODE -ne 0){throw 'Repair compile failed'}
& (Join-Path $Jdk 'jar.exe') cf (Join-Path $build 'classes.jar') -C (Join-Path $build 'classes') .
if($LASTEXITCODE -ne 0){throw 'Repair jar failed'}
$previous=$env:JAVA_HOME
try{$env:JAVA_HOME=Split-Path $Jdk -Parent;& $d8 --min-api 31 --lib $android --output (Join-Path $build 'repair.jar') (Join-Path $build 'classes.jar');if($LASTEXITCODE -ne 0){throw 'Repair DEX failed'}}finally{$env:JAVA_HOME=$previous}
Invoke-Z6Adb -Arguments @('-s',$Dispositivo,'push',(Join-Path $build 'repair.jar'),'/data/local/tmp/z6-adb-repair.jar')|Out-Null
Invoke-Z6Shell @('CLASSPATH=/data/local/tmp/z6-adb-repair.jar app_process /system/bin local.z6qa.AdbKeyRepair '+$fingerprint)|Out-Host
Start-Sleep -Milliseconds 700
$after=Invoke-Z6Shell @('dumpsys','adb')
foreach($hostKey in $other){if(!$after.Contains($hostKey)){throw 'Host authorization preservation could not be verified. Do not reboot.'}}
Write-Output ('Renewed only the panel authorization record '+$fingerprint+'. Now stop/recover local control in Diagnostics and authorize this same fingerprint with Always allow. No root was used.')
