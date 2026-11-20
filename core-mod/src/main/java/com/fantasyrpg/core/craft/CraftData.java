package com.fantasyrpg.core.craft;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.craft.CraftRules.ArmorTable;
import com.fantasyrpg.core.craft.CraftRules.GridRecipe;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 작업대 조합식(grid_recipes/*.json)·방어구 제작표(armor_crafting/costs.json) 로더 */
public final class CraftData {
    private CraftData() {
    }

    private static volatile List<GridRecipe> recipes = List.of();
    private static volatile ArmorTable armor;

    public static List<GridRecipe> recipes() {
        return recipes;
    }

    public static ArmorTable armor() {
        return armor;
    }

    /** 아이템 id 가 실제 아이템인지 (태그는 통과) */
    private static void check(String id) {
        if (id.startsWith("#") || id.equals("core")) return;
        if (BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)) == Items.AIR) throw new IllegalArgumentException("모르는 아이템: " + id);
    }

    public static final SimpleJsonResourceReloadListener RECIPES = new SimpleJsonResourceReloadListener(new Gson(), "grid_recipes") {
        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller pf) {
            List<GridRecipe> out = new ArrayList<>();
            int bad = 0;
            for (var e : map.entrySet()) {
                if (e.getKey().getPath().startsWith("_")) continue;
                try {
                    GridRecipe r = CraftRules.parseRecipe(e.getKey().toString(), e.getValue().getAsJsonObject());
                    r.ingredients().forEach(i -> check(i.id()));
                    check(r.result());
                    out.add(r);
                } catch (RuntimeException ex) {
                    bad++;
                    FantasyRpgCore.LOGGER.error("[제작] 작업대 조합식 {} 읽기 실패: {}", e.getKey(), ex.getMessage());
                }
            }
            recipes = List.copyOf(out);
            FantasyRpgCore.LOGGER.info("[제작] 작업대 조합식 {}개 로드, 오류 {}건", out.size(), bad);
        }
    };

    public static final SimpleJsonResourceReloadListener ARMOR = new SimpleJsonResourceReloadListener(new Gson(), "armor_crafting") {
        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller pf) {
            JsonElement json = map.get(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "costs"));
            if (json == null) {
                FantasyRpgCore.LOGGER.error("[제작] armor_crafting/costs.json 없음 — 계열 갑옷 만들기·승급이 꺼짐");
                return;
            }
            try {
                ArmorTable t = CraftRules.parseArmor(json.getAsJsonObject());
                t.core().values().forEach(CraftData::check);
                t.upgrade().values().forEach(m -> m.values().forEach(l -> l.forEach(i -> check(i.id()))));
                t.create().values().forEach(c -> c.items().forEach(i -> check(i.id())));
                armor = t;
                FantasyRpgCore.LOGGER.info("[제작] 방어구 제작표 로드: 계열 {}개, 승급 2~{}급, 오류 0건", t.weight().size(), CraftRules.MAX_UPGRADE);
            } catch (RuntimeException ex) {
                FantasyRpgCore.LOGGER.error("[제작] costs.json 읽기 실패, 이전 값 유지: {}", ex.getMessage());
            }
        }
    };
}
