# 인계문서 (HANDOFF)

이 문서만 읽으면 누구든(사람이든 새 Claude 세션이든) 이어서 작업할 수 있도록 유지합니다.
**작업이 끝날 때마다 "현재 상태"와 "작업 기록"을 갱신합니다.**

---

## 현재 상태 (2026-11-22 기준)

| 항목 | 상태 |
|---|---|
| 기준 기획 | `docs/design/직업_전투시스템_v0.4.md` |
| 구현 진행 | 8.3장 **1단계(피해 계산 엔진) 완료**. **방어 등급 상한은 2026-11-18 폐지**, 바닐라 갑옷은 원래 수치로 복원. **계열 방어구 20종(5계열×4부위, 1~10급, 방어력 최대 30)** 등록, **룬석 소켓(6종·4등급, 제작대 끼우기·빼기·합성·연마)**, **플레이어 성장 최소판(직업·레벨·경험치·SP)**, **실제 전투 연결(직업 무기 6종, 근접·활·지팡이 기본 공격 → RPG 피해, PvP 차단)**, **스킬트리 틀(투자·되돌리기·초기화권·장착칸, 코어 자원, 공통 패시브, K 화면·Z X C V B·HUD)**, **6직업 스킬트리 156노드 완성(1~6단 + 혼합, 궁극기 24·마스터리 24)**, **서장 NPC 직업 선택(magiccity 개인 퀘스트 1장 "왕의 소명")**, **기사 기본 가드·패리, 바닐라 방패 금지, 장착 프리셋 3개**, **유산 무기(하나뿐인 성장형 주무기)**, **2~12장 챕터 퀘스트(magiccity)·각성 강화 재료**, **보스 고정 피해 패턴(magiccity)·직업 방어 통과**, **장별 중간 단계(magiccity)·중간 단계 경험치**, **챕터 보스 인스턴스 전장·필드보스 꾸러미(magiccity)** — 모두 게임 확인 전. **Better Combat 연동 준비**(순수 로직·컴파일 의존성)는 `main`에 있음 |
| 플랫폼 | Minecraft 1.21.1 / NeoForge **21.1.266** / Java 21 / ModDevGradle 2.0.141. **필수 의존 모드 magiccity** (JitPack `com.github.aswq0258:magic-city-mod:51ac66a` (2026-11-21 마을·파티 API·보스전 등급 차 경고·물 패스 점검, 2026-11-21 꾸러미 룬석, 2026-11-20 외부 보스 방어구 무시 공격 배율 제외, 2026-11-20 라디키온 보스 전장 연결, 2026-11-20 보스 수치 데이터·외부 보스 연결, 2026-11-20 크리퍼 없음, 2026-11-20 보스 방 복사 전장·보스 꾸러미 지역 소재, 하수도 납골당 물 샘 수정·`fix crypt`, 전장 종료 멈춤·좀벌레 사라짐 수정, 2026-11-19 인스턴스 전장·필드보스 꾸러미 포함, 2026-11-19 퀘스트 API·NPC 상시 선택지·2~12장 챕터 퀘스트·보스 고정 피해 `isBossFixed`·중간 단계 `QuestStepCompleteEvent`), `gradle.properties`의 `magiccity_commit`) |
| CI | push마다 빌드·JUnit·도구 자체 검사·테스트 팩 생성 (테스트 결과는 로그의 "테스트 합계" 줄). 모드 구성이 바뀌면 `modpack smoke`가 외부 모드 18개 + 우리 모드 2개로 **NeoForge 전용 서버를 실제 기동** |
| 1차 외부 모드 구성 | `modpack/mods.lock.json` (필수 13 + 권장 5, 버전 고정). 2026-11-17 전용 서버 기동 통과 (run 44061047052). 클라이언트 실행은 아직 확인 안 함 |
| 우리 모드 3개 | `fantasyrpg`(이 저장소), `magiccity`(JitPack), `royalqol`(클라이언트 전용 편의 모드, `magic-city-mod` 저장소 `royalqol/`, `mods.lock.json`의 `royalqol.commit`으로 빌드 — `tools/build_royalqol.sh`). 테스트 팩과 서버 기동 검사에 셋 다 들어감. **챕터 보스 모드 `fantasybosses`**(3장 라디키온, ARR — 사용자 허락으로 공개 저장소): `magic-city-mod` 저장소 `fantasybosses/`, `mods.lock.json` 의 `fantasybosses.commit` 으로 빌드(`tools/build_fantasybosses.sh`) → 테스트 팩·통합 팩, 서버 기동 검사는 2단계(GeckoLib 필요)에만. `modpack/local-mods/` 는 지금 비어 있음 |
| 최신 테스트 팩 | **v0.27.2-test.1** (빛·입자 + Effectual TLib 고침 + 지하 동굴 + 장비 그림 v0.7 + Combat Traces + 오버월드 7급·무기 그림·갑옷 수리·방어구 성향 + fantasybosses 0.9.0). 모두 게임 확인 전 — `docs/TESTING.md` 빠른 확인 20~24회차 |
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
- **(v0.7, 게임 확인 전) 스킬트리 틀:** 직업 노드(`skill_nodes/<직업>/*.json`, 기획 v0.4 견본 형식)·공통 패시브(`passive_nodes`)·직업 코어(`class_cores`) 로더. 저장(`RpgPlayerData`): 노드 랭크·장착칸 5(0~3 액티브, 4 궁극기)·공통 패시브 랭크·무료 초기화 사용 수. 서버 판정 `SkillService`(투자·1랭크 되돌리기(선행 깨면 거부)·전체 초기화(무료권: 직업 선택 1 + 6장 완료 1, 유료 비용 [미정]이라 무료권 없으면 거부)·장착·사용). 공통 패시브 포인트 = 등급 × 2(최대 12), 효과는 임시 속성 수정자 `fantasyrpg:passive/<id>`(접속·부활·차원 이동·/reload 때 다시). 코어: 서버 메모리 값(저장 안 함), 기본 공격 적중 `basic_attack_hit`로 얻고 회복·감소. 패킷 4종(`network/RpgNetwork`), 클라이언트 K 화면·Z X C V B·HUD(`client/`). 스킬 효과 등록소 `SkillEffects`는 비어 있어 사용하면 "아직 효과 없는 스킬". 시험 명령 `/rpg skill …`, `/rpg passive …`. **배포 데이터에는 노드·패시브·코어가 0개**, 시험 데이터팩에만 있음

- **(v0.13, 게임 확인 전) 서장 NPC 직업 선택:** magiccity 가 왕좌의 전당에 퀘스트 NPC "왕실 시종장 에드윈"을 두고, 대화 선택지 `choose_class`를 서버에서 확인한 뒤 `NpcActionEvent`를 발행 → `playerdata/ClassSelection`이 무직이면 직업 선택 화면(`client/ClassSelectScreen`, 패킷 `OpenClassSelect`)을 열고 5분 동안 기억 → 클라이언트 `ChooseClass` 요청은 기억이 있고 무직일 때만 받아 직업 선택 + **1급 직업 무기 지급** → `MagicCityApi.questSignal(fantasyrpg:class_chosen)`로 퀘스트 다음 단계. `/rpg class choose`도 신호를 보냄. 신호를 기다리는 단계가 시작·접속될 때(`QuestStepStartEvent`) 이미 직업이 있으면 바로 신호. 직업 **변경**은 NPC에 없음(비용 [미정], 명령만). magiccity 커밋 `magiccity_commit`을 퀘스트 API가 들어간 커밋으로 올림
- **(v0.14, 게임 확인 전) 기사 기본 가드·장착 프리셋:** 기사가 창(`ClassWeaponItem` SPEAR, 사용 동작 BLOCK)으로 오른쪽 버튼을 누르고 있으면 `combat/GuardEvents`가 받는 피해에서 정면 감소·패리(들어 올린 시각과 직전 시각으로 `GuardSettings.isParry`). 스킬 완벽 방어가 켜져 있으면 기본 가드는 건너뜀. 패리는 `lastParryTick`만 갱신해 "패리 후" 효과 창을 열고, 완벽 방어 랭크 효과·패리 패시브(쿨타임 감소·흡수 체력·창돌진 초기화)는 스킬 패리에만. 직업이 있으면 바닐라 방패(`ItemAbilities.SHIELD_BLOCK`) 사용 시작을 서버·클라이언트 모두에서 취소. 수치 `combat/guard.json` [초안]. 프리셋: `RpgPlayerData.presets`(3개, `SkillPreset` 장착칸 5 + 마스터리), 요청 `PRESET_SAVE`·`PRESET_APPLY`, 적용은 `PresetRules.apply`로 배운 노드·칸 종류 확인, 마스터리는 `isSafe`일 때만. 해금 레벨 `skill_settings/presets.json` [초안]. 직업 변경 때 비움
- **(v0.15, 게임 확인 전) 유산 무기:** 무기는 하나뿐(기획 6.1 2026-11-19). 진짜 상태는 `RpgPlayerData.heirloom`(`HeirloomData`: 받음·등급·깨어남·특성·일련번호), 아이템은 직업 무기 + 컴포넌트 `fantasyrpg:heirloom`(주인·일련번호) + `gear_tier`. `RpgAttacks.check(p, …)`가 주인·일련번호가 다르면 `DENY_NOT_OWNER`(유산 표식이 없는 직업 무기는 시험용이라 허용). 강화 한계 = 깨어났으면 `MagicCityApi.grade`, 특성 고르기 = 각성 단계 − 1(`HeirloomRules`). 강화는 K 화면 유산 탭(`HEIRLOOM_ENHANCE`, 인벤토리 재료 소모), 특성 `HEIRLOOM_TRAIT`. 특성 효과: `RpgDamage.apply`에서 물리·마법 보너스, `SkillPassives.critBonus`에 치명타(트리 15%p 상한과 따로), `SkillEvents` 받는 피해 마지막에 감소(최대 50%). 1장 첫 완료 이벤트로 깨어남(접속 때도 맞춤), 등급 상승·봉인 장에 서술 문장. 죽을 때 `LivingDropsEvent`에서 빼 두었다가 `PlayerEvent.Clone`에서 돌려줌. 다시 부르기는 magiccity 시종장 상시 선택지 `recall_heirloom` → `NpcActionEvent` → `ClassSelection` → `HeirloomService.recall`(일련번호 +1). 직업 변경 때 `reform`. 스킬 전체 초기화 때 특성도 비움. 수치·재료 `heirloom/settings.json` [초안·시험용]. (v0.16) 각성 단계가 오를 때 새로 열린 등급의 강화 비용만큼 재료 지급(`stage_up_materials`, `HeirloomRules.stageUpGift`) — 2~6급 합계가 오버월드 강화에 딱 맞음
- **(v0.18, 게임 확인 전) 중간 단계 경험치:** magiccity `QuestStepCompleteEvent`(직접 끝낸 단계마다, 장을 깨서 건너뛴 단계는 없음)의 `isMidStep()`이면 `RpgProgress.onQuestStepComplete`가 지금 등급 × `mid_step_xp_per_grade`(50 [초안], `progression/leveling.json`) 경험치. 계산 `ProgressionMath.midStepXp`
- **(v0.17, 게임 확인 전) 보스 고정 피해 통과:** magiccity 피해 유형 `magiccity:boss_fixed`(예고 뒤 범위 안에만, 바닐라 감소는 태그로 건너뜀)이면 `SkillEvents.onIncomingDamage` 맨 앞에서 바로 돌아감 → 도적 회피·무효 궁극기·불굴의 성벽·기사 완벽 방어·기본 가드·패리·모든 감소 스킬·마력 방벽·유산 수호 특성이 적용되지 않음(기획 5.3, 2026-11-19). 피해 뒤 반응(`onDamagePost`: 수호 패시브 코어 획득·위기 반응)은 그대로. 판정은 `MagicCityApi.isBossFixed`
- **(준비, 게임 연결 없음) 무기 사용 권한·기본 공격 입력:** `combat/WeaponPermission`(정책 P1 판정), `combat/BasicAttackMath`(콤보 배율 정리·바닐라 충전 곡선). 아직 어떤 이벤트에도 연결하지 않음
- **도구:** `tools/check_mods.py` — mods 폴더 jar 의존성·버전 범위 검사. `tools/fetch_mods.py` — `mods.lock.json`의 외부 모드 내려받기. `tools/smoke_server.py` — 전용 서버 기동·콘솔 명령·로그 검사

