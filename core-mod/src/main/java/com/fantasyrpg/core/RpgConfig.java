package com.fantasyrpg.core;

import com.fantasyrpg.core.gear.armorcap.ArmorCapDefaults;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

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

    // ── 방어 등급 상한 (프로젝트 문서 claude/장비등급제한_설계.md 1-4절) ──
    public static final ModConfigSpec.BooleanValue ARMOR_CAP_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> ARMOR_CAPS;
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> TOUGHNESS_CAPS;

    // ── 바닐라 방어구 조정 (투구·흉갑·각반·장화 순서) ──
    public static final ModConfigSpec.BooleanValue VANILLA_TUNING_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> DIAMOND_ARMOR;
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> DIAMOND_TOUGHNESS;
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> NETHERITE_ARMOR;
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> NETHERITE_TOUGHNESS;

    // 기본값은 Minecraft 없이 테스트할 수 있도록 ArmorCapDefaults 에 둔다
    public static final List<Double> DEFAULT_ARMOR_CAPS = ArmorCapDefaults.ARMOR_CAPS;
    public static final List<Double> DEFAULT_TOUGHNESS_CAPS = ArmorCapDefaults.TOUGHNESS_CAPS;
    public static final List<Double> DEFAULT_DIAMOND_ARMOR = ArmorCapDefaults.DIAMOND_ARMOR;
    public static final List<Double> DEFAULT_DIAMOND_TOUGHNESS = ArmorCapDefaults.DIAMOND_TOUGHNESS;
    public static final List<Double> DEFAULT_NETHERITE_ARMOR = ArmorCapDefaults.NETHERITE_ARMOR;
    public static final List<Double> DEFAULT_NETHERITE_TOUGHNESS = ArmorCapDefaults.NETHERITE_TOUGHNESS;

    static {
        BUILDER.comment("플레이어 방어력·방어 강도 등급 상한. ARMOR·ARMOR_TOUGHNESS 속성에 직접 더해지는 모든 값(방어구·장신구·스킬·다른 모드)에 적용")
                .push("armorCap");
        ARMOR_CAP_ENABLED = BUILDER.comment("상한 보정 사용")
                .define("enabled", true);
        ARMOR_CAPS = BUILDER.comment("1~10급 방어력 상한 (10개)")
                .<Number>defineList("armorCaps", DEFAULT_ARMOR_CAPS, RpgConfig::isNonNegativeNumber);
        TOUGHNESS_CAPS = BUILDER.comment("1~10급 방어 강도 상한 (10개)")
                .<Number>defineList("toughnessCaps", DEFAULT_TOUGHNESS_CAPS, RpgConfig::isNonNegativeNumber);
        BUILDER.pop();

        BUILDER.comment("바닐라 방어구 능력치 조정. 아이템은 그대로 두고 능력치만 바꾼다. 값은 투구·흉갑·각반·장화 순서 (철 15/0은 바닐라와 같아 조정하지 않음)")
                .push("vanillaTuning");
        VANILLA_TUNING_ENABLED = BUILDER.comment("조정 사용")
                .define("enabled", true);
        DIAMOND_ARMOR = BUILDER.comment("다이아몬드 방어력 (합계 18)")
                .<Number>defineList("diamondArmor", DEFAULT_DIAMOND_ARMOR, RpgConfig::isNonNegativeNumber);
        DIAMOND_TOUGHNESS = BUILDER.comment("다이아몬드 방어 강도 (합계 2)")
                .<Number>defineList("diamondToughness", DEFAULT_DIAMOND_TOUGHNESS, RpgConfig::isNonNegativeNumber);
        NETHERITE_ARMOR = BUILDER.comment("네더라이트 방어력 (합계 20)")
                .<Number>defineList("netheriteArmor", DEFAULT_NETHERITE_ARMOR, RpgConfig::isNonNegativeNumber);
        NETHERITE_TOUGHNESS = BUILDER.comment("네더라이트 방어 강도 (합계 6)")
                .<Number>defineList("netheriteToughness", DEFAULT_NETHERITE_TOUGHNESS, RpgConfig::isNonNegativeNumber);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private RpgConfig() {
    }

    private static boolean isNonNegativeNumber(Object o) {
        return o instanceof Number n && Double.isFinite(n.doubleValue()) && n.doubleValue() >= 0.0;
    }

    /**
     * 목록 설정값을 읽는다. 설정이 아직 로드되지 않았거나 개수가 맞지 않으면 기본값을 쓴다.
     */
    public static double[] numbers(ModConfigSpec.ConfigValue<List<? extends Number>> value, List<Double> fallback) {
        List<? extends Number> list = fallback;
        if (SPEC.isLoaded()) {
            List<? extends Number> configured = value.get();
            if (configured.size() == fallback.size()) list = configured;
        }
        double[] out = new double[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i).doubleValue();
        return out;
    }

    public static boolean flag(ModConfigSpec.BooleanValue value, boolean fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }
}
