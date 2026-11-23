#!/usr/bin/env python3
"""개발자 전용 테스트 장비(fantasyrpg devtest) 리소스 관리 — 표준 라이브러리만 씀 (Python 3.8+).

폴더 (source_assets/test_armor/):
  manifest.json                    테스트 장비 목록 (아이템 id·직업·등급·부위·경로·활성 리비전)
  <id>/current/                    지금 작업 중인 파일 (Blockbench 는 여기를 열고 저장)
      <id>.bbmodel  <id>.geo.json  <id>.png(입은 모습 텍스처)  <id>_icon.png(인벤토리 아이콘)  REVISION
  <id>/revisions/vNN/              보관한 판 (지우지 않음)

명령:
  list                          항목·활성 리비전·보관한 판
  validate                      파일·JSON·본 이름·좌우·UV 범위·텍스처 크기 검사 (오류가 있으면 종료 코드 1)
  snapshot <id> [--note 글]     current 를 새 판(vNN+1)으로 보관하고 manifest 의 revision 을 그 판으로
  use <id> <rev>                보관한 판을 current 로 되돌리고 manifest 의 revision 을 그 판으로
  import <id> --bbmodel P [--geo P] [--texture P] [--icon P] [--note 글]
                                받은 파일로 새 판(vNN+1)을 만들고 current·manifest 를 그 판으로.
                                --geo 를 주지 않으면 bbmodel 에서 Blockbench 와 같은 규칙으로 만든다(bb2geo.py, 권장),
                                --texture 가 없으면 bbmodel 안의 그림, --icon 이 없으면 3D 앞모습으로 만든다(preview.py, Pillow 필요).
  deploy repo                   current → 모드 소스 리소스(core-mod/src/main/resources) — Gradle 빌드해야 jar 에 들어감
  deploy pack <폴더>            current → 리소스팩 폴더 (게임에서 F3+T 로 바로 갱신)
  deploy instance <인스턴스>    current → <인스턴스>/resourcepacks/fantasyrpg_devtest + 목록을 <인스턴스>/config/fantasyrpg-devtest/
                                (목록에 새 항목을 넣었으면 게임을 다시 켜야 아이템이 생김)

예:
  python3 tools/devtest/test_armor.py validate
  python3 tools/devtest/test_armor.py snapshot test_mage_chest_t05 --note "로브 하단 2칸 줄임"
  python3 tools/devtest/test_armor.py use test_mage_chest_t05 v02
  python3 tools/devtest/test_armor.py deploy instance "C:/Users/나/AppData/Roaming/PrismLauncher/instances/마법왕국"
"""
import json
import os
import re
import shutil
import struct
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
ARMOR_BONES = {"armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg", "armorLeftLeg",
               "armorRightBoot", "armorLeftBoot"}
SLOTS = {"helmet", "chestplate", "leggings", "boots"}
ID_RE = re.compile(r"^[a-z0-9_]+$")
PACK_FORMAT = 34  # Minecraft 1.21.1 리소스팩


def find_src(argv):
    if "--src" in argv:
        i = argv.index("--src")
        p = argv[i + 1]
        del argv[i:i + 2]
        return os.path.abspath(p)
    for cand in (os.path.join(HERE, "source_assets", "test_armor"), os.path.join(REPO, "source_assets", "test_armor")):
        if os.path.isfile(os.path.join(cand, "manifest.json")):
            return cand
    sys.exit("source_assets/test_armor/manifest.json 을 찾지 못함 (--src 로 지정)")


def load_manifest(src):
    with open(os.path.join(src, "manifest.json"), encoding="utf-8") as f:
        return json.load(f)


