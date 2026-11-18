package com.fantasyrpg.core.gear.rune;

import java.util.Locale;

/**
 * 공통 룬석 6종 (프로젝트 문서 claude/계열방어구_상위옵션_초안.md 3-3). Minecraft 비의존.
 * 효과 수치·끼울 수 있는 부위는 데이터 {@code data/fantasyrpg/runes/<id>.json}에 있다.
 * 직업 룬석(순환·가속)은 직업 시스템이 생긴 뒤에 추가한다.
 *
 * <p>{@code placeholderTexture}: 전용 그림 전까지 빌려 쓰는 바닐라 아이템 그림.
 */
public enum RuneType {
    LIFE("red_dye"),
    GUARD("prismarine_crystals"),
    STEADY("iron_nugget"),
    GALE("feather"),
    LEAP("rabbit_foot"),
    CLIMB("amethyst_shard");

    private final String placeholderTexture;

    RuneType(String placeholderTexture) {
        this.placeholderTexture = placeholderTexture;
    }

    /** life, guard, steady, gale, leap, climb */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 아이템 이름 (rune_life 등) */
    public String itemName() {
        return "rune_" + id();
    }

    public String placeholderTexture() {
        return placeholderTexture;
    }

    public static RuneType byId(String id) {
        for (RuneType t : values()) {
            if (t.id().equals(id)) return t;
        }
        return null;
    }
}
