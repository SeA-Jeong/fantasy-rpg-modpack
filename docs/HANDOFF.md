# 인계문서 (HANDOFF)

이 문서만 읽으면 누구든(사람이든 새 Claude 세션이든) 이어서 작업할 수 있도록 유지합니다.
**작업이 끝날 때마다 "현재 상태"와 "작업 기록"을 갱신합니다.**

---

## 현재 상태 (2026-11-18 기준)

| 항목 | 상태 |
|---|---|
| 기준 기획 | `docs/design/직업_전투시스템_v0.4.md` |
| 구현 진행 | 8.3장 **1단계(피해 계산 엔진) 완료**. **방어 등급 상한은 2026-11-18 폐지**, 바닐라 갑옷은 원래 수치로 복원. **계열 방어구 20종(5계열×4부위, 1~10급, 방어력 최대 30)** 등록, **룬석 소켓(6종·4등급, 제작대 끼우기·빼기·합성·연마)**, **플레이어 성장 최소판(직업·레벨·경험치·SP)**, **실제 전투 연결(직업 무기 6종, 근접·활·지팡이 기본 공격 → RPG 피해, PvP 차단)** — 모두 게임 확인 전. **Better Combat 연동 준비**(순수 로직·컴파일 의존성)는 `main`에 있음 |
| 플랫폼 | Minecraft 1.21.1 / NeoForge **21.1.266** / Java 21 / ModDevGradle 2.0.141. **필수 의존 모드 magiccity** (JitPack `com.github.aswq0258:magic-city-mod:7eb93cc`, `gradle.properties`의 `magiccity_commit`) |
| CI | push마다 빌드·JUnit·도구 자체 검사·테스트 팩 생성 (테스트 결과는 로그의 "테스트 합계" 줄). 모드 구성이 바뀌면 `modpack smoke`가 외부 모드 18개 + 우리 모드 2개로 **NeoForge 전용 서버를 실제 기동** |
| 1차 외부 모드 구성 | `modpack/mods.lock.json` (필수 13 + 권장 5, 버전 고정). 2026-11-17 전용 서버 기동 통과 (run 44061047052). 클라이언트 실행은 아직 확인 안 함 |
| 우리 모드 3개 | `fantasyrpg`(이 저장소), `magiccity`(JitPack), `royalqol`(클라이언트 전용 편의 모드, `magic-city-mod` 저장소 `royalqol/`, `mods.lock.json`의 `royalqol.commit`으로 빌드 — `tools/build_royalqol.sh`). 테스트 팩과 서버 기동 검사에 셋 다 들어감 |
| 최신 테스트 팩 | v0.6.0-test.1 (방어구 + 룬석 + 성장 + 전투 연결, 게임 확인 전 — Releases 탭). 이전: v0.5.0-test.1, v0.4.0-test.1, v0.3.0-test.1, v0.2.0-test.4: 통합 팩 `magic-kingdom-testpack-*.mrpack`(우리 모드 3개 + 외부 18개, Prism 가져오기) + 기존 `fantasyrpg-testpack-*.zip` |
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
- **(v0.6, 게임 확인 전) 실제 전투:** 직업 무기 6종(`gear/weapon`, `gear_tier` 1~10, 공격력 = W·공격 속도 속성). 직업 플레이어의 근접 공격(`AttackEntityEvent` 취소 → RPG 피해 1회), 궁수 활 화살(쏠 때 기록 → 명중 시 RPG 피해로 대체), 마법사 지팡이 우클릭 마법 탄(즉시 명중 16칸, 대기 0.8초). 무기 권한(태그 `fantasyrpg:class_weapons/<직업>/<melee|ranged>`), 다른 활·쇠뇌·삼지창 사용 시작 막기, 기록 없는 직업 플레이어 화살 제거, PvP 차단(플레이어→플레이어 모든 피해). 치명타 5%·×1.5. Better Combat 콤보 배율(`compat/bettercombat`, 모드가 있을 때만). 수치 `combat/basic_attack.json` [초안]
- **방어 등급 상한: 2026-11-18 폐지.** 입은 갑옷 수치가 진행 등급과 무관하게 그대로 적용됨. 이전 구현(`gear/armorcap`, `/rpg armorcap`)은 커밋 `56c5bf0` 이전 기록과 복구 지점 `backup/main-before-cap-removal-2026-11-18`에 남음
- **바닐라 갑옷:** 원래 수치 그대로(2026-11-18 조정 폐지). 다이아 20/8, 네더라이트 20/12
- **(v0.3, 게임 확인 전) 계열 방어구:** 기사·드워프·고룡·엘프·마도 × 투구·흉갑·각반·장화 = 20종. 등급은 `fantasyrpg:gear_tier` 컴포넌트(1~10). 능력치는 `armor_stats` 수치표(모드 jar 기본값, 데이터팩으로 덮어쓰기)에서 세트 합계를 3:8:6:3으로 나눠 붙임. 10급 기사·드워프 30/16, 고룡 30/15, 엘프·마도 24/12. `/rpg armor give|tier|info`, 창작 탭 "판타지 RPG 장비". 그림은 바닐라 갑옷 그림 임시 사용. 받는 피해는 바닐라 공식 그대로
- **(v0.4, 게임 확인 전) 룬석 소켓:** 룬석 6종(생명·수호·버팀·질풍·도약·등반) × 4등급, 연마재(1~3), 추출 도구. 소켓 수는 `armor_stats`의 `sockets`(1~3급 1, 4~6급 2, 7~10급 3). 끼운 룬석은 `fantasyrpg:sockets` 컴포넌트(인챈트와 따로), 능력치 ID `fantasyrpg:rune/<부위>/<칸>`, 합계 상한 없음. 제작대 특수 조합 4종(끼우기·빼기·합성·연마), 모루 오른쪽 칸 차단, 툴팁, `/rpg rune give|abrasive|extractor|insert|clear`. 하급 룬석·연마재·추출 도구 제작법은 시험용 임시
- **(v0.5, 게임 확인 전) 플레이어 성장:** 데이터 첨부 `fantasyrpg:player`(직업·레벨·경험치·사용 SP·시작 SP·완료 보너스 기록·직업 변경 횟수, 죽어도 유지). 레벨 상한 = min(60, `MagicCityApi.levelCap`), 상한에서 경험치는 1레벨 분량까지 보관. SP 합계는 계산(직업 1 + 레벨 − 1 + 보너스). 완료 보너스는 `reconcileClearBonus` 한 함수(챕터 첫 완료 이벤트·접속). 경험치: 적대 몬스터 처치, 챕터 첫 완료(등급 × 400). `/rpg player info|reset`, `/rpg class choose|change`, `/rpg xp add`. **이 저장소에서 처음으로 `MagicCityApi`·`ChapterFirstClearEvent`를 실제로 씀**

