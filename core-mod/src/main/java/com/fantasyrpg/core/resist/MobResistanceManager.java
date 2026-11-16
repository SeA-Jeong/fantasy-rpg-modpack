package com.fantasyrpg.core.resist;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.RpgConfig;
import com.fantasyrpg.core.combat.Resistances;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code data/<ns>/mob_resistances/*.json}을 읽어 엔티티별 저항을 보관한다. /reload로 갱신된다.
 * 파일 이름이 '_'로 시작하면 형식 견본으로 보고 무시한다.
 */
public final class MobResistanceManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "mob_resistances";
    public static final MobResistanceManager INSTANCE = new MobResistanceManager();

    private volatile Map<String, MobResistanceProfile> profiles = Map.of();
    private volatile Resistances fallback = Resistances.NONE;

    private MobResistanceManager() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, MobResistanceProfile> loaded = new HashMap<>();
        Resistances newFallback = Resistances.NONE;
        int errors = 0;

        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            ResourceLocation file = e.getKey();
            String name = file.getPath().substring(file.getPath().lastIndexOf('/') + 1);
            if (name.startsWith("_")) {
                continue;
            }
            try {
                MobResistanceProfile p = MobResistanceParser.parse(e.getValue().getAsJsonObject());
                if (MobResistanceProfile.DEFAULT_ENTITY.equals(p.entityId())) {
                    newFallback = p.resist();
                    continue;
                }
                if (loaded.put(p.entityId(), p) != null) {
                    FantasyRpgCore.LOGGER.warn("[저항] {} 엔티티가 여러 파일에 정의됨, {} 기준 사용", p.entityId(), file);
                }
                checkGap(file, p);
            } catch (RuntimeException ex) {
                errors++;
                FantasyRpgCore.LOGGER.error("[저항] {} 읽기 실패: {}", file, ex.getMessage());
            }
        }

        profiles = Map.copyOf(loaded);
        fallback = newFallback;
        FantasyRpgCore.LOGGER.info("[저항] {}개 엔티티 로드, 기본 저항 물리 {} / 마법 {}, 오류 {}건",
                loaded.size(), newFallback.physical(), newFallback.magic(), errors);
    }

    private static void checkGap(ResourceLocation file, MobResistanceProfile p) {
        double def = RpgConfig.BOSS_RESIST_GAP_DEFAULT.get();
        double max = RpgConfig.BOSS_RESIST_GAP_EXCEPTION.get();
        switch (ResistanceRules.checkBossGap(p.role(), p.resist(), def, max)) {
            case EXCEPTION_RANGE -> FantasyRpgCore.LOGGER.warn(
                    "[저항] 챕터 보스 {}의 저항 차이 {}%p가 기본 상한 {}%p 초과 (예외 범위) — {}",
                    p.entityId(), pct(p.resist().gap()), pct(def), file);
            case VIOLATION -> FantasyRpgCore.LOGGER.error(
                    "[저항] 챕터 보스 {}의 저항 차이 {}%p가 예외 상한 {}%p 초과 — 기획 5.1장 위반 — {}",
                    p.entityId(), pct(p.resist().gap()), pct(max), file);
            case OK -> {
            }
        }
    }

    private static long pct(double ratio) {
        return Math.round(ratio * 100);
    }

    /** 엔티티 종류의 저항. 데이터가 없으면 기본값 파일(entity: "default")의 값, 그것도 없으면 0/0. */
    public Resistances get(EntityType<?> type) {
        MobResistanceProfile p = profiles.get(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        return p != null ? p.resist() : fallback;
    }

    /** 데이터 파일이 있으면 프로필, 없으면 null. */
    public MobResistanceProfile profile(EntityType<?> type) {
        return profiles.get(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }
}
