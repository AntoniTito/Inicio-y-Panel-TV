[CmdletBinding()]
param([string]$Salida)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
$null=Get-Z6Artifact;$null=Get-Z6Artifact -Recovery
if(!$Salida){$Salida=Join-Path $script:Z6Root '.local\Z6-Panel-0.10.4-portable.zip'}
$staging=Join-Path $script:Z6Root ('.local\package\'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $staging|Out-Null
foreach($folder in @('app','herramientas','perfiles','pruebas')){Copy-Item -LiteralPath (Join-Path $script:Z6Root $folder) -Destination $staging -Recurse}
$docTarget=Join-Path $staging 'docs';New-Item -ItemType Directory -Force -Path $docTarget|Out-Null
Get-ChildItem -LiteralPath (Join-Path $script:Z6Root 'docs') -File|ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination $docTarget}
foreach($name in @('README.md','AGENTS.md','CLAUDE.md','z6.ps1','.gitignore')){Copy-Item -LiteralPath (Join-Path $script:Z6Root $name) -Destination $staging}
$releaseTarget=Join-Path $staging 'entregas';New-Item -ItemType Directory -Force -Path $releaseTarget|Out-Null
foreach($name in @('manifest.json','Z6-Panel-0.10.4.apk','Z6-Panel-recuperacion-0.10.3.apk')){Copy-Item -LiteralPath (Join-Path $script:Z6Root ('entregas\'+$name)) -Destination $releaseTarget}
New-Item -ItemType Directory -Force -Path (Split-Path ([IO.Path]::GetFullPath($Salida)) -Parent)|Out-Null
Compress-Archive -Path (Join-Path $staging '*') -DestinationPath $Salida -Force
$path=[IO.Path]::GetFullPath($staging);$allowed=[IO.Path]::GetFullPath((Join-Path $script:Z6Root '.local\package'))+[IO.Path]::DirectorySeparatorChar
if(!$path.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe staging cleanup path'}
Remove-Item -LiteralPath $path -Recurse -Force
Get-FileHash -LiteralPath $Salida -Algorithm SHA256
