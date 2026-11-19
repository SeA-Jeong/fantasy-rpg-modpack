package com.fantasyrpg.core.combat;

import com.fantasyrpg.core.FantasyRpgCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * {@code data/fantasyrpg/combat/basic_attack.json}을 읽는다. /reload로 갱신. 기본값은 모드 jar 안.
 * 없거나 읽기에 실패하면 이전 값을 쓰고, 처음부터 없으면 {@link #settings()}가 null이다(직업 공격은 바닐라로 빠짐).
 */
public final class CombatManager extends SimpleJsonResourceReloadListener {
    public static final CombatManager INSTANCE = new CombatManager();

    private volatile CombatSettings settings;
    private volatile GuardSettings guard;

    private CombatManager() {
        super(new Gson(), "combat");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        JsonElement g = files.get(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "guard"));
        if (g == null) {
            FantasyRpgCore.LOGGER.error("[전투] combat/guard.json 없음 — 기사 기본 가드가 꺼짐");
        } else {
            try {
                GuardSettings gs = GuardSettings.parse(g.getAsJsonObject());
                guard = gs;
                FantasyRpgCore.LOGGER.info("[전투] 기본 가드 로드: 감소 {}%, 패리 {}틱, 재무장 {}틱",
                        Math.round(gs.reduction() * 100), gs.parryTicks(), gs.rearmTicks());
            } catch (RuntimeException ex) {
                FantasyRpgCore.LOGGER.error("[전투] guard.json 읽기 실패, 이전 값 유지: {}", ex.getMessage());
            }
        }
        JsonElement json = files.get(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "basic_attack"));
        if (json == null) {
            FantasyRpgCore.LOGGER.error("[전투] combat/basic_attack.json 없음 — 직업 기본 공격이 바닐라로 처리됨");
            return;
        }
        try {
            CombatSettings s = CombatSettings.parse(json.getAsJsonObject());
            settings = s;
            FantasyRpgCore.LOGGER.info("[전투] 기본 공격 로드: W 1급 {} ~ 10급 {}, 치명타 {}% ×{}, 직업 {}개",
                    s.power(1), s.power(10), Math.round(s.critChance() * 100), s.critMultiplier(), s.classes().size());
        } catch (RuntimeException ex) {
            FantasyRpgCore.LOGGER.error("[전투] basic_attack.json 읽기 실패, 이전 값 유지: {}", ex.getMessage());
        }
    }

    /** 기사 기본 가드 수치, 없으면 null */
    public GuardSettings guard() {
        return guard;
    }

    /** 없으면 null */
    public CombatSettings settings() {
        return settings;
    }
}
