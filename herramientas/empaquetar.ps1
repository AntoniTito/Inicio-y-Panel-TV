[CmdletBinding()]
param([string]$Salida)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
$release=Get-Z6Artifact;$recoveries=@(Get-Z6Artifact -Recovery -Api 29;Get-Z6Artifact -Recovery -Api 31)
$releaseManifest=Read-Z6Json (Join-Path $script:Z6Root 'entregas\manifest.json')
$version=([xml](Get-Content -LiteralPath (Join-Path $script:Z6Root 'app\AndroidManifest.xml') -Raw -Encoding UTF8)).manifest.versionName
if(!$Salida){$Salida=Join-Path $script:Z6Root ('.local\Z6-Panel-'+$version+'-portable.zip')}
$staging=Join-Path $script:Z6Root ('.local\package\'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $staging|Out-Null
foreach($folder in @('app','herramientas','perfiles','pruebas')){Copy-Item -LiteralPath (Join-Path $script:Z6Root $folder) -Destination $staging -Recurse}
$docTarget=Join-Path $staging 'docs';New-Item -ItemType Directory -Force -Path $docTarget|Out-Null
Get-ChildItem -LiteralPath (Join-Path $script:Z6Root 'docs') -File|ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination $docTarget}
foreach($name in @('README.md','AGENTS.md','CLAUDE.md','z6.ps1','.gitignore','.gitattributes')){Copy-Item -LiteralPath (Join-Path $script:Z6Root $name) -Destination $staging}
$releaseTarget=Join-Path $staging 'entregas';New-Item -ItemType Directory -Force -Path $releaseTarget|Out-Null
$names=@([IO.Path]::GetFileName($release.path))+@($recoveries|ForEach-Object {[IO.Path]::GetFileName($_.path)})
foreach($name in $names){Copy-Item -LiteralPath (Join-Path $script:Z6Root ('entregas\'+$name)) -Destination $releaseTarget}
$releaseManifest.artifacts=@($releaseManifest.artifacts|Where-Object {$_.file -in $names})
Write-Z6Json $releaseManifest (Join-Path $releaseTarget 'manifest.json')
New-Item -ItemType Directory -Force -Path (Split-Path ([IO.Path]::GetFullPath($Salida)) -Parent)|Out-Null
Compress-Archive -Path (Join-Path $staging '*') -DestinationPath $Salida -Force
$path=[IO.Path]::GetFullPath($staging);$allowed=[IO.Path]::GetFullPath((Join-Path $script:Z6Root '.local\package'))+[IO.Path]::DirectorySeparatorChar
if(!$path.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe staging cleanup path'}
Remove-Item -LiteralPath $path -Recurse -Force
Get-FileHash -LiteralPath $Salida -Algorithm SHA256
