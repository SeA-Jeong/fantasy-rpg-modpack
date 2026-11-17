package com.fantasyrpg.core.gear.armorcap;

import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Mod;
import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Op;
import com.fantasyrpg.core.gear.armorcap.ArmorCapMath.Plan;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 방어 등급 상한 보정 순수 계산 (설계 문서 장비등급제한 2-2절). */
class ArmorCapMathTest {
    private static final double ARMOR_MIN = 0, ARMOR_MAX = 30, TOUGH_MAX = 20;

    private static List<Mod> armorSet(double... perSlot) {
        List<Mod> mods = new ArrayList<>();
        for (int i = 0; i < perSlot.length; i++) mods.add(new Mod("minecraft:armor.slot" + i, Op.ADD_VALUE, perSlot[i]));
        return mods;
    }

    /** 보정 수정자를 더해 바닐라 순서로 계산 (게임의 getValue() 역할). */
    private static ArmorCapMath.Evaluator evaluator(List<Mod> others, double max) {
        return k -> {
            List<Mod> all = new ArrayList<>(others);
            all.add(new Mod("fantasyrpg:grade_limit", Op.ADD_MULTIPLIED_TOTAL, k));
            return ArmorCapMath.clamp(ArmorCapMath.calc(0, all), ARMOR_MIN, max);
        };
    }

    private static double solveAndApply(List<Mod> others, Map<String, Double> rep, double cap, double max, boolean floor) {
        Plan p = ArmorCapMath.plan(0, others, rep, cap, ARMOR_MIN, max);
        if (!p.needed()) return ArmorCapMath.clamp(p.rawPre(), ARMOR_MIN, max);
        ArmorCapMath.Evaluator ev = evaluator(others, max);
        double k = ArmorCapMath.refine(p.k(), p.target(), floor, ev);
        return ev.valueWith(k);
    }

    @Test
    void 바닐라_계산_순서() {
        List<Mod> mods = List.of(new Mod("a", Op.ADD_VALUE, 10), new Mod("b", Op.ADD_MULTIPLIED_BASE, 0.5),
                new Mod("c", Op.ADD_MULTIPLIED_TOTAL, 1.0), new Mod("d", Op.ADD_MULTIPLIED_BASE, 0.5));
        // (0 + 10) → 10 + 10×0.5 + 10×0.5 = 20 → ×2 = 40
        assertEquals(40.0, ArmorCapMath.calc(0, mods), 1e-12);
    }

    @Test
    void 일급_철갑옷은_12() {
        List<Mod> iron = armorSet(2, 6, 5, 2);
        assertEquals(12.0, Math.floor(solveAndApply(iron, Map.of(), 12, ARMOR_MAX, true)));
    }

    @Test
    void 이급_철갑옷은_그대로_15() {
        Plan p = ArmorCapMath.plan(0, armorSet(2, 6, 5, 2), Map.of(), 15, ARMOR_MIN, ARMOR_MAX);
        assertFalse(p.needed());
        assertEquals(15.0, p.target());
    }

    @Test
    void 육급_네더라이트_강도는_4() {
        // 네더라이트 조정값 강도 1/2/2/1 = 6, 6급 상한 4
        double v = solveAndApply(armorSet(1, 2, 2, 1), Map.of(), 4, TOUGH_MAX, false);
        assertEquals(4.0, v, 1e-9);
    }

    @Test
    void 가상교체는_곱셈_전에_적용된다() {
        // 결정 문서 예시: 흉갑 8 → 적용 등급 값 6, 다른 효과 최종 ×1.5 → 목표 9 (10이 아님)
        List<Mod> mods = List.of(new Mod("chest", Op.ADD_VALUE, 8), new Mod("other", Op.ADD_MULTIPLIED_TOTAL, 0.5));
        Plan p = ArmorCapMath.plan(0, mods, Map.of("chest", 6.0), 20, ARMOR_MIN, ARMOR_MAX);
        assertEquals(12.0, p.rawPre(), 1e-12);
        assertEquals(9.0, p.target(), 1e-12);
    }

    @Test
    void 장신구_방어력도_상한에_포함된다() {
        List<Mod> mods = armorSet(3, 8, 6, 3);
        mods.add(new Mod("othermod:ring", Op.ADD_VALUE, 3));
        assertEquals(20.0, Math.floor(solveAndApply(mods, Map.of(), 20, ARMOR_MAX, true)));
    }

