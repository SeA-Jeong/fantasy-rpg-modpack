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
            c.hit(t, c.v("physical"), 0, mult, false);
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
}
