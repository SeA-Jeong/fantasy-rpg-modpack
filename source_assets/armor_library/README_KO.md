# FantasyRPG — 오픈소스 장비 라이브러리 1차 구축

**중요: 이 ZIP에는 다른 제작자의 실제 PNG/BBMODEL 파일이 포함되어 있지 않습니다.**
현 환경에서 GitHub 원본을 컨테이너 파일로 다운로드하지 못했으므로, 소스 고정 커밋·경로·재사용 허가·배정표와 실행 가능한 다운로드/스테이징 프로그램을 제공했습니다.
Claude의 인터넷 연결된 개발 환경에서 `tools/fetch_upstream.py`를 실행해 원본을 얻으세요.

## 이 조사에서 확인한 수량
- More Armor: 공개된 36세트 / 144개 아이템 아이콘 / 바닐라식 착용 레이어, **기본 3D Blockbench 의복 모델 0개** (저장소 내 .bbmodel, .geo.json 검색 결과)
- cleannrooster: 26개 .bbmodel 원본 (전부 착용용 아님). 그중 15개 인간형/장비 참고 후보를 수작업으로 추천.
- Loy's Goodies: 119개 .bbmodel 중 착용 액세서리 후보 7개를 추림 (전체 방어구 세트 아님).
- FrenchKrab: 57개 .bbmodel, 무기/도구 중심 (방어구 240종 수량에는 0개 반영).
- ImmersiveArmors: GPL-3.0, 자동 도입 제외. 원저작/제3자 리소스 권리 확인 필요.

## 현재 상태
- **240개 슬롯의 실제 모델링/게임 연동: 0개** (이번 신규 오픈소스 전환 기준).
- 60개 세트 가운데 2D 후보가 임시로 배정된 세트: **27개** → 108개의 슬롯에 아이콘 원본 경로를 후보로 기록.
- 2D 후보가 아직 없는 세트: **33개** → 132개 슬롯.
- 3D 구조 참고/개조 모델 후보: 15개 (중복 대상이 있을 수 있음). 각 후보를 곧바로 4부위 완성 장비로 세지 않음.
- 이 배정은 미술적 적합도 심사가 끝난 확정안이 아닌 **검수용 매핑**입니다.

## 실행 방법
Windows PowerShell 또는 터미널에서 Python 3.10+ 실행:
```sh
python tools/verify_catalog.py
python tools/fetch_upstream.py --source primary
python tools/build_staging.py
```
- `fetch_upstream`는 원본 고정 커밋의 트리 목록을 가져와, 2D 후보 폴더 전체 및 15개 Blockbench 모델/7개 CC0 액세서리를 다운로드합니다.
- 원본은 `upstream/`으로 보존하고, `build_staging.py`는 오직 `staging/` 아래에 **테스트용 복사본**만 배치합니다.
- 기존 FantasyRPG 모드/아이템 등록 파일에는 어떠한 자동 변경도 가하지 않습니다.
- 완료 후 Claude가 모델 슬롯 분리 및 게임 렌더러에 맞는 변환을 수행해야 합니다.

## 파일
- `01_sources.csv`: 권리·고정 커밋·용도
- `02_morearmor_36_sets.csv`: 36세트 원본 경로 및 대응 후보
- `03_cleann_15_models.csv`: 15개 모델 재가공 우선 후보
- `04_240_slots.csv`: 240개 등록 슬롯별 상태 (직접 편집 가능한 작업 표)
- `05_60_sets.csv`: 6×10 세트별 후보/누락 상태
- `06_accessories.csv`: CC0 액세서리
- `source_registry.json`, `assignment.json`, `model_candidates.json`: 다운로드·배분 스크립트용 데이터
- `CLAUDE_NEXT_KO.md`: Claude 실제 작업 지시

## 절대 하면 안 되는 일
- MIT/GPL 등 라이선스 의무 누락, 출처가 다른 파일의 권리를 일괄 해석하는 행위.
- 동일한 2D 재질을 10번 복제하여 10종의 고유한 3D 갑옷으로 집계.
- 라이선스·리그·인게임 검증 없이 '완료'로 표시.
- 원본 리포지터리의 원본을 변형하여 무단으로 출처를 숨기는 것.
