const q=require('./adb-fondos.cjs'),s=process.argv[2],dir=process.argv[3];
if(!s||!dir||q.command(s,'shell','getprop','ro.kernel.qemu')!=='1')throw Error('Emulator and explicit directory containing six disposable carousel fixtures required');
const p=()=>q.panel(s,'inspect'),key=k=>q.panel(s,'key',String(k));
const exp=()=>q.panel(s,'prefs_export').match(/base64=([A-Za-z0-9+/=]+)/)[1];
(async()=>{
 const original=exp(),before=JSON.parse(Buffer.from(original,'base64').toString()),oldCount=before.catalog_snapshot.value.split('|').length;
 const installed=[],checks=[];
 function check(v,name){if(!v)throw Error(name);checks.push(name);console.log('OK '+name);}
 try{
  for(let i=1;i<=6;i++){const pkg='local.z6qa.carousel'+i;const found=q.cp.spawnSync(q.adb,['-s',s,'shell','pm','path',pkg],{encoding:'utf8',windowsHide:true});if(found.stdout.trim())throw Error('Fixture already installed');q.command(s,'install',q.path.join(dir,String(i),'fixture.apk'));installed.push(pkg);}
  await q.wait(()=>JSON.parse(Buffer.from(exp(),'base64').toString()).catalog_snapshot.value.split('|').length===oldCount+6);
  const all=JSON.parse(Buffer.from(exp(),'base64').toString()),pkgs=all.catalog_snapshot.value.split('|').map(x=>x.split(';')[0]);all.hidden_apps={type:'set',value:[]};all.shown_apps={type:'set',value:pkgs};q.panel(s,'prefs_restore',Buffer.from(JSON.stringify(all)).toString('base64'));
  q.command(s,'shell','am','start','-a','android.settings.SETTINGS');await q.wait(()=>!p().homeVisible);key(3);key(20);while(p().bottomSectionIndex!==0)key(22);
  key(21);check(p().bottomSectionIndex===pkgs.length-1,'overflow row wraps to last');
  const last=p().bottomFocusedId.substring(4);check(installed.includes(last),'selected removable fixture');
  await q.sleep(250);q.fs.writeFileSync(q.path.join(q.root,'.local/carrusel-0170-ultimo.png'),q.cp.execFileSync(q.adb,['-s',s,'exec-out','screencap','-p'],{windowsHide:true,maxBuffer:20*1024*1024}));
  key(22);check(p().bottomSectionIndex===0,'overflow row returns directly to first');
  await q.sleep(250);q.fs.writeFileSync(q.path.join(q.root,'.local/carrusel-0170-primero.png'),q.cp.execFileSync(q.adb,['-s',s,'exec-out','screencap','-p'],{windowsHide:true,maxBuffer:20*1024*1024}));
  key(21);q.command(s,'uninstall',last);installed.splice(installed.indexOf(last),1);
  await q.wait(()=>p().bottomAppCount===pkgs.length-1);
  check(p().bottomSection==='apps'&&p().bottomSectionIndex===pkgs.length-2&&p().bottomFocusedId!=='app:'+last,'removal keeps nearest app position');
 }finally{
  for(const pkg of installed)q.command(s,'uninstall',pkg);
  await q.wait(()=>JSON.parse(Buffer.from(exp(),'base64').toString()).catalog_snapshot.value.split('|').length===oldCount);
  q.panel(s,'prefs_restore',original);
 }
 check(exp()===original,'original preferences and catalog restored');
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-catalogo-carrusel-0.17.0-api31.json'),JSON.stringify({api:31,emulator:true,fixtureCount:6,checks},null,2)+'\n');
})().catch(e=>{console.error(e);process.exitCode=1});
