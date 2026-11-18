package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.skilltree.SkillEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 전투사제 1단 액티브 4종 (claude/4직업_스킬_초안.md 7절). 회복은 전투사제만.
 * 신성 방벽의 받는 피해 처리·끝날 때 효과는 {@link SkillEvents}.
 */
public final class ClericSkills {
    private ClericSkills() {
    }

    public static void register() {
        SkillEffects.register(SkillIds.C_JUDGMENT, ClericSkills::judgment);
        SkillEffects.register(SkillIds.C_HOLY_BURST, ClericSkills::holyBurst);
        SkillEffects.register(SkillIds.C_RETRIBUTION, ClericSkills::retribution);
        SkillEffects.register(SkillIds.C_PURIFY_FLURRY, ClericSkills::purifyFlurry);
        SkillEffects.register(SkillIds.C_SMITE, ClericSkills::smite);
        SkillEffects.register(SkillIds.C_HOLY_LIGHT, ClericSkills::holyLight);
        SkillEffects.register(SkillIds.C_SANCTUARY, ClericSkills::sanctuary);
        SkillEffects.register(SkillIds.C_PURIFY, ClericSkills::purify);
    }

    // ── 징벌 강타 ──
    private static boolean smite(Cast c) {
        ServerPlayer p = c.player;
        LivingEntity t = SkillTargets.look(p, c.v("range"));
        if (t == null) {
            WarriorSkills.noTarget(c);
            return false;
        }
        double mult = c.empower(); // 5랭크
        double radius = mult > 1 ? c.v("empower_radius") : c.v("splash_radius");
        c.hit(t, c.v("physical"), c.v("magic"), mult, false);
        SkillTargets.slow(t, (int) c.v("stun_ticks"), 3);
        for (LivingEntity e : SkillTargets.around(p, t.position().add(0, t.getBbHeight() / 2, 0), radius)) {
            if (e != t) c.hit(e, c.v("physical"), c.v("magic"), mult * c.v("splash_ratio"), false);
        }
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY(), t.getZ(), 20, radius / 2, 0.2, radius / 2, 0.05);
        l.playSound(null, t.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.4F);
        return true;
    }

    // ── 성광 ──
    private static boolean holyLight(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        LivingEntity aimed = SkillTargets.look(p, range);
        Vec3 center = aimed != null ? aimed.position().add(0, aimed.getBbHeight() / 2, 0) : SkillTargets.rayEnd(p, range);
        double radius = c.v("radius");
        int hit = light(c, center, radius, 1.0);
        double heal = Math.min(c.v("heal_cap"), hit * c.v("heal_per_hit"));
        if (heal > 0) {
            SkillPassives.heal(p, heal);
            p.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY(1.2), p.getZ(), 3, 0.3, 0.3, 0.3, 0);
        }
        double echo = c.v("echo_ratio");
        if (echo > 0) SkillRuntime.schedule(p.server, (int) c.v("echo_delay"), () -> {
            if (!p.isRemoved()) light(c, center, radius, echo);
        });
        return true;
    }

    private static int light(Cast c, Vec3 center, double radius, double ratio) {
        int n = 0;
        for (LivingEntity t : SkillTargets.around(c.player, center, radius)) {
            double mult = ratio * (SkillPassives.undead(t) ? 1.0 + c.v("undead_bonus") : 1.0);
            c.hit(t, 0, c.v("magic"), mult, false);
            n++;
        }
        ServerLevel l = c.player.serverLevel();
        l.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1, center.z, 30, radius / 2, 0.8, radius / 2, 0.02);
        l.sendParticles(ParticleTypes.GLOW, center.x, center.y, center.z, 20, radius / 2, 0.3, radius / 2, 0.02);
        l.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.6F);
        return n;
    }

    // ── 신성 방벽 ── (감소·신성력·끝날 때 효과는 SkillEvents)
    private static boolean sanctuary(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.sanctEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.sanctReduced = 0;
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY(1.0), p.getZ(), 40, 0.6, 0.8, 0.6, 0.5);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    /** 신성 방벽이 끝날 때: 3랭크 주변 신성 피해, 5랭크 줄인 피해 일부 회복 */
    static void sanctuaryEnd(ServerPlayer p, SkillPassives.Learned l, double reduced) {
        Cast c = new Cast(p, l.node(), l.rank(), 0);
        if (l.v("end_magic") > 0) {
            for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), l.v("end_radius"))) c.hit(t, 0, l.v("end_magic"));
            p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY(0.5), p.getZ(), 30, 1.5, 0.3, 1.5, 0.05);
        }
        double heal = reduced * l.v("end_heal_ratio");
        if (heal > 0) {
            SkillPassives.heal(p, heal);
            p.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY(1.2), p.getZ(), 4, 0.3, 0.3, 0.3, 0);
        }
    }

    // ── 정화의 일격 ──
    private static boolean purify(Cast c) {
        ServerPlayer p = c.player;
        LivingEntity t = SkillTargets.look(p, c.v("range"));
        if (t == null) {
            WarriorSkills.noTarget(c);
            return false;
        }
        c.hit(t, c.v("physical"), c.v("magic"));
        int remove = purge(p, c.v("purge_all") > 0 ? Integer.MAX_VALUE : 1);
        if (remove > 0) {
            CoreService.gain(p, c.v("purge_core"));
            SkillPassives.heal(p, remove * c.v("purge_heal"));
            if (c.v("per_purge_magic") > 0) c.hit(t, 0, c.v("per_purge_magic") * remove);
            p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY(1.0), p.getZ(), 10, 0.4, 0.5, 0.4, 0);
        }
        p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    // ── 3단: 심판의 망치 ──
    private static boolean judgment(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        LivingEntity aimed = SkillTargets.look(p, range);
        Vec3 center = aimed != null ? aimed.position().add(0, 0.5, 0) : SkillTargets.rayEnd(p, range);
        double mult = c.empower();
        double radius = c.v("radius");
        int n = 0;
        for (LivingEntity t : SkillTargets.around(p, center, radius)) {
            double m = mult * (SkillPassives.undead(t) ? 1.0 + c.v("undead_bonus") : 1.0);
            c.hit(t, c.v("physical"), c.v("magic"), m, false);
            SkillTargets.slow(t, (int) c.v("stun_ticks"), 3);
            n++;
        }
        CoreService.gain(p, n * c.v("hit_core"));
        ServerLevel l = p.serverLevel();
        for (double y = 5; y > 0; y -= 0.5) l.sendParticles(ParticleTypes.END_ROD, center.x, center.y + y, center.z, 1, 0.1, 0, 0.1, 0);
        l.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        l.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    // ── 3단: 신성 폭발 ──
    private static boolean holyBurst(Cast c) {
        ServerPlayer p = c.player;
        double radius = c.v("radius");
        int n = 0;
        for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), radius)) {
            c.hit(t, 0, c.v("magic"), SkillPassives.undead(t) ? c.v("undead_mult") : 1.0, false);
            n++;
        }
        double heal = c.v("heal") + Math.min(c.v("heal_cap"), n * c.v("heal_per_hit"));
        SkillPassives.heal(p, heal);
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY(1.0), p.getZ(), 40, radius / 2, 0.5, radius / 2, 0.05);
        l.sendParticles(ParticleTypes.HEART, p.getX(), p.getY(1.2), p.getZ(), 3, 0.3, 0.3, 0.3, 0);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    // ── 3단: 응징의 방패 ── (반격·감소는 SkillEvents)
    private static boolean retribution(Cast c) {
        ServerPlayer p = c.player;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.retribEnd = SkillRuntime.now(p) + (long) c.v("duration_ticks");
        st.retribLast = -1000;
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY(1.0), p.getZ(), 30, 0.5, 0.7, 0.5, 0.3);
        l.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    /** 응징의 방패가 끝날 때 (5랭크 주변 신성 피해) */
    static void retributionEnd(ServerPlayer p, SkillPassives.Learned l) {
        if (l.v("end_magic") <= 0) return;
        Cast c = new Cast(p, l.node(), l.rank(), 0);
        for (LivingEntity t : SkillTargets.around(p, p.position().add(0, 1, 0), l.v("end_radius"))) c.hit(t, 0, l.v("end_magic"));
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY(0.5), p.getZ(), 30, 1.5, 0.3, 1.5, 0.05);
    }

    /** 해로운 효과 n개 해제, 해제한 수 */
    static int purge(ServerPlayer p, int max) {
        List<Holder<MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance e : p.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(e.getEffect());
        }
        int n = Math.min(max, bad.size());
        for (int i = 0; i < n; i++) p.removeEffect(bad.get(i));
        return n;
    }

    // ── 3단: 정화의 연타 ──
    private static boolean purifyFlurry(Cast c) {
        ServerPlayer p = c.player;
        double range = c.v("range");
        LivingEntity t = SkillTargets.look(p, range);
        if (t == null) {
            WarriorSkills.noTarget(c);
            return false;
        }
        int hits = (int) Math.round(c.v("hits"));
        int interval = (int) Math.max(1, c.v("interval_ticks"));
        for (int i = 0; i < hits; i++) {
            Runnable strike = () -> {
                if (p.isRemoved() || !p.isAlive() || !t.isAlive() || t.distanceTo(p) > range + 1.5) return;
                c.hit(t, c.v("physical"), c.v("magic"));
                int removed = purge(p, 1);
                if (removed > 0) {
                    CoreService.gain(p, removed * c.v("purge_core"));
                    SkillPassives.heal(p, removed * c.v("purge_heal"));
                }
                p.serverLevel().sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY(0.6), t.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.4F);
            };
            if (i == 0) strike.run();
            else SkillRuntime.schedule(p.server, i * interval, strike);
        }
        return true;
    }
}
