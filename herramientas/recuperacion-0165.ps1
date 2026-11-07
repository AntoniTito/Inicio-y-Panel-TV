[CmdletBinding()]
param([string]$Salida)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$archive=Join-Path $projectRoot '.local\recuperacion-0164-c49.zip'
$target=Join-Path $projectRoot '.local\recuperacion-0164-c49'
& git -C $projectRoot archive --format=zip -o $archive v0.16.4 app
if($LASTEXITCODE -ne 0){throw 'Historical v0.16.4 tag required'}
Expand-Archive -LiteralPath $archive -DestinationPath $target -Force
$manifest=Join-Path $target 'app\AndroidManifest.xml'
$text=[IO.File]::ReadAllText($manifest).Replace('versionCode="46"','versionCode="49"')
[IO.File]::WriteAllText($manifest,$text,(New-Object Text.UTF8Encoding($false)))
if(!$Salida){$Salida=Join-Path $projectRoot '.local\build\Z6-Panel-recuperacion-0.16.4-c49.apk'}
& (Join-Path $PSScriptRoot 'compilar.ps1') -Proyecto (Join-Path $target 'app') -Salida $Salida
