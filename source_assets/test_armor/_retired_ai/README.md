# 폐기한 AI 제작 테스트 장비 (보관 전용, 배포 금지)

2026-11-23 오픈소스 장비 라이브러리 지침(`source_assets/armor_library/CLAUDE_NEXT_KO.md` 6항)에 따라,
v01~v05 판(AI 가 만들었거나 원본을 재조립·다시 칠하고 새 큐브를 더한 것)을 게임·배포물에서 뺐다.

- `<id>/v01~v05/` : 기사 T01·마법사 T05·도적 T10 흉갑의 옛 판 (되돌려 보기용). `test_armor.py use` 대상이 아님.
- `build_from_upstream_v05.py` : v05 를 만든 스크립트 (재조립·다시 칠하기·새 큐브)
- `_upstream_v05/` : v05 가 쓴 cleannrooster 원본 사본 (MIT, 같은 커밋 a1ca6f85). 지금은 `source_assets/armor_library/upstream/cleann` 을 쓴다.

이 폴더는 테스트 팩 묶음(make_kit.sh)·jar·mrpack 에 들어가지 않는다 (CI packcheck 가 확인).
되살리려면 폴더를 `<id>/revisions/` 로 옮긴 뒤 `test_armor.py use <id> vNN`.
