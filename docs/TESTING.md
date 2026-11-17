# 게임 안에서 테스트하는 방법

## 1. 준비 (처음 한 번)

1. Prism Launcher 또는 CurseForge 앱에서 새 인스턴스를 만듭니다.
   - Minecraft **1.21.1**, 로더 **NeoForge 21.1.266 이상** (v0.2부터. magiccity와 같은 버전)
2. 테스트 팩을 받습니다. 둘 중 하나를 고르면 됩니다.
   - **Releases** (권장): 저장소 오른쪽 Releases에서 `fantasyrpg-testpack-*.zip`
   - **최신 빌드**: Actions 탭 → 가장 최근 성공한 `core-mod build` → Artifacts의 `fantasyrpg-testpack`
3. zip을 풀면 아래 구조가 나옵니다.
   ```
   mods/fantasyrpg-<버전>.jar
   mods/magic-city-mod-<커밋>.jar   (v0.2부터 필수 의존 모드 magiccity)
   datapacks/fantasyrpg_content/
   datapacks/fantasyrpg_test/
   kubejs/ (아직 비어 있음)
   ```
4. `mods/` 안의 jar를 **모두** 인스턴스의 `mods` 폴더에 넣습니다. 새 버전이 나오면 이전 jar는 지우세요. magiccity가 없으면 게임이 시작되지 않습니다(필수 의존성).

## 2. 테스트 월드

1. 새 월드 생성 화면에서 **치트 허용: 켜기**, 게임 모드 **크리에이티브**.
2. **데이터 팩** 버튼 → 열린 폴더에 `datapacks/` 안의 두 폴더를 복사 → 둘 다 "선택됨"으로 옮깁니다.
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

결과는 번호별로 성공·실패·미확인으로 알려주세요. 실패하면 `/rpg armorcap info` 출력 전체를 함께 보내 주세요.

