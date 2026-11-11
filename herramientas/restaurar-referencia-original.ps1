# Restores only the saved original TV configuration, never a different TV Box.
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Dispositivo,[string]$AdbPath,[switch]$Simular)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
. (Join-Path $PSScriptRoot 'tv-recuperacion.ps1')
$saved=Join-Path $script:Z6Root 'privado\referencia\original'
if($Simular){Write-Output 'SIMULACION: verify original TV identity and restore original package/animation states. No ADB executed.';return}
$identityFile=Join-Path $script:Z6Root 'privado\referencia\identidad.json'
if(!(Test-Path -LiteralPath $identityFile)){throw 'Original TV identity was not captured during consolidation. Run diagnosticar on that TV and review identity before enabling this legacy restoration.'}
Connect-Z6 $Dispositivo $AdbPath
$current=Get-Z6DeviceInfo;$expected=Read-Z6Json $identityFile
if($current.id -ne $expected.id){throw 'This restoration belongs exclusively to the original TV Box.'}
if(Test-Z6TvRecoverySupport){Stop-Z6TvRecovery (Join-Path $script:Z6Root 'privado\referencia\recuperacion-tv')}
$home=Read-Z6Json (Join-Path $saved 'estado-inicio-anterior.json')
$packages=Read-Z6Json (Join-Path $saved 'estado-anterior-paquetes.json')
# Enable and validate a recoverable HOME first.
$package=$home.previousHome.Split('/')[0]
Set-Z6PackageState $package ([int]$home.previousEnabledState)
Invoke-Z6Shell @('cmd','package','set-home-activity','--user','0',$home.previousHome)|Out-Null
if((Get-Z6Home) -ne $home.previousHome){throw 'Cannot verify restored HOME'}
foreach($item in $packages){Set-Z6PackageState $item.package ([int]$item.enabled)}
foreach($line in Get-Content -LiteralPath (Join-Path $saved 'animaciones-anteriores.txt')){$parts=$line -split '=',2;Set-Z6Setting 'global' $parts[0] $parts[1]}
$before=(Get-Content -LiteralPath (Join-Path $saved 'accesibilidad-anterior.txt') -Raw).Trim()
$currentServices=@((Get-Z6Setting 'secure' 'enabled_accessibility_services') -split ':'|Where-Object {$_ -and $_ -ne 'null' -and $_ -notmatch '^local\.z6panel/'})
$prior=@($before -split ':'|Where-Object {$_ -and $_ -ne 'null'})
$services=@(($currentServices+$prior)|Select-Object -Unique)
Set-Z6Setting 'secure' 'enabled_accessibility_services' $(if($services.Count){$services -join ':'}else{'null'})
Set-Z6Setting 'secure' 'accessibility_enabled' $(if($services.Count){'1'}else{'0'})
Invoke-Z6Shell @('am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')|Out-Null
Write-Output 'Original TV settings restored. Panel remains installed.'
