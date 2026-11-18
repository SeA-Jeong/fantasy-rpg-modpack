package com.fantasyrpg.core.skilltree;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스킬 효과 구현 등록소. 노드 데이터의 {@code effect} id → 실제 동작.
 * 이번 틀에서는 비어 있다(스킬 내용은 다음 단계). 등록되지 않은 효과의 스킬은 "아직 효과 없음" 안내만 한다.
 */
public final class SkillEffects {
    private SkillEffects() {
    }

    @FunctionalInterface
    public interface Effect {
        /** 서버에서 실행. 쿨타임·코어 소비는 효과가 직접 처리(다음 단계에서 공통화) */
        void run(ServerPlayer player, SkillNode node, int rank);
    }

    private static final Map<String, Effect> EFFECTS = new ConcurrentHashMap<>();

    public static void register(String id, Effect effect) {
        EFFECTS.put(id, effect);
    }

    /** 없으면 null */
    public static Effect get(String id) {
        return id == null || id.isEmpty() ? null : EFFECTS.get(id);
    }
}
