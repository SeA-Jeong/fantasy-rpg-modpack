package com.fantasyrpg.core.gear.armorcap;

import java.util.List;

/**
 * 방어 등급 상한과 바닐라 방어구 조정의 기본값.
 *
 * <p>Minecraft·NeoForge 클래스를 참조하지 않으므로 단위 테스트에서 그대로 쓸 수 있다.
 * 설정 파일({@code RpgConfig})의 기본값은 이 상수를 그대로 가리킨다.
 * 근거: 프로젝트 문서 claude/장비등급제한_설계.md 1-4절, claude/방어구_전투밸런스_기준_v1.0.md.
 */
public final class ArmorCapDefaults {
    /** 등급 1~10별 방어력 상한. */
    public static final List<Double> ARMOR_CAPS = List.of(12.0, 15.0, 18.0, 19.0, 20.0, 20.0, 20.0, 20.0, 20.0, 20.0);
    /** 등급 1~10별 방어 강도 상한. */
    public static final List<Double> TOUGHNESS_CAPS = List.of(0.0, 0.0, 2.0, 2.0, 3.0, 4.0, 6.0, 8.0, 10.0, 12.0);
    /** 다이아 방어구 방어력 (투구·흉갑·각반·장화). */
    public static final List<Double> DIAMOND_ARMOR = List.of(3.0, 7.0, 5.0, 3.0);
    /** 다이아 방어구 방어 강도 (투구·흉갑·각반·장화). */
    public static final List<Double> DIAMOND_TOUGHNESS = List.of(0.0, 1.0, 1.0, 0.0);
    /** 네더라이트 방어구 방어력 (투구·흉갑·각반·장화). */
    public static final List<Double> NETHERITE_ARMOR = List.of(3.0, 8.0, 6.0, 3.0);
    /** 네더라이트 방어구 방어 강도 (투구·흉갑·각반·장화). */
    public static final List<Double> NETHERITE_TOUGHNESS = List.of(1.0, 2.0, 2.0, 1.0);

    private ArmorCapDefaults() {
    }

    /** 등급에 맞는 상한. ArmorCapService.capFor 와 같은 규칙(범위 밖 등급은 양 끝 값). */
    public static double capFor(List<Double> caps, int grade) {
        return caps.get(Math.max(0, Math.min(caps.size() - 1, grade - 1)));
    }
}
