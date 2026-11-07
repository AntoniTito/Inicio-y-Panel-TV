const q=require('./adb-fondos.cjs'),s=process.argv[2];if(!s)throw Error('Explicit device required');
const p=()=>q.panel(s,'inspect'),key=k=>q.panel(s,'key',String(k));
(async()=>{
 if(p().visible)q.panel(s,'action','close');q.command(s,'shell','am','start','-a','android.settings.SETTINGS');await q.wait(()=>!p().homeVisible);key(3);key(20);
 const initial=p(),before=q.json(s,'metrics'),moves=17;
 if(!initial.bottomAppCount||initial.editMode)throw Error('Requires visible apps outside editing');
 q.command(s,'shell','input','dpad','keyevent',...Array(moves).fill('22'));
 const expected=(initial.bottomSectionIndex+moves)%initial.bottomAppCount;
 await q.sleep(500);
 const after=q.json(s,'metrics'),last=p(),delivered=after.focusUpdates>before.focusUpdates,passed=delivered&&last.bottomSectionIndex===expected&&last.bottomSection==='apps'&&before.fullRenders===after.fullRenders&&before.catalogScans===after.catalogScans;
 key(4);
 const api=+q.command(s,'shell','getprop','ro.build.version.sdk');
 q.fs.writeFileSync(q.path.join(q.root,'pruebas/resultados-rafaga-panel-0.17.0-api'+api+'.json'),JSON.stringify({api,emulator:q.command(s,'shell','getprop','ro.kernel.qemu')==='1',moves,method:'Native input dpad keyevent with 17 right presses in a single process; protected bridge used only to prepare and inspect',initialIndex:initial.bottomSectionIndex,expectedIndex:expected,actualIndex:last.bottomSectionIndex,focusUpdates:after.focusUpdates-before.focusUpdates,status:!delivered?'injected_keys_not_delivered_to_overlay':passed?'passed':'failed',passed:delivered?passed:null},null,2)+'\n');if(delivered&&!passed)throw Error('Burst rebuilt UI, changed row or missed keys');console.log(delivered?'Native burst passed':'Native injected events did not reach overlay; physical-remote test required');
})().catch(e=>{console.error(e);process.exitCode=1});
