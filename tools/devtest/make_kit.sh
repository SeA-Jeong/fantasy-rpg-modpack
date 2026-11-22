#!/usr/bin/env bash
# 테스트 팩에 넣을 개발 테스트 도구 묶음을 만든다: <출력 폴더>/{config,resourcepacks,devtest}
# 사용: bash tools/devtest/make_kit.sh <출력 폴더>
set -euo pipefail
out="$1"
mkdir -p "$out/devtest/source_assets"
cp -r modpack/devtest_kit/. "$out/"
cp tools/devtest/test_armor.py "$out/devtest/"
cp -r source_assets/test_armor "$out/devtest/source_assets/"
cp docs/devtest/README.md "$out/devtest/README.md"
find "$out" -type f | wc -l
