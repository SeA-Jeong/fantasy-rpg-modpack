/**
 * 피해 계산 엔진 (기획 v0.4 4장).
 *
 * <p>처리 순서: 무기 위력 W → 계수(Cp, Cm) → 스킬 강화(Sp, Sm) → 관통·저항 → 치명타 → 최종 보정 F.
 * <pre>
 * Dp = W × Cp × (1+Sp) × (1 − Reff,p)
 * Dm = W × Cm × (1+Sm) × (1 − Reff,m)
 * D  = (Dp + Dm) × Kcrit × F
 * Reff,t = max(0, Rt − Pt)
 * </pre>
 * 순수 계산 클래스는 Minecraft 의존 없이 작성하고 부록 A-1 케이스를 JUnit으로 검증한다.
 * 바닐라 방어구 계산은 RPG 피해에 적용하지 않으며, 기본 공격과 스킬 판정을 분리한다.
 */
package com.fantasyrpg.core.combat;
