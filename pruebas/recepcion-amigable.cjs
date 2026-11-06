// Focused receiver integration. Uses only an explicitly chosen emulator.
const fs=require('fs'),path=require('path'),cp=require('child_process');
const root=path.resolve(__dirname,'..'),serial=process.argv[2];
if(!/^emulator-\d+$/.test(serial||''))throw Error('Supply an emulator serial');
const adb=path.join(root,'.local/sdk/platform-tools/adb.exe'),checks=[];
function cmd(...args){return cp.execFileSync(adb,['-s',serial,...args],{encoding:'utf8',timeout:30000,windowsHide:true}).trim();}
if(cmd('shell','getprop','ro.kernel.qemu')!=='1')throw Error('Emulator required');
const api=Number(cmd('shell','getprop','ro.build.version.sdk'));
function panel(method,arg){const args=['shell','content','call','--uri','content://local.z6panel.bridge','--method',method];if(arg!==undefined)args.push('--arg',arg);const raw=cmd(...args);if(raw.includes('error='))throw Error(raw);if(method.endsWith('inspect'))return JSON.parse(raw.match(/json=(.*)\}\]$/s)[1]);return raw;}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function wait(fn){const end=Date.now()+12000;while(Date.now()<end){const value=fn();if(value)return value;await sleep(200);}throw Error('State timeout');}
function check(value,name){if(!value)throw Error('FAILED: '+name);checks.push({name,passed:true});console.log('OK',name);}
const prefs=panel('prefs_export').match(/base64=([A-Za-z0-9+/=]+)/)[1];
const backup=path.join(root,'privado/actualizaciones/recepcion-0122-'+serial+'-'+Date.now());fs.mkdirSync(backup,{recursive:true});fs.writeFileSync(path.join(backup,'preferencias.base64'),prefs);
let forward=0,originalIds=new Set(),photosKnown=false;
async function start(){panel('wallpaper_action','receive');const state=await wait(()=>{const s=panel('wallpaper_inspect');return s.receiverRunning?s:null;});const remote=new URL(state.receiverUrl);forward=Number(cmd('forward','tcp:0','tcp:'+remote.port));return 'http://127.0.0.1:'+forward+remote.pathname;}
async function closed(url){try{await fetch(url,{signal:AbortSignal.timeout(1200)});return false}catch(e){return true}}
function removeForward(){if(forward){cmd('forward','--remove','tcp:'+forward);forward=0;}}
(async()=>{try{
 cmd('install','-r',path.join(root,'.local/build/Z6-Panel-0.12.2.apk'));
 cmd('shell','am','start','-W','-n','local.z6panel/.HomeActivity');
 await wait(()=>{try{return !!panel('inspect')}catch(e){return false}});
 panel('open','appearance');panel('action','wallpapers');await wait(()=>panel('wallpaper_inspect').visible);
 let state=panel('wallpaper_inspect');originalIds=new Set(state.photos.map(p=>p.id));photosKnown=true;const mode=state.mode;
 const image=fs.readFileSync(path.join(root,'.local/test-images/amber.png'));
 let url=await start();state=panel('wallpaper_inspect');check(state.receivedCount===0&&!state.receivedPreviewAvailable,'New reception hides preview until a photo arrives');
 check((await fetch(url)).status===200,'Local authenticated page opens');
 let response=await fetch(url+'upload?name=primera.png',{method:'POST',body:image});check(response.status===200,'First upload is confirmed after saving');
 state=await wait(()=>{const s=panel('wallpaper_inspect');return s.receivedCount===1?s:null;});check(state.receivedPreviewAvailable&&state.status.includes('Foto recibida')&&state.receiveDoneLabel.includes('(1)'),'TV visibly confirms reception and offers preview and library');
 check(state.mode===mode,'Receiving does not apply a wallpaper');
 cmd('shell','uiautomator','dump','/sdcard/z6-receiver.xml');let tree=cmd('shell','cat','/sdcard/z6-receiver.xml');check(/text="Ver foto recibida"[^>]*focused="true"/.test(tree),'First receipt focuses the direct preview button');
 const actionable=[...tree.matchAll(/<node\b[^>]*text="(Ver foto recibida|Terminar y abrir biblioteca[^\"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g)];check(actionable.length===2&&actionable.every(x=>Number(x[3])>=0&&Number(x[5])<=720),'Both next-step buttons fit the 720p screen');
 response=await fetch(url+'upload?name=invalida.png',{method:'POST',body:Buffer.from('invalid image')});check(response.status===400,'Invalid file reports a failure');state=panel('wallpaper_inspect');check(state.receivedCount===1&&state.status.includes('No se recibió'),'Failed upload does not increment the saved-photo count');
 response=await fetch(url+'upload?name=segunda.png',{method:'POST',body:image});check(response.status===200,'Second upload succeeds in the same session');state=await wait(()=>{const s=panel('wallpaper_inspect');return s.receivedCount===2?s:null;});check(state.receiveDoneLabel.includes('(2)')&&state.status.includes('Fotos recibidas: 2'),'Counter and library button reflect multiple uploads');
 cmd('shell','input','keyevent','23');await wait(()=>panel('wallpaper_inspect').page==='photo');state=panel('wallpaper_inspect');check(!state.receiverRunning&&await closed(url),'Remote OK opens preview and closes the receiver port');check(state.mode===mode,'Opening preview still requires choosing Use this photo');removeForward();
 cmd('shell','input','keyevent','4');await wait(()=>panel('wallpaper_inspect').page==='library');
 url=await start();check(panel('wallpaper_inspect').receivedCount===0,'New session resets its own received count');
 await fetch(url+'upload?name=tercera.png',{method:'POST',body:image});await wait(()=>panel('wallpaper_inspect').receivedCount===1);panel('wallpaper_action','receive_done');await wait(()=>panel('wallpaper_inspect').page==='library');check(!panel('wallpaper_inspect').receiverRunning&&await closed(url),'Finish opens the library and closes the port');check(panel('wallpaper_inspect').photos.length===originalIds.size+3,'All three saved photos are present in the library');removeForward();
 url=await start();await fetch(url+'upload?name=cuarta.png',{method:'POST',body:image});await wait(()=>panel('wallpaper_inspect').receivedCount===1);panel('wallpaper_action','expire');await wait(()=>!panel('wallpaper_inspect').receiverRunning);check(await closed(url),'Expiry closes the listener');panel('wallpaper_action','received_preview');await wait(()=>panel('wallpaper_inspect').page==='photo');check(true,'Saved photo can still be previewed after session expiry');removeForward();
 fs.writeFileSync(path.join(root,`pruebas/resultados-recepcion-0.12.2-api${api}.json`),JSON.stringify({version:'0.12.2',code:30,api,emulator:true,utc:new Date().toISOString(),checks,physicalDeviceContacted:false},null,2)+'\n');
 console.log('Passed',checks.length,'checks');
 }finally{removeForward();if(photosKnown){try{for(const p of panel('wallpaper_inspect').photos)if(!originalIds.has(p.id)){panel('wallpaper_action','remove:'+p.id);await wait(()=>!panel('wallpaper_inspect').photos.some(x=>x.id===p.id));}}catch(e){console.error('Cleanup:',e.message)}}try{panel('wallpaper_action','close');panel('wallpaper_action','close');panel('prefs_restore',prefs);cmd('shell','am','start','-W','-n','local.z6panel/.HomeActivity');}catch(e){console.error('Restore:',e.message)}}
})().catch(e=>{console.error(e);process.exitCode=1});
