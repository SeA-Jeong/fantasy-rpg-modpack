#!/usr/bin/env bash
# 우리 편의 모드 royalqol 을 mods.lock.json 에 고정한 커밋으로 빌드해 <출력 폴더>에 jar 를 넣는다 (CI용).
# 사용: tools/build_royalqol.sh modpack/mods.lock.json <출력 폴더>
set -euo pipefail
LOCK="$1"; OUT="$(mkdir -p "$2" && cd "$2" && pwd)"
REPO=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['royalqol']['repo'])" "$LOCK")
COMMIT=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['royalqol']['commit'])" "$LOCK")
WORK="$(mktemp -d)"
git clone -q "$REPO" "$WORK/src"
git -C "$WORK/src" checkout -q "$COMMIT"
(cd "$WORK/src" && chmod +x gradlew && ./gradlew -p royalqol build -x test --no-daemon -q)
find "$WORK/src/royalqol/build/libs" -name 'royalqol-*.jar' ! -name '*-sources.jar' -exec cp {} "$OUT/" \;
ls "$OUT"/royalqol-*.jar
