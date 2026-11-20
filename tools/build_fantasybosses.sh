#!/usr/bin/env bash
# 챕터 보스 모드 fantasybosses 를 mods.lock.json 에 고정한 커밋으로 빌드해 <출력 폴더>에 jar 를 넣는다 (CI용, 2026-11-20).
# 사용: tools/build_fantasybosses.sh modpack/mods.lock.json <출력 폴더>
set -euo pipefail
LOCK="$1"; OUT="$(mkdir -p "$2" && cd "$2" && pwd)"
REPO=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['fantasybosses']['repo'])" "$LOCK")
COMMIT=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['fantasybosses']['commit'])" "$LOCK")
WORK="$(mktemp -d)"
git clone -q "$REPO" "$WORK/src"
git -C "$WORK/src" checkout -q "$COMMIT"
(cd "$WORK/src" && chmod +x gradlew && ./gradlew -p fantasybosses build -x test --no-daemon -q)
find "$WORK/src/fantasybosses/build/libs" -name 'fantasybosses-*.jar' ! -name '*-sources.jar' -exec cp {} "$OUT/" \;
ls "$OUT"/fantasybosses-*.jar