- **(준비, 게임 연결 없음) 무기 사용 권한·기본 공격 입력:** `combat/WeaponPermission`(정책 P1 판정), `combat/BasicAttackMath`(콤보 배율 정리·바닐라 충전 곡선). 아직 어떤 이벤트에도 연결하지 않음
- **도구:** `tools/check_mods.py` — mods 폴더 jar 의존성·버전 범위 검사. `tools/fetch_mods.py` — `mods.lock.json`의 외부 모드 내려받기. `tools/smoke_server.py` — 전용 서버 기동·콘솔 명령·로그 검사

### 아직 없는 것 (다음 작업 후보)
- 스킬·코어 자원·가드·패리·경직, 파티원 공격 제외(FTB Teams), 쇠뇌, 바닐라 전투 인챈트 데이터팩(정의 덮어쓰기·획득 차단), 무기 제작법, 활 당김 모습(전용 그림·속성), 궁수 근접(지금은 활로 근접하면 "직업 무기가 아닙니다")
- 성장 화면 표시(레벨 바·HUD)와 클라이언트 동기화, 서장 NPC 직업 선택, 직업 변경 비용, 파티 경험치 분배
- 치명타 총상한(기교 보너스 합산)·장비 치명타 옵션
- 직업 코어 자원, 스킬트리(SP 사용), 장비 W 전부
- magiccity 연동 중 레벨·SP 부분(연동작업지침 4~8절). 이번에는 의존성 연결(1~3절)만 함
- 룬석 정식 재료·드롭·중급 제작(4×4), 직업 룬석(순환·가속, 직업 시스템 이후), 세트·고유 효과, 계열 방어구 제작법·작업대·승급, 전용 그림, 계열별 내구도. 심연 계열은 설계 보류
- 전용 서버에 접속한 클라이언트는 수치표가 없어 계열 방어구 툴팁에 수치가 안 보일 수 있음(판정은 서버 값). 수치표 동기화 패킷 필요

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
| `combat/WeaponPermission`, `combat/BasicAttackMath` | **순수** 무기 사용 권한(P1)·기본 공격 입력 (연동 실험 준비, 미연결) |
| `gear/armor/ArmorPart`, `PartSplit`, `ArmorStatTable`, `ArmorStatParser`, `ArmorDamageMath`, `ArmorFamily` | **순수** 계열 방어구 수치표·검사(1~10급, 방어력 ≤30, 강도 ≤20, 단조 증가)·부위 배분(최대 나머지)·바닐라 방어 공식 사본 |
| `gear/armor/GearRegistry`, `FamilyArmorItem`, `FamilyArmorAttributes`, `ArmorStatManager` | 등급 컴포넌트·재질·아이템 20종·창작 탭 등록, `ItemAttributeModifierEvent`로 수치표 능력치 부여(수정자 ID `fantasyrpg:armor/<부위>`), `armor_stats` 로더 |
| `command/ArmorCommands` | `/rpg armor give <계열> <등급>`, `tier <등급>`, `info` |
| `gear/rune/RuneType`, `RuneDefinition`, `RuneDefinitionParser`, `RuneRules` | **순수** 룬석 종류·효과 데이터 검사·끼우기/빼기/합성/연마/효과 계산 규칙 |
| `gear/rune/RuneRegistry`, `RuneItem`, `RuneSockets`, `RuneRecipes`, `RuneEvents`, `RuneStatManager` | 컴포넌트(`rune_grade`, `sockets`)·아이템·조합 직렬화기 등록, 제작대 특수 조합 4종, 모루 차단·툴팁, `runes` 로더 |
| `command/RuneCommands` | `/rpg rune give|abrasive|extractor|insert|clear` |
| `playerdata/RpgClass`, `ProgressionSettings`, `ProgressionMath` | **순수** 직업 6종, 레벨 곡선·경험치 수치, 레벨업·상한·보관·SP·보너스 계산 |
| `playerdata/RpgPlayerData`, `RpgProgress`, `ProgressionManager` | 데이터 첨부 레코드·코덱, 서버 처리(경험치 지급·레벨업 알림·보너스 맞추기·직업 선택/변경·이벤트), `progression` 로더 |
| `command/PlayerCommands` | `/rpg player info|reset`, `/rpg class choose|change`, `/rpg xp add` |
| `combat/CombatSettings` | **순수** 기본 공격 수치(W 표·직업 계수·공격 속도·지팡이 탄·치명타) 파싱·검사 |
| `combat/CombatManager`, `RpgAttacks`, `CombatEvents` | `combat` 로더, 무기 사실(태그)·피해 입력·치명타·거부 안내, 근접·활·사용 시작·PvP 이벤트 |
| `gear/weapon/WeaponType`, `ClassWeaponItem`, `ArcherBowItem`, `WeaponRegistry` | 직업 무기 6종, 지팡이 마법 탄, 공격력·공격 속도 속성 |
| `compat/bettercombat/BetterCombatCompat` | Better Combat 콤보 배율 (모드가 있을 때만 호출) |
| `core-mod/src/main/resources/data/` | 피해 유형 정의, 바닐라 피해 태그 추가, `fantasyrpg/armor_stats/*.json` 계열 수치표 기본값, `fantasyrpg/runes/*.json` 룬석 효과, `fantasyrpg/recipe/` 룬석 조합(특수 4종 + 시험용 임시 제작법), `fantasyrpg/progression/` 레벨 곡선·몬스터 경험치 [초안], `fantasyrpg/combat/basic_attack.json` 기본 공격 [초안], `fantasyrpg/tags/item/class_weapons/` 직업 무기 태그, `fantasyrpg/weapon_attributes/` Better Combat 동작, 바닐라 갑옷 부위 태그(인챈트용)에 계열 방어구 추가 |
| `core-mod/src/main/resources/assets/` | 언어(ko·en), 계열 방어구 아이템 모델(바닐라 모델을 부모로) |
| `core-mod/src/test/` | JUnit 51개: 기본 공격 수치·직업 간 화력 ±15%(`CombatSettingsTest`), 성장(`ProgressionMathTest`), 룬석 규칙(`RuneRulesTest`), 부록 A-1, 4.3장 표, 계산 순서, 저항 파싱·판정, 계열 방어구 수치표·배분·바닐라 대비·받는 피해(`ArmorStatTableTest`), 무기 권한, 기본 공격 입력 |
| `tools/check_mods.py` | mods 폴더 의존성 검사 (자체 검사 `tools/test_check_mods.py`, CI) |
| `modpack/mods.lock.json`, `tools/fetch_mods.py`, `tools/smoke_server.py`, `.github/workflows/modpack-smoke.yml` | 1차 외부 모드 고정 목록과 혼합 서버 기동 검사 (1단계 우리 모드만 → 전체 20개) |
| `modpack/datapacks/fantasyrpg_content` | 실제 콘텐츠 데이터 (지금은 형식 견본만) |
| `modpack/datapacks/fantasyrpg_test` | 테스트 전용 데이터 (좀비·스켈레톤·철골렘 저항, `function/armor/hit10` 피해 시험(받은 피해×1000 표시), 시험용 장신구 칸 반지·목걸이) |

