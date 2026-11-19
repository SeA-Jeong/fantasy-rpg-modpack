package com.fantasyrpg.core.skilltree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 장착 프리셋 규칙 (기획 v0.4 7.1, 2026-11-19 결정: 1차는 장착칸 + 활성 마스터리만). Minecraft 비의존.
 * 프리셋은 노드 id 만 저장하고, 적용할 때 지금 배운 노드·칸 종류를 다시 확인한다(SP 배분은 바꾸지 않음).
 */
public final class PresetRules {
    private PresetRules() {}

    public static final int COUNT = 3;
    public static final int SLOT_COUNT = 5;
    public static final int ULTIMATE_SLOT = 4;

    /** 레벨로 쓸 수 있는 프리셋 수. unlockLevels[i] = i번째 프리셋을 여는 레벨 (오름차순) */
    public static int unlocked(int level, List<Integer> unlockLevels) {
        int n = 0;
        for (int i = 0; i < Math.min(COUNT, unlockLevels.size()); i++) if (level >= unlockLevels.get(i)) n++;
        return n;
    }

    /** 적용 결과: 새 장착칸과 빠진(배우지 않았거나 칸이 안 맞는) 노드 수 */
    public record Applied(List<String> slots, int dropped) {}

    /**
     * 프리셋 장착칸을 지금 상태에 맞춰 고른다. 배우지 않은 노드, 칸 종류가 안 맞는 노드(궁극기는 4번 칸만),
     * 같은 노드가 두 칸에 있으면 뒤쪽은 비운다.
     */
    public static Applied apply(List<String> presetSlots, Map<String, Integer> ranks, Map<String, SkillNode> nodes) {
        List<String> out = new ArrayList<>();
        int dropped = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            String id = i < presetSlots.size() ? presetSlots.get(i) : "";
            if (id.isEmpty()) {
                out.add("");
                continue;
            }
            SkillNode n = nodes.get(id);
            boolean ok = n != null && ranks.getOrDefault(id, 0) > 0 && n.kind().equippable()
                    && (n.kind() == SkillNode.Kind.ULTIMATE) == (i == ULTIMATE_SLOT) && !out.contains(id);
            out.add(ok ? id : "");
            if (!ok) dropped++;
        }
        return new Applied(List.copyOf(out), dropped);
    }

    /** 프리셋의 마스터리가 지금 바꿀 대상인지: 비어 있지 않고, 배운 마스터리이고, 지금 활성과 다름 */
    public static boolean masteryChanges(String presetMastery, String activeMastery, Map<String, Integer> ranks, Map<String, SkillNode> nodes) {
        if (presetMastery.isEmpty() || presetMastery.equals(activeMastery)) return false;
        SkillNode n = nodes.get(presetMastery);
        return n != null && n.kind() == SkillNode.Kind.MASTERY && ranks.getOrDefault(presetMastery, 0) > 0;
    }
}
