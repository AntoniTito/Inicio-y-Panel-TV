# Device-local TV routes never belong in a shared replica profile.
function Test-Z6TvRecoverySupport {
 $info=Invoke-Z6Shell @('dumpsys','package','local.z6panel') -AllowFailure
 $match=[regex]::Match($info,'versionName=(\d+\.\d+\.\d+)')
 # Recovery 0.17.0/c53 has a higher code but does not implement TV automation.
 return $match.Success -and ([version]$match.Groups[1].Value -ge [version]'0.18.0')
}
function Get-Z6TvPreferences {
 $raw=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_export')
 $m=[regex]::Match($raw,'base64=([A-Za-z0-9+/=]+)')
 if(!$m.Success){throw 'TV recovery configuration export failed'}
 $null=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($m.Groups[1].Value))|ConvertFrom-Json
 return $m.Groups[1].Value
}
function Test-Z6RemoteActionsSupport {
 $info=Invoke-Z6Shell @('dumpsys','package','local.z6panel') -AllowFailure
 $m=[regex]::Match($info,'versionName=(\d+\.\d+\.\d+)')
 return $m.Success -and ([version]$m.Groups[1].Value -ge [version]'0.19.0')
}
function Get-Z6RemoteActions {
 $raw=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_buttons_export')
 $m=[regex]::Match($raw,'base64=([A-Za-z0-9+/=]+)')
 if(!$m.Success){throw 'Button configuration export failed'}
 $null=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($m.Groups[1].Value))|ConvertFrom-Json
 return $m.Groups[1].Value
}
function Restore-Z6NativeRemoteLayout([string]$Encoded){
 $config=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($Encoded))|ConvertFrom-Json
 if(!$config.layout -or !$config.layout.PSObject.Properties['current']){return}
 $raw=Invoke-Z6Shell @('pm','path','local.z6panel')
 $m=[regex]::Match($raw,'package:(/[A-Za-z0-9_./=+~\-]+)')
 if(!$m.Success){throw 'Cannot locate installed APK for native-key recovery'}
 $backup=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(($config.layout|ConvertTo-Json -Compress)))
 $result=Invoke-Z6Shell @("CLASSPATH='$($m.Groups[1].Value)' app_process /system/bin local.z6bridge.NativeRemoteLayout restore:$backup")
 if($result -notmatch '"status":"OK"'){throw 'Native keyboard configuration restoration was not confirmed'}
}
function Stop-Z6TvRecovery([string]$Directory){
 New-Item -ItemType Directory -Force -Path $Directory|Out-Null
 $encoded=Get-Z6TvPreferences
 [IO.File]::WriteAllText((Join-Path $Directory ('tv-antes-restaurar-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffffffZ')+'.base64')),$encoded)
 $buttons=$null
 if(Test-Z6RemoteActionsSupport){$buttons=Get-Z6RemoteActions;[IO.File]::WriteAllText((Join-Path $Directory ('botones-antes-restaurar-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffffffZ')+'.base64')),$buttons)}
 $reply=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_cancel')
 if($reply -notmatch 'status=OK'){throw 'Cannot cancel TV automation; restoration aborted'}
 $inputStop=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_remote_stop')
 if($inputStop -notmatch 'status=OK'){throw 'Cannot verify physical-button listener shutdown; restoration aborted'}
 if($buttons){Restore-Z6NativeRemoteLayout $buttons}
 $services=@((Get-Z6Setting 'secure' 'enabled_accessibility_services') -split ':'|Where-Object {$_ -and $_ -ne 'null' -and $_ -notmatch '^local\.z6panel/(local\.z6panel\.)?TvNavigationService$' -and $_ -ne 'local.z6panel/.TvNavigationService'})
 Set-Z6Setting 'secure' 'enabled_accessibility_services' $(if($services.Count){$services -join ':'}else{'null'})
 Set-Z6Setting 'secure' 'accessibility_enabled' $(if($services.Count){'1'}else{'0'})
}
