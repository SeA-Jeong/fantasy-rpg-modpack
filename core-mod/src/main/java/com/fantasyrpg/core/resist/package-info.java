/**
 * 몬스터 저항 (기획 v0.4 5.1장).
 *
 * <p>{@code data/fantasyrpg/mob_resistances/*.json}에서 엔티티별 물리·마법 저항(Rp, Rm)을 읽는다.
 * 데이터가 없는 엔티티에는 기본 저항을 적용한다.
 * 필수 챕터 보스는 저항 차이 기본 15%p, 예외 20%p 이내인지 로딩 시 경고한다.
 */
package com.fantasyrpg.core.resist;
