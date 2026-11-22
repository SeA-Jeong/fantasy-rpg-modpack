package com.fantasyrpg.core.devtest;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 개발자 전용 테스트 갑옷 한 벌 (GeckoLib 입체 모델).
 * 외형은 목록 항목의 경로(모델·텍스처·애니메이션)만 보고 그리므로, 그 파일을 바꾸면 Java 코드를 고칠 일이 없다.
 * 능력치 0 (재질 방어력 0), 내구도 없음, 기존 계열 갑옷 코드(세트·성향·룬석·파손)와 무관.
 */
public class DevTestArmorItem extends ArmorItem implements GeoItem {
    private static final Map<String, String> CLASS_KO = Map.of("warrior", "전사", "knight", "기사", "rogue", "도적",
            "archer", "궁수", "mage", "마법사", "cleric", "전투사제");
    private static final Map<String, String> SLOT_KO = Map.of("helmet", "투구", "chestplate", "흉갑", "leggings", "각반", "boots", "장화");

    private final DevTestManifest.Entry entry;
    private final String namespace;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DevTestArmorItem(Holder<ArmorMaterial> material, Type type, DevTestManifest.Entry entry, String namespace, Properties props) {
        super(material, type, props);
        this.entry = entry;
        this.namespace = namespace;
    }

    public DevTestManifest.Entry entry() {
        return entry;
    }

    public String namespace() {
        return namespace;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("[테스트] " + CLASS_KO.getOrDefault(entry.cls(), entry.cls()) + " " + entry.grade() + "급 "
                + SLOT_KO.getOrDefault(entry.slot(), entry.slot()) + (entry.revision().isEmpty() ? "" : " (" + entry.revision() + ")"))
                .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("개발 테스트 장비 — 능력치 없음").withStyle(ChatFormatting.GRAY));
        if (!entry.note().isEmpty()) tooltip.add(Component.literal(entry.note()).withStyle(ChatFormatting.DARK_GRAY));
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal("모델 " + entry.model()).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("텍스처 " + entry.texture()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (entry.idleAnimation().isEmpty()) return;   // 애니메이션이 없으면 몸 움직임(걷기·팔 흔들기)만 따라감
        RawAnimation idle = RawAnimation.begin().thenLoop(entry.idleAnimation());
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** GeckoLib 이 실제 클라이언트에서만 부른다 (렌더러 클래스는 그때 처음 불러짐) */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HumanoidModel<?> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack,
                                                                                 @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.renderer == null) this.renderer = new com.fantasyrpg.core.client.DevTestArmorRenderer();
                return this.renderer;
            }
        });
    }
}
