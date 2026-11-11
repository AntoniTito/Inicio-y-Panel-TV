const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const p=()=>q.json(s,'tv_inspect'),c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const encoded=()=>q.panel(s,'tv_export').match(/base64=([A-Za-z0-9+/=]+)/)[1],pid=()=>q.command(s,'shell','pidof','local.z6qa.tvfixture');
const checks=[],check=(v,name)=>{if(!v)throw Error(name+' '+JSON.stringify(p()));checks.push(name);console.log('OK '+name);};
const quiet=(...args)=>q.cp.execFileSync(q.adb,['-s',s,...args],{encoding:'utf8',windowsHide:true,timeout:3000,stdio:['ignore','pipe','pipe']}).trim();
function helper(){const apk=q.command(s,'shell','pm','path','local.z6panel').replace(/^package:/,'');if(!/^\/[A-Za-z0-9_./=+~\-]+$/.test(apk))throw Error('Invalid APK path');const uid=q.command(s,'shell','dumpsys','package','local.z6panel').match(/userId=(\d+)/)[1];q.command(s,'shell',"for p in $(pidof z6panel_bridge); do kill $p; done; CLASSPATH='"+apk+"' nohup app_process /system/bin --nice-name=z6panel_bridge local.z6bridge.BridgeMain "+uid+" >/data/local/tmp/z6-bridge.log 2>&1 </dev/null &");}
(async()=>{
 await q.wait(()=>p().navigationService);const original=encoded(),services=q.command(s,'shell','settings','get','secure','enabled_accessibility_services');let assisted=false,restored=false;q.fs.writeFileSync(q.path.join(q.root,'.local/ciclo-tv-0180-original.json'),JSON.stringify({tv:original,services}));
 try{
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','list');q.panel(s,'tv_restore',q.fs.readFileSync(q.path.join(q.root,'.local','fixture-0180-list-verified.base64'),'utf8'));c({op:'activate'});
  const boot=+q.command(s,'shell','settings','get','global','boot_count');q.command(s,'reboot');
  await q.wait(()=>+quiet('shell','settings','get','global','boot_count')>boot&&quiet('shell','getprop','sys.boot_completed')==='1',180000);await q.wait(()=>p().navigationService,60000);
  if(!q.panel(s,'bridge','PING').includes('status=OK')){assisted=true;helper();}
  await q.wait(()=>p().phase==='idle'&&p().reason==='arranque'&&p().status.startsWith('Canal preparado:'),80000);
  check(+q.command(s,'shell','settings','get','global','boot_count')>boot,'real emulator reboot advances Android boot count');
  check(p().verified&&p().boot&&p().wake,'boot preserves route and independent switches');let before=pid();
  c({op:'event',event:'boot'});await q.sleep(1600);check(pid()===before,'boot broadcast duplicate does not reopen app');
  q.command(s,'shell','input','keyevent','223');await q.wait(()=>/mWakefulness=(Asleep|Dozing)/.test(q.command(s,'shell','dumpsys','power')),10000);
  q.command(s,'shell','input','keyevent','224');await q.wait(()=>p().phase==='idle'&&p().reason==='despertar'&&p().status.startsWith('Canal preparado:'),60000);
  check(pid()!==before,'actual Android sleep-wake freshly reopens app and selects destination');before=pid();
  q.command(s,'shell','am','force-stop','local.z6panel');q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity');q.command(s,'shell','settings','put','secure','enabled_accessibility_services','null');q.command(s,'shell','settings','put','secure','enabled_accessibility_services',services);
  await q.wait(()=>p().navigationService,30000);await q.sleep(2200);check(p().phase==='idle'&&pid()===before,'PC-assisted panel process recreation does not duplicate boot recovery');
 }finally{let online=false;try{online=quiet('get-state')==='device'}catch(ignored){}if(online){q.command(s,'shell','settings','put','secure','enabled_accessibility_services',services);await q.wait(()=>p(),30000);c({op:'cancel'});q.panel(s,'tv_restore',original);restored=true;}else console.error('Emulator unavailable; original state saved in .local/ciclo-tv-0180-original.json');}
 if(!restored)throw Error('Emulator restoration pending');
 q.fs.writeFileSync(q.path.join(q.root,'pruebas','resultados-ciclo-tv-0.18.0-api31.json'),JSON.stringify({api:31,emulator:true,fixture:true,actualVideo:false,coldBootHelperPcAssisted:assisted,processRebindPcAssisted:true,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
