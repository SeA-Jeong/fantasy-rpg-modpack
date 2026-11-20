package com.fantasyrpg.core.gear.armor;

import java.util.Locale;

/**
 * 계열 방어구 (claude/장비등급제한_설계.md v3). 심연은 2026-11-18 설계 보류라 없다. Minecraft 비의존.
 *
 * <p>{@code placeholderLook}: 전용 그림이 나오기 전까지 빌려 쓰는 바닐라 갑옷 그림 이름.
 */
public enum ArmorFamily {
    KNIGHT("iron"),
    DWARF("chainmail"),
    DRAGON("netherite"),
    ELF("golden"),
    ARCANE("diamond");

    private final String placeholderLook;

    ArmorFamily(String placeholderLook) {
        this.placeholderLook = placeholderLook;
    }

    /** 데이터·아이템 id에 쓰는 이름 (knight, dwarf, dragon, elf, arcane) */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 바닐라 아이템 그림 이름 (iron, chainmail, netherite, golden, diamond) */
    public String placeholderLook() {
        return placeholderLook;
    }

    /** 바닐라 갑옷 착용 그림 이름 (gold 는 golden 이 아니라 gold) */
    public String placeholderLayer() {
        return placeholderLook.equals("golden") ? "gold" : placeholderLook;
    }

    /** 경갑 (엘프·마도): 직업 룬석을 끼울 수 있음 */
    public boolean light() {
        return this == ELF || this == ARCANE;
    }

    public static ArmorFamily byId(String id) {
        for (ArmorFamily f : values()) {
            if (f.id().equals(id)) return f;
        }
        return null;
    }
}
