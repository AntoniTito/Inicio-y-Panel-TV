const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Explicit emulator required');
const p=()=>q.json(s,'tv_inspect'),c=o=>q.panel(s,'tv_control',Buffer.from(JSON.stringify(o)).toString('base64'));
const encoded=()=>q.panel(s,'tv_export').match(/base64=([A-Za-z0-9+/=]+)/)[1],pid=()=>q.command(s,'shell','pidof','local.z6qa.tvfixture');
const checks=[],check=(v,name)=>{if(!v)throw Error(name+' '+JSON.stringify(p()));checks.push(name);console.log('OK '+name);};
const tap=(key,scan=0)=>{c({op:'key',key,scan});c({op:'key',key,scan,up:true});};
async function finished(){await q.wait(()=>p().phase==='idle'&&p().status.startsWith('Canal preparado:'),25000);}
(async()=>{
 await q.wait(()=>p().navigationService);const original=encoded();
 try{
  q.command(s,'shell','am','start','-n','local.z6qa.tvfixture/.MainActivity','--es','mode','normal');q.panel(s,'tv_restore',q.fs.readFileSync(q.path.join(q.root,'.local','fixture-0180-verified.base64'),'utf8'));
  c({op:'prepare_key',name:'Netflix'});await q.wait(()=>p().keyPromptReady);check(!p().learningKey,'remote preparation waits for the user before starting timeout');q.command(s,'shell','input','keyevent','23');await q.wait(()=>p().learningKey);tap(289,207);check(!p().learningKey&&p().key===0,'capture does not assign before confirmation');
  q.command(s,'shell','input','keyevent','23');await q.wait(()=>p().key===289&&p().scan===207);check(p().keyName==='Netflix'&&!p().button,'native confirmation saves button identity without enabling it');
  c({op:'activate'});let before=pid();tap(289,208);await q.sleep(700);check(pid()===before&&p().phase==='idle','different scan code retains its original handling');
  tap(289,207);await finished();check(pid()!==before,'branded app key reopens chosen TV app');
  await q.sleep(500);before=pid();c({op:'event',event:'held_button'});await finished();check(pid()!==before,'IR repeated down-up pairs produce one recovery');const once=pid();await q.sleep(900);check(pid()===once,'held button does not queue additional launches');
  c({op:'assign',key:0,scan:404,name:'Otro botón'});c({op:'activate'});before=pid();tap(0,405);check(pid()===before&&p().phase==='idle','unknown Android key requires the learned physical scan code');tap(0,404);await finished();check(pid()!==before,'vendor unknown key can be recognized by scan code');
  let blocked=false;try{c({op:'assign',key:3,scan:999})}catch(e){blocked=true}check(blocked,'scan code cannot bypass reserved Home key');
  c({op:'learn_key',name:'YouTube'});await q.wait(()=>p().learningKey);tap(4);check(!p().learningKey&&p().scan===404,'Back cancels learning without changing assignment');
  c({op:'assign',key:0});check(!p().button&&p().scan===0,'remove assignment restores ordinary handling');
 }finally{c({op:'cancel'});q.panel(s,'tv_restore',original);}
 q.fs.writeFileSync(q.path.join(q.root,'pruebas','resultados-teclas-tv-0.18.0-api31.json'),JSON.stringify({api:31,emulator:true,syntheticKeyEvents:true,physicalRemote:false,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
