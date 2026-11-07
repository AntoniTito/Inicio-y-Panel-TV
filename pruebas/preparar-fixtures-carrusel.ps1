[CmdletBinding()]
param([string]$Jdk)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$base=Join-Path $root '.local\fixtures-carrusel'
New-Item -ItemType Directory -Force -Path $base|Out-Null
if(!$Jdk){$Jdk=$env:JAVA_HOME}
if(!$Jdk){$Jdk=@(Get-ChildItem -LiteralPath "$env:ProgramFiles\Java" -Directory|Sort-Object Name -Descending)[0].FullName}
if(!(Test-Path -LiteralPath (Join-Path $Jdk 'bin\keytool.exe'))){throw 'Specify -Jdk pointing to a JDK installation'}
$java=$Jdk
$env:JAVA_HOME=$java
$bt=Join-Path $root '.local\sdk\build-tools\35.0.0'
$android=Join-Path $root '.local\sdk\platforms\android-35\android.jar'
$key=Join-Path $base 'debug.jks'
if(!(Test-Path -LiteralPath $key)){$ErrorActionPreference='Continue';& "$java\bin\keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias debug -dname 'CN=Disposable navigation test' -keyalg RSA -validity 30 2>&1|Out-Null;$result=$LASTEXITCODE;$ErrorActionPreference='Stop';if($result){throw 'Test key generation failed'}}
foreach($i in 1..6){
 $dir=Join-Path $base $i;New-Item -ItemType Directory -Force -Path $dir|Out-Null
 $xml='<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="local.z6qa.carousel'+$i+'" android:versionCode="1"><uses-sdk android:minSdkVersion="29" android:targetSdkVersion="31"/><application android:hasCode="false" android:label="Z6 prueba '+$i+'"><activity android:name="android.app.Activity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LEANBACK_LAUNCHER"/></intent-filter></activity></application></manifest>'
 [IO.File]::WriteAllText((Join-Path $dir 'AndroidManifest.xml'),$xml)
 & "$bt\aapt.exe" package -f -M (Join-Path $dir 'AndroidManifest.xml') -I $android -F (Join-Path $dir 'unsigned.apk');if($LASTEXITCODE){throw 'Fixture packaging failed'}
 & "$bt\zipalign.exe" -f 4 (Join-Path $dir 'unsigned.apk') (Join-Path $dir 'aligned.apk')
 & "$bt\apksigner.bat" sign --ks $key --ks-pass pass:android --out (Join-Path $dir 'fixture.apk') (Join-Path $dir 'aligned.apk');if($LASTEXITCODE){throw 'Fixture signing failed'}
}
Write-Output 'Prepared six disposable fixture APKs; not installed.'
