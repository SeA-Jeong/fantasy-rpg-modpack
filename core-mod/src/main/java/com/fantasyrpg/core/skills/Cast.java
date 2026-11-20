package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.combat.CombatManager;
import com.fantasyrpg.core.combat.CombatSettings;
import com.fantasyrpg.core.combat.DamageInput;
import com.fantasyrpg.core.combat.RpgDamage;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.fantasyrpg.core.skilltree.SkillNode;
import com.fantasyrpg.core.skilltree.SkillValues;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 스킬 한 번 사용. 수치는 {@link #v}(노드 값 + 다른 패시브의 modifies), 피해는 {@link #hit}:
 * W(손에 든 직업 무기 등급, 사용할 때 고정) × 계수 → 스킬 강화(4단 패시브 합) → 저항 → 치명타 → 최종 보정.
 *
 * @param bonus 이 사용에 붙은 최종 피해 보너스 (연쇄, 사용할 때 정해짐)
 */
public final class Cast {
    public final ServerPlayer player;
    public final SkillNode node;
    public final int rank;
    public final double bonus;
    private final double weaponPower;
    private boolean anyHit;
    /** 효과가 true로 하면 코어 비용을 받지 않음 (패리 직후 등) */
    public boolean free;
    /** 효과가 정하는 쿨타임 배율 (예: 3마리 이상 맞히면 0.5) */
    public double cooldownMult = 1.0;
    /** {@link #empower()}가 강화했는지 */
    public boolean empowered;
    private int hitCount;

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
        return node.value(key, rank) + SkillPassives.modSum(player, node.effect(), key);
    }

    public int cooldownTicks() {
        return (int) Math.max(0, Math.round(v(SkillValues.COOLDOWN)));
    }

    public boolean anyHit() {
        return anyHit;
    }

    public int hitCount() {
        return hitCount;
    }

    /**
     * 5랭크 "코어가 문턱 이상이면 더 쓰고 강화" (empower_threshold·empower_cost·empower_mult).
     * 기본 비용은 아직 빠지지 않은 상태라 비용 + 추가분을 함께 확인. 배율(아니면 1)
     */
    public double empower() {
        double threshold = v("empower_threshold");
        if (threshold <= 0) return 1.0;
        if (CoreService.value(player) < Math.max(threshold, node.cost(rank) + v("empower_cost"))) return 1.0;
        CoreService.spend(player, v("empower_cost"));
        empowered = true;
        return v("empower_mult");
    }

    /** 역습의 리듬: 피해를 받은 뒤 잠시 스킬 피해 + */
    private double afterHurt() {
        double b = SkillPassives.sum(player, "after_hurt_bonus");
        return b > 0 && SkillRuntime.now(player) - SkillRuntime.state(player).lastHurtTick <= 60 ? b : 0;
    }

    public boolean hit(LivingEntity target, double physical, double magic) {
        return hit(target, physical, magic, 1.0, false);
    }

    /** 스킬 피해 한 번. 대상이 죽었거나 플레이어면 아무 일도 없음. mult = 최종 배율, forceCrit = 치명타 확정. 치명타였으면 true */
    public boolean hit(LivingEntity target, double physical, double magic, double mult, boolean forceCrit) {
        if (!SkillTargets.single(target, player) || (physical <= 0 && magic <= 0) || mult <= 0) return false;
        CombatSettings s = CombatManager.INSTANCE.settings();
        if (s == null) return false;
        double magicBonus = SkillPassives.sum(player, "skill_magic_bonus")
                + com.fantasyrpg.core.gear.armor.SetEffects.skillMagicBonus(player);                // 마도 4세트 (2026-11-21)
        double physicalBonus = SkillPassives.sum(player, "skill_physical_bonus");
        DamageInput in = DamageInput.of(weaponPower, physical, magic).withBonus(physicalBonus, magicBonus);
        double fin = (1.0 + bonus) * mult * (1.0 + SkillPassives.sum(player, "all_damage_bonus") + SkillPassives.afterParryBonus(player)
                + SkillPassives.buffDamage(player) + afterHurt());
        // 도적 그림자 칼날 5랭크 "암영": 내 스킬 피해 +
        Long shadow = SkillRuntime.state(player).shadowMarks.get(target.getUUID());
        if (shadow != null && shadow > SkillRuntime.now(player)) {
            SkillPassives.Learned blades = SkillPassives.learned(player, SkillIds.R_SHADOW_BLADES, 5);
            if (blades != null) fin *= 1.0 + blades.v("shadow_bonus");
        }
        if (fin != 1.0) in = in.withFinalModifier(fin);
        boolean crit = forceCrit || s.critRoll(player.getRandom().nextDouble(), SkillPassives.critBonus(player));   // 합계 상한 (2026-11-21)
        if (crit) in = in.withCrit(s.critMultiplier() + SkillPassives.critMultBonus(player));
        RpgDamage.apply(target, player, in);
        player.setLastHurtMob(target);
        SkillRuntime.state(player).lastSkillHitTick = SkillRuntime.now(player);
        hitCount++;
        if (!anyHit) {
            anyHit = true;
            // 주문 방향 스킬이 처음 맞을 때 코어 (비전 이해 5랭크, 마력 순환, 마력 각성, 원소 숙련)
            if (node.directions().contains(SkillNode.Direction.SPELL)) CoreService.gain(player, SkillPassives.sum(player, "spell_hit_core"));
        }
        if (crit) SkillPassives.onCrit(player);
        return crit;
    }
}
