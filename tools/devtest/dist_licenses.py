#!/usr/bin/env python3
"""배포물에 넣을 외부 자산 크레딧(LICENSES/)과 출처 목록(ASSET_PROVENANCE.csv)을 만든다 — 표준 라이브러리만.

실제로 게임에 들어간 것만 적는다: source_assets/test_armor/manifest.json 의 항목 × build_report_v06.json 의 원본.
원본 고정 커밋·URL·라이선스는 source_assets/armor_library/source_registry.json, 원본 해시는 armor_library/upstream 에서 계산.

출력 (둘 다 저장소에 커밋, CI 가 다시 만들어 비교):
  modpack/devtest_kit/LICENSES/<source>/{LICENSE,README.md}  + modpack/devtest_kit/ASSET_PROVENANCE.csv   (테스트 팩·mrpack 최상위)
  core-mod/src/main/resources/licenses/ASSET_PROVENANCE.csv                                            (jar 안)
사용: python3 tools/devtest/dist_licenses.py [--check]   (--check: 다시 만든 결과가 커밋된 것과 다르면 종료 코드 1)
"""
import csv
import hashlib
import io
import json
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
LIB = os.path.join(REPO, "source_assets", "armor_library")
SRC = os.path.join(REPO, "source_assets", "test_armor")
KIT = os.path.join(REPO, "modpack", "devtest_kit")
JAR_LIC = os.path.join(REPO, "core-mod", "src", "main", "resources", "licenses")
AUTHOR = {"cleann": "cleannrooster"}
HEADER = ["game_file", "item_id", "revision", "source", "author", "license", "repository", "pinned_commit",
          "original_path", "original_sha256", "original_url", "changes"]


def sha(path):
    return hashlib.sha256(open(path, "rb").read()).hexdigest()


def build():
    reg = json.load(open(os.path.join(LIB, "source_registry.json"), encoding="utf-8"))
    rep = json.load(open(os.path.join(SRC, "build_report_v06.json"), encoding="utf-8"))
    man = json.load(open(os.path.join(SRC, "manifest.json"), encoding="utf-8"))
    ns = man.get("namespace", "fantasyrpg")
    by_id = {}
    for key, r in rep.items():
        for slot, tid in r["made"].items():
            by_id[tid] = r
    rows, used = [], set()
    for e in man["entries"]:
        r = by_id.get(e["id"])
        if r is None:
            sys.exit(f"{e['id']}: build_report_v06.json 에 원본 기록 없음 — 출처 없는 자산은 배포하지 않음")
        source = "cleann"
        used.add(source)
        g = reg[source]
        orig = f"bb/{r['source']}"
        url = f"https://github.com/{g['repo']}/blob/{g['sha']}/{orig}"
        changes = ("4부위로 나눔(이 부위의 큐브만), 몹 리그 → GeckoLib 갑옷 본, 텍스처 " + r["texture"] + " 그대로"
                   + (", 뺀 것: " + "; ".join(r["dropped"]) if r["dropped"] else "")
                   + (", 고친 것: " + "; ".join(r["fixes"]) if r["fixes"] else ""))
        for kind, path in (("model", f"assets/{ns}/geo/test_armor/{e['id']}.geo.json"),
                           ("texture", f"assets/{ns}/textures/armor/test_armor/{e['id']}.png"),
                           ("icon", f"assets/{ns}/textures/item/test_armor/{e['id']}.png")):
            rows.append([path, e["id"], e.get("revision", ""), source, AUTHOR[source], g["license"], g["repo"], g["sha"], orig,
                         sha(os.path.join(LIB, "upstream", source, orig)), url,
                         changes if kind == "model" else ("원본 텍스처 그대로" if kind == "texture" else "모델 앞모습 렌더(preview.py)")])
    buf = io.StringIO()
    w = csv.writer(buf, lineterminator="\n")
    w.writerow(HEADER)
    w.writerows(rows)
    return buf.getvalue(), sorted(used), reg


def main(argv):
    text, used, reg = build()
    outs = {os.path.join(KIT, "ASSET_PROVENANCE.csv"): text, os.path.join(JAR_LIC, "ASSET_PROVENANCE.csv"): text}
    lic = {}
    for s in used:
        for n in ("LICENSE", "README.md"):
            p = os.path.join(LIB, "upstream", s, n)
            if os.path.isfile(p):
                lic[os.path.join(KIT, "LICENSES", reg[s]["repo"].replace("/", "__"), n)] = open(p, encoding="utf-8").read()
    outs.update(lic)
    if "--check" in argv:
        bad = [p for p, t in outs.items() if not os.path.isfile(p) or open(p, encoding="utf-8").read() != t]
        for p in bad:
            print("다름:", os.path.relpath(p, REPO))
        print("크레딧·출처 목록", "다름 — python3 tools/devtest/dist_licenses.py 로 다시 만들기" if bad else "일치", f"({len(outs)}개)")
        return 1 if bad else 0
    if os.path.isdir(os.path.join(KIT, "LICENSES")):
        shutil.rmtree(os.path.join(KIT, "LICENSES"))
    for p, t in outs.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, "w", encoding="utf-8", newline="").write(t)
    print(f"출처 {text.count(chr(10)) - 1}줄, 라이선스 {len(lic)}개 ({', '.join(used)})")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
