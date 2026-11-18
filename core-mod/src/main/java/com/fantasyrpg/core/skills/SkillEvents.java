package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.skilltree.SkillService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 스킬의 이벤트 처리:
 * 받는 피해(회피·완벽 방어·패리·불굴·신성 방벽·후퇴 사격·굳건함·마력 방벽·결계 반사),
 * 받은 뒤(수호 2단 패시브·불굴 3랭크), 틱(지속 효과 끝, 시간 제한 수정자, 예약 실행), 처치(연쇄 표식), 넉백(굳건함).
 */
public final class SkillEvents {
    private SkillEvents() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        SkillRuntime.runDue(event.getServer());
        if (event.getServer().getTickCount() % 200 == 0) {
            long now = event.getServer().getTickCount();
            SkillRuntime.KILL_WATCH.values().removeIf(w -> w.until() < now);
        }
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
        if (st.sanctEnd > 0 && now >= st.sanctEnd) {
            st.sanctEnd = -1;
            SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.C_SANCTUARY);
            if (l != null && p.isAlive()) ClericSkills.sanctuaryEnd(p, l, st.sanctReduced);
            st.sanctReduced = 0;
        }
        if (st.barrierEnd > 0 && now >= st.barrierEnd) st.barrierEnd = -1;
        SkillPassives.tickTimed(p);
    }

    /** 방어구 계산 전 들어오는 피해 (플레이어가 받을 때) */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill·공허
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity le && SkillTargets.single(le, p) ? le : null;

        // 도적 회피: 피해 0
        if (st.evadeEnd > now) {
            event.setCanceled(true);
            if (st.evadeFromSkill && !st.evadeSucceeded) {
                st.evadeSucceeded = true;
                SkillPassives.Learned evade = SkillPassives.learned(p, SkillIds.R_EVADE);
                if (evade != null) RogueSkills.evaded(p, evade, attacker);
            }
            return;
        }

        float amount = event.getAmount();

        // 기사 완벽 방어: 정면이면 감소, 처음 몇 틱이면 패리(피해 0)
        if (st.guardEnd > now) {
            SkillPassives.Learned guard = SkillPassives.learned(p, SkillIds.K_PERFECT_GUARD);
            Vec3 src = event.getSource().getSourcePosition();
            if (guard != null && src != null) {
                Vec3 look = p.getViewVector(1.0F);
                boolean front = SkillMath.inCone(src.x - p.getX(), src.z - p.getZ(), look.x, look.z, guard.v("front_angle"));
                if (front) {
                    if (now - st.guardStart <= guard.v("parry_ticks")) {
                        event.setCanceled(true);
                        st.guardEnd = now; // 패리 한 번으로 자세 끝
                        KnightSkills.parried(p, guard, attacker);
                        return;
                    }
                    amount = (float) SkillMath.reduced(amount, guard.v("reduction"));
                }
            }
        }
        // 전사 불굴
        if (st.indomitableEnd > now) {
            SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.W_INDOMITABLE);
            if (l != null) amount = (float) SkillMath.reduced(amount, l.v("reduction"));
        }
        // 전투사제 신성 방벽: 줄인 만큼 신성력
        if (st.sanctEnd > now) {
            SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.C_SANCTUARY);
            if (l != null) {
                float after = (float) SkillMath.reduced(amount, l.v("reduction"));
                double cut = amount - after;
                st.sanctReduced += cut;
                CoreService.gain(p, cut * l.v("core_per_damage"));
                amount = after;
            }
        }
        // 궁수 후퇴 사격 5랭크
        if (st.retreatGuardEnd > now) {
            SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.A_RETREAT_SHOT, 5);
            if (l != null) amount = (float) SkillMath.reduced(amount, l.v("guard_reduction"));
        }
        // 기사 굳건함 5랭크: 결의가 높으면 감소
        SkillPassives.Learned steadfast = SkillPassives.learned(p, SkillIds.K_STEADFAST, 5);
        if (steadfast != null && CoreService.value(p) >= steadfast.v("high_core_guard")) {
            amount = (float) SkillMath.reduced(amount, steadfast.v("high_core_reduction"));
        }
        // 마법사 마력 방벽: 일부를 마나로
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

        // 마법사 결계 충격 5랭크: 흡수 체력이 남아 있는 동안 공격자에게 반사
        if (st.barrierEnd > now && p.getAbsorptionAmount() > 0 && attacker != null) {
            SkillPassives.Learned barrier = SkillPassives.learned(p, SkillIds.M_BARRIER_SHOCK, 5);
            if (barrier != null && now - st.lastReflect >= barrier.v("reflect_interval")) {
                st.lastReflect = now;
                new Cast(p, barrier.node(), barrier.rank(), 0).hit(attacker, 0, barrier.v("reflect_magic"));
            }
        }
    }

    /** 실제로 피해를 받은 뒤: 수호 2단 패시브(코어·위기 반응), 불굴 3랭크 */
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getNewDamage() <= 0 || !p.isAlive()) return;
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        SkillPassives.Learned g = SkillPassives.anyOf(p, SkillIds.GUARD_PASSIVE);
        if (g != null) {
            CoreService.gain(p, g.v("hurt_core"));
            if (g.v("hurt_speed") > 0) {
                SkillPassives.timedAttribute(p, "hurt_speed", Attributes.MOVEMENT_SPEED, g.v("hurt_speed"),
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, (int) g.v("hurt_speed_ticks"));
            }
            if (g.rank() >= 3 && g.v("low_health") > 0 && now >= st.lowHealthReady
                    && p.getHealth() < p.getMaxHealth() * g.v("low_health")) {
                st.lowHealthReady = now + (long) g.v("low_cooldown_ticks");
                CoreService.gain(p, g.v("low_core"));
                if (g.v("low_heal") > 0) p.heal((float) g.v("low_heal"));
                if (g.v("low_speed") > 0) {
                    SkillPassives.timedAttribute(p, "low_speed", Attributes.MOVEMENT_SPEED, g.v("low_speed"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, (int) g.v("low_speed_ticks"));
                }
                if (g.v("low_evade_ticks") > 0) {
                    st.evadeEnd = now + (long) g.v("low_evade_ticks");
                    st.evadeFromSkill = false;
                }
                SkillPassives.grantAbsorption(p, "guard_passive", g.v("low_absorption"), (int) g.v("absorption_ticks"));
            }
        }
        if (st.indomitableEnd > now) {
            SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.W_INDOMITABLE, 3);
            if (l != null) CoreService.gain(p, l.v("hit_core"));
        }
    }

    /** 기사 굳건함 3랭크: 결의가 높으면 밀려나지 않음 */
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        SkillPassives.Learned l = SkillPassives.learned(p, SkillIds.K_STEADFAST, 3);
        if (l != null && CoreService.value(p) >= l.v("knockback_core")) event.setCanceled(true);
    }

    /** 도적 연쇄 표식 5랭크: 표식 단검을 맞은 적이 정해진 시간 안에 죽으면 쿨타임 초기화 */
    public static void onDeath(LivingDeathEvent event) {
        SkillRuntime.KillWatch w = SkillRuntime.KILL_WATCH.remove(event.getEntity().getUUID());
        if (w == null || event.getEntity().level().getServer() == null) return;
        ServerPlayer p = event.getEntity().level().getServer().getPlayerList().getPlayer(w.player());
        if (p == null || SkillRuntime.now(p) > w.until()) return;
        SkillRuntime.setCooldown(p, w.nodeId(), 0);
    }

    /** 접속·부활 때 */
    public static void refresh(ServerPlayer p) {
        SkillPassives.clearTimed(p);
        SkillRuntime.State st = SkillRuntime.state(p);
        st.indomitableEnd = -1;
        st.barrierEnd = -1;
        st.sanctEnd = -1;
        st.guardEnd = -1;
        st.evadeEnd = -1;
        st.retreatGuardEnd = -1;
        SkillRuntime.resendCooldowns(p);
    }
}
