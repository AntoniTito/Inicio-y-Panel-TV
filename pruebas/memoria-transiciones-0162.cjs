const q=require('./adb-fondos.cjs'),s=process.argv[2];if(!s)throw Error('Explicit ADB device required');
const samples=[];const pss=()=>{const r=q.command(s,'shell','dumpsys','meminfo','local.z6panel');const m=r.match(/TOTAL PSS:\s*(\d+)/)||r.match(/^\s*TOTAL\s+(\d+)/m);if(!m)throw Error('No PSS');return +m[1]};
(async()=>{for(let i=1;i<=20;i++){
 q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity');await q.wait(()=>q.panel(s,'home_inspect').wallpaper.loaded);
 q.panel(s,'open','settings');q.panel(s,'action','personalize');q.panel(s,'action','close');q.panel(s,'open','settings');q.panel(s,'action','android');
 await q.wait(()=>!q.panel(s,'home_inspect').wallpaper.active);
 q.command(s,'shell','am','start','-n','local.z6panel/.HomeActivity');await q.wait(()=>q.panel(s,'home_inspect').wallpaper.loaded);await q.sleep(150);
 if(i%5===0){samples.push({cycle:i,pssKb:pss()});console.log(JSON.stringify(samples.at(-1)))}
 }const growth=samples.at(-1).pssKb-samples[1].pssKb,report={version:'0.16.2',api:+q.command(s,'shell','getprop','ro.build.version.sdk'),emulator:q.command(s,'shell','getprop','ro.kernel.qemu')==='1',cycles:20,samples,growthAfterWarmupKb:growth,limitKb:8192,passed:growth<=8192};q.fs.writeFileSync(q.path.join(q.root,'pruebas','resultados-memoria-transiciones-0.16.2-api'+report.api+'.json'),JSON.stringify(report,null,2)+'\n');if(!report.passed)throw Error('Memory growth exceeded 8 MB');})().catch(e=>{console.error(e);process.exitCode=1});
