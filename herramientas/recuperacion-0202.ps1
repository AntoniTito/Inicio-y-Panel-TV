# Recovery of the immutable 0.20.1 source; no replacement signing identity.
[CmdletBinding()]
param([string]$Salida)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$revision='94a60e2cf277865f8b288dfd0985e05dcdc56401'
$build=Join-Path $root ('.local\recuperacion-0201-c63-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $build|Out-Null
$archive=Join-Path $build 'source.zip'
& git -C $root archive --format=zip -o $archive $revision app
if($LASTEXITCODE){throw 'Historical 0.20.1 source is required; fetch v0.20.1 without moving its tag.'}
Expand-Archive -LiteralPath $archive -DestinationPath $build
$manifest=Join-Path $build 'app\AndroidManifest.xml'
$text=[IO.File]::ReadAllText($manifest)
if($text -notmatch 'versionCode="60"' -or $text -notmatch 'versionName="0.20.1"'){throw 'Unexpected recovery source version'}
$text=$text.Replace('versionCode="60"','versionCode="63"')
[IO.File]::WriteAllText($manifest,$text,(New-Object Text.UTF8Encoding($false)))
if(!$Salida){$Salida=Join-Path $root '.local\build\Inicio-y-Panel-TV-recuperacion-0.20.1-c63.apk'}
& (Join-Path $root 'herramientas\compilar.ps1') -Proyecto (Join-Path $build 'app') -Salida $Salida
