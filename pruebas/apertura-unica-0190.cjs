const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const p=()=>q.json(s,'tv_inspect'),c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const read=m=>q.panel(s,m).match(/base64=([A-Za-z0-9+/=]+)/)[1];
const checks=[],check=(v,n)=>{if(!v)throw Error(n+' '+JSON.stringify(p()));checks.push(n);console.log('OK '+n);};
const selector=(id,text,type='android.widget.Button',description='')=>({id:'local.z6qa.tvfixture:id/'+id,text,description,type,hint:''});
const named={activity:'local.z6qa.tvfixture.MainActivity',marker:selector('playing','TV Perú · reproducción de prueba','android.widget.TextView')};
(async()=>{
 const original=read('tv_export'),services=q.command(s,'shell','settings','get','secure','enabled_accessibility_services');
 const usage=q.command(s,'shell','cmd','appops','get','local.z6panel','GET_USAGE_STATS').match(/GET_USAGE_STATS:\s*(allow|default|deny|ignore)/)?.[1]||'default';
 try{
  const all=services.split(':').filter(x=>x&&x!=='null');if(!all.some(x=>x.endsWith('TvNavigationService')))all.push('local.z6panel/local.z6panel.TvNavigationService');
  q.command(s,'shell','settings','put','secure','enabled_accessibility_services',all.join(':'));q.command(s,'shell','cmd','appops','set','local.z6panel','GET_USAGE_STATS','allow');await q.wait(()=>p().navigationService);
  c({op:'select',package:'local.z6qa.tvfixture'});const target=JSON.parse(Buffer.from(read('tv_export'),'base64'));
  target.steps=[['television','TELEVISIÓN'],['peru','PERÚ'],['channel','TV PERÚ']].map(([id,label])=>({op:'click',activity:'local.z6qa.tvfixture.MainActivity',target:selector(id,label)}));
  target.channel='TV PERÚ';target.verified=true;target.boot=true;target.wake=false;target.button=false;target.endpoint=named;
  for(const mode of ['delayed','advanced','early-player','other-player']){
   q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode',mode,'--ez','stage_only','true');q.command(s,'shell','am','force-stop','local.z6qa.tvfixture');
   target.endpoint=mode==='other-player'?{marker:selector('player','','android.view.SurfaceView','Reproductor de prueba')}:named;
   q.panel(s,'tv_restore',Buffer.from(JSON.stringify(target)).toString('base64'));c({op:'event',event:'boot_waiting'});
   await q.wait(()=>p().phase==='navigating',15000);const pid=q.command(s,'shell','pidof','local.z6qa.tvfixture');
   if(mode==='other-player'){
    await q.sleep(11000);check(p().phase==='navigating'&&p().step===0,'generic video surface never claims the chosen channel');
    await q.wait(()=>p().phase==='idle',65000);check(!p().status.startsWith('Canal preparado:')&&q.command(s,'shell','pidof','local.z6qa.tvfixture')===pid,'unverified player remains open after timeout');
   }else{await q.wait(()=>p().phase==='idle',45000);check(p().status.startsWith('Canal preparado:'),'correct endpoint '+mode);}
   check(p().appLaunches===1&&!p().automaticRetried&&q.command(s,'shell','pidof','local.z6qa.tvfixture')===pid,'one process launch without reopening '+mode);
   if(mode==='advanced')check(p().routeResumes===1,'already exposed exact destination skips missing intermediate menu');
   if(mode==='early-player')check(p().routeResumes===0,'named playing endpoint needs no extra menu clicks');
  }
 }finally{c({op:'cancel'});q.panel(s,'tv_restore',original);q.command(s,'shell','settings','put','secure','enabled_accessibility_services',services);q.command(s,'shell','cmd','appops','set','local.z6panel','GET_USAGE_STATS',usage);}
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-apertura-unica-0.19.0-api31.json'),JSON.stringify({api:31,emulator:true,actualVideo:false,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
