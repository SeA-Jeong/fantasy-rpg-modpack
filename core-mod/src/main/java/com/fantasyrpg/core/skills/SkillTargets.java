package com.fantasyrpg.core.skills;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 스킬 대상 고르기. 플레이어는 절대 대상이 아님(PvP 차단).
 * 한 대상 스킬(조준)은 기본 공격처럼 플레이어가 아닌 모든 생물, 범위 스킬은 적대 몬스터({@link Enemy})만 [초안]
 * — 마을에서 범위 스킬로 주민·가축을 다치게 하지 않게.
 */
public final class SkillTargets {
    private SkillTargets() {
    }

    public static boolean single(Entity e, ServerPlayer p) {
        return e instanceof LivingEntity l && l != p && l.isAlive() && !(l instanceof Player) && !(l instanceof ArmorStand)
                && !l.isSpectator();
    }

    public static boolean area(Entity e, ServerPlayer p) {
        return single(e, p) && e instanceof Enemy;
    }

    /** 시선 끝이 닿는 지점 (블록에 막히면 그 앞) */
    public static Vec3 rayEnd(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1.0F).scale(range));
        BlockHitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return block.getType() == HitResult.Type.MISS ? end : block.getLocation();
    }

    /** 바라보는 첫 대상 (블록 뒤는 안 됨) */
    public static LivingEntity look(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = rayEnd(p, range);
        AABB box = p.getBoundingBox().expandTowards(p.getViewVector(1.0F).scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box, e -> single(e, p) && e.isPickable());
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** 임의 방향으로 쏜 첫 대상 (그림자 칼날의 부채꼴 단검) */
    public static LivingEntity firstAlong(ServerPlayer p, Vec3 dir, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(dir.normalize().scale(range));
        BlockHitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB box = new AABB(eye, end).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box, e -> single(e, p) && e.isPickable());
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** 수평 시선을 yaw 도만큼 돌린 방향 */
    public static Vec3 rotatedLook(ServerPlayer p, double degrees) {
        return Vec3.directionFromRotation(p.getXRot(), p.getYRot() + (float) degrees);
    }

    /**
     * 수평으로 앞으로 이동할 수 있는 끝 지점 (블록에 막히면 그 앞). 돌진·점멸용.
     *
     * @param horizontal true면 시선의 위아래를 무시
     */
    public static Vec3 freeTravel(ServerPlayer p, double distance, boolean horizontal) {
        Vec3 start = p.position();
        Vec3 look = p.getViewVector(1.0F);
        if (horizontal) look = new Vec3(look.x, 0, look.z).normalize();
        Vec3 best = start;
        AABB box = p.getBoundingBox();
        for (double d = 0.25; d <= distance + 1e-6; d += 0.25) {
            Vec3 at = start.add(look.scale(d));
            if (!p.level().noCollision(p, box.move(at.subtract(start)))) break;
            best = at;
        }
        return best;
    }

    /** 두 점 사이를 지나간 적대 몬스터 (폭 width) */
    public static List<LivingEntity> sweep(ServerPlayer p, Vec3 from, Vec3 to, double width) {
        AABB box = new AABB(from, to).inflate(width, 1.0, width);
        List<LivingEntity> out = new ArrayList<>();
        Vec3 a = from.add(0, 1, 0), b = to.add(0, 1, 0);
        for (Entity e : p.level().getEntities(p, box, e -> area(e, p))) {
            if (e.getBoundingBox().inflate(width).clip(a, b).isPresent() || e.getBoundingBox().inflate(width).contains(a)) out.add((LivingEntity) e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        return out;
    }

    /** 뒤(시선 반대)로 도약 — 순간이동이 아니라 속도로 밈 */
    public static void leapBack(ServerPlayer p, double strength) {
        Vec3 look = p.getViewVector(1.0F);
        Vec3 back = new Vec3(-look.x, 0, -look.z).normalize().scale(strength);
        p.setDeltaMovement(back.x, 0.35, back.z);
        p.hurtMarked = true;
        p.resetFallDistance();
    }

    /** 시선 직선 위 모든 대상 (가까운 순, 블록까지) */
    public static List<LivingEntity> line(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = rayEnd(p, range);
        AABB box = new AABB(eye, end).inflate(1.0);
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : p.level().getEntities(p, box, e -> single(e, p) && e.isPickable())) {
            if (e.getBoundingBox().inflate(0.3).clip(eye, end).isPresent()) out.add((LivingEntity) e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        return out;
    }

    /** 앞쪽 부채꼴 (수평 각도, 거리 range) 적대 몬스터 */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double angle) {
        Vec3 look = p.getViewVector(1.0F);
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(range), e -> area(e, p))) {
            if (e.distanceTo(p) > range + e.getBbWidth() / 2) continue;
            double dx = e.getX() - p.getX();
            double dz = e.getZ() - p.getZ();
            if (SkillMath.inCone(dx, dz, look.x, look.z, angle)) out.add((LivingEntity) e);
        }
        return out;
    }

    /** 점 주변 반경 안 적대 몬스터 */
    public static List<LivingEntity> around(ServerPlayer p, Vec3 center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        AABB box = new AABB(center, center).inflate(radius);
        for (Entity e : p.level().getEntities(p, box, e -> area(e, p))) {
            if (e.getBoundingBox().getCenter().distanceTo(center) <= radius + e.getBbWidth() / 2) out.add((LivingEntity) e);
        }
        return out;
    }

    // ── 제어 효과 (보스에는 듣지 않음) ──

    public static void slow(LivingEntity e, int ticks, int amplifier) {
        if (ticks <= 0 || SkillSettings.INSTANCE.controlImmune(e)) return;
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier, false, true));
    }

    /** source 에서 멀어지는 쪽으로 밀기 */
    public static void push(LivingEntity e, Vec3 source, double strength) {
        if (strength <= 0 || SkillSettings.INSTANCE.controlImmune(e)) return;
        e.knockback(strength, source.x - e.getX(), source.z - e.getZ());
        e.hurtMarked = true;
    }

    public static ServerLevel level(ServerPlayer p) {
        return p.serverLevel();
    }
}
