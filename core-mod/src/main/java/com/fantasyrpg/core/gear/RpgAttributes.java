package com.fantasyrpg.core.gear;

import com.fantasyrpg.core.FantasyRpgCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * fantasyrpg 플레이어 능력치 (2026-11-21, claude/직업룬석_초안.md).
 * <ul>
 *   <li>{@code fantasyrpg:core_regen} — 코어 자원이 늘어나는 양에 곱하는 추가 비율 (0.05 = +5%)</li>
 *   <li>{@code fantasyrpg:skill_haste} — 스킬 가속. 재사용 대기 = 기본 × 100 ÷ (100 + 가속)</li>
 * </ul>
 */
public final class RpgAttributes {
    private RpgAttributes() {}

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, FantasyRpgCore.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> CORE_REGEN = ATTRIBUTES.register("core_regen",
            () -> new RangedAttribute("attribute.fantasyrpg.core_regen", 0.0, 0.0, 10.0).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> SKILL_HASTE = ATTRIBUTES.register("skill_haste",
            () -> new RangedAttribute("attribute.fantasyrpg.skill_haste", 0.0, 0.0, 1000.0).setSyncable(true));

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(RpgAttributes::onModify);
    }

    private static void onModify(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, CORE_REGEN);
        event.add(EntityType.PLAYER, SKILL_HASTE);
    }
}
