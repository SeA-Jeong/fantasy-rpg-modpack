package com.fantasyrpg.core.client;

import com.fantasyrpg.core.network.DataSync;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

/** 클라이언트 쪽 데이터 동기화 받기 (2026-11-21) */
public final class ClientDataSync {
    private ClientDataSync() {}

    public static void accept(String dir, String json) {
        if (Minecraft.getInstance().getSingleplayerServer() != null) return;   // 혼자 하기: 이미 같은 값
        DataSync.apply(dir, json);
        if (("grid_recipes".equals(dir) || "armor_crafting".equals(dir)) && ModList.get().isLoaded("jei")) {
            com.fantasyrpg.core.compat.jei.FantasyJeiPlugin.refresh();
        }
    }
}
