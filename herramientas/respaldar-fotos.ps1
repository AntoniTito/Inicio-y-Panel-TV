[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Dispositivo,[string]$AdbPath,[string]$Salida)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'comun.ps1')
Connect-Z6 $Dispositivo $AdbPath
$identity=Get-Z6DeviceInfo
if(!$Salida){$Salida=Join-Path $script:Z6Root ('privado\fotos\'+$identity.id+'\'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ'))}
$Salida=[IO.Path]::GetFullPath($Salida)
New-Item -ItemType Directory -Force -Path $Salida|Out-Null
function Read-PhotoReply([string]$Method,[string]$Argument){
 $arguments=@('content','call','--uri','content://local.z6panel.bridge','--method',$Method)
 if($Argument){$arguments+=@('--arg',$Argument)}
 $raw=Invoke-Z6Shell $arguments
 $match=[regex]::Match($raw,'json=(.*)\}\]$')
 if(!$match.Success -or $raw -match 'error='){throw 'Photo backup unavailable. Requires Z6 0.16.0 or its compatible recovery.'}
 return ($match.Groups[1].Value|ConvertFrom-Json)
}
$manifest=Read-PhotoReply 'wallpaper_backup_manifest' ''
Write-Z6Json $identity (Join-Path $Salida 'dispositivo.json')
foreach($photo in $manifest.images){
 if($photo.id -notmatch '^(daily|[a-f0-9]{32})$' -or $photo.size -lt 1 -or $photo.size -gt 4MB){throw 'Invalid backup entry'}
 $path=Join-Path $Salida ($photo.id+'.jpg')
 if(Test-Path -LiteralPath $path){throw 'Backup already contains this photo; choose a new output directory.'}
 $stream=[IO.File]::Open($path,[IO.FileMode]::CreateNew)
 try{for([long]$offset=0;$offset -lt $photo.size;){$reply=Read-PhotoReply 'wallpaper_backup_chunk' ($photo.id+':'+$offset);$bytes=[Convert]::FromBase64String($reply.base64);if(!$bytes.Length -or $bytes.Length -gt 49152 -or $offset+$bytes.Length -gt $photo.size){throw 'Incomplete photo backup'};$stream.Write($bytes,0,$bytes.Length);$offset+=$bytes.Length}}finally{$stream.Dispose()}
 if((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $photo.sha256){throw 'Photo backup hash mismatch; do not release the migration hold.'}
}
Write-Z6Json $manifest (Join-Path $Salida 'imagenes.json')
Write-Output ('Verified photo backup: '+@($manifest.images).Count+' images; '+$Salida)
