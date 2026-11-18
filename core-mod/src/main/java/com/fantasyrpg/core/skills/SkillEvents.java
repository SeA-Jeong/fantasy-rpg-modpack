package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.skilltree.SkillService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 스킬의 이벤트 처리: 받는 피해(불굴·마력 방벽·결계 반사), 받은 뒤(투지·불굴 3랭크), 틱(지속 효과 끝, 흡수 체력, 예약 실행).
 */
public final class SkillEvents {
    private SkillEvents() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        SkillRuntime.runDue(event.getServer());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        if (st.indomitableEnd > 0 && now >= st.indomitableEnd) {
            st.indomitableEnd = -1;
            SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.W_INDOMITABLE, 5);
            if (l != null && p.isAlive()) WarriorSkills.indomitableEnd(p, l);
        }
        if (st.barrierEnd > 0 && now >= st.barrierEnd) st.barrierEnd = -1;
        SkillPassives.tickAbsorption(p);
    }

    /** 방어구 계산 전 들어오는 피해 (플레이어가 받을 때) */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill·공허
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        float amount = event.getAmount();

        // 불굴: 지속 중 감소
        if (st.indomitableEnd > now) {
            SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.W_INDOMITABLE);
            if (l != null) amount = (float) SkillMath.reduced(amount, l.v("reduction"));
        }
        // 마력 방벽: 일부를 마나로
        SkillPassives.Learned ward = SkillPassives.learned(p, SkillIds.M_MANA_WARD);
        if (ward != null && amount > 0) {
            double ratio = ward.v("ratio");
            if (ward.rank() >= 3 && p.getHealth() <= p.getMaxHealth() * ward.v("low_health")) ratio *= ward.v("low_mult");
            SkillMath.Ward w = SkillMath.manaWard(amount, ratio, CoreService.value(p), ward.v("mana_per_hp"));
            if (w.absorbed() > 0) {
                CoreService.spendUpTo(p, w.manaSpent());
                amount -= (float) w.absorbed();
                if (ward.rank() >= 5 && ward.v("auto_barrier") > 0 && CoreService.value(p) < 0.5) {
                    SkillService.castFree(p, SkillIds.M_BARRIER_SHOCK);
                }
            }
        }
        event.setAmount(Math.max(0f, amount));

        // 결계 충격 5랭크: 흡수 체력이 남아 있는 동안 공격자에게 반사
        if (st.barrierEnd > now && p.getAbsorptionAmount() > 0
                && event.getSource().getEntity() instanceof LivingEntity attacker && SkillTargets.single(attacker, p)) {
            SkillPassives.Learned barrier = SkillPassives.learned(p, SkillIds.M_BARRIER_SHOCK, 5);
            if (barrier != null && now - st.lastReflect >= barrier.v("reflect_interval")) {
                st.lastReflect = now;
                new Cast(p, barrier.node(), barrier.rank(), 0).hit(attacker, 0, barrier.v("reflect_magic"));
            }
        }
    }

    /** 실제로 피해를 받은 뒤 */
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getNewDamage() <= 0 || !p.isAlive()) return;
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        SkillPassives.Learned spirit = SkillPassives.learned(p, SkillIds.W_FIGHTING_SPIRIT);
        if (spirit != null) {
            CoreService.gain(p, spirit.v("hurt_core"));
            if (spirit.rank() >= 3 && now >= st.lowHealthReady && p.getHealth() < p.getMaxHealth() * spirit.v("low_health")) {
                st.lowHealthReady = now + (long) spirit.v("low_cooldown_ticks");
                CoreService.gain(p, spirit.v("low_core"));
                SkillPassives.grantAbsorption(p, "fighting_spirit", spirit.v("low_absorption"), (int) spirit.v("absorption_ticks"));
            }
        }
        if (st.indomitableEnd > now) {
            SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.W_INDOMITABLE, 3);
            if (l != null) CoreService.gain(p, l.v("hit_core"));
        }
    }

    /** 접속·부활 때 */
    public static void refresh(ServerPlayer p) {
        SkillPassives.clearAbsorption(p);
        SkillRuntime.State st = SkillRuntime.state(p);
        st.indomitableEnd = -1;
        st.barrierEnd = -1;
        SkillRuntime.resendCooldowns(p);
    }
}
