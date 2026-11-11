# Local integration fixture. Never contacts a device.
$ErrorActionPreference='Stop'
$statePath=$env:Z6_FAKE_STATE
if(!$statePath){throw 'Z6_FAKE_STATE required'}
$state=Get-Content -LiteralPath $statePath -Raw|ConvertFrom-Json
$tokens=@($args|ForEach-Object {[string]$_})
Add-Content -LiteralPath ($statePath+'.calls') -Value ($tokens|ConvertTo-Json -Compress)
if($tokens[0] -eq '-s'){$tokens=@($tokens|Select-Object -Skip 2)}
if($tokens[0] -eq 'shell'){$tokens=@($tokens|Select-Object -Skip 1)}
function Save-State {$state|ConvertTo-Json -Depth 30|Set-Content -LiteralPath $statePath -Encoding UTF8}
function Set-Field($Object,[string]$Name,$Value){$Object|Add-Member -NotePropertyName $Name -NotePropertyValue $Value -Force}
function Emit([string]$Value){Write-Output $Value;$global:LASTEXITCODE=0}
$command=$tokens -join ' '
switch -Regex ($command){
 '^connect ' {Emit 'connected';break}
 '^get-state$' {Emit 'device';break}
 '^getprop (.+)$' {Emit ([string]$state.props.($Matches[1]));break}
 '^wm size$' {Emit 'Physical size: 1280x720';break}
 '^wm density$' {Emit 'Physical density: 213';break}
 '^settings get (secure|global) (.+)$' {$space=$Matches[1];$key=$Matches[2];$p=$state.settings.$space.PSObject.Properties[$key];Emit $(if($p){[string]$p.Value}else{'null'});break}
 '^settings put (secure|global) (\S+) (.+)$' {Set-Field $state.settings.($Matches[1]) $Matches[2] $Matches[3];Save-State;Emit '';break}
 '^settings delete (secure|global) (\S+)$' {$state.settings.($Matches[1]).PSObject.Properties.Remove($Matches[2]);Save-State;Emit 'Deleted 1 rows';break}
 '^cmd package resolve-activity ' {Emit ("priority=0`n"+$state.home);break}
 '^cmd package set-home-activity --user 0 (.+)$' {$failed=$state.PSObject.Properties['failHomeSet'] -and $state.failHomeSet;$priority=$state.PSObject.Properties['priorityHome'] -and $state.priorityHome;if(!$failed -and (!$priority -or $state.packages.'vendor.launcher' -eq 3 -or $Matches[1] -eq 'vendor.launcher/.Home')){$state.home=$Matches[1]};Save-State;Emit 'Success';break}
 '^pm path (.+)$' {$pkg=$Matches[1];Emit $(if($pkg -eq 'local.z6panel' -and $state.code -gt 0){'package:/data/app/panel/base.apk'}elseif($pkg -ne 'local.z6panel'){'package:/system/app/existing.apk'}else{''});break}
 '^dumpsys package (.+)$' {
  $pkg=$Matches[1]
  if($pkg -eq 'local.z6panel'){
   if($state.code -eq 0){Emit '';break}
   $lines=@('versionCode='+$state.code+' minSdk=24 targetSdk=35','User 0: installed=true enabled=0')
   $lines+=('versionName='+$(if($state.code -eq 52){'0.18.0'}elseif($state.code -eq 53 -or $state.code -eq 50){'0.17.0'}else{'0.16.0'}))
   foreach($p in $state.permissions.PSObject.Properties){$lines+=($p.Name+': granted='+([string]$p.Value).ToLowerInvariant())}
   Emit ($lines -join "`n")
  }else{$p=$state.packages.PSObject.Properties[$pkg];Emit ('User 0: installed=true enabled='+$(if($p){$p.Value}else{0}))}
  break
 }
 '^pm (default-state|enable|disable|disable-user|disable-until-used) --user 0 (.+)$' {$value=switch($Matches[1]){'default-state'{0}'enable'{1}'disable'{2}'disable-user'{3}'disable-until-used'{4}};Set-Field $state.packages $Matches[2] $value;Save-State;Emit 'Success';break}
 '^pm (grant|revoke) local.z6panel (.+)$' {Set-Field $state.permissions $Matches[2] ($Matches[1] -eq 'grant');Save-State;Emit '';break}
 '^install -r (.+)$' {$state.code=if($Matches[1] -like '*-c53.apk'){53}elseif($Matches[1] -like '*0.18.0*'){52}elseif($Matches[1] -like '*-c51.apk'){51}elseif($Matches[1] -like '*0.17.0*'){50}elseif($Matches[1] -like '*-c49.apk'){49}elseif($Matches[1] -like '*0.16.5*'){48}elseif($Matches[1] -like '*-c47.apk'){47}elseif($Matches[1] -like '*0.16.4*'){46}elseif($Matches[1] -like '*-c45.apk'){45}elseif($Matches[1] -like '*0.16.3*'){44}elseif($Matches[1] -like '*-c43.apk'){43}elseif($Matches[1] -like '*0.16.2*'){42}elseif($Matches[1] -like '*-c41.apk'){41}elseif($Matches[1] -like '*0.16.1*'){40}elseif($Matches[1] -like '*-c39.apk'){39}elseif($Matches[1] -like '*0.16.0*'){38}elseif($Matches[1] -like '*-c37.apk'){37}elseif($Matches[1] -like '*0.15.0*'){36}elseif($Matches[1] -like '*-c35.apk'){35}elseif($Matches[1] -like '*0.14.0*'){34}elseif($Matches[1] -like '*-c33.apk'){33}elseif($Matches[1] -like '*0.13.0*'){32}elseif($Matches[1] -like '*-c31.apk'){31}elseif($Matches[1] -like '*0.12.2*'){30}elseif($Matches[1] -like '*-c29.apk'){29}elseif($Matches[1] -like '*0.12.1*'){28}elseif($Matches[1] -like '*-c27.apk'){27}elseif($Matches[1] -like '*0.12.0*'){26}elseif($Matches[1] -like '*recuperacion-0.10.4*'){25}elseif($Matches[1] -like '*recuperacion*'){23}elseif($Matches[1] -like '*0.11.0*'){24}else{22};Save-State;Emit 'Success';break}
 '^cmd appops set local.z6panel SCHEDULE_EXACT_ALARM (allow|default|deny|ignore)$' {Set-Field $state 'alarmMode' $Matches[1];Save-State;Emit '';break}
 '^cmd appops get local.z6panel SCHEDULE_EXACT_ALARM$' {Emit $(if($state.PSObject.Properties['alarmMode']){'SCHEDULE_EXACT_ALARM: '+$state.alarmMode}else{'No operations.'});break}
 '^pull \S+ (.+)$' {[IO.File]::WriteAllText($Matches[1],'FAKE APK');Emit 'pulled';break}
 '^content call ' {
  $method=$tokens[([Array]::IndexOf($tokens,'--method')+1)]
  $argIndex=[Array]::IndexOf($tokens,'--arg');$argument=if($argIndex -ge 0){$tokens[$argIndex+1]}else{''}
  switch($method){
   'wallpaper_backup_manifest' {if($state.PSObject.Properties['photoBackupFails'] -and $state.photoBackupFails){Emit 'Result: Bundle[{error=Injected photo backup failure}]'}else{Emit 'Result: Bundle[{json={"images":[]}}]'}}
   'prefs_export' {$encoded=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(($state.preferences|ConvertTo-Json -Depth 20 -Compress)));Emit ('Result: Bundle[{base64='+$encoded+'}]')}
   'prefs_restore' {$state.preferences=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($argument))|ConvertFrom-Json;Save-State;Emit 'Result: Bundle[{status=OK}]'}
   'inspect' {Emit $(if($state.code -gt 0 -and $state.settings.secure.enabled_accessibility_services -match 'z6panel'){if($state.PSObject.Properties['homeForeground'] -and $state.homeForeground){'Result: Bundle[{json={"homeVisible":true}}]'}else{'Result: Bundle[{json={}}]'}}else{'Result: Bundle[{error=Panel service is not active}]'})}
   'tv_export' {$value=if($state.PSObject.Properties['tvRecovery']){$state.tvRecovery}else{[pscustomobject]@{schema=1;boot=$false;wake=$false;button=$false}};$encoded=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(($value|ConvertTo-Json -Compress)));Emit ('Result: Bundle[{base64='+$encoded+'}]')}
   'tv_cancel' {Set-Field $state 'tvCancelled' $true;Save-State;Emit 'Result: Bundle[{status=OK}]'}
   'tv_remote_stop' {Set-Field $state 'tvInputStopped' $true;Save-State;Emit 'Result: Bundle[{status=OK}]'}
   'tv_restore' {Set-Field $state 'tvRecovery' ([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($argument))|ConvertFrom-Json);Save-State;Emit 'Result: Bundle[{status=OK}]'}
   'timer_cancel' {Set-Field $state 'timerCancelled' $true;Save-State;Emit 'Result: Bundle[{status=OK}]'}
   'open' {Emit 'Result: Bundle[{status=OK}]'}
   'action' {$state.helper=$true;Set-Field $state.preferences 'local_adb_enabled' ([pscustomobject]@{type='boolean';value=$true});Save-State;Emit 'Result: Bundle[{status=OK}]'}
   'bridge' {Emit $(if($state.helper){'Result: Bundle[{status=OK}]'}else{'Result: Bundle[{status=Unavailable}]'})}
   default {throw "Unhandled provider method: $method"}
  };break
 }
 '^am start ' {if($command -match '-n local.z6panel/.HomeActivity'){Set-Field $state 'homeForeground' $true;Save-State};Emit 'Starting: Intent';break}
 '^pidof z6panel_bridge$' {Emit $(if($state.helper){'4321'}else{''});break}
 '^kill \d+$' {$state.helper=$false;Save-State;Emit '';break}
 default {throw "Unhandled fake ADB command: $command"}
}
