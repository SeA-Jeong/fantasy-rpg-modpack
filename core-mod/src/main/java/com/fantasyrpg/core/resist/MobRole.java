package com.fantasyrpg.core.resist;

import java.util.Locale;

/** 몬스터 역할. 필수 챕터 보스만 저항 차이 상한(5.1장)을 적용받는다. */
public enum MobRole {
    NORMAL,
    ELITE,
    FIELD_BOSS,
    CHAPTER_BOSS;

    public static MobRole parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        return MobRole.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
