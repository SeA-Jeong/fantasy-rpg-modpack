# 테스트 장비 모델링 반복 테스트 — 사용 설명서

Blockbench 에서 고치고 → 파일을 바꾸고 → 게임에서 입어 보고 → 다시 고치는 작업을 위한 개발자 전용 도구입니다.
일반 게임 기능(무기·갑옷·강화·직업·퀘스트·전투)은 건드리지 않습니다. 테스트 장비는 능력치 0, 제작법·드롭 없음.

## 0. 처음 테스트하는 순서 (약 30분, v0.28.3 기준)
결과는 `번호 / 통과·실패 / 본 것` 한 줄씩 + 이상하면 F2 스크린샷.

**준비**
1. Prism Launcher → 추가 → 가져오기 → `magic-kingdom-testpack-v0.28.3-test.1.mrpack` (새 인스턴스). 실행.
2. 메인 화면 **Mods** → `Fantasy RPG Core 0.28.3`, `GeckoLib` 이 있는지.
3. **옵션 → 리소스 팩** → `fantasyrpg_devtest` 를 오른쪽(사용 중)으로 → 완료.
4. 새 월드: 게임 모드 **크리에이티브**, **치트 허용 켬**. 들어가면 `/time set day`, 평평한 곳에 서기.

**1단계 — 등록**
5. `/fantasyrpgtest armor list` → **12개**(3벌 × 투구·흉갑·각반·장화), `목록: 모드 기본, 오류 0건`.
6. E → 창작 탭 **"판타지 RPG 개발 테스트 장비"** → 아이콘 12개, 이름 `[테스트] 기사 1급 투구 (v06)` 등.

**2단계 — 한 벌 입어 보기**
7. 철 갑옷 한 벌을 입고 `/fantasyrpgtest armor equip knight 1` → 4칸 모두 "기존 장비는 가방으로", 철 갑옷 4개가 가방에.
8. **F5** 두 번(앞에서 보기) → 앞·옆·뒤. F1 로 HUD 숨기기.
9. 걷기 · 달리기 · 점프 · 웅크리기 · 공격 반복. 볼 것:
   - 기사(templar): 십자 투구·빨간 망토(등)·허리 장갑·장화 테. 견갑이 **같은 쪽 팔**을 따라가는지, 망토가 다리를 뚫는 정도
   - `/fantasyrpgtest armor equip mage 5` (archmagefire): 후드·넓은 어깨 망토·긴 로브 자락. **로브 자락은 몸통 본이라 걸을 때 다리가 뚫고 나오는지**(원본 구조 그대로라 예상되는 문제), 어깨 망토와 팔이 겹치는 정도
   - `/fantasyrpgtest armor equip rogue 10` (thiefmob): 후드·대각 어깨끈·등 망토·작은 가죽 견갑(양쪽 대칭)
10. 부위 하나만: `/fantasyrpgtest armor equip rogue 10 helmet` → 투구 칸만 바뀌는지.

**3단계 — 게임 끄지 않고 바꾸기**
11. 인스턴스 **폴더** → `devtest/source_assets/test_armor/test_knight_chest_t01/current/test_knight_chest_t01.png` 를 복사해 눈에 띄게 칠함.
12. `resourcepacks/fantasyrpg_devtest/assets/fantasyrpg/textures/armor/test_armor/test_knight_chest_t01.png` 로 저장 → 기사 흉갑을 입고 `/fantasyrpgtest armor reload` → 입은 모습이 바뀌는지. 파일 지우고 다시 `reload` → 원래대로.
13. (Blockbench 있으면) `…/current/<id>.bbmodel` 을 **열 수 있는지**(오류·빠진 텍스처 없이) 먼저 확인 — 이번 판에서 아직 아무도 확인하지 않음. 큐브 하나 옮겨 **Export Bedrock Geometry** → `resourcepacks/…/geo/test_armor/<id>.geo.json` → `reload`.

**4단계 — 정리**
14. `/fantasyrpgtest armor clear` → 원래 모습으로. (선택) `config/fantasyrpg-devtest/ENABLE` 지우고 다시 시작 → 명령·창작 탭이 없음.

## 리비전 내력
| 판 | 내용 |
|---|---|
| v01~v05 | **폐기 (AI 제작·재조립)** — 흉갑 3종만 있었음. 저장소 `source_assets/test_armor/_retired_ai/` 에만 보관, 테스트 팩·jar 에 넣지 않음. v01 검증 모델 v1.1 / v02·v03 좌우 잘못 바꾼 판 / v04 v2.0 / v05 cleannrooster 큐브 재조립 + 다시 칠하기 + 새 큐브 |
| **v06** | **오픈소스 원본 구조 그대로** (2026-11-23) — 배분표 `source_assets/armor_library/04_240_slots.csv` 의 원본(기사 T01 = templar, 마법사 T05 = archmagefire, 도적 T10 = thiefmob; cleannrooster 커밋 a1ca6f85, MIT)을 4부위로 나눔. 큐브 위치·크기·회전·UV·텍스처는 원본 그대로, 바꾼 것은 `source_assets/test_armor/build_report_v06.json` (몹 리그 → 갑옷 본, 몹 맨살 머리 뺌, 마법사 팔 안쪽 큐브 inflate 0 → 0.4). 도구 `tools/devtest/build_from_library.py`, 비교 그림 `docs/devtest/compare_v05_v06.png` |

