package com.fantasyrpg.core.command;

import com.fantasyrpg.core.gear.armor.ArmorFamily;
import com.fantasyrpg.core.gear.armor.ArmorPart;
import com.fantasyrpg.core.gear.armor.ArmorStatManager;
import com.fantasyrpg.core.gear.armor.ArmorStatTable;
import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Locale;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 계열 방어구 시험 명령 (권한 2).
 * <pre>
 * /rpg armor give &lt;계열&gt; &lt;등급&gt;   — 그 계열·등급 4부위를 받는다 (knight, dwarf, dragon, elf, arcane)
 * /rpg armor tier &lt;등급&gt;          — 손에 든 계열 방어구의 등급을 바꾼다
 * /rpg armor info                 — 지금 적용되는 방어력·강도·넉백 저항과 입은 계열 방어구
 * </pre>
 */
public final class ArmorCommands {
    private ArmorCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return literal("armor")
                .then(literal("give")
                        .then(argument("family", word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(ArmorFamily.values()).map(ArmorFamily::id), b))
                                .then(argument("tier", integer(ArmorStatTable.MIN_TIER, ArmorStatTable.MAX_TIER))
                                        .executes(ArmorCommands::give))))
                .then(literal("tier")
                        .then(argument("tier", integer(ArmorStatTable.MIN_TIER, ArmorStatTable.MAX_TIER))
                                .executes(ArmorCommands::setTier)))
                .then(literal("info").executes(ArmorCommands::info));
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ArmorFamily family = ArmorFamily.byId(getString(ctx, "family"));
        if (family == null) {
            ctx.getSource().sendFailure(Component.literal("모르는 계열입니다. knight, dwarf, dragon, elf, arcane 중 하나"));
            return 0;
        }
        int tier = getInteger(ctx, "tier");
        for (ArmorPart part : ArmorPart.values()) {
            ItemStack s = GearRegistry.stack(family, part, tier);
            if (!player.getInventory().add(s)) player.drop(s, false);
        }
        ArmorStatTable t = ArmorStatManager.INSTANCE.get(family);
        String stats = t == null ? "수치표 없음" : t.tier(tier).armor() + "/" + t.tier(tier).toughness();
        send(ctx, "§6[방어구] §f" + family.id() + " " + tier + "급 4부위 지급 §7(세트 합계 " + stats + ")");
        return 1;
    }

    private static int setTier(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof FamilyArmorItem)) {
            ctx.getSource().sendFailure(Component.literal("손에 계열 방어구를 들어야 합니다"));
            return 0;
        }
        int tier = getInteger(ctx, "tier");
        held.set(GearRegistry.GEAR_TIER.get(), tier);
        send(ctx, "§6[방어구] §f손에 든 방어구를 " + tier + "급으로 바꿈");
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        send(ctx, String.format(Locale.ROOT, "§6[방어구] §f방어력 %.2f (피해 계산은 정수 %d) / 강도 %.2f / 넉백 저항 %.2f",
                player.getAttributeValue(Attributes.ARMOR), player.getArmorValue(),
                player.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
                player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.getItem() instanceof FamilyArmorItem item) {
                send(ctx, "§7  " + slot.getName() + ": " + item.family().id() + " " + GearRegistry.tierOf(s) + "급");
            } else if (!s.isEmpty()) {
                send(ctx, "§7  " + slot.getName() + ": " + s.getHoverName().getString());
            }
        }
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
