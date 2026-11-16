package com.fantasyrpg.core.combat;

import java.util.List;
import java.util.Locale;

/**
 * 피해 계산 결과와 단계별 중간값. /rpg simulate 출력과 테스트에 사용한다.
 *
 * @param physicalBase    W × Cp × (1+Sp) — 저항 적용 전 물리 성분
 * @param magicBase       W × Cm × (1+Sm) — 저항 적용 전 마법 성분
 * @param effectivePhysicalResist Reff,p
 * @param effectiveMagicResist    Reff,m
 * @param physicalDamage  Dp
 * @param magicDamage     Dm
 * @param critMultiplier  Kcrit
 * @param finalModifier   F
 * @param total           D = (Dp + Dm) × Kcrit × F
 */
public record DamageBreakdown(
        double physicalBase,
        double magicBase,
        double effectivePhysicalResist,
        double effectiveMagicResist,
        double physicalDamage,
        double magicDamage,
        double critMultiplier,
        double finalModifier,
        double total
) {
    /** 사람이 읽을 수 있는 단계별 요약 (계산 순서 그대로). */
    public List<String> describe() {
        return List.of(
                fmt("① 기초  물리 %.2f / 마법 %.2f", physicalBase, magicBase),
                fmt("② 실효 저항  물리 %.0f%% / 마법 %.0f%%",
                        effectivePhysicalResist * 100, effectiveMagicResist * 100),
                fmt("③ 저항 후  Dp %.2f + Dm %.2f = %.2f",
                        physicalDamage, magicDamage, physicalDamage + magicDamage),
                fmt("④ 치명타 ×%.2f, 최종 보정 ×%.2f", critMultiplier, finalModifier),
                fmt("⑤ 최종 피해 D = %.2f", total)
        );
    }

    private static String fmt(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }
}
