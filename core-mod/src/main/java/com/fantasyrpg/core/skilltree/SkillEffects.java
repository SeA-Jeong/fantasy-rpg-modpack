package com.fantasyrpg.core.skilltree;

import com.fantasyrpg.core.skills.Cast;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스킬 효과 구현 등록소. 노드 데이터의 {@code effect} id → 실제 동작.
 * 전사·마법사 1~2단은 {@code skills/WarriorSkills}, {@code skills/MageSkills}가 등록한다. 등록되지 않은 효과의 스킬은 "아직 효과 없음" 안내만 한다.
 */
public final class SkillEffects {
    private SkillEffects() {
    }

    @FunctionalInterface
    public interface Effect {
        /**
         * 서버에서 실행. 쿨타임·코어 비용은 {@link SkillService}가 처리한다(성공했을 때만).
         * @return 실제로 썼으면 true (대상이 없어 못 쓰면 false — 비용·쿨타임 없음)
         */
        boolean run(Cast cast);
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
