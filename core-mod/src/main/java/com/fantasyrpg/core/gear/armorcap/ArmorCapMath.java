package com.fantasyrpg.core.gear.armorcap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 방어 속성 상한 보정의 순수 계산 (Minecraft 클래스 비의존, JUnit 대상).
 *
 * <p>설계: 프로젝트 문서 {@code claude/장비등급제한_설계.md} 2-2절.
 * <ol>
 *   <li>우리 보정 수정자를 뺀 수정자 목록(others)으로 계산한다 — 자기 보정 제외.</li>
 *   <li>우리 장비가 준 수정자를 effectiveTier 값으로 <b>가상 교체</b>한 사본으로 다시 계산한다.</li>
 *   <li>바닐라 {@code AttributeInstance}와 같은 순서(덧셈 → 기본 곱셈 → 최종 곱셈)로 계산하고, 범위 제한 전 값을 쓴다.</li>
 *   <li>목표값 = min(범위 제한한 effPre, 등급 상한, 범위 제한한 rawPre).</li>
 *   <li>보정은 최종 곱셈(ADD_MULTIPLIED_TOTAL) 하나: k = target / rawPre − 1 (범위 제한 전 값 기준).</li>
 *   <li>실제 적용 결과를 다시 읽어 {@link #refine}로 ulp 단위 조정한다(방어력은 바닐라가 정수 내림해 쓰므로 정수 경계를 맞춘다).</li>
 * </ol>
 */
public final class ArmorCapMath {
    /** 바닐라 AttributeModifier.Operation 과 같은 의미. */
    public enum Op { ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL }

    /** 속성 수정자 하나 (id는 비교·가상 교체용 문자열). */
    public record Mod(String id, Op op, double amount) {
    }

    /**
     * 계산 결과.
     *
     * @param rawPre  보정이 없을 때의 범위 제한 전 값
     * @param effPre  가상 교체 후 범위 제한 전 값
     * @param target  적용해야 할 최종값
     * @param needed  보정 수정자가 필요한지 (false면 보정을 제거)
     * @param k       최종 곱셈 보정값의 첫 추정치 (needed일 때만 의미)
     */
    public record Plan(double rawPre, double effPre, double target, boolean needed, double k) {
    }

    /** 실제 적용 결과를 돌려주는 함수 (게임에서는 수정자를 넣고 getValue()를 읽는다). */
    @FunctionalInterface
    public interface Evaluator {
        double valueWith(double k);
    }

    private static final double SNAP = 1e-9;
    private static final double TOLERANCE = 1e-9;
    private static final int MAX_ULP_STEPS = 64;

    private ArmorCapMath() {
    }

    /** 바닐라 AttributeInstance.calculateValue 와 같은 순서. 범위 제한 전 값을 돌려준다. */
    public static double calc(double base, Iterable<Mod> mods) {
        double d0 = base;
        for (Mod m : mods) {
            if (m.op() == Op.ADD_VALUE) d0 += m.amount();
        }
        double d1 = d0;
        for (Mod m : mods) {
            if (m.op() == Op.ADD_MULTIPLIED_BASE) d1 += d0 * m.amount();
        }
        for (Mod m : mods) {
            if (m.op() == Op.ADD_MULTIPLIED_TOTAL) d1 *= 1.0 + m.amount();
        }
        return d1;
    }

    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** id가 replacements에 있는 수정자의 값만 바꾼 사본 (가상 교체). */
    public static List<Mod> replace(List<Mod> mods, Map<String, Double> replacements) {
        if (replacements.isEmpty()) return mods;
        List<Mod> out = new ArrayList<>(mods.size());
        for (Mod m : mods) {
            Double v = replacements.get(m.id());
            out.add(v == null ? m : new Mod(m.id(), m.op(), v));
        }
        return out;
    }

    /** 부동소수 흔들림 제거: 정수에 아주 가까우면 정수로. */
    static double snap(double v) {
        double r = Math.rint(v);
        return Math.abs(v - r) < SNAP ? r : v;
    }

    /**
     * @param others       우리 보정 수정자를 뺀 수정자 목록
     * @param replacements 가상 교체할 우리 장비 수정자 (id → effectiveTier 값). 최소 기능 단계에서는 비어 있다
     * @param cap          등급 상한 (상한이 없으면 {@link Double#POSITIVE_INFINITY})
     * @param min          속성 최솟값 (ARMOR 0)
     * @param max          속성 최댓값 (ARMOR 30)
     */
    public static Plan plan(double base, List<Mod> others, Map<String, Double> replacements,
                            double cap, double min, double max) {
        double rawPre = calc(base, others);
        double effPre = calc(base, replace(others, replacements));
        if (!Double.isFinite(rawPre) || !Double.isFinite(effPre)) {
            return new Plan(rawPre, effPre, Double.NaN, false, 0.0);
        }
        double rawClamped = clamp(rawPre, min, max);
        double target = snap(Math.min(Math.min(clamp(effPre, min, max), cap), rawClamped));
        boolean needed = rawPre > 0.0 && rawClamped > target;
        double k = needed ? target / rawPre - 1.0 : 0.0;
        return new Plan(rawPre, effPre, target, needed, k);
    }

    /** 적용 결과가 목표를 만족하는지. 방어력(integerFloor)은 바닐라처럼 정수 내림한 값이 같아야 한다. */
    public static boolean accepted(double value, double target, boolean integerFloor) {
        if (integerFloor) return Math.floor(value) == Math.floor(target);
        return Math.abs(value - target) <= TOLERANCE * Math.max(1.0, Math.abs(target));
    }

    /**
     * 실제 적용 결과로 k를 조정한다. 먼저 선형 보정(값은 1+k에 비례)을 한 번 하고,
     * 그래도 맞지 않으면 {@link Math#nextUp}/{@link Math#nextDown}으로 한 칸씩 움직인다.
     * 방어력은 내림 값이 목표보다 작으면 올리고, 크면 내린다 — 목표를 넘는 쪽으로 받아들이지 않는다.
     *
     * @return 조정된 k. 끝까지 맞지 않으면 마지막으로 시도한 k (호출 쪽에서 {@link #accepted}로 다시 확인)
     */
    public static double refine(double k, double target, boolean integerFloor, Evaluator ev) {
        double v = ev.valueWith(k);
        if (accepted(v, target, integerFloor)) return k;
        double scale = 1.0 + k;
        if (scale != 0.0 && v != 0.0 && Double.isFinite(v)) {
            double c = v / scale;
            k = target / c - 1.0;
            v = ev.valueWith(k);
            if (accepted(v, target, integerFloor)) return k;
        }
        for (int i = 0; i < MAX_ULP_STEPS; i++) {
            boolean low = integerFloor ? Math.floor(v) < Math.floor(target) : v < target;
            k = low ? Math.nextUp(k) : Math.nextDown(k);
            v = ev.valueWith(k);
            if (accepted(v, target, integerFloor)) return k;
        }
        return k;
    }
}
