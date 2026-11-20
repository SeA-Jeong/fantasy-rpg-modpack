package com.fantasyrpg.core.party;

/**
 * 파티 규칙 (순수 계산, 2026-11-21). 파티는 magiccity API(FTB Teams 파티)로 읽는다.
 * 수치는 data/fantasyrpg/skill_settings/party.json [초안].
 */
public final class PartyRules {
    private PartyRules() {}

    /**
     * @param healRadius  파티 회복이 닿는 거리(칸)
     * @param healShare   시전자가 받는 회복량 대비 파티원이 받는 비율
     * @param buffRadius  파티 버프(신성 방벽 감소)가 닿는 거리
     * @param buffShare   시전자 감소율 대비 파티원 감소율
     * @param xpRadius    처치 경험치를 나눠 받는 거리
     * @param xpBonus     함께한 파티원 1명마다 경험치 총량 +비율
     */
    public record Settings(double healRadius, double healShare, double buffRadius, double buffShare,
                           double xpRadius, double xpBonus) {
        public static final Settings DEFAULT = new Settings(16, 0.5, 16, 0.5, 48, 0.2);

        /** 빠진 키는 기본값 */
        public static Settings parse(com.google.gson.JsonObject o) {
            Settings d = DEFAULT;
            return new Settings(num(o, "heal_radius", d.healRadius), num(o, "heal_share", d.healShare),
                    num(o, "buff_radius", d.buffRadius), num(o, "buff_share", d.buffShare),
                    num(o, "xp_radius", d.xpRadius), num(o, "xp_bonus", d.xpBonus));
        }

        private static double num(com.google.gson.JsonObject o, String k, double def) {
            return o.has(k) ? o.get(k).getAsDouble() : def;
        }

        public Settings {
            if (healRadius < 0 || buffRadius < 0 || xpRadius < 0) throw new IllegalArgumentException("거리는 0 이상");
            if (healShare < 0 || healShare > 1 || buffShare < 0 || buffShare > 1) throw new IllegalArgumentException("비율은 0~1");
            if (xpBonus < 0) throw new IllegalArgumentException("xp_bonus 는 0 이상");
        }
    }

    /**
     * 처치 경험치 나누기: n명(처치자 포함)이 근처에 있으면 총량 = xp × (1 + bonus × (n − 1)), 한 사람 몫 = 총량 ÷ n (올림).
     * 혼자면 그대로. 경험치가 0이면 0.
     */
    public static int xpShare(int xp, int n, double bonus) {
        if (xp <= 0) return 0;
        if (n <= 1) return xp;
        return (int) Math.ceil(xp * (1 + bonus * (n - 1)) / n);
    }

    /** 감소율 r 의 파티원 몫 */
    public static double buffReduction(double reduction, double share) {
        return Math.max(0, Math.min(1, reduction * share));
    }
}
