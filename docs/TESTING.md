# 게임 안에서 테스트하는 방법

## 1. 준비 (처음 한 번)

### 1-A. 통합 테스트 팩으로 한 번에 설치 (권장)
우리 모드 3개(`magiccity`·`fantasyrpg`·`royalqol`)와 1차 외부 모드 18개, Minecraft 1.21.1 + NeoForge 21.1.266을 한 번에 맞춥니다.

1. **Prism Launcher**를 설치합니다 (https://prismlauncher.org, 무료).
2. 통합 팩 파일 `magic-kingdom-testpack-*.mrpack`을 받습니다.
   - **Releases** (권장): 저장소 오른쪽 Releases의 최신 테스트 팩 → `magic-kingdom-testpack-<버전>.mrpack`
   - **최신 빌드**: Actions 탭 → 가장 최근 성공한 `core-mod build` → Artifacts의 `magic-kingdom-testpack-mrpack` (zip을 풀면 `.mrpack`)
3. Prism Launcher → **인스턴스 추가** → 왼쪽 **가져오기(Import)** → `.mrpack` 파일 선택 (Releases 파일 주소를 그대로 붙여 넣어도 됨) → 확인.
   - 외부 모드 18개를 자동으로 내려받습니다.
   - 런처가 **"신뢰할 수 없는 모드"** 창을 띄울 수 있습니다. 목록에 FTB Library·Teams·Quests(FTB 공식 저장소 maven.ftb.dev에서 받음)와 우리 모드 3개(팩 안에 들어 있음)가 보이면 정상이니 **확인(수락)**을 누르세요. Modrinth가 아닌 곳에서 오는 파일을 알려 주는 창입니다.
4. 인스턴스 설정 → Java 메모리를 **4GB 이상**으로 둡니다.
5. 인스턴스 폴더(인스턴스 우클릭 → 폴더 열기)에 `datapacks/`(테스트 데이터팩 2개)와 `테스트방법.md`(이 문서)가 같이 들어 있습니다.

새 테스트 팩이 나오면 인스턴스를 새로 가져오는 것이 가장 깔끔합니다(월드는 `saves/`를 옮기면 됨).

### 1-B. 직접 설치 (팩을 쓰지 않을 때)
1. Prism Launcher 또는 CurseForge 앱에서 새 인스턴스를 만듭니다. Minecraft **1.21.1**, 로더 **NeoForge 21.1.266 이상**.
2. Actions의 `fantasyrpg-testpack`(또는 Releases의 `fantasyrpg-testpack-*.zip`)을 받아 풀면:
   ```
   mods/fantasyrpg-<버전>.jar
   mods/magic-city-mod-<커밋>.jar   (필수 의존 모드 magiccity)
   mods/royalqol-<버전>.jar         (편의 모드, 클라이언트 전용)
   datapacks/fantasyrpg_content/
   datapacks/fantasyrpg_test/
   kubejs/ (아직 비어 있음)
   ```
3. `mods/` 안의 jar를 **모두** 인스턴스의 `mods` 폴더에 넣습니다. 새 버전이 나오면 이전 jar는 지우세요.
4. 외부 모드 18개(선택): `modpack/mods.lock.json`에 버전까지 고정돼 있습니다. 파이썬이 있으면 `python3 tools/fetch_mods.py modpack/mods.lock.json 받은폴더`, 없으면 이름·버전대로 Modrinth(FTB 3종은 CurseForge)에서 받습니다.

서버 쪽 동시 로딩은 CI `modpack smoke`에서 통합 팩을 실제로 설치해 확인합니다(우리 모드 3개 + 외부 18개). 아래 "모드 혼합"·"royalqol" 표는 클라이언트·게임 안에서만 확인할 수 있는 항목입니다.

## 2. 테스트 월드

1. 새 월드 생성 화면에서 **치트 허용: 켜기**, 게임 모드 **크리에이티브**.
2. **데이터 팩** 버튼 → 열린 폴더에 `datapacks/`(통합 팩이면 인스턴스 폴더 안) 안의 두 폴더를 복사 → 둘 다 "선택됨"으로 옮깁니다.
3. 이미 만든 월드라면 `saves/<월드>/datapacks/`에 두 폴더를 넣고 게임 안에서 `/reload`.

## 3. 버전별 확인 항목

### v0.1 — 피해 계산 엔진 (8.3장 1단계)

비율은 0~1로 입력합니다(25% → 0.25). 결과는 채팅에 단계별로 나옵니다.

| # | 입력 | 기대 결과 |
|---|---|---|
| 1 | `/rpg simulate 100 2 0 0.25 0` | 최종 피해 **150** |
| 2 | `/rpg simulate 100 2 0 0.25 0 0.1` | **170** (물리 관통 10%p) |
| 3 | `/rpg simulate 100 1 1 0.25 0.15` | **160** (혼합) |
| 4 | `/rpg simulate 100 2 0 0.1 0 0.2` | **200** (관통이 저항보다 커도 증폭 없음) |
| 5 | `/rpg simulate 100 2 0 0.25 0 0 0 1.5` | **225** (치명타는 저항 후 적용) |
| 6 | 좀비 소환 후 `/rpg resist @e[type=zombie,limit=1,sort=nearest]` | 물리 25% / 마법 15% |
| 7 | `/rpg hit @e[type=zombie,limit=1,sort=nearest] 4 2 0` | 피해 **6.0**, 체력 20 → 14 |
| 8 | 좀비에게 다이아 갑옷을 입히고 7번 반복 | 똑같이 **6.0** (바닐라 방어구 무시) |
| 9 | 7번을 연속으로 빠르게 두 번 | 두 번 모두 피해 (무적 시간 무시) |
| 10 | 스켈레톤에 `/rpg hit ... 4 0 2` | 마법 40% 저항 → **4.8** |
| 11 | 게임 로그(`logs/latest.log`) 확인 | `iron_golem ... 기획 5.1장 위반` 오류 1줄 |

이상한 점이 있으면 입력한 커맨드와 채팅 출력(스크린샷)을 그대로 알려주세요.

### v0.2 — 방어 등급 상한 + 바닐라 다이아·네더라이트 조정 (최소 기능)

설계: 프로젝트 문서 `claude/장비등급제한_설계.md`. 등급은 magiccity 진행도(완료한 메인 챕터 수, 0~1개 1급 … 10개 이상 6급)이고, 7급 이상은 아직 magiccity에 없어서 테스트용 덮어쓰기로 시험합니다.

준비: **서바이벌 모드**(크리에이티브는 피해를 받지 않음), 난이도 보통, 테스트 데이터팩 적용. 갑옷은 `/give`로 받아 직접 입습니다. 피해 시험은 `/function fantasyrpg:armorcap/hit10` (피해 10, 전후 체력 표시) — 매번 체력을 가득 채운 뒤(`/effect give @s minecraft:instant_health 1 4`, 다음 틱에 회복) 실행하세요. 상태 확인은 `/rpg armorcap info`.

| # | 상황 | 확인 | 기대 결과 |
|---|---|---|---|
| 1 | 새 월드(완료 0개), 철 풀세트 | `/rpg armorcap info` | 등급 1급(magiccity), 방어력 원래 15 → 목표 12, HUD 방어 칸 6개 |
| 2 | 1번 상태에서 `hit10` | 체력 | 피해 **7.2** (20 → 12.8) |
| 3 | `/magiccity story mark 1`, `mark 2` (완료 2개) | 바로 `info` | **2급**, 방어력 15 (보정 없음). `mark`는 이벤트를 내지 않지만 다음 틱에 반영돼야 함 |
| 4 | 3번 상태에서 `hit10` | 체력 | 피해 **6.0** (20 → 14) |
| 5 | 다이아 흉갑 툴팁 | F3+H 없이 툴팁 | 방어력 +7, 방어 강도 +1 (바닐라 +8 / +2에서 조정) |
| 6 | `mark 3`, `mark 4`(완료 4개 → 3급), 다이아 풀세트 | `info` → `hit10` | 18/2, 보정 없음, 피해 **4.4** (→ 15.6) |
| 7 | `/rpg armorcap grade @s 6`, 네더라이트 풀세트 | `info` → `hit10` | 방어력 20, 강도 6 → 목표 **4**, 피해 **3.333** (→ 16.667) |
| 8 | `/rpg armorcap grade @s 7` | `info` → `hit10` | 20/6 그대로(보정 없음), 피해 **3.143** (→ 16.857) |
| 9 | `/rpg armorcap grade @s 1`, 네더라이트 풀세트 | `info` → `hit10` | 12/0, 피해 **7.2** |
| 10 | 1급 + 철 풀세트에서 `/function fantasyrpg:armorcap/ring` (방어력 +3, 장신구 흉내) | `info` | 원래 18 → 목표 **12** (장신구도 상한에 포함). 해제: `ring_off` |
| 11 | 1급 + 철 풀세트에서 `/function fantasyrpg:armorcap/multiplier` (최종 ×1.5) | `info` → `hit10` | 원래 22.5 → **12**, 피해 7.2. 해제: `multiplier_off` |
| 12 | 1급 + 철 풀세트에서 `/function fantasyrpg:armorcap/same_tick` | 출력된 전후 체력 | 같은 틱에 방어력 +10 후 피해 → 피해 **7.2** (상한 적용). 2.0이면 피격 직전 갱신 실패 |
| 13 | 7번 상태에서 서버 재접속(또는 싱글 월드 다시 열기) | `info` | 보정이 다시 붙어 있음. 덮어쓰기는 재시작하면 사라짐(의도) |
| 14 | 네더로 이동했다 돌아오기, 죽었다 부활 | `info` | 보정 유지·복구 |
| 15 | `/rpg armorcap grade @s clear` | `info` | magiccity 등급으로 돌아감 |
| 16 | 좀비에게 다이아 갑옷 입히고 `/attribute @e[type=zombie,limit=1,sort=nearest] minecraft:generic.armor get` | 값 | **20** = 좀비 기본 방어 2 + 다이아 18 (바닐라 조정은 몹 갑옷에도 적용, 상한은 플레이어만) |
| 17 | 아무것도 입지 않고 1~3번 반복 | `info` | 보정 없음, 오류 없음 |
| 18 | `logs/latest.log` | "방어 상한 보정이 목표값에 맞지 않음" | **나오면 안 됨** |

#### 자동 테스트와 게임 안 테스트 구분 (2026-11-17)
`ArmorCapScenarioTest`(JUnit)가 위 표의 **수치**를 Minecraft 없이 다시 계산합니다: 장비 수정자 → `ArmorCapMath` 보정 → 정수 내림 → 바닐라 1.21.1 갑옷 공식(`CombatRules.getDamageAfterAbsorb`, 테스트 안에 옮겨 적음).
자동 테스트가 통과해도 게임 안 동작(이벤트·틱 갱신·저장·HUD·실제 바닐라 코드)은 확인되지 않으므로 아래 "게임 안" 항목은 그대로 해야 합니다.

| # | 자동 테스트 (JUnit) | 게임 안에서만 확인 |
|---|---|---|
| 1·2 | 목표 12, 피해 7.2 | magiccity 등급 읽기, HUD 방어 칸 6개, 실제 체력 변화 |
| 3·4 | 2급 보정 없음, 피해 6.0 | `story mark` 뒤 다음 틱 반영 (이벤트 없는 변경) |
| 5 | 설정 기본값 7/1 | 툴팁 표시 (`ItemAttributeModifierEvent`) |
| 6 | 18/2 보정 없음, 피해 4.4 | 다이아 실제 수정자 교체 |
| 7·8·9 | 6급 강도 4·피해 3.333, 7급 3.143, 1급 네더라이트 7.2 | `/rpg armorcap grade` 덮어쓰기 명령 |
| 10·11 | 장신구 +3 → 12, 최종 ×1.5 → 12·피해 7.2 | 실제 속성 명령으로 붙인 수정자 인식 |
| 12 | 상한 적용 시 7.2 (실패 시 2.0) | **피격 직전 갱신**(같은 틱) — 게임에서만 확인 가능 |
| 13·14·15 | — | 재접속·차원 이동·부활 후 보정 유지, 덮어쓰기 해제 |
| 16 | — | 몹 갑옷에도 바닐라 조정 적용, 상한은 플레이어만 |
| 17 | 맨몸 보정 없음 | 오류 없음 |
| 18 | — | 로그 경고 없음 |

결과는 번호별로 성공·실패·미확인으로 알려주세요. 실패하면 `/rpg armorcap info` 출력 전체를 함께 보내 주세요.

### 모드 혼합 (1차 구성 20개) — 게임 안 확인

| # | 할 일 | 기대 결과 | 자동 검증 |
|---|---|---|---|
| M1 | 클라이언트에 20개 모드를 넣고 실행 | 메인 화면까지 오류 없이 도달, 모드 목록에 fantasyrpg·magiccity·FTB 3종·Better Combat·Curios·Easy NPC·JEI·JourneyMap 표시 | 전용 서버 기동만 (CI) |
| M2 | 새 월드 생성 후 접속 | 월드 진입, 크래시 없음 | 아니오 |
| M3 | `/magiccity status`, `/rpg simulate 100 2.0 0 0.25 0` | 각각 출력. simulate 최종 150 | 서버 콘솔에서 통과 (CI) |
| M4 | 검으로 좀비를 Better Combat 콤보로 공격 | 콤보 동작·애니메이션 표시. 피해는 아직 바닐라 경로(RPG 연결 전) | 아니오 |
| M5 | 장신구 슬롯 열기(Curios 키) | 슬롯 화면 표시 | 아니오 |
| M6 | FTB Quests 책 열기, `/ftbteams party create 시험` | 화면 열림, 파티 생성 | 아니오 |
| M7 | Easy NPC 생성 후 대화 편집 화면 열기 | Config UI 표시 | 아니오 |
| M8 | JEI 아이템 목록, JourneyMap 지도(J), Jade 툴팁 | 각각 표시 | 아니오 |
| M9 | `/rpg armorcap info` 를 다이아 갑옷 착용 상태로 | v0.2 표와 같은 값 | 계산만 JUnit |

### royalqol (편의 모드, 클라이언트 전용) — 게임 안 확인

`magiccity`로 왕국 대륙(`/magiccity kingdom` 뒤 차원 `magiccity:kingdom`)에 들어가서 확인합니다. 전용 서버에서 서버 실행을 막지 않는 것까지는 CI로 확인됐습니다.

| # | 할 일 | 기대 결과 | 자동 검증 |
|---|---|---|---|
| Q1 | 월드 접속 | 왼쪽 위 HUD: 좌표·방위, "N일차 HH:MM", 지역, 가까운 곳 | 아니오 (방위·시각 계산은 JUnit) |
| Q2 | 제자리에서 한 바퀴 돌기 | 방위가 북→동→남→서 순서로 바뀜 (F3의 Facing과 일치) | 계산만 JUnit |
| Q3 | H 키 두 번, `/rq hud` | HUD 꺼짐·켜짐, 게임을 다시 켜도 상태 유지 | 아니오 |
| Q4 | 왕성도시 (260, 260) 근처로 이동 | 화면 아래 "― 왕성도시 ―", HUD 지역: 왕성도시 | 지역 판정만 JUnit |
| Q5 | `/rq near`, `/rq list` | 가까운 5곳 / 33곳과 거리·방위 | 아니오 |
| Q6 | `/rq go 북문` 후 이동 | 목적지 줄에 거리·화살표, 바라보는 방향에 맞게 화살표가 바뀜, 도착하면 채팅 알림 후 안내 종료 | 화살표 계산만 JUnit |
| Q7 | G 키 여러 번 | 가까운 5곳이 차례로 목적지가 됨 | 아니오 |
| Q8 | `/rq add 우리 집`, `/rq add 30 광장`, 게임 재시작, `/rq list` | ★ 표시로 남아 있음, `config/royalqol/waypoints.json`에 저장 | JSON 형식만 JUnit |
| Q9 | `/rq remove 왕성도시`, `/rq remove 우리 집` | 내장 지역은 거부, 내 웨이포인트는 지워짐 | 규칙만 JUnit |
| Q10 | 오버월드·네더로 이동 | 지역 "-", 내장 지역은 안 보임(차원별) | 규칙만 JUnit |
| Q11 | 조작 설정 | "왕국 편의 기능" 분류에 H·G 키, 바꿀 수 있음 | 아니오 |
| Q12 | 각 지역에 가서 이름이 맞는지 | 특히 서리 결투장(좌표 근사)과 지역 반경이 너무 크거나 작지 않은지 | 아니오 |

