#!/usr/bin/env python3
"""Blockbench .bbmodel → GeckoLib/Bedrock .geo.json (1.12.0) 변환 — 표준 라이브러리만.

Blockbench 의 Bedrock 내보내기(js/formats/bedrock/bedrock.js compileCube·compileGroup)와 같은 규칙:
  - 큐브 origin.x = -(from.x + size.x), 본 pivot.x = -origin.x  (Blockbench 화면과 geo 파일은 x 가 반대)
  - 큐브·본 회전은 x·y 축 부호를 바꿈 (z 는 그대로)
  - 면별 UV: [u, v] + uv_size, up·down 면은 (u+w, v+h) 에서 (-w, -h)
  - 상자 UV(box_uv): uv = uv_offset, mirror
그래서 Blockbench 화면에서 armorRightArm 은 x>0 에 있고, 내보낸 geo 에서는 x<0 이 된다 (GeckoLib 갑옷 규칙).

사용: python3 bb2geo.py <입력.bbmodel> <출력.geo.json> [--identifier geometry.x]
"""
import json
import sys


def size(e):
    return [e["to"][i] - e["from"][i] for i in range(3)]


def compile_cube(e, group_mirror=False):
    s = size(e)
    t = {"origin": [round(-(e["from"][0] + s[0]), 4), round(e["from"][1], 4), round(e["from"][2], 4)],
         "size": [round(v, 4) for v in s]}
    if e.get("inflate"):
        t["inflate"] = e["inflate"]
    rot = e.get("rotation") or [0, 0, 0]
    if any(rot):
        piv = list(e.get("origin", [0, 0, 0]))
        piv[0] = -piv[0]
        t["pivot"] = piv
        t["rotation"] = [-rot[0], -rot[1], rot[2]]
    if e.get("box_uv"):
        t["uv"] = e.get("uv_offset", [0, 0])
        if e.get("mirror_uv") and not group_mirror:
            t["mirror"] = True
    else:
        uv = {}
        for key, f in (e.get("faces") or {}).items():
            if f.get("texture") is None:
                continue
            u1, v1, u2, v2 = f["uv"]
            fu = {"uv": [u1, v1], "uv_size": [u2 - u1, v2 - v1]}
            if f.get("rotation"):
                fu["uv_rotation"] = f["rotation"]
            if key in ("up", "down"):
                fu["uv"] = [fu["uv"][0] + fu["uv_size"][0], fu["uv"][1] + fu["uv_size"][1]]
                fu["uv_size"] = [-fu["uv_size"][0], -fu["uv_size"][1]]
            uv[key] = fu
        t["uv"] = uv
    return t


def convert(bb, identifier=None):
    els = {e["uuid"]: e for e in bb.get("elements", []) if e.get("type", "cube") == "cube"}
    groups = {g["uuid"]: g for g in bb.get("groups", [])}
    bones = []

    def walk(nodes, parent):
        for n in nodes:
            if not isinstance(n, dict):
                continue
            g = groups.get(n.get("uuid"), n)
            if g.get("export") is False:
                continue
            bone = {"name": g["name"]}
            if parent:
                bone["parent"] = parent
            piv = list(g.get("origin", [0, 0, 0]))
            piv[0] = -piv[0]
            bone["pivot"] = piv
            rot = g.get("rotation") or [0, 0, 0]
            if any(rot):
                bone["rotation"] = [-rot[0], -rot[1], rot[2]]
            cubes = [compile_cube(els[c], g.get("mirror_uv", False)) for c in n.get("children", [])
                     if isinstance(c, str) and c in els and els[c].get("export", True) is not False]
            if cubes:
                bone["cubes"] = cubes
            bones.append(bone)
            walk(n.get("children", []), g["name"])

    walk(bb.get("outliner", []), None)
    res = bb.get("resolution", {"width": 64, "height": 64})
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": identifier or bb.get("model_identifier") or "geometry.unknown",
                        "texture_width": res["width"], "texture_height": res["height"],
                        "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1, 0]},
        "bones": bones}]}


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    bb = json.load(open(argv[0], encoding="utf-8-sig"))
    ident = argv[argv.index("--identifier") + 1] if "--identifier" in argv else None
    with open(argv[1], "w", encoding="utf-8") as f:
        json.dump(convert(bb, ident), f, indent=1)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
