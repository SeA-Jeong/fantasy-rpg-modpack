package com.fantasyrpg.core.skills;

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
 * 기사 1단 액티브 4종 (claude/4직업_스킬_초안.md 4절). 완벽 방어의 받는 피해 처리는 {@link SkillEvents}.
 */
public final class KnightSkills {
    private KnightSkills() {
    }

    public static void register() {
        SkillEffects.register(SkillIds.K_CHARGE, KnightSkills::charge);
        SkillEffects.register(SkillIds.K_MANA_SPEAR, KnightSkills::manaSpear);
        SkillEffects.register(SkillIds.K_PERFECT_GUARD, KnightSkills::perfectGuard);
        SkillEffects.register(SkillIds.K_RIPOSTE, KnightSkills::riposte);
    }

    // ── 창돌진 ──
    private static boolean charge(Cast c) {
        ServerPlayer p = c.player;
        double mult = c.empower(); // 5랭크: 거리·피해 강화
        double dist = c.v("distance") * (mult > 1 ? c.v("empower_distance_mult") : 1.0);
        Vec3 start = p.position();
        Vec3 end = SkillTargets.freeTravel(p, dist, true);
        if (end.distanceToSqr(start) < 1.0) {
            WarriorSkills.blocked(c);
            return false;
        }
        List<LivingEntity> hit = SkillTargets.sweep(p, start, end, 0.8);
        ServerLevel l = p.serverLevel();
        for (double d = 0; d < 1; d += 0.1) {
            Vec3 at = start.lerp(end, d);
            l.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        p.teleportTo(end.x, end.y, end.z);
        p.resetFallDistance();
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
        boolean first = true;
        for (LivingEntity t : hit) {
            c.hit(t, c.v("physical"), 0, mult, false);
            SkillTargets.push(t, start, c.v("knockback"));
            if (first) SkillTargets.slow(t, (int) c.v("stun_ticks"), 3);
            first = false;
        }
        SkillRuntime.state(p).lastChargeTick = SkillRuntime.now(p);
        return true;
    }

    // ── 마력 관통창 ──
    private static boolean manaSpear(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 end = SkillTargets.rayEnd(p, range);
        ServerLevel l = p.serverLevel();
        double len = end.subtract(eye).length();
        for (double d = 1.0; d < len; d += 0.5) {
            Vec3 at = eye.add(look.scale(d));
            l.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        l.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        List<LivingEntity> line = SkillTargets.line(p, range);
        for (LivingEntity t : line) {
            c.hit(t, c.v("physical"), c.v("magic"));
            SkillTargets.slow(t, (int) c.v("slow_ticks"), 1);
        }
        if (c.v("end_magic") > 0) {
            for (LivingEntity t : SkillTargets.around(p, end, c.v("end_radius"))) c.hit(t, 0, c.v("end_magic"));
            l.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        }
        // 마력 각인 3랭크: 2명 이상 맞히면 결의
        SkillPassives.Learned engrave = SkillPassives.learned(p, SkillIds.K_RUNE_ENGRAVE, 3);
        if (engrave != null && line.size() >= 2) com.fantasyrpg.core.classcore.CoreService.gain(p, engrave.v("multi_hit_core"));
        return true;
    }

    // ── 완벽 방어 ── (막기·패리 판정은 SkillEvents.onIncomingDamage)
    private static boolean perfectGuard(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        st.guardStart = now;
        st.guardEnd = now + (long) c.v("duration_ticks");
        ServerLevel l = p.serverLevel();
        Vec3 front = p.position().add(p.getViewVector(1.0F).multiply(1, 0, 1).normalize().scale(1.0));
        l.sendParticles(ParticleTypes.END_ROD, front.x, p.getY() + 1, front.z, 12, 0.4, 0.5, 0.4, 0.01);
        l.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 패리 성공 (SkillEvents에서): 결의, 공격자 경직, 3랭크 쿨타임 절반, 5랭크 반격, 전투 리듬 5랭크 */
    static void parried(ServerPlayer p, SkillPassives.Learned guard, LivingEntity attacker) {
        SkillRuntime.State st = SkillRuntime.state(p);
        st.lastParryTick = SkillRuntime.now(p);
        com.fantasyrpg.core.classcore.CoreService.gain(p, guard.v("parry_core"));
        if (attacker != null) {
            SkillTargets.slow(attacker, (int) guard.v("parry_stun_ticks"), 3);
            if (guard.v("counter_physical") > 0) new Cast(p, guard.node(), guard.rank(), 0).hit(attacker, guard.v("counter_physical"), 0);
        }
        if (guard.v("parry_cooldown_ratio") > 0) {
            int left = SkillRuntime.cooldownLeft(p, guard.node().id());
            SkillRuntime.setCooldown(p, guard.node().id(), (int) Math.round(left * guard.v("parry_cooldown_ratio")));
        }
        SkillPassives.Learned rhythm = SkillPassives.learned(p, SkillIds.K_BATTLE_RHYTHM, 5);
        if (rhythm != null) st.forceCritBasic = true;
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY(1.0), p.getZ(), 20, 0.4, 0.4, 0.4, 0.3);
        l.playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.7F, 1.8F);
    }

    // ── 반격 찌르기 ──
    private static boolean riposte(Cast c) {
        ServerPlayer p = c.player;
        LivingEntity t = SkillTargets.look(p, c.v("range"));
        if (t == null) {
            WarriorSkills.noTarget(c);
            return false;
        }
        boolean afterParry = SkillRuntime.now(p) - SkillRuntime.state(p).lastParryTick <= c.v("parry_window_ticks");
        double mult = afterParry ? c.v("parry_mult") : 1.0;
        if (afterParry) c.free = true;
        int hits = (int) Math.round(c.v("hits"));
        for (int i = 0; i < hits; i++) {
            Runnable thrust = () -> {
                if (p.isRemoved() || !p.isAlive() || !t.isAlive() || t.distanceTo(p) > c.v("range") + 1.5) return;
                c.hit(t, c.v("physical"), 0, mult, false);
                p.serverLevel().sendParticles(afterParry ? ParticleTypes.CRIT : ParticleTypes.ENCHANTED_HIT,
                        t.getX(), t.getY(0.6), t.getZ(), 10, 0.2, 0.2, 0.2, 0.2);
                p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8F, 1.3F);
            };
            if (i == 0) thrust.run();
            else SkillRuntime.schedule(p.server, i * 5, thrust);
        }
        return true;
    }
}
