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

    // ── 강격 ──
    private static boolean heavyStrike(Cast c) {
        LivingEntity t = SkillTargets.look(c.player, c.v("range"));
        if (t == null) {
            noTarget(c);
            return false;
        }
        double mult = 1.0;
        // 5랭크: 기세가 문턱 이상이면 더 쓰고 강화 (기본 비용은 아직 빠지지 않은 상태라 비용 + 추가분을 함께 확인)
        double threshold = c.v("empower_threshold");
        if (threshold > 0 && CoreService.value(c.player) >= Math.max(threshold, c.node.cost(c.rank) + c.v("empower_cost"))) {
            CoreService.spend(c.player, c.v("empower_cost"));
            mult = c.v("empower_mult");
        }
        c.hit(t, c.v("physical") * mult, 0);
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
                c.hit(t, 0, c.v("burst_magic"));
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
}
