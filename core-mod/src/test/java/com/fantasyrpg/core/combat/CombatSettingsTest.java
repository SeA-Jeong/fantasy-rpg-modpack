package com.fantasyrpg.core.combat;

import com.fantasyrpg.core.playerdata.RpgClass;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 기본 공격 수치 (claude/전투연결_설계.md 3절). 데이터는 모드 jar 기본값 combat/basic_attack.json.
 */
class CombatSettingsTest {
    private static CombatSettings load() throws IOException {
        String text = Files.readString(Path.of("src/main/resources/data/fantasyrpg/combat/basic_attack.json"), StandardCharsets.UTF_8);
        return CombatSettings.parse(JsonParser.parseString(text).getAsJsonObject());
    }

    @Test
    @DisplayName("데이터: W 1급 6 ~ 10급 30, 치명타 5%·150%(확정), 맨손 1, 6직업 모두 있음")
    void data() throws IOException {
        CombatSettings s = load();
        assertEquals(6, s.power(1));
        assertEquals(30, s.power(10));
        assertEquals(30, s.power(99));
        assertEquals(0.05, s.critChance());
        assertEquals(1.5, s.critMultiplier());
        assertEquals(1.0, s.unarmedDamage());
        for (RpgClass c : RpgClass.values()) assertNotNull(s.forClass(c.id()), c.id());
        assertNotNull(s.forClass("mage").bolt());
        assertEquals(16, s.forClass("mage").bolt().cooldownTicks());
    }

    @Test
    @DisplayName("직업 간 완충 초당 피해가 평균의 ±15% 안 (기획 v0.4 화력 격차 목표, 마법사 근접은 제외)")
    void dpsBalance() throws IOException {
        CombatSettings s = load();
        double sum = 0;
        for (RpgClass c : RpgClass.values()) sum += s.forClass(c.id()).dpsPerW();
        double mean = sum / RpgClass.values().length;
        for (RpgClass c : RpgClass.values()) {
            double d = s.forClass(c.id()).dpsPerW();
            assertTrue(Math.abs(d - mean) / mean <= 0.15, c.id() + " " + d + " vs 평균 " + mean);
        }
        // 1급 전사 = 6 × 0.85 × 1.6 = 8.16
        assertEquals(8.16, s.power(1) * s.forClass("warrior").dpsPerW(), 1e-9);
    }

    @Test
    @DisplayName("기본 공격 계수: 완충·콤보 1 → 그대로, 충전 0 → 20%, 치명타·저항은 계산기 순서대로")
    void basicDamage() throws IOException {
        CombatSettings s = load();
        double k = BasicAttackMath.basicCoefficient(s.forClass("warrior").physical(), 1.0, 1.0);
        assertEquals(0.85, k, 1e-12);
        assertEquals(0.85 * 0.2, BasicAttackMath.basicCoefficient(0.85, 1.0, 0.0), 1e-12);
        DamageInput in = DamageInput.of(s.power(1), k, 0).withCrit(s.critMultiplier());
        assertEquals(6 * 0.85 * 1.5, DamageCalculator.damage(in, Resistances.NONE), 1e-9);
        // 물리 저항 25% 좀비: 6 × 0.85 × 0.75
        assertEquals(6 * 0.85 * 0.75, DamageCalculator.damage(DamageInput.of(6, 0.85, 0), new Resistances(0.25, 0)), 1e-9);
    }
}
