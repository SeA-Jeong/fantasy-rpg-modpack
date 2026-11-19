package com.fantasyrpg.core.combat;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.gear.weapon.ClassWeaponItem;
import com.fantasyrpg.core.gear.weapon.WeaponType;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.fantasyrpg.core.skills.SkillMath;
import com.fantasyrpg.core.skills.SkillRuntime;
import com.fantasyrpg.core.skills.SkillTargets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 기사 기본 가드·패리와 바닐라 방패 금지 (기획 v0.4 5.3, 2026-11-19 결정).
 * <ul>
 *   <li>기사가 직업 무기(창)를 들고 오른쪽 버튼을 누르고 있으면 가드: 정면 피해 감소, 들어 올린 직후 패리(피해 0)</li>
 *   <li>패리: 결의, 일반 몬스터 경직, "패리 후" 효과 창(반격 찌르기·방패 강타 등)이 열림. 스킬 완벽 방어의 랭크 효과는 없음</li>
 *   <li>직업이 있는 플레이어는 바닐라 방패를 들 수 없음 (정면 100% 방어가 직업 방어 밸런스를 깸)</li>
 * </ul>
 * 수치는 {@code combat/guard.json} ({@link GuardSettings}).
 */
public final class GuardEvents {
    private GuardEvents() {}

    public static boolean isSpear(ItemStack stack) {
        return stack.getItem() instanceof ClassWeaponItem w && w.type() == WeaponType.SPEAR;
    }

    private static boolean isShield(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    /** 들어 올리기 시작: 기사 창이면 시각 기록, 직업이 있으면 바닐라 방패 막기 */
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (isShield(stack)) {
            boolean hasClass = player instanceof ServerPlayer sp ? RpgProgress.get(sp).classOrNull() != null
                    : player.level().isClientSide() && FMLEnvironment.dist.isClient() && ClientSide.hasClass();
            if (hasClass) {
                event.setCanceled(true);
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("message.fantasyrpg.shield_blocked").withStyle(ChatFormatting.RED), true);
                }
            }
            return;
        }
        if (player instanceof ServerPlayer sp && isSpear(stack) && RpgProgress.get(sp).classOrNull() == RpgClass.KNIGHT) {
            SkillRuntime.State st = SkillRuntime.state(sp);
            st.basicGuardPrev = st.basicGuardStart;
            st.basicGuardStart = SkillRuntime.now(sp);
        }
    }

    /** 지금 기본 가드 중인지 (기사, 창을 들어 올리는 중) */
    public static boolean guarding(ServerPlayer p) {
        return p.isUsingItem() && isSpear(p.getUseItem()) && RpgProgress.get(p).classOrNull() == RpgClass.KNIGHT;
    }

    /**
     * 받는 피해에 기본 가드 적용 (SkillEvents 에서, 스킬 완벽 방어가 켜져 있지 않을 때).
     * @return 줄인 피해. 패리면 이벤트를 취소하고 -1
     */
    public static float apply(ServerPlayer p, LivingIncomingDamageEvent event, LivingEntity attacker, float amount) {
        GuardSettings g = CombatManager.INSTANCE.guard();
        if (g == null || !guarding(p)) return amount;
        Vec3 src = event.getSource().getSourcePosition();
        if (src == null) return amount;
        Vec3 look = p.getViewVector(1.0F);
        if (!SkillMath.inCone(src.x - p.getX(), src.z - p.getZ(), look.x, look.z, g.frontAngle())) return amount;
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        if (g.isParry(now, st.basicGuardStart, st.basicGuardPrev)) {
            event.setCanceled(true);
            st.basicGuardStart = now - g.parryTicks() - 1;   // 같은 자세에서 두 번째 패리는 없음
            st.lastParryTick = now;
            CoreService.gain(p, g.parryCore());
            if (attacker != null) SkillTargets.slow(attacker, g.parryStunTicks(), 3);
            Vec3 f = p.position().add(look.multiply(1, 0, 1).normalize());
            p.serverLevel().sendParticles(ParticleTypes.CRIT, f.x, p.getY() + 1.2, f.z, 10, 0.3, 0.3, 0.3, 0.1);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.5F);
            return -1;
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 0.9F);
        return (float) g.guarded(amount);
    }

    /** 클라이언트 전용 확인 (전용 서버에서는 불리지 않음) */
    private static final class ClientSide {
        static boolean hasClass() {
            return !com.fantasyrpg.core.client.ClientRpgState.rpgClass.isEmpty();
        }
    }
}
