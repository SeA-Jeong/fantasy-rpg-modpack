package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.skilltree.SkillEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * 전사 1단 액티브 4종 (claude/전사마법사_스킬_초안.md 4절). 2단 패시브는 {@link SkillPassives}·{@link SkillEvents}.
 */
public final class WarriorSkills {
    private WarriorSkills() {
    }

    private static final DustParticleOptions MANA = new DustParticleOptions(new Vector3f(0.45F, 0.55F, 1.0F), 1.2F);

    public static void register() {
        SkillEffects.register(SkillIds.W_WHIRLWIND, WarriorSkills::whirlwind);
        SkillEffects.register(SkillIds.W_BLADE_STORM, WarriorSkills::bladeStorm);
        SkillEffects.register(SkillIds.W_COUNTER_SMASH, WarriorSkills::counterSmash);
        SkillEffects.register(SkillIds.W_GALE_THRUST, WarriorSkills::galeThrust);
        SkillEffects.register(SkillIds.W_HEAVY_STRIKE, WarriorSkills::heavyStrike);
        SkillEffects.register(SkillIds.W_MANA_SLASH, WarriorSkills::manaSlash);
        SkillEffects.register(SkillIds.W_INDOMITABLE, WarriorSkills::indomitable);
        SkillEffects.register(SkillIds.W_FLURRY, WarriorSkills::flurry);
    }

    static ServerLevel level(Cast c) {
        return c.player.serverLevel();
    }

