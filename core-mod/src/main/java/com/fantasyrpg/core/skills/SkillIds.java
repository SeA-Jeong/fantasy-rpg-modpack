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

    // ── 3단 액티브·4단 패시브 (claude/6직업_3-4단_스킬_초안.md) ──
    public static final String W_WHIRLWIND = "fantasyrpg:warrior_whirlwind";
    public static final String W_FRENZY = "fantasyrpg:warrior_frenzy";
    public static final String W_BLADE_STORM = "fantasyrpg:warrior_blade_storm";
    public static final String W_MANA_FLOW = "fantasyrpg:warrior_mana_flow";
    public static final String W_COUNTER_SMASH = "fantasyrpg:warrior_counter_smash";
    public static final String W_STEEL_WILL = "fantasyrpg:warrior_steel_will";
    public static final String W_GALE_THRUST = "fantasyrpg:warrior_gale_thrust";
    public static final String W_COMBO_MASTERY = "fantasyrpg:warrior_combo_mastery";
    public static final String M_BOLT_BARRAGE = "fantasyrpg:mage_bolt_barrage";
    public static final String M_BOLT_RESONANCE = "fantasyrpg:mage_bolt_resonance";
    public static final String M_ARCANE_STORM = "fantasyrpg:mage_arcane_storm";
    public static final String M_ARCANE_AMPLIFY = "fantasyrpg:mage_arcane_amplify";
    public static final String M_BARRIER_BURST = "fantasyrpg:mage_barrier_burst";
    public static final String M_CYCLE_BARRIER = "fantasyrpg:mage_cycle_barrier";
    public static final String M_HASTE = "fantasyrpg:mage_haste";
    public static final String M_SPELL_MASTERY = "fantasyrpg:mage_spell_mastery";
    public static final String K_SPIN = "fantasyrpg:knight_spin";
    public static final String K_CHARGE_MASTERY = "fantasyrpg:knight_charge_mastery";
    public static final String K_SPEAR_RAIN = "fantasyrpg:knight_spear_rain";
    public static final String K_MANA_AWAKEN = "fantasyrpg:knight_mana_awaken";
    public static final String K_SHIELD_BASH = "fantasyrpg:knight_shield_bash";
    public static final String K_BULWARK = "fantasyrpg:knight_bulwark";
    public static final String K_THRUST_FLURRY = "fantasyrpg:knight_thrust_flurry";
    public static final String K_COUNTER_MASTERY = "fantasyrpg:knight_counter_mastery";
    public static final String R_ASSASSINATE = "fantasyrpg:rogue_assassinate";
    public static final String R_VITAL_MASTERY = "fantasyrpg:rogue_vital_mastery";
    public static final String R_SHADOW_VORTEX = "fantasyrpg:rogue_shadow_vortex";
    public static final String R_SHADOW_MASTERY = "fantasyrpg:rogue_shadow_mastery";
    public static final String R_SHADOW_COUNTER = "fantasyrpg:rogue_shadow_counter";
    public static final String R_SHADOW_STEP = "fantasyrpg:rogue_shadow_step";
    public static final String R_MARK_BURST = "fantasyrpg:rogue_mark_burst";
    public static final String R_MARK_MASTERY = "fantasyrpg:rogue_mark_mastery";
    public static final String A_POWER_SHOT = "fantasyrpg:archer_power_shot";
    public static final String A_FOCUS_FIRE = "fantasyrpg:archer_focus_fire";
    public static final String A_ARROW_RAIN = "fantasyrpg:archer_arrow_rain";
    public static final String A_ELEMENT_MASTERY = "fantasyrpg:archer_element_mastery";
    public static final String A_TRAP = "fantasyrpg:archer_trap";
    public static final String A_SURVIVAL = "fantasyrpg:archer_survival";
    public static final String A_LEAP_SHOT = "fantasyrpg:archer_leap_shot";
    public static final String A_RAPID_MASTERY = "fantasyrpg:archer_rapid_mastery";
    public static final String C_JUDGMENT = "fantasyrpg:cleric_judgment";
    public static final String C_SMITE_MASTERY = "fantasyrpg:cleric_smite_mastery";
    public static final String C_HOLY_BURST = "fantasyrpg:cleric_holy_burst";
    public static final String C_FAITH_AWAKEN = "fantasyrpg:cleric_faith_awaken";
    public static final String C_RETRIBUTION = "fantasyrpg:cleric_retribution";
    public static final String C_OATH = "fantasyrpg:cleric_oath";
    public static final String C_PURIFY_FLURRY = "fantasyrpg:cleric_purify_flurry";
    public static final String C_PURIFY_MASTERY = "fantasyrpg:cleric_purify_mastery";

    /** 연쇄 패시브 (스킬을 이어 쓰면): 마법사 주문 연쇄, 전사 연계 숙달 */
    public static final String[] CHAIN = {M_SPELL_CHAIN, W_COMBO_MASTERY};

    /** 코어 문턱(bonus_core_min)이 걸리는 합산 키 */
    public static final java.util.Set<String> CORE_GATED = java.util.Set.of("all_damage_bonus", "skill_magic_bonus",
            "skill_physical_bonus", "cooldown_rate", "basic_hit_core_high");

    // ── 5단 궁극기 (claude/6직업_궁극기_초안.md) ──
    public static final String U_W_RUIN = "fantasyrpg:warrior_ult_ruin";
    public static final String U_W_RELEASE = "fantasyrpg:warrior_ult_release";
    public static final String U_W_SHOUT = "fantasyrpg:warrior_ult_shout";
    public static final String U_W_THOUSAND = "fantasyrpg:warrior_ult_thousand_blades";
    public static final String U_M_BOLT_STORM = "fantasyrpg:mage_ult_bolt_storm";
    public static final String U_M_METEOR = "fantasyrpg:mage_ult_meteor";
    public static final String U_M_ABSOLUTE = "fantasyrpg:mage_ult_absolute_barrier";
    public static final String U_M_TIME = "fantasyrpg:mage_ult_time_accel";
    public static final String U_K_THUNDER = "fantasyrpg:knight_ult_thunder_charge";
    public static final String U_K_LANCE = "fantasyrpg:knight_ult_holy_lance";
    public static final String U_K_WALL = "fantasyrpg:knight_ult_rampart";
    public static final String U_K_DANCE = "fantasyrpg:knight_ult_spear_dance";
    public static final String U_R_EXECUTE = "fantasyrpg:rogue_ult_execute";
    public static final String U_R_CLONE = "fantasyrpg:rogue_ult_shadow_clone";
    public static final String U_R_VEIL = "fantasyrpg:rogue_ult_shadow_veil";
    public static final String U_R_CHAIN = "fantasyrpg:rogue_ult_chain_execution";
    public static final String U_A_SKYPIERCER = "fantasyrpg:archer_ult_skypiercer";
    public static final String U_A_STORM = "fantasyrpg:archer_ult_element_storm";
    public static final String U_A_VANISH = "fantasyrpg:archer_ult_vanish";
    public static final String U_A_DELUGE = "fantasyrpg:archer_ult_arrow_deluge";
    public static final String U_C_HAMMER = "fantasyrpg:cleric_ult_divine_hammer";
    public static final String U_C_SANCTUM = "fantasyrpg:cleric_ult_sanctum";
    public static final String U_C_PRAYER = "fantasyrpg:cleric_ult_prayer";
    public static final String U_C_RADIANCE = "fantasyrpg:cleric_ult_radiance";
}
