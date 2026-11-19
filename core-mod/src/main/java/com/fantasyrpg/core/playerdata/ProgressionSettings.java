package com.fantasyrpg.core.playerdata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 레벨·경험치 수치 (데이터 {@code data/fantasyrpg/progression/leveling.json}, {@code mob_xp.json}). Minecraft 비의존.
 * 기준: 프로젝트 문서 claude/플레이어성장_설계.md 4절 (수치는 [초안]).
 *
 * @param base Lv.1→2 필요 경험치
 * @param step 레벨마다 늘어나는 필요 경험치. need(L) = base + step × (L − 1)
 * @param maxLevel 최대 레벨 (60)
 * @param chapterClearXpPerGrade 메인 챕터 최초 완료 경험치 = 완료 뒤 등급 × 이 값
 * @param midStepXpPerGrade 장 퀘스트 중간 단계 하나를 끝낼 때 경험치 = 지금 등급 × 이 값 (claude/장별중간단계_초안.md 5절)
 * @param defaultHostileXp 적대 몬스터 기본 처치 경험치
 * @param entityXp 엔티티별 처치 경험치 (id → 값, 0이면 없음)
 */
public record ProgressionSettings(int base, int step, int maxLevel, int chapterClearXpPerGrade,
                                  int defaultHostileXp, Map<String, Integer> entityXp, int midStepXpPerGrade) {
    public static final ProgressionSettings DEFAULT = new ProgressionSettings(100, 20, 60, 400, 10, Map.of(), 50);

    public ProgressionSettings(int base, int step, int maxLevel, int chapterClearXpPerGrade, int defaultHostileXp, Map<String, Integer> entityXp) {
        this(base, step, maxLevel, chapterClearXpPerGrade, defaultHostileXp, entityXp, 50);
    }

    public ProgressionSettings {
        entityXp = Map.copyOf(entityXp);
        if (base <= 0 || step < 0 || maxLevel < 2) {
            throw new IllegalArgumentException("곡선 값이 잘못됨: base " + base + ", step " + step + ", max_level " + maxLevel);
        }
        if (chapterClearXpPerGrade < 0 || defaultHostileXp < 0 || midStepXpPerGrade < 0) throw new IllegalArgumentException("경험치는 0 이상");
    }

    /** 레벨 L에서 L+1로 가는 데 필요한 경험치. 최대 레벨이면 0 */
    public int need(int level) {
        if (level >= maxLevel) return 0;
        return base + step * (Math.max(1, level) - 1);
    }

    /** Lv.1에서 해당 레벨까지 필요한 합계 */
    public long totalTo(int level) {
        long sum = 0;
        for (int l = 1; l < Math.min(level, maxLevel); l++) sum += need(l);
        return sum;
    }

    /** 엔티티 처치 경험치. 목록에 있으면 그 값, 아니면 적대 몬스터면 기본값, 그 외 0 */
    public int killXp(String entityId, boolean hostile) {
        Integer v = entityXp.get(entityId);
        if (v != null) return v;
        return hostile ? defaultHostileXp : 0;
    }

    public ProgressionSettings withLeveling(JsonObject json) {
        JsonObject curve = json.has("curve") ? json.getAsJsonObject("curve") : new JsonObject();
        return new ProgressionSettings(
                curve.has("base") ? curve.get("base").getAsInt() : base,
                curve.has("step") ? curve.get("step").getAsInt() : step,
                json.has("max_level") ? json.get("max_level").getAsInt() : maxLevel,
                json.has("chapter_clear_xp_per_grade") ? json.get("chapter_clear_xp_per_grade").getAsInt() : chapterClearXpPerGrade,
                defaultHostileXp, entityXp,
                json.has("mid_step_xp_per_grade") ? json.get("mid_step_xp_per_grade").getAsInt() : midStepXpPerGrade);
    }

    public ProgressionSettings withMobXp(JsonObject json) {
        Map<String, Integer> map = new HashMap<>();
        if (json.has("entities")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("entities").entrySet()) {
                int v = e.getValue().getAsInt();
                if (v < 0) throw new IllegalArgumentException(e.getKey() + " 경험치가 음수");
                map.put(e.getKey(), v);
            }
        }
        return new ProgressionSettings(base, step, maxLevel, chapterClearXpPerGrade,
                json.has("default_hostile") ? json.get("default_hostile").getAsInt() : defaultHostileXp, map, midStepXpPerGrade);
    }
}
