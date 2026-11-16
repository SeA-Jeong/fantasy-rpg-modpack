# 인계문서 (HANDOFF)

이 문서만 읽으면 누구든(사람이든 새 Claude 세션이든) 이어서 작업할 수 있도록 유지합니다.
**작업이 끝날 때마다 "현재 상태"와 "작업 기록"을 갱신합니다.**

---

## 현재 상태 (2026-11-16 기준)

| 항목 | 상태 |
|---|---|
| 기준 기획 | `docs/design/직업_전투시스템_v0.4.md` |
| 구현 진행 | 8.3장 **1단계(피해 계산 엔진) 완료** → 다음은 2단계(직업 코어 6종) |
| 플랫폼 | Minecraft 1.21.1 / NeoForge 21.1.228 / Java 21 / ModDevGradle 2.0.141 |
| CI | push마다 빌드·JUnit·테스트 팩 생성 (테스트 결과는 로그의 "테스트 합계" 줄) |
| 최신 테스트 팩 | [v0.1.0 Release](https://github.com/SeA-Jeong/fantasy-rpg-modpack/releases/tag/v0.1.0) |
| 게임 테스트 | `docs/TESTING.md`의 v0.1 체크리스트 |

### 테스트 팩 Release 만드는 법
Claude 세션에서는 태그 push가 막혀 있으므로 **수동 실행**으로 만듭니다.
- Claude: `gh api -X POST repos/SeA-Jeong/fantasy-rpg-modpack/actions/workflows/build.yml/dispatches -f ref=main -f 'inputs[release_tag]=v0.X.Y'`
- 사람: Actions 탭 → core-mod build → Run workflow → `release_tag`에 버전 입력
- 버전을 올릴 때 `core-mod/gradle.properties`의 `mod_version`도 맞춥니다.

### 동작하는 것
- 기획 4장 피해 공식 전체: W → 계수 → 스킬 강화 → 관통·저항 → 치명타 → 최종 보정
- 몬스터 저항 데이터 로딩 (`mob_resistances/*.json`, `/reload` 반영, `_`로 시작하는 파일은 무시)
- 필수 챕터 보스 저항 차이 검사 (기본 15%p 초과 시 경고, 20%p 초과 시 오류 로그)
- RPG 피해 유형 `fantasyrpg:rpg`: 바닐라 방어구·보호 마법부여·무적 시간을 무시
- 커맨드: `/rpg simulate`, `/rpg resist`, `/rpg hit` (권한 2)

### 아직 없는 것 (다음 작업 후보)
- 플레이어 기본 공격·스킬이 RPG 피해를 쓰는 연결 (지금은 커맨드로만 RPG 피해 발생)
- 치명타 확률 판정 (지금은 배율만 입력받음. 확률·상한은 5.4장, 8.4장 미확정 값)
- 직업, 코어 자원, 스킬트리, SP, 장비 W 전부

---

## 코드 지도

| 위치 | 내용 |
|---|---|
| `core-mod/.../FantasyRpgCore.java` | 진입점. 설정 등록, 리로드 리스너·커맨드 이벤트 연결 |
| `core-mod/.../RpgConfig.java` | `config/fantasyrpg-common.toml` (보스 저항 차이 상한) |
| `combat/DamageCalculator` | **순수 계산** (MC 비의존). 공식은 클래스 주석 참고 |
| `combat/DamageInput`, `Resistances`, `DamageBreakdown` | 입력·저항·단계별 결과 레코드 |
| `combat/RpgDamage` | 계산 결과를 실제 엔티티에 적용 (피해 유형 `fantasyrpg:rpg`) |
| `resist/MobResistanceManager` | 데이터팩 저항 로더 (싱글턴, 리로드 리스너) |
| `resist/MobResistanceParser`, `ResistanceRules` | JSON 파싱, 보스 저항 차이 판정 (순수) |
| `command/RpgCommands` | `/rpg` 커맨드 |
| `core-mod/src/main/resources/data/` | 피해 유형 정의, 바닐라 피해 태그 추가 |
| `core-mod/src/test/` | JUnit: 부록 A-1, 4.3장 표, 계산 순서, 저항 파싱·판정 |
| `modpack/datapacks/fantasyrpg_content` | 실제 콘텐츠 데이터 (지금은 형식 견본만) |
| `modpack/datapacks/fantasyrpg_test` | 테스트 전용 데이터 (좀비·스켈레톤·철골렘 저항) |

## 데이터 형식

`mob_resistances/*.json`
```json
{ "entity": "minecraft:zombie", "role": "normal|elite|field_boss|chapter_boss",
  "chapter": 7, "resist": { "physical": 0.25, "magic": 0.15 } }
```
- `entity: "default"`인 파일이 있으면 데이터 없는 모든 엔티티의 기본 저항이 됩니다. 없으면 0/0.
- 알 수 없는 필드(`max_health`, `_comment` 등)는 무시합니다.

---

## 결정 사항 기록

기획서에 명시되지 않아 구현하면서 정한 것입니다. 기획 변경 시 함께 검토하세요.

| 날짜 | 결정 | 이유 |
|---|---|---|
| 11-16 | 음수 저항(취약)은 지금 허용하지 않음 (0~1) | 4.2장 "특수 취약 상태는 추후" |
| 11-16 | 음수 스킬 강화로 성분이 음수가 되면 피해 0 | 피해가 회복으로 바뀌는 것 방지 |
| 11-16 | 치명타 확률 판정은 계산기 밖에서, 계산기는 배율만 받음 | 순수 계산의 재현성·테스트 용이성 |
| 11-16 | RPG 피해는 무적 시간(i-frame)을 무시 | 다단 히트 스킬이 바닐라 0.5초 무적에 막히지 않게. 연타 제한은 스킬 쿨다운으로 관리 |
| 11-16 | 보스 저항 차이 상한은 config 값 (0.15/0.20) | 8.4장: 테스트로 확정할 값 |

## 확인이 필요한 질문 (기획자에게)

- 치명타 기본 배율(예: 150%)과 기본 확률은? — 2~4단계 진행 전에 초기값이 필요합니다.
- 플레이어가 받는 피해도 RPG 계산(플레이어 방어·저항)으로 바꿀지, 당분간 바닐라 방어구를 유지할지?

---

## 작업 기록

### 2026-11-16 — 8.3장 1단계: 피해 계산 엔진
- 순수 계산기와 JUnit 테스트(부록 A-1 5건, 4.3장 표, 계산 순서, 입력 검증)
- 몬스터 저항 데이터 로더, 챕터 보스 저항 차이 검사, 설정 파일
- RPG 피해 유형과 바닐라 방어구·무적 시간 우회 태그
- `/rpg simulate|resist|hit` 커맨드, 테스트 전용 데이터팩
- CI: 테스트 팩 zip 아티팩트, 수동 실행·태그로 Release 생성, 테스트 결과 로그 출력
- 단위 테스트 클래스패스에는 Minecraft가 없음 → Gson 등 필요한 라이브러리는 `testImplementation`으로 추가
- v0.1.0 테스트 팩 Release 발행
- 게임 테스트: `docs/TESTING.md` v0.1 항목

### 2026-11-16 — 저장소 초기 구성
- 기획 문서 이전(v0.4 마크다운 변환, 이전 초안 보관), NeoForge 모드 골격, 데이터팩 형식 견본, CI
