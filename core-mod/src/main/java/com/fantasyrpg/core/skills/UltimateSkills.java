package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.skilltree.SkillEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 6직업 5단 궁극기 24종 (claude/6직업_궁극기_초안.md). B키 칸에 하나만 장착.
 * 지속형 궁극기의 받는 피해·끝날 때 처리는 {@link SkillEvents}.
 */
public final class UltimateSkills {
    private UltimateSkills() {
    }

    public static void register() {
        SkillEffects.register(SkillIds.U_W_RUIN, UltimateSkills::ruin);
        SkillEffects.register(SkillIds.U_W_RELEASE, UltimateSkills::release);
        SkillEffects.register(SkillIds.U_W_SHOUT, UltimateSkills::shout);
        SkillEffects.register(SkillIds.U_W_THOUSAND, UltimateSkills::thousandBlades);
        SkillEffects.register(SkillIds.U_M_BOLT_STORM, UltimateSkills::boltStorm);
        SkillEffects.register(SkillIds.U_M_METEOR, UltimateSkills::meteor);
        SkillEffects.register(SkillIds.U_M_ABSOLUTE, UltimateSkills::absoluteBarrier);
        SkillEffects.register(SkillIds.U_M_TIME, UltimateSkills::timeAccel);
        SkillEffects.register(SkillIds.U_K_THUNDER, UltimateSkills::thunderCharge);
        SkillEffects.register(SkillIds.U_K_LANCE, UltimateSkills::holyLance);
        SkillEffects.register(SkillIds.U_K_WALL, UltimateSkills::rampart);
        SkillEffects.register(SkillIds.U_K_DANCE, UltimateSkills::spearDance);
        SkillEffects.register(SkillIds.U_R_EXECUTE, UltimateSkills::execute);
        SkillEffects.register(SkillIds.U_R_CLONE, UltimateSkills::shadowClone);
        SkillEffects.register(SkillIds.U_R_VEIL, UltimateSkills::shadowVeil);
        SkillEffects.register(SkillIds.U_R_CHAIN, UltimateSkills::chainExecution);
        SkillEffects.register(SkillIds.U_A_SKYPIERCER, UltimateSkills::skypiercer);
        SkillEffects.register(SkillIds.U_A_STORM, UltimateSkills::elementStorm);
        SkillEffects.register(SkillIds.U_A_VANISH, UltimateSkills::vanish);
        SkillEffects.register(SkillIds.U_A_DELUGE, UltimateSkills::arrowDeluge);
        SkillEffects.register(SkillIds.U_C_HAMMER, UltimateSkills::divineHammer);
        SkillEffects.register(SkillIds.U_C_SANCTUM, UltimateSkills::sanctum);
        SkillEffects.register(SkillIds.U_C_PRAYER, UltimateSkills::prayer);
        SkillEffects.register(SkillIds.U_C_RADIANCE, UltimateSkills::radiance);
    }

    // ── 공통 ──

    private static LivingEntity target(Cast c) {
        LivingEntity t = SkillTargets.look(c.player, c.v("range"));
        if (t == null) WarriorSkills.noTarget(c);
        return t;
    }

    private static Vec3 point(Cast c) {
        LivingEntity aimed = SkillTargets.look(c.player, c.v("range"));
        return aimed != null ? aimed.position().add(0, 0.5, 0) : SkillTargets.rayEnd(c.player, c.v("range"));
    }

