package com.fantasyrpg.core.playerdata;

import java.util.Locale;

/**
 * 직업 6종 (기획 v0.4 2장, 전직 없음, 주력 무기 고정). Minecraft 비의존.
 */
public enum RpgClass {
    WARRIOR,
    KNIGHT,
    ROGUE,
    ARCHER,
    MAGE,
    CLERIC;

    /** warrior, knight, rogue, archer, mage, cleric */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 없으면 null (빈 문자열 = 무직) */
    public static RpgClass byId(String id) {
        if (id == null) return null;
        for (RpgClass c : values()) {
            if (c.id().equals(id)) return c;
        }
        return null;
    }
}
