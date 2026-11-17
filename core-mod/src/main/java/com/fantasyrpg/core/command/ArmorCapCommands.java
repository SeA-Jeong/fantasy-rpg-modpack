package com.fantasyrpg.core.command;

import com.fantasyrpg.core.gear.armorcap.ArmorCapService;
import com.fantasyrpg.core.gear.armorcap.GradeSource;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 방어 등급 상한 확인·테스트 커맨드 ({@code /rpg armorcap ...}, 권한은 상위 {@code /rpg}의 2).
 * <pre>
 * /rpg armorcap info [플레이어]                 — 등급, 원래 값 → 목표 → 적용값, 보정 배율
 * /rpg armorcap grade &lt;플레이어&gt; &lt;1~10&gt;        — 테스트용 등급 덮어쓰기 (메모리, 재시작하면 사라짐)
 * /rpg armorcap grade &lt;플레이어&gt; clear          — 덮어쓰기 해제 (magiccity 등급으로 돌아감)
 * /rpg armorcap refresh [플레이어]              — 강제로 다시 계산
 * </pre>
 */
final class ArmorCapCommands {
    private ArmorCapCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        return literal("armorcap")
                .then(literal("info")
                        .executes(ctx -> info(ctx, ctx.getSource().getPlayerOrException()))
                        .then(argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(literal("grade")
                        .then(argument("player", EntityArgument.player())
                                .then(literal("clear").executes(ArmorCapCommands::clear))
                                .then(argument("grade", integer(GradeSource.MIN, GradeSource.MAX))
                                        .executes(ArmorCapCommands::override))))
                .then(literal("refresh")
                        .executes(ctx -> refresh(ctx, ctx.getSource().getPlayerOrException()))
                        .then(argument("player", EntityArgument.player())
                                .executes(ctx -> refresh(ctx, EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        ArmorCapService.refresh(player, false);
        ctx.getSource().sendSuccess(() -> Component.literal("[방어 상한] " + player.getGameProfile().getName()), false);
        for (String line : ArmorCapService.describe(player)) {
            ctx.getSource().sendSuccess(() -> Component.literal("  " + line), false);
        }
        return 1;
    }

    private static int override(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int grade = getInteger(ctx, "grade");
        GradeSource.override(player, grade);
        ArmorCapService.refresh(player, true);
        ctx.getSource().sendSuccess(() -> Component.literal("[방어 상한] " + player.getGameProfile().getName()
                + " 등급을 " + grade + "급으로 덮어씀 (테스트용, 재시작하면 사라짐)"), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        GradeSource.clearOverride(player);
        ArmorCapService.refresh(player, true);
        ctx.getSource().sendSuccess(() -> Component.literal("[방어 상한] " + player.getGameProfile().getName()
                + " 등급 덮어쓰기 해제 → " + GradeSource.grade(player) + "급"), true);
        return 1;
    }

    private static int refresh(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        ArmorCapService.refresh(player, true);
        return info(ctx, player);
    }
}