- **(v0.26, 게임 확인 전) 갑옷 파손·수리:** 계열 갑옷은 내구도 1에서 멈추고 파손 상태(`FamilyArmorItem.isBroken`, 능력치·세트·룬석 꺼짐). 제작대 `FamilyArmorRepairRecipe`로 수리(`armor_crafting/costs.json` rework.repair)
- **(v0.26, 게임 확인 전) 방어구 성향:** 컴포넌트 `fantasyrpg:affinity`, 성향석 4종, `FamilyArmorRetuneRecipe`, 공명·방향 일치 ×1.25(`AffinityRules`·`AffinityEffects`, `armor_affinity/settings.json`)
- **(v0.26, 게임 확인 전) 오버월드 7급:** `CraftRules.MAX_UPGRADE` 7, 7급 승급·유산 7급 재료(오버월드 부품), magiccity `4da6f55`(12장 완료 → 7급, 레벨 상한 Lv.60 유지)
- **(v0.26~0.27, 게임 확인 전) 장비 그림:** 무기 6종 × 10등급(인벤토리 2D·손 3D, `separate_transforms`, 속성 `fantasyrpg:tier`), 활 당기기 3단계(우리 활에 `pull`·`pulling` 속성 등록), 재료 17종 입체·아이콘, 계열 갑옷 아이콘 200(속성 `fantasyrpg:tier`), **입은 갑옷 계열·등급별 모습**(`FamilyArmorItem.getArmorTexture` → `textures/models/armor/<계열>_tNN_layer_1|2.png`), 룬석·연마재 등급 그림(속성 `fantasyrpg:grade`, `rune_sel/`), 작업대 블록 그림(`GridBenchBlock.FACING`, 놓을 때 플레이어 쪽, 이전 월드는 북쪽)
- **(v0.27.1, magiccity `92f7ee8`, 게임 확인 전) 지하 동굴:** 왕국 대륙 Y 0 미만에만 동굴(`world/CaveRules` 순수 노이즈·JUnit 4, Y −55 이하 용암), 지어 둔 던전 영역(`Restore` 영역 상자 = `world/UndergroundZones`) + 8칸 제외, 그 영역 땅속 바닐라 액체는 생성 때 돌로, 기존 월드 `/magiccity fix caves [반지름]`. 지표 입구 없음
- **(v0.27.2, 게임 확인 전) 빛·입자:** 입체 아이템 모델(무기 8~10급·룬석·재료 9종)과 작업대 블록 모델에 빛 마스크 겹침 면(0.01 키움, NeoForge 면 데이터 `block_light`·`sky_light` 15, `render_type` translucent — 작업용 스크립트로 생성), 입은 갑옷 8~10급 빛 층(`client/GlowArmorLayer`, `RenderType.eyes`, 플레이어만, 파손 시 끔), 입자 12종(`fx/RpgParticles`, `client/GlowSparkParticle`: 룬석 8 반짝임·작업대 룬 먼지·불꽃), 룬석 입자 뿜기(`client/RuneFx`: 손 중급 이상·바닥, 프리셋 v0.4 초당 개수, 24칸), 작업대 `ACTIVE` 상태(화면 열린 수를 서버가 셈, 작동 모델·빛 7·`animateTick` 입자)
- **(팩) Combat Traces 1.1.0:** `modpack/local-mods/` jar, 클라이언트·서버 둘 다

