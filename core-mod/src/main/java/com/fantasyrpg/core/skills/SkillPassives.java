package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.combat.DamageInput;
import com.fantasyrpg.core.combat.RpgDamage;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgPlayerData;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.fantasyrpg.core.skilltree.SkillData;
import com.fantasyrpg.core.skilltree.SkillNode;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 배운 노드 찾기와 패시브 효과의 공통 처리 (기본 공격·화살·받는 피해·흡수 체력·시간 제한 속성).
 * 노드는 데이터의 {@code effect} id로 찾는다(노드 id를 데이터팩에서 바꿔도 동작).
 * 같은 역할 패시브는 직업마다 하나씩이라 {@link SkillIds}의 묶음으로 한 번에 처리한다.
 */
public final class SkillPassives {
    private SkillPassives() {
    }

    /** 배운 노드와 랭크 (랭크 0이면 null). v()는 다른 패시브의 modifies 까지 더한 값 */
    public record Learned(SkillNode node, int rank, ServerPlayer player) {
        public double v(String key) {
            return node.value(key, rank) + modSum(player, node.effect(), key);
        }
    }

    /** 효과가 있는 랭크: 마스터리는 활성 1개만 (기획 2.4) */
    public static int rankOf(RpgPlayerData d, SkillNode n) {
        if (n.kind() == SkillNode.Kind.MASTERY && !n.id().equals(d.activeMastery())) return 0;
        return d.nodeRanks().getOrDefault(n.id(), 0);
    }

    /** 배운 노드 전부 */
    public static List<Learned> allLearned(ServerPlayer p) {
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        if (c == null || d.nodeRanks().isEmpty()) return List.of();
        List<Learned> out = new java.util.ArrayList<>();
        for (SkillNode n : SkillData.nodes(c.id()).values()) {
            int r = rankOf(d, n);
            if (r > 0) out.add(new Learned(n, r, p));
        }
        return out;
    }

    public static Learned learned(ServerPlayer p, String effectId) {
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        if (c == null) return null;
        for (SkillNode n : SkillData.nodes(c.id()).values()) {
            if (!effectId.equals(n.effect())) continue;
            int r = rankOf(d, n);
            if (r > 0) return new Learned(n, r, p);
        }
        return null;
    }

    /** 배운 패시브들이 effect 의 key 수치에 더하는 양 ("modifies") */
    public static double modSum(ServerPlayer p, String effect, String key) {
        if (p == null || effect == null || effect.isEmpty()) return 0;
        RpgPlayerData d = RpgProgress.get(p);
        RpgClass c = d.classOrNull();
        if (c == null) return 0;
        double total = 0;
        for (SkillNode n : SkillData.nodes(c.id()).values()) {
            if (n.mods().isEmpty()) continue;
            int r = rankOf(d, n);
            if (r <= 0) continue;
            for (SkillNode.Mod m : n.mods()) if (m.effect().equals(effect) && m.key().equals(key)) total += m.at(r);
        }
        return total;
    }

    /**
     * 배운 모든 노드의 같은 키 합 (피해·쿨타임·회복 보너스 등). {@link SkillIds#CORE_GATED} 키는
     * 그 노드의 bonus_core_min 보다 코어가 낮으면 빠짐.
     */
    public static double sum(ServerPlayer p, String key) {
        double total = 0;
        boolean gated = SkillIds.CORE_GATED.contains(key);
        double core = gated ? CoreService.value(p) : 0;
        for (Learned l : allLearned(p)) {
            double v = l.node().value(key, l.rank());
            if (v == 0) continue;
            double min = l.node().value("bonus_core_min", l.rank());
            if (gated && min > 0 && core < min) continue;
            total += v;
        }
        return total;
    }

    /** 지속 버프 피해 보너스 (도적 그림자 분신) */
    public static double buffDamage(ServerPlayer p) {
        SkillRuntime.State st = SkillRuntime.state(p);
        return st.cloneEnd > SkillRuntime.now(p) ? st.cloneBonus : 0;
    }

    /** 패리 뒤 피해 보너스 (기사 반격 숙달 3랭크) */
    public static double afterParryBonus(ServerPlayer p) {
        double b = sum(p, "after_parry_bonus");
        return b > 0 && SkillRuntime.now(p) - SkillRuntime.state(p).lastParryTick <= 60 ? b : 0;
    }

