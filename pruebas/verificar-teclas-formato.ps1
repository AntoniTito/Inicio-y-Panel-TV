[CmdletBinding()]
param([string]$Jdk)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if(!$Jdk){$candidate=Get-ChildItem "$env:ProgramFiles\Java\jdk-21*\bin\javac.exe"|Select-Object -Last 1;$Jdk=$candidate.DirectoryName}
$out=Join-Path $root '.local\tests\remote-format';New-Item -ItemType Directory -Force -Path $out|Out-Null
& "$Jdk\javac.exe" -encoding UTF-8 --release 8 -d $out (Join-Path $root 'app\src\local\z6bridge\RemoteKeyFormat.java') (Join-Path $PSScriptRoot 'RemoteKeyFormatTest.java')
if($LASTEXITCODE){throw 'Remote metadata compilation failed'}
& "$Jdk\java.exe" -cp $out RemoteKeyFormatTest
if($LASTEXITCODE){throw 'Remote metadata verification failed'}
