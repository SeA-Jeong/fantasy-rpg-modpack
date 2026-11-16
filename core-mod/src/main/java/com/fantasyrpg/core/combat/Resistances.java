package com.fantasyrpg.core.combat;

/**
 * 대상의 물리·마법 저항률(0~1). 예: 0.25 = 피해 25% 감소.
 * 특수 취약(음수 저항)은 기획 v0.4 4.2장에서 추후 결정이므로 지금은 허용하지 않는다.
 */
public record Resistances(double physical, double magic) {
    public static final Resistances NONE = new Resistances(0.0, 0.0);

    public Resistances {
        requireRatio("physical", physical);
        requireRatio("magic", magic);
    }

    /** 물리·마법 저항 차이(절댓값). 필수 보스 기준 검사에 사용. */
    public double gap() {
        return Math.abs(physical - magic);
    }

    static void requireRatio(String name, double value) {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + "은(는) 0~1 사이여야 합니다: " + value);
        }
    }
}
