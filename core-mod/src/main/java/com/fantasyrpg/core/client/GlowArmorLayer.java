package com.fantasyrpg.core.client;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.gear.armor.FamilyArmorItem;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * 입은 계열 갑옷 빛 (2026-11-22, 사용자 제공 그림): 8~10급은 마력 무늬 마스크
 * textures/models/armor/<계열>_tNN_layer_1|2_emissive.png 를 바닐라 갑옷 모양 위에 최대 밝기로 한 번 더 그림.
 * 파손된 갑옷은 빛이 꺼짐. 플레이어에게만.
 */
public final class GlowArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final int MIN_TIER = 8;
    private final HumanoidArmorModel<AbstractClientPlayer> inner, outer;

    public GlowArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.inner = new HumanoidArmorModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        this.outer = new HumanoidArmorModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot);
            if (!(s.getItem() instanceof FamilyArmorItem fa) || FamilyArmorItem.isBroken(s)) continue;
            int tier = GearRegistry.tierOf(s);
            if (tier < MIN_TIER) continue;
            boolean legs = slot == EquipmentSlot.LEGS;
            HumanoidArmorModel<AbstractClientPlayer> model = legs ? inner : outer;
            getParentModel().copyPropertiesTo(model);
            show(model, slot);
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, String.format(Locale.ROOT,
                    "textures/models/armor/%s_t%02d_layer_%d_emissive.png", fa.family().id(), Math.min(10, tier), legs ? 2 : 1));
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(tex)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }

    /** 바닐라 HumanoidArmorLayer 와 같은 부위 표시 */
    private static void show(HumanoidModel<?> m, EquipmentSlot slot) {
        m.setAllVisible(false);
        switch (slot) {
            case HEAD -> { m.head.visible = true; m.hat.visible = true; }
            case CHEST -> { m.body.visible = true; m.rightArm.visible = true; m.leftArm.visible = true; }
            case LEGS -> { m.body.visible = true; m.rightLeg.visible = true; m.leftLeg.visible = true; }
            case FEET -> { m.rightLeg.visible = true; m.leftLeg.visible = true; }
            default -> { }
        }
    }
}
