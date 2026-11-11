const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const p=()=>q.json(s,'tv_inspect'),c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const exportTv=()=>q.panel(s,'tv_export').match(/base64=([A-Za-z0-9+/=]+)/)[1],checks=[];
function check(v,name){if(!v)throw Error(name+' '+JSON.stringify(p()));checks.push(name);console.log('OK '+name);}
(async()=>{
 await q.wait(()=>p().navigationService);const original=exportTv();
 try{
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','list');c({op:'select',package:'local.z6qa.tvfixture'});c({op:'teach'});
  for(const label of ['television','peru','tv peru']){
   const norm=t=>t.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
   const row=await q.wait(()=>p().navigation.controls.find(x=>norm(x.label)===label),15000),before=p().draftSteps;
   c({op:'choose',id:row.id});await q.wait(()=>p().draftSteps===before+1,15000);
  }
  check(p().draftSteps===3,'guided native list selection uses stable labels');
  await q.sleep(500);c({op:'choose',id:'done'});await q.wait(()=>p().phase==='confirming',25000);
  check(p().step===3,'native focus and verified directional navigation replay');
  c({op:'choose',id:'confirm_yes'});check(p().verified,'native list route saved only after confirmation');
  q.fs.writeFileSync(q.path.join(q.root,'.local','fixture-0180-list-verified.base64'),exportTv());
  const pid=q.command(s,'shell','pidof','local.z6qa.tvfixture');c({op:'run'});await q.wait(()=>p().phase==='idle',25000);
  check(p().status.startsWith('Canal preparado:')&&pid!==q.command(s,'shell','pidof','local.z6qa.tvfixture'),'native list route restarts app and reaches player');
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','list-alt');c({op:'run'});await q.wait(()=>p().phase==='idle',25000);
  check(p().status.startsWith('Canal preparado:')&&p().step===3,'alternate native channel-row layout matches the exact taught channel');
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','list-ambiguous');c({op:'run'});await q.wait(()=>p().phase==='idle',25000);
  check(!p().verified&&p().status.includes('ambiguos'),'duplicate channel labels fail without selecting a guessed row');
 }finally{c({op:'cancel'});q.panel(s,'tv_restore',original);}
 q.fs.writeFileSync(q.path.join(q.root,'pruebas','resultados-listas-tv-0.18.0-api31.json'),JSON.stringify({api:31,emulator:true,fixture:true,actualVideo:false,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