## 출처·크레딧
- 배포물(테스트 팩·mrpack 최상위)에 `LICENSES/` 와 `ASSET_PROVENANCE.csv`, jar 안 `licenses/ASSET_PROVENANCE.csv`. 게임 파일마다 원본 저장소·고정 커밋·원본 경로·원본 해시·바꾼 것.
- 만들기: `python3 tools/devtest/dist_licenses.py` (CI 는 `--check` 로 커밋된 것과 같은지 확인).
- 원본 사본: `source_assets/armor_library/upstream/` (GitHub 이 막힌 곳에서는 `tools/fetch_upstream.py --local <source>=<그 커밋을 가진 git 클론>`). 라이브러리 규칙: `source_assets/armor_library/LICENSE_POLICY_KO.md`. ImmersiveArmors(GPL)는 승인 전 받지 않음.

## 1. 켜기·끄기
- 통합 테스트 팩에는 `config/fantasyrpg-devtest/ENABLE` 파일이 들어 있어 **켜진 상태**입니다. 지우면 다음 실행부터 꺼집니다.
- 그 밖에: 시스템 속성 `-Dfantasyrpg.devtest=true|false` 가 가장 우선, Gradle `runClient` 개발 환경은 기본으로 켜짐.
- GeckoLib 이 없으면 켜지지 않습니다(팩에는 들어 있음).
- 켜지면 로그에 `[개발 테스트] 테스트 장비 12개 등록 (목록: 모드 기본), 오류 0건`.

## 2. 지금 들어 있는 테스트 장비 (v06)
| 한 벌 | 아이템 id (`fantasyrpg:`) | 원본 | 명령 |
|---|---|---|---|
| 기사 1급 | `test_knight_{helmet,chest,legs,boots}_t01` | cleannrooster `templar.geo.bbmodel` (templar.png) | `/fantasyrpgtest armor equip knight 1` |
| 마법사 5급 | `test_mage_{helmet,chest,legs,boots}_t05` | cleannrooster `archmagefire.bbmodel` | `/fantasyrpgtest armor equip mage 5` |
| 도적 10급 | `test_rogue_{helmet,chest,legs,boots}_t10` | cleannrooster `thiefmob.bbmodel` | `/fantasyrpgtest armor equip rogue 10` |

아이콘은 모델 앞모습 렌더(임시) — 마법사 T05 에 배정된 More Armor `redstone` 2D 아이콘은 판금 모양·밝은 빨강이라 로브와 맞지 않아 쓰지 않았습니다.

## 3. 명령어 (권한 2 = 치트 허용 싱글이면 됨)
| 명령 | 하는 일 |
|---|---|
| `/fantasyrpgtest armor list` | 등록된 테스트 장비·리비전·모델 경로, 목록 오류 |
| `/fantasyrpgtest armor give <직업> <등급> [부위]` | 지급 (부위 생략 = 그 직업·등급의 **모든 부위**) |
| `/fantasyrpgtest armor equip <직업> <등급> [부위]` | 그 칸의 기존 장비를 가방으로 옮기고 착용 (부위 생략 = 한 벌) |
| `/fantasyrpgtest armor clear` | 가방·갑옷 칸의 테스트 장비 모두 지움 |
| `/fantasyrpgtest armor reload` | 리소스 다시 읽기 (F3+T 와 같음) |

## 4. 빠른 반복 (게임을 끄지 않음)
1. 게임 **옵션 → 리소스 팩**에서 `fantasyrpg_devtest` 를 켭니다 (처음 한 번).
2. Blockbench 에서 `devtest/source_assets/test_armor/<id>/current/<id>.bbmodel` 을 열어 고칩니다.
3. 내보내기: **File → Export → Export Bedrock Geometry** (또는 GeckoLib 플러그인 내보내기) 로 아래 경로에 저장
   `resourcepacks/fantasyrpg_devtest/assets/fantasyrpg/geo/test_armor/<id>.geo.json`
   텍스처를 고쳤으면 `…/textures/armor/test_armor/<id>.png`, 아이콘은 `…/textures/item/test_armor/<id>.png`.
4. 게임에서 **F3+T** (또는 `/fantasyrpgtest armor reload`).
5. F5 로 3인칭 → 걷기·달리기·점프·웅크리기·공격으로 확인.

파이썬이 있으면 3번을 스크립트로: `python devtest/test_armor.py deploy instance .` (인스턴스 폴더에서. `current/` 의 파일을 리소스팩과 설정 폴더에 한 번에 넣음)

