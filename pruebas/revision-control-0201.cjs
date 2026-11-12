const q=require('./adb-fondos.cjs'),s=process.argv[2],app='local.z6qa.tvfixture',other='com.android.z6qa.fixture';
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const p=()=>q.json(s,'tv_inspect'),ui=()=>q.json(s,'tv_ui_inspect'),b=()=>q.json(s,'tv_buttons_inspect'),d=()=>q.json(s,'tv_destinations_inspect');
const read=m=>q.panel(s,m).match(/base64=([A-Za-z0-9+/=]+)/)[1],write=(m,o)=>q.panel(s,m,Buffer.from(JSON.stringify(o)).toString('base64'));
const checks=[],check=(ok,name)=>{if(!ok)throw Error(name);checks.push(name);console.log('OK '+name);};
const norm=x=>x.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
async function enter(){q.panel(s,'open','manage');q.panel(s,'action','tvRecovery');await q.wait(()=>{const value=ui();return value.visible&&value.page==='main'&&value.windowFocused&&value.dialogs===0},10000);}
async function chooseApp(pkg){const choices=q.json(s,'tv_apps_inspect').apps.filter(a=>a.openable);const index=choices.findIndex(a=>a.package===pkg);if(index<0)throw Error('Missing app');c({op:'ui_choose',id:'app'});await q.sleep(250);if(index)q.command(s,'shell','input','keyevent',...Array(index).fill('20'));q.command(s,'shell','input','keyevent','23');await q.wait(()=>b().actions.find(a=>a.id==='qa-route').target.package===pkg);}
async function recordLibrary(){
 c({op:'button_teach',id:'qa-open'});await q.wait(()=>p().phase==='teaching');console.log('Teaching generic section');await q.sleep(650);
 for(const label of ['biblioteca','mis listas']){const before=p().draftSteps;for(let i=0;i<5&&norm(p().navigation.recordedFocus)!==label;i++){q.command(s,'shell','input','keyevent','20');await q.sleep(250);}if(norm(p().navigation.recordedFocus)!==label)throw Error('Library focus '+label);c({op:'key',key:23});q.command(s,'shell','input','keyevent','23');c({op:'key',key:23,up:true});await q.wait(()=>p().draftSteps===before+1);console.log('Recorded '+label);await q.sleep(500);}
 c({op:'record_done'});c({op:'choose',id:'done'});console.log('Testing generic section');await q.wait(()=>p().phase==='confirming',30000);
}
(async()=>{
 const original=read('tv_export'),buttons=read('tv_buttons_export'),services=q.command(s,'shell','settings','get','secure','enabled_accessibility_services');
 try{
  c({op:'select',package:app});const target=JSON.parse(Buffer.from(read('tv_export'),'base64'));
  const selector=(id,text,type='android.widget.Button')=>({id:app+':id/'+id,text,description:'',type,hint:''});
  target.steps=[['television','TELEVISIÓN'],['peru','PERÚ'],['channel','TV PERÚ']].map(([id,text])=>({op:'click',activity:app+'.MainActivity',target:selector(id,text)}));target.endpoint={activity:app+'.MainActivity',marker:selector('playing','TV Perú · reproducción de prueba','android.widget.TextView')};target.channel='TV PERÚ';target.verified=true;
  c({op:'select',package:other});const second=JSON.parse(Buffer.from(read('tv_export'),'base64'));
  const actions=[{id:'qa-route',kind:'route',label:'Destino probado',enabled:true,target:{...target,key:183,keyName:'KEYCODE_PROG_RED'}},{id:'qa-open',kind:'open',label:'Biblioteca',enabled:true,target:{...second,key:184,keyName:'KEYCODE_PROG_GREEN'}}];
  write('tv_buttons_restore',{actions,layout:{},destinations:[{id:'qa-destination',target}],pendingDestination:null});
  c({op:'event',event:'control_task_root'});await q.wait(()=>ui().visible);q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity','-f','0x10000000');
  const stack=q.command(s,'shell','dumpsys','activity','activities');check(stack.includes('.HomeActivity')&&stack.includes('.TvRecoveryActivity'),'home and control task coexist before reopening');
  await enter();check(ui().visible&&ui().page==='main','real panel entry opens configuration after HOME');
  for(let i=0;i<4;i++){c({op:'ui_choose',id:'button:qa-route'});q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity','-f','0x10000000');await enter();}
  check(ui().page==='main'&&ui().dialogs===0,'repeated entry returns to main with no stale editor');
  c({op:'ui_choose',id:'add'});q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity','-f','0x10000000');await enter();check(ui().dialogs===0&&!p().learningKey&&!p().keyPromptReady,'reopening dismisses capture dialog and releases key interception');
  write('tv_buttons_restore',{actions,layout:{},destinations:[{id:'qa-destination',target}],pendingDestination:null});c({op:'destination_use',id:'qa-destination'});c({op:'toggle',kind:'boot'});c({op:'toggle',kind:'wake'});
  await enter();c({op:'ui_choose',id:'destinations'});check(ui().page==='destinations'&&ui().controls.some(a=>a.id==='routeDelete'),'route deletion is directly visible in manager');
  c({op:'ui_choose',id:'routeDelete'});c({op:'ui_choose',id:'route_delete_cancel'});check(d().destinations.length===1&&b().actions[0].target.verified&&p().boot,'cancel route deletion preserves all assignments');
  c({op:'ui_choose',id:'routeDelete'});c({op:'ui_choose',id:'route_delete_only'});check(d().destinations.length===0&&b().actions[0].target.verified&&p().verified,'library-only deletion keeps button and startup copies');
  const state=JSON.parse(Buffer.from(read('tv_buttons_export'),'base64'));state.destinations=[{id:'qa-destination',target}];state.pendingDestination={target,profileId:'qa-route',personal:true,passed:true};write('tv_buttons_restore',state);
  await enter();c({op:'ui_choose',id:'destinations'});c({op:'ui_choose',id:'routeDelete'});c({op:'ui_choose',id:'route_delete_all'});
  check(d().destinations.length===0&&d().pending===null,'complete route deletion removes library and matching draft');
  check(b().actions[0].kind==='reopen'&&!b().actions[0].target.verified&&b().actions[0].target.key===183,'complete deletion keeps button identity and normal reopen action');
  check(!p().boot&&!p().wake&&!p().verified,'complete deletion disables matching automatic route');
  check(b().actions[1].kind==='open'&&b().actions[1].target.key===184,'deletion preserves unrelated app button');
  check(ui().page==='destinations'&&ui().controls.some(a=>a.id==='back'),'empty route manager retains a way back');
  const reset=JSON.parse(Buffer.from(read('tv_buttons_export'),'base64'));reset.actions[0]={...actions[0]};write('tv_buttons_restore',reset);
  await enter();c({op:'ui_choose',id:'button:qa-route'});c({op:'ui_choose',id:'destination'});check(ui().dialogMessages.length>0&&!/canal|televisi/i.test(ui().dialogMessages.join(' ')),'route instructions describe a generic screen rather than television');q.command(s,'shell','input','keyevent','4');
  q.command(s,'shell','am','start','-n',other+'/.MainActivity','--es','mode','library');await enter();c({op:'ui_choose',id:'button:qa-route'});await chooseApp(other);
  check(b().actions[0].kind==='open'&&!b().actions[0].target.verified,'choosing a different app resets stale route to normal opening');
  check(!ui().controls.some(a=>a.id==='destination'),'normal opening exposes no compulsory destination');c({op:'ui_choose',id:'test'});await q.wait(()=>p().phase==='idle');check(p().appLaunches===1&&p().status==='App abierta','another app opens without teaching a channel or section');
  c({op:'select',package:other});const noNav=services.split(':').filter(x=>!x.includes('TvNavigationService')).join(':');q.command(s,'shell','settings','put','secure','enabled_accessibility_services',noNav);await q.wait(()=>!p().navigationService);c({op:'toggle',kind:'boot'});c({op:'event',event:'boot_waiting'});await q.wait(()=>p().phase==='idle'&&p().appLaunches===1,45000);
  check(p().kind==='reopen'&&p().status==='App cerrada y reabierta','automatic startup works without route or navigation permission');c({op:'toggle',kind:'boot'});q.command(s,'shell','settings','put','secure','enabled_accessibility_services',services);await q.wait(()=>p().navigationService);
  await recordLibrary();check(!/canal|imagen y sonido/i.test(p().navigation.controls.map(a=>a.label).join(' ')),'generic destination confirmation contains no video requirement');c({op:'choose',id:'confirm_yes'});
  check(b().actions.find(a=>a.id==='qa-open').target.channel.toLowerCase()==='mis listas'&&d().destinations.some(e=>e.target.package===other),'optional non-TV section records tests and saves successfully');
  await enter();check(ui().visible&&ui().page==='main'&&ui().dialogs===0,'configuration remains accessible after saving generic destination');
 }catch(error){q.fs.writeFileSync(q.path.join(q.root,'.local/revision-control-failure.json'),JSON.stringify({state:p(),ui:ui(),destinations:d()},null,2));throw error;}finally{c({op:'cancel'});q.command(s,'shell','settings','put','secure','enabled_accessibility_services',services);q.panel(s,'tv_restore',original);q.panel(s,'tv_buttons_restore',buttons);}
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-revision-control-0.20.1-api31.json'),JSON.stringify({api:31,emulator:true,actualVideo:false,checks},null,2)+'\n');
})().catch(e=>{console.error(e.message);process.exitCode=1});
