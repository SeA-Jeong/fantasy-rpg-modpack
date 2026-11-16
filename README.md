# Fantasy RPG Modpack (Minecraft)

마인크래프트 판타지 RPG 모드팩입니다. 오버월드 12장(Lv.1~60)을 진행한 뒤 네더 2막으로 이어집니다.

- **플랫폼:** Minecraft Java 1.21.1 / NeoForge 21.1.x / Java 21
- **기준 기획서:** [`docs/design/직업_전투시스템_v0.4.md`](docs/design/직업_전투시스템_v0.4.md)
- **현재 진행 상황·인계:** [`docs/HANDOFF.md`](docs/HANDOFF.md)
- **게임에서 테스트하기:** [`docs/TESTING.md`](docs/TESTING.md)

## 저장소 구조

```
.
├─ docs/                    기획 문서 (기획이 코드보다 우선)
│  └─ design/
│     ├─ 직업_전투시스템_v0.4.md   ← 현재 기준안
│     ├─ 재료_수급_v0.1.md
│     └─ archive/                 이전 초안·검토 의견
├─ core-mod/                ① 엔진: 커스텀 코어 모드 (NeoForge, Java)
│  └─ src/main/java/com/fantasyrpg/core/
│     ├─ combat/      피해 계산 엔진        (기획 4장)
│     ├─ resist/      몬스터 저항 로딩      (5.1장)
│     ├─ playerdata/  레벨·SP·노드·프리셋   (2·7장)
│     ├─ skilltree/   노드 정의·해금 검증    (2장)
│     ├─ skills/      스킬 효과 타입 레지스트리
│     ├─ classcore/   직업 코어 자원 6종     (3장)
│     ├─ guard/       가드·패리 판정         (5.3장)
│     ├─ gear/        무기 W·성향·재조율     (6장)
│     ├─ arena/       보스 구역 지형 제한    (5.5장)
│     ├─ ui/          스킬트리 화면·HUD      (7장)
│     └─ command/     /rpg 관리·시뮬레이션 커맨드
├─ modpack/                 ②③ 콘텐츠·진행 (빌드 없이 수정 가능)
│  ├─ datapacks/fantasyrpg_content/   스킬 노드·코어·저항·장비·지역 데이터
│  ├─ kubejs/                          레시피·교환·드롭 스크립트
│  ├─ config/                          모드 설정, FTB Quests 퀘스트
│  └─ mods.md                          사용할 외부 모드 목록
└─ tools/                   좌표 변환·밸런스 시뮬레이터 등 보조 도구
```

## 3개 층의 역할

| 층 | 위치 | 담당 |
|---|---|---|
| ① 엔진 | `core-mod/` | 규칙(공식·검증·저장). 수치는 하드코딩하지 않음 |
| ② 콘텐츠 데이터 | `modpack/datapacks`, `modpack/kubejs` | 계수·저항·드롭·레시피 등 테스트로 바꿀 값 |
| ③ 진행·월드 | `modpack/config/ftbquests`, 맵 | 12장 퀘스트, 확정 보상, 지역 |

밸런스 수치는 `/reload`만으로 반영되도록 ②에 둡니다.

## 빌드

```bash
cd core-mod
./gradlew build        # 모드 jar + 테스트
./gradlew runClient    # 개발용 클라이언트 실행
```

push할 때마다 GitHub Actions가 `core-mod`를 빌드하고 테스트합니다.
