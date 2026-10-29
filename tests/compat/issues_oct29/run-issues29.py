from pathlib import Path
import os,shutil,subprocess,sys
r=Path(os.environ.get('TC4_COMPAT_WORKSPACE',Path(__file__).resolve().parents[4]/'compat-test'));out=r/'issues-20261029';out.mkdir(exist_ok=True)
jars={
'old': [r.parent/'thaumcraft4-modern/versions/0.17.190/tc4modern-0.17.190.jar',r.parent/'thaumic-tinkerer-modern/versions/0.1.13/thaumic-tinkerer-modern-0.1.13.jar',r.parent/'forbidden-magic-modern/versions/0.1.2/forbidden-magic-0.1.2.jar'],
'new': [r.parent/'thaumcraft4-modern/build/libs/tc4modern-0.17.191.jar',r.parent/'thaumic-tinkerer-issues-oct29/build/libs/thaumic-tinkerer-modern-0.1.14.jar',r.parent/'forbidden-magic-modern/build/libs/forbidden-magic-0.1.3.jar']}
for variant in sys.argv[1:] or ['old','new']:
 folder=out/variant;shutil.rmtree(folder,ignore_errors=True);pack=folder/'pack';run=folder/'run';pack.mkdir(parents=True);(run/'saves').mkdir(parents=True);(run/'config').mkdir()
 for f in (r/'sodium-visual/pack-vanilla').glob('*.jar'):
  if not f.name.startswith(('tc4modern','thaumic-tinkerer','forbidden-magic')):shutil.copy2(f,pack/f.name)
 for f in jars[variant]:shutil.copy2(f,pack/f.name)
 shutil.copytree(r.parent/'thaumcraft4-modern/run-focal-audit/saves/FocalInspection',run/'saves/Showcase');(run/'saves/Showcase/session.lock').unlink(missing_ok=True)
 (run/'config/port-updates.properties').write_text('checkOnStartup=false\n')
 (run/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nlang:ru_ru\nmaxFps:60\nenableVsync:false\nrenderDistance:6\nguiScale:2\n'+''.join('soundCategory_'+x+':0.0\n' for x in ['master','music','record','weather','block','hostile','neutral','player','ambient','voice']))
 print('RUN',variant,flush=True)
 with (out/(variant+'.log')).open('w') as log:
  x=subprocess.run(['xvfb-run','-a','-s','-screen 0 1280x960x24','timeout','420','./gradlew','--offline','--no-daemon','--max-workers=2','-q','--init-script',str(r/'issues29-audit.init.gradle'),'runIssues29','-PpackDir='+str(pack),'-PissuesRun='+str(run),'-PissuesVariant='+variant,'-PissuesOut='+str(folder/'proofs')],cwd=r/'audit-mod',env={**os.environ,'JAVA_HOME':'/usr/lib/jvm/java-21-openjdk','ALSOFT_DRIVERS':'null'},stdout=log,stderr=subprocess.STDOUT)
 text=(out/(variant+'.log')).read_text(errors='replace');print('\n'.join(l for l in text.splitlines() if 'ISSUES29' in l),flush=True);print('EXIT',variant,x.returncode,flush=True)
 if x.returncode or 'ISSUES29 DONE '+variant not in text:sys.exit(1)
