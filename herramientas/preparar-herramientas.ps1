[CmdletBinding()]
param([switch]$SoloAdb,[switch]$Simular)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
$manifest=Read-Z6Json (Join-Path $PSScriptRoot 'dependencias.json')
$sdk=Join-Path $script:Z6Root '.local\sdk'
foreach($dep in $manifest.dependencies){
 if($SoloAdb -and $dep.name -ne 'adb'){continue}
 $target=Join-Path $sdk $dep.destination
 $marker=switch($dep.name){'adb'{'adb.exe'}'platform'{'android.jar'}'build-tools'{'aapt2.exe'}}
 if(Test-Path -LiteralPath (Join-Path $target $marker)){Write-Output ('Available: '+$dep.name+' '+$dep.version);continue}
 if($Simular){Write-Output ('SIMULACION: download '+$dep.url+', SHA-256 '+$dep.sha256);continue}
 if($dep.url -notlike 'https://dl.google.com/android/repository/*'){throw 'Dependency source is not an official Android repository URL'}
 $temp=Join-Path $script:Z6Root ('.local\downloads\'+[guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Force -Path $temp|Out-Null
 $zip=Join-Path $temp 'download.zip'
 [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12
 Invoke-WebRequest -Uri $dep.url -OutFile $zip -UseBasicParsing
 if((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash -ne $dep.sha256){throw 'Dependency hash mismatch. Download retained for inspection; not extracted.'}
 $extract=Join-Path $temp 'extract'
 Expand-Archive -LiteralPath $zip -DestinationPath $extract
 $source=Join-Path $extract $dep.inside
 if(!(Test-Path -LiteralPath (Join-Path $source $marker))){throw 'Unexpected dependency archive layout'}
 New-Item -ItemType Directory -Force -Path $target|Out-Null
 foreach($item in Get-ChildItem -LiteralPath $source -Force){Copy-Item -LiteralPath $item.FullName -Destination $target -Recurse -Force}
 if(!(Test-Path -LiteralPath (Join-Path $target $marker))){throw 'Dependency install verification failed'}
 $resolved=[IO.Path]::GetFullPath($temp)
 $allowed=[IO.Path]::GetFullPath((Join-Path $script:Z6Root '.local\downloads'))+[IO.Path]::DirectorySeparatorChar
 if(!$resolved.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe temporary cleanup path'}
 Remove-Item -LiteralPath $resolved -Recurse -Force
 Write-Output ('Installed: '+$dep.name+' '+$dep.version)
}
