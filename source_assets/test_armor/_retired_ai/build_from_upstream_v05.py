#!/usr/bin/env python3
"""테스트 흉갑 3종 리비전 v05: cleannrooster(MIT) 원본에서 큐브·텍스처를 골라 우리 장비로 재작업한 bbmodel 을 만든다.

- 원본: source_assets/test_armor/_upstream/cleannrooster-a1ca6f8/bb/*.bbmodel (커밋 고정, LICENSE·ATTRIBUTION 같이 둠)
- 결과: source_assets/test_armor/<id>/revisions/v05/<id>.bbmodel (+ 텍스처 PNG). geo·아이콘은 test_armor.py import --from-bbmodel 이 만든다.
- Blockbench 좌표 그대로: x>0 = 캐릭터 오른쪽(armorRight*), 앞 = -z. 원본도 같은 규칙.
- 원본 큐브는 상자 UV 그대로(텍스처를 아틀라스 왼쪽 위 128×128 에 둠), 새 큐브는 상자 UV 로 아틀라스 빈 곳에 그림을 그린다.
- 그림 색은 원본 그림을 다시 칠함(기사 = 평범한 강철, 마법사 = 남색·금, 도적 = 먹빛 보라). 재현 가능하도록 무작위 시드 고정.

사용: python3 build_from_upstream.py [--out <source_assets/test_armor>]
"""
import base64
import colorsys
import copy
import io
import json
import math
import os
import random
import sys
import uuid

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(REPO, "source_assets", "test_armor")
UP = os.path.join(SRC, "_upstream", "cleannrooster-a1ca6f8", "bb")
ATLAS = 256
RNG = random.Random(20261123)


# ---------- 원본 읽기 ----------
def load(name):
    d = json.load(open(os.path.join(UP, name), encoding="utf-8-sig"))
    tex = [Image.open(io.BytesIO(base64.b64decode(t["source"].split(",", 1)[1]))).convert("RGBA") for t in d["textures"]]
    els = {e["uuid"]: e for e in d["elements"]}
    groups = {g["uuid"]: g for g in d["groups"]}
    by_group = {}

    def walk(nodes, gname):
        for n in nodes:
            if isinstance(n, str) and n in els:
                by_group.setdefault(gname, []).append(els[n])
            elif isinstance(n, dict):
                g = groups.get(n["uuid"], n)
                by_group.setdefault(g["name"], [])
                walk(n.get("children", []), g["name"])

    walk(d["outliner"], None)
    return {"bb": d, "tex": tex, "groups": {g["name"]: g for g in groups.values()}, "by_group": by_group}


def size(e):
    return [e["to"][i] - e["from"][i] for i in range(3)]


def box_faces(u, v, w, h, d):
    w, h, d = (max(0, math.floor(a + 1e-6)) for a in (w, h, d))   # GeckoLib·Blockbench 처럼 내림
    return {"north": [u + d, v + d, u + d + w, v + d + h], "east": [u, v + d, u + d, v + d + h],
            "south": [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], "west": [u + d + w, v + d, u + 2 * d + w, v + d + h],
            "up": [u + d + w, v + d, u + d, v], "down": [u + d + 2 * w, v, u + d + w, v + d]}


def take(src, group, idx, dx=0, dy=0, name=None, **over):
    """원본 큐브 하나를 복사해 아틀라스 위치(dx, dy)만큼 UV 를 옮긴다"""
    e = copy.deepcopy(src["by_group"][group][idx])
    e["uuid"] = str(uuid.UUID(int=RNG.getrandbits(128)))
    e["name"] = name or e.get("name", group)
    e.update(over)
    if e.get("box_uv"):
        u, v = e.get("uv_offset", [0, 0])
        e["uv_offset"] = [u + dx, v + dy]
        s = size(e)
        for k, r in box_faces(u + dx, v + dy, *s).items():
            e.setdefault("faces", {}).setdefault(k, {})["uv"] = r
    else:
        for f in e.get("faces", {}).values():
            if f.get("uv"):
                f["uv"] = [f["uv"][0] + dx, f["uv"][1] + dy, f["uv"][2] + dx, f["uv"][3] + dy]
    for f in e.get("faces", {}).values():
        f["texture"] = 0
    return e