## 데이터 형식

`mob_resistances/*.json`
```json
{ "entity": "minecraft:zombie", "role": "normal|elite|field_boss|chapter_boss",
  "chapter": 7, "resist": { "physical": 0.25, "magic": 0.15 } }
```
- `entity: "default"`인 파일이 있으면 데이터 없는 모든 엔티티의 기본 저항이 됩니다. 없으면 0/0.
- 알 수 없는 필드(`max_health`, `_comment` 등)는 무시합니다.

`armor_stats/*.json` (계열 방어구 세트 합계, 부위 값은 자동 배분)
```json
{ "family": "knight", "class": "heavy",
  "tiers": [ {"tier": 1, "armor": 13, "toughness": 1, "knockback": 0}, … 10급까지 ] }
```
- 1~10급 모두 필요, 방어력 0~30, 강도 0~20, 넉백 저항 0~1(0.01 단위), 등급이 오를 때 세트·부위 값이 줄면 그 파일 거부.
- 기본값은 모드 jar 안에 있음. CLAUDE.md는 테스트로 바뀔 수치를 `modpack/datapacks/fantasyrpg_content/`에 두라고 하지만, 데이터팩 없이도 갑옷이 0이 되지 않도록 jar에 기본값을 넣고 데이터팩의 같은 경로 파일로 덮어쓰게 함(11-18 결정)

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
| 11-17 | 방어구·작업대는 이 모드가 담당, magiccity는 진행도만 | 프로젝트 결정(통합문서 1절) |
| 11-18 | **방어 등급 상한 폐지** (11-17 "상위 방어구 조기 획득 방지"의 방어력·강도 상한 부분 취소). 바닐라 다이아·네더라이트 조정은 유지. 모드에서 만드는 방어구가 바닐라보다 상위 능력치를 갖도록 설계 | 사용자 결정 2026-11-18 (게임 확인 뒤 "갑옷을 입는 의미가 없다"). 기획 반영: 프로젝트 통합문서 5절 |
| 11-18 | **바닐라 갑옷 원래 수치 복원**, 모드 계열 갑옷만 방어력 20 초과(최대 30), 피해 공식은 바닐라 그대로 | 사용자 결정 2026-11-18. 기획: 프로젝트 `claude/계열방어구_상위옵션_초안.md`, `claude/장비등급제한_설계.md` v3 |
| 11-18 | 계열 방어구 수치표 기본값을 모드 jar에 넣음(데이터팩으로 덮어쓰기) | 데이터팩을 월드에 넣지 않아도 갑옷 능력치가 0이 되지 않게 |
| 11-18 | 계열 방어구 그림은 바닐라 갑옷 그림을 빌려 씀 | 전용 그림 전까지 시험 가능하게 |
| 11-18 | 룬석 끼우기·빼기·합성·연마는 전용 작업대 전까지 **일반 제작대 특수 조합**으로 | 화면을 새로 만들지 않고 바로 시험 가능. 빼기는 추출 도구 칸에 룬석을 남기는 방식(조합 남은 아이템) |
| 11-18 | 룬석 소켓 수를 `armor_stats` 데이터(`sockets`)에 둠 | 테스트로 바뀔 수치는 데이터에 두는 규칙 |
| 11-18 | 등급을 낮춰(시험 명령) 칸 수보다 많은 룬석은 데이터에 남기고 효과만 끔 | 룬석이 사라지지 않게 |
| 11-18 | 받은 SP 합계는 저장하지 않고 계산(직업 1 + 레벨 − 1 + 보너스), 보관 경험치는 별도 필드 없이 xp가 need(L)까지 쌓이는 것으로 | 저장 값끼리 어긋날 일이 없게 (연동작업지침의 `spTotal`·`heldXp` 필드 대신) |
| 11-18 | 무직이어도 경험치·레벨업·완료 보너스는 쌓이고, 직업을 고를 때 시작 SP 1 | 서장 NPC 전에 잡은 몬스터 경험치가 사라지지 않게 [제안] |
| 11-18 | 처치 경험치는 마지막 일격을 넣은 플레이어만, 적대 몬스터(`Enemy`)만 | 파티 분배는 [미정] |
| 11-18 | 활 피해 배율 = 화살 속도 ÷ 3(바닐라 최대) 그대로(제곱 아님), 충전 곡선은 근접만 | 바닐라 활 피해가 속도에 비례하는 것과 맞춤 |
| 11-18 | 직업 플레이어의 기록 없는 화살·삼지창은 생물에 맞으면 피해 없음(화살은 사라짐, 삼지창은 남김) | 정책 P1. 삼지창을 잃지 않게 |
| 11-18 | 사용 시작 막기는 서버에서만 (클라이언트에는 직업 정보가 없어 잠깐 당기는 모습이 보일 수 있음) | 성장 데이터 동기화 전까지 |
| 11-18 | 마법사 지팡이는 근접·원거리 둘 다 마법사 무기(태그), 탄은 우클릭 즉시 명중 | 좌클릭은 서버로 신호가 안 가서(허공) |
| 11-17 | 플레이어가 받는 피해에 `fantasyrpg:rpg` 유형을 쓰지 않음 | 방어 무시라서 방어 투자가 무의미해짐 |
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