    @Test
    void 보정값은_클램프_전_값으로_계산한다() {
        // 원래 40(바닐라가 30으로 자름), 상한 20 → k = 20/40 − 1 = −0.5
        Plan p = ArmorCapMath.plan(0, List.of(new Mod("x", Op.ADD_VALUE, 40)), Map.of(), 20, ARMOR_MIN, ARMOR_MAX);
        assertTrue(p.needed());
        assertEquals(-0.5, p.k(), 1e-12);
    }

    @Test
    void 값이_0_이하면_보정하지_않는다() {
        Plan zero = ArmorCapMath.plan(0, List.of(), Map.of(), 12, ARMOR_MIN, ARMOR_MAX);
        assertFalse(zero.needed());
        Plan negative = ArmorCapMath.plan(0, List.of(new Mod("a", Op.ADD_VALUE, 10),
                new Mod("b", Op.ADD_MULTIPLIED_BASE, -1.0)), Map.of(), 12, ARMOR_MIN, ARMOR_MAX);
        assertFalse(negative.needed());
    }

    @Test
    void 상한_0이면_정확히_0() {
        double v = solveAndApply(armorSet(1, 2, 2, 1), Map.of(), 0, TOUGH_MAX, false);
        assertEquals(0.0, v, 0.0);
    }

    @Test
    void 덧셈_기본곱셈_최종곱셈이_섞여도_목표와_일치하고_넘지_않는다() {
        Random r = new Random(20261117L);
        Op[] ops = Op.values();
        double[] amounts = {2, -1, 0.5, -0.3, 1.0, 3, 0.25, 0.1, 0.7, -0.05, 1.3};
        int needed = 0;
        for (int t = 0; t < 100_000; t++) {
            List<Mod> mods = new ArrayList<>();
            Map<String, Double> rep = new HashMap<>();
            double[] slot = {3, 8, 6, 3};
            for (int i = 0; i < 4; i++) {
                if (r.nextInt(5) == 0) continue;
                mods.add(new Mod("s" + i, Op.ADD_VALUE, slot[i] + r.nextInt(3) * 0.5));
                if (r.nextBoolean()) rep.put("s" + i, (double) r.nextInt(9));
            }
            int n = r.nextInt(4);
            for (int j = 0; j < n; j++) mods.add(new Mod("o" + j, ops[r.nextInt(3)], amounts[r.nextInt(amounts.length)]));
            boolean floor = r.nextBoolean();
            double max = floor ? ARMOR_MAX : TOUGH_MAX;
            double cap = new double[]{12, 15, 18, 19, 20, 0, 2, 4, 6, 12}[r.nextInt(10)];

            Plan p = ArmorCapMath.plan(0, mods, rep, cap, ARMOR_MIN, max);
            if (!p.needed()) continue;
            needed++;
            // 실제 게임의 합산 순서가 다를 수 있으므로 보정 수정자 위치와 순서를 섞어서 평가 (같은 k에는 같은 값)
            int pos = r.nextInt(mods.size() + 1);
            long seed = r.nextLong();
            List<Mod> base = new ArrayList<>(mods);
            ArmorCapMath.Evaluator ev = k -> {
                List<Mod> all = new ArrayList<>(base);
                all.add(pos, new Mod("fantasyrpg:grade_limit", Op.ADD_MULTIPLIED_TOTAL, k));
                Collections.shuffle(all, new Random(seed));
                return ArmorCapMath.clamp(ArmorCapMath.calc(0, all), ARMOR_MIN, max);
            };
            double v = ev.valueWith(ArmorCapMath.refine(p.k(), p.target(), floor, ev));
            assertTrue(ArmorCapMath.accepted(v, p.target(), floor), () -> "목표 " + p.target() + " 결과 " + v + " " + mods);
            if (floor) assertTrue(Math.floor(v) <= Math.min(cap, max), "정수 방어력이 상한을 넘음");
            else assertTrue(v <= Math.min(cap, max) + 1e-9, "강도가 상한을 넘음");

            // 자기 보정 제외: 같은 입력이면 같은 계획 (보정 결과에 다시 계산해도 바뀌지 않음)
            Plan again = ArmorCapMath.plan(0, mods, rep, cap, ARMOR_MIN, max);
            assertEquals(p, again);
        }
        assertTrue(needed > 10_000, "보정이 필요한 경우가 충분히 시험되지 않음: " + needed);
    }
}
