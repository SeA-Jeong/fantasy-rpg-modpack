# 외부 모드 목록 (NeoForge 1.21.1)

버전은 도입할 때 확정합니다. 여기에 없는 모드를 추가하기 전에 "피할 모드" 기준을 확인하세요.

## 사용
| 모드 | 용도 | 상태 |
|---|---|---|
| FTB Quests, FTB Library, FTB Teams, FTB XMod Compat | 12장 퀘스트북 | 예정 |
| KubeJS (+ Rhino) | 레시피·교환·드롭 스크립트 | 예정 |
| GeckoLib | 몬스터·보스 애니메이션, 공격 예고 동작 | 예정 |
| JEI 또는 EMI | 레시피 조회 | 예정 |
| Jade | 대상 정보 표시 (저항 표시 애드온은 코어 모드에서) | 예정 |
| 성능 최적화 모드 | 마지막 단계에서 추가 | 보류 |
| fantasybosses 0.8.0 (우리가 빌드: magic-city-mod `fantasybosses/`, `mods.lock.json` 고정 커밋) | 3장 보스 "세계수 잠식자 라디키온"(공격 6종·기관·알집·균열·BGM, GeckoLib, 보스 전장으로 옮겨 싸움). magiccity `boss_entity/parasite.json` 으로 연결 | 사용 (2026-11-20) |

## 피할 모드
- 피해·속성 체계를 바꾸는 전투·마법·레벨·스킬 모드 (피해 공식과 SP가 두 벌이 됨)
- 무기·방어구를 대량 추가하는 모드 (단일 W 예산 붕괴)
- 몹 추가 모드 (저항 데이터 없는 몬스터 발생). 넣는다면 `mob_resistances` 기본값 매핑 필수
