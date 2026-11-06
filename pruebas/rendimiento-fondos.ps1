[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Dispositivo,[string]$Referencia)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot '..\herramientas\comun.ps1')
Connect-Z6 $Dispositivo ''
if($Dispositivo -notmatch '^emulator-\d+$' -or (Invoke-Z6Shell @('getprop','ro.kernel.qemu')) -ne '1'){throw 'Emulator required'}
if(!$Referencia){$Referencia=Join-Path $script:Z6Root '.local\rendimiento-antes-0120.json'}
$baseline=Read-Z6Json $Referencia
function Call([string]$Method,[string]$Value){$a=@('content','call','--uri','content://local.z6panel.bridge','--method',$Method);if($Value){$a+=@('--arg',$Value)};$out=Invoke-Z6Shell $a;if($out -match 'error='){throw $out};return $out}
function Json([string]$Method){$raw=Call $Method '';$m=[regex]::Match($raw,'json=(.*)\}\]$');if(!$m.Success){throw $raw};return $m.Groups[1].Value|ConvertFrom-Json}
function Home-Screen {Call action close|Out-Null;Invoke-Z6Shell @('am','start','-W','-n','local.z6panel/.HomeActivity')|Out-Null}
function Get-Pss { $text=Invoke-Z6Shell @('dumpsys','meminfo','local.z6panel');$m=[regex]::Match($text,'TOTAL PSS:\s*(\d+)');if(!$m.Success){$m=[regex]::Match($text,'(?m)^\s*TOTAL\s+(\d+)')};if(!$m.Success){throw 'No PSS metric'};return [int]$m.Groups[1].Value }
$before=Get-Z6PanelPreferences
$state=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($before))|ConvertFrom-Json
$results=@();$memory=@()
try{
 Call open appearance|Out-Null;Call action wallpapers|Out-Null;$images=(Json wallpaper_inspect).photos;if(!$images.Count){throw 'Import synthetic images before performance test'}
 $choice=$images[0].id
 foreach($mode in @('original','photo','slideshow')){
  $state|Add-Member wallpaper_mode ([pscustomobject]@{type='string';value=$mode}) -Force
  $state|Add-Member wallpaper_photo ([pscustomobject]@{type='string';value=$choice}) -Force
  $encoded=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(($state|ConvertTo-Json -Depth 20 -Compress)))
  Call prefs_restore $encoded|Out-Null;Home-Screen;Start-Sleep -Seconds 2
  Call open settings|Out-Null
  $rounds=@()
  for($round=0;$round -lt 3;$round++){
   $times=@();for($i=0;$i -lt 30;$i++){$k=if($i%2){'19'}else{'20'};$s=[Diagnostics.Stopwatch]::StartNew();$out=& $script:Z6Adb -s $Dispositivo shell content call --uri content://local.z6panel.bridge --method key --arg $k;$s.Stop();if($out -notmatch 'status=OK'){throw 'Key response failed'};$times+=$s.Elapsed.TotalMilliseconds}
   $ordered=@($times|Sort-Object);$p95=$ordered[[int][math]::Ceiling(.95*$ordered.Count)-1];$rounds+=@{p95=$p95;samples=$times};Write-Output ($mode+' round '+($round+1)+' p95 '+[math]::Round($p95,2)+' ms')
  }
  $results+=@{mode=$mode;rounds=$rounds}
 }
 for($cycle=1;$cycle -le 20;$cycle++){Home-Screen;Start-Sleep -Milliseconds 200;Invoke-Z6Shell @('am','start','-a','android.settings.SETTINGS')|Out-Null;if($cycle -in @(5,10,15,20)){$memory+=@{cycle=$cycle;pssKb=(Get-Pss)}}}
 $originalP95=@($baseline.rounds.p95|Sort-Object)[1];$limit=$originalP95+[math]::Max($originalP95*.1,8)
 foreach($entry in $results){$median=@($entry.rounds.p95|Sort-Object)[1];$entry.medianP95=$median;$entry.limit=$limit;$entry.passed=$median -le $limit}
 $growth=$memory[-1].pssKb-$memory[1].pssKb
 $report=[ordered]@{version='0.12.0';api=(Invoke-Z6Shell @('getprop','ro.build.version.sdk'));utc=[DateTime]::UtcNow.ToString('o');method=$baseline.method;baselineMedianP95=$originalP95;results=$results;memory=$memory;memoryGrowthAfterWarmupKb=$growth;memoryPassed=($growth -le 8192);note='ADB transport response, not isolated input-to-frame latency. No concurrent emulators during measurement.'}
 Write-Z6Json $report (Join-Path $script:Z6Root 'pruebas\resultados-rendimiento-0.12.0.json')
 if(@($results|Where-Object {!$_.passed}).Count -or !$report.memoryPassed){throw 'Performance acceptance requires review; see report'}
}finally{Set-Z6PanelPreferences $before;Home-Screen}
