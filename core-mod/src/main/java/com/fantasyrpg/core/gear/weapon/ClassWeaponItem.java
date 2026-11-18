package com.fantasyrpg.core.gear.weapon;

import com.fantasyrpg.core.combat.CombatManager;
import com.fantasyrpg.core.combat.CombatSettings;
import com.fantasyrpg.core.combat.DamageInput;
import com.fantasyrpg.core.combat.RpgAttacks;
import com.fantasyrpg.core.skills.SkillPassives;
import com.fantasyrpg.core.combat.RpgDamage;
import com.fantasyrpg.core.combat.WeaponPermission;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgProgress;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 근접 직업 무기(검·창·단검·철퇴)와 마법사 지팡이. 등급은 {@code fantasyrpg:gear_tier}.
 * 공격력·공격 속도는 {@link WeaponRegistry#onItemAttributes}가 데이터로 붙이고, 직업 플레이어의 실제 피해는 {@code combat/CombatEvents}가 계산한다.
 * 지팡이는 우클릭으로 마법 탄(즉시 명중)을 쏜다.
 */
public class ClassWeaponItem extends Item {
    private final WeaponType type;

    public ClassWeaponItem(WeaponType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public WeaponType type() {
        return type;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.fantasyrpg.tiered", super.getName(stack), GearRegistry.tierOf(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (type != WeaponType.STAFF) return InteractionResultHolder.pass(stack);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        RpgClass c = RpgProgress.get(p).classOrNull();
        CombatSettings s = CombatManager.INSTANCE.settings();
        if (c == null || s == null) return InteractionResultHolder.pass(stack);
        WeaponPermission.Verdict v = RpgAttacks.check(c, WeaponPermission.AttackKind.RANGED, stack);
        if (v.denied()) {
            RpgAttacks.notice(p, v);
            return InteractionResultHolder.fail(stack);
        }
        CombatSettings.ClassAttack attack = s.forClass(c.id());
        if (attack == null || attack.bolt() == null) return InteractionResultHolder.pass(stack);
        castBolt(p, (ServerLevel) level, stack, s, attack.bolt());
        // 마탄 단련: 대기 시간, N발마다 실체 마탄
        p.getCooldowns().addCooldown(this, SkillPassives.boltCooldown(p, attack.bolt().cooldownTicks()));
        SkillPassives.afterBolt(p);
        return InteractionResultHolder.consume(stack);
    }

    /** 마법 탄: 블록에 막히기 전까지 시선 방향으로 사거리 안 첫 생물에게 즉시 명중 */
    private static void castBolt(ServerPlayer p, ServerLevel level, ItemStack staff, CombatSettings s, CombatSettings.Bolt bolt) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(bolt.range()));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(bolt.range())).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, p, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e.isPickable() && e != p);
        Vec3 stop = hit != null ? hit.getLocation() : end;

        double len = stop.subtract(eye).length();
        for (double d = 1.0; d < len; d += 0.5) {
            Vec3 at = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6F, 1.6F);

        if (hit == null) return;
        Entity target = hit.getEntity();
        if (target instanceof Player || !(target instanceof LivingEntity living)) return; // PvP 금지
        DamageInput in = RpgAttacks.basic(s, bolt.physical() + SkillPassives.boltPhysicalBonus(p), bolt.magic(), staff, 1.0, p,
                SkillPassives.critBonus(p));
        RpgDamage.apply(living, p, in);
        p.setLastHurtMob(living);
        if (in.critMultiplier() > 1.0) SkillPassives.onCrit(p);
    }
}
