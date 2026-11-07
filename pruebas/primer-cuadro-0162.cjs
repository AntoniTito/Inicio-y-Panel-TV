const q=require('./adb-fondos.cjs'),s=process.argv[2];
const version=process.argv[3]||'0.16.2';if(!/^0\.16\.[23]$/.test(version))throw Error('Unsupported test report version');
if(!/^emulator-\d+$/.test(s||'')||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Emulator only');
const original=q.panel(s,'prefs_export').match(/base64=([A-Za-z0-9+/=]+)/)[1],state=JSON.parse(Buffer.from(original,'base64')),checks=[];const initialFallbacks=q.panel(s,'home_inspect').wallpaper.firstFrameFallbacks;let created='',forward=0;
const assert=(name,ok)=>{checks.push({name,passed:!!ok});if(!ok)throw Error(name);console.log('OK '+name)};
(async()=>{try{
 q.panel(s,'wallpaper_open');await q.wait(()=>q.panel(s,'wallpaper_inspect').visible);let p=q.panel(s,'wallpaper_inspect').photos[0];
 if(!p){q.panel(s,'wallpaper_action','receive');const receiver=await q.wait(()=>{const r=q.panel(s,'wallpaper_inspect');return r.receiverRunning?r:null}),u=new URL(receiver.receiverUrl);forward=+q.command(s,'forward','tcp:0','tcp:'+u.port);const response=await fetch('http://127.0.0.1:'+forward+u.pathname+'upload?name=first-frame-test.png',{method:'POST',body:q.fs.readFileSync(require('./imagen-fixture.cjs').syntheticImage(q.root))});if(!response.ok)throw Error(await response.text());created=(await response.json()).id;p={id:created};q.panel(s,'wallpaper_action','close');q.command(s,'forward','--remove','tcp:'+forward);forward=0;}
 q.panel(s,'wallpaper_action','close');
 for(const mode of ['original','photo','slideshow','missing']){
  const next={...state,wallpaper_mode:{type:'string',value:mode==='missing'?'photo':mode},wallpaper_photo:{type:'string',value:mode==='missing'?'00000000000000000000000000000000':p.id},wallpaper_slideshow_use_all:{type:'boolean',value:true}};
  q.panel(s,'prefs_restore',Buffer.from(JSON.stringify(next)).toString('base64'));
  q.panel(s,'wallpaper_open');await q.wait(()=>q.panel(s,'wallpaper_inspect').visible);await q.wait(()=>!q.panel(s,'home_inspect').wallpaper.loaded);q.panel(s,'wallpaper_action','close');q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity');
  const h=await q.wait(()=>{const h=q.panel(s,'home_inspect');return h.visible&&!h.wallpaper.firstDrawPending&&(['photo','slideshow'].includes(mode)?h.wallpaper.loaded:!h.wallpaper.loaded)?h:null});
  assert(mode+' returns without a blank-frame timeout',h.wallpaper.firstFrameFallbacks===initialFallbacks);
 }
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-primer-cuadro-'+version+'-api31.json'),JSON.stringify({version,api:31,emulator:true,initialFallbacks,checks},null,2)+'\n');
 }finally{if(forward)q.command(s,'forward','--remove','tcp:'+forward);if(created){q.panel(s,'wallpaper_open');await q.wait(()=>q.panel(s,'wallpaper_inspect').visible);q.panel(s,'wallpaper_action','remove:'+created);await q.wait(()=>!q.panel(s,'wallpaper_inspect').photos.some(p=>p.id===created));q.panel(s,'wallpaper_action','close');}q.panel(s,'prefs_restore',original);q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity')}})().catch(e=>{console.error(e);process.exitCode=1});
