package com.fantasyrpg.core.gear.enchant;

import com.fantasyrpg.core.FantasyRpgCore;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 바닐라 전투 인챈트 정리 (정책 P2, 2026-11-21). 규칙은 {@link EnchantRules}.
 * 효과 없음·획득 차단·보호 재정의는 데이터(모드 안 data/minecraft/enchantment, tags/enchantment),
 * 여기서는 ① 플레이어가 가진 아이템·마법이 부여된 책에서 전투 인챈트를 지우고 ② 서버 시작 때 데이터가 실제로 적용됐는지 검사.
 * 약탈은 전리품 표가 무기의 인챈트 레벨을 직접 읽으므로(1.21) 효과를 비워도 남아 있으면 작동 → 지우는 것이 확실한 방법.
 */
public final class CombatEnchants {
    private CombatEnchants() {}

    public static final TagKey<Enchantment> DISABLED = TagKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "disabled_combat"));

    /** 플레이어 인벤토리 검사 주기 (틱) */
    private static final int SCAN_TICKS = 40;

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) strip(p);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % SCAN_TICKS == 7) strip(p);
    }

    /** 인벤토리 전체(갑옷·보조 손 포함)에서 전투 인챈트를 지움. 지운 개수 */
    public static int strip(ServerPlayer p) {
        Inventory inv = p.getInventory();
        int removed = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            int n = stripStack(s);
            if (n > 0) {
                removed += n;
                if (s.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantmentsForCrafting(s).isEmpty()) {
                    ItemStack book = new ItemStack(Items.BOOK, s.getCount());
                    if (s.has(DataComponents.CUSTOM_NAME)) book.set(DataComponents.CUSTOM_NAME, s.get(DataComponents.CUSTOM_NAME));
                    inv.setItem(i, book);
                }
            }
        }
        ItemStack carried = p.containerMenu.getCarried();
        if (!carried.isEmpty()) removed += stripStack(carried);
        if (removed > 0)
            p.displayClientMessage(Component.literal("§7전투 인챈트 " + removed + "개를 지웠습니다 (이 세계에서는 효과가 없음 — 내구성·수선 등은 그대로)"), true);
        return removed;
    }

    /** 한 아이템(책은 저장된 인챈트)에서 전투 인챈트를 지움. 지운 개수 */
    public static int stripStack(ItemStack s) {
        ItemEnchantments e = EnchantmentHelper.getEnchantmentsForCrafting(s);
        if (e.isEmpty()) return 0;
        int n = 0;
        for (Holder<Enchantment> h : e.keySet()) if (h.is(DISABLED)) n++;
        if (n == 0) return 0;
        EnchantmentHelper.updateEnchantments(s, m -> m.removeIf(h -> h.is(DISABLED)));
        return n;
    }

    /** 서버 시작 때: 전투 인챈트 정의가 실제로 비었는지, 보호 최대 레벨이 바뀌었는지 */
    public static void onServerStarted(ServerStartedEvent event) {
        Registry<Enchantment> reg = event.getServer().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        int empty = 0, total = 0, bad = 0;
        for (String id : EnchantRules.DISABLED) {
            var e = reg.getHolder(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(id)));
            if (e.isEmpty()) continue;
            total++;
            if (e.get().value().effects().isEmpty()) empty++;
            else {
                bad++;
                FantasyRpgCore.LOGGER.error("[인챈트] {} 에 효과가 남아 있음 — 다른 데이터팩이 덮어썼을 수 있음", id);
            }
            if (!e.get().is(DISABLED)) {
                bad++;
                FantasyRpgCore.LOGGER.error("[인챈트] {} 이 fantasyrpg:disabled_combat 태그에 없음", id);
            }
        }
        int prot = maxLevel(reg, "protection"), fire = maxLevel(reg, "fire_protection"),
                blast = maxLevel(reg, "blast_protection"), proj = maxLevel(reg, "projectile_protection");
        boolean hasProt = reg.getHolder(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("protection")))
                .map(h -> !h.value().getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION).isEmpty()).orElse(false);
        if (prot != 3 || fire != 2 || blast != 2 || proj != 2 || !hasProt) bad++;
        FantasyRpgCore.LOGGER.info("[인챈트] 전투 인챈트 {}종 중 효과 없음 {}종, 보호 최대 레벨 {}·{}·{}·{}, 오류 {}건",
                total, empty, prot, fire, blast, proj, bad);
    }

    private static int maxLevel(Registry<Enchantment> reg, String id) {
        return reg.getHolder(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(id)))
                .map(h -> h.value().getMaxLevel()).orElse(-1);
    }
}