### 아직 없는 것 (다음 작업 후보)
- 파티원 공격 제외(FTB Teams), 쇠뇌, 무기 제작법
- 장비 그림 v0.7 중 보관만 한 것: 기사 방패(아이템 없음), 네더 15종(구현 금지, 7급 촉매는 폐지), GeckoLib 입체 갑옷, 발광 마스크·입자, 작업대 작동 상태. 성향석 4종 그림 없음
- NPC 직업 변경 메뉴(지금은 `/rpgclass change`), 2장 이후 개인 퀘스트·다른 NPC 대사, FTB 파티 퀘스트
- 장비 치명타 옵션(합계 상한 50%는 있음)
- 기사 전용 방패 아이템·그림, 다른 직업 가드, 엔딩 후 무료 초기화(magiccity 엔딩 API 필요), 장비 W 전부, 파티 대상 버프 더 넓히기(지금은 전투사제 회복 5종·신성 방벽만)
- 분해·계열 전환의 JEI 표시
- magiccity 연동 중 레벨·SP 부분(연동작업지침 4~8절). 이번에는 의존성 연결(1~3절)만 함
- 계열 고유 특성, 8~10급 승급(네더), 계열별 내구도. 심연 계열은 설계 보류

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
| `client/FantasyRpgClient` (FMLClientSetupEvent) | 아이템 속성 등록: 무기·계열 갑옷 `fantasyrpg:tier`(등급÷10), 룬석·연마재 `fantasyrpg:grade`(등급÷10), 활 `pull`·`pulling` — 모델 overrides 가 이 값으로 그림을 고름 |
| `gear/armor/AffinityRules`, `AffinityEffects`, `AffinityTooltip`, `craft/FamilyArmorRepairRecipe`, `FamilyArmorRetuneRecipe`, `ReworkRules`, `GridBenchBlock` | **순수** 성향 규칙 / 성향 효과·툴팁, 갑옷 수리·성향 바꾸기 조합, 분해·전환·수리 비용(순수), 작업대 블록(방향 `FACING`) |
| `compat/bettercombat/BetterCombatCompat` | Better Combat 콤보 배율 (모드가 있을 때만 호출) |
| `skilltree/SkillNode`, `SkillTreeRules`, `PassiveNode`, `ResetRules`, `classcore/CoreDef` | **순수** 노드 형식·투자/되돌리기 판정(부록 A-2)·공통 패시브 포인트·무료 초기화권·코어 회복/감소 |
| `skilltree/SkillData`, `SkillService`, `SkillEffects`, `classcore/CoreService` | 로더 3개, 서버 처리·동기화, 스킬 효과 등록소(비어 있음), 코어 현재값 |
| `network/RpgNetwork` | 패킷: 상태·정의·코어(서버→클라), 요청(클라→서버) |
| `client/FantasyRpgClient`, `ClientRpgState`, `SkillTreeScreen`, `RpgHud` | 클라이언트 전용 진입점(`dist = CLIENT`)·단축키, 받은 상태, K 화면, HUD |
| `skills/SkillMath`, `SkillIds` | **순수** 부채꼴·마력 방벽·연쇄 쿨타임·감소 계산, 효과 id |
| `skilltree/SkillValues` | **순수** 노드의 랭크별 수치(`cooldown_ticks`·`cost`·`values`) |
| `skills/Cast`, `SkillTargets`, `SkillRuntime`, `SkillPassives`, `SkillEvents`, `SkillSettings` | 스킬 한 번 사용(피해·치명타·강화·암영, 5랭크 코어 강화), 대상 고르기(조준·직선·부채꼴·주변·지나간 경로)·이동(돌진·도약)·제어 효과(보스 면역), 쿨타임·예약 실행·지속 효과, 패시브(묶음별 공통: 공격 2단·주문 2단·수호 2단·기교 2단, 화살, 흡수 체력, 시간 제한 속성), 받는 피해(회피·패리·감소·마력 방벽)·틱·처치·넉백 이벤트, `skill_settings` 로더 |
| `skills/WarriorSkills`, `MageSkills`, `KnightSkills`, `RogueSkills`, `ArcherSkills`, `ClericSkills` | 직업별 1단·3단 액티브 4종씩 |
| `skills/UltimateSkills` | 5단 궁극기 24종 |
| `gear/heirloom/HeirloomSettings`, `HeirloomRules` | **순수** 유산 무기 수치(`heirloom/settings.json`)·한계·특성 고르기·강화 판정 |
| `gear/heirloom/HeirloomService`, `HeirloomManager`, `HeirloomTag`, `playerdata/HeirloomData`, `command/HeirloomCommands` | 유산 무기 지급·다시 부르기·모양 바꾸기·강화·특성·각성·죽음 보관, 로더, 아이템 컴포넌트, 저장 레코드, `/rpg heirloom` |
| `combat/GuardSettings`, `combat/GuardEvents` | **순수** 기본 가드 수치·패리 판정(`guard.json`) / 기사 창 가드·패리 적용, 바닐라 방패 금지 |
| `skilltree/PresetRules`, `playerdata/SkillPreset` | **순수** 장착 프리셋 해금·적용 규칙 / 저장 레코드 |
| `playerdata/ClassSelection`, `client/ClassSelectScreen` | 서장 NPC 직업 선택(magiccity `NpcActionEvent`·`QuestStepStartEvent` 수신, 화면 열기 기억 5분, 1급 무기 지급, 퀘스트 신호), 6직업 카드 화면(글은 언어 파일 `class.fantasyrpg.<직업>.weapon|core|desc|play`) |
| `command/SkillCommands` | `/rpg skill list|invest|refund|reset [force]|equip|cast`, `/rpg passive list|invest|refund` |
| `core-mod/src/main/resources/data/` | 피해 유형 정의, 바닐라 피해 태그 추가, `fantasyrpg/armor_stats/*.json` 계열 수치표 기본값, `fantasyrpg/runes/*.json` 룬석 효과, `fantasyrpg/recipe/` 룬석 조합(특수 4종 + 시험용 임시 제작법), `fantasyrpg/progression/` 레벨 곡선·몬스터 경험치 [초안], `fantasyrpg/combat/basic_attack.json` 기본 공격 [초안], `fantasyrpg/tags/item/class_weapons/` 직업 무기 태그, `fantasyrpg/weapon_attributes/` Better Combat 동작, 바닐라 갑옷 부위 태그(인챈트용)에 계열 방어구 추가 |
| `core-mod/src/main/resources/assets/` | 언어(ko·en), 사용자 제공 그림: `models/item/weapons|weapon_icons|weapon_sel`(무기·활 당김), `armor/`(갑옷 아이콘), `materials|material_icons`(재료), `runes|rune_icons|rune_sel`(룬석), `textures/models/armor/`(입은 갑옷 층), `blockstates|models/block`(작업대). 최상위 `models/item/<id>.json` 은 overrides·`separate_transforms` 로 고름 |
| `core-mod/src/test/` | JUnit 73개: 6직업 스킬 데이터·수치·코어·순수 계산(`ClassSkillsTest`), 스킬트리·부록 A-2(`SkillTreeRulesTest`), 기본 공격 수치·직업 간 화력 ±15%(`CombatSettingsTest`), 성장(`ProgressionMathTest`), 룬석 규칙(`RuneRulesTest`), 부록 A-1, 4.3장 표, 계산 순서, 저항 파싱·판정, 계열 방어구 수치표·배분·바닐라 대비·받는 피해(`ArmorStatTableTest`), 무기 권한, 기본 공격 입력 |
| `tools/check_mods.py` | mods 폴더 의존성 검사 (자체 검사 `tools/test_check_mods.py`, CI) |
| `modpack/mods.lock.json`, `tools/fetch_mods.py`, `tools/smoke_server.py`, `.github/workflows/modpack-smoke.yml` | 1차 외부 모드 고정 목록과 혼합 서버 기동 검사 (1단계 우리 모드만 → 전체 20개) |
| `modpack/datapacks/fantasyrpg_content` | 실제 콘텐츠 데이터 (지금은 형식 견본만) |
| `modpack/datapacks/fantasyrpg_test` | 테스트 전용 데이터 (직업마다 시험 노드 5개·시험 코어 6개·시험 공통 패시브 2개, 좀비·스켈레톤·철골렘 저항, `function/armor/hit10` 피해 시험(받은 피해×1000 표시), 시험용 장신구 칸 반지·목걸이) |

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
| 11-18 | 스킬 되돌리기: 노드 1랭크 되돌리기(선행 조건을 깨면 거부) + 전체 초기화, 무료 초기화권은 직업 선택 1회·6장 완료 1회, 엔딩 후 무제한(엔딩 API 전까지 미구현). 비용은 `skill_reset.json` 예정, **지금은 노드 되돌리기 무료·유료 전체 초기화 불가** | 사용자 결정 "1번을 기조로 하되 엔딩 후에는 3번도". 기획 v0.4 7.1·A-2 먼저 고침 |
| 11-18 | 공통 패시브: 직업 트리와 별도 트리·별도 포인트(등급마다 2, 최대 12), 직업 변경해도 유지 | 사용자 결정 |
| 11-18 | 코어 현재값은 저장하지 않음(접속·부활·직업 변경 때 시작값) | 전투 상태라서 [제안] |
| 11-18 | 혼합 노드는 화면에서 첫 방향 열에 그림 | 화면 배치 [초안] |
| 11-18 | 스킬 노드·코어 기본 데이터를 모드 jar에 넣음(데이터팩으로 덮어쓰기). 시험 데이터팩의 전사·마법사 시험 코어는 지움 | 계열 방어구와 같은 방식. 같은 직업 코어가 둘이면 어느 쪽이 쓰일지 정해지지 않아서 |
| 11-18 | 스킬은 직업 무기를 들어야 씀. 한 대상 스킬은 플레이어가 아닌 모든 생물, 범위 스킬은 적대 몬스터만 | 정책 P1, 마을에서 주민·가축 보호 [초안] |
| 11-18 | 경직·느림·밀쳐내기 면역(보스) = 엔티티 태그 `c:bosses` + magiccity 보스 스코어보드 태그 + 최대 체력 100 이상 (`skill_settings/control.json`) | magiccity 보스 태그가 통일돼 있지 않아서 [초안] |
| 11-18 | 쿨타임·지속 효과는 서버 메모리(저장 안 함), 서버 틱 수 기준. 대상이 없거나 막혀서 못 쓰면 비용·쿨타임 없음 | |
| 11-18 | 같은 역할 패시브(공격 2단·주문 2단·수호 2단·기교 2단)는 직업 구분 없이 같은 수치 키로 처리(`SkillIds` 묶음). 주문 2단 5랭크 쿨타임 감소 키 이름을 `cooldown_cut`으로 통일 | 4직업 추가 때 코드 중복을 줄이려고 |
| 11-18 | 기사 패리는 완벽 방어 스킬 안에서만(처음 6틱, 정면 좌우 60°, 패리 한 번으로 자세 끝). 도적 회피는 피해 0(취소), 자동 회피(날렵함)는 성공 효과 없음. 돌진은 순간이동, 도약은 속도로 밈 | [초안] |
| 11-18 | 시험 데이터팩의 시험 코어 6개는 모두 지움(실제 코어가 모드 jar에 생김) | 같은 직업 코어가 둘이면 어느 쪽이 쓰일지 정해지지 않아서 |
| 11-19 | 4단 패시브는 노드 데이터 `modifies`(대상 효과·수치 키·랭크별 가감, 음수 허용)로 다른 스킬 수치를 바꾸고, 공통 효과는 합산 키(`all_damage_bonus`·`skill_magic_bonus`·`skill_physical_bonus`·`cooldown_rate`·`spell_hit_core`·`basic_hit_core`·`heal_bonus`·`max_core_bonus`·`crit_mult_add` 등)로 처리. `bonus_core_min`이 있으면 피해·쿨타임 키만 그 코어 이상일 때 | 코드 없이 데이터로 조정하려고 |
| 11-19 | 쿨타임 = (노드 값 + modifies − 주문 2단 감소) × (1 − 쿨타임 비율 합, 최대 50%) × 효과가 정한 배율 → 연쇄 감소 | |
| 11-19 | 마스터리 활성 1개는 `RpgPlayerData.activeMastery`(저장), 효과 계산(`SkillPassives.rankOf`)에서 활성 아닌 마스터리는 0랭크로 봄. 처음 배운 마스터리 자동 활성. 교체는 "16칸 안 적대 몬스터 없음 + 10초 동안 피해 없음"일 때만 | 기획 2.4 "안전 지역" [초안] |
| 11-19 | 혼합 노드는 레벨 조건 없음(두 방향 8SP씩), K 화면 7번째 줄 | [초안] |
| 11-20 | 챕터 보스를 외부 보스 모드 몹으로 바꿀 수 있게: magiccity `boss_entity/<키>.json`(모드 없으면 기본 보스), 외부 보스면 우리 기술·부하·체력바 끄고 고정 피해 패턴만, 기준점 NBT로 전장 위치 전달. 3장 = `fantasybosses:radicion`(임시) | 사용자 결정 2026-11-20 (`claude/외부보스연결_초안.md` A~D 추천안) |
| 11-20 | 보스 수치(챕터 12·등급표·필드 10)는 magiccity 데이터 파일, 외부 데이터팩으로 **수치만** 덮어씀. 등급표는 설정 파일에서 데이터로 옮김 | 사용자 결정 2026-11-20 ("보스는 외부에서 작성한 데이터팩", "수치만 덮어씀") |
| 11-20 | Modrinth 에 없는 외부 모드는 `modpack/local-mods/` 에 jar 로 두고 통합 팩에 파일로 넣음 (ARR 은 사용자 허락) | 사용자 결정 2026-11-20 (D) |
| 11-20 | fantasybosses 소스는 `magic-city-mod` 저장소 `fantasybosses/`(공개 허락), 팩은 고정 커밋 빌드. 라디키온 v0.8 = 전장 원점 변환(이동량 NBT) + 마디 RPG 피해 + 등급 배율 기준 6 [초안] + 3장 우리 고정 피해 끔 | 사용자 결정 2026-11-20 (`claude/FantasyBosses_등록_v0.8_초안.md` A~D) |
| 11-21 | 바닐라 전투 인챈트 22종: 효과 없음(데이터) + 획득 태그 제외(데이터) + 플레이어 아이템에서 지움(코드), 모든 플레이어·몹. 보호 III·3%/레벨, 특수 보호 II·6%/레벨, 넘는 레벨은 상한 | 사용자 결정 2026-11-21 (`claude/바닐라인챈트_정리_초안.md`). 1.21 약탈은 효과를 비워도 작동해서 지우는 방식 |
| 11-20 | 여러 부위 몹의 부위를 맞히면 계산은 몸체로 한 번, 피해는 부위로(`RpgDamage.apply(target, hit, …)`) | 부위·그로기 배율은 보스 모드가 한 번만 곱하게 |
| 11-19 | 챕터 퀘스트 완료 보상의 강화 재료는 퀘스트가 아니라 **각성 단계가 오를 때** fantasyrpg가 지급 | 2~10장이 자유 순서라 장마다 정한 재료로는 순서에 따라 모자랄 수 있음. 진행 등급 기준이면 순서와 상관없이 딱 맞음. 퀘스트를 받지 않아도 받음 |
| 11-19 | 무기는 유산 무기 하나: 진행이 각성·강화 한계, 재료로 등급, 각성마다 4방향 특성, 성격·대사 없음 | 사용자 결정 2026-11-19. 기획 6.1·6.3 먼저 수정 |
| 11-19 | 유산 강화는 제작대 조합 대신 K 화면 유산 탭 버튼(서버가 인벤토리 재료 소모) | 제작대 조합은 플레이어 진행도(한계)를 알 수 없어서. 전용 작업대가 생기면 옮김 [초안] |
| 11-19 | 유산 표식이 없는 직업 무기(시험 명령·v0.13에서 받은 것)는 그대로 쓸 수 있음 | 시험·이전 저장 호환. 게임 안에서는 유산 무기만 얻음 |
| 11-19 | 기본 가드는 기사만(창 오른쪽 버튼), 직업이 있으면 바닐라 방패 금지, 프리셋은 장착칸·마스터리만(SP 배분 제외) | 사용자 결정 2026-11-19. 기획 5.3·7.1 먼저 수정 |
| 11-19 | 기본 패리는 "패리 후" 창만 열고 패리 패시브(쿨타임 감소·흡수 체력 등)는 스킬 완벽 방어에만 | 1초마다 패리할 수 있어 패시브까지 붙으면 너무 강함 [초안] |
| 11-19 | 서장 NPC 대화는 magiccity 자체 대화(채팅 클릭 선택지, A안), 직업 선택 화면은 fantasyrpg. 선택지 명령은 권한 0이지만 서버가 NPC 표식·6칸·단계·화면 열기 기록을 다시 확인 | 사용자 승인 2026-11-19. Easy NPC는 월드마다 손으로 배치해야 하고 치환자 문법 미확인 |
| 11-19 | 직업 선택 때 그 직업 1급 무기 지급(NPC 화면으로 고를 때만, 명령으로 고르면 없음) | 사용자 승인 2026-11-19 |
| 11-19 | 궁극기: 피해 무효(절대 결계·부활 직후)·회피(그림자 장막)·정면 무효(불굴의 성벽)는 받는 피해 이벤트를 취소. 시간 가속은 궁극기 쿨타임·비용에는 적용 안 함. 은신 후퇴는 보스(제어 면역 대상)의 표적은 유지 | 초안 4번 확인 사항 기본값 |
| 11-18 | 흡수 체력은 최대 흡수 체력 속성에 임시 수정자를 붙여 채우고 시간이 지나면 거둠. 투지 5랭크 흡수 체력 지속 10초 | 1.21에서 흡수 체력은 최대치 속성이 필요. 10초는 초안 표에 없던 값 [초안] |
| 11-17 | 플레이어가 받는 피해에 `fantasyrpg:rpg` 유형을 쓰지 않음 | 방어 무시라서 방어 투자가 무의미해짐 |
| 11-17 | 직업 외 무기로는 RPG 피해 없음, 도구로 공격 차단, 맨손은 최소 피해, 예외 권한 자리 마련 (정책 P1) | 프로젝트 결정 (`claude/혼합개발_연동설계_v1.md` 0절) |
| 11-17 | 바닐라 전투 인챈트는 RPG 전투에서 제외, 보호 인챈트는 유지하고 밸런스 v1.0 수치로 재정의 (P2) | 프로젝트 결정. 보호 인챈트 재정의는 아직 구현 안 함 |
| 11-17 | PvP 1차 완전 비활성 | 프로젝트 결정 |
| 11-17 | 기본 공격 충전 배율은 바닐라 `0.2 + 0.8c²`를 그대로 씀 | 새 수치를 만들지 않음. 기획 결정이 나오면 `BasicAttackMath.chargeScale`만 교체 |
| 11-21 | 재사용 대기 비율 감소는 스킬 가속(`기본 × 100 ÷ (100 + 가속)`, 상한 없음)으로 통일. 트리 % 패시브 5개를 같은 효과 가속으로 바꿈(반올림) | 사용자 제안(LoL식). 기획 5.4 |
| 11-21 | 치명타 합계 상한 50% [초안], 궁수 활 근접 물리 0.4·초당 1 [초안] | "추천안으로 전부" (사용자). 데이터 `combat/basic_attack.json` |
| 11-21 | 마스터리 교체·빌드 바꾸기·직업 변경은 magiccity 마을 안 + 안전 조건 | 기획 2.4 "비전투 안전 지역" |
| 11-21 | 파티 = magiccity `partyMembers`(FTB 파티). 회복 50%·방벽 50%(16칸), 경험치 48칸 총량 +20%/명 [초안] | "추천안으로 전부" |
| 11-21 | 비용 [초안]: 노드 되돌리기 무료, 전체 초기화 에메랄드 8×등급, 빌드 4×등급, 직업 변경 에메랄드 16·다이아 1×등급 | 기존 경제 아이템, 등급에 비례 |
| 11-21 | 분해 = 숫돌 + 갑옷 → 재료 50%(룬석은 먼저 빼기), 계열 전환 = 바꿀 계열 1급 재료 + 4급부터 핵심 부품 × (등급 − 3), 인챈트·룬석 유지 [초안] | "추천안으로 전부" |
| 11-21 | 전용 서버 데이터 동기화: 서버가 읽은 원본 JSON 을 접속·/reload 때 보내고 클라이언트는 같은 읽기 코드로 적용 | 새 직렬화 코드 없이 툴팁·JEI 를 맞춤 |

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

