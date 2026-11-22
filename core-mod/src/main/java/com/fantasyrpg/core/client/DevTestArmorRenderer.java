package com.fantasyrpg.core.client;

import com.fantasyrpg.core.devtest.DevTestArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 개발자 전용 테스트 갑옷 렌더러 (GeckoLib {@link GeoArmorRenderer}).
 * <p>
 * 경로는 아이템의 목록 항목에서 읽는다 → 모델·텍스처 파일만 바꾸면 되고, F3+T 로 다시 읽힌다(GeckoLib 캐시·텍스처 모두).
 * <p>
 * 본 규칙(GeckoLib 갑옷): armorHead·armorBody·armorRightArm·armorLeftArm·armorRightLeg·armorLeftLeg·armorRightBoot·armorLeftBoot.
 * 각 본은 플레이어의 같은 부위를 따라 움직인다(걷기·팔 흔들기·웅크리기·공격). Bedrock 좌표 x&lt;0 이 캐릭터 오른쪽.
 * <p>
 * 흉갑은 머리 본·다리 본도 그린다 — 후드·마스크(머리를 따라 돎), 로브 하단·코트 자락(다리를 따라 움직임)용.
 * GeckoLib 기본은 흉갑 칸에서 몸통·팔만 보여 주므로 여기서 머리·다리도 켠다 (부품이 없는 본은 그려도 아무것도 없음).
 */
public final class DevTestArmorRenderer extends GeoArmorRenderer<DevTestArmorItem> {
    public DevTestArmorRenderer() {
        super(new Model());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);
        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.head, true);
            setBoneVisible(this.rightLeg, true);
            setBoneVisible(this.leftLeg, true);
        }
    }

    private static final class Model extends GeoModel<DevTestArmorItem> {
        private static final String EMPTY_ANIMATION = "animations/test_armor/empty.animation.json";

        @Override
        public ResourceLocation getModelResource(DevTestArmorItem item) {
            return ResourceLocation.fromNamespaceAndPath(item.namespace(), item.entry().model());
        }

        @Override
        public ResourceLocation getTextureResource(DevTestArmorItem item) {
            return ResourceLocation.fromNamespaceAndPath(item.namespace(), item.entry().texture());
        }

        @Override
        public ResourceLocation getAnimationResource(DevTestArmorItem item) {
            String a = item.entry().animation();
            return ResourceLocation.fromNamespaceAndPath(item.namespace(), a.isEmpty() ? EMPTY_ANIMATION : a);
        }
    }
}
