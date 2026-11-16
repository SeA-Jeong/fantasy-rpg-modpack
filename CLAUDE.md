# 작업 규칙 (Claude 및 협업자용)

이 저장소는 Claude가 직접 편집하고 커밋합니다. 작업 전에 아래 규칙을 따르세요.

## 기준 문서
- 직업·전투 규칙의 기준은 `docs/design/직업_전투시스템_v0.4.md`입니다. 코드와 기획이 다르면 기획을 따르고, 차이는 보고합니다.
- 규칙을 바꾸는 결정이 나오면 **기획 문서를 먼저 수정**한 뒤 코드와 데이터를 바꿉니다.
- `docs/design/archive/`는 이전 초안입니다. 참고만 하고 수정하지 않습니다.

## 어디에 무엇을 두는가
- **규칙(공식, 검증, 저장)**: `core-mod/` Java 코드.
- **테스트로 바뀔 수치**(스킬 계수, 저항, 코어 최대치, 패리 틱, 장비 W 예산, 드롭 수량): `modpack/datapacks/fantasyrpg_content/` JSON 또는 config. Java에 하드코딩하지 않습니다.
- 레시피, 교환, 드롭 조정: `modpack/kubejs/server_scripts/`.
- 퀘스트: `modpack/config/ftbquests/quests/`.

## 코드 규칙
- 패키지 루트: `com.fantasyrpg.core`, mod id: `fantasyrpg`.
- 피해 계산 등 순수 로직은 Minecraft 클래스에 의존하지 않는 클래스로 분리하고 JUnit 테스트를 붙입니다.
- 기획서 부록 A의 계산·해금 케이스는 반드시 자동 테스트로 유지합니다.
- 모든 계산은 서버 측에서 수행하고 클라이언트에는 결과만 동기화합니다.

## 언어
- 문서, 커밋 메시지, 주석 설명은 한국어. 식별자(클래스, 필드, JSON 키)는 영어.

## 커밋
- 한 커밋은 하나의 의미 있는 변경. 메시지 형식: `[영역] 요약` (예: `[combat] 관통·저항 계산 구현`).
- 영역: docs, combat, skilltree, classcore, gear, data, quests, pack, tools, ci.