### 2026-11-21 — 오버월드 필드 몬스터 30종 모델 v1.2 (fantasybosses 0.9.0, magic-city-mod `4da5507`)
- 사용자 제공 모델 패키지 v1.1(30종)·v1.2(정예 10종 교체)를 검수: v1.2 는 면별 UV 겹침 0, 발광 픽셀이 모두 쓰이는 면 위, 마스크 색 = 텍스처 색. 프로젝트 문서 `claude/오버월드몬스터모델_v1.1_검수.md`.
- magic-city-mod `fantasybosses/`: 리소스 `geo|animations|textures/entity/field/`, 테스트용 엔티티 30종(`field/FieldMonsters`·`FieldMonster`, 자연 스폰 없음, 근접 AI, 수치 [실험]), 렌더러(정예 `AutoGlowingGeoLayer`, 바닐라 사망 회전 끔), 몸통 기준 히트박스, `/fantasybosses field gallery|anim`. 0.8.0 → 0.9.0, main 빨리 감기(복구 지점 `backup/main-before-field-monsters-2026-11-21`).
- 이 저장소: `mods.lock.json` fantasybosses 고정 커밋 → `4da5507`, CI 의 jar 이름·서버 로그 검사 0.9.0. 사전 릴리스 `v0.27.0-test.2`. 게임 확인 TESTING FM1~FM8, 빠른 확인 22회차.
- 남은 것: 원거리·비행·스킬 AI, 지역별 자연 스폰(지역 고정 등급), 드롭·수치, 균열 포식자 부위 판정 여부.


### 2026-11-22 — 장비 그림 v0.8 적용 (fantasyrpg 0.27.3)
- 사용자 제공 `fantasyrpg_equipment_assets_v0.8.zip`(파일 1,984). v0.7 대비: 갑옷 아이콘 200·무기 모델 110·무기 그림 161·GeckoLib 갑옷 50(geo·아틀라스·발광)·애니메이션 10·갑옷 오오라 입자 15 바뀌거나 새로. 재료·룬석·작업대·입은 갑옷 층 그림(`textures/models/armor`)은 같음. 기사 방패·네더는 여전히 아이템 없음.
- 넣은 것: 같은 작업용 스크립트(`apply_eq07.py` → `glow_eq07.py`)로 다시 생성 — 아이콘·무기 모델·활 당김·빛 겹침(무기 1~10급 전부 마스크 있음). 작업대 블록 상태는 자료가 방향만 있는 판이라 작동(`active`) 판으로 다시 덮음. 갑옷 아이콘 모델의 GUI 회전값은 뺌. 오오라 입자 15종(`RpgParticles.aura`, `RuneFx`). 모델 670개 참조 검사 빠짐 0.
- 넣지 않은 것: **GeckoLib 입체 갑옷(입은 모습)** — 렌더러·의존성(fantasyrpg → GeckoLib 필수)이 필요해 사용자 결정 대기. 그때까지 입은 모습은 바닐라 층 그림 + 8~10급 빛 층.
- 게임 확인 TESTING v0.27.3 EQ8-1~EQ8-6, 빠른 확인 25회차.

### 2026-11-22 — 빛·입자 연결 (fantasyrpg 0.27.2)
- 사용자: "아이템이 어둡고 빛 효과가 없다" → 장비 그림 v0.7 때 빛 마스크·입자는 보관만 했던 것. 새 모델링 없이 받은 마스크(무기 33·룬석 64·재료 18·입은 갑옷 30·작업대 8)와 입자 그림으로 연결. 사용자 "그것도 넣어줘"(입자 포함).
- 기사 방패·네더 아이템 빛은 아이템이 없어 넣지 않음. 인벤토리 아이콘의 어두움은 빛과 무관(그림 색감) — 게임 확인 뒤 판단. 갑옷 9급 맥동·10급 발자국 같은 움직이는 연출(자료 effects_spec)은 아직.
- 게임 확인 TESTING v0.27.2 GL1~GL8, 빠른 확인 24회차.

### 2026-11-22 — Effectual 필수 라이브러리 빠짐 (클라이언트 시작 실패)
- 사용자 화면: "Effectual (effectual) has failed to load correctly — NoClassDefFoundError: com/imeetake/tlib/client/particle/TParticles". Effectual 1.3.0(NeoForge)은 같은 만든이의 TLib 를 필수로 쓰는데, 서버 기동 검사는 클라이언트 전용 모드를 빼고 돌아서 못 잡았음.
- 고침: `mods.lock.json` 에 TLib `1.5.0-1.21.1`(클라이언트 전용) 추가, 통합 팩 모드 25 → 26. `tools/fetch_mods.py` 가 Modrinth 의 "필수" 의존 모드가 목록에 빠져 있으면 실패로 알리고 이름·맞는 버전을 보여 줌(클라이언트 포함 전체 받기 때) — 이번 CI 에서 다른 빠진 필수 모드 없음 확인. 사전 릴리스 `v0.27.1-test.2`.

