#!/usr/bin/env python3
"""테스트 장비 v06: 오픈소스 장비 라이브러리(source_assets/armor_library)의 원본 모델을 그대로 4부위로 나눈다.

AI 가 만든 큐브·그림은 쓰지 않는다 (v05 까지의 재조립·다시 칠하기·새 큐브는 폐기, _retired_ai 에 보관).
- 원본: source_assets/armor_library/upstream/cleann/bb/*.bbmodel (커밋 a1ca6f85 고정, MIT)
- 배정: 04_240_slots.csv 의 3d_source_candidates (기사 T01 = templar, 마법사 T05 = archmagefire, 도적 T10 = thiefmob)
- 원본 큐브의 위치·크기·회전·상자 UV·텍스처는 바꾸지 않는다. 바꾸는 것은 아래 FIXES 에 적은 것뿐이며 NOTES 에 그대로 남긴다.
  1) 몹 리그(torso·head 등) → GeckoLib 갑옷 본(armorBody 등)으로 옮김 (좌표는 절대 좌표라 그대로)
  2) 몹 맨살 큐브(head 그룹의 inflate 0 머리)는 갑옷이 아니라서 뺌
  3) 맨살과 같은 크기·inflate 0 인 큐브는 플레이어 피부와 겹쳐 깜빡이므로 inflate 만 0.4 로
- 슬롯: helmet = armorHead / chestplate = armorBody·팔 / leggings = 다리 / boots = 장화 (GeckoLib 기본 슬롯 규칙)

결과: <out>/<id>/<id>.bbmodel  (geo·텍스처·아이콘은 test_armor.py import 가 만든다)
사용: python3 build_from_library.py --out <폴더>
"""
import base64
import copy
import io
import json
import os
import sys
import uuid

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
UP = os.path.join(REPO, "source_assets", "armor_library", "upstream", "cleann", "bb")
SKIN_INFLATE = 0.4

BONES = {"armorHead": [0, 24, 0], "armorBody": [0, 24, 0], "armorRightArm": [5, 22, 0], "armorLeftArm": [-5, 22, 0],
         "armorRightLeg": [1.9, 12, 0], "armorLeftLeg": [-1.9, 12, 0], "armorRightBoot": [1.9, 12, 0], "armorLeftBoot": [-1.9, 12, 0]}
SLOT_BONES = {"helmet": ["armorHead"], "chestplate": ["armorBody", "armorRightArm", "armorLeftArm"],
              "leggings": ["armorRightLeg", "armorLeftLeg"], "boots": ["armorRightBoot", "armorLeftBoot"]}
SHORT = {"helmet": "helmet", "chestplate": "chest", "leggings": "legs", "boots": "boots"}
SKIN_BOXES = {"armorHead": ([-4, 24, -4], [4, 32, 4]), "armorBody": ([-4, 12, -2], [4, 24, 2]),
              "armorRightArm": ([4, 12, -2], [8, 24, 2]), "armorLeftArm": ([-8, 12, -2], [-4, 24, 2])}

# 원본 파일, 쓸 텍스처 번호, 원본 그룹 → 갑옷 본, 빼는 그룹
SETS = {
    ("knight", 1): {"file": "templar.geo.bbmodel", "texture": 1,   # templar.png (0 은 네더라이트판 — 오버월드 1급이라 기본판)
                    "map": {"armorHead": "armorHead", "armorBody": "armorBody", "armorWaist": "armorBody",
                            "armorRightArm": "armorRightArm", "armorLeftArm": "armorLeftArm",
                            "armorRightLeg": "armorRightLeg", "armorLeftLeg": "armorLeftLeg",
                            "armorRightBoot": "armorRightBoot", "armorLeftBoot": "armorLeftBoot"}},
    ("mage", 5): {"file": "archmagefire.bbmodel", "texture": 0,
                  "map": {"armorHead": "armorHead", "torso": "armorBody",
                          "armorRightArm": "armorRightArm", "armorLeftArm": "armorLeftArm",
                          "armorRightLeg": "armorRightLeg", "armorLeftLeg": "armorLeftLeg",
                          "armorRightBoot": "armorRightBoot", "armorLeftBoot": "armorLeftBoot"},
                  "drop": {"head": "몹 맨살 머리(inflate 0)", "itemBone": "몹 손 본(큐브 없음)"}},
    ("rogue", 10): {"file": "thiefmob.bbmodel", "texture": 0,
                    "map": {"armorHead": "armorHead", "torso": "armorBody",
                            "armorRightArm": "armorRightArm", "armorLeftArm": "armorLeftArm",
                            "armorRightLeg": "armorRightLeg", "armorLeftLeg": "armorLeftLeg",
                            "armorRightBoot": "armorRightBoot", "armorLeftBoot": "armorLeftBoot"},
                    "drop": {"head": "몹 맨살 머리(inflate 0)", "itemBone": "몹 손 본(큐브 없음)"}},
}


