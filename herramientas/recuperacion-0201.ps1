[CmdletBinding()]
param([string]$Salida)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$revision='96c622fd53cbdcef371f373905fce630fc34a211'
$archive=Join-Path $root '.local\recuperacion-0200-c61.zip'
$target=Join-Path $root '.local\recuperacion-0200-c61'
& git -C $root archive --format=zip -o $archive $revision app
if($LASTEXITCODE){throw 'Historical 0.20.0 commit required'}
Expand-Archive -LiteralPath $archive -DestinationPath $target -Force
$manifest=Join-Path $target 'app\AndroidManifest.xml'
$text=[IO.File]::ReadAllText($manifest).Replace('versionCode="58"','versionCode="61"').Replace('android:name=".TvRecoveryActivity" android:exported="false"','android:name=".TvRecoveryActivity" android:exported="false" android:launchMode="singleTask"')
[IO.File]::WriteAllText($manifest,$text,(New-Object Text.UTF8Encoding($false)))
if(!$Salida){$Salida=Join-Path $root '.local\build\Z6-Panel-recuperacion-0.20.0-c61.apk'}
& (Join-Path $PSScriptRoot 'compilar.ps1') -Proyecto (Join-Path $target 'app') -Salida $Salida
