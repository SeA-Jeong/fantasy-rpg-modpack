package com.fantasyrpg.core.combat;

import com.google.gson.JsonObject;

/**
 * 기사 기본 가드·패리 수치 ({@code data/fantasyrpg/combat/guard.json}) 와 판정 규칙. Minecraft 비의존.
 * 기준: 기획 v0.4 5.3 (2026-11-19 결정), 프로젝트 문서 claude/가드_프리셋_초안.md 1절.
 *
 * @param reduction 정면 피해 감소율 (0~0.95)
 * @param frontAngle 정면으로 보는 각도 (전체 각, 1~360)
 * @param parryTicks 들어 올린 뒤 패리가 되는 틱 수 (기획 5.3: 5~8 범위에서 조정)
 * @param rearmTicks 직전에 들어 올린 때부터 이만큼 지나야 다시 패리가 됨 (연타 방지)
 * @param parryCore 패리 때 얻는 결의
 * @param parryStunTicks 패리 때 일반 몬스터 경직 (보스는 제어 면역)
 */
public record GuardSettings(double reduction, double frontAngle, int parryTicks, int rearmTicks, double parryCore,
                            int parryStunTicks) {

    public GuardSettings {
        if (reduction < 0 || reduction > 0.95) throw new IllegalArgumentException("reduction 은 0~0.95");
        if (frontAngle <= 0 || frontAngle > 360) throw new IllegalArgumentException("front_angle 은 1~360");
        if (parryTicks < 0 || parryTicks > 20) throw new IllegalArgumentException("parry_ticks 는 0~20");
        if (rearmTicks < 0) throw new IllegalArgumentException("rearm_ticks 는 0 이상");
        if (parryCore < 0) throw new IllegalArgumentException("parry_core 는 0 이상");
        if (parryStunTicks < 0) throw new IllegalArgumentException("parry_stun_ticks 는 0 이상");
    }

    public static GuardSettings parse(JsonObject o) {
        return new GuardSettings(
                o.get("reduction").getAsDouble(),
                o.has("front_angle") ? o.get("front_angle").getAsDouble() : 120,
                o.get("parry_ticks").getAsInt(),
                o.has("rearm_ticks") ? o.get("rearm_ticks").getAsInt() : 20,
                o.has("parry_core") ? o.get("parry_core").getAsDouble() : 0,
                o.has("parry_stun_ticks") ? o.get("parry_stun_ticks").getAsInt() : 0);
    }

    /**
     * 이번 피격이 패리인지.
     * @param now 지금 틱
     * @param raisedAt 이번에 들어 올린 틱
     * @param previousRaisedAt 바로 전에 들어 올렸던 틱 (처음이면 아주 작은 값)
     */
    public boolean isParry(long now, long raisedAt, long previousRaisedAt) {
        if (now - raisedAt > parryTicks) return false;
        return raisedAt - previousRaisedAt >= rearmTicks;
    }

    /** 정면이면 줄인 피해, 아니면 그대로 */
    public double guarded(double amount) {
        return amount * (1.0 - reduction);
    }
}
