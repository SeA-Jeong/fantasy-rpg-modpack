package com.fantasyrpg.core.command;

import com.fantasyrpg.core.combat.DamageBreakdown;
import com.fantasyrpg.core.combat.DamageCalculator;
import com.fantasyrpg.core.combat.DamageInput;
import com.fantasyrpg.core.combat.Resistances;
import com.fantasyrpg.core.combat.RpgDamage;
import com.fantasyrpg.core.resist.MobResistanceManager;
import com.fantasyrpg.core.resist.MobResistanceProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.function.Function;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 테스트·관리 커맨드 (권한 2 이상).
 * <pre>
 * /rpg simulate &lt;W&gt; &lt;Cp&gt; &lt;Cm&gt; &lt;Rp&gt; &lt;Rm&gt; [Pp] [Pm] [치명타배율]   — 숫자만으로 계산
 * /rpg resist &lt;대상&gt;                                       — 대상의 저항 데이터
 * /rpg hit &lt;대상&gt; &lt;W&gt; &lt;Cp&gt; &lt;Cm&gt; [Pp] [Pm] [치명타배율]        — 대상 저항으로 계산 후 실제 피해
 * /rpg armor give|tier|info                              — 계열 방어구 시험 ({@link ArmorCommands})
 * /rpg rune give|abrasive|extractor|insert|clear         — 룬석 시험 ({@link RuneCommands})
 * /rpg player info|reset, /rpg class choose|change, /rpg xp add — 성장 시험 ({@link PlayerCommands})
 * /rpg skill list|invest|refund|reset|equip|cast, /rpg passive list|invest|refund — 스킬트리 시험 ({@link SkillCommands})
 * </pre>
 * 비율 값은 0~1로 입력한다 (예: 25% → 0.25).
 */
public final class RpgCommands {
    private RpgCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("rpg")
                .requires(s -> s.hasPermission(2))
                .then(literal("simulate")
                        .then(argument("W", doubleArg(0))
                                .then(argument("Cp", doubleArg(0))
                                        .then(argument("Cm", doubleArg(0))
                                                .then(argument("Rp", doubleArg(0, 1))
                                                        .then(withOptionalTail(argument("Rm", doubleArg(0, 1)),
                                                                RpgCommands::simulate)))))))
                .then(literal("resist")
                        .then(argument("target", EntityArgument.entity())
                                .executes(RpgCommands::resist)))
                .then(ArmorCommands.node())
                .then(RuneCommands.node())
                .then(PlayerCommands.player())
                .then(PlayerCommands.rpgClass())
                .then(PlayerCommands.xp())
                .then(SkillCommands.skill())
                .then(SkillCommands.passive())
                .then(literal("hit")
                        .then(argument("target", EntityArgument.entity())
                                .then(argument("W", doubleArg(0))
                                        .then(argument("Cp", doubleArg(0))
                                                .then(withOptionalTail(argument("Cm", doubleArg(0)),
                                                        RpgCommands::hit)))))));
    }

    /** [Pp] [Pm] [치명타배율] 선택 인자를 붙인다. 각 단계에서 실행 가능. */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withOptionalTail(
            T node, Function<CommandContext<CommandSourceStack>, Integer> action) {
        return node.executes(action::apply)
                .then(argument("Pp", doubleArg(0, 1)).executes(action::apply)
                        .then(argument("Pm", doubleArg(0, 1)).executes(action::apply)
                                .then(argument("crit", doubleArg(0)).executes(action::apply))));
    }

    private static double optional(CommandContext<CommandSourceStack> ctx, String name, double def) {
        try {
            return getDouble(ctx, name);
        } catch (IllegalArgumentException missing) {
            return def;
        }
    }

    private static DamageInput readInput(CommandContext<CommandSourceStack> ctx) {
        return DamageInput.of(getDouble(ctx, "W"), getDouble(ctx, "Cp"), getDouble(ctx, "Cm"))
                .withPenetration(optional(ctx, "Pp", 0), optional(ctx, "Pm", 0))
                .withCrit(optional(ctx, "crit", 1));
    }

    private static int simulate(CommandContext<CommandSourceStack> ctx) {
        Resistances r = new Resistances(getDouble(ctx, "Rp"), getDouble(ctx, "Rm"));
        DamageBreakdown b = DamageCalculator.calculate(readInput(ctx), r);
        send(ctx, "§6[RPG 계산] §f저항 물리 " + pct(r.physical()) + " / 마법 " + pct(r.magic()));
        b.describe().forEach(line -> send(ctx, "§7" + line));
        return (int) Math.round(b.total());
    }

    private static int resist(CommandContext<CommandSourceStack> ctx) {
        Entity target;
        try {
            target = EntityArgument.getEntity(ctx, "target");
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        MobResistanceProfile p = MobResistanceManager.INSTANCE.profile(target.getType());
        Resistances r = RpgDamage.resistancesOf(target);
        send(ctx, "§6[RPG 저항] §f" + id + (p == null ? " §8(데이터 없음, 기본값)" : ""));
        send(ctx, "§7물리 " + pct(r.physical()) + " / 마법 " + pct(r.magic())
                + (p == null ? "" : " / 역할 " + p.role() + " / " + p.chapter() + "장"));
        return 1;
    }

    private static int hit(CommandContext<CommandSourceStack> ctx) {
        Entity target;
        try {
            target = EntityArgument.getEntity(ctx, "target");
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        if (!(target instanceof LivingEntity living)) {
            ctx.getSource().sendFailure(Component.literal("생명체가 아닌 대상에는 피해를 줄 수 없습니다"));
            return 0;
        }
        float before = living.getHealth();
        DamageBreakdown b = RpgDamage.apply(living, ctx.getSource().getEntity(), readInput(ctx));
        Resistances r = RpgDamage.resistancesOf(living);
        send(ctx, "§6[RPG 타격] §f" + BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())
                + " §7(저항 물리 " + pct(r.physical()) + " / 마법 " + pct(r.magic()) + ")");
        b.describe().forEach(line -> send(ctx, "§7" + line));
        send(ctx, String.format(Locale.ROOT, "§a체력 %.1f → %.1f", before, living.getHealth()));
        return (int) Math.round(b.total());
    }

    private static String pct(double ratio) {
        return String.format(Locale.ROOT, "%.0f%%", ratio * 100);
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
