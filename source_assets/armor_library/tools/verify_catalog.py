#!/usr/bin/env python3
from pathlib import Path
import csv,json,collections
R=Path(__file__).resolve().parents[1]
def load(n):
 with (R/n).open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f))
rows=load('04_240_slots.csv');sets=load('05_60_sets.csv');sources=load('02_morearmor_36_sets.csv');models=load('03_cleann_15_models.csv')
assert len(rows)==240 and len(sets)==60 and len(sources)==36 and len(models)==15
assert len({x['target_id_provisional'] for x in rows})==240
assert len({(x['class'],x['grade']) for x in sets})==60
assert len({x['asset_set'] for x in sources})==36
m=[x for x in sets if x['2d_asset_set']]
assert len(m)==len(json.loads((R/'assignment.json').read_text(encoding='utf-8')))
assert len({x['2d_asset_set'] for x in m})==len(m)
# 2026-11-23 FantasyRPG: 처음엔 '모두 0' 이었으나 진행 기록을 남기므로 단계 순서(가져옴 ≥ 변환 ≥ 연결 ≥ 테스트)와 게임 아이템 연결을 검사
F=['files_imported','3d_model_converted','game_linked','game_tested']
for x in rows:
  v=[int(x[k]) for k in F]
  assert all(a in (0,1) for a in v) and v==sorted(v,reverse=True),('flag order',x['target_id_provisional'],v)
  if v[0]: assert x.get('game_item_id','').startswith('fantasyrpg:'),('imported without game_item_id',x['target_id_provisional'])
man=R.parents[0]/'test_armor'/'manifest.json'
if man.exists():
  ids={'fantasyrpg:'+e['id'] for e in json.loads(man.read_text(encoding='utf-8'))['entries']}
  for x in rows:
    if int(x['files_imported']): assert x['game_item_id'] in ids,('game_item_id not in devtest manifest',x['game_item_id'])
done={k:sum(int(x[k]) for x in rows) for k in F}
print(f'PASS: {len(rows)} slots, {len(sets)} sets, {len(m)} provisional 2D assignments ({len(m)*4} icons), {len(models)} source model candidates. progress {done}')
