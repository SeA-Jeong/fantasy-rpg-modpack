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

import java.util.List;

/**
 * 궁수 1단 액티브 4종 (claude/4직업_스킬_초안.md 6절). 스킬 화살은 즉시 명중 + 입자(실제 투사체 아님).
 */
public final class ArcherSkills {
    private ArcherSkills() {
    }

    public static void register() {
        SkillEffects.register(SkillIds.A_POWER_SHOT, ArcherSkills::powerShot);
        SkillEffects.register(SkillIds.A_ARROW_RAIN, ArcherSkills::arrowRain);
        SkillEffects.register(SkillIds.A_TRAP, ArcherSkills::trap);
        SkillEffects.register(SkillIds.A_LEAP_SHOT, ArcherSkills::leapShot);
        SkillEffects.register(SkillIds.A_PIERCING_SHOT, ArcherSkills::piercingShot);
        SkillEffects.register(SkillIds.A_ELEMENTAL_ARROW, ArcherSkills::elementalArrow);
        SkillEffects.register(SkillIds.A_RETREAT_SHOT, ArcherSkills::retreatShot);
        SkillEffects.register(SkillIds.A_RAPID_FIRE, ArcherSkills::rapidFire);
    }

    private static void trail(ServerPlayer p, Vec3 end, net.minecraft.core.particles.ParticleOptions particle) {
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 dir = end.subtract(eye);
        double len = dir.length();
        dir = dir.normalize();
        for (double d = 1.0; d < len; d += 0.6) {
            Vec3 at = eye.add(dir.scale(d));
            l.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    // ── 관통 사격 ──
    private static boolean piercingShot(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        double mult = c.empower(); // 5랭크
        List<LivingEntity> line = SkillTargets.line(p, range);
        trail(p, SkillTargets.rayEnd(p, range), ParticleTypes.CRIT);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.7F);
        boolean first = true;
        for (LivingEntity t : line) {
            c.hit(t, c.v("physical"), c.v("magic"), mult, false); // magic: 혼합 노드 원소 관통
            if (first) SkillTargets.slow(t, (int) c.v("stun_ticks"), 3);
            first = false;
        }
        return true;
    }

    // ── 원소 화살 ──
    private static boolean elementalArrow(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        LivingEntity aimed = SkillTargets.look(p, range);
        Vec3 center = aimed != null ? aimed.position().add(0, aimed.getBbHeight() / 2, 0) : SkillTargets.rayEnd(p, range);
        double radius = c.v("radius");
        trail(p, center, ParticleTypes.FLAME);
        ServerLevel l = p.serverLevel();
        for (LivingEntity t : SkillTargets.around(p, center, radius)) {
            c.hit(t, 0, c.v("magic"));
            SkillTargets.slow(t, (int) c.v("slow_ticks"), 2);
        }
        l.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        l.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 25, radius / 2, 0.5, radius / 2, 0.05);
        l.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        // 5랭크: 불꽃 장판
        double field = c.v("field_magic");
        if (field > 0) {
            int pulses = (int) Math.round(c.v("field_pulses"));
            int interval = (int) Math.max(1, c.v("field_interval"));
            for (int i = 1; i <= pulses; i++) {
                SkillRuntime.schedule(p.server, i * interval, () -> {
                    if (p.isRemoved()) return;
                    for (LivingEntity t : SkillTargets.around(p, center, radius)) c.hit(t, 0, field);
                    p.serverLevel().sendParticles(ParticleTypes.FLAME, center.x, center.y - 0.5, center.z, 20, radius / 2, 0.1, radius / 2, 0.01);
                });
            }
        }
        return true;
    }

