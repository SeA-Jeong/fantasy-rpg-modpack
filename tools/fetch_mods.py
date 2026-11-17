#!/usr/bin/env python3
"""modpack/mods.lock.json 에 적힌 외부 모드 jar 를 내려받는다 (표준 라이브러리만).

사용:  python3 tools/fetch_mods.py modpack/mods.lock.json <출력 폴더> [--max-stage N]

- source "modrinth": Modrinth API 에서 slug·버전 번호·NeoForge·MC 버전이 맞는 파일을 찾고 sha512 를 검사한다.
- source "maven": maven-metadata.xml 에서 latest_prefix 로 시작하는 가장 높은 버전을 고른다. 실패하면 fallback 을 쓴다.
- source "url": 주소 그대로 받는다.
- 결과 목록은 <출력 폴더>/resolved.json 에 남긴다 (CI 보고용).
"""
import hashlib
import json
import os
import re
import sys
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

UA = {"User-Agent": "SeA-Jeong/fantasy-rpg-modpack fetch_mods (CI)"}


def get(url, timeout=60):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read()


def vkey(v):
    return tuple(int(p) for p in re.findall(r"\d+", v))


def fetch_modrinth(entry, mc, out):
    q = urllib.parse.urlencode({"loaders": json.dumps(["neoforge"]), "game_versions": json.dumps([mc])})
    versions = json.loads(get(f"https://api.modrinth.com/v2/project/{entry['slug']}/version?{q}"))
    match = [v for v in versions if v["version_number"] == entry["version"]]
    if not match:
        have = ", ".join(v["version_number"] for v in versions[:5])
        raise RuntimeError(f"버전 {entry['version']} 없음 (최근: {have})")
    files = match[0]["files"]
    f = next((x for x in files if x.get("primary")), files[0])
    data = get(f["url"], timeout=300)
    if hashlib.sha512(data).hexdigest() != f["hashes"]["sha512"]:
        raise RuntimeError("sha512 불일치")
    path = os.path.join(out, f["filename"])
    with open(path, "wb") as fh:
        fh.write(data)
    return {"file": f["filename"], "version": entry["version"], "from": f["url"]}


def fetch_maven(entry, out):
    base = f"{entry['repo'].rstrip('/')}/{entry['group'].replace('.', '/')}/{entry['artifact']}"
    meta = ET.fromstring(get(f"{base}/maven-metadata.xml"))
    versions = [v.text for v in meta.iter("version") if v.text and v.text.startswith(entry.get("latest_prefix", ""))]
    if not versions:
        raise RuntimeError("조건에 맞는 버전 없음")
    ver = max(versions, key=vkey)
    name = f"{entry['artifact']}-{ver}.jar"
    data = get(f"{base}/{ver}/{name}", timeout=300)
    with open(os.path.join(out, name), "wb") as fh:
        fh.write(data)
    return {"file": name, "version": ver, "from": f"{base}/{ver}/{name}"}


def fetch_url(entry, out):
    name = entry["url"].rsplit("/", 1)[-1]
    data = get(entry["url"], timeout=300)
    with open(os.path.join(out, name), "wb") as fh:
        fh.write(data)
    return {"file": name, "version": "(주소 고정)", "from": entry["url"]}


def fetch(entry, mc, out):
    src = entry["source"]
    if src == "modrinth":
        return fetch_modrinth(entry, mc, out)
    if src == "maven":
        return fetch_maven(entry, out)
    if src == "url":
        return fetch_url(entry, out)
    raise RuntimeError(f"알 수 없는 source {src}")


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("lock")
    ap.add_argument("out")
    ap.add_argument("--max-stage", type=int, default=99)
    a = ap.parse_args(argv)
    lock = json.load(open(a.lock, encoding="utf-8"))
    os.makedirs(a.out, exist_ok=True)
    resolved, failed = [], []
    for e in lock["mods"]:
        if e.get("stage", 0) > a.max_stage:
            continue
        try:
            r = fetch(e, lock["minecraft"], a.out)
        except Exception as ex:  # noqa: BLE001 — 실패 이유를 그대로 보고
            if "fallback" in e:
                try:
                    r = fetch(e["fallback"], lock["minecraft"], a.out)
                    r["note"] = f"기본 경로 실패({ex}) → 대체 경로"
                except Exception as ex2:  # noqa: BLE001
                    failed.append(f"{e['name']}: {ex} / 대체: {ex2}")
                    continue
            else:
                failed.append(f"{e['name']}: {ex}")
                continue
        r["name"] = e["name"]
        r["stage"] = e.get("stage")
        resolved.append(r)
        print(f"받음  {e['name']:<22} {r['version']:<26} {r['file']}" + (f"  ({r['note']})" if "note" in r else ""))
    with open(os.path.join(a.out, "resolved.json"), "w", encoding="utf-8") as fh:
        json.dump({"resolved": resolved, "failed": failed}, fh, ensure_ascii=False, indent=2)
    for f in failed:
        print("실패 ", f)
    print(f"\n결과: 받음 {len(resolved)}개, 실패 {len(failed)}개")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
