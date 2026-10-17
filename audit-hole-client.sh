#!/usr/bin/env bash
# Isolated, hidden and silent Portable Hole rendering regression audit.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk ALSOFT_DRIVERS=null
mkdir -p run-hole-render-audit
if [[ ! -d run-hole-render-audit/saves/HoleRenderInspection ]]; then
    mkdir -p run-hole-render-audit/saves
    cp -a run-focal-audit/saves/FocalInspection run-hole-render-audit/saves/HoleRenderInspection
fi
if [[ ! -f run-hole-render-audit/options.txt ]]; then
    printf 'onboardAccessibility:false\ntutorialStep:none\n' > run-hole-render-audit/options.txt
fi
sed -i -E '/^soundCategory_/d' run-hole-render-audit/options.txt
for category in master music record weather block hostile neutral player ambient voice; do
    echo "soundCategory_${category}:0.0" >> run-hole-render-audit/options.txt
done
exec timeout 420 xvfb-run -a -s '-screen 0 1280x960x24' ./dev.sh runHoleRenderAudit \
    -PjeiLocalJar=/home/eugheist/.local/share/PrismLauncher/instances/1.21.1/minecraft/mods/jei-1.21.1-neoforge-19.57.0.451.jar "$@"