# ---------- 다시 칠하기 ----------
def recolor(img, fn):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = fn(r, g, b) + (a,)
    return out


def hsv(r, g, b):
    return colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)


def rgb(h, s, v):
    return tuple(int(max(0, min(1, c)) * 255) for c in colorsys.hsv_to_rgb(h % 1, s, v))


def steel(r, g, b):
    h, s, v = hsv(r, g, b)
    if 0.08 <= h <= 0.17 and s > 0.35 and v > 0.35:          # 금 → 무딘 황동
        return rgb(0.11, s * 0.45, v * 0.85)
    if (h < 0.04 or h > 0.93) and s > 0.45:                   # 붉은 천 → 짙은 감청 천
        return rgb(0.62, 0.35, 0.18 + v * 0.35)
    return rgb(0.58, 0.10, 0.22 + v * 0.72)                    # 나머지 → 차가운 강철


def indigo(r, g, b):
    h, s, v = hsv(r, g, b)
    if 0.08 <= h <= 0.17 and s > 0.3:                          # 금은 그대로(조금 밝게)
        return rgb(h, s, min(1, v * 1.05))
    if h < 0.07 or h > 0.9:                                    # 붉은 로브 → 남색
        return rgb(0.68, min(1, s * 0.85 + 0.1), v * 0.9)
    return rgb(h, s, v)


def violet_dark(r, g, b):
    h, s, v = hsv(r, g, b)
    if 0.5 <= h <= 0.72:                                       # 파란 천 → 먹빛 보라
        return rgb(0.76, s * 0.55, v * 0.78)
    return rgb(h, s * 0.8, v * 0.9)


# ---------- 새 큐브 ----------
class Painter:
    def __init__(self, img, x0, y0, x1, y1):
        self.img, self.x0, self.y0, self.x1, self.y1 = img, x0, y0, x1, y1
        self.cx, self.cy, self.row = x0, y0, 0

    def alloc(self, w, h, d):
        W, H = 2 * (w + d), h + d
        if self.cx + W > self.x1:
            self.cx, self.cy, self.row = self.x0, self.cy + self.row + 1, 0
        if self.cy + H > self.y1:
            raise RuntimeError("아틀라스 공간 부족")
        u, v = self.cx, self.cy
        self.cx += W + 1
        self.row = max(self.row, H)
        return u, v

    def paint(self, u, v, w, h, d, base, dark, light, trim=None, trim_rows=0, folds=True):
        px = self.img.load()
        for face, (a, b, c, e) in box_faces(u, v, w, h, d).items():
            x1, x2 = sorted((a, c))
            y1, y2 = sorted((b, e))
            side = face in ("north", "south", "east", "west")
            for y in range(y1, y2):
                for x in range(x1, x2):
                    col = base if side else dark
                    if folds and side and (x - x1) % 3 == 2:
                        col = tuple(int(c * 0.86) for c in col)
                    if side and trim and y >= y2 - trim_rows:
                        col = trim if (y - (y2 - trim_rows)) % 2 == 0 else tuple(int(c * 0.8) for c in trim)
                    if x in (x1, x2 - 1) or y in (y1, y2 - 1):
                        col = tuple(int(c * 0.72) for c in col)
                    elif y == y1 + 1 and side:
                        col = light
                    n = RNG.randint(-6, 6)
                    px[x, y] = tuple(max(0, min(255, c + n)) for c in col) + (255,)


def cube(painter, name, frm, to, style, inflate=0.0, trim=None, trim_rows=0, rotation=None, origin=None):
    w, h, d = (math.floor(to[i] - frm[i] + 1e-6) for i in range(3))
    u, v = painter.alloc(w, h, d)
    painter.paint(u, v, w, h, d, *style, trim=trim, trim_rows=trim_rows)
    e = {"name": name, "box_uv": True, "render_order": "default", "locked": False, "allow_mirror_modeling": True,
         "from": list(frm), "to": list(to), "autouv": 0, "color": 0, "inflate": inflate,
         "origin": origin or [(frm[i] + to[i]) / 2 for i in range(3)], "uv_offset": [u, v],
         "faces": {k: {"uv": r, "texture": 0} for k, r in box_faces(u, v, w, h, d).items()},
         "type": "cube", "uuid": str(uuid.UUID(int=RNG.getrandbits(128)))}
    if rotation:
        e["rotation"] = rotation
    return e


