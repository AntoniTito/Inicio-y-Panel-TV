[CmdletBinding()]
param([string]$Salida)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$archive=Join-Path $root '.local\recuperacion-0170-c53.zip'
$target=Join-Path $root '.local\recuperacion-0170-c53'
& git -C $root archive --format=zip -o $archive v0.17.0 app
if($LASTEXITCODE){throw 'Historical v0.17.0 tag required'}
Expand-Archive -LiteralPath $archive -DestinationPath $target -Force
$manifest=Join-Path $target 'app\AndroidManifest.xml'
[IO.File]::WriteAllText($manifest,([IO.File]::ReadAllText($manifest).Replace('versionCode="50"','versionCode="53"')),(New-Object Text.UTF8Encoding($false)))
if(!$Salida){$Salida=Join-Path $root '.local\build\Z6-Panel-recuperacion-0.17.0-c53.apk'}
& (Join-Path $PSScriptRoot 'compilar.ps1') -Proyecto (Join-Path $target 'app') -Salida $Salida
