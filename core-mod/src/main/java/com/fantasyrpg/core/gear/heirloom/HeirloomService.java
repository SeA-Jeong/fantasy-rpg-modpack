package com.fantasyrpg.core.gear.heirloom;

import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.fantasyrpg.core.gear.weapon.WeaponRegistry;
import com.fantasyrpg.core.gear.weapon.WeaponType;
import com.fantasyrpg.core.playerdata.HeirloomData;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgPlayerData;
import com.fantasyrpg.core.playerdata.RpgProgress;
import io.github.aswq0258.magiccity.api.ChapterFirstClearEvent;
import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 유산 무기 서버 처리 (기획 v0.4 6.1, 2026-11-19 결정, claude/유산무기_초안.md).
 * 진짜 상태는 {@link HeirloomData}(플레이어 데이터), 아이템은 주인·일련번호가 붙은 직업 무기.
 */
public final class HeirloomService {
    private HeirloomService() {}

    /** 봉인을 되찾는 장 (2·3·5·6·9장 = 다섯 봉인, 11장 = 왕관 봉인) — 연출만 */
    private static final Set<Integer> SEAL_CHAPTERS = Set.of(2, 3, 5, 6, 9, 11);
    /** 죽을 때 떨어뜨리지 않고 부활 때 돌려줄 유산 무기 */
    private static final Map<UUID, List<ItemStack>> KEPT = new HashMap<>();

    public static HeirloomSettings settings() {
        return HeirloomManager.INSTANCE.settings();
    }

    private static HeirloomData data(ServerPlayer p) {
        return RpgProgress.get(p).heirloom();
    }

    private static void save(ServerPlayer p, HeirloomData h) {
        RpgProgress.set(p, RpgProgress.get(p).withHeirloom(h));
    }

    public static HeirloomTag tag(ItemStack s) {
        return s.get(GearRegistry.HEIRLOOM.get());
    }

    /** 이 플레이어가 지금 쓸 수 있는 유산 무기인지 (유산 표식이 없는 직업 무기는 시험용이라 허용) */
    public static boolean usable(ServerPlayer p, ItemStack s) {
        HeirloomTag t = tag(s);
        if (t == null) return true;
        HeirloomData h = data(p);
        return h.owned() && t.owner().equals(p.getUUID()) && t.serial() == h.serial();
    }

    private static boolean ownValid(ServerPlayer p, ItemStack s) {
        return tag(s) != null && usable(p, s);
    }

    public static int grade(ServerPlayer p) {
        return MagicCityApi.grade(p);
    }

    /** 특성 합 (physical_bonus·magic_bonus·damage_reduction·crit_chance). 유산이 없거나 설정이 없으면 0 */
    public static double bonus(ServerPlayer p, String key) {
        HeirloomSettings s = settings();
        HeirloomData h = data(p);
        if (s == null || !h.owned()) return 0;
        return s.bonus(h.traits(), key);
    }

    // ───────── 받기·다시 부르기·모양 바꾸기 ─────────

