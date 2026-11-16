/**
 * 직업 코어 자원 (기획 v0.4 3장).
 *
 * <p>공통 인터페이스 1개와 6개 구현: 전사 기세(momentum), 기사 결의(resolve), 도적 표식(mark),
 * 궁수 집중(focus), 마법사 마나(mana), 전투사제 신성력(divinity).
 * 최대치·생성량·소비량은 {@code data/fantasyrpg/class_cores/*.json}에서 읽는다.
 * 물리·주문 기술은 같은 코어를 공유하며, 기본 공격과 최소 1개 행동은 자원 없이도 사용 가능해야 한다.
 */
package com.fantasyrpg.core.classcore;
