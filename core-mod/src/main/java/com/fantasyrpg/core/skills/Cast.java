package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.combat.CombatManager;
import com.fantasyrpg.core.combat.CombatSettings;
import com.fantasyrpg.core.combat.DamageInput;
import com.fantasyrpg.core.combat.RpgDamage;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.fantasyrpg.core.skilltree.SkillNode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 스킬 한 번 사용. 피해는 {@link #hit}로 넣는다:
 * W(손에 든 직업 무기 등급, 사용할 때 고정) × 계수 → 비전 이해(마법 스킬 강화) → 저항 → 치명타(기본 + 연계 감각).
 *
 * @param bonus 이 사용에 붙은 최종 피해 보너스 (주문 연쇄 5랭크, 사용할 때 정해짐)
 */
public final class Cast {
    public final ServerPlayer player;
    public final SkillNode node;
    public final int rank;
    public final double bonus;
    private final double weaponPower;
    private boolean anyHit;

    public Cast(ServerPlayer player, SkillNode node, int rank, double bonus) {
        this.player = player;
        this.node = node;
        this.rank = rank;
        this.bonus = bonus;
        CombatSettings s = CombatManager.INSTANCE.settings();
        ItemStack held = player.getMainHandItem();
        this.weaponPower = s == null ? 0 : s.power(GearRegistry.tierOf(held));
    }

    public double v(String key) {
        return node.value(key, rank);
    }

    public boolean anyHit() {
        return anyHit;
    }

    /** 스킬 피해 한 번. 대상이 죽었거나 플레이어면 아무 일도 없음. 치명타였으면 true */
    public boolean hit(LivingEntity target, double physical, double magic) {
        if (!SkillTargets.single(target, player) || (physical <= 0 && magic <= 0)) return false;
        CombatSettings s = CombatManager.INSTANCE.settings();
        if (s == null) return false;
        double magicBonus = 0;
        SkillPassives.Learned insight = SkillPassives.learned(player, SkillIds.M_ARCANE_INSIGHT);
        if (insight != null) magicBonus = insight.v("skill_magic_bonus");
        DamageInput in = DamageInput.of(weaponPower, physical, magic).withBonus(0, magicBonus);
        if (bonus > 0) in = in.withFinalModifier(1.0 + bonus);
        boolean crit = player.getRandom().nextDouble() < s.critChance() + SkillPassives.critBonus(player);
        if (crit) in = in.withCrit(s.critMultiplier());
        RpgDamage.apply(target, player, in);
        player.setLastHurtMob(target);
        SkillRuntime.state(player).lastSkillHitTick = SkillRuntime.now(player);
        if (!anyHit) {
            anyHit = true;
            // 비전 이해 5랭크: 주문 방향 스킬이 처음 맞을 때 마나
            if (insight != null && insight.rank() >= 5 && node.directions().contains(SkillNode.Direction.SPELL)) {
                CoreService.gain(player, insight.v("spell_hit_core"));
            }
        }
        if (crit) SkillPassives.onCrit(player);
        return crit;
    }
}