    // ── 후퇴 사격 ──
    private static boolean retreatShot(Cast c) {
        ServerPlayer p = c.player;
        for (LivingEntity t : SkillTargets.cone(p, c.v("range"), c.v("angle"))) {
            c.hit(t, c.v("physical"), 0);
            SkillTargets.push(t, p.position(), c.v("knockback"));
            SkillTargets.slow(t, (int) c.v("slow_ticks"), 1);
        }
        CoreService.gain(p, c.v("cast_core"));
        SkillTargets.leapBack(p, c.v("leap"));
        if (c.v("guard_ticks") > 0) SkillRuntime.state(p).retreatGuardEnd = SkillRuntime.now(p) + (long) c.v("guard_ticks");
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY(1.0), p.getZ(), 3, 0.5, 0.2, 0.5, 0);
        l.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    // ── 이동 속사 ──
    private static boolean rapidFire(Cast c) {
        ServerPlayer p = c.player;
        int shots = (int) Math.round(c.v("shots"));
        int interval = (int) Math.max(1, c.v("interval_ticks"));
        double range = c.v("range");
        int[] landed = {0};
        for (int i = 0; i < shots; i++) {
            final boolean last = i == shots - 1;
            Runnable shot = () -> {
                if (p.isRemoved() || !p.isAlive()) return;
                LivingEntity t = SkillTargets.look(p, range);
                trail(p, t != null ? t.getBoundingBox().getCenter() : SkillTargets.rayEnd(p, range), ParticleTypes.CRIT);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.6F, 1.5F);
                if (t != null) {
                    c.hit(t, c.v("physical"), 0);
                    landed[0]++;
                }
                if (last && landed[0] >= shots && c.v("refund_core") > 0) CoreService.gain(p, c.v("refund_core"));
            };
            if (i == 0) shot.run();
            else SkillRuntime.schedule(p.server, i * interval, shot);
        }
        return true;
    }

    // ── 3단: 강사 (집중을 모두 씀) ──
    private static boolean powerShot(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        double focus = CoreService.value(p);
        CoreService.spendUpTo(p, focus);
        double mult = 1.0 + Math.floor(focus / 10.0) * c.v("per_ten_bonus");
        if (c.v("full_mult") > 0 && focus >= c.v("full_focus")) mult *= c.v("full_mult");
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.5F);
        if (c.v("pierce") > 0) {
            trail(p, SkillTargets.rayEnd(p, range), ParticleTypes.CRIT);
            for (LivingEntity t : SkillTargets.line(p, range)) c.hit(t, c.v("physical"), 0, mult, false);
        } else {
            LivingEntity t = SkillTargets.look(p, range);
            trail(p, t != null ? t.getBoundingBox().getCenter() : SkillTargets.rayEnd(p, range), ParticleTypes.CRIT);
            if (t != null) c.hit(t, c.v("physical"), 0, mult, false);
        }
        return true;
    }

    // ── 3단: 원소 화살비 ──
    private static boolean arrowRain(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        LivingEntity aimed = SkillTargets.look(p, range);
        Vec3 center = aimed != null ? aimed.position().add(0, 0.5, 0) : SkillTargets.rayEnd(p, range);
        double radius = c.v("radius");
        int pulses = (int) Math.round(c.v("pulses"));
        int interval = (int) Math.max(1, c.v("pulse_interval"));
        for (int i = 0; i < pulses; i++) {
            final boolean last = i == pulses - 1;
            Runnable volley = () -> {
                if (p.isRemoved()) return;
                for (LivingEntity t : SkillTargets.around(p, center, radius)) {
                    c.hit(t, 0, c.v("magic"));
                    SkillTargets.slow(t, (int) c.v("slow_ticks"), 1);
                }
                ServerLevel l = p.serverLevel();
                l.sendParticles(ParticleTypes.CRIT, center.x, center.y + 3, center.z, 12, radius / 2, 1.5, radius / 2, 0.5);
                l.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 6, radius / 2, 0.1, radius / 2, 0.01);
                l.playSound(null, center.x, center.y, center.z, SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.5F, 1.2F);
                if (last && c.v("end_magic") > 0) {
                    for (LivingEntity t : SkillTargets.around(p, center, radius)) c.hit(t, 0, c.v("end_magic"));
                    l.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
                }
            };
            if (i == 0) volley.run();
            else SkillRuntime.schedule(p.server, i * interval, volley);
        }
        return true;
    }

    // ── 3단: 견제 덫 ──
    private static boolean trap(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        int seq = ++st.trapSeq; // 새 덫을 놓으면 예전 덫은 사라짐
        Vec3 at = p.position().add(0, 0.5, 0);
        int life = (int) c.v("life_ticks");
        int step = 4;
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (int t = 0; t <= life; t += step) {
            SkillRuntime.schedule(p.server, Math.max(1, t), () -> {
                if (p.isRemoved() || st.trapSeq != seq || p.level() != c.player.level()) return;
                ServerLevel l = p.serverLevel();
                l.sendParticles(ParticleTypes.CRIT, at.x, at.y - 0.4, at.z, 2, 0.3, 0, 0.3, 0);
                if (SkillTargets.around(p, at, c.v("detect_radius")).isEmpty()) return;
                st.trapSeq++; // 한 번만 터짐
                for (LivingEntity e : SkillTargets.around(p, at, c.v("blast_radius"))) {
                    c.hit(e, c.v("physical"), 0);
                    SkillTargets.slow(e, (int) c.v("root_ticks"), 5);
                }
                CoreService.gain(p, c.v("trigger_core"));
                l.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                l.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.5F);
            });
        }
        return true;
    }

    // ── 3단: 도약 사격 ──
    private static boolean leapShot(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        p.setDeltaMovement(p.getDeltaMovement().x, c.v("jump"), p.getDeltaMovement().z);
        p.hurtMarked = true;
        p.resetFallDistance();
        int arrows = (int) Math.round(c.v("arrows"));
        int hits = 0;
        for (int i = 0; i < arrows; i++) {
            LivingEntity t = SkillTargets.look(p, range);
            trail(p, t != null ? t.getBoundingBox().getCenter() : SkillTargets.rayEnd(p, range), ParticleTypes.CRIT);
            if (t != null) {
                c.hit(t, c.v("physical"), 0);
                hits++;
            }
        }
        CoreService.gain(p, c.v("cast_core"));
        if (c.v("all_hit_cooldown_mult") > 0 && hits >= arrows) c.cooldownMult = c.v("all_hit_cooldown_mult");
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.3F);
        return true;
    }
}