### 무엇이 바로 바뀌고 무엇이 재시작이 필요한가
| 바꾼 것 | 반영 |
|---|---|
| 입체 모델 `.geo.json`, 입은 모습 텍스처, 인벤토리 아이콘, 아이템 모델 JSON, 애니메이션 `.animation.json` | **F3+T** (게임 계속) |
| 목록(`armor_manifest.json`)에 항목 추가·id·부위 변경 | **게임 다시 시작** (아이템 등록은 시작 때만). Gradle 빌드는 필요 없음 — `config/fantasyrpg-devtest/armor_manifest.json` 에 두면 모드 안 목록 대신 씀 |
| 목록의 경로만 변경 | 게임 다시 시작 |
| Java 코드 (렌더러 동작·명령어) | Gradle 빌드 + 새 jar + 다시 시작 |
| 모드 안 기본 리소스(jar) 갱신 | `test_armor.py deploy repo` → Gradle 빌드 |

## 5. 리비전 (이전 디자인으로 되돌리기)
아이템 id 는 그대로, 파일만 바꿉니다.
```
python devtest/test_armor.py list                                  # 항목·활성 판·보관한 판
python devtest/test_armor.py snapshot test_mage_chest_t05 --note "로브 하단 줄임"   # current → 새 판 vNN
python devtest/test_armor.py use test_mage_chest_t05 v02           # 보관한 판 → current
python devtest/test_armor.py deploy instance .                     # current → 리소스팩·설정 (그다음 F3+T)
python devtest/test_armor.py validate                              # 본 이름·좌우·UV·텍스처 크기 검사
python devtest/test_armor.py import test_mage_chest_t05 --bbmodel a.bbmodel --note "무엇을 바꿨나"
                                                                   # bbmodel 하나로 새 판: geo(Blockbench 규칙)·텍스처(bbmodel 안)·아이콘(3D 앞모습) 자동
                                                                   # (--texture·--icon 을 주면 그 파일을 씀. 아이콘 자동은 Pillow 필요)
python devtest/preview.py <모델.bbmodel> 비교.png --skin           # 앞·옆·뒤·실루엣 미리보기 (Pillow)
```
폴더: `source_assets/test_armor/<id>/current/` (작업본) · `revisions/v01, v02 …` (보관, 지우지 않음).

## 6. 새 직업·등급·부위 추가 (240종까지)
1. `source_assets/test_armor/<새 id>/current/` 에 `<id>.bbmodel`·`<id>.geo.json`·`<id>.png`·`<id>_icon.png`.
2. `manifest.json` 의 `entries` 에 한 줄: `{"id":"test_warrior_helmet_t03","class":"warrior","grade":3,"slot":"helmet","revision":"v01"}` — id 는 `test_` 로 시작.
3. `validate` → `deploy instance` → 게임 다시 시작 → `/fantasyrpgtest armor equip warrior 3 helmet`.
Java·렌더러·명령어는 고치지 않습니다. 경로 규칙을 바꾸려면 `manifest.json` 의 `defaults` 한 곳만.

## 7. 모델 규칙 (GeckoLib 갑옷)
- 최상위 본: `armorHead` `armorBody` `armorRightArm` `armorLeftArm` `armorRightLeg` `armorLeftLeg` `armorRightBoot` `armorLeftBoot`. 각 본은 플레이어의 같은 부위를 따라 움직입니다. 장식은 이 본들의 자식으로.
- **Blockbench(Bedrock) 좌표에서 x<0 이 캐릭터 오른쪽**입니다 (`armorRightArm` 회전축 [-5, 22, 0], `armorLeftArm` [5, 22, 0], 다리 ±2).
- 흉갑은 몸통·팔에 더해 **머리 본·다리 본도 그립니다** (후드·마스크, 로브 하단·코트 자락용). 투구·각반·장화는 GeckoLib 기본대로 자기 부위만.
- **좌우 (2026-11-23 바로잡음):** Blockbench 는 Bedrock 으로 내보낼 때 x 를 뒤집습니다. 그래서 **Blockbench(bbmodel)에서는 `armorRightArm` 이 x>0(회전축 [5, 22, 0])**, 내보낸 geo.json 에서는 x<0 이 됩니다 — cleannrooster 원본·GeckoLib 템플릿과 같은 규칙. 지금까지 받은 v1.1·v2.0 은 bbmodel 은 맞았고 **geo.json 을 x 를 뒤집지 않고 만든 것**이 문제였습니다. 그래서 geo 는 손으로 만들지 말고 **Blockbench 에서 내보내거나 `import --bbmodel` 로 넣으면 `tools/devtest/bb2geo.py` 가 Blockbench 와 같은 규칙으로 만듭니다**. `validate` 가 bbmodel 과 geo 가 다르면 알려 줍니다.
- 예전 v02·v03 은 본 이름만 바꿔 고쳤기 때문에 **비대칭 부품이 반대쪽**에 있습니다(비교용 보관). 올바른 기준은 v04.
- 텍스처 투명도는 0 또는 255 만 (갑옷은 잘라내기 렌더라 반투명이 안 보임).
- geo 형식 `1.12.0`, 텍스처 크기는 geo 의 `texture_width/height` 와 같게.
