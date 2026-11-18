package com.fantasyrpg.core.playerdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 플레이어 RPG 상태 (NeoForge 데이터 첨부 {@code fantasyrpg:player}, 죽어도 유지). 바꿀 때는 새 값을 setData 한다.
 * 기준: claude/플레이어성장_설계.md 2절. 받은 SP 합계는 저장하지 않고 {@link ProgressionMath#spTotal}로 계산한다.
 *
 * @param rpgClass 직업 id (빈 문자열 = 무직)
 * @param level 1~60
 * @param xp 현재 레벨에서 쌓인 경험치 (상한에 걸리면 보관 경험치)
 * @param spSpent 스킬트리에 쓴 SP (스킬트리 전까지 0)
 * @param startSpGranted 직업 선택 SP 지급 여부
 * @param clearBonusGranted 지급한 챕터 완료 보너스 SP 수 (0~6)
 * @param classChanges 직업을 바꾼 횟수
 */
public record RpgPlayerData(String rpgClass, int level, int xp, int spSpent, boolean startSpGranted,
                            int clearBonusGranted, int classChanges) {
    public static final RpgPlayerData DEFAULT = new RpgPlayerData("", 1, 0, 0, false, 0, 0);

    public static final Codec<RpgPlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("class", "").forGetter(RpgPlayerData::rpgClass),
            Codec.INT.optionalFieldOf("level", 1).forGetter(RpgPlayerData::level),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(RpgPlayerData::xp),
            Codec.INT.optionalFieldOf("sp_spent", 0).forGetter(RpgPlayerData::spSpent),
            Codec.BOOL.optionalFieldOf("start_sp_granted", false).forGetter(RpgPlayerData::startSpGranted),
            Codec.INT.optionalFieldOf("clear_bonus_granted", 0).forGetter(RpgPlayerData::clearBonusGranted),
            Codec.INT.optionalFieldOf("class_changes", 0).forGetter(RpgPlayerData::classChanges)
    ).apply(i, RpgPlayerData::new));

    public RpgClass classOrNull() {
        return RpgClass.byId(rpgClass);
    }

    public int spTotal() {
        return ProgressionMath.spTotal(startSpGranted, level, clearBonusGranted);
    }

    public int spAvailable() {
        return spTotal() - spSpent;
    }

    public RpgPlayerData withLevel(int newLevel, int newXp) {
        return new RpgPlayerData(rpgClass, newLevel, newXp, spSpent, startSpGranted, clearBonusGranted, classChanges);
    }

    public RpgPlayerData withClass(RpgClass c, boolean change) {
        return new RpgPlayerData(c.id(), level, xp, change ? 0 : spSpent, true, clearBonusGranted,
                classChanges + (change ? 1 : 0));
    }

    public RpgPlayerData withClearBonus(int granted) {
        return new RpgPlayerData(rpgClass, level, xp, spSpent, startSpGranted, granted, classChanges);
    }
}
