# 인계문서 (HANDOFF)

이 문서만 읽으면 누구든(사람이든 새 Claude 세션이든) 이어서 작업할 수 있도록 유지합니다.
**작업이 끝날 때마다 "현재 상태"와 "작업 기록"을 갱신합니다.**

---

## 현재 상태 (2026-11-17 기준)

| 항목 | 상태 |
|---|---|
| 기준 기획 | `docs/design/직업_전투시스템_v0.4.md` |
| 구현 진행 | 8.3장 **1단계(피해 계산 엔진) 완료**. 별도 작업으로 **방어 등급 상한 최소 기능(v0.2) 구현, 게임 확인 전** (브랜치 `claude/armor-grade-cap`, 수치는 JUnit으로 재현됨) → 다음은 v0.2 게임 확인. **Better Combat 연동 실험 준비**(순수 로직·컴파일 의존성)는 브랜치 `claude/combat-experiment-prep` |
| 플랫폼 | Minecraft 1.21.1 / NeoForge **21.1.266** / Java 21 / ModDevGradle 2.0.141. **필수 의존 모드 magiccity** (JitPack `com.github.aswq0258:magic-city-mod:7eb93cc`, `gradle.properties`의 `magiccity_commit`) |
| CI | push마다 빌드·JUnit·도구 자체 검사·테스트 팩 생성 (테스트 결과는 로그의 "테스트 합계" 줄). 모드 구성이 바뀌면 `modpack smoke`가 외부 모드 18개 + 우리 모드 2개로 **NeoForge 전용 서버를 실제 기동** |
| 1차 외부 모드 구성 | `modpack/mods.lock.json` (필수 13 + 권장 5, 버전 고정). 2026-11-17 전용 서버 기동 통과 (run 44061047052). 클라이언트 실행은 아직 확인 안 함 |
| 최신 테스트 팩 | [v0.1.0 Release](https://github.com/SeA-Jeong/fantasy-rpg-modpack/releases/tag/v0.1.0) |
| 게임 테스트 | `docs/TESTING.md`의 v0.1·v0.2 체크리스트 |

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
- **(v0.2, 게임 확인 전) 방어 등급 상한:** 플레이어 ARMOR·ARMOR_TOUGHNESS에 등급별 상한(1급 12/0 … 10급 20/12). 장신구·스킬·다른 모드 수정자 포함. 최종 곱셈형 임시 수정자 `fantasyrpg:grade_limit` 하나로 맞춤, 매 틱(상태가 바뀔 때만)과 피격 직전 갱신. `/rpg armorcap info|grade|refresh`
- **(v0.2, 게임 확인 전) 바닐라 갑옷 조정:** 다이아 18/2, 네더라이트 20/6 (아이템은 그대로, 능력치만). 철 15/0은 바닐라와 같음

- **(준비, 게임 연결 없음) 무기 사용 권한·기본 공격 입력:** `combat/WeaponPermission`(정책 P1 판정), `combat/BasicAttackMath`(콤보 배율 정리·바닐라 충전 곡선). 아직 어떤 이벤트에도 연결하지 않음
- **도구:** `tools/check_mods.py` — mods 폴더 jar 의존성·버전 범위 검사. `tools/fetch_mods.py` — `mods.lock.json`의 외부 모드 내려받기. `tools/smoke_server.py` — 전용 서버 기동·콘솔 명령·로그 검사

### 아직 없는 것 (다음 작업 후보)
- 플레이어 기본 공격·스킬이 RPG 피해를 쓰는 연결 (지금은 커맨드로만 RPG 피해 발생). 설계: 프로젝트 문서 `claude/혼합개발_연동설계_v1.md` C-4 (`AttackEntityEvent` 취소 → `RpgDamage.apply`, 실험 채택). 플레이어 데이터(직업) 구현 후 연결
- 치명타 확률 판정 (지금은 배율만 입력받음. 확률·상한은 5.4장, 8.4장 미확정 값)
- 직업, 코어 자원, 스킬트리, SP, 장비 W 전부
- magiccity 연동 중 레벨·SP 부분(연동작업지침 4~8절). 이번에는 의존성 연결(1~3절)만 함
- 계열 방어구 24종·effectiveTier 가상 교체(계산 함수에는 자리만 있음)·세트/고유 효과·넉백 저항·작업대 (설계: 프로젝트 문서 `claude/장비등급제한_설계.md`)

---

## 코드 지도

| 위치 | 내용 |
|---|---|
| `core-mod/.../FantasyRpgCore.java` | 진입점. 설정 등록, 리로드 리스너·커맨드 이벤트 연결 |
| `core-mod/.../RpgConfig.java` | `config/fantasyrpg-common.toml` (보스 저항 차이 상한, `armorCap` 등급 상한표, `vanillaTuning` 부위별 값) |
| `combat/DamageCalculator` | **순수 계산** (MC 비의존). 공식은 클래스 주석 참고 |
| `combat/DamageInput`, `Resistances`, `DamageBreakdown` | 입력·저항·단계별 결과 레코드 |
| `combat/RpgDamage` | 계산 결과를 실제 엔티티에 적용 (피해 유형 `fantasyrpg:rpg`) |
| `resist/MobResistanceManager` | 데이터팩 저항 로더 (싱글턴, 리로드 리스너) |
| `resist/MobResistanceParser`, `ResistanceRules` | JSON 파싱, 보스 저항 차이 판정 (순수) |
| `command/RpgCommands` | `/rpg` 커맨드 |
| `command/ArmorCapCommands` | `/rpg armorcap` (상한 진단, 테스트용 등급 덮어쓰기) |
| `gear/armorcap/ArmorCapDefaults` | **순수** 기본값(등급 상한·다이아·네더라이트). `RpgConfig` 기본값이 이를 가리킴 |
| `combat/WeaponPermission`, `combat/BasicAttackMath` | **순수** 무기 사용 권한(P1)·기본 공격 입력 (연동 실험 준비, 미연결) |
| `gear/armorcap/ArmorCapMath` | **순수 계산** (MC 비의존): 가상 교체 → 바닐라 순서 재계산(클램프 전 값) → 상한 → 최종 곱셈 보정 k, 적용 후 ulp 조정(`refine`) |
| `gear/armorcap/ArmorCapService` | 플레이어 속성에 보정 수정자 적용. 상태값(등급·상한·기본값·수정자 목록·현재 보정) 비교, 재진입 방지, 40틱 연속 변경 시 호환성 경고(5분 1회) |
| `gear/armorcap/VanillaArmorTuning` | `ItemAttributeModifierEvent`로 바닐라 다이아·네더라이트 갑옷 수정자 값 교체 |
| `gear/armorcap/GradeSource` | 장비 등급 = `MagicCityApi.grade`(지금 1~6) + 테스트 덮어쓰기. magiccity에 `gearGrade`가 생기면 여기만 바꿈 |
| `core-mod/src/main/resources/data/` | 피해 유형 정의, 바닐라 피해 태그 추가 |
| `core-mod/src/test/` | JUnit 51개: 부록 A-1, 4.3장 표, 계산 순서, 저항 파싱·판정, 방어 상한 계산·v0.2 게임 수치 재현(`ArmorCapScenarioTest`), 무기 권한, 기본 공격 입력 |
| `tools/check_mods.py` | mods 폴더 의존성 검사 (자체 검사 `tools/test_check_mods.py`, CI) |
| `modpack/mods.lock.json`, `tools/fetch_mods.py`, `tools/smoke_server.py`, `.github/workflows/modpack-smoke.yml` | 1차 외부 모드 고정 목록과 혼합 서버 기동 검사 (1단계 우리 모드만 → 전체 20개) |
| `modpack/datapacks/fantasyrpg_content` | 실제 콘텐츠 데이터 (지금은 형식 견본만) |
| `modpack/datapacks/fantasyrpg_test` | 테스트 전용 데이터 (좀비·스켈레톤·철골렘 저항, `function/armorcap/*` 방어 상한 시험 함수) |

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
| 11-17 | 방어구·작업대·방어 상한은 이 모드가 담당, magiccity는 진행도만 | 프로젝트 결정(통합문서 1절) |
| 11-17 | 방어 상한은 피해량이 아니라 **속성값**을 맞춤 (최종 곱셈 임시 수정자) | 바닐라 방어 공식이 한 번만 계산, HUD와 서버 값 일치. 피해 이벤트에서 방어 공식을 다시 계산하지 않음 |
| 11-17 | 보정 k는 첫 추정 뒤 **실제 getValue()로 다시 읽어 ulp 단위 조정** | 방어력은 정수 내림으로 쓰여 부동소수 오차로 1이 깎일 수 있음. Java 시뮬레이션 50만 건에서 첫 추정이 틀린 경우 약 10%, 조정 후 불일치 0 |
| 11-17 | 계산 횟수 제한 없음, 상태가 바뀌면 같은 틱에도 다시 계산 | 피해 계산 시점의 정확성 우선 (프로젝트 결정) |
| 11-17 | 등급은 지금 `MagicCityApi.grade`(1~6). 7~10급은 `/rpg armorcap grade`로만 시험 | magiccity에 `gearGrade` 미구현 (네더 원정 전에는 두 값이 같음) |
| 11-17 | 플레이어가 받는 피해에 `fantasyrpg:rpg` 유형을 쓰지 않음 | 방어 무시라서 상한·방어 투자가 무의미해짐 |
| 11-17 | 직업 외 무기로는 RPG 피해 없음, 도구로 공격 차단, 맨손은 최소 피해, 예외 권한 자리 마련 (정책 P1) | 프로젝트 결정 (`claude/혼합개발_연동설계_v1.md` 0절) |
| 11-17 | 바닐라 전투 인챈트는 RPG 전투에서 제외, 보호 인챈트는 유지하고 밸런스 v1.0 수치로 재정의 (P2) | 프로젝트 결정. 보호 인챈트 재정의는 아직 구현 안 함 |
| 11-17 | PvP 1차 완전 비활성 | 프로젝트 결정 |
| 11-17 | 기본 공격 충전 배율은 바닐라 `0.2 + 0.8c²`를 그대로 씀 | 새 수치를 만들지 않음. 기획 결정이 나오면 `BasicAttackMath.chargeScale`만 교체 |

## 관련 프로젝트와의 관계

같은 claude.ai 프로젝트 "마크"에서 월드·스토리 모드 `magiccity`(저장소 `aswq0258/magic-city-mod`)와 방어구·전투 밸런스 기준 v1.0, 확장 작업대 설계가 따로 진행 중입니다. 전체 현황은 프로젝트 문서 `claude/통합문서.md`, 이 엔진의 요약과 충돌 목록은 `claude/직업전투엔진_현재상태.md`에 있습니다.

2026-11-17에 확인한, v0.4와 맞지 않는 점 (모두 미결):
- 받는 피해는 밸런스 기준이 바닐라 갑옷 공식으로 설계함. RPG 피해 유형은 갑옷을 무시하므로 플레이어에게 쓰면 안 됨 (11-17 결정 기록에 반영)
- 월드는 1~10장 자유 순서, v0.4는 장 번호에 레벨·장비 등급을 묶음
- 스킬 초기화 규칙이 반대 (밸런스 기준: 재료 소모와 노드 단위 환급 / v0.4: 전체 초기화와 화폐)
- 밸런스 기준의 체력·공통 패시브 트리, 1~10급 등급 체계, 작업대 무기 레시피와 v0.4의 4방향 트리, 광물 등급, 무기 위력 W의 대응이 없음
- 지역 좌표: CSV는 도면 좌표. 월드 좌표 = 260 + (도면 − 260) × 2, 차원 `magiccity:kingdom`

## 확인이 필요한 질문 (기획자에게)

- 치명타 기본 배율(예: 150%)과 기본 확률은? — 2~4단계 진행 전에 초기값이 필요합니다.
- 플레이어가 받는 피해도 RPG 계산(플레이어 방어·저항)으로 바꿀지, 당분간 바닐라 방어구를 유지할지?

---

## 작업 기록

### 2026-11-17 — 모드 혼합 서버 기동 검사 (브랜치 `claude/modpack-smoke`)
- 병합: fantasyrpg main ← v0.2 + 연동 실험 준비 (`1dc15a6`, 복구 지점 `backup/main-before-v0.2-merge-2026-11-17`), magiccity main ← 테스트 추가 (`f30d2f2`, 복구 지점 `backup/main-before-ff-2026-11-17`). 게임 확인 전 병합임
- `modpack/mods.lock.json`: 필수 13 + 권장 5. FTB 3종은 FTB 메이븐 2101.1.40(Library)·2101.1.12(Teams)·2101.1.39(Quests)로 고정
- CI `modpack smoke`: 우리 모드 빌드 → 외부 모드 받기(sha512) → NeoForge 21.1.266 전용 서버 설치 → 1단계(우리 모드 2개) 기동 → 전체 20개(+자동 포함 3개) 기동. 둘 다 `magiccity status`, `rpg simulate` 콘솔 명령 통과. 치명 로그 없음
- 발견: JEI 19.51.0.418 jar가 `minecraft` 범위를 `[1.21, 1.21.1)`로 선언 → `check_mods.py`는 오류로 판정하지만 NeoForge 21.1.266은 그대로 로드함. 전체 구성의 check_mods 단계는 기록용(`continue-on-error`)으로 두고 판정은 서버 기동으로 함
- 경고만 있음: mixin refmap 경고(배포판에서 정상), Lootr의 Create 연동 mixin 대상 클래스 없음(선택 연동)
- 확인 안 한 것: 클라이언트 실행·화면·렌더링, 플레이어 접속, Better Combat 실제 공격, 파티·퀘스트 동작 (`docs/TESTING.md` 모드 혼합 항목)

### 2026-11-17 — v0.2 자동 테스트, Better Combat 연동 실험 준비
- (브랜치 `claude/armor-grade-cap`, 커밋 `c5d316e`) `ArmorCapDefaults` 분리, `ArmorCapScenarioTest` 11개: `docs/TESTING.md` v0.2 #1~#12·#17 수치를 바닐라 1.21.1 갑옷 공식으로 재현. TESTING.md에 자동/게임 안 구분 표. CI 41개 통과
- (브랜치 `claude/combat-experiment-prep`) `WeaponPermission`·`BasicAttackMath` + JUnit 10개, Better Combat `compileOnly`(Modrinth 메이븐, 배포 jar 미포함), `tools/check_mods.py` + 자체 검사 + CI 단계. CI 51개 통과
- 확인한 것: FTB XMod Compat 21.1.14는 JEI를 선택 의존성 `[19.53.0.425,)`으로 선언 → JEI 정식판 19.51.0.418과 함께 넣으면 범위 밖 → 1차 구성에서 XMod 제외 권장 (check_mods.py 자체 검사에 같은 경우를 넣음)
- 게임 안 확인은 하지 않음

### 2026-11-17 — 방어 등급 상한 최소 기능 (v0.2, 브랜치 `claude/armor-grade-cap`)
- magiccity 의존성 연결: JitPack 커밋 해시, `localRuntime` 구성, mods.toml 필수 의존, NeoForge 21.1.266, 테스트 팩에 magiccity jar 포함·mods.toml 검사
- `gear/armorcap` 패키지: 순수 계산·서비스·바닐라 조정·등급 출처, `/rpg armorcap`, 설정 2절
- JUnit 10개 추가(무작위 10만 건 포함), 합계 30개 통과(CI). 게임 확인은 아직 안 함 → `docs/TESTING.md` v0.2
- 테스트 데이터팩 함수: `armorcap/hit10|same_tick|ring|multiplier`
- 이번에 하지 않음: 계열 방어구 24종, 세트·고유 효과, 넉백 저항, 작업대, 승급, 네더 7~10급

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
