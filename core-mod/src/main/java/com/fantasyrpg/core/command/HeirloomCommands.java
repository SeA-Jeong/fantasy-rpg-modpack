package com.fantasyrpg.core.command;

import com.fantasyrpg.core.gear.heirloom.HeirloomRules;
import com.fantasyrpg.core.gear.heirloom.HeirloomService;
import com.fantasyrpg.core.playerdata.HeirloomData;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 유산 무기 시험 명령 (권한 2).
 * /rpg heirloom info | give | awaken | tier &lt;1~10&gt; | enhance | pick &lt;방향&gt; | reset
 */
public final class HeirloomCommands {
    private HeirloomCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return literal("heirloom")
                .then(literal("info").executes(HeirloomCommands::info))
                .then(literal("give").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (HeirloomService.grant(p).isEmpty()) {
                        c.getSource().sendFailure(Component.literal("직업이 없습니다"));
                        return 0;
                    }
                    return info(c);
                }))
                .then(literal("awaken").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    RpgProgress.set(p, RpgProgress.get(p).withHeirloom(RpgProgress.get(p).heirloom().awaken()));
                    HeirloomService.refreshStacks(p);
                    return info(c);
                }))
                .then(literal("tier").then(argument("tier", integer(1, 10)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    RpgProgress.set(p, RpgProgress.get(p).withHeirloom(RpgProgress.get(p).heirloom().withTier(getInteger(c, "tier"))));
                    HeirloomService.refreshStacks(p);
                    return info(c);
                })))
                .then(literal("enhance").executes(c -> HeirloomService.enhance(c.getSource().getPlayerOrException()) ? info(c) : 0))
                .then(literal("pick").then(argument("direction", word()).executes(c ->
                        HeirloomService.pickTrait(c.getSource().getPlayerOrException(), getString(c, "direction")) ? info(c) : 0)))
                .then(literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    RpgProgress.set(p, RpgProgress.get(p).withHeirloom(HeirloomData.NONE));
                    return info(c);
                }));
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        HeirloomData h = RpgProgress.get(p).heirloom();
        int g = HeirloomService.grade(p);
        c.getSource().sendSuccess(() -> Component.literal("§6[유산] §f" + (h.owned() ? "" : "(없음) ")
                + h.tier() + "급 / 한계 " + HeirloomRules.cap(g, h.awakened()) + "급, " + (h.awakened() ? "깨어남" : "잠듦")
                + ", 각성 단계 " + HeirloomRules.stage(g, h.awakened()) + ", 특성 " + h.traits()
                + " (남은 고르기 " + HeirloomRules.picksLeft(g, h.awakened(), h.traits()) + "), 일련번호 " + h.serial()), false);
        return 1;
    }
}
