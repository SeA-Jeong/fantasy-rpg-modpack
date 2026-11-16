package com.fantasyrpg.core.resist;

import com.fantasyrpg.core.combat.Resistances;

/**
 * 데이터팩 {@code mob_resistances/*.json} 한 파일의 내용.
 *
 * @param entityId 엔티티 ID 문자열 (예: "minecraft:zombie"), 기본값 파일은 "default"
 * @param role     몬스터 역할
 * @param chapter  등장 장 (없으면 0)
 * @param resist   물리·마법 저항
 */
public record MobResistanceProfile(String entityId, MobRole role, int chapter, Resistances resist) {
    public static final String DEFAULT_ENTITY = "default";
}
