#!/usr/bin/env python3
"""Place this checkout's visible audit window between the user's pinned panel popups.

KWin uses actual screen coordinates; XWayland wmctrl/xdotool coordinates are scaled differently.
Never selects a Prism window: verify the Minecraft process's working directory first.
"""
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time

os.environ.setdefault('DISPLAY', ':1')
root = Path(__file__).resolve().parents[1]
for attempt in range(55):
    rows = subprocess.check_output(['wmctrl', '-l'], text=True).splitlines()
    for row in rows:
        if 'Minecraft NeoForge* 1.21.1' not in row:
            continue
        wid = row.split()[0]
        try:
            prop = subprocess.check_output(['xprop', '-id', wid, '_NET_WM_PID'], text=True, stderr=subprocess.DEVNULL)
            pid = int(re.search(r'=\s*(\d+)', prop).group(1))
            cwd = Path(f'/proc/{pid}/cwd').resolve()
        except (OSError, AttributeError, subprocess.CalledProcessError):
            continue
        if cwd != root / 'run-focal-audit':
            continue
        script = ('for (const w of workspace.windowList()) { if (w.pid === ' + str(pid) + ') {'
                  ' const g=w.frameGeometry; w.frameGeometry={x:540,y:140,width:g.width,height:g.height}; } }')
        with tempfile.NamedTemporaryFile(mode='w', suffix='.js', prefix='tc4-audit-position-', delete=False) as file:
            file.write(script)
            path = Path(file.name)
        try:
            subprocess.run(['qdbus6', 'org.kde.KWin', '/Scripting', 'org.kde.kwin.Scripting.unloadScript', 'tc4-audit-position'], stdout=subprocess.DEVNULL)
            sid = subprocess.check_output(['qdbus6', 'org.kde.KWin', '/Scripting', 'org.kde.kwin.Scripting.loadScript', str(path), 'tc4-audit-position'], text=True).strip()
            subprocess.run(['qdbus6', 'org.kde.KWin', '/Scripting/Script' + sid, 'org.kde.kwin.Script.run'], check=True)
            print(f'Placed own audit PID {pid} at native KWin x540 y140')
        finally:
            path.unlink(missing_ok=True)
        raise SystemExit(0)
    time.sleep(1)
raise SystemExit('No window from this checkout appeared within 55 seconds')
