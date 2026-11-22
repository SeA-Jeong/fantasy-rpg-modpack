package com.fantasyrpg.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** 개발 테스트: 서버 명령 {@code /fantasyrpgtest armor reload} 를 받으면 리소스를 다시 읽는다 (F3+T 와 같은 동작) */
public final class DevTestClient {
    private DevTestClient() {
    }

    public static void reloadResources() {
        Minecraft mc = Minecraft.getInstance();
        mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("[개발 테스트] 리소스 다시 읽기 끝"), false);
        }));
    }
}
