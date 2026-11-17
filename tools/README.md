# 보조 도구

| 도구 | 내용 | 상태 |
|---|---|---|
| `regions_from_csv` | 지역별 중심좌표·도로 경유좌표 CSV → `regions/*.json` 변환 | 예정 |
| `balance_sim` | 빌드·장비·보스 저항을 넣어 스킬 피해와 보스 처치 시간 비교 | 예정 |
| `validate_data` | 노드 JSON 규격 검사 (방향당 24SP, 선행 SP, 보스 저항 차이 상한) | 예정 |
| `check_mods.py` | mods 폴더의 jar 들이 필수 의존성·버전 범위(선택 의존성 범위 포함)를 만족하는지 검사. `python3 tools/check_mods.py <mods 폴더> --neoforge 21.1.266`. jar 만 읽으므로 실제 실행(mixin 충돌 등)은 게임에서 확인 | 사용 가능 (자체 검사 4개, CI) |
