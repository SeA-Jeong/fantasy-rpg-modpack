package com.fantasyrpg.core.combat;

import java.util.Set;

/**
 * 무기 사용 권한 판정 (순수 계산, Minecraft 비의존).
 *
 * <p>정책 P1 (프로젝트 문서 claude/혼합개발_연동설계_v1.md 0절, 2026-11-17 확정):
 * <ul>
 *   <li>직업에 맞지 않는 무기로는 RPG 피해를 줄 수 없다. 근접·원거리 모두 적용</li>
 *   <li>다른 직업 무기의 소지·거래는 자유 → 이 판정은 "공격할 때"만 쓴다 (착용 제한과 별개)</li>
 *   <li>채집 도구 등 무기가 아닌 아이템으로 공격하면 막는다 (채집 자체는 이 판정을 거치지 않음)</li>
 *   <li>맨손 근접은 최소 기본 피해만 (스킬·장비 효과 없음)</li>
 *   <li>예외 권한(스킬·장비 효과로 다른 무기 허용)은 {@code grantedClasses}·{@code playerGrants}로 확장</li>
 * </ul>
 *
 * <p>Minecraft 쪽 코드는 아이템 태그·데이터 컴포넌트를 읽어 {@link WeaponFacts}를 만들고 이 함수만 부른다.
 */
public final class WeaponPermission {

    /** 공격 종류. 근접은 공격 이벤트, 원거리는 사용 시작·투사체 명중에서 판정한다. */
    public enum AttackKind { MELEE, RANGED }

    /** 판정 결과. */
    public enum Verdict {
        /** 직업 판정 대상이 아님(직업 없음) — 1차 실험에서는 바닐라 처리 */
        NOT_APPLICABLE,
        /** 직업 무기 또는 예외 권한 — RPG 피해 */
        ALLOW,
        /** 맨손 근접 — 고정 최소 피해 */
        UNARMED,
        /** 다른 직업의 무기 */
        DENY_OTHER_CLASS,
        /** 무기가 아닌 아이템(채집 도구 포함) 또는 맨손 원거리 */
        DENY_NOT_WEAPON;

        public boolean denied() {
            return this == DENY_OTHER_CLASS || this == DENY_NOT_WEAPON;
        }
    }

    /**
     * 손에 든 아이템에 대한 사실.
     *
     * @param empty          빈손인가
     * @param itemId         아이템 id (예외 권한 비교용, 빈손이면 빈 문자열)
     * @param weaponClasses  이 공격 종류에서 이 아이템을 주력 무기로 쓰는 직업 id들 (태그 {@code fantasyrpg:class_weapons/<직업>/<melee|ranged>})
     * @param grantedClasses 아이템 데이터 컴포넌트 {@code fantasyrpg:weapon_grant}가 추가로 허용하는 직업 id들
     */
    public record WeaponFacts(boolean empty, String itemId, Set<String> weaponClasses, Set<String> grantedClasses) {
        public WeaponFacts {
            itemId = itemId == null ? "" : itemId;
            weaponClasses = weaponClasses == null ? Set.of() : Set.copyOf(weaponClasses);
            grantedClasses = grantedClasses == null ? Set.of() : Set.copyOf(grantedClasses);
        }

        public static WeaponFacts emptyHand() {
            return new WeaponFacts(true, "", Set.of(), Set.of());
        }
    }

    private WeaponPermission() {
    }

    /**
     * @param playerClass  플레이어 직업 id. 없으면 null 또는 빈 문자열
     * @param kind         공격 종류
     * @param item         손에 든 아이템
     * @param playerGrants 플레이어 데이터의 예외 권한 (허용된 아이템 id). 1차에서는 비어 있음
     */
    public static Verdict check(String playerClass, AttackKind kind, WeaponFacts item, Set<String> playerGrants) {
        if (playerClass == null || playerClass.isEmpty()) return Verdict.NOT_APPLICABLE;
        if (item == null || item.empty()) {
            return kind == AttackKind.MELEE ? Verdict.UNARMED : Verdict.DENY_NOT_WEAPON;
        }
        if (item.weaponClasses().contains(playerClass)) return Verdict.ALLOW;
        if (item.grantedClasses().contains(playerClass)) return Verdict.ALLOW;
        if (playerGrants != null && playerGrants.contains(item.itemId())) return Verdict.ALLOW;
        return item.weaponClasses().isEmpty() ? Verdict.DENY_NOT_WEAPON : Verdict.DENY_OTHER_CLASS;
    }
}
