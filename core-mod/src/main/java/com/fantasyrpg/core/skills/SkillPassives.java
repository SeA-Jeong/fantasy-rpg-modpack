package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgPlayerData;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.fantasyrpg.core.skilltree.SkillData;
import com.fantasyrpg.core.skilltree.SkillNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;


/**
 * 배운 노드 찾기와 패시브 효과의 공통 처리 (기본 공격·받는 피해·흡수 체력).
 * 노드는 데이터의 {@code effect} id로 찾는다(노드 id를 데이터팩에서 바꿔도 동작).
 */
public final class SkillPassives {
    private SkillPassives() {
    }

    /** 배운 노드와 랭크 (랭크 0이면 null) */
    public record Learned(SkillNode node, int rank) {
        public double v(String key) {
            return node.value(key, rank);
        }
    }

    public static Learned learned(ServerPlayer p, String effectId) {
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        if (c == null) return null;
        for (SkillNode n : SkillData.nodes(c.id()).values()) {
            if (!effectId.equals(n.effect())) continue;
            int r = d.nodeRanks().getOrDefault(n.id(), 0);
            if (r > 0) return new Learned(n, r);
        }
        return null;
    }

    public static Learned learned(ServerPlayer p, String effectId, int minRank) {
        Learned l = learned(p, effectId);
        return l != null && l.rank() >= minRank ? l : null;
    }

    // ── 치명타 ──

    /** 기본 확률에 더할 치명타 확률 (연계 감각). 기교 트리 합계 상한 +15%p [기획 5.4] */
    public static double critBonus(ServerPlayer p) {
        Learned l = learned(p, SkillIds.W_COMBO_SENSE);
        return l == null ? 0 : Math.min(0.15, l.v("crit_chance"));
    }

    /** 치명타가 났을 때 (연계 감각 5랭크) */
    public static void onCrit(ServerPlayer p) {
        Learned l = learned(p, SkillIds.W_COMBO_SENSE, 5);
        if (l != null) CoreService.gain(p, l.v("crit_core"));
    }

    // ── 전사 기본 공격 ──

    public record MeleeMods(double physical, double magic, double factor) {
    }

    public static MeleeMods melee(ServerPlayer p, LivingEntity target, double physical, double magic) {
        double factor = 1.0;
        SkillRuntime.State st = SkillRuntime.state(p);
        Learned mastery = learned(p, SkillIds.W_SWORD_MASTERY);
        if (mastery != null) {
            physical *= 1.0 + mastery.v("basic_physical_bonus");
            boolean same = target.getUUID().equals(st.lastBasicTarget);
            st.basicConsecutive = SkillMath.consecutive(same, st.basicConsecutive);
            st.lastBasicTarget = target.getUUID();
            if (mastery.rank() >= 5 && st.basicConsecutive >= 3) {
                physical += mastery.v("third_hit_physical");
                st.basicConsecutive = 0;
            }
        }
        Learned blade = learned(p, SkillIds.W_SPELLBLADE);
        if (blade != null) {
            double add = blade.v("basic_magic");
            if (blade.rank() >= 3 && CoreService.value(p) >= blade.v("high_core")) add *= blade.v("high_core_mult");
            magic += add;
        }
        Learned combo = learned(p, SkillIds.W_COMBO_SENSE, 3);
        if (combo != null && SkillRuntime.now(p) - st.lastSkillHitTick <= combo.v("after_skill_ticks")) {
            factor *= 1.0 + combo.v("after_skill_bonus");
        }
        return new MeleeMods(physical, magic, factor);
    }

    /** 근접 기본 공격이 맞은 뒤 */
    public static void afterMelee(ServerPlayer p, boolean crit) {
        Learned mastery = learned(p, SkillIds.W_SWORD_MASTERY, 3);
        if (mastery != null) CoreService.gain(p, mastery.v("hit_core_bonus"));
        if (crit) onCrit(p);
    }

    // ── 마법사 기본 탄 ──

    public static double boltPhysicalBonus(ServerPlayer p) {
        Learned l = learned(p, SkillIds.M_BOLT_TRAINING);
        return l == null ? 0 : l.v("bolt_physical");
    }

    public static int boltCooldown(ServerPlayer p, int base) {
        Learned l = learned(p, SkillIds.M_BOLT_TRAINING);
        return l == null ? base : (int) Math.round(l.v("bolt_cooldown_ticks"));
    }

    /** 기본 탄을 쏜 뒤 (마탄 단련 5랭크: N발마다 실체 마탄 1발) */
    public static void afterBolt(ServerPlayer p) {
        Learned l = learned(p, SkillIds.M_BOLT_TRAINING, 5);
        if (l == null) return;
        SkillRuntime.State st = SkillRuntime.state(p);
        st.boltCount++;
        int every = (int) Math.round(l.v("extra_every"));
        if (every > 0 && st.boltCount >= every) {
            st.boltCount = 0;
            Learned solid = learned(p, SkillIds.M_SOLID_BOLTS);
            if (solid != null) MageSkills.solidBolt(new Cast(p, solid.node(), solid.rank(), 0), false);
        }
    }

    // ── 코어 최대치 ──

    public static double coreMaxBonus(ServerPlayer p) {
        Learned l = learned(p, SkillIds.M_ARCANE_INSIGHT, 3);
        return l == null ? 0 : l.v("max_core_bonus");
    }

    // ── 흡수 체력 (최대 흡수 체력 속성에 임시 수정자를 붙이고 채움, 시간이 지나면 거둠) ──

    private static ResourceLocation absorptionId(String key) {
        return ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "skill/absorption/" + key);
    }

    public static void grantAbsorption(ServerPlayer p, String key, double amount, int ticks) {
        if (amount <= 0) return;
        AttributeInstance max = p.getAttribute(Attributes.MAX_ABSORPTION);
        if (max == null) return;
        ResourceLocation id = absorptionId(key);
        boolean had = max.getModifier(id) != null;
        max.removeModifier(id);
        max.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        double cur = p.getAbsorptionAmount();
        p.setAbsorptionAmount((float) Math.min(p.getMaxAbsorption(), had ? Math.max(cur, amount) : cur + amount));
        SkillRuntime.state(p).absorptionEnd.put(key, SkillRuntime.now(p) + ticks);
    }

    /** 매 틱: 끝난 흡수 체력 거두기 */
    public static void tickAbsorption(ServerPlayer p) {
        SkillRuntime.State st = SkillRuntime.state(p);
        if (st.absorptionEnd.isEmpty()) return;
        long now = SkillRuntime.now(p);
        st.absorptionEnd.entrySet().removeIf(e -> {
            if (e.getValue() > now) return false;
            removeAbsorption(p, e.getKey());
            return true;
        });
    }

    public static void removeAbsorption(ServerPlayer p, String key) {
        AttributeInstance max = p.getAttribute(Attributes.MAX_ABSORPTION);
        if (max == null) return;
        max.removeModifier(absorptionId(key));
        if (p.getAbsorptionAmount() > p.getMaxAbsorption()) p.setAbsorptionAmount((float) p.getMaxAbsorption());
    }

    /** 접속·부활 때 남은 흡수 수정자 정리 (임시 수정자라 보통 없음) */
    public static void clearAbsorption(ServerPlayer p) {
        for (String k : java.util.List.copyOf(SkillRuntime.state(p).absorptionEnd.keySet())) removeAbsorption(p, k);
        SkillRuntime.state(p).absorptionEnd.clear();
    }
}
