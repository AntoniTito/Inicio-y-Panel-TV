const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const p=()=>q.json(s,'tv_inspect'),c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const checks=[],check=(v,name)=>{if(!v)throw Error(name+' '+JSON.stringify(p()));checks.push(name);console.log('OK '+name);};
const device='0003|ffff|0001|Z6 test remote',pid=()=>q.command(s,'shell','pidof','local.z6qa.tvfixture');
(async()=>{
 await q.wait(()=>p().navigationService);
 const original=q.panel(s,'tv_export').match(/base64=([A-Za-z0-9+/=]+)/)[1];
 try{
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','normal');
  q.panel(s,'tv_restore',q.fs.readFileSync(q.path.join(q.root,'.local/fixture-0180-verified.base64'),'utf8'));
  c({op:'prepare_key',name:'Netflix'});await q.wait(()=>p().keyPromptReady);
  q.command(s,'shell','input','keyevent','23');await q.wait(()=>p().learningKey);
  c({op:'remote_fixture',type:'learn',device,code:115});await q.sleep(300);
  check(p().learningKey,'auxiliary learning ignores volume and other essential keys');
  c({op:'remote_fixture',type:'learn',device,code:88});await q.wait(()=>p().keyLearning.result==='detected_auxiliary');
  check(!p().learningKey&&p().remoteCode===0,'special function key awaits explicit confirmation');
  q.command(s,'shell','input','keyevent','23');await q.wait(()=>p().remoteCode===88);
  check(p().keyName==='Netflix'&&!p().button,'confirmed special key is saved without enabling automatic behavior');
  c({op:'activate'});await q.sleep(500);const before=pid();
  c({op:'remote_fixture',type:'key',device:'another device',code:88});
  c({op:'remote_fixture',type:'key',device,code:87});await q.sleep(500);
  check(pid()===before&&p().phase==='idle','device identity and exact function key must both match');
  c({op:'remote_fixture',type:'key',device,code:88});
  await q.wait(()=>p().phase==='idle'&&p().status.startsWith('Canal preparado:'),25000);
  check(pid()!==before,'matching auxiliary callback freshly launches the app and completes the route');
  q.panel(s,'tv_cancel');check(q.panel(s,'tv_remote_stop').includes('status=OK'),'rollback waits for auxiliary stop acknowledgement');
  const stats=q.json(s,'metrics').helper;check(!stats.remote.watcherAlive&&stats.remote.mode==='stop','disabled auxiliary has no input watcher');
 }finally{q.panel(s,'tv_cancel');q.panel(s,'tv_remote_stop');q.panel(s,'tv_restore',original);}
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-teclas-auxiliares-tv-0.18.0-api31.json'),JSON.stringify({api:31,emulator:true,syntheticBinderCallback:true,physicalKernelInput:false,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
