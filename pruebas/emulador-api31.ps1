[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Dispositivo,[switch]$Reiniciar)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. (Join-Path $root 'herramientas\comun.ps1')
Connect-Z6 $Dispositivo ''
if((Invoke-Z6Shell @('getprop','ro.kernel.qemu')) -ne '1' -or (Invoke-Z6Shell @('getprop','ro.build.version.sdk')) -ne '31'){throw 'This integration suite is restricted to an Android-12 emulator.'}
$checks=New-Object 'System.Collections.Generic.List[object]'
function Check([bool]$Condition,[string]$Name,$Evidence){if(!$Condition){throw ('FAILED: '+$Name+'; '+($Evidence|ConvertTo-Json -Depth 5 -Compress))};$checks.Add([pscustomobject]@{name=$Name;passed=$true;evidence=$Evidence});Write-Host ('OK '+$Name)}
function Panel([string]$Method,[string]$Argument){
 $args=@('content','call','--uri','content://local.z6panel.bridge','--method',$Method)
 if($Argument){$args+=@('--arg',$Argument)}
 $result=Invoke-Z6Shell $args
 if($Method -eq 'inspect' -or $Method -eq 'metrics'){$match=[regex]::Match($result,'json=(.*)\}\]$');if(!$match.Success){throw ('Invalid panel JSON: '+$result)};return $match.Groups[1].Value|ConvertFrom-Json}
 if($result -match 'error=' -or $result -match 'status=(DISABLED|NOT_FOUND)'){throw ('Panel operation failed: '+$result)}
 return $result
}
function Act([string]$Id){$null=Panel action $Id;Start-Sleep -Milliseconds 180}
function Key([int]$Value){Invoke-Z6Shell @('input','keyevent',"$Value")|Out-Null;Start-Sleep -Milliseconds 180}
function Open([string]$Page){$null=Panel open $Page;Start-Sleep -Milliseconds 100}
function Volume {$p=Panel inspect '';return [int]([regex]::Match(($p.actions|Where-Object {$_.id -eq 'volume'}).label,'\d+').Value)}
function OverSettings {
 if((Panel inspect '').visible){Open settings;Act close}
 Invoke-Z6Shell @('am','start','-W','-a','android.settings.SETTINGS')|Out-Null
 Start-Sleep -Milliseconds 200
 Key 3
 $p=Panel inspect ''
 Check ($p.visible -and !$p.homeVisible -and $p.page -eq 'bottom') 'Native HOME restores existing task and opens overlay' $p.foreground
}
$before=Get-Z6PanelPreferences
$testStarted=[DateTime]::UtcNow.ToString('o')
try{
 Check ((Panel bridge 'PING') -match 'status=OK') 'Helper is connected' 'PING OK'
 Check ((Invoke-Z6Shell @('wm','size')) -match 'Override size: 1280x720') 'Effective display matches reference' '1280x720'
 OverSettings
 $p=Panel inspect ''
 Check ($p.panelClockVisible -and $p.panelClockTextColor -eq '#B8BFC5') 'Top clock visible with subtle gray text' $p.panelClockText
 # The lower bar starts on Audio; three Right presses reach Ajustes.
 Key 22;Key 22;Key 22;Key 23
 $p=Panel inspect ''
 Check ($p.page -eq 'settings' -and $p.actions.Count -eq 10) 'Native arrows and OK open all eight settings plus footer' $p.actions.Count
 Check (@($p.actions|Where-Object {$_.y -lt 0 -or $_.y+$_.height -gt 720}).Count -eq 0) 'Settings entries fit within 720 pixels' 'all rows inside display'
 $metrics=Panel metrics '';Key 20;$afterFocus=Panel metrics ''
 Check ($afterFocus.focusUpdates -gt $metrics.focusUpdates -and $afterFocus.fullRenders -eq $metrics.fullRenders) 'Focus updates without rebuilding whole panel' @{before=$metrics.fullRenders;after=$afterFocus.fullRenders}
 Key 4
 Check ((Panel inspect '').page -eq 'bottom') 'Native Back returns one level' 'bottom'
 Open appearance;Act overlayClock;Key 4
 Check (!(Panel inspect '').panelClockVisible) 'Clock can be disabled in settings' 'hidden'
 Open appearance;Act overlayClock;Key 4
 Check ((Panel inspect '').panelClockVisible) 'Clock can be enabled again' 'visible'
 Open appearance;Act clock;Key 4
 Check ((Panel inspect '').panelClockText -match '^\d{2}:\d{2}') '24-hour clock format applies to overlay' (Panel inspect '').panelClockText
 Open appearance;Act clock
 Open sound;$originalVolume=Volume
 Key 22;$higher=Volume;Key 21;$restored=Volume
 Check ($higher -ge $originalVolume -and $restored -eq $originalVolume) 'Native volume adjustment and restoration' @{before=$originalVolume;higher=$higher;restored=$restored}
 $muteBefore=((Panel inspect '').actions|Where-Object {$_.id -eq 'mute'}).label
 Act mute;$muteAfter=((Panel inspect '').actions|Where-Object {$_.id -eq 'mute'}).label
 Act mute;$muteRestored=((Panel inspect '').actions|Where-Object {$_.id -eq 'mute'}).label
 Check ($muteAfter -ne $muteBefore -and $muteRestored -eq $muteBefore) 'Mute toggles and original state is restored' $muteRestored
 Open bluetooth;$bt=Panel inspect ''
 Check (@($bt.actions|Where-Object {$_.id -eq 'btNative'}).Count -eq 1) 'Bluetooth provides supported native configuration route' 'btNative'
 Act btNative
 Check ((Invoke-Z6Shell @('dumpsys','activity','activities')) -match 'com.android.tv.settings') 'Bluetooth native destination opens' 'TV settings'
 OverSettings
 Open timer
 foreach($minutes in @(30,60,90)){Act ('timer'+$minutes);$remaining=(Panel inspect '').timerRemaining;Check ($remaining -gt ($minutes*60000-10000) -and $remaining -le $minutes*60000) ('Timer '+$minutes+' minutes replaces previous deadline') $remaining}
 Act timerCancel
 Check ((Panel inspect '').timerRemaining -eq 0) 'Timer cancellation verified' 0
 $null=Panel timer_test '5'
 Start-Sleep -Seconds 7
 $power=Invoke-Z6Shell @('dumpsys','power')
 Check ($power -match 'mWakefulness=Asleep') 'Short timer actually suspends emulator' 'Asleep'
 Key 224
 Check ((Invoke-Z6Shell @('dumpsys','power')) -match 'mWakefulness=Awake') 'Explicit wake restores emulator' 'Awake'
 Check ((Panel inspect '').timerRemaining -eq 0) 'Expired timer is consumed' 0
 if($Reiniciar){
  Open timer;Act timer30
  $boot=Invoke-Z6Shell @('cat','/proc/sys/kernel/random/boot_id')
  Invoke-Z6Adb -Arguments @('-s',$Dispositivo,'reboot')|Out-Null
  $deadline=[DateTime]::UtcNow.AddSeconds(180);$ready=$false
  while([DateTime]::UtcNow -lt $deadline){Start-Sleep -Seconds 2;$state=Invoke-Z6Adb -Arguments @('-s',$Dispositivo,'get-state') -AllowFailure;if($state -ne 'device'){continue};$completed=Invoke-Z6Shell @('getprop','sys.boot_completed') -AllowFailure;if($completed -ne '1'){continue};$ping=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','bridge','--arg','PING') -AllowFailure;if($ping -match 'status=OK'){$ready=$true;break}}
  Check $ready 'Helper recovers after full reboot without PC bootstrap' 'PING OK within 180 seconds'
  Check ((Invoke-Z6Shell @('cat','/proc/sys/kernel/random/boot_id')) -ne $boot) 'Actual reboot identity changed' 'new boot_id'
  Check ((Get-Z6Home) -match '^local.z6panel/') 'Z6 remains default HOME after reboot' 'local.z6panel'
  Check ((Panel inspect '').timerRemaining -eq 0) 'Reboot cancels pending timer' 0
 }
 $null=Panel timer_cancel ''
 Set-Z6PanelPreferences $before
 Write-Z6Json ([ordered]@{version='0.11.0';code=24;api=31;emulator=$true;startedUtc=$testStarted;completedUtc=[DateTime]::UtcNow.ToString('o');checks=@($checks.ToArray());externalAppsInstalled=$false;physicalBoxModified=$false;pending=@('Video continuity with YouTube FOR and Xuper: user will install later','Real JBL Bluetooth output','HDMI-CEC and physical remote')}) (Join-Path $root 'pruebas\resultados-api31.json')
 Write-Output ('Passed '+$checks.Count+' emulator checks.')
}finally{
 Invoke-Z6Shell @('input','keyevent','224') -AllowFailure|Out-Null
}
