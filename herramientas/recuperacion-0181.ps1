[CmdletBinding()]
param([string]$Salida)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$archive=Join-Path $root '.local\recuperacion-0180-c55.zip'
$target=Join-Path $root '.local\recuperacion-0180-c55'
& git -C $root archive --format=zip -o $archive v0.18.0 app
if($LASTEXITCODE){throw 'Historical v0.18.0 tag required'}
Expand-Archive -LiteralPath $archive -DestinationPath $target -Force
$manifest=Join-Path $target 'app\AndroidManifest.xml'
[IO.File]::WriteAllText($manifest,([IO.File]::ReadAllText($manifest).Replace('versionCode="52"','versionCode="55"')),(New-Object Text.UTF8Encoding($false)))
if(!$Salida){$Salida=Join-Path $root '.local\build\Z6-Panel-recuperacion-0.18.0-c55.apk'}
& (Join-Path $PSScriptRoot 'compilar.ps1') -Proyecto (Join-Path $target 'app') -Salida $Salida