### 2026-11-22 — 지하 동굴 (fantasyrpg 0.27.1, magiccity `92f7ee8`)
- 사용자 요청: 특정 높이 아래로 동굴, 던전과 겹치지 않게(지어 둔 지형 기준 여백), 용암·지하수가 던전을 망가뜨리지 않게. 결정: Y 0 미만, 여백 8칸(사용자 5칸 제안 → 상자 오차 여유로 8 추천), 물 없음·바닥 용암, 기존 월드 명령.
- 지형 계산식 표본(±5000, 8칸 간격): 지면 가운데값 Y 66, 바다 바닥 최저 29, 봉우리 최고 248. 땅 밑 건축: 하수도 47, 심층 광산 21, 뿌리 던전 8, 심연 균열 −30, 드래곤 레어 보물 동굴 −40 → 동굴 높이와 겹치는 것은 균열·레어뿐이고 여백으로 제외.
- magiccity: `CaveRules`(굴 = 노이즈 둘의 0 근처 교차, 방 = 느린 노이즈, 끝으로 갈수록 좁게, 동굴 약 8.5%·가장 큰 연결 덩어리 약 79%), `UndergroundZones`(Restore 영역 지연 초기화), `KingdomChunkGenerator.blockAt`·`sealZones`, `site/CaveFix` + `/magiccity fix caves`, README.
- 이 저장소: `magiccity_commit` → `92f7ee8`, 0.27.1, 서버 기동 검사에 왕국 대륙 청크 강제 로드 2곳(심연 균열·들판)과 동굴 생성 오류 검사. 게임 확인 TESTING CV1~CV7, 빠른 확인 23회차.
- 첫 CI: JitPack 첫 빌드 시간 초과 → 다시 실행. 그 사이 Modrinth 에서 spark 버전 이름이 `1.10.124-neoforge-1.21.1` → `1.10.124-neoforge` 로 바뀌어 받기 실패 → `mods.lock.json` 고침. 서버 기동 검사에서 왕국 대륙 청크 2곳 생성·동굴 오류 없음 확인, 사전 릴리스 `v0.27.1-test.1`.
- 남은 것(별도 결정): 지역 채굴 금지·자동 수복(던전 벽을 뚫는 것을 막기), 동굴 입구, 동굴 몬스터 등급.

### 2026-11-21 — 장비 그림 v0.7 적용 (fantasyrpg 0.27.0)
- 사용자 제공 `fantasyrpg_equipment_assets_v0.7.zip`(파일 1,720개, JSON 607개 오류·빠진 참조 0)을 대조해 1+2 범위만 넣음(사용자 "진행하자"). 자료 안 스크립트 5개는 실행하지 않았고, 복사·모델 생성은 작업용 스크립트(저장소 밖)로 함.
- 넣은 것: 재료 17(`materials/` 입체 + `material_icons/`, 최상위 `separate_transforms`), 갑옷 아이콘 200(`armor/`, 최상위 overrides `fantasyrpg:tier`), 갑옷 층 100(`textures/models/armor/<계열>_tNN_layer_1|2.png`, `FamilyArmorItem.getArmorTexture` 로 계열·등급별), 무기 8~10급 수정, 활 당기기(`weapon_sel/archer_bow_tNN_pulling_k`, 최상위 overrides = 등급 + `pulling`/`pull`; 자료는 overrides 를 하위 모델에 넣어 두었는데 게임이 무시하므로 옮김; 우리 활에 `pull`·`pulling` 속성 등록), 룬석 8종×4·연마재×3(`fantasyrpg:grade` = 등급÷10, `rune_sel/`), 룬 원석·추출 도구, 작업대 블록·아이템 모델(블록에 `facing` 속성 추가, 이전 월드 작업대는 북쪽).
- 넣지 않은 것: 기사 방패 20(아이템 없음), 네더 15종(구현 금지, 7급 촉매는 폐지된 결정), GeckoLib 입체 갑옷·애니메이션, `_emissive` 마스크, 입자, 작업대 작동 상태(`_active` 블록 상태). 성향석 4종은 자료에 그림이 없음.
- 게임 확인 TESTING v0.27 EQ1~EQ11, 빠른 확인 21회차.

### 2026-11-21 — Combat Traces 1.1.0 추가
- 사용자가 CurseForge 에서 받은 `combattraces-1.1.0.jar` 를 `modpack/local-mods/` 에 넣음(압축만 풀어 mods.toml·믹스인·라이선스 확인, 실행하지 않음). NeoForge [21.1.248,)·MC 1.21.1·Better Combat [2.4.0,2.5) 선택 → 팩(21.1.266, BC 2.4.0)과 맞음. 믹스인은 `client` 목록뿐이라 서버에서 안전. 라이선스 CC BY-NC-SA 4.0 + 모드팩·서버 배포 허가.
- CI: 테스트 팩·서버 기동 검사에 `modpack/local-mods/*.jar` 복사, 통합 팩 모드 수 24 → 25, 서버 로그에서 `Combat Traces 1.1.0 (combattraces)` 확인. 게임 확인 TESTING CT1~CT8.
- 앞서 "클라이언트 전용"으로 적었던 것은 틀림 — 모드 설명대로 서버에도 넣음.

### 2026-11-21 — 오버월드 최대 7급, 직업 무기 그림 v0.1 (fantasyrpg 0.26.0, magiccity `4da6f55`)
- 사용자 결정(`claude/등급재배치_7급오버월드_초안.md` A~E 추천안): 12장까지 모두 완료 → 7급, 12장 보스 7급 고정, 레벨 Lv.60 그대로, 네더 13~15장 첫 완료 수 → 8·9·10급(설계만), 16장 최종 각성(방향만)
- magiccity `ProgressionRules`(MAX_GRADE 7, LEVEL_GRADE 6), 등급표 7칸(예전 6칸 데이터도 읽음). fantasyrpg `CraftRules.MAX_UPGRADE` 7, 7급 승급·유산 7급 재료
- 무기 그림: 사용자 제공 zip(`fantasyrpg_weapon_assets_v0.1`)의 텍스처 120장·모델 120개를 `assets/fantasyrpg/{textures,models}/item/weapons|weapon_models|weapon_icons` 에, 등급 선택 모델 `weapon_sel/*_tNN`(NeoForge `separate_transforms`: gui = 아이콘, 나머지 = 3D) 60개 생성, 무기 6종 모델에 `fantasyrpg:tier` overrides. 클라이언트 속성 등록 `FantasyRpgClient`(FMLClientSetupEvent). zip 안 생성 스크립트는 쓰지 않음
- 게임 확인: G7-1~G7-6, WA1~WA6

### 2026-11-21 — 갑옷 파손·수리, 방어구 성향
- 파손: `FamilyArmorItem.damageItem` 이 내구도 1에서 멈춤, `isBroken` 이면 `FamilyArmorAttributes`·`SetEffects.worn`·`AffinityEffects` 가 건너뜀. 수리 `craft/FamilyArmorRepairRecipe`(일반 제작대), 재료 `ReworkRules.repairCost`
- 성향: 컴포넌트 `fantasyrpg:affinity`, 순수 `gear/armor/AffinityRules`(JUnit 3), `AffinityEffects`(10틱마다 합계 → 체력·가속 속성, `bonus()` 를 `RpgDamage`·`Cast.hit`·`SkillPassives.critBonus` 가 읽음), `AffinityTooltip`, 각인석 4종(`CraftRegistry.AFFINITY_STONES`), 1급 만들기에 각인석 선택, `FamilyArmorRetuneRecipe`. 데이터 `armor_affinity/settings.json`(전용 서버 동기화 포함)
- 게임 확인: `docs/TESTING.md` DR1~DR5, AF1~AF9

### 2026-11-21 — 외형 모드 추가 (Sol's Indicator, Effectual)
- 사용자 결정: 외형 모드 추천 조합. `mods.lock.json` 에 Sol's Indicator `1.21.1-v1.2.0`(보스바·피해 숫자·체력바, 서버에도)·Effectual `1.3.0-1.21.1`(클라이언트 전용, `"side": "client"`)
- 도구: `fetch_mods.py --side server` 면 클라이언트 전용을 받지 않음(서버 기동 검사), `build_mrpack.py` 는 그 모드의 서버 env 를 unsupported 로. 통합 팩 모드 수 22 → 24
- Combat Traces(무기 궤적)는 Modrinth 에 없어 받을 주소가 없음 → jar 를 받으면 `modpack/local-mods/` 경로로 넣을 예정
- 게임 확인: `docs/TESTING.md` VS1~VS8

### 2026-11-21 — 남은 프로그램 작업 일괄 v0.25.0 (magiccity `51ac66a`)
- 사용자 지시: "2부 네더·전용 그림을 제외하고 생성형 없이 프로그램만으로 되는 것은 추천안으로 전부 진행"
- **경갑 직업 룬석·스킬 가속:** 능력치 `fantasyrpg:core_regen`·`fantasyrpg:skill_haste`(`gear/RpgAttributes`, 플레이어에 추가). 룬석 `cycle`(투구·흉갑, 코어 증가량 +5/8/12/16%)·`haste`(투구·각반, +2/4/6/8), 데이터 `light_only` → `RuneRules.canInsert(..., lightArmor)` `LIGHT_ONLY`. 가속 = `SkillPassives.haste`(트리 `skill_haste` + 능력치), `SkillMath.hasteMult`, `SkillService` 쿨타임. 코어 `CoreService.gain/onEvent` 증가량 ×(1 + 순환), 자연 회복 보너스에도 더함. K 화면 쿨타임 표시에 가속(`ClientRpgState.haste`)
- **치명타 합계 상한:** `CombatSettings.critCap/critRoll` (모든 굴림 자리). **궁수 활 근접:** `ClassAttack.meleePhysical`, 태그 `class_weapons/archer/melee`
- **파티:** `party/PartyRules`(순수)·`PartyService`(근처 파티원, 회복·방벽 몫·경험치 받는 사람). `SkillRuntime.allyReduce*` → `SkillEvents` 받는 피해. `RpgProgress.onLivingDeath` 경험치 나누기. 설정 `skill_settings/party.json`
- **마을 판정:** magiccity `world/Towns` + `MagicCityApi.inTown/partyMembers`. `SkillService.masterySwapOk`(안전 + 마을, `presets.json mastery_swap.town_only`)
- **보스전 등급 차 경고(magiccity):** 투표 시작 때 보스 등급보다 2급 이상 낮은 파티원 이름을 알림 (`magiccity-progression.json warn_grade_gap`)
- **전용 서버 데이터 동기화:** `network/DataSync`(원본 기억·보내기·적용), `client/ClientDataSync`(내장 서버면 건너뜀), 리스너 6개가 생성자에서 읽기 코드 등록·apply 첫 줄에서 원본 기억. 모드 생성자 `DataSync.bootstrap()`. JEI `FantasyJeiPlugin.refresh()`
- **비용·빌드 프리셋:** `skilltree/CostRules`(순수)·`CostService`(인벤토리에서 내기, 창작 무료)·`BuildRules`(낮은 단부터 다시 찍기). `SkillPreset.ranks`. 플레이어 명령 `/rpgclass change <직업> [confirm]`. 설정 `skill_settings/costs.json`
- **분해·계열 전환:** `craft/ReworkRules`(순수)·`GridCrafting.rework`·`GridCraftMenu` 추가 결과(`extras`). 설정 `armor_crafting/costs.json rework`
- **magiccity 그 밖:** 물 패스 점검(`Placer`: WATER 패스는 공기·빛 블록 자리에만 물), royalqol 0.2.2 서리 결투장 좌표(-872, -2432, 계산값)
- 알아 둘 것: 전용 서버 동기화는 서버 기동 검사로 확인 불가(플레이어 접속이 있어야 보냄). 분해 결과 칸에는 첫 재료만 보이고 나머지는 가져갈 때 인벤토리로
- CI: JUnit 116개 통과, 서버 기동 검사 통과(새 grep 6줄)
- 게임 확인: `docs/TESTING.md` v0.25 HA1~HA20, 빠른 확인 19회차

