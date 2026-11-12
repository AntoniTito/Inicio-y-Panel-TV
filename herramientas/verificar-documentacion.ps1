[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$documents=@(Get-ChildItem -LiteralPath $root -File -Filter *.md)+@(Get-ChildItem -LiteralPath (Join-Path $root 'docs'),(Join-Path $root 'pruebas') -File -Recurse -Filter *.md)
$links=0
foreach($file in $documents){
 $content=Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
 foreach($match in [regex]::Matches($content,'\]\(([^)]+)\)')){
  $target=$match.Groups[1].Value.Trim('<','>').Split('#')[0]
  if(!$target -or $target -match '^[a-zA-Z][a-zA-Z0-9+.-]*:|^/|^\$'){continue}
  $target=[Uri]::UnescapeDataString($target)
  $resolved=[IO.Path]::GetFullPath((Join-Path $file.DirectoryName $target))
  if(!(Test-Path -LiteralPath $resolved)){throw ('Broken local link: '+$file.Name+' -> '+$target)}
  $links++
 }
}
$scripts=@(Get-Item -LiteralPath (Join-Path $root 'z6.ps1'))+@(Get-ChildItem -LiteralPath (Join-Path $root 'herramientas'),(Join-Path $root 'pruebas') -Recurse -File -Filter *.ps1)
foreach($file in $scripts){$errors=$null;[Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$null,[ref]$errors)|Out-Null;if($errors){throw ('PowerShell syntax error: '+$file.Name)}}
$json=@(Get-ChildItem -LiteralPath (Join-Path $root 'entregas'),(Join-Path $root 'perfiles'),(Join-Path $root 'pruebas') -Recurse -File -Filter *.json|Where-Object {$_.Name -notlike '*-local.json' -or $_.DirectoryName -ne (Join-Path $root 'perfiles')})
foreach($file in $json){$null=Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8|ConvertFrom-Json}
$manifest=[xml](Get-Content -LiteralPath (Join-Path $root 'app\AndroidManifest.xml') -Raw -Encoding UTF8)
if($manifest.manifest.package -ne 'local.z6panel'){throw 'Android package compatibility changed'}
if(!(Test-Path -LiteralPath (Join-Path $root 'LICENSE')) -or (Get-Content -LiteralPath (Join-Path $root 'LICENSE') -Raw) -notmatch 'GNU GENERAL PUBLIC LICENSE'){throw 'GPL license missing'}
Write-Output ('Verified '+$links+' local links, '+$scripts.Count+' PowerShell files, '+$json.Count+' JSON files, Android manifest and license. No ADB executed.')
