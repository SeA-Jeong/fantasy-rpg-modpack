package com.fantasyrpg.core.skills;

/** 스킬 효과 id (노드 데이터의 "effect"). 노드 데이터와 이 이름이 맞아야 효과가 붙는다. */
public final class SkillIds {
    private SkillIds() {
    }

    // 전사
    public static final String W_HEAVY_STRIKE = "fantasyrpg:warrior_heavy_strike";
    public static final String W_SWORD_MASTERY = "fantasyrpg:warrior_sword_mastery";
    public static final String W_MANA_SLASH = "fantasyrpg:warrior_mana_slash";
    public static final String W_SPELLBLADE = "fantasyrpg:warrior_spellblade";
    public static final String W_INDOMITABLE = "fantasyrpg:warrior_indomitable";
    public static final String W_FIGHTING_SPIRIT = "fantasyrpg:warrior_fighting_spirit";
    public static final String W_FLURRY = "fantasyrpg:warrior_flurry";
    public static final String W_COMBO_SENSE = "fantasyrpg:warrior_combo_sense";
    // 마법사
    public static final String M_SOLID_BOLTS = "fantasyrpg:mage_solid_bolts";
    public static final String M_BOLT_TRAINING = "fantasyrpg:mage_bolt_training";
    public static final String M_ARCANE_BURST = "fantasyrpg:mage_arcane_burst";
    public static final String M_ARCANE_INSIGHT = "fantasyrpg:mage_arcane_insight";
    public static final String M_BARRIER_SHOCK = "fantasyrpg:mage_barrier_shock";
    public static final String M_MANA_WARD = "fantasyrpg:mage_mana_ward";
    public static final String M_BLINK = "fantasyrpg:mage_blink";
    public static final String M_SPELL_CHAIN = "fantasyrpg:mage_spell_chain";

    // 기사
    public static final String K_CHARGE = "fantasyrpg:knight_charge";
    public static final String K_SPEAR_MASTERY = "fantasyrpg:knight_spear_mastery";
    public static final String K_MANA_SPEAR = "fantasyrpg:knight_mana_spear";
    public static final String K_RUNE_ENGRAVE = "fantasyrpg:knight_rune_engrave";
    public static final String K_PERFECT_GUARD = "fantasyrpg:knight_perfect_guard";
    public static final String K_STEADFAST = "fantasyrpg:knight_steadfast";
    public static final String K_RIPOSTE = "fantasyrpg:knight_riposte";
    public static final String K_BATTLE_RHYTHM = "fantasyrpg:knight_battle_rhythm";
    // 도적
    public static final String R_VITAL_FLURRY = "fantasyrpg:rogue_vital_flurry";
    public static final String R_VITALS = "fantasyrpg:rogue_vitals";
    public static final String R_SHADOW_BLADES = "fantasyrpg:rogue_shadow_blades";
    public static final String R_SHADOW_EDGE = "fantasyrpg:rogue_shadow_edge";
    public static final String R_EVADE = "fantasyrpg:rogue_shadow_evade";
    public static final String R_NIMBLE = "fantasyrpg:rogue_nimble";
    public static final String R_CHAIN_MARK = "fantasyrpg:rogue_chain_mark";
    public static final String R_VITAL_SENSE = "fantasyrpg:rogue_vital_sense";
    // 궁수
    public static final String A_PIERCING_SHOT = "fantasyrpg:archer_piercing_shot";
    public static final String A_PRECISION = "fantasyrpg:archer_precision";
    public static final String A_ELEMENTAL_ARROW = "fantasyrpg:archer_elemental_arrow";
    public static final String A_ELEMENTAL_FLETCH = "fantasyrpg:archer_elemental_fletching";
    public static final String A_RETREAT_SHOT = "fantasyrpg:archer_retreat_shot";
    public static final String A_KEEP_DISTANCE = "fantasyrpg:archer_keep_distance";
    public static final String A_RAPID_FIRE = "fantasyrpg:archer_rapid_fire";
    public static final String A_MARKSMAN = "fantasyrpg:archer_marksman";
    // 전투사제
    public static final String C_SMITE = "fantasyrpg:cleric_smite";
    public static final String C_MACE_MASTERY = "fantasyrpg:cleric_mace_mastery";
    public static final String C_HOLY_LIGHT = "fantasyrpg:cleric_holy_light";
    public static final String C_HOLY_ENGRAVE = "fantasyrpg:cleric_holy_engrave";
    public static final String C_SANCTUARY = "fantasyrpg:cleric_sanctuary";
    public static final String C_FAITH_ARMOR = "fantasyrpg:cleric_faith_armor";
    public static final String C_PURIFY = "fantasyrpg:cleric_purify";
    public static final String C_HOLY_RHYTHM = "fantasyrpg:cleric_holy_rhythm";

    // ── 같은 역할의 패시브 묶음 (직업마다 하나씩) ──

    /** 공격 2단: 근접 기본 공격 물리 계수 +% (basic_physical_bonus) */
    public static final String[] MELEE_MASTERY = {W_SWORD_MASTERY, K_SPEAR_MASTERY, R_VITALS, C_MACE_MASTERY};
    /** 주문 2단: 근접 기본 공격 마법 추가 (basic_magic, high_core·high_core_mult) */
    public static final String[] MELEE_MAGIC = {W_SPELLBLADE, K_RUNE_ENGRAVE, R_SHADOW_EDGE, C_HOLY_ENGRAVE};
    /** 주문 2단 5랭크: 주문 1단 쿨타임 감소 (cooldown_cut) — 패시브 → 대상 액티브 */
    public static final String[][] COOLDOWN_CUT = {{W_SPELLBLADE, W_MANA_SLASH}, {K_RUNE_ENGRAVE, K_MANA_SPEAR},
            {R_SHADOW_EDGE, R_SHADOW_BLADES}, {A_ELEMENTAL_FLETCH, A_ELEMENTAL_ARROW}, {C_HOLY_ENGRAVE, C_HOLY_LIGHT}};
    /** 기교 2단: 치명타 확률(crit_chance), 스킬 뒤 기본 공격(after_skill_*), 치명타 때 코어(crit_core) */
    public static final String[] FINESSE_SENSE = {W_COMBO_SENSE, K_BATTLE_RHYTHM, R_VITAL_SENSE, A_MARKSMAN, C_HOLY_RHYTHM};
    /** 수호 2단: 맞으면 코어(hurt_core), 위기 반응(low_*) */
    public static final String[] GUARD_PASSIVE = {W_FIGHTING_SPIRIT, K_STEADFAST, R_NIMBLE, A_KEEP_DISTANCE, C_FAITH_ARMOR};
}
