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
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent.class, e -> {
            e.register(com.fantasyrpg.core.craft.CraftRegistry.MENU_4.get(), GridCraftScreen::new);
            e.register(com.fantasyrpg.core.craft.CraftRegistry.MENU_5.get(), GridCraftScreen::new);
        });
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, FantasyRpgClient::onClientTick);
        // 직업 무기 등급별 모양 (2026-11-21): 모델 overrides 가 fantasyrpg:tier (= 등급 ÷ 10) 로 _t01~_t10 을 고름
        modBus.addListener(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent.class, e -> e.enqueueWork(() -> {
            for (com.fantasyrpg.core.gear.weapon.WeaponType t : com.fantasyrpg.core.gear.weapon.WeaponType.values()) {
                net.minecraft.client.renderer.item.ItemProperties.register(com.fantasyrpg.core.gear.weapon.WeaponRegistry.item(t),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "tier"),
                        (stack, level, entity, seed) -> com.fantasyrpg.core.gear.armor.GearRegistry.tierOf(stack) / 10f);
            }
            // 장비 그림 v0.7: 계열 갑옷 등급별 아이콘, 룬석·연마재 등급별 그림, 활 당기기
            net.minecraft.resources.ResourceLocation tierKey = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "tier");
            for (com.fantasyrpg.core.gear.armor.ArmorFamily f : com.fantasyrpg.core.gear.armor.ArmorFamily.values()) {
                for (com.fantasyrpg.core.gear.armor.ArmorPart p : com.fantasyrpg.core.gear.armor.ArmorPart.values()) {
                    net.minecraft.client.renderer.item.ItemProperties.register(com.fantasyrpg.core.gear.armor.GearRegistry.item(f, p), tierKey,
                            (stack, level, entity, seed) -> com.fantasyrpg.core.gear.armor.GearRegistry.tierOf(stack) / 10f);
                }
            }
            net.minecraft.resources.ResourceLocation gradeKey = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "grade");
            java.util.List<net.minecraft.world.item.Item> graded = new java.util.ArrayList<>();
            for (com.fantasyrpg.core.gear.rune.RuneType rt : com.fantasyrpg.core.gear.rune.RuneType.values()) graded.add(com.fantasyrpg.core.gear.rune.RuneRegistry.item(rt));
            graded.add(com.fantasyrpg.core.gear.rune.RuneRegistry.ABRASIVE.get());
            for (net.minecraft.world.item.Item it : graded) {
                net.minecraft.client.renderer.item.ItemProperties.register(it, gradeKey,
                        (stack, level, entity, seed) -> com.fantasyrpg.core.gear.rune.RuneRegistry.gradeOf(stack) / 10f);
            }
            // 우리 활은 바닐라 활과 다른 아이템이라 당기기 속성(pull·pulling)을 따로 등록 (바닐라와 같은 계산)
            net.minecraft.world.item.Item bow = com.fantasyrpg.core.gear.weapon.WeaponRegistry.item(com.fantasyrpg.core.gear.weapon.WeaponType.BOW);
            net.minecraft.client.renderer.item.ItemProperties.register(bow, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0f
                            : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20f);
            net.minecraft.client.renderer.item.ItemProperties.register(bow, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
        }));
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
