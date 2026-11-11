const q=require('./adb-fondos.cjs'),s=process.argv[2];if(!s)throw Error('Explicit device required');
(async()=>{
 const initial=q.panel(s,'tv_inspect');if(initial.phase!=='idle'||!initial.remoteInput.helperConnected)throw Error('Idle connected helper required');
 const pid=q.command(s,'shell','pidof','z6panel_bridge');
 if(!/^\d+$/.test(pid)||!/^Uid:\s+2000\s/m.test(q.command(s,'shell','cat','/proc/'+pid+'/status')))throw Error('Helper identity mismatch');
 const adb=q.command(s,'shell','settings','get','global','adb_enabled'),started=Date.now();
 q.command(s,'shell','kill','-9',pid);const lost=q.panel(s,'tv_inspect').remoteInput;
 await q.wait(()=>q.json(s,'metrics').helper?.remote?.watcherAlive,45000);
 const final=q.panel(s,'tv_inspect'),result={api:+q.command(s,'shell','getprop','ro.build.version.sdk'),physical:q.command(s,'shell','getprop','ro.kernel.qemu')!=='1',version:'0.18.1',helperOnlyKilled:true,pcStartedHelper:false,helperProcessChanged:pid!==q.command(s,'shell','pidof','z6panel_bridge'),recoveryMs:Date.now()-started,disconnectedStatusObserved:!lost.helperConnected&&lost.status!=='OK',remoteReaderRestored:final.remoteInput.helperConnected&&final.remoteInput.status==='OK',adbEnabledUnchanged:adb===q.command(s,'shell','settings','get','global','adb_enabled')};
 result.passed=result.helperProcessChanged&&result.disconnectedStatusObserved&&result.remoteReaderRestored&&result.adbEnabledUnchanged;
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-recuperacion-auxiliar-0.18.1-api'+result.api+'.json'),JSON.stringify(result,null,2)+'\n');console.log(result);if(!result.passed)throw Error('Helper recovery failed');
})().catch(e=>{console.error(e);process.exitCode=1});
