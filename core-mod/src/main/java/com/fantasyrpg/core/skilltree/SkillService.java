package com.fantasyrpg.core.skilltree;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.network.RpgNetwork;
import com.fantasyrpg.core.playerdata.ProgressionManager;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgPlayerData;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.fantasyrpg.core.combat.RpgAttacks;
import com.fantasyrpg.core.combat.WeaponPermission;
import com.fantasyrpg.core.skills.Cast;
import com.fantasyrpg.core.skills.SkillIds;
import com.fantasyrpg.core.skills.SkillMath;
import com.fantasyrpg.core.skills.SkillPassives;
import com.fantasyrpg.core.skills.SkillRuntime;
import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 스킬트리·공통 패시브·초기화·장착·스킬 사용의 서버 처리 (claude/스킬트리_설계.md).
 * 클라이언트 요청({@link RpgNetwork.Action})과 시험 명령이 이 함수들을 부른다. 모든 판정은 여기서.
 */
public final class SkillService {
    private SkillService() {
    }

    public enum ActionType { INVEST, REFUND, FULL_RESET, EQUIP, UNEQUIP, PASSIVE_INVEST, PASSIVE_REFUND, CAST }

    public static void handle(ServerPlayer p, int action, String id, int slot) {
        if (action < 0 || action >= ActionType.values().length) return;
        switch (ActionType.values()[action]) {
            case INVEST -> invest(p, id);
            case REFUND -> refund(p, id);
            case FULL_RESET -> fullReset(p, false);
            case EQUIP -> equip(p, id, slot);
            case UNEQUIP -> unequip(p, slot);
            case PASSIVE_INVEST -> passiveInvest(p, id);
            case PASSIVE_REFUND -> passiveRefund(p, id);
            case CAST -> cast(p, slot);
        }
    }

