package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.skilltree.SkillEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 도적 1단 액티브 4종 (claude/4직업_스킬_초안.md 5절). 코어 "표식"은 0~5개.
 * 그림자 회피의 받는 피해 처리는 {@link SkillEvents}.
 */
public final class RogueSkills {
    private RogueSkills() {
    }

    public static void register() {
        SkillEffects.register(SkillIds.R_ASSASSINATE, RogueSkills::assassinate);
        SkillEffects.register(SkillIds.R_SHADOW_VORTEX, RogueSkills::shadowVortex);
        SkillEffects.register(SkillIds.R_SHADOW_COUNTER, RogueSkills::shadowCounter);
        SkillEffects.register(SkillIds.R_MARK_BURST, RogueSkills::markBurst);
        SkillEffects.register(SkillIds.R_VITAL_FLURRY, RogueSkills::vitalFlurry);
        SkillEffects.register(SkillIds.R_SHADOW_BLADES, RogueSkills::shadowBlades);
        SkillEffects.register(SkillIds.R_EVADE, RogueSkills::evade);
        SkillEffects.register(SkillIds.R_CHAIN_MARK, RogueSkills::chainMark);
    }

    // ── 급소 난무: 표식을 모두 써서 강화 ──
    private static boolean vitalFlurry(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        LivingEntity t = SkillTargets.look(p, range);
        if (t == null) {
            WarriorSkills.noTarget(c);
            return false;
        }
        int marks = consumeMarks(p);
        double mult = 1.0 + marks * c.v("per_mark_bonus");
        boolean back = c.v("behind_crit") > 0 && SkillPassives.behind(p, t);
        int hits = (int) Math.round(c.v("hits"));
        if (marks >= c.v("full_marks") && c.v("full_hits") > hits) hits = (int) Math.round(c.v("full_hits"));
        for (int i = 0; i < hits; i++) {
            Runnable stab = () -> {
                if (p.isRemoved() || !p.isAlive() || !t.isAlive() || t.distanceTo(p) > range + 1.5) return;
                c.hit(t, c.v("physical"), 0, mult, back);
                p.serverLevel().sendParticles(ParticleTypes.CRIT, t.getX(), t.getY(0.6), t.getZ(), 6, 0.2, 0.2, 0.2, 0.2);
                p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6F, 1.5F);
            };
            if (i == 0) stab.run();
            else SkillRuntime.schedule(p.server, i * 3, stab);
        }
        return true;
    }

    // ── 그림자 칼날: 부채꼴 단검 ──
    private static boolean shadowBlades(Cast c) {
        ServerPlayer p = c.player;
        int knives = (int) Math.round(c.v("knives"));
        double spread = c.v("spread");
        double range = c.v("range");
        ServerLevel l = p.serverLevel();
        SkillRuntime.State st = SkillRuntime.state(p);
        for (int i = 0; i < knives; i++) {
            double angle = knives == 1 ? 0 : -spread / 2 + spread * i / (knives - 1);
            Vec3 dir = SkillTargets.rotatedLook(p, angle);
            LivingEntity t = SkillTargets.firstAlong(p, dir, range);
            Vec3 eye = p.getEyePosition();
            double len = t == null ? range : t.position().distanceTo(p.position());
            for (double d = 1.0; d < len; d += 0.7) {
                Vec3 at = eye.add(dir.scale(d));
                l.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            if (t != null) {
                c.hit(t, c.v("physical"), c.v("magic"));
                CoreService.gain(p, c.v("hit_core"));
                if (c.v("shadow_ticks") > 0) st.shadowMarks.put(t.getUUID(), SkillRuntime.now(p) + (long) c.v("shadow_ticks"));
            }
        }
        st.shadowMarks.values().removeIf(end -> end < SkillRuntime.now(p));
        l.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }

    // ── 그림자 회피 ── (회피 성공 처리는 SkillEvents)
    private static boolean evade(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.evadeEnd = SkillRuntime.now(p) + (long) c.v("evade_ticks");
        st.evadeFromSkill = true;
        st.evadeSucceeded = false;
        SkillTargets.leapBack(p, c.v("leap"));
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(0.5), p.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
        l.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    }

    /** 스킬 회피 성공 (SkillEvents에서, 회피마다 한 번) */
    static void evaded(ServerPlayer p, SkillPassives.Learned evade, LivingEntity attacker) {
        CoreService.gain(p, evade.v("evade_core"));
        SkillRuntime.state(p).lastEvadeSuccessTick = SkillRuntime.now(p);
        // 그림자 몸놀림 5랭크: 이동 속도
        SkillPassives.timedAttribute(p, "evade_speed", net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                evade.v("evade_speed"), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 40);
        if (attacker != null && evade.v("counter_physical") > 0) {
            new Cast(p, evade.node(), evade.rank(), 0).hit(attacker, evade.v("counter_physical"), 0);
        }
        if (evade.v("evade_cooldown_ratio") > 0) {
            int left = SkillRuntime.cooldownLeft(p, evade.node().id());
            SkillRuntime.setCooldown(p, evade.node().id(), (int) Math.round(left * evade.v("evade_cooldown_ratio")));
        }
        p.serverLevel().sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY(1.0), p.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
    }

    // ── 연쇄 표식 ──
    private static boolean chainMark(Cast c) {
        ServerPlayer p = c.player;
        LivingEntity t = SkillTargets.look(p, c.v("range"));
        if (t == null) {
            WarriorSkills.noTarget(c);
            return false;
        }
        boolean many = CoreService.value(p) >= c.v("bonus_threshold") && c.v("bonus_threshold") > 0;
        c.hit(t, c.v("physical"), 0);
        CoreService.gain(p, c.v("mark_gain") + (many ? c.v("bonus_marks") : 0));
        if (c.v("kill_reset_ticks") > 0) {
            SkillRuntime.KILL_WATCH.put(t.getUUID(),
                    new SkillRuntime.KillWatch(p.getUUID(), c.node.id(), SkillRuntime.now(p) + (long) c.v("kill_reset_ticks")));
        }
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY(0.8), t.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
        l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.8F);
        return true;
    }

    /** 표식을 모두 씀. 급소 숙달 5랭크면 쓴 표식의 일부를 돌려받음. 쓴 개수 */
    static int consumeMarks(ServerPlayer p) {
        int marks = (int) Math.floor(CoreService.value(p) + 1e-6);
        CoreService.spendUpTo(p, marks);
        int back = (int) Math.floor(marks * SkillPassives.sum(p, "mark_refund_ratio"));
        if (back > 0) CoreService.gain(p, back);
        return marks;
    }

    // ── 3단: 암살 ──
    private static boolean assassinate(Cast c) {
        ServerPlayer p = c.player;
        LivingEntity t = SkillTargets.look(p, c.v("range"));
        if (t == null) {
            WarriorSkills.noTarget(c);
            return false;
        }
        int marks = consumeMarks(p);
        double mult = 1.0 + marks * c.v("per_mark_bonus");
        if (t.getHealth() <= t.getMaxHealth() * c.v("execute_health")) mult *= c.v("execute_mult");
        boolean back = c.v("behind_crit") > 0 && SkillPassives.behind(p, t);
        c.hit(t, c.v("physical"), 0, mult, back);
        if (!t.isAlive() || t.isDeadOrDying()) CoreService.gain(p, c.v("kill_refund"));
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY(0.7), t.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
        l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    // ── 3단: 그림자 소용돌이 ──
    private static boolean shadowVortex(Cast c) {
        ServerPlayer p = c.player;
        int pulses = (int) Math.round(c.v("pulses"));
        int interval = (int) Math.max(1, c.v("pulse_interval"));
        int[] gained = {0};
        for (int i = 0; i < pulses; i++) {
            final boolean last = i == pulses - 1;
            Runnable pulse = () -> {
                if (p.isRemoved() || !p.isAlive()) return;
                double r = c.v("radius");
                for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), r)) {
                    c.hit(t, 0, c.v("magic"));
                    if (gained[0] < c.v("max_marks")) {
                        CoreService.gain(p, 1);
                        gained[0]++;
                    }
                }
                p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(0.5), p.getZ(), 20, r / 2, 0.3, r / 2, 0.02);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.6F);
                if (last && c.v("end_evade_ticks") > 0) {
                    SkillRuntime.State st = SkillRuntime.state(p);
                    st.evadeEnd = SkillRuntime.now(p) + (long) c.v("end_evade_ticks");
                    st.evadeFromSkill = false;
                }
            };
            if (i == 0) pulse.run();
            else SkillRuntime.schedule(p.server, i * interval, pulse);
        }
        return true;
    }

    // ── 3단: 그림자 반격 ──
    private static boolean shadowCounter(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        double mult = SkillRuntime.now(p) - st.lastEvadeSuccessTick <= c.v("evade_window_ticks") ? c.v("evade_mult") : 1.0;
        for (LivingEntity t : SkillTargets.cone(p, c.v("range"), c.v("angle"))) {
            c.hit(t, c.v("physical"), 0, mult, false);
            SkillTargets.slow(t, (int) c.v("slow_ticks"), 1);
        }
        CoreService.gain(p, c.v("cast_core"));
        SkillTargets.leapBack(p, c.v("leap"));
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY(1.0), p.getZ(), 3, 0.5, 0.2, 0.5, 0);
        l.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY(0.5), p.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.3F);
        return true;
    }

    // ── 3단: 표식 폭발 ──
    private static boolean markBurst(Cast c) {
        ServerPlayer p = c.player;
        int marks = consumeMarks(p);
        double r = marks >= c.v("full_marks") && c.v("full_radius") > 0 ? c.v("full_radius") : c.v("radius");
        double phys = c.v("physical") + marks * c.v("per_mark_physical");
        int kills = 0;
        for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), r)) {
            c.hit(t, phys, 0);
            if (!t.isAlive() || t.isDeadOrDying()) kills++;
        }
        CoreService.gain(p, kills * c.v("kill_marks"));
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY(1.0), p.getZ(), 20, r / 2, 0.5, r / 2, 0.1);
        l.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(0.5), p.getZ(), 1, 0, 0, 0, 0);
        l.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.8F);
        return true;
    }
}
