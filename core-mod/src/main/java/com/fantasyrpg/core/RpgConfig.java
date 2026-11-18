package com.fantasyrpg.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 공통 설정 (config/fantasyrpg-common.toml). 테스트로 확정할 값은 여기나 데이터팩에 둔다.
 */
public final class RpgConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue BOSS_RESIST_GAP_DEFAULT = BUILDER
            .comment("필수 챕터 보스의 물리·마법 저항 차이 기본 상한 (기획 5.1장, 0.15 = 15%p)")
            .defineInRange("bossResistGapDefault", 0.15, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BOSS_RESIST_GAP_EXCEPTION = BUILDER
            .comment("필수 챕터 보스의 물리·마법 저항 차이 예외 상한 (기획 5.1장, 0.20 = 20%p)")
            .defineInRange("bossResistGapException", 0.20, 0.0, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private RpgConfig() {
    }
}
