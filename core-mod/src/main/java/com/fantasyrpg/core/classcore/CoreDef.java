package com.fantasyrpg.core.classcore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 직업 코어 자원 정의 (기획 v0.4 3장, claude/스킬트리_설계.md 5절). Minecraft 비의존.
 *
 * <pre>
 * { "id": "fantasyrpg:momentum", "class": "warrior", "max": 100, "start": 0,
 *   "gain": { "basic_attack_hit": 8 }, "regen_per_second": 0,
 *   "decay": { "delay_ticks": 100, "per_second": 10 } }
 * </pre>
 */
public record CoreDef(String id, String classId, double max, double start, Map<String, Double> gain,
                      double regenPerSecond, int decayDelayTicks, double decayPerSecond) {

    public CoreDef {
        gain = Map.copyOf(gain);
    }

    public double gainFor(String event) {
        return gain.getOrDefault(event, 0.0);
    }

    /**
     * 한 틱 진행. 자동 회복을 더하고, 마지막으로 얻은 뒤 지연 틱이 지났으면 감소. 0~max로 자름.
     *
     * @param ticksSinceGain 마지막으로 사건으로 얻은 뒤 지난 틱
     */
    public double tick(double value, long ticksSinceGain) {
        double v = value + regenPerSecond / 20.0;
        if (decayPerSecond > 0 && ticksSinceGain >= decayDelayTicks) v -= decayPerSecond / 20.0;
        return clamp(v);
    }

    public double add(double value, double amount) {
        return clamp(value + amount);
    }

    public double clamp(double v) {
        return Math.max(0.0, Math.min(max, v));
    }

    public static CoreDef parse(JsonObject json) {
        String id = json.get("id").getAsString().trim();
        String cls = json.get("class").getAsString().trim();
        double max = json.get("max").getAsDouble();
        if (!(max > 0)) throw new IllegalArgumentException(id + ": max > 0");
        double start = json.has("start") ? json.get("start").getAsDouble() : 0;
        Map<String, Double> gain = new HashMap<>();
        if (json.has("gain")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("gain").entrySet()) gain.put(e.getKey(), e.getValue().getAsDouble());
        }
        double regen = json.has("regen_per_second") ? json.get("regen_per_second").getAsDouble() : 0;
        int delay = 0;
        double decay = 0;
        if (json.has("decay")) {
            JsonObject d = json.getAsJsonObject("decay");
            delay = d.has("delay_ticks") ? d.get("delay_ticks").getAsInt() : 0;
            decay = d.has("per_second") ? d.get("per_second").getAsDouble() : 0;
        }
        if (start < 0 || start > max || regen < 0 || decay < 0 || delay < 0) {
            throw new IllegalArgumentException(id + ": start 0~max, 회복·감소 0 이상");
        }
        return new CoreDef(id, cls, max, start, gain, regen, delay, decay);
    }
}
