# fantasyrpg_content 데이터팩

테스트로 조정할 수치를 모두 여기에 둡니다. 게임 안에서 `/reload`로 반영됩니다.

| 폴더 | 내용 | 기획서 |
|---|---|---|
| `skill_nodes/<class>/` | 직업별 스킬 노드 (방향당 6개 + 혼합 2개) | 2장 |
| `class_cores/` | 코어 자원 최대치·생성·소비 | 3장 |
| `mob_resistances/` | 엔티티별 물리·마법 저항 | 5.1장 |
| `gear_tiers/` | 장별 무기 위력 W와 성향 옵션 예산 | 6장 |
| `regions/` | 장·지역·보스 아레나 좌표 | 5.5장 |
| `loot_tables/` | 몬스터 드롭, 토벌 증표 | 재료 수급 문서 |

## 파일 이름 규칙
- 직업 폴더: `warrior`, `knight`, `rogue`, `archer`, `mage`, `cleric`
- 노드 파일: `<방향>_<단계>.json` — 방향은 `attack`, `spell`, `guard`, `finesse`, 단계는 1~6
- 혼합 노드: `hybrid_<방향1>_<방향2>.json`

테스트 전용 데이터(바닐라 몹에 시험용 저항 등)는 옆의 `fantasyrpg_test` 데이터팩에 두고, 배포판에는 넣지 않습니다.

각 폴더의 `_example.json`은 형식 견본이며, 엔진은 `_`로 시작하는 파일을 무시합니다.
