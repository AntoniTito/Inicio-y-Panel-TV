const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const pkg='local.z6qa.tvfixture',component=pkg+'/.MainActivity';
const p=()=>q.json(s,'tv_inspect'),control=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const checks=[],check=(v,name)=>{if(!v)throw Error(name+' '+JSON.stringify(p()));checks.push(name);console.log('OK '+name);};
const exportTv=()=>q.panel(s,'tv_export').match(/base64=([A-Za-z0-9+/=]+)/)[1];
const pid=()=>q.command(s,'shell','pidof',pkg);
async function normal(){q.command(s,'shell','am','start','-n',component,'--es','mode','normal');await q.sleep(350);}
async function choose(part){const row=await q.wait(()=>p().navigation.controls.find(x=>x.id.includes(part)),15000);control({op:'choose',id:row.id});await q.sleep(600);}
async function finished(){await q.wait(()=>p().phase==='idle',15000);check(p().status.startsWith('Canal preparado:'),'verified endpoint after fresh launch');}
(async()=>{
 await q.wait(()=>p().navigationService);const original=exportTv();
 try{
  await normal();control({op:'select',package:pkg});let before=pid();control({op:'teach'});
  for(const part of ['/television|','/peru|','/channel|'])await choose(part);
  await q.sleep(800);control({op:'choose',id:'done'});await q.wait(()=>p().phase==='confirming',15000);
  check(pid()!==before,'teaching replays from a new process');check(!p().verified,'route not saved before playback confirmation');
  control({op:'choose',id:'confirm_yes'});check(p().verified&&p().steps===3,'confirmed generic route saved');
  const good=exportTv();before=pid();control({op:'run'});await finished();check(pid()!==before,'foreground app force-stopped and freshly launched');
  q.command(s,'shell','am','start','-a','android.settings.SETTINGS');before=pid();control({op:'run'});await finished();check(pid()!==before,'background app freshly launched');
  q.command(s,'shell','am','force-stop',pkg);control({op:'run'});await finished();check(!!pid(),'closed app launches correctly');
  let rejected=false;try{control({op:'select',package:'local.z6panel'})}catch(e){rejected=true}check(rejected&&p().package===pkg,'panel protected from restart selection');
  rejected=false;try{control({op:'assign',key:3})}catch(e){rejected=true}check(rejected,'Home cannot be assigned');
  control({op:'assign',key:183});control({op:'activate'});check(p().boot&&p().wake&&p().button,'independent boot wake and button settings available');
  before=pid();control({op:'key',key:183});for(let i=1;i<=8;i++)control({op:'key',key:183,repeat:i});control({op:'key',key:183,up:true});await finished();const once=pid();check(once!==before,'shortcut performs fresh restart');await q.sleep(1400);check(pid()===once,'held shortcut does not queue restarts');
  control({op:'run'});control({op:'key',key:4});control({op:'key',key:4,up:true});check(p().phase==='idle'&&!p().navigation.active,'Back cancels active recovery');
  control({op:'run'});control({op:'key',key:3});control({op:'key',key:3,up:true});check(p().phase==='idle','Home cancels without remapping Home');
  control({op:'event',event:'off'});control({op:'event',event:'on'});control({op:'event',event:'on'});await q.wait(()=>p().phase!=='idle',6000);await finished();const awakened=pid();await q.sleep(2000);check(pid()===awakened,'duplicate wake event does not restart twice');
  control({op:'event',event:'boot'});await q.sleep(1500);check(pid()===awakened,'duplicate boot event in same boot is ignored');
  control({op:'event',event:'wake_cancel'});await q.sleep(2200);check(pid()===awakened&&!p().wake&&!p().screenListener,'disable cancels a pending wake before its delayed callback');
  for(const mode of ['login','inaccessible','changed','ambiguous']){
   q.panel(s,'tv_restore',good);q.command(s,'shell','am','start','-n',component,'--es','mode',mode);control({op:'run'});await q.sleep(2300);
   const state=p();check(!JSON.stringify(state).includes('secret-fixture-never-record'),'password excluded in '+mode);
   check(state.phase==='navigating'||state.phase==='idle','unavailable route stays bounded in '+mode);
   control({op:'cancel'});check(p().phase==='idle'&&!p().navigation.overlay,'cancel clears UI and work in '+mode);
  }
  await normal();q.panel(s,'tv_restore',good);control({op:'run'});await finished();
  const queryCount=p().navigation.queries;await q.sleep(2200);check(queryCount===p().navigation.queries,'idle service makes no UI queries');
  q.fs.writeFileSync(q.path.join(q.root,'.local','fixture-0180-verified.base64'),good);
 }finally{control({op:'cancel'});q.panel(s,'tv_restore',original);}
 q.fs.writeFileSync(q.path.join(q.root,'pruebas','resultados-recuperacion-tv-0.18.0-api31.json'),JSON.stringify({api:31,emulator:true,fixture:true,actualVideo:false,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
