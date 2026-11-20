# 파일로 넣는 외부 모드

Modrinth·CurseForge 에 없어서 주소만 적을 수 없는 외부 모드 jar 를 여기에 둡니다. 통합 팩(.mrpack)의 `overrides/mods/` 에 그대로 들어갑니다.

| jar | 모드 | 용도 | 필요 | 라이선스·허락 |
|---|---|---|---|---|
| `fantasybosses-0.7.5.1.jar` (sha1 e7db0df2…) | `fantasybosses` 0.7.5.1 (2026-11-20, 0.7.2.2 대체) | 3장 보스 "세계수 잠식자 라디키온": 11마디 부위 판정·공격 6종·그로기·흡수 기관·알집·마력 균열·전용 BGM. 만든 분이 빌드한 JAR(받은 소스와 클래스·리소스 일치 확인). magiccity `data/magiccity/boss_entity/parasite.json` 으로 연결 | GeckoLib 4.7.6 이상 (팩 4.9.3) | All Rights Reserved — 사용자 결정(2026-11-20, `claude/외부보스연결_초안.md` 6절 D)으로 팩에 포함 |

- 새 jar 를 넣거나 바꾸면 이 표, `modpack/mods.md`, CI 의 모드 수 검사(`build.yml` 의 `-eq`)를 같이 고칩니다.
- 이 모드가 빠지면 3장은 magiccity 기본 보스(바닐라 바탕 세계수 기생충)로 싸웁니다.
