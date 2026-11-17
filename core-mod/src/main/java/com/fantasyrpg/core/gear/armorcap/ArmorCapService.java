package com.fantasyrpg.core.gear.armorcap;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.RpgConfig;
import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Mod;
import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Op;
import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Plan;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 플레이어 방어력·방어 강도에 등급 상한을 적용한다 (서버 전용).
 *
 * <p>방식: 플레이어의 ARMOR·ARMOR_TOUGHNESS 속성에 최종 곱셈형 임시 수정자 {@link #MODIFIER_ID} 하나를 둔다.
 * 아이템은 바꾸지 않는다. 바닐라 방어 공식이 이 속성값을 읽으므로 감소 계산은 바닐라가 한 번만 한다.
 * 피해량은 건드리지 않는다.
 *
 * <p>갱신 시점: 매 틱 끝(상태가 바뀌었을 때만)과 피격 직전({@link LivingIncomingDamageEvent}, 가장 낮은 우선순위).
 * 상태가 바뀌었으면 같은 틱이라도 다시 계산한다(계산 횟수 제한 없음 — 피해 계산 시점의 정확성 우선).
 *
 * <p>설계: 프로젝트 문서 {@code claude/장비등급제한_설계.md} 2절.
 */
public final class ArmorCapService {
    public static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "grade_limit");

    /** 연속으로 상태가 바뀐 틱 수가 이만큼이면 호환성 경고. */
    private static final int CONFLICT_STREAK = 40;
    /** 같은 플레이어 경고 간격 (5분). */
    private static final int WARN_INTERVAL_TICKS = 20 * 60 * 5;

    /** 엔티티 객체 기준: 리스폰으로 새 플레이어 객체가 생기면 자동으로 다시 계산된다. */
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    /** 재진입 방지 (서버 스레드 하나에서만 호출됨). */
    private static boolean busy;

    private ArmorCapService() {
    }

    // ── 이벤트 ──

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player, false);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player, false);
        }
    }

    // ── 공개 ──

    /** 상태가 바뀌었으면(또는 force) 다시 계산해 보정 수정자를 맞춘다. */
    public static void refresh(ServerPlayer player, boolean force) {
        if (busy) return;
        busy = true;
        try {
            State state = STATES.computeIfAbsent(player, p -> new State());
            Signature now = signature(player);
            if (!force && now.equals(state.signature)) {
                state.changeStreak = 0;
                return;
            }
            if (state.signature != null) {
                state.changeStreak++;
                warnIfConflicting(player, state);
            }
            boolean enabled = now.enabled();
            apply(player, Attributes.ARMOR, enabled, now.armorCap(), true);
            apply(player, Attributes.ARMOR_TOUGHNESS, enabled, now.toughnessCap(), false);
            state.signature = signature(player);
        } finally {
            busy = false;
        }
    }

    /** 명령어 표시용 진단 정보. */
    public static List<String> describe(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        int grade = GradeSource.grade(player);
        lines.add("등급 " + grade + "급" + (GradeSource.isOverridden(player) ? " (테스트 덮어쓰기)" : " (magiccity)")
                + ", 상한 " + (RpgConfig.flag(RpgConfig.ARMOR_CAP_ENABLED, true) ? "켜짐" : "꺼짐"));
        lines.add(describeOne(player, Attributes.ARMOR, "방어력", capFor(RpgConfig.ARMOR_CAPS, RpgConfig.DEFAULT_ARMOR_CAPS, grade)));
        lines.add(describeOne(player, Attributes.ARMOR_TOUGHNESS, "방어 강도", capFor(RpgConfig.TOUGHNESS_CAPS, RpgConfig.DEFAULT_TOUGHNESS_CAPS, grade)));
        lines.add("실제 방어력(정수 내림) " + player.getArmorValue());
        return lines;
    }

    // ── 내부 ──

    private static void apply(ServerPlayer player, Holder<Attribute> attribute, boolean enabled, double cap, boolean integerFloor) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier current = inst.getModifier(MODIFIER_ID);
        if (!enabled) {
            if (current != null) inst.removeModifier(MODIFIER_ID);
            return;
        }
        Plan plan = ArmorCapMath.plan(inst.getBaseValue(), others(inst), Map.of(), cap,
                minOf(attribute), maxOf(attribute));
        if (!plan.needed()) {
            if (current != null) inst.removeModifier(MODIFIER_ID);
            return;
        }
        // 지금 붙어 있는 보정이 이미 목표를 만족하면 다시 등록하지 않는다 (불필요한 동기화 방지)
        if (current != null && ArmorCapMath.accepted(inst.getValue(), plan.target(), integerFloor)) return;

        double k = ArmorCapMath.refine(plan.k(), plan.target(), integerFloor, candidate -> {
            set(inst, candidate);
            return inst.getValue();
        });
        set(inst, k);
        if (!ArmorCapMath.accepted(inst.getValue(), plan.target(), integerFloor)) {
            FantasyRpgCore.LOGGER.warn("방어 상한 보정이 목표값에 맞지 않음: {} {} 목표 {} 결과 {}",
                    player.getGameProfile().getName(), attribute.getRegisteredName(), plan.target(), inst.getValue());
        }
    }

    private static void set(AttributeInstance inst, double k) {
        AttributeModifier current = inst.getModifier(MODIFIER_ID);
        if (current != null) {
            if (current.amount() == k) return;
            inst.removeModifier(MODIFIER_ID);
        }
        inst.addTransientModifier(new AttributeModifier(MODIFIER_ID, k, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** 우리 보정 수정자를 뺀 수정자 목록 (id 순으로 정렬해 비교가 순서에 흔들리지 않게). */
    private static List<Mod> others(AttributeInstance inst) {
        List<Mod> mods = new ArrayList<>();
        for (AttributeModifier m : inst.getModifiers()) {
            if (m.id().equals(MODIFIER_ID)) continue;
            mods.add(new Mod(m.id().toString(), toOp(m.operation()), m.amount()));
        }
        mods.sort(Comparator.comparing(Mod::id).thenComparing(m -> m.op().ordinal()));
        return mods;
    }

    private static Op toOp(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_VALUE -> Op.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> Op.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> Op.ADD_MULTIPLIED_TOTAL;
        };
    }

    private static double minOf(Holder<Attribute> attribute) {
        return attribute.value() instanceof RangedAttribute r ? r.getMinValue() : Double.NEGATIVE_INFINITY;
    }

    private static double maxOf(Holder<Attribute> attribute) {
        return attribute.value() instanceof RangedAttribute r ? r.getMaxValue() : Double.POSITIVE_INFINITY;
    }

    private static double capFor(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<List<? extends Number>> value,
                                 List<Double> fallback, int grade) {
        double[] caps = RpgConfig.numbers(value, fallback);
        return caps[Math.max(0, Math.min(caps.length - 1, grade - 1))];
    }

    private static Signature signature(ServerPlayer player) {
        int grade = GradeSource.grade(player);
        return new Signature(
                RpgConfig.flag(RpgConfig.ARMOR_CAP_ENABLED, true),
                grade,
                capFor(RpgConfig.ARMOR_CAPS, RpgConfig.DEFAULT_ARMOR_CAPS, grade),
                capFor(RpgConfig.TOUGHNESS_CAPS, RpgConfig.DEFAULT_TOUGHNESS_CAPS, grade),
                snapshot(player.getAttribute(Attributes.ARMOR)),
                snapshot(player.getAttribute(Attributes.ARMOR_TOUGHNESS)));
    }

    private static Snapshot snapshot(AttributeInstance inst) {
        if (inst == null) return new Snapshot(0.0, List.of(), null);
        AttributeModifier ours = inst.getModifier(MODIFIER_ID);
        return new Snapshot(inst.getBaseValue(), others(inst), ours == null ? null : ours.amount());
    }

    private static String describeOne(ServerPlayer player, Holder<Attribute> attribute, String label, double cap) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return label + ": 속성 없음";
        Plan plan = ArmorCapMath.plan(inst.getBaseValue(), others(inst), Map.of(), cap, minOf(attribute), maxOf(attribute));
        AttributeModifier ours = inst.getModifier(MODIFIER_ID);
        return String.format(java.util.Locale.ROOT, "%s: 원래 %.3f → 목표 %.3f (상한 %.0f), 적용값 %.6f, 보정 %s",
                label, ArmorCapMath.clamp(plan.rawPre(), minOf(attribute), maxOf(attribute)), plan.target(), cap,
                inst.getValue(), ours == null ? "없음" : String.format(java.util.Locale.ROOT, "×%.9f", 1.0 + ours.amount()));
    }

    private static void warnIfConflicting(ServerPlayer player, State state) {
        if (state.changeStreak < CONFLICT_STREAK) return;
        int now = player.server.getTickCount();
        if (now - state.lastWarnTick < WARN_INTERVAL_TICKS && state.lastWarnTick != Integer.MIN_VALUE) return;
        state.lastWarnTick = now;
        FantasyRpgCore.LOGGER.warn("[호환성] {}의 방어 속성이 {}틱 연속으로 바뀌고 있습니다. 다른 모드가 방어력을 계속 조정하는지 확인하세요. "
                + "상한 계산은 계속 적용됩니다.", player.getGameProfile().getName(), state.changeStreak);
    }

    private record Snapshot(double base, List<Mod> mods, Double ourAmount) {
    }

    private record Signature(boolean enabled, int grade, double armorCap, double toughnessCap,
                             Snapshot armor, Snapshot toughness) {
    }

    private static final class State {
        Signature signature;
        int changeStreak;
        int lastWarnTick = Integer.MIN_VALUE;
    }
}
