#!/usr/bin/env python3
"""통합 테스트 팩을 Modrinth 모드팩 형식(.mrpack)으로 만든다 (표준 라이브러리만).

사용:
  python3 tools/build_mrpack.py --lock modpack/mods.lock.json --resolved <fetch_mods 결과>/resolved.json \
      --ours <우리 모드 jar 폴더> --datapacks modpack/datapacks --guide docs/TESTING.md \
      --version 0.2.0 --out 마법왕국대륙-테스트팩.mrpack

- 외부 모드 18개는 jar 를 넣지 않고 내려받을 주소와 sha1·sha512·크기만 적는다 (launcher 가 직접 받음).
- 우리 모드 3개(fantasyrpg·magiccity·royalqol)와 modpack/local-mods/ 의 외부 jar(주소가 없는 모드)는 overrides/mods/ 에 jar 로 넣는다.
- 테스트 데이터팩은 overrides/datapacks/ 에 넣는다 (월드를 만들 때 직접 고름).
- Prism Launcher 등에서 "가져오기"로 열면 Minecraft·NeoForge 버전까지 맞춘 인스턴스가 만들어진다.
"""
import argparse
import json
import os
import sys
import zipfile

# Modrinth 형식에서 launcher 가 경고 없이 받는 주소 (그 밖의 주소는 launcher 가 확인을 물을 수 있다)
WHITELIST = ("https://cdn.modrinth.com/", "https://github.com/", "https://raw.githubusercontent.com/", "https://gitlab.com/")


def build(lock, resolved, ours_dir, datapacks_dir, guide, version, out):
    files, others = [], []
    for r in resolved["resolved"]:
        url = r["from"]
        if not url.startswith(WHITELIST):
            others.append(f"{r['name']}: {url}")
        files.append({
            "path": f"mods/{r['file']}",
            "hashes": {"sha1": r["sha1"], "sha512": r["sha512"]},
            "env": {"client": "required", "server": "required"},
            "downloads": [url],
            "fileSize": r["size"],
        })
    if resolved.get("failed"):
        raise SystemExit("외부 모드 받기에 실패한 항목이 있어 팩을 만들지 않음: " + "; ".join(resolved["failed"]))
    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": version,
        "name": f"마법 왕국 대륙 통합 테스트 팩 {version}",
        "summary": "magiccity + fantasyrpg + royalqol + 외부 보스 모드(파일 포함) + 1차 외부 모드 18개 (테스트용)",
        "files": sorted(files, key=lambda f: f["path"]),
        "dependencies": {"minecraft": lock["minecraft"], "neoforge": lock["neoforge"]},
    }
    ours = sorted(f for f in os.listdir(ours_dir) if f.endswith(".jar"))
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("modrinth.index.json", json.dumps(index, ensure_ascii=False, indent=2))
        for f in ours:
            z.write(os.path.join(ours_dir, f), f"overrides/mods/{f}")
        if datapacks_dir:
            for name in ("fantasyrpg_content", "fantasyrpg_test", "magiccity_boss_stats_sample"):
                base = os.path.join(datapacks_dir, name)
                for root, _, names in os.walk(base):
                    for n in names:
                        full = os.path.join(root, n)
                        z.write(full, "overrides/datapacks/" + os.path.relpath(full, datapacks_dir).replace(os.sep, "/"))
        if guide:
            z.write(guide, "overrides/테스트방법.md")
    print(f"만듦: {out}")
    print(f"  외부 모드 {len(files)}개 (주소만), 우리 모드 {len(ours)}개 (파일 포함): {', '.join(ours)}")
    for o in others:
        print(f"  주의: 기본 허용 목록 밖 주소 — launcher 가 받을지 물을 수 있음: {o}")
    return index


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--lock", required=True)
    ap.add_argument("--resolved", required=True)
    ap.add_argument("--ours", required=True)
    ap.add_argument("--datapacks")
    ap.add_argument("--guide")
    ap.add_argument("--version", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args(argv)
    lock = json.load(open(a.lock, encoding="utf-8"))
    resolved = json.load(open(a.resolved, encoding="utf-8"))
    build(lock, resolved, a.ours, a.datapacks, a.guide, a.version, a.out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
