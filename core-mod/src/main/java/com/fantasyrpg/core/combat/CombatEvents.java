package com.fantasyrpg.core.combat;

import com.fantasyrpg.core.compat.bettercombat.BetterCombatCompat;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.fantasyrpg.core.gear.weapon.ArcherBowItem;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.skills.SkillPassives;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 직업 플레이어의 기본 공격을 RPG 피해로 바꾼다 (claude/전투연결_설계.md 2절, 혼합개발 연동설계 C-2·C-4·C-7).
 * <ul>
 *   <li>근접: 바닐라 공격 취소 → W × 계수 × 콤보 × 충전 → RPG 피해 한 번</li>
 *   <li>활: 궁수 활 화살에 W·계수·당김을 기록, 생물 명중 시 RPG 피해로 대체. 기록 없는 직업 플레이어 화살은 피해 없이 사라짐</li>
 *   <li>다른 활·쇠뇌·삼지창 사용 시작 막기(무기 권한)</li>
 *   <li>PvP: 플레이어 → 플레이어 피해 모두 막음</li>
 * </ul>
 * 직업이 없는 플레이어는 바닐라 그대로.
 */
public final class CombatEvents {
    private CombatEvents() {
    }

    private static final String SHOT = "fantasyrpg_shot";
    private static final double MAX_BOW_SPEED = 3.0; // 바닐라 활 최대 속도

    private static boolean betterCombat() {
        return ModList.get().isLoaded("bettercombat");
    }

    // ── 근접 ──
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Entity hit = event.getTarget();
        LivingEntity target = RpgDamage.body(hit);                                          // 부위(PartEntity)면 그 몸체 (라디키온 마디)
        if (target == null) return;
        RpgClass c = RpgProgress.get(p).classOrNull();
        CombatSettings s = CombatManager.INSTANCE.settings();
        if (c == null || s == null) return; // 무직·데이터 없음 → 바닐라
        event.setCanceled(true);
        float charge = p.getAttackStrengthScale(0.5F); // 초기화 전에 읽음
        try {
            if (target instanceof Player) return; // PvP 금지
            ItemStack weapon = p.getMainHandItem();
            WeaponPermission.Verdict v = RpgAttacks.check(p, c, WeaponPermission.AttackKind.MELEE, weapon);
            if (v.denied()) {
                RpgAttacks.notice(p, v);
                return;
            }
            DamageInput in;
            SkillPassives.MeleeMods mods = null;
            if (v == WeaponPermission.Verdict.UNARMED) {
                in = RpgAttacks.unarmed(s, charge);
            } else {
                CombatSettings.ClassAttack a = s.forClass(c.id());
                if (a == null) return;
                double combo = betterCombat() ? BetterCombatCompat.comboMultiplier(p) : 1.0;
                double factor = BasicAttackMath.basicCoefficient(1.0, combo, charge);
                // 직업 패시브(공격·주문·기교 2단)
                mods = SkillPassives.melee(p, target, a.physical(), a.magic());
                in = RpgAttacks.basic(s, s.power(GearRegistry.tierOf(weapon)), mods.physical(), mods.magic(),
                        factor * mods.factor(), p, SkillPassives.critBonus(p), mods.forceCrit(), SkillPassives.critMultBonus(p));
            }
            RpgDamage.apply(target, hit, p, in);
            p.setLastHurtMob(target);
            if (v != WeaponPermission.Verdict.UNARMED) {
                CoreService.onEvent(p, CoreService.BASIC_ATTACK_HIT);
                SkillPassives.afterMelee(p, target, in, mods != null && mods.behind());
            }
        } finally {
            p.resetAttackStrengthTicker(); // 취소로 빠진 바닐라 쿨타임 초기화를 대신함
        }
    }

    // ── 활: 쏠 때 기록 ──
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer p)) return;
        if (arrow.getPersistentData().contains(SHOT)) return;
        RpgClass c = RpgProgress.get(p).classOrNull();
        CombatSettings s = CombatManager.INSTANCE.settings();
        if (c == null || s == null) return;
        ItemStack bow = bowInUse(p);
        if (bow.isEmpty() || RpgAttacks.check(p, c, WeaponPermission.AttackKind.RANGED, bow).denied()) return;
        CombatSettings.ClassAttack a = s.forClass(c.id());
        if (a == null) return;
        double power = Math.min(1.0, arrow.getDeltaMovement().length() / MAX_BOW_SPEED);
        CompoundTag tag = new CompoundTag();
        tag.putInt("tier", GearRegistry.tierOf(bow));
        tag.putDouble("physical", a.physical());
        tag.putDouble("magic", a.magic());
        tag.putDouble("power", power);
        arrow.getPersistentData().put(SHOT, tag);
    }

    private static ItemStack bowInUse(ServerPlayer p) {
        ItemStack use = p.getUseItem();
        if (use.getItem() instanceof ArcherBowItem) return use;
        if (p.getMainHandItem().getItem() instanceof ArcherBowItem) return p.getMainHandItem();
        if (p.getOffhandItem().getItem() instanceof ArcherBowItem) return p.getOffhandItem();
        return ItemStack.EMPTY;
    }

    // ── 활: 맞을 때 RPG 피해로 대체 ──
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (proj.level().isClientSide()) return;
        if (!(proj instanceof AbstractArrow)) return; // 눈덩이·엔더진주 등은 그대로
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        LivingEntity target = RpgDamage.body(hit.getEntity());                             // 부위면 그 몸체
        if (target == null) return;
        if (!(proj.getOwner() instanceof ServerPlayer p)) return;
        if (RpgProgress.get(p).classOrNull() == null) return; // 무직 → 바닐라
        CombatSettings s = CombatManager.INSTANCE.settings();
        if (s == null) return;
        event.setCanceled(true);
        CompoundTag shot = proj.getPersistentData().getCompound(SHOT);
        boolean tagged = proj.getPersistentData().contains(SHOT);
        if (!(proj instanceof ThrownTrident)) proj.discard(); // 삼지창은 잃어버리지 않게 지우지 않음
        if (!tagged || target instanceof Player) return; // 직업 무기가 아닌 투사체·PvP → 피해 없음
        // 궁수 패시브(정밀 사격·원소 깃·사수의 감)
        SkillPassives.ArrowMods mods = SkillPassives.arrow(p, target, shot.getDouble("physical"), shot.getDouble("magic"), p.position());
        DamageInput in = RpgAttacks.basic(s, s.power(shot.getInt("tier")), mods.physical(), mods.magic(),
                shot.getDouble("power") * mods.factor(), p, SkillPassives.critBonus(p), false, SkillPassives.critMultBonus(p));
        RpgDamage.apply(target, hit.getEntity(), p, in);
        p.setLastHurtMob(target);
        SkillPassives.afterArrow(p, in);
        CoreService.onEvent(p, CoreService.BASIC_ATTACK_HIT);
    }

    // ── 다른 원거리 무기 사용 막기 ──
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack stack = event.getItem();
        boolean ranged = stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem;
        if (!ranged) return;
        RpgClass c = RpgProgress.get(p).classOrNull();
        if (c == null) return;
        WeaponPermission.Verdict v = RpgAttacks.check(p, c, WeaponPermission.AttackKind.RANGED, stack);
        if (v.denied()) {
            event.setCanceled(true);
            RpgAttacks.notice(p, v);
        }
    }

    // ── PvP 차단 ──
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Entity src = event.getSource().getEntity();
        if (src instanceof Player attacker && attacker != victim) event.setCanceled(true);
    }
}
