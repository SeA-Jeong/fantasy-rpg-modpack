package com.fantasyrpg.core.gear.enchant;

import java.util.List;
import java.util.Set;

/**
 * 바닐라 인챈트 정리 규칙 (순수 계산, 정책 P2·P2-1, claude/바닐라인챈트_정리_초안.md — 2026-11-21 사용자 결정 A·B·C 추천안).
 * <ul>
 *   <li>전투 인챈트 22종: 효과 없음(데이터) + 획득 태그에서 제외(데이터) + 플레이어 아이템에서 지움(코드)</li>
 *   <li>보호 최대 III·레벨당 3%, 화염·폭발·발사체 보호 최대 II·레벨당 6%. 최대치를 넘는 기존 장비는 최대치만큼만</li>
 * </ul>
 * 바닐라 보호 계산: 보호 점수(damage_protection) 1 = 4%, 합계 20(= 80%)이 상한.
 */
public final class EnchantRules {
    private EnchantRules() {}

    /** RPG 전투에서 빼는 바닐라 전투 인챈트 (경로 이름, minecraft: 생략) */
    public static final List<String> DISABLED = List.of(
            "sharpness", "smite", "bane_of_arthropods", "fire_aspect", "knockback", "sweeping_edge", "looting",
            "power", "punch", "flame", "infinity", "multishot", "piercing", "quick_charge",
            "impaling", "riptide", "loyalty", "channeling", "density", "breach", "wind_burst", "thorns");

    /** 그대로 두는 비전투 인챈트 (획득 태그에 남김) */
    public static final Set<String> KEPT = Set.of(
            "protection", "fire_protection", "blast_protection", "projectile_protection", "feather_falling",
            "respiration", "aqua_affinity", "depth_strider", "efficiency", "silk_touch", "unbreaking", "fortune",
            "luck_of_the_sea", "lure", "mending", "frost_walker", "soul_speed", "swift_sneak", "binding_curse", "vanishing_curse");

    public static final double POINT_PERCENT = 4.0, VANILLA_CAP_POINTS = 20.0;

    /** 보호 점수 (상한 적용): min(레벨, 최대) × 레벨당 점수 */
    public static double points(int level, double perLevel, int maxLevel) {
        return Math.max(0, Math.min(level, maxLevel)) * perLevel;
    }

    /** 보호 점수 → 감소 % (바닐라 상한 80%) */
    public static double percent(double points) {
        return Math.min(points, VANILLA_CAP_POINTS) * POINT_PERCENT;
    }

    public static boolean disabled(String path) {
        return DISABLED.contains(path);
    }
}
