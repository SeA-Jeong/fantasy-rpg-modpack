# v02 — 좌우 본 이름 고침 (Claude, 2026-11-23)

v01 과 큐브·UV·그림은 같음. `armorLeftArm`↔`armorRightArm`, `armorLeftLeg`↔`armorRightLeg` 이름만 바꿈.
GeckoLib 갑옷 규칙: Bedrock 좌표에서 x<0 이 캐릭터 오른쪽(`armorRightArm` 회전축 [-5,22,0]). v01 은 반대라서 오른쪽에 있는 부품이 왼팔을 따라 움직였음.
결과: Blockbench 에서 보이는 모습 그대로 게임에 나옴. 디자인 설명의 "왼쪽" 큰 견갑은 Blockbench 화면 기준 위치(= 캐릭터 오른쪽 어깨)에 그대로 있음 — 왼쪽 어깨로 옮기려면 Blockbench 에서 좌우 반전.


> 2026-11-23 주의: 이 판은 좌우 본 이름을 바꾸는 방식으로 고쳐서 **비대칭 부품이 반대쪽에** 있고 bbmodel 그룹 이름도 바뀌어 있음. 올바른 판은 v04(같은 모양을 Blockbench 규칙으로 내보냄). 비교용으로만 보관.
