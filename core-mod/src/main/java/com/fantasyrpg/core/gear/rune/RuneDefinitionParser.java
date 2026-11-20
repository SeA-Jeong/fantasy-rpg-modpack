package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.gear.armor.ArmorPart;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * runes JSON → {@link RuneDefinition}. 검사에 걸리면 {@link IllegalArgumentException}. Minecraft 비의존.
 *
 * <pre>
 * {
 *   "rune": "life",
 *   "attribute": "minecraft:generic.max_health",
 *   "operation": "add_value",
 *   "parts": ["helmet", "chestplate"],
 *   "values": [1, 2, 3, 4],
 *   "light_bonus": 0
 * }
 * </pre>
 * 검사: 아는 룬석 종류, 연산 3종 중 하나, 부위 1개 이상(helmet·chestplate·leggings·boots),
 * 값 4개·유한·0 이상·등급이 오를 때 줄지 않음, light_bonus 0 이상.
 */
public final class RuneDefinitionParser {
    private RuneDefinitionParser() {
    }

    public static RuneDefinition parse(JsonObject json) {
        RuneType type = RuneType.byId(require(json, "rune").getAsString().trim());
        if (type == null) throw new IllegalArgumentException("모르는 룬석 종류: " + json.get("rune"));
        String attribute = require(json, "attribute").getAsString().trim();
        if (!attribute.contains(":")) throw new IllegalArgumentException("능력치 id에 이름공간이 없습니다: " + attribute);
        String operation = require(json, "operation").getAsString().trim().toLowerCase(Locale.ROOT);
        if (!RuneDefinition.OPERATIONS.contains(operation)) {
            throw new IllegalArgumentException("연산은 " + RuneDefinition.OPERATIONS + " 중 하나: " + operation);
        }

        Set<ArmorPart> parts = EnumSet.noneOf(ArmorPart.class);
        for (JsonElement e : require(json, "parts").getAsJsonArray()) {
            ArmorPart part = partOf(e.getAsString().trim());
            if (part == null) throw new IllegalArgumentException("모르는 부위: " + e.getAsString());
            parts.add(part);
        }
        if (parts.isEmpty()) throw new IllegalArgumentException("부위가 없습니다");

        JsonArray arr = require(json, "values").getAsJsonArray();
        if (arr.size() != RuneDefinition.MAX_GRADE) {
            throw new IllegalArgumentException("값은 하급·중급·상급·최고급 4개여야 합니다 (" + arr.size() + "개)");
        }
        double[] values = new double[RuneDefinition.MAX_GRADE];
        for (int i = 0; i < values.length; i++) {
            values[i] = arr.get(i).getAsDouble();
            if (!Double.isFinite(values[i]) || values[i] < 0) {
                throw new IllegalArgumentException((i + 1) + "번째 값 " + values[i] + "은(는) 0 이상이어야 합니다");
            }
            if (i > 0 && values[i] < values[i - 1]) {
                throw new IllegalArgumentException("등급이 오를 때 값이 줄어듭니다: " + values[i - 1] + " → " + values[i]);
            }
        }
        double lightBonus = json.has("light_bonus") ? json.get("light_bonus").getAsDouble() : 0.0;
        if (!(lightBonus >= 0) || !Double.isFinite(lightBonus)) {
            throw new IllegalArgumentException("light_bonus는 0 이상: " + lightBonus);
        }
        boolean lightOnly = json.has("light_only") && json.get("light_only").getAsBoolean();
        return new RuneDefinition(type, attribute, operation, parts, values, lightBonus, lightOnly);
    }

    private static ArmorPart partOf(String suffix) {
        for (ArmorPart p : ArmorPart.values()) {
            if (p.itemSuffix().equals(suffix)) return p;
        }
        return null;
    }

    private static JsonElement require(JsonObject o, String key) {
        if (!o.has(key)) throw new IllegalArgumentException("'" + key + "' 필드가 없습니다");
        return o.get(key);
    }
}