# ---------- bbmodel 쓰기 ----------
BONES = [("armorHead", [0, 24, 0]), ("armorBody", [0, 24, 0]), ("armorRightArm", [5, 22, 0]), ("armorLeftArm", [-5, 22, 0]),
         ("armorRightLeg", [1.9, 12, 0]), ("armorLeftLeg", [-1.9, 12, 0])]


def write_bbmodel(tid, parts, children, atlas, path):
    """parts: {bone: [element]}, children: {bone: [(group_name, origin, [element])]}"""
    elements, groups, outliner = [], [], []
    for bone, origin in BONES:
        gid = str(uuid.UUID(int=RNG.getrandbits(128)))
        groups.append({"name": bone, "uuid": gid, "origin": origin, "rotation": [0, 0, 0], "visibility": True, "export": True})
        node = {"uuid": gid, "isOpen": True, "children": []}
        for e in parts.get(bone, []):
            elements.append(e)
            node["children"].append(e["uuid"])
        for cname, corigin, cels in children.get(bone, []):
            cid = str(uuid.UUID(int=RNG.getrandbits(128)))
            groups.append({"name": cname, "uuid": cid, "origin": corigin, "rotation": [0, 0, 0], "visibility": True, "export": True})
            sub = {"uuid": cid, "isOpen": True, "children": []}
            for e in cels:
                elements.append(e)
                sub["children"].append(e["uuid"])
            node["children"].append(sub)
        outliner.append(node)
    px = atlas.load()                                   # 갑옷은 잘라내기 렌더: 알파는 0 또는 255 로
    for y in range(atlas.height):
        for x in range(atlas.width):
            c = px[x, y]
            if 0 < c[3] < 255:
                px[x, y] = c[:3] + ((255,) if c[3] >= 128 else (0,))
    buf = io.BytesIO()
    atlas.save(buf, "PNG")
    tex = {"name": f"{tid}.png", "id": "0", "uuid": str(uuid.UUID(int=RNG.getrandbits(128))), "path": "", "relative_path": "",
           "folder": "", "namespace": "", "group": "", "width": ATLAS, "height": ATLAS, "uv_width": ATLAS, "uv_height": ATLAS,
           "particle": False, "use_as_default": False, "layers_enabled": False, "sync_to_project": "", "render_mode": "default",
           "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
           "visible": True, "internal": True, "saved": False,
           "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()}
    bb = {"meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": True}, "name": tid,
          "model_identifier": f"geometry.fantasyrpg.test_armor.{tid}", "visible_box": [3, 3, 0],
          "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [],
          "unhandled_root_fields": {}, "resolution": {"width": ATLAS, "height": ATLAS},
          "elements": elements, "groups": groups, "outliner": outliner, "textures": [tex]}
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(bb, open(path, "w", encoding="utf-8"), indent=1)
    atlas.save(os.path.splitext(path)[0].replace(".bbmodel", "") + ".png")
    return len(elements)


# ---------- 3종 ----------
def knight(out):
    T, C = load("templar.geo.bbmodel"), load("cityguardian.bbmodel")
    atlas = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    atlas.alpha_composite(recolor(T["tex"][1], steel), (0, 0))     # templar.png (네더라이트판 아님)
    atlas.alpha_composite(recolor(C["tex"][0], steel), (128, 0))
    body = [take(T, "armorBody", 0, name="templar_breastplate"),
            take(T, "armorBody", 2, name="templar_front_tabard"),
            take(T, "armorWaist", 0, name="templar_fauld"),
            take(T, "armorWaist", 1, name="templar_belt"),
            take(T, "armorWaist", 2, name="templar_buckle"),
            take(C, "armorBody", 3, 128, 0, name="guardian_upper_plate"),
            take(C, "armorBody", 1, 128, 0, name="guardian_gorget_r"),
            take(C, "armorBody", 2, 128, 0, name="guardian_gorget_l")]
    tassets = []
    for g, nm in (("waistFront", "tasset_front"), ("waistRight", "tasset_right"), ("waistLeft", "tasset_left"), ("waistBack", "tasset_back")):
        els = [take(C, g, i, 128, 0, name=f"{nm}_{i}") for i in range(len(C["by_group"][g]))]
        tassets.append((nm, C["groups"][g]["origin"], els))
    arm = lambda b, pre: [take(T, b, i, name=f"{pre}_{n}") for i, n in enumerate(
        ("rerebrace", "pauldron", "pauldron_ring", "vambrace", "couter") if b == "armorRightArm" else
        ("rerebrace", "vambrace", "pauldron_ring", "couter", "pauldron"))]
    parts = {"armorBody": body, "armorRightArm": arm("armorRightArm", "r"), "armorLeftArm": arm("armorLeftArm", "l")}
    return write_bbmodel("test_knight_chest_t01", parts, {"armorBody": tassets}, atlas, out("test_knight_chest_t01"))


