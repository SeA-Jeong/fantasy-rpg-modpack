package com.fantasyrpg.core.gear.weapon;

import com.fantasyrpg.core.playerdata.RpgClass;

/**
 * 직업 무기 6종 (claude/전투연결_설계.md 1절, 기획 v0.4 2장: 직업마다 주력 무기 고정). Minecraft 비의존.
 *
 * @param rpgClass 이 무기를 쓰는 직업
 * @param placeholderTexture 전용 그림 전까지 빌려 쓰는 바닐라 아이템 그림 (minecraft:item/…)
 * @param betterCombatPreset Better Combat 무기 속성 (bettercombat:…)
 */
public enum WeaponType {
    SWORD("warrior_sword", RpgClass.WARRIOR, "iron_sword", "sword"),
    SPEAR("knight_spear", RpgClass.KNIGHT, "trident", "spear"),
    DAGGER("rogue_dagger", RpgClass.ROGUE, "golden_sword", "dagger"),
    BOW("archer_bow", RpgClass.ARCHER, "bow", "bow_two_handed_light"),
    STAFF("mage_staff", RpgClass.MAGE, "blaze_rod", "wand"),
    MACE("cleric_mace", RpgClass.CLERIC, "mace", "mace");

    private final String itemName;
    private final RpgClass rpgClass;
    private final String placeholderTexture;
    private final String betterCombatPreset;

    WeaponType(String itemName, RpgClass rpgClass, String placeholderTexture, String betterCombatPreset) {
        this.itemName = itemName;
        this.rpgClass = rpgClass;
        this.placeholderTexture = placeholderTexture;
        this.betterCombatPreset = betterCombatPreset;
    }

    public String itemName() {
        return itemName;
    }

    public RpgClass rpgClass() {
        return rpgClass;
    }

    public String placeholderTexture() {
        return placeholderTexture;
    }

    public String betterCombatPreset() {
        return betterCombatPreset;
    }
}
