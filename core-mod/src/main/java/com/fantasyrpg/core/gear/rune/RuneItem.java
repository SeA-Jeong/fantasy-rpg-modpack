package com.fantasyrpg.core.gear.rune;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 룬석 한 종류. 등급(1~4)은 {@code fantasyrpg:rune_grade} 컴포넌트에 있고, 없으면 하급.
 * 이름 뒤에 "(하급)" 같은 등급을 붙인다. 룬석 연마재도 같은 클래스(종류 없음)를 쓴다.
 */
public final class RuneItem extends Item {
    /** 연마재·추출 도구면 null */
    private final RuneType type;

    public RuneItem(RuneType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public RuneType type() {
        return type;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.fantasyrpg.graded", super.getName(stack),
                Component.translatable("rune.fantasyrpg.grade." + RuneRegistry.gradeOf(stack)));
    }
}
