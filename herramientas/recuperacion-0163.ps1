[CmdletBinding()]
param([string]$Salida)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$archive=Join-Path $projectRoot '.local\recuperacion-0162-c45.zip'
$target=Join-Path $projectRoot '.local\recuperacion-0162-c45'
& git -C $projectRoot archive --format=zip -o $archive v0.16.2 app
if($LASTEXITCODE -ne 0){throw 'Historical v0.16.2 tag required'}
Expand-Archive -LiteralPath $archive -DestinationPath $target -Force
$manifest=Join-Path $target 'app\AndroidManifest.xml'
$text=[IO.File]::ReadAllText($manifest).Replace('versionCode="42"','versionCode="45"')
[IO.File]::WriteAllText($manifest,$text,(New-Object Text.UTF8Encoding($false)))
if(!$Salida){$Salida=Join-Path $projectRoot '.local\build\Z6-Panel-recuperacion-0.16.2-c45.apk'}
& (Join-Path $PSScriptRoot 'compilar.ps1') -Proyecto (Join-Path $target 'app') -Salida $Salida
