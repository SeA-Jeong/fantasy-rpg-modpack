package com.fantasyrpg.core.client;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.network.RpgNetwork;
import com.fantasyrpg.core.skilltree.SkillService;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 클라이언트 전용 진입점: 단축키(K 스킬트리, Z X C V 액티브, B 궁극기)와 HUD.
 * 키는 조작 설정에서 바꿀 수 있다. 사용 요청만 서버로 보내고 판정은 서버가 한다.
 */
@Mod(value = FantasyRpgCore.MOD_ID, dist = Dist.CLIENT)
public final class FantasyRpgClient {
    public static final String CATEGORY = "key.categories.fantasyrpg";
    public static final KeyMapping OPEN_TREE = key("skill_tree", GLFW.GLFW_KEY_K);
    public static final KeyMapping[] SLOTS = {
            key("skill_1", GLFW.GLFW_KEY_Z),
            key("skill_2", GLFW.GLFW_KEY_X),
            key("skill_3", GLFW.GLFW_KEY_C),
            key("skill_4", GLFW.GLFW_KEY_V),
            key("ultimate", GLFW.GLFW_KEY_B),
    };

    private static KeyMapping key(String name, int glfw) {
        return new KeyMapping("key.fantasyrpg." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, glfw, CATEGORY);
    }

    public FantasyRpgClient(IEventBus modBus) {
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> {
            e.register(OPEN_TREE);
            for (KeyMapping k : SLOTS) e.register(k);
        });
        modBus.addListener(RegisterGuiLayersEvent.class, e ->
                e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "rpg_hud"), RpgHud::render));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, FantasyRpgClient::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (OPEN_TREE.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new SkillTreeScreen());
        }
        for (int i = 0; i < SLOTS.length; i++) {
            while (SLOTS[i].consumeClick()) {
                if (mc.screen == null) send(SkillService.ActionType.CAST, "", i);
            }
        }
    }

    public static void send(SkillService.ActionType type, String id, int slot) {
        PacketDistributor.sendToServer(new RpgNetwork.Action(type.ordinal(), id, slot));
    }
}
