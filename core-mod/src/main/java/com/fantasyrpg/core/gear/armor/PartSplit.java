package com.fantasyrpg.core.gear.armor;

import java.util.Arrays;
import java.util.Comparator;

/**
 * 세트 합계를 부위별 정수로 나눈다 (최대 나머지 방식). Minecraft 비의존.
 *
 * <ol>
 *   <li>부위 정확한 값 = 합계 × 비율 / 20</li>
 *   <li>정수 부분을 먼저 준다</li>
 *   <li>모자란 만큼 소수점 아래가 큰 부위부터 1씩. 같으면 흉갑 → 각반 → 투구 → 장화</li>
 * </ol>
 * 결과 배열 순서는 {@link ArmorPart#values()} (투구, 흉갑, 각반, 장화).
 */
public final class PartSplit {
    private PartSplit() {
    }

    public static int[] split(int total) {
        if (total < 0) throw new IllegalArgumentException("합계가 음수: " + total);
        ArmorPart[] parts = ArmorPart.values();
        int[] out = new int[parts.length];
        int[] remainder = new int[parts.length]; // 나머지 × 20 (정수 비교로 부동소수 오차 없음)
        int given = 0;
        for (int i = 0; i < parts.length; i++) {
            int scaled = total * parts[i].share();
            out[i] = scaled / ArmorPart.SHARE_TOTAL;
            remainder[i] = scaled % ArmorPart.SHARE_TOTAL;
            given += out[i];
        }
        Integer[] order = {0, 1, 2, 3};
        Arrays.sort(order, Comparator.<Integer>comparingInt(i -> -remainder[i])
                .thenComparingInt(i -> parts[i].tiePriority()));
        for (int k = 0; k < total - given; k++) out[order[k]]++;
        return out;
    }

    /** 넉백 저항처럼 0.01 단위 값: 100배 정수로 나눈 뒤 되돌린다. */
    public static double[] splitHundredths(double total) {
        int[] cents = split((int) Math.round(total * 100));
        double[] out = new double[cents.length];
        for (int i = 0; i < cents.length; i++) out[i] = cents[i] / 100.0;
        return out;
    }
}
