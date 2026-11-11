const q=require('./adb-fondos.cjs'),s=process.argv[2],phase=process.argv[3]||'despues';if(!['antes','despues'].includes(phase))throw Error('Explicit phase required');if(!s)throw Error('Explicit device required');
const p=()=>q.panel(s,'inspect'),key=k=>q.panel(s,'key',String(k));
function sample(){const m=q.command(s,'shell','dumpsys','meminfo','local.z6panel');const total=+(m.match(/TOTAL PSS:\s*(\d+)/)||m.match(/TOTAL\s+(\d+)/)||[])[1];if(!total)throw Error('Missing PSS');return total;}
(async()=>{
 if(p().visible)q.panel(s,'action','close');if(p().homeVisible)q.command(s,'shell','am','start','-a','android.settings.SETTINGS');await q.wait(()=>!p().homeVisible);
 const cycle=async()=>{key(3);await q.sleep(100);key(4);await q.sleep(100);};
 for(let i=0;i<5;i++)await cycle();await q.sleep(1000);
 const pid=q.command(s,'shell','pidof','local.z6panel'),before=sample(),samples=[];
 for(let i=0;i<20;i++){await cycle();if((i+1)%5===0){await q.sleep(500);samples.push({cycle:i+1,pssKB:sample()});console.log(JSON.stringify(samples.at(-1)));}}
 const growthKB=samples.at(-1).pssKB-before,processStable=pid===q.command(s,'shell','pidof','local.z6panel');
 const version=q.command(s,'shell','dumpsys','package','local.z6panel').match(/versionName=([^\s]+)/)[1];const api=+q.command(s,'shell','getprop','ro.build.version.sdk'),result={phase,version,api,emulator:q.command(s,'shell','getprop','ro.kernel.qemu')==='1',warmupCycles:5,cycles:20,beforePssKB:before,samples,growthKB,processStable,passed:growthKB<8192&&processStable};
 q.fs.writeFileSync(q.path.join(q.root,'pruebas','resultados-memoria-panel-0.18.0-'+phase+'-api'+api+'.json'),JSON.stringify(result,null,2)+'\n');if(!result.passed)throw Error('Memory acceptance failed '+JSON.stringify(result));
})().catch(e=>{console.error(e);process.exitCode=1});