    /** 회복 (전투사제 신앙 각성: 회복량 +, 회복 때 신성력) */
    public static void heal(ServerPlayer p, double amount) {
        if (amount <= 0) return;
        p.heal((float) (amount * (1.0 + sum(p, "heal_bonus"))));
        CoreService.gain(p, sum(p, "heal_core"));
    }

    public static Learned learned(ServerPlayer p, String effectId, int minRank) {
        Learned l = learned(p, effectId);
        return l != null && l.rank() >= minRank ? l : null;
    }

    /** 묶음 중 처음 배운 것 */
    public static Learned anyOf(ServerPlayer p, String[] effectIds) {
        for (String id : effectIds) {
            Learned l = learned(p, id);
            if (l != null) return l;
        }
        return null;
    }

    // ── 치명타 ──

    /** 기본 확률에 더할 치명타 확률 (기교 2단). 기교 트리 합계 상한 +15%p [기획 5.4] */
    public static double critBonus(ServerPlayer p) {
        Learned l = anyOf(p, SkillIds.FINESSE_SENSE);
        double heirloom = com.fantasyrpg.core.gear.heirloom.HeirloomService.bonus(p, "crit_chance");   // 유산 기교 특성 (트리 +15%p 상한과 따로)
        return (l == null ? 0 : Math.min(0.15, l.v("crit_chance"))) + heirloom
                + com.fantasyrpg.core.gear.armor.AffinityEffects.bonus(p, "crit_chance");                // 기교 성향 (합계 상한 50% 안)
    }

    /** 치명타 배율에 더할 값 (도적 급소 감각 5랭크: 표식이 다 찼을 때) */
    public static double critMultBonus(ServerPlayer p) {
        Learned l = learned(p, SkillIds.R_VITAL_SENSE, 5);
        double full = l != null && CoreService.value(p) >= l.v("crit_mult_core") ? l.v("crit_mult_bonus") : 0;
        return full + sum(p, "crit_mult_add");
    }

    /** 치명타가 났을 때 (기교 2단: 코어) */
    public static void onCrit(ServerPlayer p) {
        Learned l = anyOf(p, SkillIds.FINESSE_SENSE);
        if (l != null) CoreService.gain(p, l.v("crit_core"));
    }

    /** 스킬 적중 뒤 기본 공격 보너스 (기교 2단 3랭크) */
    private static double afterSkill(ServerPlayer p, SkillRuntime.State st) {
        Learned l = anyOf(p, SkillIds.FINESSE_SENSE);
        if (l == null || l.v("after_skill_ticks") <= 0) return 1.0;
        return SkillRuntime.now(p) - st.lastSkillHitTick <= l.v("after_skill_ticks") ? 1.0 + l.v("after_skill_bonus") : 1.0;
    }

