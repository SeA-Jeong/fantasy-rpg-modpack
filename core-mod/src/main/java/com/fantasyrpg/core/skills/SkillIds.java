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

    /** 패시브 효과 id (등록만 해서 "효과 없음" 표시를 피함) */
    public static final String[] PASSIVES = {W_SWORD_MASTERY, W_SPELLBLADE, W_FIGHTING_SPIRIT, W_COMBO_SENSE,
            M_BOLT_TRAINING, M_ARCANE_INSIGHT, M_MANA_WARD, M_SPELL_CHAIN};
}
