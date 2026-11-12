const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const p=()=>q.json(s,'tv_inspect'),c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const read=m=>q.panel(s,m).match(/base64=([A-Za-z0-9+/=]+)/)[1];
const checks=[],check=(v,label)=>{if(!v)throw Error(label+' '+JSON.stringify(p()));checks.push(label);console.log('OK '+label);};
(async()=>{
 const original=read('tv_export'),buttons=read('tv_buttons_export'),services=q.command(s,'shell','settings','get','secure','enabled_accessibility_services');
 const usage=q.command(s,'shell','cmd','appops','get','local.z6panel','GET_USAGE_STATS').match(/GET_USAGE_STATS:\s*(allow|default|ignore|deny)/)?.[1]||'default';
 let target;
 const base=services.split(':').filter(x=>x&&!x.includes('TvNavigationService')&&x!=='null').join(':'),nav='local.z6panel/local.z6panel.TvNavigationService';
 try{
  q.command(s,'shell','cmd','appops','set','local.z6panel','GET_USAGE_STATS','allow');
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','normal');
  c({op:'select',package:'local.z6qa.tvfixture'});target=JSON.parse(Buffer.from(read('tv_export'),'base64'));
  const selector=(id,text,type='android.widget.Button')=>({id:'local.z6qa.tvfixture:id/'+id,text,description:'',type,hint:''});
  target.steps=[['television','TELEVISIÓN'],['peru','PERÚ'],['channel','TV PERÚ']].map(([id,label])=>({op:'click',activity:'local.z6qa.tvfixture.MainActivity',target:selector(id,label)}));
  target.endpoint={activity:'local.z6qa.tvfixture.MainActivity',marker:selector('playing','TV Perú · reproducción de prueba','android.widget.TextView')};
  target.channel='TV PERÚ';target.verified=true;target.boot=true;target.wake=false;target.button=false;
  q.panel(s,'tv_restore',Buffer.from(JSON.stringify(target)).toString('base64'));
  q.command(s,'shell','settings','put','secure','enabled_accessibility_services',base);await q.wait(()=>!p().navigationService);
  c({op:'event',event:'boot_waiting'});check(p().phase==='waiting','boot accepted while navigation service is not connected');
  await q.sleep(1200);check(p().phase==='waiting','boot remains pending instead of consuming the request');
  q.command(s,'shell','settings','put','secure','enabled_accessibility_services',base+':'+nav);
  await q.wait(()=>p().phase==='idle'&&/^(Canal|Destino) preparado:/.test(p().status),45000);check(p().step===3,'late navigation service resumes the same boot request');
  const before=q.command(s,'shell','pidof','local.z6qa.tvfixture');
  q.command(s,'shell','input','keyevent','223');await q.sleep(350);c({op:'event',event:'boot_waiting'});
  check(p().phase==='waiting','automatic boot waits while display is off');
  check(q.command(s,'shell','pidof','local.z6qa.tvfixture')===before,'no target app restart before wake');
  q.command(s,'shell','input','keyevent','224');await q.wait(()=>p().phase==='idle'&&/^(Canal|Destino) preparado:/.test(p().status),45000);
  check(p().step===3,'screen-on event resumes automatic preparation');
  const after=q.command(s,'shell','pidof','local.z6qa.tvfixture');c({op:'event',event:'boot'});await q.sleep(700);check(q.command(s,'shell','pidof','local.z6qa.tvfixture')===after,'duplicate boot does not restart the app twice');
 }finally{q.command(s,'shell','input','keyevent','224');c({op:'cancel'});q.panel(s,'tv_restore',original);q.panel(s,'tv_buttons_restore',buttons);q.command(s,'shell','settings','put','secure','enabled_accessibility_services',services);q.command(s,'shell','cmd','appops','set','local.z6panel','GET_USAGE_STATS',usage);}
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-arranque-mando-0.19.0-api31.json'),JSON.stringify({api:31,emulator:true,syntheticBootOrdering:true,actualPhysicalReboot:false,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
