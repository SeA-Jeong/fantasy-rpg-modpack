package com.fantasyrpg.core.gear.vanilla;

import java.util.List;

/**
 * 바닐라 다이아몬드·네더라이트 방어구 조정 기본값 (투구·흉갑·각반·장화).
 *
 * <p>Minecraft·NeoForge 클래스를 참조하지 않으므로 단위 테스트에서 그대로 쓸 수 있다.
 * 설정 파일({@code RpgConfig})의 기본값은 이 상수를 가리킨다.
 * 근거: 프로젝트 문서 claude/방어구_전투밸런스_기준_v1.0.md 3-1 (다이아 18/2, 네더라이트 20/6).
 * 2026-11-18 방어 등급 상한을 없앤 뒤에도 유지한다 — 모드 계열 방어구가 같은 단계 바닐라 갑옷보다 높게 설계되는 기준선.
 */
public final class VanillaArmorDefaults {
    public static final List<Double> DIAMOND_ARMOR = List.of(3.0, 7.0, 5.0, 3.0);
    public static final List<Double> DIAMOND_TOUGHNESS = List.of(0.0, 1.0, 1.0, 0.0);
    public static final List<Double> NETHERITE_ARMOR = List.of(3.0, 8.0, 6.0, 3.0);
    public static final List<Double> NETHERITE_TOUGHNESS = List.of(1.0, 2.0, 2.0, 1.0);

    private VanillaArmorDefaults() {
    }
}
