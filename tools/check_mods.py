#!/usr/bin/env python3
"""mods 폴더의 jar 들이 서로의 필수 의존성과 버전 범위를 만족하는지 검사한다.

사용:  python3 tools/check_mods.py <mods 폴더> [--neoforge 21.1.266] [--minecraft 1.21.1]

- 각 jar 의 META-INF/neoforge.mods.toml 을 읽어 modId·버전·의존성을 모은다 (jar 안의 jar-in-jar 는 META-INF/jarjar 까지 본다).
- required 의존성이 없거나 버전 범위를 벗어나면 오류, optional 의존성이 설치돼 있는데 범위를 벗어나면 오류
  (NeoForge 는 이 경우 게임을 시작하지 않는다), incompatible 이 함께 있으면 오류.
- ${...} 처럼 치환되지 않은 값은 "확인 불가"로 표시한다.
- 버전 비교는 Maven 범위 표기([a,b), (a,] 등)와 NeoForge 버전 문자열을 숫자 우선으로 비교한다. 접미사(+1.21.1, -beta 등)는 무시한다.

이 도구는 jar 파일만 읽는다. 게임 실행(mixin 충돌, 실제 동작)은 검증하지 않는다.
"""
import io
import re
import sys
import zipfile

try:
    import tomllib
except ImportError:  # Python < 3.11
    tomllib = None

BUILTIN = {"neoforge", "minecraft", "java"}


def version_key(v):
    """'2.4.0+1.21.1-neoforge' → (2, 4, 0). 숫자 덩어리를 앞에서부터 비교."""
    v = re.split(r"[+\-]", v.strip(), maxsplit=1)[0]
    return tuple(int(p) if p.isdigit() else 0 for p in re.findall(r"\d+|[A-Za-z]+", v))


def cmp(a, b):
    ka, kb = version_key(a), version_key(b)
    n = max(len(ka), len(kb))
    ka += (0,) * (n - len(ka))
    kb += (0,) * (n - len(kb))
    return (ka > kb) - (ka < kb)


def in_range(version, spec):
    """Maven 범위 표기. 여러 구간은 쉼표로 이어질 수 있음: [1,2),[3,)"""
    if spec is None or spec.strip() in ("", "*"):
        return True
    if "${" in spec:
        return None
    spec = spec.strip()
    if spec[0] not in "[(":
        return cmp(version, spec) >= 0  # 단일 값 = 최솟값으로 취급 (NeoForge 관례)
    for m in re.finditer(r"([\[(])\s*([^,\])]*)\s*,?\s*([^\])]*)\s*([\])])", spec):
        lo_inc, lo, hi, hi_inc = m.group(1) == "[", m.group(2).strip(), m.group(3).strip(), m.group(4) == "]"
        single = "," not in m.group(0)
        if single:
            if cmp(version, lo) == 0:
                return True
            continue
        ok = True
        if lo:
            c = cmp(version, lo)
            ok &= c > 0 or (lo_inc and c == 0)
        if hi:
            c = cmp(version, hi)
            ok &= c < 0 or (hi_inc and c == 0)
        if ok:
            return True
    return False


def parse_toml(text):
    if tomllib is not None:
        return tomllib.loads(text)
    raise SystemExit("Python 3.11 이상이 필요합니다 (tomllib)")


def read_jar(data, name, out):
    try:
        z = zipfile.ZipFile(io.BytesIO(data))
    except zipfile.BadZipFile:
        return
    with z:
        names = set(z.namelist())
        version_from_manifest = None
        if "META-INF/MANIFEST.MF" in names:
            mf = z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
            m = re.search(r"Implementation-Version:\s*(\S+)", mf)
            version_from_manifest = m.group(1) if m else None
        if "META-INF/neoforge.mods.toml" in names:
            toml = parse_toml(z.read("META-INF/neoforge.mods.toml").decode("utf-8", "replace"))
            deps = toml.get("dependencies", {})
            for mod in toml.get("mods", []):
                mid = mod.get("modId")
                ver = str(mod.get("version", "?"))
                if ver == "${file.jarVersion}" and version_from_manifest:
                    ver = version_from_manifest
                out.append({"modId": mid, "version": ver, "jar": name, "deps": deps.get(mid, [])})
        for inner in names:
            if inner.startswith("META-INF/jarjar/") and inner.endswith(".jar"):
                read_jar(z.read(inner), name + "!/" + inner.split("/")[-1], out)


def main(argv):
    import argparse
    import os
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("mods_dir")
    ap.add_argument("--neoforge", default="21.1.266")
    ap.add_argument("--minecraft", default="1.21.1")
    a = ap.parse_args(argv)

    mods = []
    for f in sorted(os.listdir(a.mods_dir)):
        if f.endswith(".jar"):
            with open(os.path.join(a.mods_dir, f), "rb") as fh:
                read_jar(fh.read(), f, mods)

    present = {m["modId"]: m for m in mods}
    present.setdefault("neoforge", {"modId": "neoforge", "version": a.neoforge, "jar": "(로더)"})
    present.setdefault("minecraft", {"modId": "minecraft", "version": a.minecraft, "jar": "(게임)"})

    errors, unknown = [], []
    print(f"모드 {len(mods)}개 (jar-in-jar 포함), NeoForge {a.neoforge}, Minecraft {a.minecraft}\n")
    for m in sorted(mods, key=lambda x: x["modId"] or ""):
        print(f"- {m['modId']} {m['version']}  ({m['jar']})")
        for d in m["deps"]:
            did = d.get("modId")
            typ = str(d.get("type", "required" if d.get("mandatory", True) else "optional")).lower()
            rng = d.get("versionRange")
            dep = present.get(did)
            if typ == "required":
                if dep is None:
                    errors.append(f"{m['modId']}: 필수 의존성 {did} {rng} 없음")
                    continue
            elif typ == "incompatible":
                if dep is not None:
                    errors.append(f"{m['modId']}: {did} 와 함께 쓸 수 없음")
                continue
            elif dep is None:
                continue  # optional/discouraged 미설치
            ok = in_range(dep["version"], rng)
            if ok is None or dep["version"].startswith("${"):
                unknown.append(f"{m['modId']} → {did} {rng} (설치 {dep['version']}): 버전 확인 불가")
            elif not ok:
                errors.append(f"{m['modId']} → {did}: 설치 {dep['version']} 이(가) 범위 {rng} 밖 ({typ})")

    print()
    for u in unknown:
        print("확인 불가:", u)
    for e in errors:
        print("오류:", e)
    print(f"\n결과: 오류 {len(errors)}건, 확인 불가 {len(unknown)}건")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
