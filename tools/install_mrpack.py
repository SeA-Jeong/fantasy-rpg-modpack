#!/usr/bin/env python3
""".mrpack 을 launcher 처럼 풀어서 설치한다 (CI 검증용, 표준 라이브러리만).

사용:  python3 tools/install_mrpack.py <팩.mrpack> <설치 폴더>

modrinth.index.json 의 각 파일을 적힌 주소에서 받아 sha1·sha512·크기를 확인하고, overrides/ 를 덮어쓴다.
launcher 가 할 일을 그대로 해 보는 것이라, 이 검사가 통과하면 주소·해시가 맞는 팩이다.
"""
import hashlib
import json
import os
import sys
import urllib.request
import zipfile

UA = {"User-Agent": "SeA-Jeong/fantasy-rpg-modpack install_mrpack (CI)"}


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 2
    pack, dest = argv
    os.makedirs(dest, exist_ok=True)
    errors = []
    with zipfile.ZipFile(pack) as z:
        index = json.loads(z.read("modrinth.index.json"))
        print(f"{index['name']} — Minecraft {index['dependencies'].get('minecraft')}, NeoForge {index['dependencies'].get('neoforge')}")
        for f in index["files"]:
            root = os.path.abspath(dest)
            path = os.path.abspath(os.path.join(dest, f["path"]))
            if not path.startswith(root + os.sep):
                errors.append(f"{f['path']}: 설치 폴더 밖 경로")
                continue
            data = None
            for url in f["downloads"]:
                try:
                    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=300) as r:
                        data = r.read()
                    break
                except Exception as e:  # noqa: BLE001
                    print(f"  받기 실패 {url}: {e}")
            if data is None:
                errors.append(f"{f['path']}: 모든 주소에서 받기 실패")
                continue
            if hashlib.sha1(data).hexdigest() != f["hashes"]["sha1"] or hashlib.sha512(data).hexdigest() != f["hashes"]["sha512"]:
                errors.append(f"{f['path']}: 해시 불일치")
                continue
            if f.get("fileSize") is not None and f["fileSize"] != len(data):
                errors.append(f"{f['path']}: 크기 불일치")
                continue
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as fh:
                fh.write(data)
            print(f"  받음 {f['path']}")
        for name in z.namelist():
            if name.startswith("overrides/") and not name.endswith("/"):
                target = os.path.join(dest, name[len("overrides/"):])
                os.makedirs(os.path.dirname(target), exist_ok=True)
                with open(target, "wb") as fh:
                    fh.write(z.read(name))
                if name.startswith("overrides/mods/"):
                    print(f"  포함 {name[len('overrides/'):]}")
    for e in errors:
        print("오류:", e)
    print(f"결과: 파일 {len(index['files'])}개 중 오류 {len(errors)}건")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
