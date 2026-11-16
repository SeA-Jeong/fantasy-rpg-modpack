package com.fantasyrpg.core.resist;

import com.fantasyrpg.core.combat.Resistances;
import com.google.gson.JsonObject;

/**
 * mob_resistances JSON → {@link MobResistanceProfile}. Minecraft 비의존이라 단위 테스트 가능.
 *
 * <pre>
 * {
 *   "entity": "fantasyrpg:corrupted_spirit_lord",   // 또는 "default"
 *   "chapter": 7,
 *   "role": "chapter_boss",
 *   "resist": { "physical": 0.25, "magic": 0.15 }
 * }
 * </pre>
 */
public final class MobResistanceParser {
    private MobResistanceParser() {
    }

    public static MobResistanceProfile parse(JsonObject json) {
        if (!json.has("entity")) {
            throw new IllegalArgumentException("'entity' 필드가 없습니다");
        }
        String entity = json.get("entity").getAsString().trim();
        MobRole role = MobRole.parse(json.has("role") ? json.get("role").getAsString() : null);
        int chapter = json.has("chapter") ? json.get("chapter").getAsInt() : 0;

        double physical = 0;
        double magic = 0;
        if (json.has("resist")) {
            JsonObject r = json.getAsJsonObject("resist");
            physical = r.has("physical") ? r.get("physical").getAsDouble() : 0;
            magic = r.has("magic") ? r.get("magic").getAsDouble() : 0;
        }
        return new MobResistanceProfile(entity, role, chapter, new Resistances(physical, magic));
    }
}
