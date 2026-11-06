[CmdletBinding()]
param([string]$Jdk,[string]$Keystore,[string]$Salida)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
if(!$Jdk){
 if($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))){$Jdk=Join-Path $env:JAVA_HOME 'bin'}
 else{$candidate=Get-ChildItem -Path "$env:ProgramFiles\Java\jdk-21*\bin\javac.exe" -ErrorAction SilentlyContinue|Select-Object -Last 1;if($candidate){$Jdk=$candidate.DirectoryName}}
}
if(!$Jdk -or !(Test-Path -LiteralPath (Join-Path $Jdk 'javac.exe'))){throw 'Install JDK 21 and set JAVA_HOME, or provide -Jdk (bin directory).'}
if(!$Keystore){$Keystore=Join-Path $script:Z6Root 'privado\firma\original.p12'}
if(!(Test-Path -LiteralPath $Keystore)){throw 'Original signing key required. No replacement key will be generated.'}
$sdk=Join-Path $script:Z6Root '.local\sdk'
$bt=Join-Path $sdk 'build-tools\35.0.0'
$android=Join-Path $sdk 'platforms\android-35\android.jar'
if(!(Test-Path -LiteralPath $android) -or !(Test-Path -LiteralPath (Join-Path $bt 'aapt2.exe'))){throw 'Android SDK missing. See docs\COMPILACION.md.'}
$project=Join-Path $script:Z6Root 'app'
if(!$Salida){$Salida=Join-Path $script:Z6Root '.local\build\Z6-Panel-recompilado.apk'}
$Salida=[IO.Path]::GetFullPath($Salida)
foreach($entry in (Read-Z6Json (Join-Path $script:Z6Root 'entregas\manifest.json')).artifacts){if($Salida -eq [IO.Path]::GetFullPath((Join-Path $script:Z6Root ('entregas\'+$entry.file)))){throw 'Build output must not overwrite a verified release.'}}
$build=Join-Path $script:Z6Root ('.local\build\'+[guid]::NewGuid().ToString('N'))
foreach($dir in @('classes','dex','gen')){New-Item -ItemType Directory -Force -Path (Join-Path $build $dir)|Out-Null}
New-Item -ItemType Directory -Force -Path (Split-Path $Salida -Parent)|Out-Null
function Check-Native {if($LASTEXITCODE -ne 0){throw "Build command failed: $LASTEXITCODE"}}
$oldJava=$env:JAVA_HOME;$oldPassword=$env:Z6_STORE_PASSWORD
try{
 $env:JAVA_HOME=Split-Path $Jdk -Parent
 if(!$env:Z6_STORE_PASSWORD){
  $credential=Join-Path $script:Z6Root 'privado\firma\password.dpapi'
  $secure=if(Test-Path -LiteralPath $credential){(Get-Content -LiteralPath $credential -Raw).Trim()|ConvertTo-SecureString}else{Read-Host 'Signing key password' -AsSecureString}
  $handle=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try{$env:Z6_STORE_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($handle)}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($handle)}
 }
 & "$bt\aapt2.exe" compile --dir "$project\res" -o "$build\resources.zip";Check-Native
 & "$bt\aapt2.exe" link -o "$build\unsigned.apk" --manifest "$project\AndroidManifest.xml" -I $android --java "$build\gen" "$build\resources.zip";Check-Native
 $sources=@(Get-ChildItem "$project\src","$build\gen" -Recurse -Filter *.java|ForEach-Object {$_.FullName})
 & "$Jdk\javac.exe" -encoding UTF-8 --release 8 -classpath $android -d "$build\classes" @sources;Check-Native
 & "$Jdk\jar.exe" cf "$build\classes.jar" -C "$build\classes" .;Check-Native
 & "$bt\d8.bat" --lib $android --min-api 24 --output "$build\dex" "$build\classes.jar";Check-Native
 Push-Location "$build\dex"
 try{& "$bt\aapt.exe" add "$build\unsigned.apk" classes.dex;Check-Native}finally{Pop-Location}
 & "$bt\zipalign.exe" -f 4 "$build\unsigned.apk" "$build\aligned.apk";Check-Native
 & "$bt\apksigner.bat" sign --ks $Keystore --ks-pass env:Z6_STORE_PASSWORD --out $Salida "$build\aligned.apk";Check-Native
 & "$bt\apksigner.bat" verify --print-certs $Salida;Check-Native
 Get-FileHash -LiteralPath $Salida -Algorithm SHA256
}finally{$env:JAVA_HOME=$oldJava;$env:Z6_STORE_PASSWORD=$oldPassword}
