#!/usr/bin/env python3
"""Download approved original source assets pinned to known GitHub commits.
Downloads NEVER touch Minecraft source paths. Python 3.10+, standard library only.
"""
from pathlib import Path
import argparse,json,urllib.request,urllib.error,urllib.parse,time,csv,hashlib
ROOT=Path(__file__).resolve().parents[1]
REG=json.loads((ROOT/'source_registry.json').read_text(encoding='utf-8'))
AS=json.loads((ROOT/'assignment.json').read_text(encoding='utf-8'))
MODELS=json.loads((ROOT/'model_candidates.json').read_text(encoding='utf-8'))
EXTRA=json.loads((ROOT/'accessories.json').read_text(encoding='utf-8'))

def request(url):
    req=urllib.request.Request(url,headers={'User-Agent':'FantasyRPG-SourceAssetAudit/1.0','Accept':'application/vnd.github+json'})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req,timeout=30) as r:return r.read()
        except (urllib.error.URLError,TimeoutError) as e:
            if attempt==2: raise
            time.sleep(2+attempt)

LOCAL={}  # source -> 같은 커밋의 git 클론 폴더 (--local; raw·API 가 막힌 환경용, 2026-11-23 추가)

def git(d,*a):
    import subprocess
    return subprocess.run(['git','-C',str(d),*a],check=True,capture_output=True).stdout

def tree(source):
    r=REG[source]
    if source in LOCAL:
        d=LOCAL[source]
        git(d,'cat-file','-e',r['sha']+'^{commit}')  # 고정 커밋이 클론에 있어야 함
        return {p for p in git(d,'ls-tree','-r','--name-only',r['sha']).decode('utf-8').splitlines()}
    url=f'https://api.github.com/repos/{r["repo"]}/git/trees/{r["sha"]}?recursive=1'
    obj=json.loads(request(url))
    if obj.get('truncated') or not obj.get('tree'):raise RuntimeError('GitHub tree missing/truncated: '+source)
    return {i['path'] for i in obj['tree'] if i.get('type')=='blob'}

def requested(source,available):
    if source=='morearmor':
        wanted=set()
        for a in AS:
            slug=a['morearmor_asset']
            prefix=f'src/main/resources/assets/morearmor/textures/item/averagetateman/armors/{slug}/'
            modelprefix=f'src/main/resources/assets/morearmor/textures/models/averagetateman/armors/{slug}/'
            found={p for p in available if (p.startswith(prefix) or p.startswith(modelprefix)) and p.lower().endswith(('.png','.png.mcmeta','.json'))}
            icons=[p for p in found if p.startswith(prefix) and p.endswith('.png') and Path(p).stem in ['helmet','chestplate','leggings','boots']]
            if len(icons)!=4:print('[WARN] missing icons',slug,icons)
            wanted.update(found)
        wanted.update(p for p in ['LICENSE','README.md'] if p in available)
        return wanted
    if source=='cleann':
        files={'bb/'+m['file'] for m in MODELS}
        files.update({'bb/README.humanoid_armor.txt','bb/README.humanoid_animations.txt','LICENSE','README.md'})
        return files & available
    if source=='loy':return (set(EXTRA)|{'LICENSE','README.md','LICENCE'}) & available
    if source=='french':
        # Optional source only; do not blindly import every model into armor system.
        return {p for p in available if p.lower().endswith('.bbmodel') and p.startswith('tools/')}|({'LICENCE','README.md'}&available)
    return set()

def safe_path(parent,p):
    rel=Path(p)
    if rel.is_absolute() or '..' in rel.parts:raise ValueError('unsafe path')
    out=parent/rel;out.parent.mkdir(parents=True,exist_ok=True);return out

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--source',choices=['primary','all','morearmor','cleann','loy','french'],default='primary')
    ap.add_argument('--dry-run',action='store_true')
    ap.add_argument('--local',action='append',default=[],metavar='SOURCE=DIR',help='GitHub 대신 그 커밋을 가진 로컬 git 클론에서 읽기')
    args=ap.parse_args()
    for spec in args.local:
        k,_,d=spec.partition('=');LOCAL[k]=Path(d)
    targets={'primary':['morearmor','cleann','loy'],'all':['morearmor','cleann','loy','french']}.get(args.source,[args.source])
    sourcefile=ROOT/'ASSET_PROVENANCE.csv'
    rows=[]
    for source in targets:
        reg=REG[source];avail=tree(source);files=sorted(requested(source,avail))
        print(f'{source}: {len(files)} files (pinned {reg["sha"][:12]})')
        for i,p in enumerate(files,1):
            loc=ROOT/'upstream'/source
            dest=safe_path(loc,p)
            url=f'https://raw.githubusercontent.com/{reg["repo"]}/{reg["sha"]}/'+urllib.parse.quote(p,safe='/')
            if not args.dry_run:
                if not dest.exists():
                    dest.write_bytes(git(LOCAL[source],'show',f"{reg['sha']}:{p}") if source in LOCAL else request(url))
                digest=hashlib.sha256(dest.read_bytes()).hexdigest()
            else:digest=''
            rows.append([source,reg['repo'],reg['sha'],reg['license'],p,str(dest.relative_to(ROOT)),digest,url])
            if i%40==0:print(' ',i,'/',len(files))
    with sourcefile.open('w',newline='',encoding='utf-8-sig') as f:
        w=csv.writer(f);w.writerow(['source','repository','pinned_commit','license','original_path','local_path','sha256','download_url']);w.writerows(rows)
    print('DONE:',len(rows),'entries; do not use assets until verified in Blockbench and rights retained.')
if __name__=='__main__':main()
