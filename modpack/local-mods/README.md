# 파일로 넣는 외부 모드

Modrinth·CurseForge 에 없고 소스도 없어 주소만 적거나 빌드할 수 없는 외부 모드 jar 를 여기에 둡니다. 통합 팩(.mrpack)의 `overrides/mods/` 에 그대로 들어갑니다.

지금 들어 있는 것:
| jar | 모드 | 라이선스 | 출처 |
|---|---|---|---|
| `combattraces-1.1.0.jar` | Combat Traces 1.1.0 (CappleApple) — Better Combat 무기 궤적·타격 효과. NeoForge ≥ 21.1.248, MC 1.21.1, Better Combat [2.4.0, 2.5) 선택 의존. 믹스인은 클라이언트 전용. 클라이언트·서버 둘 다 설치 | CC BY-NC-SA 4.0 + 모드팩·서버 추가 허가(모드팩에 넣어 배포 가능, 단독 유료 판매 금지) | CurseForge, 2026-11-21 사용자가 받아 줌. sha1 `dc99c572cd8a4bec8ea50cc1fda69f32949ba2b2` |

- `fantasybosses`(3장 보스 라디키온)는 2026-11-20 0.7.2.2 → 0.7.5.1 을 여기 jar 로 넣었다가, 같은 날 소스를 `magic-city-mod` 저장소 `fantasybosses/` 로 옮겨 **고정 커밋으로 빌드**하게 바꿈(`modpack/mods.lock.json` 의 `fantasybosses`, `tools/build_fantasybosses.sh`).
- 새 jar 를 넣으면 이 파일, `modpack/mods.md`, CI 의 모드 수 검사(`build.yml` 의 `-eq`)와 jar 를 모으는 단계(`cp modpack/local-mods/*.jar`)를 같이 고칩니다.