### 2026-11-21 — 계열 방어구 세트 효과 v0.24.0
- 사용자 결정: `claude/세트효과_초안.md` A·B·C 추천안(표 그대로 [초안], 등급 구간 배율 ×1/×1.5/×2, 직업 제한 없음)
- `gear/armor/SetRules`(순수, JUnit 4: 판정·배율·데이터), `SetEffects`(데이터 `armor_sets/<계열>.json`, 10틱마다 입은 상태 갱신 → 속성 수정자 `fantasyrpg:set/...`(넉백 저항·불타는 시간·이동 속도·최대 흡수 체력), 받는 피해(고룡 화염 감소·기사 반격·고룡 역린, 보스 고정 피해 제외), 주는 근접 피해(드워프 자세), 효과 추가(드워프 느려짐 단축 — 다음 틱에 줄여서 다시 넣음), 엘프 질주 중 쿨타임 끝 시각 당김·1초마다 다시 보냄, 마도 코어 자연 회복·스킬 마법 보너스·다음 기본 공격 `DamageInput.withMagicCoef`), `SetTooltip`(룬석 툴팁 앞)
- 알아 둘 것: 툴팁 문구의 숫자는 기본값(데이터를 바꾸면 문구는 그대로). 반격·역린 피해는 가시 피해 유형(우리 RPG 계산 밖 작은 고정 피해)
- 게임 확인: `docs/TESTING.md` v0.24 SE1~SE10, 빠른 확인 18회차

### 2026-11-21 — 룬석 정식 재료·드롭 v0.23.0 (magiccity `e15c297`)
- 사용자 결정: `claude/룬석_재료드롭_초안.md` A~D 추천안(원석은 몬스터·보스만, 보스마다 정한 종류, 하급은 일반 제작대, 수치 초안)
- fantasyrpg: 재료 `rune_ore`(CraftRegistry `RUNE_MATS`), 하급 룬석 6·하급 연마재·추출 도구 조합식 교체, 자수정 임시 중급·상급 연마재 조합식 삭제 → `grid_recipes` 8개 추가(중급 룬석 6·중급·상급 연마재, 결과 `rune_grade`), 아이템 태그 `fantasyrpg:regional_materials`, `CraftRules.GridRecipe.resultGrade`·`resultKey`, `GridCrafting.resultOf`(JEI 도), 몬스터 드롭 `RuneDropRules`(순수, JUnit 3)·`RuneDrops`(데이터 `rune_drops/settings.json`, 플레이어가 잡은 `Enemy`, magiccity 표식 몹 제외)
- magiccity: 꾸러미 항목 `components`(DataComponentPatch, 못 읽으면 건너뜀), 챕터 보스 12종 `boss_materials`(5개 새로)·필드보스 10마리 `field_boss_loot` 에 룬석·원석 (JUnit 2)
- 게임 확인: `docs/TESTING.md` v0.23 L1~L8, 빠른 확인 17회차

### 2026-11-21 — 바닐라 전투 인챈트 정리 v0.22.0 (정책 P2·P2-1)
- 사용자 결정(`claude/바닐라인챈트_정리_초안.md` A·B·C 추천안): 이미 붙은 전투 인챈트는 지움, 모든 플레이어·몹, 최대치 넘는 보호는 최대치만큼
- 데이터(모드 jar, 바닐라 1.21.1 원본(misode/mcmeta `1.21.1-data`)에서 만듦): `data/minecraft/enchantment/` 전투 22종 `effects: {}` + 보호 4종 `clamped` 상한·`max_level` 3/2, `data/minecraft/tags/enchantment/` 획득 태그 5개 통째로 바꿈(`replace`, 다른 태그 참조 없이 나열), `data/fantasyrpg/tags/enchantment/disabled_combat.json`
- 코드 `gear/enchant/EnchantRules`(순수, JUnit 3 — 데이터 파일 대조 포함)·`CombatEnchants`(접속·40틱마다 인벤토리·커서에서 지움, 빈 책 → 책, 서버 시작 검사 로그)
- 발견: 1.21 약탈은 전리품 표가 무기 인챈트 레벨을 직접 읽어서 효과를 비워도 작동 → 지우는 방식으로 해결
- 알아 둘 것: 인챈트는 동적 레지스트리라 서버를 다시 켜야 적용. 상자·화로 안 아이템은 플레이어 손에 들어올 때 지워짐
- 게임 확인: `docs/TESTING.md` v0.22 I1~I8, 빠른 확인 16회차

### 2026-11-20 — 라디키온 한계 고침 v0.21.0-test.2 (magiccity `8265530`)
- 사용자 질문 "한계 때문에 생기는 문제는?" → 확인: 마법사 지팡이·한 대상 스킬은 라디키온에게 안 맞음(경로 중 몸체 조준 불가, 마디는 생물 아님), 범위 스킬은 머리에만·적대 분류가 아니라 아예 대상 아님, 방어구 무시 공격에 등급 배율 ×3 → 6급 한 방. 사용자 결정: 추천안
- fantasyrpg `combat/PartHits`(같은 틱 "이 몸체의 이 부위" 기록 → `RpgDamage.apply(target, attacker, in)` 이 부위로 돌림), `SkillTargets` look·firstAlong·line·sweep·cone·around 가 부위도 찾고 몸체로 바꿈(중복 제거, 가장 가까운 부위), `area` 에 여러 부위 몹·제어 면역 보스 포함, 지팡이 마법 탄도 부위
- magiccity `ProgressionRules.skipScaling`: 외부 보스의 `BYPASSES_ARMOR` 피해는 배율 없음 (JUnit 1)
- 게임 확인: Z11~Z13

### 2026-11-20 — 라디키온을 3장 보스 전장에서 v0.21.0-test.1 (fantasyrpg 0.21.0, magiccity `b5c5728`)
- 사용자 결정: 소스 공개 허락 → magic-city-mod `fantasybosses/`(첫 커밋 `388515c` = 0.7.5.1 그대로, 브랜치 `fantasybosses-v0.7.5.1`), v0.8 한 번에, 3장 우리 고정 피해 끔, 배율 기준 6
- fantasybosses 0.8.0: `ArenaRules` 이동량 규칙(JUnit 5), `RadicionEntity` 이동량 동기 데이터·NBT `RadicionOffset`·`head/anchors/bounds/inArena/floorY`, 경로·기관·균열·바닥·상자·청크·렌더러·BGM 모두 이동량 적용, 자동 시작 30초 재시도
- magiccity: `boss_entity` `offset_nbt`·`design_attack`·`our_patterns`, 이동량 = 전장 보스 자리 − 왕국 대륙 보스 자리(보스 방 사본이 있을 때만), `Progression.apply(…, design)`, `BossPatterns` 외부 보스 끄기 (JUnit 1 추가)
- fantasyrpg: `RpgDamage.body`·`apply(target, hit, …)` — 근접·화살이 부위를 맞히면 몸체로 계산해 부위에 피해(스킬 범위 피해는 몸체에 = 배율 1.0)
- 팩: `tools/build_fantasybosses.sh` + `mods.lock.json` `fantasybosses`, local-mods jar 삭제
- 복구 지점 `backup/main-before-fantasybosses-2026-11-20`(magiccity), `backup/main-before-radicion-arena-2026-11-20`(fantasyrpg)
- 게임 확인: `docs/TESTING.md` v0.21 Z1~Z10, 빠른 확인 15회차

### 2026-11-20 — 라디키온 0.7.5.1 등록 v0.20.0-test.4
- 사용자: 인계 zip `FantasyBosses_v0.7.5.1_Claude_handoff.zip`(소스·인계서) + 빌드한 `fantasybosses-0.7.5.1.jar`. JAR 를 받은 소스와 대조(클래스 목록·녹화 경로·공격 포즈·모델·BGM 일치) → `modpack/local-mods/` 의 0.7.2.2 를 0.7.5.1 로 교체, 서버 기동 검사에 모드 이름 줄
- 소스는 ARR 이고 두 저장소가 공개라 올리지 않음 → 위치 결정 대기(`claude/FantasyBosses_등록_v0.8_초안.md` 6절 A)
- 소스 분석(초안 2절): 절대 좌표 9곳 → 우리 전장에서는 서 있음, 마디(`PartEntity`)는 fantasyrpg RPG 피해를 안 거침, 라디키온에 공격력 속성이 없어 등급 배율 1.0. v0.8(전장 원점 변환·부위 RPG 피해·`design_attack`)은 결정 뒤
- 게임 확인: `docs/TESTING.md` X9~X11(원래 둥지에서 기준 동작)

