package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.skilltree.SkillEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 마법사 1단 액티브 4종 (claude/전사마법사_스킬_초안.md 5절). 2단 패시브는 {@link SkillPassives}·{@link SkillEvents}.
 */
public final class MageSkills {
    private MageSkills() {
    }

    public static void register() {
        SkillEffects.register(SkillIds.M_SOLID_BOLTS, MageSkills::solidBolts);
        SkillEffects.register(SkillIds.M_ARCANE_BURST, MageSkills::arcaneBurst);
        SkillEffects.register(SkillIds.M_BARRIER_SHOCK, MageSkills::barrierShock);
        SkillEffects.register(SkillIds.M_BLINK, MageSkills::blink);
    }

    // ── 실체 마탄 ──
    private static boolean solidBolts(Cast c) {
        ServerPlayer p = c.player;
        int bolts = (int) Math.round(c.v("bolts"));
        int interval = (int) Math.max(1, c.v("interval_ticks"));
        Map<UUID, Integer> hits = new HashMap<>();
        for (int i = 0; i < bolts; i++) {
            final boolean last = i == bolts - 1;
            Runnable shot = () -> {
                if (p.isRemoved() || !p.isAlive()) return;
                LivingEntity first = solidBolt(c, true);
                if (first != null) hits.merge(first.getUUID(), 1, Integer::sum);
                // 3랭크: 모두 같은 적에 맞으면 밀쳐냄
                if (last && first != null && hits.getOrDefault(first.getUUID(), 0) >= bolts) {
                    SkillTargets.push(first, p.position(), c.v("knockback"));
                }
            };
            if (i == 0) shot.run();
            else SkillRuntime.schedule(p.server, i * interval, shot);
        }
        return true;
    }

    /** 마탄 한 발 (마탄 단련 5랭크에서도 씀). 처음 맞은 대상을 돌려줌 */
    static LivingEntity solidBolt(Cast c, boolean sound) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        List<LivingEntity> line = SkillTargets.line(p, range);
        boolean pierce = c.v("pierce") > 0;
        LivingEntity first = line.isEmpty() ? null : line.get(0);
        Vec3 stop = pierce || first == null ? SkillTargets.rayEnd(p, range) : first.getBoundingBox().getCenter();
        ServerLevel l = p.serverLevel();
        double len = stop.subtract(eye).length();
        for (double d = 1.0; d < len; d += 0.6) {
            Vec3 at = eye.add(look.scale(d));
            l.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        if (sound) l.playSound(null, p.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 0.5F, 1.6F);
        if (pierce) {
            for (LivingEntity t : line) c.hit(t, c.v("physical"), 0);
        } else if (first != null) {
            c.hit(first, c.v("physical"), 0);
        }
        return first;
    }

    // ── 비전 폭발 ──
    private static boolean arcaneBurst(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        LivingEntity aimed = SkillTargets.look(p, range);
        Vec3 center = aimed != null ? aimed.position().add(0, aimed.getBbHeight() / 2, 0) : SkillTargets.rayEnd(p, range);
        double radius = c.v("radius");
        burst(c, center, radius, c.v("magic"));
        double echo = c.v("echo_ratio");
        if (echo > 0) {
            SkillRuntime.schedule(p.server, (int) c.v("echo_delay"), () -> {
                if (!p.isRemoved() && p.level() == c.player.level()) burst(c, center, radius, c.v("magic") * echo);
            });
        }
        return true;
    }

    private static void burst(Cast c, Vec3 center, double radius, double magic) {
        ServerLevel l = c.player.serverLevel();
        for (LivingEntity t : SkillTargets.around(c.player, center, radius)) c.hit(t, 0, magic);
        l.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        l.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 40, radius / 2, radius / 3, radius / 2, 0.1);
        l.playSound(null, center.x, center.y, center.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // ── 결계 충격 ──
    private static boolean barrierShock(Cast c) {
        ServerPlayer p = c.player;
        double radius = c.v("radius");
        Vec3 center = p.position().add(0, 1, 0);
        for (LivingEntity t : SkillTargets.around(p, center, radius)) {
            c.hit(t, 0, c.v("magic"));
            SkillTargets.push(t, p.position(), c.v("knockback"));
            SkillTargets.slow(t, (int) c.v("slow_ticks"), 1);
        }
        int ticks = (int) c.v("absorption_ticks");
        SkillPassives.grantAbsorption(p, "barrier", c.v("absorption"), ticks);
        SkillRuntime.state(p).barrierEnd = c.v("reflect_magic") > 0 ? SkillRuntime.now(p) + ticks : -1;
        ServerLevel l = p.serverLevel();
        for (int i = 0; i < 32; i++) {
            double a = Math.PI * 2 * i / 32;
            l.sendParticles(ParticleTypes.END_ROD, p.getX() + Math.cos(a) * radius, p.getY() + 0.5, p.getZ() + Math.sin(a) * radius, 1, 0, 0.2, 0, 0.01);
        }
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    // ── 점멸 ──
    private static boolean blink(Cast c) {
        ServerPlayer p = c.player;
        double dist = c.v("distance");
        Vec3 start = p.position();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 best = start;
        AABB box = p.getBoundingBox();
        for (double d = 0.25; d <= dist + 1e-6; d += 0.25) {
            Vec3 at = start.add(look.scale(d));
            if (!p.level().noCollision(p, box.move(at.subtract(start)))) break;
            best = at;
        }
        if (best.distanceToSqr(start) < 1.0) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.fantasyrpg.skill.blocked")
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.PORTAL, start.x, start.y + 1, start.z, 30, 0.3, 0.6, 0.3, 0.3);
        p.teleportTo(best.x, best.y, best.z);
        p.resetFallDistance();
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, best.x, best.y + 1, best.z, 30, 0.3, 0.6, 0.3, 0.1);
        l.playSound(null, best.x, best.y, best.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.4F);
        double after = c.v("afterimage_magic");
        if (after > 0) {
            double r = c.v("afterimage_radius");
            Vec3 center = start.add(0, 1, 0);
            for (LivingEntity t : SkillTargets.around(p, center, r)) c.hit(t, 0, after);
            l.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 25, r / 2, 0.4, r / 2, 0.05);
        }
        return true;
    }
}