### 2026-11-18 — 실제 전투 연결 (브랜치 `claude/combat-link`)
- 사용자 결정: 직업 무기 6종 임시 모양으로, 치명타 5%·150%, 범위 근접 + 활(+지팡이) + PvP 차단
- 기획 먼저: 프로젝트 문서 `claude/전투연결_설계.md` 신설 (W·계수 [초안])
- Better Combat 2.4.0+1.21.1 소스(태그 `2.4.0+1.21.1`)에서 무기 속성 이름(sword·spear·dagger·mace·wand·bow_two_handed_light)과 `EntityPlayer_BetterCombat.getCurrentAttack()` 확인
- 추가: `combat`(설정·이벤트·공통), `gear/weapon`, `compat/bettercombat`, 데이터(`combat/basic_attack.json`, 무기 태그, Better Combat 속성), 모델·언어. JUnit `CombatSettingsTest` 3개 → CI 51개 통과, 서버 기동 검사(전투 데이터 로드 확인 추가, Better Combat 오류 없음) 통과
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.6 C1~C15

### 2026-11-18 — 플레이어 성장 최소판 (브랜치 `claude/player-data`)
- 사용자 결정: 경험치 = 몬스터 처치 + 퀘스트·챕터 완료, 곡선 완만하게 증가, 직업은 서장 NPC에서 선택, 변경 가능(비용 있음, 미정)
- 기획 먼저: 프로젝트 문서 `claude/플레이어성장_설계.md` 신설 (수치 [초안])
- 추가: `playerdata` 패키지, `progression/*.json`, `/rpg player|class|xp`, 언어. JUnit `ProgressionMathTest` 6개 → CI 48개 통과, 서버 기동 검사(성장 데이터 로드 확인 추가) 통과
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.5 P1~P12

