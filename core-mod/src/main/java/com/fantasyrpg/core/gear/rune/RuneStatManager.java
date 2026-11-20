package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.FantasyRpgCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.EnumMap;
import java.util.Map;

/**
 * {@code data/<ns>/runes/*.json}을 읽어 룬석 효과를 보관한다. /reload로 갱신된다.
 * 기본값은 모드 jar 안에 있고, 데이터팩의 같은 경로 파일이 덮어쓴다. '_'로 시작하는 파일은 무시.
 * 혼자 하기(내장 서버)에서는 클라이언트 툴팁도 같은 값을 쓰고, 전용 서버 클라이언트는 DataSync 로 받는다 (2026-11-21).
 */
public final class RuneStatManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "runes";
    public static final RuneStatManager INSTANCE = new RuneStatManager();

    private volatile Map<RuneType, RuneDefinition> defs = Map.of();

    private RuneStatManager() {
        super(new Gson(), DIRECTORY);
        com.fantasyrpg.core.network.DataSync.register(DIRECTORY, f -> apply(f, null, null));   // 전용 서버 클라이언트 툴팁
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        com.fantasyrpg.core.network.DataSync.remember(DIRECTORY, files);
        Map<RuneType, RuneDefinition> loaded = new EnumMap<>(RuneType.class);
        int errors = 0;
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            ResourceLocation file = e.getKey();
            String name = file.getPath().substring(file.getPath().lastIndexOf('/') + 1);
            if (name.startsWith("_")) continue;
            try {
                RuneDefinition d = RuneDefinitionParser.parse(e.getValue().getAsJsonObject());
                if (loaded.put(d.type(), d) != null) {
                    FantasyRpgCore.LOGGER.warn("[룬석] {}가 여러 파일에 있음, {} 기준 사용", d.type().id(), file);
                }
            } catch (RuntimeException ex) {
                errors++;
                FantasyRpgCore.LOGGER.error("[룬석] {} 읽기 실패: {}", file, ex.getMessage());
            }
        }
        for (RuneType t : RuneType.values()) {
            if (!loaded.containsKey(t)) FantasyRpgCore.LOGGER.error("[룬석] {}의 효과 데이터가 없음 — 끼워도 효과 없음", t.id());
        }
        defs = Map.copyOf(loaded);
        FantasyRpgCore.LOGGER.info("[룬석] 룬석 효과 {}개 로드, 오류 {}건", loaded.size(), errors);
    }

    /** 없으면 null */
    public RuneDefinition get(RuneType type) {
        return defs.get(type);
    }
}
