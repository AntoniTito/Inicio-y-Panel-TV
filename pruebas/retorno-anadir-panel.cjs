const q=require('./adb-fondos.cjs'),s=process.argv[2],phase=process.argv[3];
if(!s||!['antes','despues'].includes(phase))throw Error('Explicit device and antes/despues required; start over an external app');
const p=()=>q.panel(s,'inspect'),key=k=>q.panel(s,'key',String(k));
const state=x=>({page:x.page,title:x.title,edit:x.editMode,selection:x.bottomSelection,addVisible:x.bottomAddVisible,clock:x.panelClockVisible,home:x.homeVisible});
(async()=>{const original=q.panel(s,'prefs_export').match(/base64=([A-Za-z0-9+/=]+)/)[1],images=q.json(s,'wallpaper_backup_manifest');
 await q.wait(()=>p());const initial=p();if(initial.homeVisible)throw Error('Requires external app behind panel and at least one visible app');
 let evidence;
 try{
 key(3);let x=await q.wait(()=>{const x=p();return x.visible&&x.page==='bottom'&&x.bottomAppCount>0?x:null});
 key(19);for(let i=0;i<12&&p().bottomSelection>0;i++)key(21);key(20);
 const last=5+p().bottomAppCount-1;for(let i=0;i<60&&p().bottomSelection<last;i++)key(22);
 q.panel(s,'key','23:800');await q.wait(()=>p().editMode);
 key(22);key(23);await q.wait(()=>p().page==='panelAddApp');const menu=p();key(4);await q.sleep(100);const returned=p();
 evidence={phase,api:+q.command(s,'shell','getprop','ro.build.version.sdk'),emulator:q.command(s,'shell','getprop','ro.kernel.qemu')==='1',menu:state(menu),returned:state(returned),reproducedStalePage:returned.page==='panelAddApp'&&!returned.editMode&&returned.title===''};
 if(phase==='despues'){
  if(returned.page!=='bottom'||!returned.editMode||returned.bottomSelection!==last+1||!returned.bottomAddVisible)throw Error('Return did not restore the edit panel');
  key(21);evidence.afterLeft=state(p());if(p().bottomSelection!==last)throw Error('Left navigation failed');
  key(22);key(23);await q.wait(()=>p().page==='panelAddApp');q.panel(s,'action','back');if(p().page!=='bottom'||!p().editMode)throw Error('Visible Back action failed');
  key(23);await q.wait(()=>p().page==='panelAddApp');q.panel(s,'action','installApps');if(p().page!=='install')throw Error('Nested install page missing');key(4);if(p().page!=='panelAddApp'||!p().editMode)throw Error('Nested Back lost editing');key(4);if(p().page!=='bottom'||!p().editMode)throw Error('Nested return did not restore bottom');evidence.nestedReturn=true;
  key(23);await q.wait(()=>p().page==='panelAddApp');const candidates=p().actions.filter(a=>!['installApps','back','close'].includes(a.id)&&a.enabled);if(candidates.length){const count=p().bottomAppCount;key(20);key(23);await q.wait(()=>p().page==='bottom');if(!p().editMode||p().bottomAppCount!==count+1)throw Error('Adding app did not return to editor');key(4);if(p().bottomAppCount!==count)throw Error('Cancel did not restore app visibility');evidence.addThenCancel=true;}else{key(4);key(4);evidence.addThenCancel=null;}
  if(p().editMode||p().page!=='bottom')throw Error('Back did not cancel editing');
  const n=p().bottomSelection;key(21);if(p().bottomSelection===n)throw Error('Normal navigation failed after cancellation');
  evidence.afterCancel=state(p());key(4);if(p().visible)throw Error('Panel did not close');evidence.closed=true;
 }else if(!evidence.reproducedStalePage)throw Error('Expected bug not reproduced');
 }finally{try{if(p().visible){key(3);q.panel(s,'action','close')}}catch(e){}}
 const same=(a,b)=>JSON.stringify(a.images.map(i=>({id:i.id,size:i.size,sha256:i.sha256})).sort((x,y)=>x.id.localeCompare(y.id)))===JSON.stringify(b.images.map(i=>({id:i.id,size:i.size,sha256:i.sha256})).sort((x,y)=>x.id.localeCompare(y.id)));
 evidence.preferencesUnchanged=q.panel(s,'prefs_export').match(/base64=([A-Za-z0-9+/=]+)/)[1]===original;evidence.imagesUnchanged=same(images,q.json(s,'wallpaper_backup_manifest'));
 if(!evidence.preferencesUnchanged||!evidence.imagesUnchanged)throw Error('Original state changed');
 const name=phase==='antes'?'.local/retorno-anadir-panel-antes-api'+evidence.api+'.json':'pruebas/resultados-retorno-anadir-0.16.5-api'+evidence.api+'.json';q.fs.writeFileSync(q.path.join(q.root,name),JSON.stringify(evidence,null,2)+'\n');console.log(JSON.stringify(evidence));
})().catch(e=>{console.error(e);process.exitCode=1});
