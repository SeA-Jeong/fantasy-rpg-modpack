# 테스트 장비 모델링 반복 테스트 — 사용 설명서

Blockbench 에서 고치고 → 파일을 바꾸고 → 게임에서 입어 보고 → 다시 고치는 작업을 위한 개발자 전용 도구입니다.
일반 게임 기능(무기·갑옷·강화·직업·퀘스트·전투)은 건드리지 않습니다. 테스트 장비는 능력치 0, 제작법·드롭 없음.

## 1. 켜기·끄기
- 통합 테스트 팩에는 `config/fantasyrpg-devtest/ENABLE` 파일이 들어 있어 **켜진 상태**입니다. 지우면 다음 실행부터 꺼집니다.
- 그 밖에: 시스템 속성 `-Dfantasyrpg.devtest=true|false` 가 가장 우선, Gradle `runClient` 개발 환경은 기본으로 켜짐.
- GeckoLib 이 없으면 켜지지 않습니다(팩에는 들어 있음).
- 켜지면 로그에 `[개발 테스트] 테스트 장비 3개 등록 (목록: 모드 기본), 오류 0건`.

## 2. 지금 들어 있는 테스트 장비
| 아이템 id | 명령 | 검증 목적 |
|---|---|---|
| `fantasyrpg:test_knight_chest_t01` | `/fantasyrpgtest armor equip knight 1` | 다층 판금·견갑·허리 장갑 (중장갑 구조) |
| `fantasyrpg:test_mage_chest_t05` | `/fantasyrpgtest armor equip mage 5` | 넓은 소매·분할 로브 (로브 하단이 다리 본에 붙음) |
| `fantasyrpg:test_rogue_chest_t10` | `/fantasyrpgtest armor equip rogue 10` | 비대칭 견갑·코트 자락 (코트 꼬리 피벗) |

창작 탭 **"판타지 RPG 개발 테스트 장비"** 에도 있습니다. 이름에 활성 리비전이 붙습니다(예: `[테스트] 마법사 5급 흉갑 (v02)`).

## 3. 명령어 (권한 2 = 치트 허용 싱글이면 됨)
| 명령 | 하는 일 |
|---|---|
| `/fantasyrpgtest armor list` | 등록된 테스트 장비·리비전·모델 경로, 목록 오류 |
| `/fantasyrpgtest armor give <직업> <등급> [부위]` | 지급 (부위 생략 = 그 직업·등급 첫 항목) |
| `/fantasyrpgtest armor equip <직업> <등급> [부위]` | 그 칸의 기존 장비를 가방으로 옮기고 착용 |
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
- 흉갑은 몸통·팔에 더해 **다리 본도 그립니다** (로브 하단·코트 자락용). 투구·각반·장화는 GeckoLib 기본대로 자기 부위만.
- 텍스처 투명도는 0 또는 255 만 (갑옷은 잘라내기 렌더라 반투명이 안 보임).
- geo 형식 `1.12.0`, 텍스처 크기는 geo 의 `texture_width/height` 와 같게.
