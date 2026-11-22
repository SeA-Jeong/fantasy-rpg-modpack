package com.fantasyrpg.core.fx;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.gear.rune.RuneType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 입자 종류 (2026-11-22, 사용자 제공 그림 v0.4·v0.6): 룬석 8종 반짝임, 작업대 룬 먼지·불꽃.
 * 그림은 assets/fantasyrpg/particles/*.json → textures/particle/. 언제 뿜을지는 클라이언트(RuneFx, GridBenchBlock.animateTick).
 */
public final class RpgParticles {
    private RpgParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, FantasyRpgCore.MOD_ID);
    /** 이름 → 종류 (클라이언트 그림 연결용) */
    public static final Map<String, DeferredHolder<ParticleType<?>, SimpleParticleType>> ALL = new LinkedHashMap<>();
    private static final Map<RuneType, DeferredHolder<ParticleType<?>, SimpleParticleType>> RUNE = new EnumMap<>(RuneType.class);

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> reg(String name) {
        DeferredHolder<ParticleType<?>, SimpleParticleType> h = PARTICLES.register(name, () -> new SimpleParticleType(false));
        ALL.put(name, h);
        return h;
    }

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BENCH4_DUST = reg("workbench_4x4_rune_dust");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BENCH4_SPARK = reg("workbench_4x4_arc_spark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BENCH5_DUST = reg("workbench_5x5_rune_dust");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BENCH5_SPARK = reg("workbench_5x5_arc_spark");

    /** 계열 갑옷 8~10급 오오라 (사용자 제공 그림 v0.8): armor_<계열>_tNN */
    private static final Map<String, DeferredHolder<ParticleType<?>, SimpleParticleType>> AURA = new LinkedHashMap<>();

    static {
        for (RuneType t : RuneType.values()) RUNE.put(t, reg(t.itemName() + "_spark"));
        for (com.fantasyrpg.core.gear.armor.ArmorFamily f : com.fantasyrpg.core.gear.armor.ArmorFamily.values()) {
            for (int tier = 8; tier <= 10; tier++) {
                String key = "armor_" + f.id() + "_t" + (tier < 10 ? "0" : "") + tier;
                AURA.put(key, reg(key));
            }
        }
    }

    /** 오오라 입자 (8~10급만, 없으면 null) */
    public static SimpleParticleType aura(com.fantasyrpg.core.gear.armor.ArmorFamily f, int tier) {
        if (tier < 8) return null;
        var h = AURA.get("armor_" + f.id() + "_t" + (Math.min(10, tier) < 10 ? "0" : "") + Math.min(10, tier));
        return h == null ? null : h.get();
    }

    public static SimpleParticleType rune(RuneType t) {
        return RUNE.get(t).get();
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
