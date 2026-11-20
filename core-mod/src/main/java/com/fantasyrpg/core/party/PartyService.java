package com.fantasyrpg.core.party;

import com.fantasyrpg.core.skills.SkillPassives;
import com.fantasyrpg.core.skills.SkillRuntime;
import com.fantasyrpg.core.skills.SkillSettings;
import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 파티 대상 스킬·경험치 나누기 (2026-11-21). 파티 = magiccity {@link MagicCityApi#partyMembers} (FTB Teams 파티, 접속 중).
 */
public final class PartyService {
    private PartyService() {}

    public static PartyRules.Settings settings() {
        return SkillSettings.INSTANCE.party();
    }

    /** 같은 차원·살아 있고 radius 안의 다른 파티원 (본인 제외) */
    public static List<ServerPlayer> near(ServerPlayer p, double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        if (radius <= 0) return out;
        for (ServerPlayer m : MagicCityApi.partyMembers(p)) {
            if (m == p || m.getUUID().equals(p.getUUID()) || !m.isAlive() || m.isSpectator()) continue;
            if (m.level() != p.level() || m.distanceToSqr(p) > radius * radius) continue;
            out.add(m);
        }
        return out;
    }

    /** 시전자 회복 + 근처 파티원에게 그 비율만큼 (시전자의 회복 보너스 적용) */
    public static void heal(ServerPlayer caster, double amount) {
        if (amount <= 0) return;
        SkillPassives.heal(caster, amount);
        PartyRules.Settings s = settings();
        if (s.healShare() <= 0) return;
        double each = amount * (1.0 + SkillPassives.sum(caster, "heal_bonus")) * s.healShare();
        for (ServerPlayer m : near(caster, s.healRadius())) {
            m.heal((float) each);
            m.serverLevel().sendParticles(ParticleTypes.HEART, m.getX(), m.getY(1.2), m.getZ(), 2, 0.3, 0.3, 0.3, 0);
        }
    }

    /** 신성 방벽 같은 피해 감소를 근처 파티원에게 (감소율 × 비율, 같은 시간) */
    public static void shareReduction(ServerPlayer caster, double reduction, long durationTicks) {
        PartyRules.Settings s = settings();
        double r = PartyRules.buffReduction(reduction, s.buffShare());
        if (r <= 0) return;
        for (ServerPlayer m : near(caster, s.buffRadius())) {
            SkillRuntime.State st = SkillRuntime.state(m);
            long end = SkillRuntime.now(m) + durationTicks;
            if (st.allyReduceEnd > SkillRuntime.now(m) && st.allyReduce > r) continue;   // 더 강한 것이 이미 있으면 그대로
            st.allyReduce = r;
            st.allyReduceEnd = end;
            m.serverLevel().sendParticles(ParticleTypes.ENCHANT, m.getX(), m.getY(1.0), m.getZ(), 20, 0.5, 0.7, 0.5, 0.4);
        }
    }

    /** 처치 경험치를 받을 사람: 처치자 + xp_radius 안의 파티원 */
    public static List<ServerPlayer> xpReceivers(ServerPlayer killer) {
        List<ServerPlayer> out = new ArrayList<>();
        out.add(killer);
        out.addAll(near(killer, settings().xpRadius()));
        return out;
    }
}
