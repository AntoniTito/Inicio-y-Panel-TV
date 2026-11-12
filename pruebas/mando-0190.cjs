const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const p=()=>q.json(s,'tv_inspect'),buttons=()=>q.json(s,'tv_buttons_inspect');
const read=m=>q.panel(s,m).match(/base64=([A-Za-z0-9+/=]+)/)[1],write=(m,o)=>q.panel(s,m,Buffer.from(JSON.stringify(o)).toString('base64'));
const checks=[],check=(v,n)=>{if(!v)throw Error(n+' '+JSON.stringify(p()));checks.push(n);console.log('OK '+n);};
const norm=x=>x.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
async function realOK(){c({op:'key',key:23});q.command(s,'shell','input','keyevent','23');c({op:'key',key:23,up:true});await q.sleep(650);}
async function record(mode,id){
 q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode',mode);
 c({op:'button_teach',id});await q.wait(()=>p().phase==='teaching',10000);await q.sleep(650);
 for(const label of ['television','peru','tv peru']){
  let before=p().draftSteps;q.command(s,'shell','input','keyevent','20');await q.sleep(220);
  for(let i=0;i<4&&norm(p().navigation.recordedFocus)!==label;i++){q.command(s,'shell','input','keyevent','20');await q.sleep(220);}
  check(norm(p().navigation.recordedFocus)===label,'real app focus '+mode+' '+label);
  await realOK();await q.wait(()=>p().draftSteps===before+1,6000);
 }
 c({op:'record_done'});c({op:'choose',id:'done'});await q.wait(()=>p().phase==='confirming'||p().phase==='idle',25000);
 check(p().phase==='confirming'&&p().step===3,'live recording replays '+mode+' before saving');
 check(p().restartBackend==='android','native verified process close '+mode);
 check(!p().navigation.remoteState,'native UI actions without injected helper keys '+mode);
 check(!buttons().actions.find(a=>a.id===id).target.verified,'draft remains separate until confirmed '+mode);
 c({op:'choose',id:'confirm_yes'});check(buttons().actions.find(a=>a.id===id).target.verified,'save independent destination '+mode);
}
(async()=>{
 const originalTv=read('tv_export'),originalButtons=read('tv_buttons_export');
 try{
  c({op:'select',package:'local.z6qa.tvfixture'});const target=JSON.parse(Buffer.from(read('tv_export'),'base64'));
  write('tv_buttons_restore',{actions:[{id:'qa-route',kind:'route',enabled:true,target:{...target,key:183,keyName:'Canal'}},{id:'qa-reopen',kind:'reopen',enabled:true,target:{...target,key:184,keyName:'Reabrir'}}],layout:{}});
  check(buttons().actions.length===2,'two simultaneous independent bindings');
  if(process.argv[3]==='list')await record('list','qa-route');else{
  await record('normal','qa-route');const saved=buttons().actions.find(a=>a.id==='qa-route');
  c({op:'key',key:184});c({op:'key',key:184,up:true});await q.wait(()=>p().phase==='idle',12000);check(p().kind==='reopen'&&p().restartBackend==='android','second button reopens without running route');
  check(buttons().actions.find(a=>a.id==='qa-route').target.channel===saved.target.channel,'second button preserves first destination');
  const before=q.command(s,'shell','pidof','local.z6qa.tvfixture');c({op:'key',key:183});for(let i=1;i<=8;i++)c({op:'key',key:183,repeat:i});c({op:'key',key:183,up:true});await q.wait(()=>p().phase==='idle',20000);check(/^(Canal|Destino) preparado:/.test(p().status)&&before!==q.command(s,'shell','pidof','local.z6qa.tvfixture'),'first button reaches its destination from fresh process');
  const once=q.command(s,'shell','pidof','local.z6qa.tvfixture');await q.sleep(1000);check(once===q.command(s,'shell','pidof','local.z6qa.tvfixture'),'held button queues no extra restarts');
  let values=buttons();values.actions[0].target.verified=false;values.actions[0].target.steps=[];values.actions[0].target.endpoint={};write('tv_buttons_restore',values);await record('list','qa-route');
  const idle=p().navigation.queries;await q.sleep(1300);check(p().navigation.queries===idle&&!p().navigation.active,'no idle tree queries or overlay');
  let rejected=false;values=buttons();values.actions[1].target.key=183;try{write('tv_buttons_restore',values);}catch(e){rejected=true;}check(rejected&&buttons().actions[1].target.key===184,'conflicting assignment fails atomically');
  values=buttons();values.actions[0].target.key=3;rejected=false;try{write('tv_buttons_restore',values);}catch(e){rejected=true;}check(rejected,'Home remains protected');
  c({op:'button_teach',id:'qa-route'});await q.wait(()=>p().phase==='teaching');c({op:'cancel'});check(p().phase==='idle'&&!p().navigation.active,'cancel clears recording windows and task');
  }
 }finally{c({op:'cancel'});q.panel(s,'tv_restore',originalTv);q.panel(s,'tv_buttons_restore',originalButtons);}
 const version=q.command(s,'shell','dumpsys','package','local.z6panel').match(/versionName=(\d+\.\d+\.\d+)/)[1];
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-mando-'+version+'-api31'+(process.argv[3]==='list'?'-list':'')+'.json'),JSON.stringify({api:31,emulator:true,fixture:true,physicalRemote:false,actualVideo:false,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
