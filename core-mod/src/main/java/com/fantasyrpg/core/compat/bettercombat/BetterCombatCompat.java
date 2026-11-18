package com.fantasyrpg.core.compat.bettercombat;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.minecraft.world.entity.player.Player;

/**
 * Better Combat 연동. 이 클래스는 Better Combat이 있을 때만 불러야 한다({@code ModList.isLoaded("bettercombat")}).
 * 외부 모드 클래스를 참조하는 코드는 compat 패키지에만 둔다(혼합개발 연동설계 B-1).
 */
public final class BetterCombatCompat {
    private BetterCombatCompat() {
    }

    /** 지금 휘두르는 콤보 단계의 피해 배율. 알 수 없으면 1.0 */
    public static double comboMultiplier(Player player) {
        if (!(player instanceof EntityPlayer_BetterCombat bc)) return 1.0;
        AttackHand hand = bc.getCurrentAttack();
        if (hand == null || hand.attack() == null) return 1.0;
        return hand.attack().damageMultiplier();
    }
}