### 2026-11-20 — 외부 보스 연결·보스 수치 데이터 v0.20.0-test.3 (magiccity `0dc385a`)
- 사용자 결정: `claude/외부보스연결_초안.md` A~D 추천안(장마다 데이터로 연결 / 외부 모드가 기준점 NBT 를 받게 고침 / 외부 보스면 우리 기술·체력바 끄고 고정 피해만 / jar 팩 포함). `claude/보스수치데이터_초안.md` 는 그 4절로 합침
- magiccity: `boss/BossDataRules`(순수, JUnit 6) · `BossData`(로더 3개 `boss_stats`·`field_boss_stats`·`boss_entity`, 값을 준 데이터팩 이름 기억, 외부 몹 소환 `loadEntityRecursive` + 기준점 `NbtUtils.writeBlockPos`) · `ManagedBoss.wake/fight`(외부면 기술·부하·광폭화·대상 지정·묶어 두기·체력바 끔) · `Progression`(등급표 데이터) · `FieldBosses` · `/magiccity boss stats [키|장]`. 데이터 12 + `_grades` + 10 + `boss_entity/parasite.json`
- 팩: `modpack/local-mods/fantasybosses-0.7.2.2.jar`(sha1 b9bb5631…), build.yml 테스트 팩·통합 팩에 넣고 모드 수 22, 서버 기동 검사 1단계(없음 → 기본 보스 1)·2단계(몹 있음 1) 로그 확인, 견본 데이터팩 `modpack/datapacks/magiccity_boss_stats_sample`(지금 값 + 안내.md)
- 알려진 한계: 라디키온은 경로가 왕국 대륙 둥지 좌표(x −2895~−2825, z −895~−825)에 고정이라 전장에서는 움직이지 않음, 전투도 아직 없음 → 외부 모드가 `RadicionAnchor`(정수 배열 x,y,z = 전장 안 보스 자리)를 읽어 상대 좌표로 재생하도록 바뀌어야 함(초안 3절). 기준점 기준: 왕국 대륙 = RootDungeon `BOSS_X/Y/Z`
- CI: magiccity JUnit(새 6개 포함) 통과, fantasyrpg JUnit 92·서버 기동 검사 통과 — 1단계(우리 모드만) `몹 없음 → 기본 보스 1`, 2단계(전체 22개) `FantasyBosses 0.7.2.2` 불러짐·`몹 있음 1`. 사전 릴리스 `v0.20.0-test.3`
- 복구 지점 `backup/main-before-boss-data-2026-11-20`(magiccity), `backup/main-before-external-boss-2026-11-20`(fantasyrpg)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.20 X1~X8, 빠른 확인 14회차

### 2026-11-20 — 보스 방 복사 전장·확장 작업대·방어구 제작법 v0.20 (fantasyrpg 0.20.0, magiccity `89538b4`·`4b0cb3d`)
- 사용자 결정(두 초안 모두 추천안): `claude/보스방복사전장_초안.md`(처음 입장 때 뜨기, 보이지 않는 벽, 자석석만 빼기) + 크리에이티브 예외 없앰, `claude/작업대_방어구제작_초안.md`(모양 없음, JEI, 지역 소재 = 챕터 보스 + 지역 필드보스, 유산 2~6급 부품)
- magiccity: `arena/BossRoomRules`(순수, JUnit 4)·`BossRooms`(뜨기·저장 `magiccity/boss_rooms/chN.nbt`·깔기·정리·경계·스폰 자리), 데이터 `boss_rooms/*.json` 12, `/magiccity boss room-capture|room-clear`, 보스 꾸러미 지역 소재(`boss_materials` 7 + 필드보스 5), 다른 모드 아이템 없으면 건너뜀
- fantasyrpg `craft/`: `CraftRules`(순수, JUnit 5), `CraftData`(grid_recipes 13·armor_crafting/costs.json), `GridCrafting`(승급 먼저, 그다음 조합식), `GridCraftMenu`·`GridBenchBlock`·`client/GridCraftScreen`(그림 없이 칸 그림), `FamilyArmorCreateRecipe`(일반 제작대 1급), `CraftRegistry`(블록 2·재료 17·메뉴 2·창작 탭), `compat/jei`(JEI API 19.51.0.418 compileOnly, maven.blamejared.com). 유산 `HeirloomSettings.Cost` 여러 재료(`items`), 2~6급 부품
- 알려진 한계: 전용 서버에 접속한 클라이언트는 조합식 데이터가 없어 JEI 분류가 비어 보임(혼자 하기는 보임) → 동기화 필요. 재료 그림은 바닐라 임시. 7~10급 승급·분해·계열 전환 없음
- CI: JitPack 첫 요청 시간 초과·429 → 몇 분 뒤 다시 실행해 통과. JUnit 92개, 서버 기동 검사 통과
- 복구 지점 `backup/main-before-workbench-2026-11-20`(fantasyrpg), `backup/main-before-boss-rooms-2026-11-20`(magiccity)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.20 R1~R9·W1~W11, 빠른 확인 13회차
- 2026-11-20 사용자: R1 통과(로그 4줄). 사용자 결정 "크리퍼 전부 삭제·생성 불가" → magiccity `ef3166c`(`world/NoCreepers` 들어오기 취소 + `neoforge/biome_modifier/no_creepers.json` 자연 생성 목록 제거) → `v0.20.0-test.2`. 게임 확인 R0

### 2026-11-19 — 챕터 보스 인스턴스 전장·필드보스 꾸러미 v0.19 (magiccity `357cfee`)
- 사용자 결정(초안 `claude/인스턴스전장_구현초안.md` v1→v3): FTB 파티 전원 투표(30초, 만장일치, 거절·시간 초과 뒤 10초), 투표 대상·입장 = 접속 중인 파티원 전원(어디에 있든), 전장 지형은 나중에(1차 공용 기본 + 6장 물 돔), 인벤토리 보존·사망 = 관전·부활은 아이템만(부활의 기도 허용), 챕터당 한 팀, 30분, 보스 12종 한 번에. 필드보스: 15분 재등장 유지, 기여도 없이 파티원 꾸러미, 첫 처치 큰 보상 / 반복 작은 보상, 반복에도 다이아 1·철 3
- magiccity: 차원 `magiccity:boss_arena`, `arena/`(ArenaRules 순수·ArenaManager·ArenaShapes·SealAltar·Party(FTB Teams 리플렉션)·ArenaCommand), 아이템 `magiccity:loot_pouch`(+ Pouches 가득 차면 방을 나간 뒤), 개별 보스 5종 → `ClassicBosses`(공용 틀), 지역에서는 더 이상 보스가 깨어나지 않음(8장 습격만), `Story.creditPlayers`, 보스 고정 피해가 전장에서도, 필드보스 `FieldLoot`·데이터 10개·첫 처치 기록. JUnit `ArenaRulesTest` 6·`FieldLootRulesTest` 3. 첫 빌드에 컴파일·테스트 통과
- fantasyrpg: 코드 변경 없음(전장 사망은 magiccity가 가장 늦게(LOWEST) 받아 부활의 기도가 먼저 막음). magiccity 커밋·0.19.0, 서버 기동 검사에 꾸러미 10개·전장 차원 오류 확인 줄
- 복구 지점 `backup/main-before-arena-2026-11-19`(두 저장소)
- 게임 확인(2026-11-19 사용자): V1·V2 통과. V4에서 3장 보스(좀벌레)가 몇 초 뒤 바닥에 숨어 사라짐 → "보스가 사라졌다"로 끝나는 순간 서버 멈춤(`ArenaManager.finish`가 참가자 목록을 도는 중 귀환 이동의 차원 이동 이벤트가 목록을 고침). magiccity `b5ab1ec`로 둘 다 수정(전투를 먼저 목록에서 빼고 사본으로 귀환, 전장에서 `EntityMobGriefingEvent` 금지) → `v0.19.0-test.2`
- 2026-11-20 게임 확인(사용자 스크린샷): 하수도 납골당 바닥에 물이 흘러 양탄자가 쓸려 감. 원인은 도시 건설 물 패스(`Placer` WATER 패스)가 나중에 다른 블록으로 덮인 물까지 놓는 것 — 하수로 물길 끝(납골당 입구 벽 z 279·계단 위 z 278, y F)에 물 근원. magiccity `8d2c924`: 물길은 방과 닿는 끝 칸에 물을 두지 않음, 기존 월드용 `/magiccity fix crypt` → `v0.19.0-test.3`. 같은 물 패스 성질이 다른 건물에도 있을 수 있음(알려진 위험)

### 2026-11-19 — 장별 중간 단계 v0.18 (magiccity `887924c`)
- 사용자 결정: 초안 `claude/장별중간단계_초안.md` 승인("진행하자", 추천안) — 빠진 커밋 병합, 안내형, 8개 장 전부, 단계마다 경험치 등급 × 50
- 발견·처리: 통합문서에 "구현됨"이던 magiccity `d5a4a3c`(심층 광산)·`791459a`(보라빛 숲)가 옛 작업 브랜치에만 있었음 → `main`에 병합(`1d9f710`, 충돌 없음, CI 통과)
- magiccity: 단계 종류 `talk_npcs`·`use_blocks`, 자리 이름 `QuestMarks`(사이트 상수에서 계산, 높이 범위 포함), 퀘스트 판 번호·여러 개 표시 저장, 장을 깨면 건너뛰기, `QuestStepCompleteEvent`, 지하 NPC 높이·크기, 심층 광산 광부·문지기를 퀘스트 NPC로, 8개 장 데이터(판 2), 지하·하늘 보스 도착 단계 높이 범위. JUnit `MidStepRulesTest` 9개
- fantasyrpg: 중간 단계 경험치, magiccity 커밋 `887924c`, mod_version 0.18.0, 서버 기동 검사 NPC 14명. JUnit 1개(전체 87개)
- 초안과 달라진 점: 2장 교수대(브란이 바로 옆에 서 있음)·9장 학자 야영지(셀레스 자리)는 빼고, 10장 사슬 다리 → 나선 계단 중턱, 11장 모험가 야영지(미궁 방 배치가 시드마다 다름) → 상층·하층 미궁
- 복구 지점 `backup/main-before-mid-steps-2026-11-19`(두 저장소)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.18 J1~J12, 빠른 확인 11회차

### 2026-11-19 — 보스 고정 피해 패턴 v0.17 (magiccity `7dad744`)
- 사용자 결정: 초안 `claude/보스고정피해_초안.md` 승인("진행하자", 추천안) — 직업 방어 기술 모두 통과(A안), 비율형은 등급 무관·고정형은 등급 계수, 12장 보스 모두 1개씩, 수치 초안
- 기획 v0.4 5.3에 결정 추가(`00ebcde`), 프로젝트 밸런스 문서 6-1절 갱신
- magiccity: 피해 유형·바닐라 태그 6개, `FixedStrikeRules`(순수)·`BossPatterns`(데이터 로더·예고·판정, 보스 클래스와 따로 태그로 찾음), 패턴 12개, `Progression` 배율 제외, API `isBossFixed`, `/magiccity boss pattern|patterns`, 사망 메시지, JUnit 6개
- fantasyrpg: `SkillEvents`에서 고정 피해면 건너뜀, magiccity 커밋 `7dad744`, mod_version 0.17.0, 서버 기동 검사에 보스 패턴 12개·태그 오류 확인 줄. 이 저장소 JUnit은 늘지 않음(MC 피해 원본 의존이라 게임 확인 B8로)
- 복구 지점 `backup/main-before-boss-fixed-2026-11-19`(두 저장소)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.17 B1~B14, 빠른 확인 10회차

