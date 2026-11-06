// Integration against an explicitly selected emulator only. No physical box is contacted.
// Requires Node and .local/browser-test/node_modules/playwright-core for the web UI test.
const fs=require('fs'),path=require('path'),cp=require('child_process'),net=require('net');
const root=path.resolve(__dirname,'..'),serial=process.argv[2];
if(!/^emulator-\d+$/.test(serial||''))throw Error('Supply an emulator serial; physical devices are refused');
const adb=path.join(root,'.local/sdk/platform-tools/adb.exe'),checks=[];
function cmd(...args){return cp.execFileSync(adb,['-s',serial,...args],{encoding:'utf8',timeout:30000,windowsHide:true}).trim();}
if(cmd('shell','getprop','ro.kernel.qemu')!=='1')throw Error('Only emulator allowed');
const api=Number(cmd('shell','getprop','ro.build.version.sdk'));if(![29,31].includes(api))throw Error('Only API 29/31 validated');const photosOnly=process.argv.includes('--photos-only');
function panel(method,arg){const args=['shell','content','call','--uri','content://local.z6panel.bridge','--method',method];if(arg!==undefined)args.push('--arg',arg);const raw=cmd(...args);if(raw.includes('error='))throw Error(raw);if(['home_inspect','inspect','wallpaper_inspect'].includes(method))return JSON.parse(raw.match(/json=(.*)\}\]$/s)[1]);return raw;}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function waitFor(fn,limit=12000){let value;const end=Date.now()+limit;while(Date.now()<end){value=fn();if(value)return value;await sleep(200);}throw Error('Timed out waiting for state');}
function check(condition,name,evidence){if(!condition)throw Error('FAILED: '+name+' '+JSON.stringify(evidence));checks.push({name,passed:true,evidence});console.log('OK',name);}
function setPrefs(obj){panel('prefs_restore',Buffer.from(JSON.stringify(obj)).toString('base64'));}
function home(){try{panel('action','close');}catch(e){}cmd('shell','am','start','-W','-n','local.z6panel/.HomeActivity');}
function key(code){cmd('shell','input','keyevent',String(code));}
async function wallpaper(){if(photosOnly)panel('wallpaper_open');else{panel('open','appearance');panel('action','wallpapers');}await waitFor(()=>panel('wallpaper_inspect').visible);}
const originalRaw=panel('prefs_export'),original=originalRaw.match(/base64=([A-Za-z0-9+/=]+)/)[1],prefs=JSON.parse(Buffer.from(original,'base64').toString());
const local=path.join(root,'.local/test-images');fs.mkdirSync(local,{recursive:true});
const resultsFile=path.join(root,'pruebas',`resultados-fondos-0.12.0-api${api}.json`);
let browser,forward=0;
(async()=>{try{
 home();let h=panel('home_inspect');const apps=h.apps;check(apps.length>0,'Launchable catalogue available',apps.length);
 const empty={...prefs,hidden_apps:{type:'set',value:apps.map(x=>x.package)},shown_apps:{type:'set',value:[]},wallpaper_mode:{type:'string',value:'original'}};
 setPrefs(empty);home();await waitFor(()=>panel('home_inspect').appCount===0);h=panel('home_inspect');check(h.addVisible&&h.focus==='add_application','Empty HOME focuses Add without long press',h.focus);
 if(!photosOnly){key(23);await waitFor(()=>panel('home_inspect').dialog);check(panel('home_inspect').dialog,'Short OK opens available installed apps');key(4);
 // The overlay remains keyboard-operable when every app is hidden.
 cmd('shell','am','start','-W','-a','android.settings.SETTINGS');if(api===29)panel('key','3');else key(3);
 let overlay=await waitFor(()=>{const s=panel('inspect');return s.visible&&s.page==='bottom'?s:null;});
 check(overlay.bottomAddVisible&&overlay.bottomAppCount===0,'Empty bottom overlay contains Add',overlay.bottomAppCount);
 if(api===29){panel('key','20');panel('key','23');}else{key(20);key(23);}overlay=panel('inspect');check(overlay.page==='panelAddApp','Down and short OK reach Add in empty overlay',overlay.page);
 const candidate=overlay.actions.find(x=>x.enabled&&x.label!=='Instalar aplicaciones'&&!['Volver','Cerrar'].includes(x.label));check(!!candidate,'Hidden installed apps offered in selector');panel('action',candidate.id);overlay=panel('inspect');check(overlay.bottomAppCount===1&&!overlay.bottomAddVisible,'Adding first app focuses populated row and removes empty Add',overlay.bottomSelection);
 home();h=panel('home_inspect');check(h.appCount===1&&!h.addVisible,'HOME reflects app selected in overlay');}
 // Import fixtures are synthetic screenshots generated in a private cache, never personal photos.
 await wallpaper();panel('wallpaper_action','receive');let receiver=await waitFor(()=>{const s=panel('wallpaper_inspect');return s.receiverRunning?s:null;});const remote=new URL(receiver.receiverUrl);forward=Number(cmd('forward','tcp:0','tcp:'+remote.port));const url='http://127.0.0.1:'+forward+remote.pathname;
 const pageResponse=await fetch(url);check(pageResponse.status===200,'Authenticated receiver web page served');check((await fetch('http://127.0.0.1:'+forward+'/invalid/')).status===403,'Invalid session rejected');
 const {chromium}=require(path.join(root,'.local/browser-test/node_modules/playwright-core'));
 const executable=process.env.Z6_TEST_CHROMIUM||path.join(process.env.LOCALAPPDATA,'ms-playwright/chromium-1243/chrome-win64/chrome.exe');
 browser=await chromium.launch({executablePath:executable,headless:true});const page=await browser.newPage({viewport:{width:390,height:844}});const errors=[];page.on('pageerror',e=>errors.push(e.message));await page.goto(url);
 // Canvas fixture generation happens only in the test browser.
 for(const [name,color]of [['amber','#c08325'],['teal','#246e72']]){const data=await page.evaluate(color=>{const c=document.createElement('canvas');c.width=2400;c.height=1350;const ctx=c.getContext('2d');ctx.fillStyle=color;ctx.fillRect(0,0,c.width,c.height);ctx.fillStyle='#ddd';ctx.font='80px sans-serif';ctx.fillText('Z6 synthetic test',100,160);return c.toDataURL('image/png').split(',')[1]},color);fs.writeFileSync(path.join(local,name+'.png'),Buffer.from(data,'base64'));}
 await page.locator('#files').setInputFiles([path.join(local,'amber.png'),path.join(local,'teal.png')]);await page.getByRole('button',{name:'Enviar fotos'}).click();await page.waitForFunction(()=>document.querySelector('#status').textContent.startsWith('Proceso terminado'),{},{timeout:30000});const messages=await page.locator('#results li').allTextContents();check(messages.length===2&&messages.every(x=>x.endsWith('recibida'))&&errors.length===0,'PC browser uploads multiple images with per-file results',messages.map(x=>x.split(' · ').pop()));
 const before=panel('wallpaper_inspect').photos.length;let bad=await fetch(url+'upload?name=invalid.png',{method:'POST',body:Buffer.from('not an image')});check(bad.status===400,'Invalid image rejected');check(panel('wallpaper_inspect').photos.length===before,'Rejected upload leaves library unchanged');
 bad=await fetch(url+'upload?name=foreign.png',{method:'POST',body:Buffer.from('invalid'),headers:{Origin:'http://foreign.invalid'}});check(bad.status===403,'Cross-origin upload rejected');
 const oversized=await new Promise((resolve,reject)=>{const s=net.connect(forward,'127.0.0.1',()=>s.write('POST '+remote.pathname+'upload?name=large.jpg HTTP/1.1\r\nHost: 127.0.0.1:'+forward+'\r\nContent-Length: 20971521\r\n\r\n'));let all='';s.setTimeout(10000,()=>s.destroy(Error('Socket timeout')));s.on('data',b=>all+=b.toString());s.on('end',()=>resolve(all));s.on('error',reject);});check(oversized.startsWith('HTTP/1.1 413'),'Oversized file rejected before reading its body');
 await new Promise(resolve=>{const s=net.connect(forward,'127.0.0.1',()=>{s.write('POST '+remote.pathname+'upload?name=interrupted.jpg HTTP/1.1\r\nHost: 127.0.0.1:'+forward+'\r\nContent-Length: 1000\r\n\r\npartial');setTimeout(()=>{s.destroy();resolve()},100)});});await sleep(300);check(panel('wallpaper_inspect').incomingTemporaryFiles===0,'Interrupted upload removes temporary file');
 panel('wallpaper_action','expire');await sleep(200);let closed=false;try{await fetch(url,{signal:AbortSignal.timeout(1500)})}catch(e){closed=true}check(closed&&!panel('wallpaper_inspect').receiverRunning,'Expiry callback closes port and session');
 panel('wallpaper_action','close');let library=panel('wallpaper_inspect').photos;const selected=library.find(p=>p.name==='amber.png');panel('wallpaper_action','select:'+selected.id);key(4);home();h=await waitFor(()=>{const v=panel('home_inspect');return v.wallpaper.loaded?v:null;});check(h.wallpaper.width<=1280&&h.wallpaper.height<=720,'Photo is decoded within light/display bounds',{width:h.wallpaper.width,height:h.wallpaper.height});
 await wallpaper();panel('wallpaper_action','mode:slideshow');await sleep(200);key(4);home();h=await waitFor(()=>{const v=panel('home_inspect');return v.wallpaper.loaded?v:null;});const first=h.wallpaper.key;panel('wallpaper_next');await waitFor(()=>panel('home_inspect').wallpaper.key!==first);check(panel('home_inspect').wallpaper.scheduled,'Slideshow advances and schedules only on visible HOME');
 cmd('shell','am','start','-W','-a','android.settings.SETTINGS');h=panel('home_inspect');check(!h.wallpaper.active&&!h.wallpaper.loaded&&!h.wallpaper.scheduled&&h.wallpaper.bytes===0,'Leaving HOME stops rotation and releases bitmap',h.wallpaper);
 await wallpaper();panel('wallpaper_action','quality');panel('wallpaper_action','select:'+selected.id);key(4);home();h=await waitFor(()=>{const v=panel('home_inspect');return v.wallpaper.loaded?v:null;});check(h.wallpaper.width<=1920&&h.wallpaper.width<=1280,'Balanced quality never exceeds effective display',h.wallpaper.width);
 await wallpaper();panel('wallpaper_action','albums');await sleep(200);check(panel('wallpaper_inspect').page==='albums','Missing document picker has native TV album alternative');key(4);
 panel('wallpaper_action','receive');receiver=await waitFor(()=>{const s=panel('wallpaper_inspect');return s.receiverRunning?s:null;});const tempPort=Number(cmd('forward','tcp:0','tcp:'+new URL(receiver.receiverUrl).port));key(4);let stopped=false;try{await fetch('http://127.0.0.1:'+tempPort+new URL(receiver.receiverUrl).pathname,{signal:AbortSignal.timeout(1500)})}catch(e){stopped=true}cmd('forward','--remove','tcp:'+tempPort);check(stopped,'Back closes receiver immediately');
 const record={version:'0.12.0',code:26,api,emulator:true,utc:new Date().toISOString(),checks,photosOnly,overlayInput:photosOnly?'Not validated: API29 stock image SurfaceFlinger overflow reproduced with 0.10.4':'Native injected DPAD',physicalBoxModified:false,pending:['Physical phone QR on TV Box','Physical USB/firmware visibility','Real Bluetooth output and video apps',...(api===29?['Physical remote/Home and autonomous helper not validated on this image']:[])]};fs.writeFileSync(resultsFile,JSON.stringify(record,null,2)+'\n');console.log('Passed',checks.length,'checks');
 }finally{if(browser)await browser.close();if(forward)try{cmd('forward','--remove','tcp:'+forward)}catch(e){}try{panel('wallpaper_action','close')}catch(e){}setPrefs(JSON.parse(Buffer.from(original,'base64').toString()));home();}
})().catch(e=>{console.error(e);process.exitCode=1});