def mage(out):
    A = load("archmagefire.bbmodel")
    atlas = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    atlas.alpha_composite(recolor(A["tex"][0], indigo), (0, 0))
    P = Painter(atlas, 0, 130, ATLAS, ATLAS)
    robe = ((46, 44, 112), (30, 28, 74), (78, 74, 160))
    lining = ((70, 52, 120), (44, 32, 80), (104, 84, 160))
    gold = (214, 172, 72)
    gem = ((60, 220, 230), (20, 120, 140), (190, 255, 255))
    body = [take(A, "torso", 6, name="robe_torso"),
            take(A, "torso", 4, name="sash_outer"),
            take(A, "torso", 5, name="sash_inner"),
            take(A, "torso", 1, name="archmage_mantle"),
            cube(P, "high_collar_back", [-4.5, 23.5, 2.2], [4.5, 27.5, 3.2], lining, trim=gold, trim_rows=1),
            cube(P, "collar_side_r", [3.6, 23.5, -2], [4.6, 26.5, 2], lining, trim=gold, trim_rows=1),
            cube(P, "collar_side_l", [-4.6, 23.5, -2], [-3.6, 26.5, 2], lining, trim=gold, trim_rows=1),
            cube(P, "arcane_brooch", [-1, 18.5, -3.6], [1, 20.5, -2.6], gem),
            cube(P, "robe_upper_front", [-4.5, 7, -3.2], [4.5, 12, -2.2], robe, trim=gold, trim_rows=1),
            cube(P, "robe_upper_back", [-4.5, 7, 2.2], [4.5, 12, 3.2], robe, trim=gold, trim_rows=1),
            cube(P, "robe_upper_side_r", [4.5, 7, -2.5], [5.5, 12, 2.5], robe, trim=gold, trim_rows=1),
            cube(P, "robe_upper_side_l", [-5.5, 7, -2.5], [-4.5, 12, 2.5], robe, trim=gold, trim_rows=1)]

    def leg(sign, pre):
        # 가운데에서 맞닿는 계단형 아래 자락 (서 있으면 한 벌, 걸으면 다리를 따라 나뉨). sign +1 = 오른쪽
        x = lambda a, b: sorted([a * sign, b * sign])
        mk = lambda n, xa, xb, y0, y1, z0, z1, tr: cube(P, f"{pre}_{n}", [x(xa, xb)[0], y0, z0], [x(xa, xb)[1], y1, z1], robe, trim=gold, trim_rows=tr)
        return [mk("mid_front", 0, 5, 3, 8, -3.6, -2.6, 1), mk("mid_back", 0, 5, 3, 8, 2.6, 3.6, 1),
                mk("mid_side", 5, 6, 3, 8, -3.0, 3.0, 1),
                mk("hem_front", 0, 6, 0, 4, -4.0, -3.0, 2), mk("hem_back", 0, 6, 0, 4, 3.0, 4.0, 2),
                mk("hem_side", 6, 7, 0, 4, -4.0, 4.0, 2)]

    def arm(b, sign, pre, sleeve_idx):
        x = lambda a, c: sorted([a * sign, c * sign])
        return [take(A, b, sleeve_idx, name=f"{pre}_archmage_sleeve"),
                cube(P, f"{pre}_bell_cuff", [x(3, 9)[0], 11, -3], [x(3, 9)[1], 14, 3], lining, trim=gold, trim_rows=1),
                cube(P, f"{pre}_sleeve_drape", [x(4, 8)[0], 9, 1.8], [x(4, 8)[1], 13, 2.8], robe, trim=gold, trim_rows=1)]

    parts = {"armorBody": body, "armorRightArm": arm("armorRightArm", 1, "r", 0), "armorLeftArm": arm("armorLeftArm", -1, "l", 0),
             "armorRightLeg": leg(1, "r_robe"), "armorLeftLeg": leg(-1, "l_robe")}
    return write_bbmodel("test_mage_chest_t05", parts, {}, atlas, out("test_mage_chest_t05"))


