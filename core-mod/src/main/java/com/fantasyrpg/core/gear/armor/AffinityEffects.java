package com.fantasyrpg.core.gear.armor;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.gear.armor.AffinityRules.Affinity;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgPlayerData;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.fantasyrpg.core.skilltree.SkillData;
import com.fantasyrpg.core.skilltree.SkillNode;
import com.fantasyrpg.core.skilltree.SkillTreeRules;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 방어구 성향 효과 (2026-11-21). 10틱마다 입은 갑옷(파손 제외)의 성향을 모아 합계를 계산한다.
 * 체력·스킬 가속은 플레이어 속성 수정자 {@code fantasyrpg:affinity/...}, 피해·치명타 보너스는 {@link #bonus}로 전투 계산에서 읽는다.
 */
public final class AffinityEffects {
    private AffinityEffects() {}

    private static volatile AffinityRules.Settings settings = AffinityRules.Settings.DEFAULT;
    private static final Map<UUID, Map<String, Double>> TOTALS = new ConcurrentHashMap<>();
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "affinity/health");
    private static final ResourceLocation HASTE_ID = ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "affinity/haste");

    public static AffinityRules.Settings settings() {
        return settings;
    }

    private static final SimpleJsonResourceReloadListener LISTENER = new SimpleJsonResourceReloadListener(new Gson(), "armor_affinity") {
        {
            com.fantasyrpg.core.network.DataSync.register("armor_affinity", f -> apply(f, null, null));
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller pf) {
            com.fantasyrpg.core.network.DataSync.remember("armor_affinity", files);
            JsonElement json = files.get(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "settings"));
            if (json == null) {
                FantasyRpgCore.LOGGER.error("[방어구] armor_affinity/settings.json 없음 — 기본값 사용");
                return;
            }
            try {
                settings = AffinityRules.parse(json.getAsJsonObject());
                FantasyRpgCore.LOGGER.info("[방어구] 성향 {}종 로드 (공명 {}부위 + 방향 SP {} → ×{}), 오류 0건",
                        settings.options().size(), settings.resonancePieces(), settings.resonanceSp(), settings.resonanceMult());
            } catch (RuntimeException ex) {
                FantasyRpgCore.LOGGER.error("[방어구] 성향 설정 읽기 실패, 이전 값 유지: {}", ex.getMessage());
            }
        }
    };

    public static void bootstrap() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(LISTENER);
    }

    /** 갑옷의 성향 (없으면 균형) */
    public static Affinity of(ItemStack stack) {
        return Affinity.byId(stack.get(GearRegistry.AFFINITY.get()));
    }

    /** 전투 계산용 합계 (physical_bonus·magic_bonus·skill_magic_bonus·crit_chance) */
    public static double bonus(ServerPlayer p, String key) {
        Map<String, Double> t = TOTALS.get(p.getUUID());
        return t == null ? 0 : t.getOrDefault(key, 0.0);
    }

    /** 트리 방향별 쓴 SP */
    public static Map<Affinity, Integer> directionSp(RpgPlayerData d) {
        Map<Affinity, Integer> out = new EnumMap<>(Affinity.class);
        RpgClass c = d.classOrNull();
        if (c == null) return out;
        Map<String, SkillNode> nodes = SkillData.nodes(c.id());
        for (SkillNode.Direction dir : SkillNode.Direction.values()) {
            out.put(Affinity.valueOf(dir.name()), SkillTreeRules.spentInDirection(dir, d.nodeRanks(), nodes, ""));
        }
        return out;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        List<AffinityRules.Piece> worn = new ArrayList<>();
        for (EquipmentSlot s : ARMOR) {
            ItemStack st = p.getItemBySlot(s);
            if (st.getItem() instanceof FamilyArmorItem && !FamilyArmorItem.isBroken(st))
                worn.add(new AffinityRules.Piece(of(st), GearRegistry.tierOf(st)));
        }
        Map<String, Double> t = worn.isEmpty() ? Map.of() : AffinityRules.total(settings, worn, directionSp(RpgProgress.get(p)));
        TOTALS.put(p.getUUID(), t);
        modifier(p, Attributes.MAX_HEALTH, HEALTH_ID, t.getOrDefault("max_health", 0.0));
        modifier(p, com.fantasyrpg.core.gear.RpgAttributes.SKILL_HASTE, HASTE_ID, t.getOrDefault("skill_haste", 0.0));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void modifier(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double value) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier old = inst.getModifier(id);
        if (value == 0) {
            if (old != null) inst.removeModifier(id);
            return;
        }
        if (old != null && old.amount() == value) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void forget(ServerPlayer p) {
        TOTALS.remove(p.getUUID());
    }
}
