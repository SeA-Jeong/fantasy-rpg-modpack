# 게임 안에서 테스트하는 방법

## 1. 준비 (처음 한 번)

1. Prism Launcher 또는 CurseForge 앱에서 새 인스턴스를 만듭니다.
   - Minecraft **1.21.1**, 로더 **NeoForge 21.1.x** (228 이상 권장)
2. 테스트 팩을 받습니다. 둘 중 하나를 고르면 됩니다.
   - **Releases** (권장): 저장소 오른쪽 Releases에서 `fantasyrpg-testpack-*.zip`
   - **최신 빌드**: Actions 탭 → 가장 최근 성공한 `core-mod build` → Artifacts의 `fantasyrpg-testpack`
3. zip을 풀면 아래 구조가 나옵니다.
   ```
   mods/fantasyrpg-<버전>.jar
   datapacks/fantasyrpg_content/
   datapacks/fantasyrpg_test/
   kubejs/ (아직 비어 있음)
   ```
4. `mods/` 안의 jar를 인스턴스의 `mods` 폴더에 넣습니다. 새 버전이 나오면 이전 jar는 지우세요.

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
