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
        int marks = (int) Math.floor(CoreService.value(p) + 1e-6);
        CoreService.spendUpTo(p, marks);
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
}