def load(name):
    d = json.load(open(os.path.join(UP, name), encoding="utf-8-sig"))
    els = {e["uuid"]: e for e in d["elements"]}
    groups = {g["uuid"]: g for g in d.get("groups", [])}
    by_group = []

    def walk(nodes, gname):
        for n in nodes:
            if isinstance(n, str) and n in els:
                by_group.append((gname, els[n]))
            elif isinstance(n, dict):
                g = groups.get(n["uuid"], n)
                walk(n.get("children", []), g["name"])

    walk(d["outliner"], None)
    return d, by_group


def build(cls, grade, out_dir):
    spec = SETS[(cls, grade)]
    d, cubes = load(spec["file"])
    tex_src = d["textures"][spec["texture"]]
    img = Image.open(io.BytesIO(base64.b64decode(tex_src["source"].split(",", 1)[1]))).convert("RGBA")
    semi = sum(img.getchannel("A").histogram()[1:255])
    report = {"source": spec["file"], "texture": tex_src["name"], "texture_semi_alpha_px": semi, "slots": {}}
    per_bone, dropped, fixes, unknown = {}, [], [], []
    for gname, e in cubes:
        if gname in spec.get("drop", {}):
            dropped.append(f"{gname} {e['from']}→{e['to']} ({spec['drop'][gname]})")
            continue
        bone = spec["map"].get(gname)
        if bone is None:
            unknown.append(f"{gname} {e['from']}→{e['to']}")
            continue
        e = copy.deepcopy(e)
        for f in e.get("faces", {}).values():
            f["texture"] = 0
        skin = SKIN_BOXES.get(bone)
        if skin and [round(x, 3) for x in e["from"]] == skin[0] and [round(x, 3) for x in e["to"]] == skin[1] \
                and abs(e.get("inflate", 0)) < 0.05 and not e.get("rotation"):
            fixes.append(f"{bone} {e['from']}→{e['to']}: inflate {e.get('inflate', 0)} → {SKIN_INFLATE} (피부와 겹침 방지)")
            e["inflate"] = SKIN_INFLATE
        per_bone.setdefault(bone, []).append(e)
    if unknown:
        raise SystemExit(f"{spec['file']}: 배정하지 않은 그룹의 큐브 {unknown}")
    report.update(dropped=dropped, fixes=fixes)
    made = {}
    for slot, bones in SLOT_BONES.items():
        tid = f"test_{cls}_{SHORT[slot]}_t{grade:02d}"
        n = sum(len(per_bone.get(b, [])) for b in bones)
        if not n:
            report["slots"][slot] = 0
            continue
        path = os.path.join(out_dir, tid, f"{tid}.bbmodel")
        write_bbmodel(tid, bones, per_bone, d, tex_src, path)
        report["slots"][slot] = n
        made[slot] = (tid, path)
    return made, report


def write_bbmodel(tid, bones, per_bone, src, tex_src, path):
    elements, groups, outliner = [], [], []
    for bone in BONES:                       # 8 갑옷 본을 모두 둔다(빈 본 포함) — GeckoLib 관례, 다른 부위 본은 비어 있음
        gid = str(uuid.uuid5(uuid.NAMESPACE_URL, f"fantasyrpg/{tid}/{bone}"))
        groups.append({"name": bone, "uuid": gid, "origin": BONES[bone], "rotation": [0, 0, 0], "visibility": True, "export": True})
        node = {"uuid": gid, "isOpen": True, "children": []}
        for e in (per_bone.get(bone, []) if bone in bones else []):
            elements.append(e)
            node["children"].append(e["uuid"])
        outliner.append(node)
    tex = dict(tex_src)
    tex.update(id="0", name=f"{tid}.png", path="", relative_path="", folder="", namespace="")
    res = src.get("resolution", {"width": 64, "height": 64})
    bb = {"meta": {"format_version": src["meta"].get("format_version", "4.10"), "model_format": "bedrock", "box_uv": True},
          "name": tid, "model_identifier": f"geometry.fantasyrpg.test_armor.{tid}", "visible_box": [3, 3, 0],
          "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {},
          "resolution": res, "elements": elements, "groups": groups, "outliner": outliner, "textures": [tex]}
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(bb, open(path, "w", encoding="utf-8"), indent=1)


def main(argv):
    if "--out" not in argv:
        sys.exit(__doc__)
    out = argv[argv.index("--out") + 1]
    allrep = {}
    for key in SETS:
        made, rep = build(*key, out)
        rep["made"] = {s: t for s, (t, _) in made.items()}
        allrep[f"{key[0]}_t{key[1]:02d}"] = rep
        print(key, rep["slots"], "고친 것", len(rep["fixes"]), "뺀 것", len(rep["dropped"]))
    json.dump(allrep, open(os.path.join(out, "build_report.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