    private static void tell(ServerPlayer p, String key, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    private static void fail(ServerPlayer p, Enum<?> result) {
        tell(p, "message.fantasyrpg.skill." + result.name().toLowerCase(java.util.Locale.ROOT), ChatFormatting.RED);
    }

    public static boolean invest(ServerPlayer p, String nodeId) {
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        if (c == null) {
            tell(p, "message.fantasyrpg.skill.no_class", ChatFormatting.RED);
            return false;
        }
        Map<String, SkillNode> nodes = SkillData.nodes(c.id());
        SkillTreeRules.Result r = SkillTreeRules.canInvest(nodeId, c.id(), d.level(), d.spTotal(), d.nodeRanks(), nodes);
        if (r != SkillTreeRules.Result.OK) {
            fail(p, r);
            return false;
        }
        RpgProgress.set(p, d.withNodeRanks(SkillTreeRules.withRank(d.nodeRanks(), nodeId, 1), d.slots()));
        return true;
    }

    /** 노드 하나 1랭크 되돌리기. 비용 [미정]: 데이터가 정해지기 전까지 무료 */
    public static boolean refund(ServerPlayer p, String nodeId) {
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        if (c == null) return false;
        SkillTreeRules.Result r = SkillTreeRules.canRefund(nodeId, d.nodeRanks(), SkillData.nodes(c.id()));
        if (r != SkillTreeRules.Result.OK) {
            fail(p, r);
            return false;
        }
        Map<String, Integer> ranks = SkillTreeRules.withRank(d.nodeRanks(), nodeId, -1);
        RpgProgress.set(p, d.withNodeRanks(ranks, ranks.containsKey(nodeId) ? d.slots() : clearSlot(d.slots(), nodeId)));
        return true;
    }

    /** 전투 트리 전체 초기화. 무료 초기화권을 쓰고, 없으면 비용 [미정]이라 거부(시험 명령은 force) */
    public static boolean fullReset(ServerPlayer p, boolean force) {
        RpgPlayerData d = RpgProgress.get(p);
        if (d.nodeRanks().isEmpty()) {
            tell(p, "message.fantasyrpg.skill.nothing_to_reset", ChatFormatting.GRAY);
            return false;
        }
        int available = freeResets(p, d);
        if (!force && available <= 0) {
            tell(p, "message.fantasyrpg.skill.no_free_reset", ChatFormatting.RED);
            return false;
        }
        RpgPlayerData next = d.withNodeRanks(Map.of(), RpgPlayerData.EMPTY_SLOTS);
        if (!force) next = next.withFreeResetsUsed(d.freeResetsUsed() + 1);
        RpgProgress.set(p, next);
        tell(p, "message.fantasyrpg.skill.reset_done", ChatFormatting.GREEN, d.spSpent());
        return true;
    }

    public static int freeResets(ServerPlayer p, RpgPlayerData d) {
        return ResetRules.freeResetsAvailable(d.classOrNull() != null, MagicCityApi.completed(p), d.freeResetsUsed(), false);
    }

    /** 액티브는 0~3칸, 궁극기는 4칸 */
    public static boolean equip(ServerPlayer p, String nodeId, int slot) {
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        if (c == null || slot < 0 || slot >= RpgPlayerData.SLOT_COUNT) return false;
        SkillNode n = SkillData.nodes(c.id()).get(nodeId);
        if (n == null || d.nodeRanks().getOrDefault(nodeId, 0) <= 0) {
            tell(p, "message.fantasyrpg.skill.not_learned", ChatFormatting.RED);
            return false;
        }
        boolean ultimate = n.kind() == SkillNode.Kind.ULTIMATE;
        if (!n.kind().equippable() || ultimate != (slot == RpgPlayerData.ULTIMATE_SLOT)) {
            tell(p, "message.fantasyrpg.skill.wrong_slot", ChatFormatting.RED);
            return false;
        }
        List<String> slots = new ArrayList<>(clearSlot(d.slots(), nodeId));
        slots.set(slot, nodeId);
        RpgProgress.set(p, d.withNodeRanks(d.nodeRanks(), slots));
        return true;
    }

    public static boolean unequip(ServerPlayer p, int slot) {
        RpgPlayerData d = RpgProgress.get(p);
        if (slot < 0 || slot >= RpgPlayerData.SLOT_COUNT) return false;
        List<String> slots = new ArrayList<>(d.slots());
        slots.set(slot, "");
        RpgProgress.set(p, d.withNodeRanks(d.nodeRanks(), slots));
        return true;
    }

    private static List<String> clearSlot(List<String> slots, String nodeId) {
        List<String> out = new ArrayList<>(slots);
        for (int i = 0; i < out.size(); i++) if (out.get(i).equals(nodeId)) out.set(i, "");
        return out;
    }

    public static boolean passiveInvest(ServerPlayer p, String nodeId) {
        RpgPlayerData d = RpgProgress.get(p);
        PassiveNode.Result r = PassiveNode.canInvest(nodeId, MagicCityApi.grade(p), d.passiveRanks(), SkillData.passives());
        if (r != PassiveNode.Result.OK) {
            tell(p, "message.fantasyrpg.passive." + r.name().toLowerCase(java.util.Locale.ROOT), ChatFormatting.RED);
            return false;
        }
        RpgProgress.set(p, d.withPassiveRanks(SkillTreeRules.withRank(d.passiveRanks(), nodeId, 1)));
        applyPassives(p);
        return true;
    }

    /** 공통 패시브 1랭크 되돌리기. 비용 [미정]: 지금은 무료 */
    public static boolean passiveRefund(ServerPlayer p, String nodeId) {
        RpgPlayerData d = RpgProgress.get(p);
        if (d.passiveRanks().getOrDefault(nodeId, 0) <= 0) return false;
        RpgProgress.set(p, d.withPassiveRanks(SkillTreeRules.withRank(d.passiveRanks(), nodeId, -1)));
        applyPassives(p);
        return true;
    }

    /** 공통 패시브 효과를 플레이어 속성에 다시 붙인다 (임시 수정자, 접속·부활·변경·/reload 때) */
    public static void applyPassives(ServerPlayer p) {
        RpgPlayerData d = RpgProgress.get(p);
        for (PassiveNode n : SkillData.passives().values()) {
            Holder<Attribute> attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(n.attribute())).orElse(null);
            if (attr == null) continue;
            AttributeInstance inst = p.getAttribute(attr);
            if (inst == null) continue;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(com.fantasyrpg.core.FantasyRpgCore.MOD_ID,
                    "passive/" + n.id().replace(':', '/'));
            inst.removeModifier(id);
            int rank = d.passiveRanks().getOrDefault(n.id(), 0);
            if (rank > 0) {
                inst.addTransientModifier(new AttributeModifier(id, n.perRank() * rank, switch (n.operation()) {
                    case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    default -> AttributeModifier.Operation.ADD_VALUE;
                }));
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** 장착칸 사용 (Z X C V B). 모든 판정은 여기서 */
    public static void cast(ServerPlayer p, int slot) {
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        if (c == null || slot < 0 || slot >= RpgPlayerData.SLOT_COUNT) return;
        String nodeId = d.slots().get(slot);
        if (nodeId.isEmpty()) {
            tell(p, "message.fantasyrpg.skill.empty_slot", ChatFormatting.GRAY, slot + 1);
            return;
        }
        SkillNode n = SkillData.nodes(c.id()).get(nodeId);
        int rank = d.nodeRanks().getOrDefault(nodeId, 0);
        if (n == null || rank <= 0) return;
        SkillEffects.Effect effect = SkillEffects.get(n.effect());
        if (effect == null || !n.kind().equippable()) {
            tell(p, "message.fantasyrpg.skill.no_effect", ChatFormatting.YELLOW, n.name());
            return;
        }
        if (!holdsClassWeapon(p, c)) {
            tell(p, "message.fantasyrpg.skill.need_weapon", ChatFormatting.RED);
            return;
        }
        int left = SkillRuntime.cooldownLeft(p, nodeId);
        if (left > 0) {
            tell(p, "message.fantasyrpg.skill.cooldown", ChatFormatting.GRAY, n.name(), String.format("%.1f", left / 20.0));
            return;
        }
        double cost = n.cost(rank);
        if (cost > 0 && CoreService.value(p) < cost) {
            tell(p, "message.fantasyrpg.skill.no_core", ChatFormatting.RED, n.name(), (int) Math.ceil(cost));
            return;
        }
        run(p, n, rank, effect, cost);
    }

    /** 비용·무기 확인 없이 (마력 방벽 5랭크의 자동 결계). 쿨타임 중이면 안 나감, 쿨타임은 공유 */
    public static void castFree(ServerPlayer p, String effectId) {
        SkillPassives.Learned l = SkillPassives.learned(p, effectId);
        if (l == null || SkillRuntime.cooldownLeft(p, l.node().id()) > 0) return;
        SkillEffects.Effect effect = SkillEffects.get(effectId);
        if (effect != null) run(p, l.node(), l.rank(), effect, 0);
    }

    private static void run(ServerPlayer p, SkillNode n, int rank, SkillEffects.Effect effect, double cost) {
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        // 연쇄(주문 연쇄·연계 숙달): 다른 스킬을 창 안에 이어 쓰면 연쇄
        SkillPassives.Learned chain = SkillPassives.anyOf(p, SkillIds.CHAIN);
        boolean chained = chain != null && !st.lastCastNode.isEmpty() && !st.lastCastNode.equals(n.id())
                && now - st.lastCastTick <= chain.v("window_ticks");
        double bonus = chained ? chain.v("chain_damage") : 0;
        if (st.chainBonusReady) {
            bonus += chain == null ? 0 : chain.v("chain_bonus");
            st.chainBonusReady = false;
        }
        // 쿨타임 비율 감소 (주문 숙련, 광폭화 5랭크: 코어 문턱은 쓰기 전 값으로)
        double rate = Math.min(0.5, SkillPassives.sum(p, "cooldown_rate"));
        Cast cast = new Cast(p, n, rank, bonus);
        if (!effect.run(cast)) {
            return; // 못 씀: 비용·쿨타임 없음 (연쇄 보너스는 이미 쓴 것으로 봄 — 단순화)
        }
        if (cost > 0 && !cast.free) CoreService.spend(p, cost);
        int cd = Math.max(0, cast.cooldownTicks() - SkillPassives.cooldownCut(p, n));
        cd = (int) Math.round(cd * (1.0 - rate) * cast.cooldownMult);
        if (chained) {
            cd = SkillMath.chainedCooldown(cd, chain.v("cooldown_cut"));
            st.chainCount++;
            CoreService.gain(p, chain.v("chain_core"));
            if (chain.v("chain_count") > 0 && st.chainCount >= (int) chain.v("chain_count") && chain.v("chain_bonus") > 0) {
                st.chainBonusReady = true;
                st.chainCount = 0;
            }
        } else {
            st.chainCount = 0;
        }
        st.lastCastNode = n.id();
        st.lastCastTick = now;
        SkillRuntime.setCooldown(p, n.id(), cd);
    }

    /** 손에 든 것이 내 직업 무기(근접이나 원거리)인지 — 스킬은 직업 무기로만 (정책 P1) */
    public static boolean holdsClassWeapon(ServerPlayer p, RpgClass c) {
        ItemStack held = p.getMainHandItem();
        for (WeaponPermission.AttackKind k : WeaponPermission.AttackKind.values()) {
            if (RpgAttacks.check(c, k, held) == WeaponPermission.Verdict.ALLOW) return true;
        }
        return false;
    }

    // ── 클라이언트 동기화 ──

    /** 성장·스킬 상태 보내기 (RpgProgress.set 이 매번 부름) */
    public static void syncState(ServerPlayer p) {
        RpgPlayerData d = RpgProgress.get(p);
        CompoundTag t = new CompoundTag();
        t.putString("class", d.rpgClass());
        t.putInt("level", d.level());
        t.putInt("xp", d.xp());
        t.putInt("need", ProgressionManager.INSTANCE.settings().need(d.level()));
        t.putInt("cap", RpgProgress.levelCap(p));
        t.putInt("sp_total", d.spTotal());
        t.putInt("sp_spent", d.spSpent());
        int grade = MagicCityApi.grade(p);
        t.putInt("passive_points", PassiveNode.points(grade));
        t.putInt("passive_spent", SkillTreeRules.spent(d.passiveRanks()));
        t.putInt("free_resets", freeResets(p, d));
        CompoundTag ranks = new CompoundTag();
        d.nodeRanks().forEach(ranks::putInt);
        t.put("ranks", ranks);
        CompoundTag pr = new CompoundTag();
        d.passiveRanks().forEach(pr::putInt);
        t.put("passive_ranks", pr);
        ListTag slots = new ListTag();
        for (String s : d.slots()) slots.add(StringTag.valueOf(s));
        t.put("slots", slots);
        PacketDistributor.sendToPlayer(p, new RpgNetwork.State(t));
    }

    /** 노드 정의 보내기 (접속·직업 변경·/reload 때) */
    public static void syncDefs(ServerPlayer p) {
        RpgClass c = RpgProgress.get(p).classOrNull();
        String cls = c == null ? "" : c.id();
        PacketDistributor.sendToPlayer(p, new RpgNetwork.Defs(cls, SkillData.rawNodes(cls), SkillData.rawPassives()));
    }

    /** 접속·부활·/reload 때 전부 다시 */
    public static void refreshAll(ServerPlayer p) {
        syncDefs(p);
        syncState(p);
        applyPassives(p);
        CoreService.reset(p);
        com.fantasyrpg.core.skills.SkillEvents.refresh(p);
    }
}
