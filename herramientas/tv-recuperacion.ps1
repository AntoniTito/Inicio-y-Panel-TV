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
function Stop-Z6TvRecovery([string]$Directory){
 New-Item -ItemType Directory -Force -Path $Directory|Out-Null
 $encoded=Get-Z6TvPreferences
 [IO.File]::WriteAllText((Join-Path $Directory ('tv-antes-restaurar-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffffffZ')+'.base64')),$encoded)
 $reply=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_cancel')
 if($reply -notmatch 'status=OK'){throw 'Cannot cancel TV automation; restoration aborted'}
 $inputStop=Invoke-Z6Shell @('content','call','--uri','content://local.z6panel.bridge','--method','tv_remote_stop')
 if($inputStop -notmatch 'status=OK'){throw 'Cannot verify physical-button listener shutdown; restoration aborted'}
 $services=@((Get-Z6Setting 'secure' 'enabled_accessibility_services') -split ':'|Where-Object {$_ -and $_ -ne 'null' -and $_ -notmatch '^local\.z6panel/(local\.z6panel\.)?TvNavigationService$' -and $_ -ne 'local.z6panel/.TvNavigationService'})
 Set-Z6Setting 'secure' 'enabled_accessibility_services' $(if($services.Count){$services -join ':'}else{'null'})
 Set-Z6Setting 'secure' 'accessibility_enabled' $(if($services.Count){'1'}else{'0'})
}