### 2026-11-18 — 룬석 소켓 (브랜치 `claude/rune-sockets`)
- 사용자 결정: 룬석은 체력 32·이동속도 +10% 상한 밖, 보스 고정 피해 패턴(설계만, `magiccity` 담당), 룬석 세부 기본값(방어력 룬석 없음, 등급 제한 없음, 승급 이전, 모루 차단, 임시 재료)
- 기획 먼저 수정: 프로젝트 문서 `claude/계열방어구_상위옵션_초안.md`(확정 #12~#14, 3-4-1 방식), 밸런스 문서 4·6-1절, 통합문서
- 추가: `gear/rune` 패키지, `runes/*.json` 6종, `armor_stats`에 `sockets`, 조합식 14개, `/rpg rune`, 아이템 모델·언어. JUnit `RuneRulesTest` 6개 → CI 42개 통과. 서버 기동 검사에 룬석 로드·조합식 읽기 오류 확인 추가 → 통과
- 도약의 룬석 값은 [임시] (게임에서 높이 확인 후 결정)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.4 U1~U17

### 2026-11-18 — 바닐라 갑옷 복원, 계열 방어구 20종 (브랜치 `claude/family-armor`)
- 사용자 결정(같은 날): 바닐라 갑옷 원래 수치, 모드 갑옷 방어력 20 초과(안 1: 바닐라 공식 유지), 10급 30, 수치표 확정, 심연 보류, 룬석 소켓 방식(설계)
- 기획 먼저 수정: 프로젝트 문서 `claude/장비등급제한_설계.md` v3, `claude/방어구_전투밸런스_기준_v1.0.md`(체력 상한 32·이동속도 상한과 룬석 합계 무제한이 충돌 → [검토] 표시, 생존 횟수표 재계산 필요), `claude/계열방어구_상위옵션_초안.md`
- 삭제: `gear/vanilla`(`VanillaArmorTuning`·`VanillaArmorDefaults`), 설정 `vanillaTuning`, JUnit `VanillaArmorDamageTest`
- 추가: `gear/armor` 패키지, `armor_stats` 5계열, `/rpg armor`, 아이템 모델·언어·인챈트 태그. JUnit `ArmorStatTableTest` 6개 → CI 36개 통과
- 발견: 확정 표의 고룡 3급(21/8)·7급(26/12)은 강도가 같은 단계 바닐라(다이아 8, 네더라이트 12)와 **같음**(방어력은 높음). 표대로 두고 테스트는 "고룡 강도 ≥ 바닐라"로 검사
- `modpack smoke`에 수치표 로드 확인 줄 추가
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.3 A1~A4, F1~F11

### 2026-11-18 — 방어 등급 상한 폐지
- 사용자 결정: 방어 상한을 완전히 없앰. 모드 제작 방어구가 바닐라보다 상위 능력치를 갖게 설계
- 삭제: `gear/armorcap`(`ArmorCapMath`·`ArmorCapService`·`GradeSource`·`ArmorCapDefaults`), `/rpg armorcap`, 설정 `armorCap`, JUnit `ArmorCapMathTest`·`ArmorCapScenarioTest`, 시험 함수 `ring`·`multiplier`·`same_tick`
- 유지·이동: 바닐라 조정 → `gear/vanilla/VanillaArmorTuning`·`VanillaArmorDefaults`, 시험 함수 `armorcap/hit10` → `armor/hit10`. 새 JUnit `VanillaArmorDamageTest` 3개
- 기존 월드: 보정 수정자는 임시(transient) 수정자라 저장되지 않아 남는 것 없음. 설정 파일의 `[armorCap]` 절은 NeoForge가 모르는 항목으로 무시
- 폐지 전 게임 확인 결과(v0.2.0-test.3, 사용자): 1·2·3·4·5·6·7·8·9·15번 통과 (2급 14.0, 3급 다이아 15.6, 6급 16.666666, 7급 16.857143, 1급 12.799999)
- 복구 지점: `backup/main-before-cap-removal-2026-11-18`

### 2026-11-18 — 첫 게임 안 확인 (사용자, 통합 팩 v0.2.0-test.1, Prism Launcher 11.1.1)
- 통합 팩 가져오기·실행 정상, 모드 목록 26개(우리 3 + 외부 18 + 함께 들어오는 3 + MC·NeoForge)
- 통과: M1·M2·M3(`magiccity status`, `rpg simulate` 150)·M4(Better Combat 휘두르기 영상)·M6(파티)·M8 일부(JourneyMap 미니맵)·M9(`armorcap info`: 다이아 흉갑 7/1 → 1급 상한 12/0으로 강도 0, 실제 방어력 7)·royalqol Q1~Q12
- 발견 1: royalqol "가까운 지역" 키 G가 Curios 장신구 창 기본 키 G와 겹침(키 설정 빨간 표시) → royalqol 0.2.1에서 Y로 변경(`magic-city-mod` `08251d2`, `mods.lock.json` 갱신)
- 발견 2: "장신구가 안 나옴" — Curios는 기본 칸이 없고 칸은 다른 모드·데이터팩이 줘야 함. 테스트 데이터팩에 시험용 반지 2·목걸이 1을 넣음(`fantasyrpg_test/data/fantasyrpg/curios/entities/`). 실제 칸 종류·개수는 [미정](fantasyrpg 결정 필요)
- Q12: 서리 결투장 근사 좌표 도착 알림은 정상, 실제 결투장 건물 위치와 맞는지는 미확인
- 이어서(같은 날, test.2 팩): M5 통과(시험용 데이터팩으로 목걸이·반지 칸 표시, 반지는 개수 미지정이라 1칸), M7 통과(`/summon easy_npc:humanoid` → 지팡이 우클릭으로 편집 화면), M8 통과(JEI 목록·Jade 표시). **모드 혼합 M1~M9 모두 통과**
- 참고: 장신구 칸이 하나도 배정되지 않은 싱글 월드에서는 `/curios add ring …`이 "존재하지 않는 장신구 슬롯"으로 실패함(Curios가 슬롯 이름 목록을 클라이언트 아이콘 동기화로 덮어씀). 칸을 데이터팩으로 배정하면 해결
- 남은 것: v0.2 13항목, magiccity ①~⑱

### 2026-11-17 — 통합 테스트 팩(.mrpack)
- `tools/build_mrpack.py`: Modrinth 모드팩 형식. 외부 18개는 주소·sha1·sha512·크기만(재배포 안 함), 우리 모드 3개·테스트 데이터팩·`테스트방법.md`는 overrides. `dependencies`로 MC 1.21.1·NeoForge 21.1.266 지정
- `tools/install_mrpack.py`: launcher처럼 설치(주소에서 받기·해시 확인). CI `core-mod build`는 팩을 만들고 설치해 21개가 모이는지 확인, `modpack smoke`는 설치한 팩으로 서버 기동. 자체 검사 `test_mrpack.py` 2개
- FTB 3종은 Modrinth에 없어 FTB 메이븐 주소 → Prism은 Modrinth CDN이 아닌 파일(FTB 3개 + overrides의 우리 모드 3개)을 "신뢰할 수 없는 모드" 창으로 확인받음(Prism 소스 `promptForUntrustedMods` 확인). TESTING.md 1-A에 안내
- 사전 릴리스 `v0.2.0-test.1` (`main` `76fe7fa`). Prism에서 실제로 가져오기는 해 보지 않음

### 2026-11-17 — 편의 모드 royalqol 포함
- 이전 대화의 `royalqol-src.zip`이 남아 있지 않아 통합문서 8절 명세대로 새로 작성: `magic-city-mod` 저장소 `royalqol/` (독립 빌드, `./gradlew -p royalqol build`), 커밋 `d811960`, magiccity `main` 병합(복구 지점 `backup/main-before-royalqol-2026-11-17`)
- 이 저장소: `tools/build_royalqol.sh`, `mods.lock.json`의 `royalqol` 고정 커밋, 테스트 팩·`modpack smoke`에 포함. `check_mods.py --side server` 추가(클라이언트 전용 의존성 건너뜀, 자체 검사 5개)
- CI: royalqol JUnit 11개 통과, 서버 기동 1단계(우리 모드 3개)·전체(21개) 통과, 서버 모드 목록에 `Royal QoL 0.2.0 (royalqol)` 확인. 게임 안 확인은 `docs/TESTING.md` Q1~Q12

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
