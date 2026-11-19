package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.FantasyRpgCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 스킬 공통 설정 {@code skill_settings/*.json}: 경직·느림·밀쳐내기가 듣지 않는 대상(보스).
 */
public final class SkillSettings extends SimpleJsonResourceReloadListener {
    public static final SkillSettings INSTANCE = new SkillSettings();

    private volatile List<TagKey<EntityType<?>>> immuneTypeTags = List.of();
    private volatile Set<String> immuneScoreboardTags = Set.of();
    private volatile double immuneMinHealth = 100;
    /** 장착 프리셋 1·2·3번째를 여는 레벨 (skill_settings/presets.json, [초안] 기획 7.1 기존안) */
    private volatile List<Integer> presetUnlockLevels = List.of(1, 20, 40);

    private SkillSettings() {
        super(new Gson(), "skill_settings");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<TagKey<EntityType<?>>> types = new java.util.ArrayList<>();
        Set<String> tags = new HashSet<>();
        double minHealth = 100;
        List<Integer> unlock = List.of(1, 20, 40);
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                if (o.has("presets")) {
                    List<Integer> lv = new java.util.ArrayList<>();
                    for (JsonElement t : o.getAsJsonObject("presets").getAsJsonArray("unlock_levels")) lv.add(t.getAsInt());
                    if (lv.size() != 3) throw new IllegalArgumentException("presets.unlock_levels 는 3개");
                    unlock = List.copyOf(lv);
                }
                if (!o.has("control_immune")) continue;
                JsonObject c = o.getAsJsonObject("control_immune");
                if (c.has("entity_tags")) for (JsonElement t : c.getAsJsonArray("entity_tags"))
                    types.add(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(t.getAsString())));
                if (c.has("scoreboard_tags")) for (JsonElement t : c.getAsJsonArray("scoreboard_tags")) tags.add(t.getAsString());
                if (c.has("min_max_health")) minHealth = c.get("min_max_health").getAsDouble();
            } catch (RuntimeException ex) {
                FantasyRpgCore.LOGGER.error("[스킬] 설정 {} 읽기 실패: {}", e.getKey(), ex.getMessage());
            }
        }
        immuneTypeTags = List.copyOf(types);
        immuneScoreboardTags = Set.copyOf(tags);
        immuneMinHealth = minHealth;
        presetUnlockLevels = unlock;
        FantasyRpgCore.LOGGER.info("[스킬] 제어 면역: 엔티티 태그 {}개, 스코어보드 태그 {}개, 최대 체력 {} 이상", types.size(), tags.size(), minHealth);
        FantasyRpgCore.LOGGER.info("[스킬] 장착 프리셋 해금 레벨 {}", unlock);
    }

    public List<Integer> presetUnlockLevels() {
        return presetUnlockLevels;
    }

    /** 경직·느림·밀쳐내기를 무시하는 대상(보스)인지 */
    public boolean controlImmune(LivingEntity e) {
        if (e.getMaxHealth() >= immuneMinHealth) return true;
        for (TagKey<EntityType<?>> t : immuneTypeTags) if (e.getType().is(t)) return true;
        for (String t : e.getTags()) if (immuneScoreboardTags.contains(t)) return true;
        return false;
    }
}
