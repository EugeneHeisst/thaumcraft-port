from pathlib import Path
import os, shutil, subprocess, sys, json

project = Path(__file__).resolve().parents[3]
root = Path(os.environ.get('TC4_COMPAT_WORKSPACE', str(project.parent / 'compat-test'))).resolve()
if not (root / 'audit-mod').is_dir():
    raise SystemExit('Set TC4_COMPAT_WORKSPACE to a compatibility workspace containing audit-mod/')
out = Path(os.environ.get('ISSUES_NOV06_OUTPUT', str(root / 'issues-20261106')))
out.mkdir(exist_ok=True)
tc = Path(os.environ.get('TC4_PROJECT_ROOT', str(project))).resolve()
for variant in sys.argv[1:] or ['old', 'new']:
    folder = out / variant
    if folder.exists():
        raise SystemExit(f'Existing evidence must be preserved: {folder}')
    pack = folder / 'pack'
    run = folder / 'run'
    pack.mkdir(parents=True)
    run.mkdir()
    # Exact current production mod set, with only Thaumcraft replaced in the candidate.
    production = Path(os.environ.get('TC4_MOD_PACK', str(root / 'full-client-mods')))
    if not production.is_dir():
        raise SystemExit('Set TC4_MOD_PACK to the compatible mod set directory')
    for source in production.glob('*.jar'):
        if not source.name.startswith('tc4modern-'):
            shutil.copy2(source, pack / source.name)
    jar = tc / ('versions/0.17.191/tc4modern-0.17.191.jar' if variant == 'old' else 'build/libs/tc4modern-0.17.192.jar')
    shutil.copy2(jar, pack / jar.name)
    (run / 'eula.txt').write_text('eula=true\n')
    (run / 'server.properties').write_text('online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\nmax-tick-time=180000\n')
    (run / 'config').mkdir()
    (run / 'config/port-updates.properties').write_text('checkOnStartup=false\n')
    print('RUN', variant, flush=True)
    with (out / (variant + '.log')).open('w') as log:
        result = subprocess.run(['./gradlew', '--offline', '--no-daemon', '--max-workers=2', '-q', '--init-script', str(root / 'issues-nov06-audit.init.gradle'), 'runIssuesNov06', '-PpackDir=' + str(pack), '-PissuesRun=' + str(run), '-PissuesVariant=' + variant, '-PissuesOut=' + str(folder / 'proofs')], cwd=root / 'audit-mod', env={**os.environ, 'JAVA_HOME': '/usr/lib/jvm/java-21-openjdk'}, stdout=log, stderr=subprocess.STDOUT, timeout=240)
    text = (out / (variant + '.log')).read_text(errors='replace')
    print('\n'.join(l for l in text.splitlines() if 'ISSUESNOV06' in l), flush=True)
    print('EXIT', variant, result.returncode, flush=True)
    if result.returncode or 'ISSUESNOV06 DONE ' + variant not in text:
        print(text[-5000:], flush=True)
        sys.exit(1)
