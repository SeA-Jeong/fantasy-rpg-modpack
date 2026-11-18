package com.fantasyrpg.core.playerdata;

import com.fantasyrpg.core.FantasyRpgCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * {@code data/fantasyrpg/progression/leveling.json}·{@code mob_xp.json}을 읽는다. /reload로 갱신.
 * 기본값은 모드 jar 안에 있고 데이터팩의 같은 경로 파일이 덮어쓴다. 읽기에 실패하면 이전 값(처음엔 기본값)을 쓴다.
 */
public final class ProgressionManager extends SimpleJsonResourceReloadListener {
    public static final ProgressionManager INSTANCE = new ProgressionManager();

    private volatile ProgressionSettings settings = ProgressionSettings.DEFAULT;

    private ProgressionManager() {
        super(new Gson(), "progression");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        ProgressionSettings s = ProgressionSettings.DEFAULT;
        try {
            JsonElement leveling = files.get(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "leveling"));
            if (leveling != null) s = s.withLeveling(leveling.getAsJsonObject());
            JsonElement mobXp = files.get(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "mob_xp"));
            if (mobXp != null) s = s.withMobXp(mobXp.getAsJsonObject());
            settings = s;
            FantasyRpgCore.LOGGER.info("[성장] 레벨 곡선 need(L) = {} + {}×(L−1), 최대 Lv.{}, 챕터 완료 등급×{}, 적대 몬스터 {}, 엔티티별 {}개",
                    s.base(), s.step(), s.maxLevel(), s.chapterClearXpPerGrade(), s.defaultHostileXp(), s.entityXp().size());
        } catch (RuntimeException ex) {
            FantasyRpgCore.LOGGER.error("[성장] progression 데이터 읽기 실패, 이전 값 유지: {}", ex.getMessage());
        }
    }

    public ProgressionSettings settings() {
        return settings;
    }
}
