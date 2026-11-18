package com.fantasyrpg.core.command;

import com.fantasyrpg.core.playerdata.ProgressionManager;
import com.fantasyrpg.core.playerdata.ProgressionMath;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgPlayerData;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 플레이어 성장 시험 명령 (권한 2). 서장 NPC가 생기기 전까지 직업 선택을 대신한다.
 * <pre>
 * /rpg player info [대상]        — 직업·레벨·경험치·SP·레벨 상한·완료 수
 * /rpg player reset              — 성장 데이터 초기화 (시험용)
 * /rpg class choose &lt;직업&gt;      — 직업 선택 (무직일 때만, 시작 SP 1)
 * /rpg class change &lt;직업&gt;      — 직업 변경 (레벨 유지, 쓴 SP 반환. 비용은 미정이라 지금은 무료)
 * /rpg xp add &lt;양&gt;              — 경험치 지급 (상한 규칙 적용)
 * </pre>
 * 직업: warrior 전사, knight 기사, rogue 도적, archer 궁수, mage 마법사, cleric 전투사제
 */
public final class PlayerCommands {
    private PlayerCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> player() {
        return literal("player")
                .then(literal("info")
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(argument("target", EntityArgument.player())
                                .executes(c -> info(c, EntityArgument.getPlayer(c, "target")))))
                .then(literal("reset").executes(c -> {
                    RpgProgress.reset(c.getSource().getPlayerOrException());
                    send(c, "§6[성장] §f성장 데이터를 초기화함 (무직 Lv.1)");
                    return 1;
                }));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> rpgClass() {
        return literal("class")
                .then(literal("choose").then(classArg().executes(c -> setClass(c, false))))
                .then(literal("change").then(classArg().executes(c -> setClass(c, true))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> xp() {
        return literal("xp")
                .then(literal("add").then(argument("amount", integer(1, 1_000_000)).executes(c -> {
                    RpgProgress.grantXp(c.getSource().getPlayerOrException(), getInteger(c, "amount"), "command");
                    return info(c, c.getSource().getPlayerOrException());
                })));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> classArg() {
        return argument("class", word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                Arrays.stream(RpgClass.values()).map(RpgClass::id), b));
    }

    private static int setClass(CommandContext<CommandSourceStack> ctx, boolean change) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        RpgClass c = RpgClass.byId(getString(ctx, "class"));
        if (c == null) {
            ctx.getSource().sendFailure(Component.literal("모르는 직업입니다. warrior, knight, rogue, archer, mage, cleric 중 하나"));
            return 0;
        }
        RpgProgress.ClassResult r = change ? RpgProgress.changeClass(p, c) : RpgProgress.chooseClass(p, c);
        switch (r) {
            case CHOSEN -> {
                send(ctx, "§6[성장] §f직업 선택: " + Component.translatable("class.fantasyrpg." + c.id()).getString() + " (시작 SP +1)");
                com.fantasyrpg.core.playerdata.ClassSelection.notifyChosen(p);   // 서장 퀘스트 단계도 넘김
            }
            case CHANGED -> send(ctx, "§6[성장] §f직업 변경: " + Component.translatable("class.fantasyrpg." + c.id()).getString() + " (쓴 SP 반환)");
            case SAME -> send(ctx, "§7[성장] 이미 그 직업입니다");
            case ALREADY_CHOSEN -> {
                ctx.getSource().sendFailure(Component.literal("이미 직업이 있습니다. 바꾸려면 /rpg class change"));
                return 0;
            }
        }
        return info(ctx, p);
    }

    private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        int need = ProgressionManager.INSTANCE.settings().need(d.level());
        int cap = RpgProgress.levelCap(p);
        send(ctx, "§6[성장] §f" + p.getGameProfile().getName() + " — "
                + (c == null ? "무직" : Component.translatable("class.fantasyrpg." + c.id()).getString())
                + " Lv." + d.level() + " §7(상한 Lv." + cap + ")");
        send(ctx, "§7경험치 " + d.xp() + " / " + need + (d.level() >= cap && need > 0 ? " §8(상한: 보관 중)" : ""));
        send(ctx, "§7SP 받은 합계 " + d.spTotal() + " = 직업 " + (d.startSpGranted() ? 1 : 0)
                + " + 레벨업 " + (d.level() - 1) + " + 완료 보너스 " + d.clearBonusGranted()
                + " §8(목표 " + ProgressionMath.bonusTarget(MagicCityApi.completed(p)) + ")§7, 사용 " + d.spSpent()
                + ", 남음 " + d.spAvailable());
        send(ctx, "§7메인 챕터 완료 " + MagicCityApi.completed(p) + "개, 등급 " + MagicCityApi.grade(p)
                + ", 직업 변경 " + d.classChanges() + "회");
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
