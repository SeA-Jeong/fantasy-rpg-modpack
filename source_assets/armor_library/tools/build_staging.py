#!/usr/bin/env python3
"""Generate class-grade review folders. Never overwrite game assets."""
from pathlib import Path
import json,csv,shutil
R=Path(__file__).resolve().parents[1]
AS=json.loads((R/'assignment.json').read_text(encoding='utf-8'))
M=json.loads((R/'model_candidates.json').read_text(encoding='utf-8'))
base=R/'upstream'/'morearmor'/'src/main/resources/assets/morearmor/textures'
if not base.exists():raise SystemExit('Run python tools/fetch_upstream.py --source primary first')
count=0
for a in AS:
    cl,tier,slug=a['class'],a['tier'],a['morearmor_asset']
    out=R/'staging'/cl/f'T{tier:02}'
    for kind,prefix in [('icon',base/'item'/'averagetateman'/'armors'/slug),('armor_layer',base/'models'/'averagetateman'/'armors'/slug)]:
        if prefix.exists():
            for src in prefix.glob('*'):
                if src.is_file():
                    dst=out/kind/src.name;dst.parent.mkdir(parents=True,exist_ok=True)
                    shutil.copy2(src,dst);count+=1
    (out/'SOURCE.json').write_text(json.dumps(a,ensure_ascii=False,indent=2),encoding='utf-8')
for m in M:
    src=R/'upstream'/'cleann'/'bb'/m['file']
    if src.exists():
        dst=R/'staging'/m['class']/f'T{m["tier"]:02}'/'bbmodel_candidate'/m['file']
        dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst);count+=1
print('Staged',count,'copies; models are UNCONVERTED, not game-ready.')