def rogue(out):
    Th, S = load("thiefmob.bbmodel"), load("sanguine.geo.bbmodel")
    atlas = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    atlas.alpha_composite(recolor(Th["tex"][0], violet_dark), (0, 0))
    atlas.alpha_composite(S["tex"][2], (128, 0))                   # sanguine_purple: 고등급 강조만
    P = Painter(atlas, 0, 130, ATLAS, ATLAS)
    leather = ((58, 44, 62), (36, 26, 40), (88, 70, 92))
    cloth = ((40, 34, 52), (26, 22, 34), (64, 56, 82))
    violet = (150, 90, 200)
    head = [take(Th, "armorHead", 0, name="thief_hood"),
            cube(P, "face_mask", [-4, 24, -5], [4, 27, -4], cloth, trim=violet, trim_rows=1)]
    body = [take(Th, "torso", 0, name="thief_vest"), take(Th, "torso", 1, name="thief_belt"),
            take(Th, "torso", 2, name="harness_strap_r"), take(Th, "torso", 3, name="harness_strap_l"),
            take(Th, "torso", 4, name="thief_back_cloak"),
            cube(P, "pouch_r", [2.2, 11.6, -3.5], [4.2, 13.6, -2.5], leather, trim=violet, trim_rows=1),
            cube(P, "pouch_l", [-4.6, 11.4, -3.2], [-2.6, 13.4, -2.2], leather),
            cube(P, "dagger_sheath", [-5.2, 10, 0], [-4.2, 15, 2], leather, trim=violet, trim_rows=1)]
    right = [take(Th, "armorRightArm", 2, name="r_sleeve"), take(Th, "armorRightArm", 0, name="r_leather_pauldron"),
             take(Th, "armorRightArm", 1, name="r_bracer")]
    left = [take(Th, "armorLeftArm", 2, name="l_sleeve"),
            take(S, "armorLeftArm", 0, 128, 0, name="l_sanguine_pauldron"),
            take(S, "armorLeftArm", 2, 128, 0, name="l_sanguine_rim"),
            take(Th, "armorLeftArm", 1, name="l_bracer")]
    rleg = [cube(P, "r_coat_tail_back", [0, 4, 2.4], [4.4, 11, 3.4], cloth, trim=violet, trim_rows=1),
            cube(P, "r_coat_tail_side", [4.4, 6, -2.6], [5.4, 11, 2.4], cloth, trim=violet, trim_rows=1)]
    lleg = [cube(P, "l_coat_tail_back", [-4.4, 1, 2.4], [0, 11, 3.4], cloth, trim=violet, trim_rows=1),
            cube(P, "l_coat_tail_side", [-5.4, 3, -2.6], [-4.4, 11, 2.4], cloth, trim=violet, trim_rows=1)]
    parts = {"armorHead": head, "armorBody": body, "armorRightArm": right, "armorLeftArm": left,
             "armorRightLeg": rleg, "armorLeftLeg": lleg}
    return write_bbmodel("test_rogue_chest_t10", parts, {}, atlas, out("test_rogue_chest_t10"))


def main(argv):
    base = argv[argv.index("--out") + 1] if "--out" in argv else os.path.join(SRC, "_build_v05")
    out = lambda tid: os.path.join(base, tid, f"{tid}.bbmodel")
    for fn in (knight, mage, rogue):
        print(fn.__name__, "큐브", fn(out))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
