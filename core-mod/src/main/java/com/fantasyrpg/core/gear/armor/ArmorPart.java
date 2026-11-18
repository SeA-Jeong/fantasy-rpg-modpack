package com.fantasyrpg.core.gear.armor;

/**
 * 방어구 부위와 배분 비율 (투구 3 : 흉갑 8 : 각반 6 : 장화 3, 합 20).
 * 기준: 프로젝트 문서 claude/장비등급제한_설계.md v3 1-3. Minecraft 비의존.
 */
public enum ArmorPart {
    HEAD("helmet", 3, 2),
    CHEST("chestplate", 8, 0),
    LEGS("leggings", 6, 1),
    FEET("boots", 3, 3);

    /** 비율 합계 */
    public static final int SHARE_TOTAL = 20;

    private final String itemSuffix;
    private final int share;
    /** 나머지 배분 동점일 때 순서 (흉갑 → 각반 → 투구 → 장화) */
    private final int tiePriority;

    ArmorPart(String itemSuffix, int share, int tiePriority) {
        this.itemSuffix = itemSuffix;
        this.share = share;
        this.tiePriority = tiePriority;
    }

    /** 아이템 이름 뒷부분 (예: knight_helmet 의 helmet) */
    public String itemSuffix() {
        return itemSuffix;
    }

    public int share() {
        return share;
    }

    int tiePriority() {
        return tiePriority;
    }
}