    static void noTarget(Cast c) {
        c.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.fantasyrpg.skill.no_target")
                .withStyle(net.minecraft.ChatFormatting.GRAY), true);
    }

    static void blocked(Cast c) {
        c.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.fantasyrpg.skill.blocked")
                .withStyle(net.minecraft.ChatFormatting.GRAY), true);
    }

    // ── 강격 ──
    private static boolean heavyStrike(Cast c) {
        LivingEntity t = SkillTargets.look(c.player, c.v("range"));
        if (t == null) {
            noTarget(c);
            return false;
        }
        double mult = c.empower(); // 5랭크
        c.hit(t, c.v("physical"), 0, mult, false);
        SkillTargets.slow(t, (int) c.v("stun_ticks"), 3);
        ServerLevel l = level(c);
        l.sendParticles(mult > 1 ? ParticleTypes.EXPLOSION : ParticleTypes.CRIT, t.getX(), t.getY(0.6), t.getZ(), mult > 1 ? 1 : 12, 0.3, 0.3, 0.3, 0.2);
        l.playSound(null, t.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, mult > 1 ? 0.6F : 0.9F);
        return true;
    }

    // ── 마력 베기 ──
    private static boolean manaSlash(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        List<LivingEntity> targets = SkillTargets.cone(p, range, c.v("angle"));
        // 부채꼴 모양 입자 (맞은 적이 없어도 휘두름)
        Vec3 look = p.getViewVector(1.0F);
        double baseYaw = Math.atan2(look.z, look.x);
        double half = Math.toRadians(c.v("angle") / 2);
        ServerLevel l = level(c);
        for (int i = 0; i <= 8; i++) {
            double a = baseYaw - half + (2 * half) * i / 8.0;
            for (double d = 1.5; d <= range; d += 1.0) {
                l.sendParticles(MANA, p.getX() + Math.cos(a) * d, p.getY() + 1.0, p.getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0);
            }
        }
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.3F);
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        st.marks.values().removeIf(end -> end < now);
        for (LivingEntity t : targets) {
            UUID id = t.getUUID();
            boolean marked = st.marks.containsKey(id);
            c.hit(t, c.v("physical"), c.v("magic"));
            if (marked && c.v("burst_magic") > 0) {
                st.marks.remove(id);
                burstMark(c, t, c.v("burst_magic"));
                l.sendParticles(ParticleTypes.ENCHANTED_HIT, t.getX(), t.getY(0.5), t.getZ(), 20, 0.3, 0.5, 0.3, 0.3);
                l.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            } else if (c.v("mark_ticks") > 0) {
                st.marks.put(id, now + (long) c.v("mark_ticks"));
                l.sendParticles(ParticleTypes.ENCHANT, t.getX(), t.getY(1.0), t.getZ(), 15, 0.3, 0.3, 0.3, 0.5);
            }
        }
        return true; // 허공에 휘둘러도 사용됨
    }

    // ── 불굴 ──
    private static boolean indomitable(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.indomitableEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.indomitableNode = c.node.id();
        CoreService.gain(p, c.v("cast_core"));
        ServerLevel l = level(c);
        l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY(1.0), p.getZ(), 25, 0.4, 0.6, 0.4, 0.2);
        l.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.5F, 1.4F);
        return true;
    }

    /** 불굴이 끝날 때 (SkillEvents 의 틱에서) — 5랭크 반격 충격 */
    static void indomitableEnd(ServerPlayer p, SkillPassives.Learned l) {
        double phys = l.v("end_physical");
        if (phys <= 0) return;
        Cast c = new Cast(p, l.node(), l.rank(), 0);
        double r = l.v("end_radius");
        for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), r)) c.hit(t, phys, 0);
        ServerLevel l2 = p.serverLevel();
        l2.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY(0.5), p.getZ(), 8, r / 2, 0.2, r / 2, 0);
        l2.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4F, 1.5F);
    }

    // ── 쾌검 연격 ──
    private static boolean flurry(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        LivingEntity t = SkillTargets.look(p, range);
        if (t == null) {
            noTarget(c);
            return false;
        }
        int hits = (int) Math.round(c.v("hits"));
        int interval = (int) Math.max(1, c.v("interval_ticks"));
        int[] landed = {0};
        for (int i = 0; i < hits; i++) {
            final boolean last = i == hits - 1;
            Runnable strike = () -> {
                if (p.isRemoved() || !p.isAlive() || !t.isAlive() || t.distanceTo(p) > range + 1.5) return;
                c.hit(t, c.v("physical"), 0);
                landed[0]++;
                p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY(0.6), t.getZ(), 1, 0.2, 0.2, 0.2, 0);
                p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.6F, 1.4F + 0.1F * landed[0]);
                if (last && landed[0] >= hits && c.v("refund_core") > 0) CoreService.gain(p, c.v("refund_core"));
            };
            if (i == 0) strike.run();
            else SkillRuntime.schedule(p.server, i * interval, strike);
        }
        return true;
    }

    /** 마력 표식 폭발 (마력 베기·마검 폭풍). burst_splash(마력 순환 5랭크)면 주변 2칸에도 */
    static void burstMark(Cast c, LivingEntity t, double magic) {
        c.hit(t, 0, magic);
        double splash = c.v("burst_splash");
        if (splash > 0) {
            for (LivingEntity e : SkillTargets.around(c.player, t.position().add(0, t.getBbHeight() / 2, 0), 2)) {
                if (e != t) c.hit(e, 0, magic * splash);
            }
        }
        ServerLevel l = c.player.serverLevel();
        l.sendParticles(ParticleTypes.ENCHANTED_HIT, t.getX(), t.getY(0.5), t.getZ(), 20, 0.3, 0.5, 0.3, 0.3);
        l.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    // ── 3단: 회전 베기 ──
    private static boolean whirlwind(Cast c) {
        spin(c, 1.0);
        double echo = c.v("echo_ratio");
        if (echo > 0) SkillRuntime.schedule(c.player.server, 20, () -> {
            if (!c.player.isRemoved() && c.player.isAlive()) spin(c, echo);
        });
        return true;
    }

    private static void spin(Cast c, double ratio) {
        ServerPlayer p = c.player;
        double r = c.v("radius");
        int n = 0;
        for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), r)) {
            c.hit(t, c.v("physical"), 0, ratio, false);
            n++;
        }
        CoreService.gain(p, n * c.v("hit_core"));
        ServerLevel l = p.serverLevel();
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            l.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + Math.cos(a) * r * 0.7, p.getY() + 1, p.getZ() + Math.sin(a) * r * 0.7, 1, 0, 0, 0, 0);
        }
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // ── 3단: 마검 폭풍 ──
    private static boolean bladeStorm(Cast c) {
        ServerPlayer p = c.player;
        c.empower(); // 5랭크: 범위만 넓어짐
        double range = c.empowered ? c.v("empower_range") : c.v("range");
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        double baseYaw = Math.atan2(look.z, look.x);
        double half = Math.toRadians(c.v("angle") / 2);
        for (int i = 0; i <= 10; i++) {
            double a = baseYaw - half + (2 * half) * i / 10.0;
            for (double d = 1.5; d <= range; d += 1.0) {
                l.sendParticles(MANA, p.getX() + Math.cos(a) * d, p.getY() + 1.0, p.getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0);
            }
        }
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        for (LivingEntity t : SkillTargets.cone(p, range, c.v("angle"))) {
            c.hit(t, c.v("physical"), c.v("magic"));
            SkillTargets.slow(t, (int) c.v("slow_ticks"), 1);
            Long mark = st.marks.get(t.getUUID());
            if (mark != null && mark >= now) {
                st.marks.remove(t.getUUID());
                burstMark(c, t, c.v("burst_magic"));
            }
        }
        return true;
    }

    // ── 3단: 반격 강타 ──
    private static boolean counterSmash(Cast c) {
        ServerPlayer p = c.player;
        LivingEntity t = SkillTargets.look(p, c.v("range"));
        if (t == null) {
            noTarget(c);
            return false;
        }
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        double mult = now - st.lastHurtTick <= c.v("hurt_window_ticks") ? c.v("hurt_mult") : 1.0;
        c.hit(t, c.v("physical"), 0, mult, false);
        SkillTargets.slow(t, (int) c.v("stun_ticks"), 3);
        if (st.indomitableEnd > now) st.indomitableEnd += (long) c.v("indomitable_extend");
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY(0.6), t.getZ(), mult > 1 ? 25 : 10, 0.3, 0.3, 0.3, 0.3);
        l.playSound(null, t.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, mult > 1 ? 0.7F : 1.1F);
        return true;
    }

    // ── 3단: 질풍 찌르기 ──
    private static boolean galeThrust(Cast c) {
        ServerPlayer p = c.player;
        Vec3 start = p.position();
        Vec3 end = SkillTargets.freeTravel(p, c.v("distance"), true);
        if (end.distanceToSqr(start) < 1.0) {
            blocked(c);
            return false;
        }
        java.util.List<LivingEntity> hit = SkillTargets.sweep(p, start, end, 0.8);
        ServerLevel l = p.serverLevel();
        for (double d = 0; d < 1; d += 0.1) {
            Vec3 at = start.lerp(end, d);
            l.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.6, at.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
        p.teleportTo(end.x, end.y, end.z);
        p.resetFallDistance();
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.5F);
        for (LivingEntity t : hit) c.hit(t, c.v("physical"), 0);
        if (c.v("many_hits") > 0 && hit.size() >= c.v("many_hits")) c.cooldownMult = 0.5;
        return true;
    }
}
