const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s)throw Error('Explicit device required');
const p=()=>q.panel(s,'inspect'),key=k=>q.panel(s,'key',String(k)),action=id=>q.panel(s,'action',id);
const checks=[];
function check(v,name){if(!v)throw Error(name+' '+JSON.stringify(p()));checks.push(name);console.log('OK '+name);}
function exported(){return q.panel(s,'prefs_export').match(/base64=([A-Za-z0-9+/=]+)/)[1];}
const decode=b=>JSON.parse(Buffer.from(b,'base64').toString('utf8'));
const encode=x=>Buffer.from(JSON.stringify(x)).toString('base64');
function focus(id){for(let i=0;i<80&&p().bottomFocusedId!==id;i++)key(22);check(p().bottomFocusedId===id,'focus '+(id.startsWith('app:')?'app':id));}
async function open(){if(p().visible)action('close');if(p().homeVisible)q.command(s,'shell','am','start','-a','android.settings.SETTINGS');await q.wait(()=>!p().homeVisible);key(3);await q.wait(()=>p().visible&&p().page==='bottom');}
function sameImages(a,b){return JSON.stringify(a.images.map(i=>[i.id,i.size,i.sha256]).sort())===JSON.stringify(b.images.map(i=>[i.id,i.size,i.sha256]).sort());}
(async()=>{
 await q.wait(()=>p());
 const original=exported(),prefs=decode(original),images=q.json(s,'wallpaper_backup_manifest');
 const all=prefs.catalog_snapshot.value.split('|').map(x=>x.split(';')[0]);
 if(all.length<2)throw Error('Requires at least two installed launcher apps');
 async function configure(n){const v=decode(original);v.hidden_apps={type:'set',value:all.slice(n)};v.shown_apps={type:'set',value:all.slice(0,n)};q.panel(s,'prefs_restore',encode(v));await open();await q.wait(()=>p().bottomAppCount===n);}
 try{
  await open();key(19);focus('action:audio');key(21);check(p().bottomFocusedId==='action:home','actions left wrap');key(22);check(p().bottomFocusedId==='action:audio','actions right wrap');key(19);check(p().bottomSection==='actions','up stays in actions');
  await configure(0);key(20);check(p().bottomFocusedId==='add_application','empty apps Add');key(21);key(22);key(20);check(p().bottomFocusedId==='add_application','empty row never spills');key(23);check(p().page==='panelAddApp','empty Add opens picker');key(4);check(p().page==='bottom'&&!p().editMode&&p().bottomFocusedId==='add_application','empty picker returns');
  await configure(1);key(20);const single=p().bottomFocusedId;key(21);key(22);check(p().bottomFocusedId===single,'single app stable');
  q.panel(s,'key','23:800');check(p().editMode,'long OK enters editor');key(20);check(p().bottomSection==='apps'&&p().editMode,'edit down confined');key(19);check(p().bottomAppCount===0&&p().bottomFocusedId==='save_changes'&&p().bottomSaveVisible,'hide last focuses Save');key(19);key(20);key(22);check(p().bottomFocusedId==='save_changes','save bounded and up safe');key(21);check(p().bottomFocusedId==='add_application','empty edit Add reachable');key(19);check(p().bottomAppCount===0,'up on Add safe');key(23);key(4);check(p().editMode&&p().page==='bottom'&&p().bottomFocusedId==='add_application','empty editor nested return');key(4);check(!p().editMode&&p().bottomAppCount===1&&p().bottomFocusedId===single,'cancel restores last app and focus');
  await configure(1);key(20);q.panel(s,'key','23:800');key(19);key(23);check(!p().editMode&&p().bottomAppCount===0&&p().bottomFocusedId==='add_application','save empty row');
  await configure(Math.min(all.length-1,3));key(20);let first=p().bottomFocusedId;key(21);const last=p().bottomFocusedId;check(first!==last&&p().bottomSectionIndex===p().bottomAppCount-1,'apps left wrap');key(22);check(p().bottomFocusedId===first,'apps right wrap');key(22);const remembered=p().bottomFocusedId;key(19);focus('action:bluetooth');key(20);check(p().bottomFocusedId===remembered,'apps independent memory');key(19);check(p().bottomFocusedId==='action:bluetooth','actions independent memory');key(4);await open();check(p().bottomFocusedId==='action:bluetooth','close reopen retains row focus');key(20);check(p().bottomFocusedId===remembered,'close reopen retains app focus');
  const before=q.json(s,'metrics');for(let i=0;i<12;i++)key(i%2?21:22);const after=q.json(s,'metrics');check(before.catalogScans===after.catalogScans&&before.fullRenders===after.fullRenders,'arrows never scan or rebuild');
  focus(last);q.panel(s,'key','23:800');key(22);check(p().bottomFocusedId==='add_application','editor Add after last');key(23);key(4);check(p().page==='bottom'&&p().editMode&&p().bottomFocusedId==='add_application','Add return keeps edit and focus');key(23);action('installApps');check(p().page==='install','nested Install opens');key(4);check(p().page==='panelAddApp'&&p().editMode,'nested Install Back');key(4);check(p().page==='bottom'&&p().editMode,'nested picker Back');key(23);const candidates=p().actions.filter(a=>a.enabled&&!['installApps','back','close'].includes(a.id));const count=p().bottomAppCount;action(candidates[0].id);check(p().page==='bottom'&&p().editMode&&p().bottomAppCount===count+1&&p().bottomFocusedId.startsWith('app:'),'add selects new app');key(4);check(!p().editMode&&p().bottomAppCount===count&&p().bottomFocusedId===last,'cancel restores before adding');
  key(19);key(20);focus(first);q.panel(s,'key','23:800');const state=exported();key(21);check(exported()===state&&p().bottomFocusedId===first,'reorder first left bounded');key(22);check(p().bottomFocusedId===first&&p().bottomSectionIndex===1,'reorder keeps package focus');key(20);check(p().bottomSection==='apps'&&p().editMode,'reorder remains in Apps');key(4);check(p().bottomFocusedId===first&&p().bottomSectionIndex===0,'cancel restores order and prior focus');
  for(const page of ['settings','sound','network','bluetooth','appearance','manage','system','timer','diagnostics','regional','storage','status','animations','timezone','display','install']){q.panel(s,'open',page);check(p().page===page&&p().actions.some(a=>a.id==='back')&&p().actions.some(a=>a.id==='close'),'menu '+page);action('close');}
 }finally{q.panel(s,'prefs_restore',original);}
 check(exported()===original,'preferences restored');check(sameImages(images,q.json(s,'wallpaper_backup_manifest')),'image hashes unchanged');
 const api=+q.command(s,'shell','getprop','ro.build.version.sdk');
 q.fs.writeFileSync(q.path.join(q.root,'pruebas','resultados-carrusel-0.18.0-api'+api+'.json'),JSON.stringify({api,emulator:q.command(s,'shell','getprop','ro.kernel.qemu')==='1',checks:checks.filter(x=>!x.startsWith('focus ')),preferencesRestored:true,imageHashesUnchanged:true},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
