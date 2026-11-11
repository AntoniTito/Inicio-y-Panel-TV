const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const p=()=>q.panel(s,'tv_inspect'),c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const checks=[],check=(v,name)=>{if(!v)throw Error(name+' '+JSON.stringify(p()));checks.push(name);console.log('OK '+name);};
const mode=m=>q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode',m,'--ez','stage_only','true');
(async()=>{
 const original=q.panel(s,'tv_export').match(/base64=([A-Za-z0-9+/=]+)/)[1];
 try{
  q.panel(s,'tv_restore',q.fs.readFileSync(q.path.join(q.root,'.local/fixture-0180-verified.base64'),'utf8'));c({op:'activate'});
  // Stage the transient state while the process is stopped: the fresh launch
  // triggered by wake must be the first consumer, rather than the setup intent.
  mode('retry-once');q.command(s,'shell','am','force-stop','local.z6qa.tvfixture');
  c({op:'event',event:'off'});c({op:'event',event:'on'});
  await q.wait(()=>p().phase!=='idle',10000);await q.wait(()=>p().phase==='idle'&&p().reason==='despertar',45000);
  check(p().automaticRetried&&p().status.startsWith('Canal preparado:'),'one automatic fresh launch recovers a transient missing channel');
  mode('retry-never');c({op:'event',event:'off'});c({op:'event',event:'on'});
  await q.wait(()=>p().phase!=='idle',10000);await q.wait(()=>{const v=p();return v.automaticRetried&&v.phase==='navigating';},30000);
  const pid=q.command(s,'shell','pidof','local.z6qa.tvfixture');await q.sleep(6000);
  check(q.command(s,'shell','pidof','local.z6qa.tvfixture')===pid,'second missing menu does not queue repeated app launches');
  await q.wait(()=>p().phase==='idle',75000);
  check(!p().status.startsWith('Canal preparado:')&&q.command(s,'shell','pidof','local.z6qa.tvfixture')===pid,'second failure expires and leaves the app available for manual use');
 }finally{c({op:'cancel'});q.panel(s,'tv_restore',original);}
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-reintento-tv-0.18.0-api31.json'),JSON.stringify({api:31,emulator:true,fixture:true,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
