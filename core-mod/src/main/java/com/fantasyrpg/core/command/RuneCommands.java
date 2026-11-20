package com.fantasyrpg.core.command;

import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import com.fantasyrpg.core.gear.rune.RuneDefinition;
import com.fantasyrpg.core.gear.rune.RuneRegistry;
import com.fantasyrpg.core.gear.rune.RuneRules;
import com.fantasyrpg.core.gear.rune.RuneSockets;
import com.fantasyrpg.core.gear.rune.RuneType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 룬석 시험 명령 (권한 2).
 * <pre>
 * /rpg rune give &lt;종류&gt; &lt;등급&gt; [개수]   — 룬석 (life, guard, steady, gale, leap, climb / 1~4)
 * /rpg rune abrasive &lt;등급&gt; [개수]      — 연마재 (1~3)
 * /rpg rune extractor [개수]             — 추출 도구
 * /rpg rune insert &lt;종류&gt; &lt;등급&gt;      — 손에 든 계열 갑옷에 바로 끼움 (부위·빈 칸 규칙은 조합과 같음)
 * /rpg rune clear                       — 손에 든 갑옷의 룬석을 모두 지움
 * </pre>
 */
public final class RuneCommands {
    private RuneCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return literal("rune")
                .then(literal("give")
                        .then(argument("type", word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(RuneType.values()).map(RuneType::id), b))
                                .then(argument("grade", integer(RuneDefinition.MIN_GRADE, RuneDefinition.MAX_GRADE))
                                        .executes(c -> give(c, 1))
                                        .then(argument("count", integer(1, 64))
                                                .executes(c -> give(c, getInteger(c, "count")))))))
                .then(literal("abrasive")
                        .then(argument("grade", integer(1, RuneRules.MAX_ABRASIVE_GRADE))
                                .executes(c -> abrasive(c, 1))
                                .then(argument("count", integer(1, 64))
                                        .executes(c -> abrasive(c, getInteger(c, "count"))))))
                .then(literal("extractor")
                        .executes(c -> extractor(c, 1))
                        .then(argument("count", integer(1, 64)).executes(c -> extractor(c, getInteger(c, "count")))))
                .then(literal("insert")
                        .then(argument("type", word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(RuneType.values()).map(RuneType::id), b))
                                .then(argument("grade", integer(RuneDefinition.MIN_GRADE, RuneDefinition.MAX_GRADE))
                                        .executes(RuneCommands::insert))))
                .then(literal("clear").executes(RuneCommands::clear));
    }

    private static RuneType type(CommandContext<CommandSourceStack> ctx) {
        RuneType t = RuneType.byId(getString(ctx, "type"));
        if (t == null) ctx.getSource().sendFailure(Component.literal("모르는 룬석입니다. life, guard, steady, gale, leap, climb 중 하나"));
        return t;
    }

    private static void giveStack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static int give(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        RuneType t = type(ctx);
        if (t == null) return 0;
        ItemStack s = RuneRegistry.runeStack(t, getInteger(ctx, "grade"));
        s.setCount(count);
        giveStack(player, s);
        send(ctx, "§6[룬석] §f" + s.getHoverName().getString() + " ×" + count + " 지급");
        return 1;
    }

    private static int abrasive(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack s = RuneRegistry.abrasiveStack(getInteger(ctx, "grade"));
        s.setCount(count);
        giveStack(player, s);
        send(ctx, "§6[룬석] §f" + s.getHoverName().getString() + " ×" + count + " 지급");
        return 1;
    }

    private static int extractor(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        giveStack(player, new ItemStack(RuneRegistry.EXTRACTOR.get(), count));
        send(ctx, "§6[룬석] §f추출 도구 ×" + count + " 지급");
        return 1;
    }

    private static int insert(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof FamilyArmorItem armor)) {
            ctx.getSource().sendFailure(Component.literal("손에 계열 방어구를 들어야 합니다"));
            return 0;
        }
        RuneType t = type(ctx);
        if (t == null) return 0;
        RuneDefinition def = RuneRegistry.def(t);
        if (def == null) {
            ctx.getSource().sendFailure(Component.literal("룬석 효과 데이터가 없습니다 (" + t.id() + ")"));
            return 0;
        }
        RuneSockets sockets = RuneRegistry.socketsOf(held);
        int count = RuneRegistry.socketCount(held);
        switch (RuneRules.canInsert(def, armor.part(), sockets.runes().size(), count, armor.family().light())) {
            case WRONG_PART -> {
                ctx.getSource().sendFailure(Component.literal("이 룬석은 이 부위에 끼울 수 없습니다"));
                return 0;
            }
            case LIGHT_ONLY -> {
                ctx.getSource().sendFailure(Component.literal("이 룬석은 엘프·마도 갑옷에만 끼울 수 있습니다"));
                return 0;
            }
            case NO_FREE_SOCKET -> {
                ctx.getSource().sendFailure(Component.literal("빈 룬석 칸이 없습니다 (" + sockets.runes().size() + "/" + count + ")"));
                return 0;
            }
            case OK -> {
            }
        }
        held.set(RuneRegistry.SOCKETS.get(), RuneSockets.fromRules(
                RuneRules.insert(sockets.toRules(), new RuneRules.Socketed(t, getInteger(ctx, "grade")))));
        send(ctx, "§6[룬석] §f끼움 (" + (sockets.runes().size() + 1) + "/" + count + ")");
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.getMainHandItem().remove(RuneRegistry.SOCKETS.get());
        send(ctx, "§6[룬석] §f손에 든 아이템의 룬석을 모두 지움");
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