    private static void ring(ServerLevel l, Vec3 at, double r, net.minecraft.core.particles.ParticleOptions particle) {
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            l.sendParticles(particle, at.x + Math.cos(a) * r, at.y + 0.3, at.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.01);
        }
    }

    /** 코어를 모두 쓰고 그 양에 따른 배율 (10당 per_ten) */
    private static double spendAll(Cast c) {
        double core = CoreService.value(c.player);
        CoreService.spendUpTo(c.player, core);
        return core;
    }

    private static void repeat(Cast c, int times, int interval, java.util.function.IntConsumer body) {
        for (int i = 0; i < times; i++) {
            final int idx = i;
            Runnable r = () -> {
                if (c.player.isRemoved() || !c.player.isAlive()) return;
                body.accept(idx);
            };
            if (i == 0) r.run();
            else SkillRuntime.schedule(c.player.server, i * interval, r);
        }
    }

    // ── 전사 ──

    private static boolean ruin(Cast c) {
        LivingEntity t = target(c);
        if (t == null) return false;
        double core = spendAll(c);
        double mult = 1.0 + Math.floor(core / 10.0) * c.v("per_ten_bonus");
        c.hit(t, c.v("physical"), 0, mult, false);
        for (LivingEntity e : SkillTargets.around(c.player, t.position().add(0, 1, 0), c.v("splash_radius"))) {
            if (e != t) c.hit(e, c.v("physical"), 0, mult * c.v("splash_ratio"), false);
        }
        ServerLevel l = c.player.serverLevel();
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, t.getX(), t.getY(), t.getZ(), 1, 0, 0, 0, 0);
        l.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    private static boolean release(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.releaseEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.releaseMagic = c.v("basic_magic");
        for (String effect : new String[]{SkillIds.W_MANA_SLASH, SkillIds.W_BLADE_STORM}) {
            SkillPassives.Learned l = SkillPassives.learned(p, effect);
            if (l != null) SkillRuntime.setCooldown(p, l.node().id(), 0);
        }
        p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY(1.0), p.getZ(), 60, 0.6, 0.8, 0.6, 1.0);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /** 마검 해방이 끝날 때 */
    static void releaseEnd(ServerPlayer p, SkillPassives.Learned l) {
        Cast c = new Cast(p, l.node(), l.rank(), 0);
        double r = l.v("end_radius");
        for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), r)) c.hit(t, 0, l.v("end_magic"));
        ring(p.serverLevel(), p.position(), r, ParticleTypes.ENCHANTED_HIT);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    private static boolean shout(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.shoutEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.shoutHits = 0;
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY(1.0), p.getZ(), 50, 0.6, 0.8, 0.6, 0.3);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    /** 불멸의 함성이 끝날 때 */
    static void shoutEnd(ServerPlayer p, SkillPassives.Learned l, int hits) {
        Cast c = new Cast(p, l.node(), l.rank(), 0);
        double phys = l.v("end_physical") + Math.min(l.v("per_hit_cap"), hits * l.v("per_hit_physical"));
        double r = l.v("end_radius");
        for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), r)) c.hit(t, phys, 0);
        ring(p.serverLevel(), p.position(), r, ParticleTypes.SWEEP_ATTACK);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static boolean thousandBlades(Cast c) {
        LivingEntity t = target(c);
        if (t == null) return false;
        int hits = (int) Math.round(c.v("hits"));
        double range = c.v("range");
        repeat(c, hits, (int) c.v("interval_ticks"), i -> {
            if (!t.isAlive() || t.distanceTo(c.player) > range + 2) return;
            c.hit(t, c.v("physical"), 0, 1.0, i == hits - 1);
            c.player.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY(0.6), t.getZ(), 1, 0.3, 0.3, 0.3, 0);
            if (i % 3 == 0) c.player.serverLevel().playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4F, 1.6F);
        });
        return true;
    }

    // ── 마법사 ──

    private static boolean boltStorm(Cast c) {
        Cast bolt = c;
        repeat(c, (int) Math.round(c.v("bolts")), (int) c.v("interval_ticks"), i -> MageSkills.solidBolt(bolt, i % 2 == 0));
        return true;
    }

    private static boolean meteor(Cast c) {
        Vec3 at = point(c);
        ServerPlayer p = c.player;
        double r = c.v("radius");
        ring(p.serverLevel(), at, r, ParticleTypes.FLAME);
        SkillRuntime.schedule(p.server, (int) c.v("delay_ticks"), () -> {
            if (p.isRemoved()) return;
            for (LivingEntity t : SkillTargets.around(p, at, r)) {
                c.hit(t, 0, c.v("magic"));
                SkillTargets.slow(t, (int) c.v("slow_ticks"), 2);
            }
            ServerLevel l = p.serverLevel();
            for (double y = 12; y > 0; y -= 0.6) l.sendParticles(ParticleTypes.LAVA, at.x, at.y + y, at.z, 2, 0.3, 0, 0.3, 0);
            l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            l.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 0.6F);
        });
        return true;
    }

    private static boolean absoluteBarrier(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        long end = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.invulnEnd = Math.max(st.invulnEnd, end);
        double r = c.v("end_radius");
        ring(p.serverLevel(), p.position(), 1.5, ParticleTypes.END_ROD);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
        SkillRuntime.schedule(p.server, (int) c.v("duration_ticks"), () -> {
            if (p.isRemoved() || !p.isAlive()) return;
            for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), r)) {
                c.hit(t, 0, c.v("end_magic"));
                SkillTargets.push(t, p.position(), c.v("knockback"));
            }
            ring(p.serverLevel(), p.position(), r, ParticleTypes.END_ROD);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.6F);
        });
        return true;
    }

    private static boolean timeAccel(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        double ratio = c.v("current_ratio");
        for (String id : List.copyOf(st.cooldownEnd.keySet())) {
            if (id.equals(c.node.id())) continue;
            int left = SkillRuntime.cooldownLeft(p, id);
            if (left > 0 && !isUltimate(p, id)) SkillRuntime.setCooldown(p, id, (int) Math.round(left * ratio));
        }
        st.timeAccelEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        p.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY(1.0), p.getZ(), 60, 0.6, 0.8, 0.6, 0.2);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 2.0F);
        return true;
    }

    static boolean isUltimate(ServerPlayer p, String nodeId) {
        for (SkillPassives.Learned l : SkillPassives.allLearned(p)) {
            if (l.node().id().equals(nodeId)) return l.node().kind() == com.fantasyrpg.core.skilltree.SkillNode.Kind.ULTIMATE;
        }
        return false;
    }

    // ── 기사 ──

    private static boolean thunderCharge(Cast c) {
        ServerPlayer p = c.player;
        Vec3 start = p.position();
        Vec3 end = SkillTargets.freeTravel(p, c.v("distance"), true);
        if (end.distanceToSqr(start) < 1.0) {
            WarriorSkills.blocked(c);
            return false;
        }
        List<LivingEntity> hit = SkillTargets.sweep(p, start, end, 1.0);
        ServerLevel l = p.serverLevel();
        for (double d = 0; d < 1; d += 0.05) {
            Vec3 at = start.lerp(end, d);
            l.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.8, at.z, 2, 0.2, 0.3, 0.2, 0.05);
        }
        p.teleportTo(end.x, end.y, end.z);
        p.resetFallDistance();
        for (LivingEntity t : hit) c.hit(t, c.v("physical"), 0);
        for (LivingEntity t : SkillTargets.around(p, end.add(0, 1, 0), c.v("end_radius"))) {
            c.hit(t, c.v("end_physical"), 0);
            SkillTargets.push(t, end, c.v("knockback"));
        }
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        l.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.4F);
        SkillRuntime.state(p).lastChargeTick = SkillRuntime.now(p);
        return true;
    }

    private static boolean holyLance(Cast c) {
        Vec3 at = point(c);
        ServerPlayer p = c.player;
        double r = c.v("radius");
        for (LivingEntity t : SkillTargets.around(p, at, r)) {
            c.hit(t, c.v("physical"), c.v("magic"));
            SkillTargets.slow(t, (int) c.v("slow_ticks"), 2);
        }
        ServerLevel l = p.serverLevel();
        for (double y = 15; y > 0; y -= 0.5) l.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 2, 0.15, 0, 0.15, 0);
        ring(l, at, r, ParticleTypes.ELECTRIC_SPARK);
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        l.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean rampart(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.wallEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.wallLast = -1000;
        Vec3 front = p.position().add(p.getViewVector(1.0F).multiply(1, 0, 1).normalize());
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, front.x, p.getY() + 1, front.z, 30, 0.6, 0.8, 0.6, 0.01);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    /** 불굴의 성벽이 끝날 때: 모든 스킬 쿨타임 감소 */
    static void rampartEnd(ServerPlayer p, SkillPassives.Learned l) {
        int cut = (int) l.v("end_cooldown_cut");
        SkillRuntime.State st = SkillRuntime.state(p);
        for (String id : List.copyOf(st.cooldownEnd.keySet())) {
            if (id.equals(l.node().id())) continue;
            int left = SkillRuntime.cooldownLeft(p, id);
            if (left > 0) SkillRuntime.setCooldown(p, id, Math.max(0, left - cut));
        }
    }

    private static boolean spearDance(Cast c) {
        LivingEntity t = target(c);
        if (t == null) return false;
        int hits = (int) Math.round(c.v("hits"));
        double range = c.v("range");
        repeat(c, hits + 1, (int) c.v("interval_ticks"), i -> {
            if (!t.isAlive() || t.distanceTo(c.player) > range + 2) return;
            boolean last = i == hits;
            c.hit(t, last ? c.v("final_physical") : c.v("physical"), 0);
            c.player.serverLevel().sendParticles(last ? ParticleTypes.EXPLOSION : ParticleTypes.CRIT, t.getX(), t.getY(0.6), t.getZ(), last ? 1 : 5, 0.2, 0.2, 0.2, 0.1);
            c.player.serverLevel().playSound(null, t.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.5F, 1.2F + i * 0.05F);
        });
        return true;
    }

    // ── 도적 ──

    private static boolean execute(Cast c) {
        LivingEntity t = target(c);
        if (t == null) return false;
        int marks = RogueSkills.consumeMarks(c.player);
        double mult = 1.0 + marks * c.v("per_mark_bonus");
        if (t.getHealth() <= t.getMaxHealth() * c.v("execute_health")) mult *= c.v("execute_mult");
        c.hit(t, c.v("physical"), 0, mult, false);
        ServerLevel l = c.player.serverLevel();
        l.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY(0.7), t.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
        l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.4F);
        return true;
    }

    private static boolean shadowClone(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.cloneEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.cloneBonus = c.v("damage_bonus");
        CoreService.gain(p, 100);
        p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(1.0), p.getZ(), 40, 0.6, 0.8, 0.6, 0.02);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean shadowVeil(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.veilEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.veilLast = -1000;
        p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(1.0), p.getZ(), 40, 0.5, 0.8, 0.5, 0.02);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    private static boolean chainExecution(Cast c) {
        ServerPlayer p = c.player;
        List<LivingEntity> targets = new ArrayList<>(SkillTargets.around(p, p.position().add(0, 1, 0), c.v("radius")));
        if (targets.isEmpty()) {
            WarriorSkills.noTarget(c);
            return false;
        }
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        int n = (int) Math.min(targets.size(), Math.round(c.v("max_targets")));
        repeat(c, n, (int) c.v("interval_ticks"), i -> {
            LivingEntity t = targets.get(i);
            if (!t.isAlive()) return;
            Vec3 behind = t.position().subtract(Vec3.directionFromRotation(0, t.getYRot()).scale(1.2));
            if (p.level().noCollision(p, p.getBoundingBox().move(behind.subtract(p.position())))) p.teleportTo(behind.x, behind.y, behind.z);
            c.hit(t, c.v("physical"), 0);
            if (!t.isAlive() || t.isDeadOrDying()) CoreService.gain(p, c.v("kill_marks"));
            p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, t.getX(), t.getY(0.5), t.getZ(), 10, 0.3, 0.4, 0.3, 0.01);
            p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.7F, 1.4F);
        });
        p.resetFallDistance();
        return true;
    }

    // ── 궁수 ──

    private static boolean skypiercer(Cast c) {
        ServerPlayer p = c.player;
        double focus = spendAll(c);
        double mult = 1.0 + Math.floor(focus / 10.0) * c.v("per_ten_bonus");
        double range = c.v("range");
        Vec3 end = SkillTargets.rayEnd(p, range);
        Vec3 eye = p.getEyePosition();
        Vec3 dir = end.subtract(eye).normalize();
        ServerLevel l = p.serverLevel();
        for (double d = 1; d < end.distanceTo(eye); d += 0.4) {
            Vec3 at = eye.add(dir.scale(d));
            l.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        for (LivingEntity t : SkillTargets.line(p, range)) c.hit(t, c.v("physical"), 0, mult, false);
        l.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    private static boolean elementStorm(Cast c) {
        Vec3 at = point(c);
        double r = c.v("radius");
        repeat(c, (int) Math.round(c.v("pulses")), (int) c.v("pulse_interval"), i -> {
            for (LivingEntity t : SkillTargets.around(c.player, at, r)) {
                c.hit(t, 0, c.v("magic"));
                SkillTargets.slow(t, (int) c.v("slow_ticks"), 1);
            }
            ServerLevel l = c.player.serverLevel();
            l.sendParticles(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, at.x, at.y + 1, at.z, 40, r / 2, 1.0, r / 2, 0.05);
            l.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.4F, 1.2F);
        });
        return true;
    }

    private static boolean vanish(Cast c) {
        ServerPlayer p = c.player;
        SkillTargets.leapBack(p, c.v("leap"));
        int ticks = (int) c.v("duration_ticks");
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false));
        double r = c.v("aggro_radius");
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, new AABB(p.blockPosition()).inflate(r))) {
            // 보스는 놓치지 않음 [초안]
            if (m.getTarget() == p && !SkillSettings.INSTANCE.controlImmune(m)) m.setTarget(null);
        }
        SkillRuntime.state(p).nextSkillBonus = c.v("next_skill_bonus");
        p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(0.5), p.getZ(), 30, 0.4, 0.5, 0.4, 0.02);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    private static boolean arrowDeluge(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        repeat(c, (int) Math.round(c.v("shots")), (int) c.v("interval_ticks"), i -> {
            LivingEntity t = SkillTargets.look(p, range);
            Vec3 end = t != null ? t.getBoundingBox().getCenter() : SkillTargets.rayEnd(p, range);
            Vec3 eye = p.getEyePosition();
            Vec3 dir = end.subtract(eye);
            double len = dir.length();
            dir = dir.normalize();
            for (double d = 1; d < len; d += 1.0) {
                Vec3 at = eye.add(dir.scale(d));
                p.serverLevel().sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            if (i % 2 == 0) p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.4F, 1.6F);
            if (t != null) c.hit(t, c.v("physical"), 0);
        });
        return true;
    }

    // ── 전투사제 ──

    private static boolean divineHammer(Cast c) {
        LivingEntity t = target(c);
        if (t == null) return false;
        for (LivingEntity e : SkillTargets.around(c.player, t.position().add(0, 1, 0), c.v("splash_radius"))) {
            double m = (e == t ? 1.0 : c.v("splash_ratio")) * (SkillPassives.undead(e) ? c.v("undead_mult") : 1.0);
            c.hit(e, c.v("physical"), c.v("magic"), m, false);
        }
        if (!SkillTargets.around(c.player, t.position().add(0, 1, 0), c.v("splash_radius")).contains(t)) {
            c.hit(t, c.v("physical"), c.v("magic"), SkillPassives.undead(t) ? c.v("undead_mult") : 1.0, false);
        }
        ServerLevel l = c.player.serverLevel();
        for (double y = 8; y > 0; y -= 0.5) l.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + y, t.getZ(), 2, 0.2, 0, 0.2, 0);
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, t.getX(), t.getY(), t.getZ(), 1, 0, 0, 0, 0);
        l.playSound(null, t.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.5F);
        return true;
    }

    private static boolean sanctum(Cast c) {
        ServerPlayer p = c.player;
        Vec3 at = p.position();
        double r = c.v("radius");
        repeat(c, (int) Math.round(c.v("pulses")), (int) c.v("pulse_interval"), i -> {
            for (LivingEntity t : SkillTargets.around(p, at.add(0, 1, 0), r)) {
                c.hit(t, 0, c.v("magic"), SkillPassives.undead(t) ? c.v("undead_mult") : 1.0, false);
            }
            com.fantasyrpg.core.party.PartyService.heal(p, c.v("heal"));                            // 파티원도 (2026-11-21)
            ServerLevel l = p.serverLevel();
            ring(l, at, r, ParticleTypes.END_ROD);
            l.sendParticles(ParticleTypes.GLOW, at.x, at.y + 0.5, at.z, 20, r / 2, 0.3, r / 2, 0.02);
            l.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.4F);
        });
        return true;
    }

    private static boolean prayer(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.state(p).prayerEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY(1.0), p.getZ(), 30, 0.5, 0.7, 0.5, 0.2);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    private static boolean radiance(Cast c) {
        ServerPlayer p = c.player;
        ClericSkills.purge(p, Integer.MAX_VALUE);
        com.fantasyrpg.core.party.PartyService.heal(p, p.getMaxHealth() * c.v("heal_ratio"));
        double r = c.v("radius");
        for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), r)) {
            c.hit(t, 0, c.v("magic"), SkillPassives.undead(t) ? c.v("undead_mult") : 1.0, false);
        }
        ServerLevel l = p.serverLevel();
        ring(l, p.position(), r, ParticleTypes.END_ROD);
        l.sendParticles(ParticleTypes.HEART, p.getX(), p.getY(1.2), p.getZ(), 6, 0.4, 0.4, 0.4, 0);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