    /** 공격자가 대상 등 뒤에 있는지 */
    public static boolean behind(Entity attacker, LivingEntity target) {
        Vec3 look = Vec3.directionFromRotation(0, target.getYRot());
        return SkillMath.isBehind(look.x, look.z, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
    }

    public static boolean undead(LivingEntity e) {
        return e.getType().is(EntityTypeTags.UNDEAD);
    }

    // ── 근접 기본 공격 (전사·기사·도적·전투사제) ──

    public record MeleeMods(double physical, double magic, double factor, boolean forceCrit, boolean behind) {
    }

    public static MeleeMods melee(ServerPlayer p, LivingEntity target, double physical, double magic) {
        double factor = 1.0;
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        boolean back = behind(p, target);
        Learned mastery = anyOf(p, SkillIds.MELEE_MASTERY);
        if (mastery != null) {
            physical *= 1.0 + mastery.v("basic_physical_bonus");
            if (back) factor *= 1.0 + mastery.v("behind_bonus");
            boolean same = target.getUUID().equals(st.lastBasicTarget);
            st.basicConsecutive = SkillMath.consecutive(same, st.basicConsecutive);
            st.lastBasicTarget = target.getUUID();
            if (mastery.rank() >= 5 && st.basicConsecutive >= 3) {
                physical += mastery.v("third_hit_physical");
                SkillTargets.slow(target, (int) mastery.v("third_hit_stun"), 3);
                st.basicConsecutive = 0;
            }
            if (now - st.lastChargeTick <= mastery.v("after_charge_ticks")) factor *= 1.0 + mastery.v("after_charge_bonus");
        }
        Learned spell = anyOf(p, SkillIds.MELEE_MAGIC);
        if (spell != null) {
            double add = spell.v("basic_magic");
            if (spell.v("high_core") > 0 && CoreService.value(p) >= spell.v("high_core")) add *= spell.v("high_core_mult");
            magic += add;
            if (undead(target)) factor *= 1.0 + spell.v("undead_bonus");
        }
        factor *= afterSkill(p, st);
        factor *= 1.0 + sum(p, "all_damage_bonus") + afterParryBonus(p) + buffDamage(p);
        if (st.releaseEnd > now) magic += st.releaseMagic; // 마검 해방
        boolean force = st.forceCritBasic;
        st.forceCritBasic = false;
        return new MeleeMods(physical, magic, factor, force, back);
    }

    /** 근접 기본 공격이 맞은 뒤 */
    public static void afterMelee(ServerPlayer p, LivingEntity target, DamageInput in, boolean behind) {
        Learned mastery = anyOf(p, SkillIds.MELEE_MASTERY);
        if (mastery != null) {
            CoreService.gain(p, mastery.v("hit_core_bonus"));
            if (behind) CoreService.gain(p, mastery.v("behind_core"));
            // 기사 창술 숙련 3랭크: 대상 바로 뒤 적도 일부 피해
            double pierce = mastery.v("pierce_ratio");
            if (pierce > 0) {
                Vec3 dir = target.position().subtract(p.position()).multiply(1, 0, 1).normalize();
                Vec3 at = target.position().add(dir.scale(1.2)).add(0, target.getBbHeight() / 2, 0);
                for (LivingEntity e : SkillTargets.around(p, at, 0.9)) {
                    if (e == target) continue;
                    RpgDamage.apply(e, p, in.withFinalModifier(in.finalModifier() * pierce));
                    break;
                }
            }
        }
        afterBasicHit(p);
        if (in.critMultiplier() > 1.0) onCrit(p);
    }

    /** 근접·화살 기본 공격 적중 공통: 4단 패시브 코어(광폭화·집중 사격), 표식 숙달 확률 */
    private static void afterBasicHit(ServerPlayer p) {
        CoreService.gain(p, sum(p, "basic_hit_core") + sum(p, "basic_hit_core_high"));
        double chance = sum(p, "extra_mark_chance");
        if (chance > 0 && p.getRandom().nextDouble() < chance) CoreService.gain(p, 1);
    }

    /** 화살이 맞은 뒤 */
    public static void afterArrow(ServerPlayer p, DamageInput in) {
        afterBasicHit(p);
        if (in.critMultiplier() > 1.0) onCrit(p);
    }

    // ── 궁수 화살 ──

    public record ArrowMods(double physical, double magic, double factor) {
    }

    public static ArrowMods arrow(ServerPlayer p, LivingEntity target, double physical, double magic, Vec3 shotFrom) {
        double factor = 1.0;
        SkillRuntime.State st = SkillRuntime.state(p);
        Learned precision = learned(p, SkillIds.A_PRECISION);
        if (precision != null) {
            factor *= 1.0 + precision.v("arrow_bonus");
            if (precision.v("far_distance") > 0 && target.position().distanceTo(shotFrom) >= precision.v("far_distance")) {
                factor *= 1.0 + precision.v("far_bonus");
            }
            boolean same = target.getUUID().equals(st.lastArrowTarget);
            st.arrowConsecutive = SkillMath.consecutive(same, st.arrowConsecutive);
            st.lastArrowTarget = target.getUUID();
            if (precision.rank() >= 5 && st.arrowConsecutive >= 3) {
                physical += precision.v("third_hit_physical");
                st.arrowConsecutive = 0;
            }
        }
        Learned fletch = learned(p, SkillIds.A_ELEMENTAL_FLETCH);
        if (fletch != null) {
            double add = fletch.v("arrow_magic");
            if (fletch.v("high_core") > 0 && CoreService.value(p) >= fletch.v("high_core")) add *= fletch.v("high_core_mult");
            magic += add;
        }
        factor *= afterSkill(p, st);
        factor *= 1.0 + sum(p, "all_damage_bonus") + afterParryBonus(p) + buffDamage(p);
        return new ArrowMods(physical, magic, factor);
    }

    // ── 마법사 기본 탄 ──

    public static double boltPhysicalBonus(ServerPlayer p) {
        Learned l = learned(p, SkillIds.M_BOLT_TRAINING);
        return l == null ? 0 : l.v("bolt_physical");
    }

    public static int boltCooldown(ServerPlayer p, int base) {
        Learned l = learned(p, SkillIds.M_BOLT_TRAINING);
        int cd = l == null ? base : (int) Math.round(l.v("bolt_cooldown_ticks"));
        SkillRuntime.State st = SkillRuntime.state(p);
        if (st.hasteEnd > SkillRuntime.now(p)) cd = (int) Math.max(1, Math.round(cd * (1.0 - st.hasteBoltCut)));
        return cd;
    }

    /** 기본 탄을 쏜 뒤 (마탄 단련 5랭크: N발마다 실체 마탄 1발) */
    public static void afterBolt(ServerPlayer p) {
        SkillRuntime.State hs = SkillRuntime.state(p);
        if (hs.hasteEnd > SkillRuntime.now(p)) CoreService.gain(p, hs.hasteBoltCore); // 가속 주문 3랭크
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

    // ── 쿨타임 감소 (주문 2단 5랭크) ──

    public static int cooldownCut(ServerPlayer p, SkillNode cast) {
        for (String[] pair : SkillIds.COOLDOWN_CUT) {
            if (!pair[1].equals(cast.effect())) continue;
            Learned l = learned(p, pair[0], 5);
            if (l != null) return (int) l.v("cooldown_cut");
        }
        return 0;
    }

    // ── 코어 최대치 ──

    public static double coreMaxBonus(ServerPlayer p) {
        return sum(p, "max_core_bonus");
    }

    // ── 흡수 체력 (최대 흡수 체력 속성에 임시 수정자를 붙이고 채움, 시간이 지나면 거둠) ──

    /** 스킬 가속 합계: 스킬 트리 패시브(skill_haste) + 능력치 fantasyrpg:skill_haste (직업 룬석) */
    public static double haste(ServerPlayer p) {
        double a = 0;
        var inst = p.getAttribute(com.fantasyrpg.core.gear.RpgAttributes.SKILL_HASTE);
        if (inst != null) a = inst.getValue();
        return sum(p, "skill_haste") + a;
    }

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

    public static void removeAbsorption(ServerPlayer p, String key) {
        AttributeInstance max = p.getAttribute(Attributes.MAX_ABSORPTION);
        if (max == null) return;
        max.removeModifier(absorptionId(key));
        if (p.getAbsorptionAmount() > p.getMaxAbsorption()) p.setAbsorptionAmount((float) p.getMaxAbsorption());
    }

    // ── 시간 제한 속성 (이동 속도 등) ──

    private static ResourceLocation attrId(String key) {
        return ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "skill/timed/" + key);
    }

    private static final java.util.Map<String, Holder<Attribute>> TIMED_ATTR = new java.util.concurrent.ConcurrentHashMap<>();

    /** key 하나에 수정자 하나. 다시 걸면 값·시간을 새로 */
    public static void timedAttribute(ServerPlayer p, String key, Holder<Attribute> attr, double amount,
                                      AttributeModifier.Operation op, int ticks) {
        if (amount == 0 || ticks <= 0) return;
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(attrId(key));
        inst.addTransientModifier(new AttributeModifier(attrId(key), amount, op));
        TIMED_ATTR.put(key, attr);
        SkillRuntime.state(p).attrEnd.put(key, SkillRuntime.now(p) + ticks);
    }

    private static void removeTimed(ServerPlayer p, String key) {
        Holder<Attribute> attr = TIMED_ATTR.get(key);
        AttributeInstance inst = attr == null ? null : p.getAttribute(attr);
        if (inst != null) inst.removeModifier(attrId(key));
    }

    /** 매 틱: 끝난 흡수 체력·속성 거두기 */
    public static void tickTimed(ServerPlayer p) {
        SkillRuntime.State st = SkillRuntime.state(p);
        long now = SkillRuntime.now(p);
        if (!st.absorptionEnd.isEmpty()) {
            st.absorptionEnd.entrySet().removeIf(e -> {
                if (e.getValue() > now) return false;
                removeAbsorption(p, e.getKey());
                return true;
            });
        }
        if (!st.attrEnd.isEmpty()) {
            st.attrEnd.entrySet().removeIf(e -> {
                if (e.getValue() > now) return false;
                removeTimed(p, e.getKey());
                return true;
            });
        }
    }

    /** 접속·부활 때 남은 수정자 정리 */
    public static void clearTimed(ServerPlayer p) {
        SkillRuntime.State st = SkillRuntime.state(p);
        for (String k : List.copyOf(st.absorptionEnd.keySet())) removeAbsorption(p, k);
        for (String k : List.copyOf(st.attrEnd.keySet())) removeTimed(p, k);
        st.absorptionEnd.clear();
        st.attrEnd.clear();
    }
}
