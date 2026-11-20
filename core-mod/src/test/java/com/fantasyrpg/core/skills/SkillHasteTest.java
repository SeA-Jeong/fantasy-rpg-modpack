package com.fantasyrpg.core.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 스킬 가속 (2026-11-21, claude/직업룬석_초안.md 2절) */
class SkillHasteTest {
    @Test
    @DisplayName("재사용 대기 = 기본 × 100 ÷ (100 + 가속): 0 → 1배, 25 → 0.8배, 100 → 0.5배, 상한 없음")
    void mult() {
        assertEquals(1.0, SkillMath.hasteMult(0), 1e-12);
        assertEquals(0.8, SkillMath.hasteMult(25), 1e-12);
        assertEquals(0.5, SkillMath.hasteMult(100), 1e-12);
        assertEquals(0.25, SkillMath.hasteMult(300), 1e-12);
        assertEquals(1.0, SkillMath.hasteMult(-10), 1e-12);
    }

    @Test
    @DisplayName("예전 % 감소와 같은 가속: 8% → 약 8.7, 15% → 약 17.6 (데이터는 반올림 9·18)")
    void fromRate() {
        assertEquals(8.7, SkillMath.hasteFromRate(0.08), 0.01);
        assertEquals(17.65, SkillMath.hasteFromRate(0.15), 0.01);
        assertEquals(0.0, SkillMath.hasteFromRate(0), 1e-12);
    }
}
