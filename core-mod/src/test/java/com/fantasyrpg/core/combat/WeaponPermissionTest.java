package com.fantasyrpg.core.combat;

import com.fantasyrpg.core.combat.WeaponPermission.AttackKind;
import com.fantasyrpg.core.combat.WeaponPermission.Verdict;
import com.fantasyrpg.core.combat.WeaponPermission.WeaponFacts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 정책 P1(직업 외 무기 사용 제한) 판정 표. */
class WeaponPermissionTest {

    private static final WeaponFacts IRON_SWORD = new WeaponFacts(false, "minecraft:iron_sword", Set.of("warrior"), Set.of());
    private static final WeaponFacts BOW = new WeaponFacts(false, "minecraft:bow", Set.of("archer"), Set.of());
    private static final WeaponFacts PICKAXE = new WeaponFacts(false, "minecraft:iron_pickaxe", Set.of(), Set.of());

    @Test
    @DisplayName("직업 없음 → 판정 대상 아님 (1차 실험은 바닐라)")
    void noClass() {
        assertEquals(Verdict.NOT_APPLICABLE, WeaponPermission.check(null, AttackKind.MELEE, IRON_SWORD, Set.of()));
        assertEquals(Verdict.NOT_APPLICABLE, WeaponPermission.check("", AttackKind.RANGED, BOW, Set.of()));
    }

    @Test
    @DisplayName("전사 + 검 근접 → 허용")
    void ownWeapon() {
        assertEquals(Verdict.ALLOW, WeaponPermission.check("warrior", AttackKind.MELEE, IRON_SWORD, Set.of()));
    }

    @Test
    @DisplayName("전사 + 활 → 다른 직업 무기로 거부 (원거리에도 적용)")
    void otherClassRanged() {
        Verdict v = WeaponPermission.check("warrior", AttackKind.RANGED, BOW, Set.of());
        assertEquals(Verdict.DENY_OTHER_CLASS, v);
        assertTrue(v.denied());
    }

    @Test
    @DisplayName("궁수 + 검 근접 → 거부")
    void otherClassMelee() {
        assertEquals(Verdict.DENY_OTHER_CLASS, WeaponPermission.check("archer", AttackKind.MELEE, IRON_SWORD, Set.of()));
    }

    @Test
    @DisplayName("곡괭이로 공격 → 무기 아님으로 거부 (채집 도구 우회 차단)")
    void toolAsWeapon() {
        assertEquals(Verdict.DENY_NOT_WEAPON, WeaponPermission.check("warrior", AttackKind.MELEE, PICKAXE, Set.of()));
    }

    @Test
    @DisplayName("맨손: 근접은 최소 피해, 원거리는 거부")
    void unarmed() {
        assertEquals(Verdict.UNARMED, WeaponPermission.check("warrior", AttackKind.MELEE, WeaponFacts.emptyHand(), Set.of()));
        assertEquals(Verdict.DENY_NOT_WEAPON, WeaponPermission.check("warrior", AttackKind.RANGED, WeaponFacts.emptyHand(), Set.of()));
        assertEquals(Verdict.UNARMED, WeaponPermission.check("warrior", AttackKind.MELEE, null, Set.of()));
        assertFalse(Verdict.UNARMED.denied());
    }

    @Test
    @DisplayName("예외 권한: 아이템 컴포넌트가 직업을 허용하거나, 플레이어 권한에 아이템이 있으면 허용")
    void grants() {
        WeaponFacts grantedBow = new WeaponFacts(false, "minecraft:bow", Set.of("archer"), Set.of("warrior"));
        assertEquals(Verdict.ALLOW, WeaponPermission.check("warrior", AttackKind.RANGED, grantedBow, Set.of()));
        assertEquals(Verdict.ALLOW, WeaponPermission.check("warrior", AttackKind.RANGED, BOW, Set.of("minecraft:bow")));
        assertEquals(Verdict.DENY_OTHER_CLASS, WeaponPermission.check("warrior", AttackKind.RANGED, BOW, Set.of("minecraft:crossbow")));
        assertEquals(Verdict.DENY_OTHER_CLASS, WeaponPermission.check("warrior", AttackKind.RANGED, BOW, null));
    }
}
