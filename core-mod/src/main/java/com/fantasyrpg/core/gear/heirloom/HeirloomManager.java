package com.fantasyrpg.core.gear.heirloom;

import com.fantasyrpg.core.FantasyRpgCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** {@code data/fantasyrpg/heirloom/settings.json} 로더. 실패하면 이전 값 유지, 처음부터 없으면 null */
public final class HeirloomManager extends SimpleJsonResourceReloadListener {
    public static final HeirloomManager INSTANCE = new HeirloomManager();
    private volatile HeirloomSettings settings;

    private HeirloomManager() {
        super(new Gson(), "heirloom");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        JsonElement json = files.get(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "settings"));
        if (json == null) {
            FantasyRpgCore.LOGGER.error("[유산] heirloom/settings.json 없음 — 강화·특성이 꺼짐");
            return;
        }
        try {
            HeirloomSettings s = HeirloomSettings.parse(json.getAsJsonObject());
            settings = s;
            FantasyRpgCore.LOGGER.info("[유산] 설정 로드: 특성 {}방향, 강화 비용 {}단계", s.traits().size(), s.enhance().size());
        } catch (RuntimeException ex) {
            FantasyRpgCore.LOGGER.error("[유산] settings.json 읽기 실패, 이전 값 유지: {}", ex.getMessage());
        }
    }

    public HeirloomSettings settings() {
        return settings;
    }
}
