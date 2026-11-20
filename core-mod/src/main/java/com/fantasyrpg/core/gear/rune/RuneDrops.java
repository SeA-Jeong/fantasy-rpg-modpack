package com.fantasyrpg.core.gear.rune;

import com.fantasyrpg.core.FantasyRpgCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Map;

/** 적대 몬스터 처치 때 룬 원석·하급 룬석 (2026-11-21). 규칙은 {@link RuneDropRules} */
public final class RuneDrops {
    private RuneDrops() {}

    private static volatile RuneDropRules.Settings settings = RuneDropRules.Settings.NONE;

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(), "rune_drops") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller pf) {
                RuneDropRules.Settings s = RuneDropRules.Settings.NONE;
                int bad = 0;
                JsonElement e = files.get(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "settings"));
                try {
                    if (e != null) s = RuneDropRules.parse(e.getAsJsonObject());
                } catch (RuntimeException ex) {
                    bad++;
                    FantasyRpgCore.LOGGER.error("[룬석] 드롭 설정 읽기 실패 — 드롭 없음: {}", ex.getMessage());
                }
                settings = s;
                FantasyRpgCore.LOGGER.info("[룬석] 몬스터 드롭 로드: 원석 {}%, 하급 룬석 {}% ({}종), 오류 {}건",
                        s.oreChance() * 100, s.runeChance() * 100, s.runeTypes().size(), bad);
            }
        });
    }

    public static void onDrops(LivingDropsEvent event) {
        LivingEntity v = event.getEntity();
        if (v.level().isClientSide() || !(v instanceof Enemy)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        for (String t : v.getTags()) if (t.startsWith("magiccity_")) return;                     // 보스·필드보스는 꾸러미로
        RuneDropRules.Settings s = settings;
        var rnd = v.getRandom();
        if (RuneDropRules.ore(s, rnd.nextDouble())) add(event, v, new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "rune_ore"))));
        String type = RuneDropRules.rune(s, rnd.nextDouble(), rnd.nextDouble());
        if (type != null) {
            RuneType rt = RuneType.byId(type);
            if (rt != null) add(event, v, RuneRegistry.runeStack(rt, 1));
        }
    }

    private static void add(LivingDropsEvent event, LivingEntity v, ItemStack s) {
        if (s.isEmpty()) return;
        event.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY() + 0.5, v.getZ(), s));
    }
}
