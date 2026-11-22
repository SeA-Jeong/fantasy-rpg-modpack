package com.fantasyrpg.core.client;

import com.fantasyrpg.core.fx.RpgParticles;
import com.fantasyrpg.core.gear.rune.RuneItem;
import com.fantasyrpg.core.gear.rune.RuneRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 룬석 반짝임 (2026-11-22, 사용자 제공 프리셋 v0.4): 손에 든 룬석·바닥에 떨어진 룬석에서 등급별로 초당 몇 개.
 * 클라이언트에서만 만들고(서버 방송 없음), 24칸 안만.
 */
final class RuneFx {
    private RuneFx() {}

    /** 초당 개수: 하급·중급·상급·최고급 */
    private static final float[] HELD = {0f, 0.2f, 0.4f, 0.75f};
    private static final float[] DROPPED = {0.15f, 0.4f, 0.85f, 1.25f};
    private static final double RANGE = 24;

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        RandomSource r = level.random;
        for (Player p : level.players()) {
            if (p.distanceToSqr(mc.player) > RANGE * RANGE) continue;
            for (boolean main : new boolean[]{true, false}) {
                ItemStack s = main ? p.getMainHandItem() : p.getOffhandItem();
                float rate = rate(s, HELD);
                if (rate <= 0 || r.nextFloat() >= rate / 20f) continue;
                boolean right = (p.getMainArm() == HumanoidArm.RIGHT) == main;
                Vec3 look = p.getViewVector(1f);
                Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(right ? 0.38 : -0.38);
                Vec3 at = p.position().add(0, p.getBbHeight() * 0.55, 0).add(side).add(look.scale(0.3));
                spawn(level, (RuneItem) s.getItem(), at, r);
            }
        }
        // 계열 갑옷 8~10급 오오라 (v0.8 rarity_effects: 초당 8급 2.0·9급 3.2·10급 4.0 — 4부위 합, 부위마다 1/4). 파손된 부위는 없음
        for (Player p : level.players()) {
            if (p.distanceToSqr(mc.player) > RANGE * RANGE || p.isInvisible()) continue;
            for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                    net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                ItemStack s = p.getItemBySlot(slot);
                if (!(s.getItem() instanceof com.fantasyrpg.core.gear.armor.FamilyArmorItem fa)
                        || com.fantasyrpg.core.gear.armor.FamilyArmorItem.isBroken(s)) continue;
                int tier = com.fantasyrpg.core.gear.armor.GearRegistry.tierOf(s);
                var type = RpgParticles.aura(fa.family(), tier);
                if (type == null) continue;
                float rate = tier >= 10 ? 4.0f : tier == 9 ? 3.2f : 2.0f;
                if (r.nextFloat() >= rate / 4f / 20f) continue;
                double radius = tier >= 10 ? 0.52 : tier == 9 ? 0.43 : 0.34;
                double h = switch (slot) { case HEAD -> 0.85; case CHEST -> 0.6; case LEGS -> 0.35; default -> 0.08; };
                double a = r.nextDouble() * Math.PI * 2;
                level.addParticle(type, p.getX() + Math.cos(a) * radius, p.getY() + p.getBbHeight() * h, p.getZ() + Math.sin(a) * radius,
                        0, 0.012 + r.nextDouble() * 0.01, 0);
            }
        }
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(RANGE))) {
            ItemStack s = e.getItem();
            float rate = rate(s, DROPPED);
            if (rate <= 0) continue;
            // 한 묶음이 여러 개여도 반짝임은 한 개 분량 (화면이 너무 번쩍이지 않게)
            if (r.nextFloat() < rate / 20f) spawn(level, (RuneItem) s.getItem(), e.position().add(0, 0.3, 0), r);
        }
    }

    private static float rate(ItemStack s, float[] table) {
        if (!(s.getItem() instanceof RuneItem ri) || ri.type() == null) return 0;
        int g = Math.max(1, Math.min(4, RuneRegistry.gradeOf(s)));
        return table[g - 1];
    }

    private static void spawn(ClientLevel level, RuneItem item, Vec3 at, RandomSource r) {
        level.addParticle(RpgParticles.rune(item.type()),
                at.x + (r.nextDouble() - 0.5) * 0.25, at.y + (r.nextDouble() - 0.5) * 0.2, at.z + (r.nextDouble() - 0.5) * 0.25,
                (r.nextDouble() - 0.5) * 0.01, 0.01 + r.nextDouble() * 0.01, (r.nextDouble() - 0.5) * 0.01);
    }
}
