const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const p=()=>q.json(s,'tv_inspect'),b=()=>q.json(s,'tv_buttons_inspect'),d=()=>q.json(s,'tv_destinations_inspect');
const read=m=>q.panel(s,m).match(/base64=([A-Za-z0-9+/=]+)/)[1],write=(m,o)=>q.panel(s,m,Buffer.from(JSON.stringify(o)).toString('base64'));
const checks=[],check=(ok,name)=>{if(!ok)throw Error(name);checks.push(name);console.log('OK '+name);};
const norm=x=>x.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
async function teach(){
 q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','normal');
 c({op:'button_teach',id:'qa-first'});await q.wait(()=>p().phase==='teaching');await q.sleep(650);
 for(const label of ['television','peru','tv peru']){
  const before=p().draftSteps;for(let i=0;i<5&&norm(p().navigation.recordedFocus)!==label;i++){q.command(s,'shell','input','keyevent','20');await q.sleep(250);}
  if(norm(p().navigation.recordedFocus)!==label)throw Error('Fixture focus '+label);
  c({op:'key',key:23});q.command(s,'shell','input','keyevent','23');c({op:'key',key:23,up:true});await q.wait(()=>p().draftSteps===before+1);await q.sleep(500);
 }
 c({op:'record_done'});c({op:'choose',id:'done'});
}
(async()=>{
 const original=read('tv_export'),buttons=read('tv_buttons_export'),services=q.command(s,'shell','settings','get','secure','enabled_accessibility_services');
 try{
  c({op:'select',package:'local.z6qa.tvfixture'});const target=JSON.parse(Buffer.from(read('tv_export'),'base64'));
  const first={id:'qa-first',kind:'route',label:'Mi canal',enabled:true,target:{...target,key:183,keyName:'KEYCODE_PROG_RED'}};
  const second={id:'qa-second',kind:'open',label:'Otra tecla',enabled:true,target:{...target,key:184,keyName:'KEYCODE_PROG_GREEN'}};
  write('tv_buttons_restore',{schema:1,actions:[first,second],layout:{},destinations:[],pendingDestination:null});
  await teach();await q.wait(()=>p().phase==='confirming',30000);
  check(d().pending.passed&&d().pending.target.steps.length===3,'completed test persists a recoverable pending destination');
  check(!b().actions[0].target.verified,'previous assignment stays unchanged before confirmation');
  c({op:'key',key:3});c({op:'key',key:3,up:true});check(p().phase==='idle'&&d().pending.passed,'Home interruption retains completed test');
  q.command(s,'shell','am','force-stop','local.z6panel');q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity');
  q.command(s,'shell','settings','delete','secure','enabled_accessibility_services');q.command(s,'shell','settings','put','secure','enabled_accessibility_services',services);q.command(s,'shell','settings','put','secure','accessibility_enabled','1');await q.wait(()=>p().navigationService);
  check(d().pending.passed&&d().pending.profileId==='qa-first','pending destination survives real process recreation');
  c({op:'pending_save'});check(b().actions[0].target.verified&&b().actions[0].target.steps.length===3,'saving recovered test updates original button');
  check(d().pending===null&&d().destinations.length===1,'confirmed destination enters common library once');
  const id=d().destinations[0].id;c({op:'destination_use',id,button:'qa-second'});c({op:'destination_use',id});
  const values=b();check(values.actions[1].kind==='route'&&values.actions[1].target.channel===values.actions[0].target.channel,'another button reuses the tested destination');
  check(values.actions[1].target.key===184&&values.actions[1].label==='Otra tecla','reusing a destination preserves key and custom label');
  const startup=JSON.parse(Buffer.from(read('tv_export'),'base64'));check(startup.verified&&JSON.stringify(startup.steps)===JSON.stringify(values.actions[0].target.steps),'startup and wake reuse same tested route');
  check(d().destinations.length===1,'reuse creates no duplicate destinations');
  c({op:'button_label',id:'qa-first',label:'Mi television'});check(b().actions[0].label==='Mi television'&&b().actions[0].target.key===183&&b().actions[0].target.scan===0,'renaming leaves physical binding unchanged');
  const staged=JSON.parse(Buffer.from(read('tv_buttons_export'),'base64'));staged.pendingDestination={target:staged.actions[0].target,profileId:'qa-first',personal:true,passed:false};write('tv_buttons_restore',staged);
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','delayed','--ez','stage_only','true');c({op:'pending_retry'});await q.wait(()=>p().phase==='navigating');c({op:'cancel'});
  check(d().pending&&!d().pending.passed&&b().actions[0].target.verified,'interrupted incomplete test keeps draft and previous verified assignment');
  let refused=false;try{c({op:'pending_save'});}catch(e){refused=true;}check(refused,'incomplete test cannot be saved as verified');
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','normal','--ez','stage_only','true');c({op:'pending_retry'});await q.wait(()=>p().phase==='confirming',30000);c({op:'choose',id:'confirm_yes'});
  check(d().pending===null&&d().destinations.length===1,'retry reuses captured route and confirmed save deduplicates it');
  q.panel(s,'tv_open');c({op:'ui_choose',id:'button:qa-first'});const ui=q.json(s,'tv_ui_inspect');
  check(ui.keyDescription.includes('KEYCODE_PROG_RED')&&ui.keyDescription.includes('183')&&ui.controls.some(x=>x.id==='delete')&&ui.controls.some(x=>x.id==='label'),'editor exposes internal key identity, rename and delete');
  c({op:'ui_choose',id:'delete'});c({op:'ui_choose',id:'delete_cancel'});check(b().actions.some(a=>a.id==='qa-first'),'cancelled deletion preserves button');
  c({op:'ui_choose',id:'delete'});c({op:'ui_choose',id:'delete_confirm'});check(!b().actions.some(a=>a.id==='qa-first')&&d().destinations.length===1,'confirmed button deletion preserves reusable destination');
  c({op:'ui_choose',id:'add'});check(b().actions.length===2,'new generic button needs no fixed Netflix or YouTube name');
  c({op:'ui_choose',id:'key_delete'});c({op:'ui_choose',id:'delete_confirm'});check(b().actions.length===1,'new unassigned button can be deleted from capture dialog');
  const exported=JSON.parse(Buffer.from(read('tv_buttons_export'),'base64'));write('tv_buttons_restore',exported);check(b().actions[0].label==='Otra tecla'&&d().destinations.length===1,'export restore preserves labels and reusable destinations');
 }finally{c({op:'cancel'});q.panel(s,'tv_restore',original);q.panel(s,'tv_buttons_restore',buttons);}
 const version=q.command(s,'shell','dumpsys','package','local.z6panel').match(/versionName=(\d+\.\d+\.\d+)/)[1];
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-destinos-'+version+'-api31.json'),JSON.stringify({api:31,emulator:true,actualVideo:false,processRecreation:true,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
