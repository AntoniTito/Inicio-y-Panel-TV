const q=require('./adb-fondos.cjs'),s=process.argv[2];if(!s)throw Error('Explicit device required');
const id=p=>q.command(s,'shell','pidof',p),ticks=pid=>{if(!/^\d+$/.test(pid))throw Error('Invalid PID');const v=q.command(s,'shell','cat','/proc/'+pid+'/stat').split(') ').slice(1).join(') ').split(/\s+/);return +v[11]+ +v[12]};
(async()=>{
 const helper=id('z6panel_bridge'),app=id('local.z6panel'),initial=q.panel(s,'tv_inspect'),stats=q.json(s,'metrics').helper,hz=+q.command(s,'shell','getconf','CLK_TCK');
 const a=ticks(helper),b=ticks(app);await q.sleep(12000);const helperCpuMs=(ticks(helper)-a)*1000/hz,appCpuMs=(ticks(app)-b)*1000/hz,final=q.panel(s,'tv_inspect'),after=q.json(s,'metrics').helper;
 const result={api:+q.command(s,'shell','getprop','ro.build.version.sdk'),seconds:12,helperCpuMs,appCpuMs,helperProcessStable:id('z6panel_bridge')===helper,navigationQueriesDelta:final.navigation.queries-initial.navigation.queries,helperReattachments:after.attachAttempts-stats.attachAttempts,watcherAlive:after.remote.watcherAlive,watchedDevices:after.remote.devices,taskStayedIdle:initial.phase==='idle'&&final.phase==='idle'};
 result.passed=result.helperProcessStable&&result.navigationQueriesDelta===0&&result.helperReattachments===0&&result.watcherAlive&&result.watchedDevices===1&&result.taskStayedIdle&&helperCpuMs<=20;
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-reposo-control-0.18.1-api'+result.api+'.json'),JSON.stringify(result,null,2)+'\n');console.log(result);if(!result.passed)throw Error('Idle acceptance failed');
})().catch(e=>{console.error(e);process.exitCode=1});
