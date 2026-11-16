/**
 * 4방향 스킬트리 (기획 v0.4 2장).
 *
 * <p>{@code data/fantasyrpg/skill_nodes/<class>/*.json}에서 노드를 로딩한다.
 * 방향(attack, spell, guard, finesse)별 6노드, 최대 24SP.
 * 해금 검증: 레벨 조건 + 같은 방향 선행 투자 5/10/15/20SP(노드 자신과 혼합 노드 투자분 제외).
 * 혼합 노드는 두 방향 각 8SP 이상 투자 시 개방, 추가 1SP로 습득.
 * 포인트 환급은 전체 초기화만 허용하며, 프리셋 적용 시 모든 조건을 재검증한다(부록 A-2).
 */
package com.fantasyrpg.core.skilltree;
