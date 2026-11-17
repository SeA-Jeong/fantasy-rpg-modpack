# 보조 도구

| 도구 | 내용 | 상태 |
|---|---|---|
| `regions_from_csv` | 지역별 중심좌표·도로 경유좌표 CSV → `regions/*.json` 변환 | 예정 |
| `balance_sim` | 빌드·장비·보스 저항을 넣어 스킬 피해와 보스 처치 시간 비교 | 예정 |
| `validate_data` | 노드 JSON 규격 검사 (방향당 24SP, 선행 SP, 보스 저항 차이 상한) | 예정 |
| `check_mods.py` | mods 폴더의 jar 들이 필수 의존성·버전 범위(선택 의존성 범위 포함)를 만족하는지 검사. `python3 tools/check_mods.py <mods 폴더> --neoforge 21.1.266`. jar 만 읽으므로 실제 실행(mixin 충돌 등)은 게임에서 확인 | 사용 가능 (자체 검사 4개, CI) |
| `fetch_mods.py` | `modpack/mods.lock.json`의 외부 모드 jar 받기(Modrinth sha512 검사, FTB 메이븐, 대체 주소). `python3 tools/fetch_mods.py modpack/mods.lock.json <폴더> [--max-stage N]` | 사용 가능 (CI) |
| `smoke_server.py` | 설치된 NeoForge 전용 서버에 mods 를 넣고 기동 → 콘솔 명령 확인(`--command "명령=>기대 문자열"`) → stop, 치명 로그 검사. 클라이언트는 보지 않음 | 사용 가능 (CI `modpack smoke`) |
| `build_mrpack.py` | 통합 테스트 팩 `.mrpack` 만들기: 외부 모드 18개는 주소·해시만, 우리 모드 3개와 테스트 데이터팩은 파일로. Prism Launcher 가져오기용 | 사용 가능 (CI, 자체 검사 `test_mrpack.py`) |
| `install_mrpack.py` | `.mrpack`을 launcher처럼 설치(주소에서 받기·해시 확인·overrides 풀기). CI에서 팩 검증용 | 사용 가능 (CI) |
| `build_royalqol.sh` | `mods.lock.json`의 고정 커밋으로 `magic-city-mod` 저장소 `royalqol/` 빌드 | 사용 가능 (CI) |
