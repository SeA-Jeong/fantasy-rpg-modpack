package com.fantasyrpg.core.command;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgPlayerData;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.fantasyrpg.core.skilltree.PassiveNode;
import com.fantasyrpg.core.skilltree.SkillData;
import com.fantasyrpg.core.skilltree.SkillNode;
import com.fantasyrpg.core.skilltree.SkillService;
import com.fantasyrpg.core.skilltree.SkillTreeRules;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 스킬트리 시험 명령 (권한 2). 화면(K)과 같은 서버 함수를 부른다.
 * <pre>
 * /rpg skill list                 — 내 직업 노드·랭크·SP·장착칸·코어
 * /rpg skill invest &lt;노드&gt;       — 1랭크 투자
 * /rpg skill refund &lt;노드&gt;       — 1랭크 되돌리기
 * /rpg skill reset [force]        — 전체 초기화 (무료권 사용, force는 무료권 없이 강제)
 * /rpg skill equip &lt;칸 1~5&gt; &lt;노드&gt; — 장착 (5칸은 궁극기)
 * /rpg skill cast &lt;칸 1~5&gt;        — 사용 (지금은 "아직 효과 없음")
 * /rpg passive list|invest|refund &lt;노드&gt; — 공통 패시브
 * </pre>
 */
public final class SkillCommands {
    private SkillCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> skill() {
        return literal("skill")
                .then(literal("list").executes(SkillCommands::list))
                .then(literal("invest").then(nodeArg().executes(c -> ok(c, SkillService.invest(me(c), getString(c, "node"))))))
                .then(literal("refund").then(nodeArg().executes(c -> ok(c, SkillService.refund(me(c), getString(c, "node"))))))
                .then(literal("reset")
                        .executes(c -> ok(c, SkillService.fullReset(me(c), false)))
                        .then(literal("force").executes(c -> ok(c, SkillService.fullReset(me(c), true)))))
                .then(literal("equip").then(argument("slot", integer(1, RpgPlayerData.SLOT_COUNT))
                        .then(nodeArg().executes(c -> ok(c, SkillService.equip(me(c), getString(c, "node"), getInteger(c, "slot") - 1))))))
                .then(literal("cast").then(argument("slot", integer(1, RpgPlayerData.SLOT_COUNT)).executes(c -> {
                    SkillService.cast(me(c), getInteger(c, "slot") - 1);
                    return 1;
                })));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> passive() {
        return literal("passive")
                .then(literal("list").executes(SkillCommands::passiveList))
                .then(literal("invest").then(passiveArg().executes(c -> ok(c, SkillService.passiveInvest(me(c), getString(c, "node"))))))
                .then(literal("refund").then(passiveArg().executes(c -> ok(c, SkillService.passiveRefund(me(c), getString(c, "node"))))));
    }

    private static ServerPlayer me(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    private static int ok(CommandContext<CommandSourceStack> c, boolean done) throws CommandSyntaxException {
        if (done) return list(c);
        return 0;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> nodeArg() {
        return argument("node", greedyString()).suggests((c, b) -> {
            ServerPlayer p = c.getSource().getPlayer();
            RpgClass cls = p == null ? null : RpgProgress.get(p).classOrNull();
            return SharedSuggestionProvider.suggest(cls == null ? java.util.List.of() : SkillData.nodes(cls.id()).keySet(), b);
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> passiveArg() {
        return argument("node", greedyString()).suggests((c, b) -> SharedSuggestionProvider.suggest(SkillData.passives().keySet(), b));
    }

    private static int list(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass cls = d.classOrNull();
        if (cls == null) {
            send(c, "§7[스킬] 무직 — /rpg class choose 먼저");
            return 0;
        }
        send(c, "§6[스킬] §f" + cls.id() + " Lv." + d.level() + " SP " + d.spAvailable() + "/" + d.spTotal()
                + " §7(무료 초기화 " + SkillService.freeResets(p, d) + "회)");
        for (SkillNode n : SkillData.nodes(cls.id()).values()) {
            int r = d.nodeRanks().getOrDefault(n.id(), 0);
            send(c, "§7 " + n.id() + " §f" + n.name() + " §7[" + n.kind().name().toLowerCase() + " " + n.tier() + "단]"
                    + " " + r + "/" + n.maxRank() + " §8Lv." + n.requiredLevel()
                    + (n.tier() > 2 ? " 방향SP " + SkillTreeRules.spentInDirection(n.directions().iterator().next(), d.nodeRanks(), SkillData.nodes(cls.id()), n.id()) : ""));
        }
        send(c, "§7장착 " + d.slots() + " §7코어 " + String.format("%.1f", CoreService.value(p)));
        return 1;
    }

    private static int passiveList(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        RpgPlayerData d = RpgProgress.get(p);
        int grade = MagicCityApi.grade(p);
        send(c, "§6[공통 패시브] §f포인트 " + (PassiveNode.points(grade) - SkillTreeRules.spent(d.passiveRanks()))
                + "/" + PassiveNode.points(grade) + " §7(등급 " + grade + ")");
        for (PassiveNode n : SkillData.passives().values()) {
            send(c, "§7 " + n.id() + " §f" + n.name() + " " + d.passiveRanks().getOrDefault(n.id(), 0) + "/" + n.maxRank()
                    + " §8등급 " + n.requiredGrade() + "+, " + n.attribute() + " +" + n.perRank() + "/랭크");
        }
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
