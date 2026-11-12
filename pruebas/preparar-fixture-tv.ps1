[CmdletBinding()]
param([string]$Jdk,[ValidateSet('local.z6qa.tvfixture','com.android.z6qa.fixture','com.google.android.z6qa.fixture','android.z6qa.fixture')][string]$Paquete='local.z6qa.tvfixture')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. (Join-Path $root 'herramientas\comun.ps1')
if(!$Jdk -and $env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))){$Jdk=$env:JAVA_HOME}
if(!$Jdk){$Jdk=@(Get-ChildItem -LiteralPath "$env:ProgramFiles\Java" -Directory|Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe')}|Sort-Object Name -Descending)[0].FullName}
if(!(Test-Path -LiteralPath (Join-Path $Jdk 'bin\javac.exe'))){throw 'Provide a JDK directory'}
$folder=if($Paquete -eq 'local.z6qa.tvfixture'){'fixture-tv'}else{'fixture-tv-'+$Paquete}
$base=Join-Path $root ('.local\'+$folder);New-Item -ItemType Directory -Force -Path $base,(Join-Path $base 'classes'),(Join-Path $base 'dex'),(Join-Path $base 'gen')|Out-Null
$bt=Join-Path $root '.local\sdk\build-tools\35.0.0';$android=Join-Path $root '.local\sdk\platforms\android-35\android.jar';$source=Join-Path $PSScriptRoot 'fixture-tv'
if($Paquete -ne 'local.z6qa.tvfixture'){
 $original=$source;$source=Join-Path $base 'source';New-Item -ItemType Directory -Force -Path $source|Out-Null
 Copy-Item -LiteralPath (Join-Path $original 'res') -Destination $source -Recurse -Force
 $javaTarget=Join-Path $source 'src';New-Item -ItemType Directory -Force -Path $javaTarget|Out-Null
 $utf8=New-Object Text.UTF8Encoding($false)
 [IO.File]::WriteAllText((Join-Path $source 'AndroidManifest.xml'),([IO.File]::ReadAllText((Join-Path $original 'AndroidManifest.xml')).Replace('local.z6qa.tvfixture',$Paquete)),$utf8)
 [IO.File]::WriteAllText((Join-Path $javaTarget 'MainActivity.java'),([IO.File]::ReadAllText((Join-Path $original 'src\local\z6qa\tvfixture\MainActivity.java')).Replace('local.z6qa.tvfixture',$Paquete)),$utf8)
}
$oldJava=$env:JAVA_HOME;$env:JAVA_HOME=$Jdk
function Check {if($LASTEXITCODE){throw 'TV fixture build failed'}}
try{
 $key=Join-Path $base 'debug.jks'
 if(!(Test-Path -LiteralPath $key)){$ErrorActionPreference='Continue';& "$Jdk\bin\keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias debug -dname 'CN=Disposable TV navigation fixture' -keyalg RSA -validity 90 2>&1|Out-Null;$code=$LASTEXITCODE;$ErrorActionPreference='Stop';if($code){throw 'Fixture key creation failed'}}
 & "$bt\aapt.exe" package -f -M (Join-Path $source 'AndroidManifest.xml') -S (Join-Path $source 'res') -I $android -J (Join-Path $base 'gen') -F (Join-Path $base 'unsigned.apk');Check
 $files=@(Get-ChildItem (Join-Path $source 'src'),(Join-Path $base 'gen') -Recurse -Filter *.java|ForEach-Object {$_.FullName})
 & "$Jdk\bin\javac.exe" -encoding UTF-8 --release 8 -classpath $android -d (Join-Path $base 'classes') @files;Check
 & "$Jdk\bin\jar.exe" cf (Join-Path $base 'classes.jar') -C (Join-Path $base 'classes') .;Check
 & "$bt\d8.bat" --lib $android --min-api 29 --output (Join-Path $base 'dex') (Join-Path $base 'classes.jar');Check
 Push-Location (Join-Path $base 'dex');try{& "$bt\aapt.exe" add (Join-Path $base 'unsigned.apk') classes.dex;Check}finally{Pop-Location}
 & "$bt\zipalign.exe" -f 4 (Join-Path $base 'unsigned.apk') (Join-Path $base 'aligned.apk');Check
 & "$bt\apksigner.bat" sign --ks $key --ks-pass pass:android --out (Join-Path $base 'fixture.apk') (Join-Path $base 'aligned.apk');Check
 Write-Output "Built .local/$folder/fixture.apk; not installed."
}finally{$env:JAVA_HOME=$oldJava}