### 2026-11-19 — 2~12장 챕터 퀘스트 v0.16 (magiccity `751513a`, 브랜치 `claude/chapter-quests`)
- 사용자 결정: 초안 `claude/챕터퀘스트_2-12장_초안.md` v2 승인("진행하자") — 의뢰 NPC 11명(9명 + 시종장), 메인 줄기 "초대 왕의 유산을 되살리는 여정", 완료 보상에 강화 재료(시험용)
- magiccity: 퀘스트 장·선행 장 조건, 보스 자리 자동 좌표, 지역 거점 자리(`QuestAnchors`)·근처에 오면 NPC 소환, 퀘스트 11개·NPC 9명, `/quest` 받을 수 있는 의뢰 목록, JUnit 9개
- fantasyrpg: 각성 단계가 오를 때 강화 재료 지급(JUnit 1개, 전체 86개), 서버 기동 검사 확인 줄을 퀘스트 12개·NPC 10명으로, mod_version 0.16.0
- CI: magiccity(퀘스트 JUnit 9개) 통과, fantasyrpg 첫 빌드에 통과(JUnit 86개, 서버 기동 검사 퀘스트 12개·NPC 10명 로드). `main` 병합(`f309392`), 복구 지점 `backup/main-before-chapter-quests-2026-11-19`, 테스트 팩 v0.16.0-test.1
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.16 E1~E11, 빠른 확인 9회차

### 2026-11-19 — 유산 무기 v0.15 (브랜치 `claude/heirloom`, magiccity `9bd0a49`)
- 사용자 결정: 메인 스토리는 무기 하나를 계속 강화하는 흐름, 성장은 섞기(진행 + 재료), 하나뿐인 주무기, 무기에 성격·대사 없음. 초안 `claude/유산무기_초안.md` 기본값 승인("진행하자")
- 기획 v0.4 6.1·6.3 먼저 수정 → fantasyrpg 유산 무기 전체(위 "동작하는 것"), JUnit 5개(전체 85개), 서버 기동 검사 확인 줄, mod_version 0.15.0
- magiccity: 퀘스트 NPC 상시 선택지(services), 시종장 `[유산을 다시 부른다]`, 1장 완료 대사
- CI: 첫 빌드에서 레코드 생성자 안 인자 실수로 컴파일 오류 → 고친 뒤 JUnit 85개·서버 기동 검사(유산 설정 로드 줄) 통과. `main` 병합(`7125f03`), 복구 지점 `backup/main-before-heirloom-2026-11-19`, 테스트 팩 v0.15.0-test.1
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.15 H1~H14, 빠른 확인 8회차

### 2026-11-19 — 기사 기본 가드·장착 프리셋 v0.14 (브랜치 `claude/guard-presets`)
- 사용자 결정: 초안 `claude/가드_프리셋_초안.md` — 가드는 기사만, 직업이 있으면 바닐라 방패 금지, 프리셋은 장착칸·마스터리만 (모두 추천안)
- 기획 v0.4 5.3·7.1 먼저 수정 → `GuardSettings`·`GuardEvents`·`PresetRules`·`SkillPreset`, 창 사용 동작, K 화면 P1~P3, 데이터 `combat/guard.json`·`skill_settings/presets.json`, JUnit 7개(전체 80개), 서버 기동 검사에 가드·프리셋 로드 줄 확인, mod_version 0.14.0
- CI: 첫 빌드에 통과(JUnit 80개, 서버 기동 검사에서 가드·프리셋 로드 줄 확인). `main` 병합, 복구 지점 `backup/main-before-guard-presets-2026-11-19`, 테스트 팩 v0.14.0-test.1
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.14 D1~D15, 빠른 확인 7회차

### 2026-11-19 — 서장 NPC·직업 선택 (fantasyrpg v0.13 + magiccity 퀘스트 최소판)
- 사용자 결정: 초안 `claude/서장NPC_직업선택_초안.md` 승인("진행하자") — A안(자체 대화), 왕실 시종장 에드윈·왕좌의 전당, 1급 무기 지급, 직업 변경은 NPC에 넣지 않음
- magiccity: `quest` 패키지(정의·규칙·저장·대화·명령, JUnit 7개), 왕좌의 전당 NPC 자동 소환·`/magiccity fix npcs`, 공개 API `questSignal`·`questState`·`questDone`·`NpcActionEvent`·`QuestStepStartEvent`, 데이터 `quests/kings_call.json`·`quest_npcs/steward.json`
- fantasyrpg: `ClassSelection`, `ClassSelectScreen`, 패킷 2개, 언어 30줄, `/rpg class choose`도 퀘스트 신호, 서버 기동 검사에 퀘스트 로드 줄 확인 추가, mod_version 0.13.0
- CI: magiccity `9051cbd`(퀘스트 JUnit 7개 포함) 통과, fantasyrpg 빌드·JUnit 73개·서버 기동 검사(퀘스트 1개·NPC 1명 로드, `magiccity quest list`) 통과. 빌드 캐시에 예전 magiccity jar가 남아 테스트 팩에 두 개 들어가던 문제를 고침(`magiccity_commit` jar만 복사). 테스트 팩 v0.13.0-test.1
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.13 W1~W15, 빠른 확인 6회차

### 2026-11-19 — 6단 마스터리·혼합 노드 (브랜치 `claude/mastery-mixed`)
- 사용자 결정: 초안 `claude/6직업_마스터리_혼합_초안.md` 승인("진행하자")
- 추가: 활성 마스터리 저장·교체(`SET_MASTERY` 요청, `/rpg skill mastery`), 합산 키 `low_health_reduction`·`core_regen`·`kill_core`·`after_hurt_bonus`, 혼합 노드용 작은 코드(강격·반격 찌르기·관통 사격 마법, 점멸 흡수 체력, 패리 → 창돌진 초기화, 회피 → 투명), K 화면 혼합 줄·활성 표시, 데이터 36노드(전체 156), JUnit 1개(전체 73개)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.12 Y1~Y9

### 2026-11-19 — 6직업 5단 궁극기 (브랜치 `claude/ultimates`)
- 사용자 결정: 초안 `claude/6직업_궁극기_초안.md` 승인("진행하자")
- 추가: `UltimateSkills` 24종, 지속형 궁극기 상태·받는 피해·끝날 때·부활(죽음 이벤트 취소), 시간 가속, 다음 스킬 보너스, K 화면 툴팁에 modifies 반영 쿨타임·"강화 중" 줄, 데이터 24노드, JUnit 1개(전체 72개)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.11 G1~G8

### 2026-11-19 — 6직업 3~4단 스킬 (브랜치 `claude/tier34-skills`)
- 사용자 결정: 범위 6직업 3~4단, 초안 승인("진행하자"). 초안 프로젝트 문서 `claude/6직업_3-4단_스킬_초안.md`
- 추가: `SkillNode.mods`(modifies), 패시브 합산(`SkillPassives.sum`)·회복 공통(`heal`), 연쇄 공통(주문 연쇄·연계 숙달), 3단 액티브 24(직업 파일에 추가), 4단 패시브 24(대부분 데이터), 응징의 방패·견제 덫·가속 주문·결계 깨짐·생존 본능·수호 서약(해로운 효과 차단) 이벤트, 데이터 48노드, JUnit 2개(전체 71개)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.10 T1~T15

### 2026-11-18 — 기사·도적·궁수·전투사제 1~2단 스킬 (브랜치 `claude/four-class-skills`)
- 사용자 결정: 다음 = 나머지 4직업 1~2단, 초안 승인("진행하자"). 초안 프로젝트 문서 `claude/4직업_스킬_초안.md`
- 추가: 직업 4종 × 액티브 4, 패시브 묶음 공통화(전사 데이터 키 하나 이름 바꿈), 패리·회피·도약·돌진·회복·해로운 효과 해제·언데드·등 뒤 판정·시간 제한 이동 속도, 데이터 32노드·코어 4, JUnit 2개(전체 69개)
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.9 N1~N22

### 2026-11-18 — 전사·마법사 1~2단 스킬 (브랜치 `claude/warrior-mage-skills`)
- 사용자 결정: 범위 1~2단, 초안 먼저 보여 주고 승인("진행하자"), 연출은 입자·소리만. 초안 프로젝트 문서 `claude/전사마법사_스킬_초안.md`
- 추가: 노드 수치 형식(`SkillValues`), 사용 흐름(무기·쿨타임·코어·연쇄), 전사·마법사 액티브 8개·패시브 8개, 쿨타임 패킷·HUD 표시·설명 툴팁, 데이터(노드 16·코어 2·제어 면역 설정), JUnit 7개(로컬 shim 통과)
- 중간에 GitHub 쓰기 권한이 끊겨(push 403) 사용자가 claude.ai에서 GitHub를 다시 연결한 뒤 push. 첫 CI에서 컴파일 오류 1건(실행 상태 필드 접근 범위) 수정 → JUnit 67개·서버 기동 검사 통과
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.8 K1~K18

### 2026-11-18 — 스킬트리 틀 (브랜치 `claude/skilltree-frame`)
- 사용자 결정: 되돌리기 "1번 기조 + 엔딩 후 3번", 공통 패시브 별도 트리·포인트(등급마다), 조작 K 화면 + Z X C V + B, 범위 "틀만"
- 기획 먼저: 기획 v0.4 7.1·부록 A-2 수정(커밋 bad7413), 프로젝트 문서 `claude/스킬트리_설계.md` 신설
- 추가: 순수 규칙 + JUnit 9개(부록 A-2 포함) → CI 60개 통과. 저장·패킷·서버 처리·코어·클라이언트 화면/HUD/단축키, 시험 데이터팩, 서버 기동 검사에 `[스킬]` 로드 줄 확인 추가
- 게임 확인: 하지 않음 → `docs/TESTING.md` v0.7 S1~S17

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
