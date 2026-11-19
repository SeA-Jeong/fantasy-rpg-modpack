package com.fantasyrpg.core.combat;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.resist.MobResistanceManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 계산된 RPG 피해를 실제 엔티티에 적용한다.
 *
 * <p>피해 유형 {@code fantasyrpg:rpg}는 바닐라 방어구·보호 마법부여·무적 시간 계산을 건너뛰도록
 * 태그를 붙여 두었다(4장: 커스텀 계산을 기준으로 삼음). 저항은 {@link MobResistanceManager} 데이터를 쓴다.
 */
public final class RpgDamage {
    public static final ResourceKey<DamageType> RPG = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "rpg"));

    private RpgDamage() {
    }

    public static Resistances resistancesOf(Entity target) {
        return MobResistanceManager.INSTANCE.get(target.getType());
    }

    /** 대상 저항을 적용해 계산하고 피해를 입힌다. 계산 결과를 돌려준다. */
    public static DamageBreakdown apply(LivingEntity target, @Nullable Entity attacker, DamageInput input) {
        if (attacker instanceof net.minecraft.server.level.ServerPlayer sp) {
            // 유산 무기 각성 특성: 물리·마법 피해 % (스킬 강화와 같은 단계에 더함)
            double pb = com.fantasyrpg.core.gear.heirloom.HeirloomService.bonus(sp, "physical_bonus");
            double mb = com.fantasyrpg.core.gear.heirloom.HeirloomService.bonus(sp, "magic_bonus");
            if (pb != 0 || mb != 0) input = input.withBonus(input.physicalBonus() + pb, input.magicBonus() + mb);
        }
        DamageBreakdown result = DamageCalculator.calculate(input, resistancesOf(target));
        if (result.total() > 0) {
            DamageSource source = new DamageSource(
                    target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RPG),
                    attacker);
            target.hurt(source, (float) result.total());
        }
        return result;
    }
}
