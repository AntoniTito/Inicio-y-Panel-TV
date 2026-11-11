const q=require('./adb-fondos.cjs'),s=process.argv[2];
if(!s||q.command(s,'shell','getprop','ro.kernel.qemu')==='1')throw Error('Explicit physical TV required');
const p=()=>q.panel(s,'tv_inspect');
(async()=>{
 const initial=p();if(initial.phase!=='idle'||!initial.verified||!initial.wake)throw Error('Configured idle TV with wake enabled required');
 const boot=q.command(s,'shell','settings','get','global','boot_count'),pid=q.command(s,'shell','pidof',initial.package);
 q.command(s,'shell','input','keyevent','223');
 await q.wait(()=>/mWakefulness=(Asleep|Dozing)/.test(q.command(s,'shell','dumpsys','power')),10000);
 await q.sleep(2000);q.command(s,'shell','input','keyevent','224');
 await q.wait(()=>p().reason==='despertar'&&p().phase!=='idle',20000);
 await q.wait(()=>p().phase==='idle',120000);const final=p();
 const result={api:+q.command(s,'shell','getprop','ro.build.version.sdk'),physical:true,androidPowerEvents:true,tvHdmiCecTested:false,helperPcStarted:false,bootUnchanged:boot===q.command(s,'shell','settings','get','global','boot_count'),freshProcess:pid!==q.command(s,'shell','pidof',initial.package),channelReached:final.status.startsWith('Canal preparado:'),automaticRetried:final.automaticRetried};
 result.passed=result.bootUnchanged&&result.freshProcess&&result.channelReached;
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-despertar-tv-0.18.0-api29.json'),JSON.stringify(result,null,2)+'\n');
 console.log(JSON.stringify(result));if(!result.passed)throw Error('Wake acceptance failed');
})().catch(e=>{console.error(e);process.exitCode=1});
