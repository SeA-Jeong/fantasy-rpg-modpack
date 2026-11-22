package com.fantasyrpg.core.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** 빛나는 작은 반짝임 (룬석·작업대, 2026-11-22): 4프레임 그림을 나이에 따라 넘기고, 천천히 떠오르며 흐려짐. 항상 최대 밝기 */
public final class GlowSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    GlowSparkParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = 18 + random.nextInt(12);
        this.quadSize = 0.06f + random.nextFloat() * 0.04f;
        this.gravity = -0.02f;
        this.friction = 0.92f;
        this.hasPhysics = false;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(sprites);
            alpha = 1f - Math.max(0f, (age - lifetime * 0.6f) / (lifetime * 0.4f));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new GlowSparkParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
