package com.fantasyrpg.core.gear.armor;

/**
 * 바닐라 1.21.1 방어 공식(CombatRules.getDamageAfterAbsorb, 파쇄 인챈트 없음)을 옮겨 적은 순수 계산.
 * 게임 판정은 바닐라가 하고, 이 클래스는 테스트·안내용이다.
 *
 * <p>실효방어 = clamp(방어력 − 피해 ÷ (2 + 강도/4), 방어력×0.2, 20), 받는 피해 = 피해 × (1 − 실효방어/25).
 * 방어력은 바닐라처럼 정수로 내려서 넣는다({@code LivingEntity.getArmorValue}).
 */
public final class ArmorDamageMath {
    private ArmorDamageMath() {
    }

    public static float afterArmor(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float f1 = Math.max(armor * 0.2F, Math.min(armor - damage / f, 20.0F));
        return damage * (1.0F - f1 / 25.0F);
    }

    /** 방어력 정수 내림까지 포함 */
    public static float taken(double damage, double armor, double toughness) {
        return afterArmor((float) damage, (float) Math.floor(armor), (float) toughness);
    }
}
