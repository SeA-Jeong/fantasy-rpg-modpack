/**
 * 장비 위력·성향 (기획 v0.4 6장).
 *
 * <p>무기는 직업별 종류 고정, 등급별 단일 위력 W를 Data Component로 가진다.
 * 성향(물리·주문·수호·기교·균형)은 W를 나누지 않고 부가 옵션으로만 구분한다.
 * 재조율 작업대에서 일반 화폐로 성향을 변경한다. 등급별 W와 옵션 예산은
 * {@code data/fantasyrpg/gear_tiers/*.json}에서 읽는다.
 */
package com.fantasyrpg.core.gear;
