// Uses the authenticated shell bridge and native Activity lifecycle, never image deletion.
const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s)throw Error('Explicit ADB device required');
const results=[],check=(name,ok,evidence)=>{results.push({name,passed:!!ok,evidence});console.log((ok?'OK ':'FAIL ')+name);if(!ok)throw Error(name)};
const home=()=>q.panel(s,'home_inspect');
const key=n=>q.command(s,'shell','input','keyevent',String(n));
async function showHome(){try{q.panel(s,'action','close')}catch(e){}q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity');return q.wait(()=>{const h=home();return h.visible&&h.wallpaper.loaded?h:null},20000)}
(async()=>{const before=q.json(s,'wallpaper_backup_manifest'),prefs=q.panel(s,'prefs_export').match(/base64=([A-Za-z0-9+/=]+)/)[1];
 const state=JSON.parse(Buffer.from(prefs,'base64'));if(!state.wallpaper_mode||state.wallpaper_mode.value==='original')throw Error('Select a prepared photo first; this test never changes wallpaper preferences');
 let h=await showHome(),initial=h.wallpaper;
 check('Full-size wallpaper is ready',initial.loaded&&initial.viewWidth===initial.width&&initial.viewHeight===initial.height,{width:initial.width,height:initial.height});
 for(let i=0;i<6;i++){q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity');await q.sleep(150)}
 h=home();check('Six native HOME intents retain the visible image',h.wallpaper.loaded&&h.wallpaper.renders===initial.renders&&h.wallpaper.releases===initial.releases,{renders:h.wallpaper.renders-initial.renders,releases:h.wallpaper.releases-initial.releases});
 key(3);h=home();check('Remote HOME retains wallpaper',h.wallpaper.loaded&&h.wallpaper.releases===initial.releases,{});
 q.panel(s,'open','settings');q.panel(s,'action','personalize');await q.sleep(300);h=home();let panel=q.panel(s,'inspect');check('Panel e inicio opens above intact Home',panel.page==='appearance'&&h.wallpaper.loaded&&h.wallpaper.releases===initial.releases,{page:panel.page});
 q.panel(s,'action','close');q.panel(s,'open','settings');q.panel(s,'action','android');await q.sleep(600);h=home();
 const activities=q.command(s,'shell','dumpsys','activity','activities');check('Android settings opens successfully',/mResumedActivity:.*com\.android\.tv\.settings/.test(activities),{homeVisible:h.visible,imageRetained:h.wallpaper.loaded,workActive:h.wallpaper.active});
 check('Image work stops behind native settings',!h.wallpaper.active,{});
 // This firmware may show settings as a translucent side activity. Only a fully hidden
 // HOME must release its bitmap; either layout must return with the image intact.
 await showHome();h=home();check('Returning from Android settings restores the same wallpaper',h.wallpaper.loaded&&h.wallpaper.key===initial.key,{});
 check('Android settings return draws without the empty-frame timeout',!h.wallpaper.firstDrawPending&&h.wallpaper.firstFrameFallbacks===initial.firstFrameFallbacks,{fallbacks:h.wallpaper.firstFrameFallbacks});
 q.panel(s,'wallpaper_open');await q.wait(()=>q.panel(s,'wallpaper_inspect').visible);
 await q.wait(()=>!home().wallpaper.loaded);h=home();check('Fully covering Home releases its bitmap',!h.wallpaper.active&&!h.wallpaper.loaded,{});
 q.panel(s,'wallpaper_action','close');await showHome();h=home();check('Returning from Fondos restores the image',h.wallpaper.loaded&&h.wallpaper.key===initial.key,{});
 check('Fondos return draws without the empty-frame timeout',!h.wallpaper.firstDrawPending&&h.wallpaper.firstFrameFallbacks===initial.firstFrameFallbacks,{fallbacks:h.wallpaper.firstFrameFallbacks});
 const photos=before.images.map(i=>({id:i.id,size:i.size,sha256:i.sha256})).sort((a,b)=>a.id.localeCompare(b.id));
 const now=q.json(s,'wallpaper_backup_manifest').images.map(i=>({id:i.id,size:i.size,sha256:i.sha256})).sort((a,b)=>a.id.localeCompare(b.id));
 check('Image bytes and IDs unchanged',JSON.stringify(photos)===JSON.stringify(now),{count:now.length});
 check('Preferences unchanged',q.panel(s,'prefs_export').match(/base64=([A-Za-z0-9+/=]+)/)[1]===prefs,{});
 const report={version:'0.16.2',api:+q.command(s,'shell','getprop','ro.build.version.sdk'),emulator:q.command(s,'shell','getprop','ro.kernel.qemu')==='1',checks:results};
 q.fs.writeFileSync(q.path.join(q.root,'pruebas','resultados-transiciones-0.16.2-api'+report.api+'.json'),JSON.stringify(report,null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