def save_manifest(src, m):
    with open(os.path.join(src, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(m, f, ensure_ascii=False, indent=2)
        f.write("\n")


def resolved(m, e):
    """항목의 실제 경로 (Java DevTestManifest.resolve 와 같은 규칙)"""
    d = m.get("defaults", {})
    out = {}
    for k in ("model", "texture", "icon", "animation", "idle_animation"):
        v = e.get(k, d.get(k, ""))
        out[k] = v.replace("{id}", e["id"]) if isinstance(v, str) else ""
    return out


def revisions(src, i):
    d = os.path.join(src, i, "revisions")
    return sorted(n for n in os.listdir(d) if re.match(r"^v\d+$", n)) if os.path.isdir(d) else []


def current_files(src, i):
    c = os.path.join(src, i, "current")
    return {"bbmodel": os.path.join(c, f"{i}.bbmodel"), "geo": os.path.join(c, f"{i}.geo.json"),
            "texture": os.path.join(c, f"{i}.png"), "icon": os.path.join(c, f"{i}_icon.png")}


# ---------- PNG (크기·알파) ----------
def png_info(path):
    """(너비, 높이, 반투명 픽셀 수 또는 None) — 8비트 RGBA·비인터레이스만 알파를 셈"""
    with open(path, "rb") as f:
        data = f.read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("PNG 아님")
    pos, w, h, depth, ctype, inter, idat = 8, 0, 0, 0, 0, 0, b""
    while pos < len(data):
        ln, typ = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + ln]
        if typ == b"IHDR":
            w, h, depth, ctype, _, _, inter = struct.unpack(">IIBBBBB", body)
        elif typ == b"IDAT":
            idat += body
        pos += 12 + ln
    if depth != 8 or ctype != 6 or inter != 0:
        return w, h, None
    raw, bpp, stride = zlib.decompress(idat), 4, w * 4
    prev, semi, p = bytearray(stride), 0, 0
    for _ in range(h):
        ft, line = raw[p], bytearray(raw[p + 1:p + 1 + stride])
        p += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if ft == 1:
                line[x] = (line[x] + a) & 255
            elif ft == 2:
                line[x] = (line[x] + b) & 255
            elif ft == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif ft == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        semi += sum(1 for x in range(3, stride, 4) if 0 < line[x] < 255)
        prev = line
    return w, h, semi


# ---------- 검사 ----------
def check_geo(path, tex_size):
    errs, warns = [], []
    try:
        g = json.load(open(path, encoding="utf-8"))
        geos = g["minecraft:geometry"]
    except Exception as ex:  # noqa: BLE001
        return [f"geo JSON 읽기 실패: {ex}"], warns
    if len(geos) != 1:
        errs.append(f"geometry 가 {len(geos)}개 (1개여야 함)")
    geo = geos[0]
    desc = geo.get("description", {})
    tw, th = desc.get("texture_width"), desc.get("texture_height")
    if tex_size and (tw, th) != tex_size:
        errs.append(f"geo 텍스처 크기 {tw}×{th} ≠ PNG {tex_size[0]}×{tex_size[1]}")
    names = [b.get("name") for b in geo.get("bones", [])]
    if "armorBody" not in names:
        errs.append("armorBody 본 없음")
    for b in geo.get("bones", []):
        n = b.get("name")
        cubes = b.get("cubes", [])
        if n not in ARMOR_BONES and not b.get("parent"):
            warns.append(f"최상위 본 '{n}' 은 GeckoLib 갑옷 본이 아님 — 몸을 따라 움직이지 않음 (armor* 본의 자식으로)")
        # 좌우: Bedrock 좌표 x<0 = 캐릭터 오른쪽 (GeckoLib armorRightArm 회전축 [-5,22,0])
        if cubes and n in ("armorRightArm", "armorLeftArm", "armorRightLeg", "armorLeftLeg"):
            cx = sum(c["origin"][0] + c["size"][0] / 2 for c in cubes) / len(cubes)
            if (n.startswith("armorRight") and cx > 0) or (n.startswith("armorLeft") and cx < 0):
                errs.append(f"{n} 큐브가 반대편(x 중심 {cx:.1f})에 있음 — 좌우 본 이름이 바뀜")
        for c in cubes:
            uv = c.get("uv")
            if isinstance(uv, dict) and tw and th:
                for face, fu in uv.items():
                    u, v = fu.get("uv", [0, 0])
                    su, sv = fu.get("uv_size", [0, 0])
                    lo_u, hi_u = min(u, u + su), max(u, u + su)
                    lo_v, hi_v = min(v, v + sv), max(v, v + sv)
                    if lo_u < 0 or lo_v < 0 or hi_u > tw or hi_v > th:
                        errs.append(f"{n} 큐브 {c['origin']} {face} 면 UV 가 텍스처 밖")
    return errs, warns


def check_bbmodel(path, geo_path):
    """bbmodel 을 Blockbench 규칙으로 내보낸 것과 geo 가 같은지 (다르면 Blockbench 에서 다시 내보내기)"""
    sys.path.insert(0, HERE)
    import bb2geo
    try:
        bb = json.load(open(path, encoding="utf-8-sig"))
        geo = json.load(open(geo_path, encoding="utf-8"))["minecraft:geometry"][0]
    except Exception as ex:  # noqa: BLE001
        return [f"bbmodel 읽기 실패: {ex}"]
    exp = bb2geo.convert(bb)["minecraft:geometry"][0]
    r = lambda v: [round(float(t), 2) for t in v] if v else v
    a = {b["name"]: (r(b.get("pivot")), sorted(r(c.get("origin")) for c in b.get("cubes", []))) for b in exp["bones"]}
    b = {x["name"]: (r(x.get("pivot")), sorted(r(c.get("origin")) for c in x.get("cubes", []))) for x in geo.get("bones", [])}
    if a != b:
        return ["geo 가 bbmodel 을 Blockbench 로 내보낸 것과 다름 — Blockbench 에서 다시 내보내거나 import --bbmodel 로 넣기"]
    return []


def _old_check_bbmodel(path, geo_path):
    try:
        bb = json.load(open(path, encoding="utf-8"))
        geo = json.load(open(geo_path, encoding="utf-8"))["minecraft:geometry"][0]
    except Exception as ex:  # noqa: BLE001
        return [f"bbmodel 읽기 실패: {ex}"]
    a = sorted(g.get("name") for g in bb.get("groups", []))
    b = sorted(x.get("name") for x in geo.get("bones", []))
    return [] if a == b else [f"bbmodel 그룹 {a} ≠ geo 본 {b} (Blockbench 에서 다시 내보내기)"]


def cmd_validate(src, m):
    bad = 0
    ids = set()
    for e in m.get("entries", []):
        i = e.get("id", "")
        errs, warns = [], []
        if not ID_RE.match(i):
            errs.append("id 는 소문자·숫자·_ 만")
        elif not i.startswith("test_"):
            errs.append("id 는 test_ 로 시작 (기존 아이템 id 와 겹치지 않게)")
        if i in ids:
            errs.append("id 중복")
        ids.add(i)
        if e.get("slot") not in SLOTS:
            errs.append(f"slot '{e.get('slot')}' (helmet·chestplate·leggings·boots)")
        if not isinstance(e.get("grade"), int) or not 1 <= e["grade"] <= 10:
            errs.append("grade 는 1~10")
        r = resolved(m, e)
        if not r["model"].startswith("geo/") or not r["model"].endswith(".geo.json"):
            errs.append("model 은 geo/…/*.geo.json (GeckoLib 규칙)")
        if r["animation"] and not r["animation"].startswith("animations/"):
            errs.append("animation 은 animations/… (GeckoLib 규칙)")
        f = current_files(src, i)
        for k in ("geo", "texture", "icon"):
            if not os.path.isfile(f[k]):
                errs.append(f"current 에 {os.path.basename(f[k])} 없음")
        tex = None
        if os.path.isfile(f["texture"]):
            w, h, semi = png_info(f["texture"])
            tex = (w, h)
            if semi:
                warns.append(f"텍스처 반투명 픽셀 {semi}개 — 갑옷은 잘라내기(cutout) 렌더라 0 또는 255 로만 보임")
        if os.path.isfile(f["icon"]):
            w, h, _ = png_info(f["icon"])
            if w != h or w not in (16, 32, 64):
                warns.append(f"아이콘 {w}×{h} (16·32·64 정사각형 권장)")
        if os.path.isfile(f["geo"]):
            ge, gw = check_geo(f["geo"], tex)
            errs += ge
            warns += gw
        if os.path.isfile(f["bbmodel"]) and os.path.isfile(f["geo"]):
            warns += check_bbmodel(f["bbmodel"], f["geo"])
        cur_rev = open(os.path.join(src, i, "current", "REVISION")).read().strip() \
            if os.path.isfile(os.path.join(src, i, "current", "REVISION")) else "?"
        if cur_rev != e.get("revision"):
            warns.append(f"current 는 {cur_rev}, manifest 는 {e.get('revision')}")
        print(f"[{'오류' if errs else '통과'}] {i}  ({e.get('class')} {e.get('grade')}급 {e.get('slot')}, {e.get('revision')})")
        for x in errs:
            print("   오류: " + x)
        for x in warns:
            print("   주의: " + x)
        bad += bool(errs)
    print(f"\n항목 {len(ids)}개, 오류 있는 항목 {bad}개")
    return 1 if bad else 0


def cmd_list(src, m):
    for e in m.get("entries", []):
        print(f"{e['id']:28s} {e.get('class', ''):8s} {e.get('grade', ''):>2}급 {e.get('slot', ''):10s} "
              f"활성 {e.get('revision')}  보관 {', '.join(revisions(src, e['id'])) or '-'}")
    return 0


def entry(m, i):
    for e in m.get("entries", []):
        if e["id"] == i:
            return e
    sys.exit(f"manifest 에 '{i}' 없음")


def cmd_snapshot(src, m, i, note):
    e = entry(m, i)
    revs = revisions(src, i)
    nxt = f"v{(int(revs[-1][1:]) + 1 if revs else 1):02d}"
    d = os.path.join(src, i, "revisions", nxt)
    shutil.copytree(os.path.join(src, i, "current"), d)
    with open(os.path.join(d, "NOTES.md"), "w", encoding="utf-8") as f:
        f.write(f"# {nxt}\n\n{note or '(메모 없음)'}\n")
    with open(os.path.join(d, "REVISION"), "w") as f:
        f.write(nxt + "\n")
    with open(os.path.join(src, i, "current", "REVISION"), "w") as f:
        f.write(nxt + "\n")
    e["revision"] = nxt
    save_manifest(src, m)
    print(f"{i}: current → revisions/{nxt} 보관, 활성 {nxt}")
    return 0


def cmd_use(src, m, i, rev):
    e = entry(m, i)
    d = os.path.join(src, i, "revisions", rev)
    if not os.path.isdir(d):
        sys.exit(f"{i} 에 {rev} 없음 (보관: {', '.join(revisions(src, i))})")
    cur = os.path.join(src, i, "current")
    shutil.rmtree(cur, ignore_errors=True)
    shutil.copytree(d, cur)
    with open(os.path.join(cur, "REVISION"), "w") as f:
        f.write(rev + "\n")
    e["revision"] = rev
    save_manifest(src, m)
    print(f"{i}: {rev} → current (그다음 deploy 로 게임/리소스팩에 넣기)")
    return 0


def cmd_import(src, m, i, opts, note):
    import base64
    sys.path.insert(0, HERE)
    import bb2geo
    entry(m, i)
    revs = revisions(src, i)
    nxt = f"v{(int(revs[-1][1:]) + 1 if revs else 1):02d}"
    d = os.path.join(src, i, "revisions", nxt)
    os.makedirs(d)
    names = {"geo": f"{i}.geo.json", "texture": f"{i}.png", "icon": f"{i}_icon.png", "bbmodel": f"{i}.bbmodel"}
    for k, name in names.items():
        if opts.get(k):
            shutil.copyfile(opts[k], os.path.join(d, name))
    made = []
    bb = json.load(open(os.path.join(d, names["bbmodel"]), encoding="utf-8-sig"))
    if not opts.get("geo"):
        with open(os.path.join(d, names["geo"]), "w", encoding="utf-8") as f:
            json.dump(bb2geo.convert(bb, f"geometry.fantasyrpg.test_armor.{i}"), f, indent=1)
        made.append("geo(bb2geo)")
    if not opts.get("texture"):
        src_png = bb["textures"][0]["source"].split(",", 1)[1]
        with open(os.path.join(d, names["texture"]), "wb") as f:
            f.write(base64.b64decode(src_png))
        made.append("텍스처(bbmodel 안)")
    if not opts.get("icon"):
        import preview
        preview.icon(bb, 32).save(os.path.join(d, names["icon"]))
        made.append("아이콘(3D 앞모습)")
    with open(os.path.join(d, "NOTES.md"), "w", encoding="utf-8") as f:
        f.write(f"# {nxt}\n\n{note or '(메모 없음)'}\n\n" + (f"만든 것: {', '.join(made)}\n" if made else ""))
    with open(os.path.join(d, "REVISION"), "w") as f:
        f.write(nxt + "\n")
    print(f"{i}: 받은 파일 → revisions/{nxt}" + (f" ({', '.join(made)})" if made else ""))
    return cmd_use(src, m, i, nxt)


def deploy_assets(src, m, assets_root):
    """current → <assets_root>/<namespace>/… (모델·텍스처·아이콘·아이템 모델)"""
    ns = m.get("namespace", "fantasyrpg")
    base = os.path.join(assets_root, ns)
    n = 0
    for e in m.get("entries", []):
        i, r, f = e["id"], resolved(m, e), current_files(src, e["id"])
        for k_src, k_dst in (("geo", "model"), ("texture", "texture"), ("icon", "icon")):
            dst = os.path.join(base, *r[k_dst].split("/"))
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copyfile(f[k_src], dst)
            n += 1
        anim = os.path.join(src, i, "current", f"{i}.animation.json")
        if r["animation"] and os.path.isfile(anim):
            dst = os.path.join(base, *r["animation"].split("/"))
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copyfile(anim, dst)
            n += 1
        icon_ref = r["icon"][len("textures/"):-len(".png")] if r["icon"].startswith("textures/") else r["icon"]
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ns}:{icon_ref}"}}
        p = os.path.join(base, "models", "item", f"{i}.json")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(model, fh, indent=2)
            fh.write("\n")
        n += 1
    return n


