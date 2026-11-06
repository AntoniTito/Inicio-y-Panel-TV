[CmdletBinding(SupportsShouldProcess=$true)]
param([Parameter(Mandatory=$true)][string]$Avd,[string]$ZonaHoraria='America/Lima',[string]$Sdk,[switch]$Simular)
$ErrorActionPreference='Stop'
if($Avd -notmatch '^[A-Za-z0-9_.-]+$'){throw 'Invalid AVD name'}
if($ZonaHoraria -notmatch '^[A-Za-z_]+(/[A-Za-z_+-]+)+$'){throw 'Use an Olson time-zone name, for example America/Lima'}
if($Simular -or $WhatIfPreference){Write-Output ('SIMULACION: iniciar '+$Avd+' con -timezone '+$ZonaHoraria+'. Sin ejecutar el emulador.');return}
if(!$Sdk){if($env:ANDROID_SDK_ROOT){$Sdk=$env:ANDROID_SDK_ROOT}elseif($env:ANDROID_HOME){$Sdk=$env:ANDROID_HOME}else{$Sdk=Join-Path $env:LOCALAPPDATA 'Android\Sdk'}}
$emulator=Join-Path $Sdk 'emulator\emulator.exe'
if(!(Test-Path -LiteralPath $emulator)){throw 'Android Emulator not found. Supply -Sdk with the Android Studio SDK path.'}
$available=@(& $emulator -list-avds)
if($LASTEXITCODE -ne 0 -or $available -notcontains $Avd){throw 'Requested AVD is not installed'}
if($PSCmdlet.ShouldProcess($Avd,'Start interactive emulator with explicit time zone')){
 # The user interacts with the emulator window; no console helper is required.
 $process=Start-Process -FilePath $emulator -ArgumentList @('-avd',$Avd,'-timezone',$ZonaHoraria,'-no-snapshot-load') -WindowStyle Normal -PassThru
 Write-Output ('Emulator process '+$process.Id+'. Existing AVD data is preserved; no wipe or snapshot reset was requested.')
}
