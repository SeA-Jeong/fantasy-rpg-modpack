package com.fantasyrpg.core.playerdata;

import com.fantasyrpg.core.skilltree.SkillTreeRules;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 플레이어 RPG 상태 (NeoForge 데이터 첨부 {@code fantasyrpg:player}, 죽어도 유지). 바꿀 때는 새 값을 setData 한다.
 * 기준: claude/플레이어성장_설계.md 2절, claude/스킬트리_설계.md. 받은 SP·쓴 SP·공통 패시브 포인트는 저장하지 않고 계산한다.
 *
 * @param rpgClass 직업 id (빈 문자열 = 무직)
 * @param level 1~60
 * @param xp 현재 레벨에서 쌓인 경험치 (상한에 걸리면 보관 경험치)
 * @param startSpGranted 직업 선택 SP 지급 여부
 * @param clearBonusGranted 지급한 챕터 완료 보너스 SP 수 (0~6)
 * @param classChanges 직업을 바꾼 횟수
 * @param nodeRanks 전투 스킬트리 노드 id → 랭크
 * @param slots 장착칸 5개 (0~3 액티브, 4 궁극기). 빈 문자열 = 비어 있음
 * @param passiveRanks 공통 패시브 노드 id → 랭크
 * @param freeResetsUsed 쓴 무료 전체 초기화권 수
 */
public record RpgPlayerData(String rpgClass, int level, int xp, boolean startSpGranted, int clearBonusGranted,
                            int classChanges, Map<String, Integer> nodeRanks, List<String> slots,
                            Map<String, Integer> passiveRanks, int freeResetsUsed) {
    public static final int SLOT_COUNT = 5;
    public static final int ULTIMATE_SLOT = 4;
    public static final List<String> EMPTY_SLOTS = Collections.nCopies(SLOT_COUNT, "");
    public static final RpgPlayerData DEFAULT = new RpgPlayerData("", 1, 0, false, 0, 0, Map.of(), EMPTY_SLOTS, Map.of(), 0);

    public static final Codec<RpgPlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("class", "").forGetter(RpgPlayerData::rpgClass),
            Codec.INT.optionalFieldOf("level", 1).forGetter(RpgPlayerData::level),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(RpgPlayerData::xp),
            Codec.BOOL.optionalFieldOf("start_sp_granted", false).forGetter(RpgPlayerData::startSpGranted),
            Codec.INT.optionalFieldOf("clear_bonus_granted", 0).forGetter(RpgPlayerData::clearBonusGranted),
            Codec.INT.optionalFieldOf("class_changes", 0).forGetter(RpgPlayerData::classChanges),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("node_ranks", Map.of()).forGetter(RpgPlayerData::nodeRanks),
            Codec.STRING.listOf().optionalFieldOf("slots", EMPTY_SLOTS).forGetter(RpgPlayerData::slots),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("passive_ranks", Map.of()).forGetter(RpgPlayerData::passiveRanks),
            Codec.INT.optionalFieldOf("free_resets_used", 0).forGetter(RpgPlayerData::freeResetsUsed)
    ).apply(i, RpgPlayerData::new));

    public RpgPlayerData {
        nodeRanks = Map.copyOf(nodeRanks);
        passiveRanks = Map.copyOf(passiveRanks);
        List<String> s = new ArrayList<>(slots);
        while (s.size() < SLOT_COUNT) s.add("");
        slots = List.copyOf(s.subList(0, SLOT_COUNT));
    }

    public RpgClass classOrNull() {
        return RpgClass.byId(rpgClass);
    }

    public int spTotal() {
        return ProgressionMath.spTotal(startSpGranted, level, clearBonusGranted);
    }

    public int spSpent() {
        return SkillTreeRules.spent(nodeRanks);
    }

    public int spAvailable() {
        return spTotal() - spSpent();
    }

    public RpgPlayerData withLevel(int newLevel, int newXp) {
        return new RpgPlayerData(rpgClass, newLevel, newXp, startSpGranted, clearBonusGranted, classChanges, nodeRanks, slots,
                passiveRanks, freeResetsUsed);
    }

    /** 직업 선택·변경. 변경이면 전투 트리 투자·장착을 모두 비움(SP 반환) */
    public RpgPlayerData withClass(RpgClass c, boolean change) {
        return new RpgPlayerData(c.id(), level, xp, true, clearBonusGranted, classChanges + (change ? 1 : 0),
                change ? Map.of() : nodeRanks, change ? EMPTY_SLOTS : slots, passiveRanks, freeResetsUsed);
    }

    public RpgPlayerData withClearBonus(int granted) {
        return new RpgPlayerData(rpgClass, level, xp, startSpGranted, granted, classChanges, nodeRanks, slots, passiveRanks,
                freeResetsUsed);
    }

    public RpgPlayerData withNodeRanks(Map<String, Integer> ranks, List<String> newSlots) {
        return new RpgPlayerData(rpgClass, level, xp, startSpGranted, clearBonusGranted, classChanges, ranks, newSlots,
                passiveRanks, freeResetsUsed);
    }

    public RpgPlayerData withPassiveRanks(Map<String, Integer> ranks) {
        return new RpgPlayerData(rpgClass, level, xp, startSpGranted, clearBonusGranted, classChanges, nodeRanks, slots, ranks,
                freeResetsUsed);
    }

    public RpgPlayerData withFreeResetsUsed(int used) {
        return new RpgPlayerData(rpgClass, level, xp, startSpGranted, clearBonusGranted, classChanges, nodeRanks, slots,
                passiveRanks, used);
    }
}
