# 파일로 넣는 외부 모드

Modrinth·CurseForge 에 없고 소스도 없어 주소만 적거나 빌드할 수 없는 외부 모드 jar 를 여기에 둡니다. 통합 팩(.mrpack)의 `overrides/mods/` 에 그대로 들어갑니다.

지금은 **비어 있습니다.**
- `fantasybosses`(3장 보스 라디키온)는 2026-11-20 0.7.2.2 → 0.7.5.1 을 여기 jar 로 넣었다가, 같은 날 소스를 `magic-city-mod` 저장소 `fantasybosses/` 로 옮겨 **고정 커밋으로 빌드**하게 바꿈(`modpack/mods.lock.json` 의 `fantasybosses`, `tools/build_fantasybosses.sh`).
- 새 jar 를 넣으면 이 파일, `modpack/mods.md`, CI 의 모드 수 검사(`build.yml` 의 `-eq`)와 jar 를 모으는 단계(`cp modpack/local-mods/*.jar`)를 같이 고칩니다.