    private static ItemStack makeStack(ServerPlayer p, RpgClass c, HeirloomData h) {
        for (WeaponType t : WeaponType.values()) {
            if (t.rpgClass() != c) continue;
            ItemStack s = WeaponRegistry.stack(t, h.tier());
            s.set(GearRegistry.HEIRLOOM.get(), new HeirloomTag(p.getUUID(), h.serial()));
            if (h.awakened()) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            return s;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 유산 무기를 (다시) 준다. 처음이면 1급 잠든 상태, 이미 있으면 등급·특성을 유지하고 일련번호만 바꿔 옛 아이템을 무효로.
     * @return 준 아이템 (직업이 없으면 빈 것)
     */
    public static ItemStack grant(ServerPlayer p) {
        RpgClass c = RpgProgress.get(p).classOrNull();
        if (c == null) return ItemStack.EMPTY;
        HeirloomData h = data(p);
        h = h.owned() ? h.nextSerial() : HeirloomData.fresh(1);
        if (!h.awakened() && MagicCityApi.cleared(p, 1)) h = h.awaken();   // 1장을 이미 깬 플레이어
        save(p, h);
        ItemStack s = makeStack(p, c, h);
        if (!s.isEmpty()) p.getInventory().placeItemBackInInventory(s.copy());
        return s;
    }

    /** 직업이 바뀌면 모양만 바꿔 다시 줌 (유산이 있을 때만) */
    public static void reform(ServerPlayer p) {
        if (!data(p).owned()) return;
        ItemStack s = grant(p);
        if (!s.isEmpty()) p.sendSystemMessage(Component.translatable("message.fantasyrpg.heirloom.reformed", s.getHoverName()).withStyle(ChatFormatting.GOLD));
    }

    /** 시종장 [유산을 다시 부른다] */
    public static void recall(ServerPlayer p) {
        if (!data(p).owned()) {
            p.sendSystemMessage(Component.translatable("message.fantasyrpg.heirloom.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack s = grant(p);
        p.sendSystemMessage(Component.translatable("message.fantasyrpg.heirloom.recalled", s.getHoverName()).withStyle(ChatFormatting.GOLD));
    }

    /** 가진 유산 무기 아이템의 등급·반짝임을 데이터에 맞춤 */
    public static void refreshStacks(ServerPlayer p) {
        HeirloomData h = data(p);
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!ownValid(p, s)) continue;
            s.set(GearRegistry.GEAR_TIER.get(), h.tier());
            if (h.awakened()) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
    }

    // ───────── 강화·특성 ─────────

    public static boolean enhance(ServerPlayer p) {
        HeirloomSettings s = settings();
        HeirloomData h = data(p);
        if (s == null || !h.owned()) return fail(p, "message.fantasyrpg.heirloom.none");
        HeirloomSettings.Cost cost = s.enhance().get(h.tier() + 1);
        Item item = cost == null ? null : BuiltInRegistries.ITEM.get(ResourceLocation.parse(cost.item()));
        int have = item == null ? 0 : p.getInventory().countItem(item);
        HeirloomRules.EnhanceResult r = HeirloomRules.canEnhance(h.tier(), grade(p), h.awakened(), s, have);
        if (r != HeirloomRules.EnhanceResult.OK) {
            if (r == HeirloomRules.EnhanceResult.NOT_ENOUGH) {
                return fail(p, "message.fantasyrpg.heirloom.not_enough", Component.translatable(item.getDescriptionId()), cost.count(), have);
            }
            return fail(p, "message.fantasyrpg.heirloom." + r.name().toLowerCase(java.util.Locale.ROOT), HeirloomRules.cap(grade(p), h.awakened()));
        }
        final Item use = item;
        p.getInventory().clearOrCountMatchingItems(st -> st.is(use), cost.count(), p.inventoryMenu.getCraftSlots());
        HeirloomData next = h.withTier(h.tier() + 1);
        save(p, next);
        refreshStacks(p);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1F, 1F);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.4, 0.6, 0.4, 0.5);
        p.sendSystemMessage(Component.translatable("message.fantasyrpg.heirloom.enhanced", next.tier()).withStyle(ChatFormatting.GOLD));
        return true;
    }

    public static boolean pickTrait(ServerPlayer p, String direction) {
        HeirloomData h = data(p);
        if (!h.owned()) return fail(p, "message.fantasyrpg.heirloom.none");
        HeirloomRules.PickResult r = HeirloomRules.canPick(direction, grade(p), h.awakened(), h.traits());
        if (r != HeirloomRules.PickResult.OK) return fail(p, "message.fantasyrpg.heirloom.no_pick");
        save(p, h.pick(direction));
        p.sendSystemMessage(Component.translatable("message.fantasyrpg.heirloom.picked",
                Component.translatable("direction.fantasyrpg." + direction)).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** 스킬 전체 초기화와 함께 특성도 비움 */
    public static RpgPlayerData clearTraits(RpgPlayerData d) {
        return d.withHeirloom(d.heirloom().clearTraits());
    }

    private static boolean fail(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
        return false;
    }

    // ───────── 각성 (진행) ─────────

    public static void onChapterFirstClear(ChapterFirstClearEvent e) {
        ServerPlayer p = e.getPlayer();
        HeirloomData h = data(p);
        if (!h.owned()) return;
        if (e.getChapter() == 1 && !h.awakened()) {
            save(p, h.awaken());
            refreshStacks(p);
            narrate(p, "message.fantasyrpg.heirloom.awakened");
            return;
        }
        if (!h.awakened()) return;
        if (e.gradeChanged()) {
            narrate(p, "message.fantasyrpg.heirloom.stage_up", e.getGrade(), HeirloomRules.cap(e.getGrade(), true));
            // 새로 열린 등급의 강화 재료 (메인 진행만으로 재료 확보, 기획 6.2)
            HeirloomSettings s = settings();
            HeirloomSettings.Cost gift = s == null ? null : HeirloomRules.stageUpGift(e.getGrade(), s);
            Item item = gift == null ? null : BuiltInRegistries.ITEM.get(ResourceLocation.parse(gift.item()));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                p.getInventory().placeItemBackInInventory(new ItemStack(item, gift.count()));
                p.sendSystemMessage(Component.translatable("message.fantasyrpg.heirloom.gift",
                        Component.translatable(item.getDescriptionId()), gift.count()).withStyle(ChatFormatting.GOLD));
            }
        } else if (SEAL_CHAPTERS.contains(e.getChapter())) {
            narrate(p, "message.fantasyrpg.heirloom.seal");
        }
    }

    /** 접속: 이미 1장을 깬 플레이어는 깨어난 상태로 맞춤, 아이템 등급 맞춤 */
    public static void onLogin(ServerPlayer p) {
        HeirloomData h = data(p);
        if (h.owned() && !h.awakened() && MagicCityApi.cleared(p, 1)) save(p, h.awaken());
        refreshStacks(p);
    }

    private static void narrate(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD));
        p.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.2F);
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
    }

    // ───────── 죽어도 떨어뜨리지 않음 ─────────

    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        List<ItemStack> kept = new ArrayList<>();
        e.getDrops().removeIf(ie -> {
            ItemStack s = ie.getItem();
            if (!ownValid(p, s)) return false;
            kept.add(s.copy());
            return true;
        });
        if (!kept.isEmpty()) KEPT.put(p.getUUID(), kept);
    }

    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath() || !(e.getEntity() instanceof ServerPlayer p)) return;
        List<ItemStack> kept = KEPT.remove(p.getUUID());
        if (kept != null) for (ItemStack s : kept) p.getInventory().add(s);
    }


}
