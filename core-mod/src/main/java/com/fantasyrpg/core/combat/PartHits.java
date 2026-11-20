package com.fantasyrpg.core.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 여러 부위 몹(예: fantasybosses 라디키온 11마디)을 스킬로 맞혔을 때, 스킬 코드는 몸체를 대상으로 다루고
 * 실제 피해는 맞은 부위에 주기 위한 짧은 기록 (2026-11-20).
 *
 * <p>대상 고르기({@code SkillTargets})가 부위를 찾으면 "이 플레이어가 이번 틱에 이 몸체의 이 부위를 맞혔다"를 남기고,
 * {@link RpgDamage#apply(LivingEntity, Entity, DamageInput)} 가 같은 틱에 그 몸체에 피해를 줄 때 부위로 돌린다.
 * 다음 틱이면 기록은 버린다(몸체에 그대로).
 */
public final class PartHits {
    private PartHits() {
    }

    private record Hit(long tick, Entity part) {
    }

    private static final Map<UUID, Map<Integer, Hit>> HITS = new HashMap<>();

    private static long now(Entity e) {
        return e.level().getGameTime();
    }

    /** 부위면 그 몸체를 돌려주고 기록, 아니면 그대로(생물이 아니면 null) */
    @Nullable
    public static LivingEntity resolve(ServerPlayer p, Entity hit) {
        LivingEntity body = RpgDamage.body(hit);
        if (body != null && body != hit) mark(p, body, hit);
        return body;
    }

    /** 같은 몸체를 여러 부위가 맞으면 플레이어에게 가장 가까운 부위 하나 */
    public static void mark(ServerPlayer p, LivingEntity body, Entity part) {
        Map<Integer, Hit> m = HITS.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        long t = now(p);
        Hit old = m.get(body.getId());
        if (old != null && old.tick() == t && old.part().distanceToSqr(p) <= part.distanceToSqr(p)) return;
        m.put(body.getId(), new Hit(t, part));
    }

    /** 이번 틱에 이 공격자가 이 몸체의 부위를 맞혔으면 그 부위, 아니면 몸체 */
    public static Entity redirect(@Nullable Entity attacker, LivingEntity target) {
        if (!(attacker instanceof ServerPlayer p)) return target;
        Map<Integer, Hit> m = HITS.get(p.getUUID());
        if (m == null) return target;
        Hit h = m.get(target.getId());
        if (h == null || h.tick() != now(p) || h.part().isRemoved()) return target;
        return h.part();
    }

    public static void forget(UUID player) {
        HITS.remove(player);
    }
}
