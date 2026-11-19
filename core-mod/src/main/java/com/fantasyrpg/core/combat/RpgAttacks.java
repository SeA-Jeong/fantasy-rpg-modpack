package com.fantasyrpg.core.combat;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.fantasyrpg.core.playerdata.RpgClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기본 공격 공통 처리: 무기 사실(태그), 피해 입력 만들기, 치명타 굴림, 거부 안내.
 * 직업 무기는 아이템 태그 {@code fantasyrpg:class_weapons/<직업>/<melee|ranged>}로 판정한다(다른 모드 무기도 태그로 추가 가능).
 */
public final class RpgAttacks {
    private RpgAttacks() {
    }

    private static final Map<String, TagKey<Item>> TAGS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_NOTICE = new ConcurrentHashMap<>();

    private static TagKey<Item> tag(RpgClass c, WeaponPermission.AttackKind kind) {
        String path = "class_weapons/" + c.id() + "/" + (kind == WeaponPermission.AttackKind.MELEE ? "melee" : "ranged");
        return TAGS.computeIfAbsent(path, p -> TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, p)));
    }

    public static WeaponPermission.WeaponFacts facts(ItemStack stack, WeaponPermission.AttackKind kind) {
        if (stack.isEmpty()) return WeaponPermission.WeaponFacts.emptyHand();
        Set<String> classes = new HashSet<>();
        for (RpgClass c : RpgClass.values()) {
            if (stack.is(tag(c, kind))) classes.add(c.id());
        }
        return new WeaponPermission.WeaponFacts(false, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), classes, Set.of());
    }

    public static WeaponPermission.Verdict check(RpgClass c, WeaponPermission.AttackKind kind, ItemStack stack) {
        return WeaponPermission.check(c.id(), kind, facts(stack, kind), Set.of());
    }

    /** 직업 판정 + 유산 무기 주인·일련번호 확인 */
    public static WeaponPermission.Verdict check(ServerPlayer p, RpgClass c, WeaponPermission.AttackKind kind, ItemStack stack) {
        WeaponPermission.Verdict v = check(c, kind, stack);
        if (v == WeaponPermission.Verdict.ALLOW && !com.fantasyrpg.core.gear.heirloom.HeirloomService.usable(p, stack)) {
            return WeaponPermission.Verdict.DENY_NOT_OWNER;
        }
        return v;
    }

    /**
     * 직업 무기 기본 공격 입력. factor = 콤보 × 충전(또는 활 당김) 배율. 치명타는 확률로 굴린다.
     */
    public static DamageInput basic(CombatSettings s, double physical, double magic, ItemStack weapon, double factor,
                                    ServerPlayer p) {
        return basic(s, s.power(GearRegistry.tierOf(weapon)), physical, magic, factor, p);
    }

    /** 치명타 확률 보너스(연계 감각 등)를 더하는 판 */
    public static DamageInput basic(CombatSettings s, double physical, double magic, ItemStack weapon, double factor,
                                    ServerPlayer p, double critBonus) {
        DamageInput in = DamageInput.of(s.power(GearRegistry.tierOf(weapon)), physical * factor, magic * factor);
        if (p.getRandom().nextDouble() < s.critChance() + critBonus) in = in.withCrit(s.critMultiplier());
        return in;
    }

    /** 스킬 패시브를 반영한 판: 치명타 확률 보너스, 치명타 확정, 치명타 배율 보너스 */
    public static DamageInput basic(CombatSettings s, double weaponPower, double physical, double magic, double factor,
                                    ServerPlayer p, double critBonus, boolean forceCrit, double critMultBonus) {
        DamageInput in = DamageInput.of(weaponPower, physical * factor, magic * factor);
        if (forceCrit || p.getRandom().nextDouble() < s.critChance() + critBonus) in = in.withCrit(s.critMultiplier() + critMultBonus);
        return in;
    }

    /** W를 직접 넘기는 판 (쏜 화살처럼 무기 아이템이 손에 없을 때) */
    public static DamageInput basic(CombatSettings s, double weaponPower, double physical, double magic, double factor,
                                    ServerPlayer p) {
        DamageInput in = DamageInput.of(weaponPower, physical * factor, magic * factor);
        if (p.getRandom().nextDouble() < s.critChance()) in = in.withCrit(s.critMultiplier());
        return in;
    }

    /** 맨손: 고정 피해 × 충전 배율, 계수·장비·치명타 없음 */
    public static DamageInput unarmed(CombatSettings s, double charge) {
        return DamageInput.of(s.unarmedDamage(), BasicAttackMath.chargeScale(charge), 0);
    }

    /** 무기 거부 안내 (액션바, 플레이어마다 1초에 한 번) */
    public static void notice(ServerPlayer p, WeaponPermission.Verdict v) {
        long now = p.level().getGameTime();
        Long last = LAST_NOTICE.get(p.getUUID());
        if (last != null && now - last < 20) return;
        LAST_NOTICE.put(p.getUUID(), now);
        String key = v == WeaponPermission.Verdict.DENY_OTHER_CLASS ? "message.fantasyrpg.weapon_other_class"
                : v == WeaponPermission.Verdict.DENY_NOT_OWNER ? "message.fantasyrpg.heirloom.not_owner"
                : "message.fantasyrpg.weapon_not_weapon";
        p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }
}