def cmd_deploy(src, m, where, target):
    if where == "repo":
        res = os.path.join(REPO, "core-mod", "src", "main", "resources")
        n = deploy_assets(src, m, os.path.join(res, "assets"))
        os.makedirs(os.path.join(res, "fantasyrpg_devtest"), exist_ok=True)
        shutil.copyfile(os.path.join(src, "manifest.json"), os.path.join(res, "fantasyrpg_devtest", "armor_manifest.json"))
        print(f"모드 소스에 {n}개 + 목록 복사. Gradle 빌드 뒤 jar 에 들어감")
        return 0
    if not target:
        sys.exit("폴더를 지정")
    if where == "instance":
        pack = os.path.join(target, "resourcepacks", "fantasyrpg_devtest")
        cfg = os.path.join(target, "config", "fantasyrpg-devtest")
        os.makedirs(cfg, exist_ok=True)
        shutil.copyfile(os.path.join(src, "manifest.json"), os.path.join(cfg, "armor_manifest.json"))
        print(f"목록 → {cfg} (새 항목은 게임을 다시 켜야 아이템이 생김)")
    else:
        pack = target
    os.makedirs(pack, exist_ok=True)
    with open(os.path.join(pack, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump({"pack": {"pack_format": PACK_FORMAT, "description": "fantasyrpg 테스트 장비 작업본 (devtest)"}},
                  f, ensure_ascii=False, indent=2)
    n = deploy_assets(src, m, os.path.join(pack, "assets"))
    print(f"리소스팩 {pack} 에 {n}개. 게임에서 리소스팩을 켜 두고 F3+T (또는 /fantasyrpgtest armor reload)")
    return 0


def main(argv):
    argv = list(argv)
    src = find_src(argv)
    m = load_manifest(src)
    if not argv or argv[0] in ("-h", "--help", "help"):
        print(__doc__)
        return 0
    c = argv[0]
    if c == "list":
        return cmd_list(src, m)
    if c == "validate":
        return cmd_validate(src, m)
    if c == "snapshot" and len(argv) >= 2:
        note = argv[argv.index("--note") + 1] if "--note" in argv else ""
        return cmd_snapshot(src, m, argv[1], note)
    if c == "use" and len(argv) == 3:
        return cmd_use(src, m, argv[1], argv[2])
    if c == "import" and len(argv) >= 2:
        opts = {k: argv[argv.index("--" + k) + 1] for k in ("geo", "texture", "icon", "bbmodel") if "--" + k in argv}
        if "bbmodel" not in opts:
            sys.exit("import 에는 --bbmodel 이 필요 (geo·텍스처·아이콘은 없으면 bbmodel 에서 만듦)")
        note = argv[argv.index("--note") + 1] if "--note" in argv else ""
        return cmd_import(src, m, argv[1], opts, note)
    if c == "deploy" and len(argv) >= 2 and argv[1] in ("repo", "pack", "instance"):
        return cmd_deploy(src, m, argv[1], argv[2] if len(argv) > 2 else None)
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
