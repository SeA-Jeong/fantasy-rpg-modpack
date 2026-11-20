package com.fantasyrpg.core.gear.armor;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.combat.DamageInput;
import com.fantasyrpg.core.skills.SkillRuntime;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 계열 방어구 세트 효과 (2026-11-21, claude/세트효과_초안.md). 규칙·판정은 {@link SetRules}, 값은 데이터 armor_sets.
 * 상시 능력치는 속성 수정자 {@code fantasyrpg:set/<계열>/<2|4>}, 반격·자세·흡수 체력·질주 가속은 이벤트·틱.
 */
public final class SetEffects {
    private SetEffects() {}

    private static volatile Map<ArmorFamily, SetRules.SetDef> defs = Map.of();

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(LISTENER);
    }

    /** 정적 리스너: 전용 서버 클라이언트도 같은 읽기 코드로 받음 (2026-11-21) */
    private static final SimpleJsonResourceReloadListener LISTENER = new SimpleJsonResourceReloadListener(new Gson(), "armor_sets") {
            {
                com.fantasyrpg.core.network.DataSync.register("armor_sets", f -> apply(f, null, null));
            }

            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller pf) {
                com.fantasyrpg.core.network.DataSync.remember("armor_sets", files);
                Map<ArmorFamily, SetRules.SetDef> out = new EnumMap<>(ArmorFamily.class);
                int bad = 0;
                for (var e : files.entrySet()) {
                    ArmorFamily f = ArmorFamily.byId(e.getKey().getPath());
                    try {
                        if (f == null) throw new IllegalArgumentException("모르는 계열");
                        out.put(f, SetRules.parse(f.id(), e.getValue().getAsJsonObject()));
                    } catch (RuntimeException ex) {
                        bad++;
                        FantasyRpgCore.LOGGER.error("[방어구] 세트 효과 {} 읽기 실패: {}", e.getKey(), ex.getMessage());
                    }
                }
                defs = Map.copyOf(out);
                FantasyRpgCore.LOGGER.info("[방어구] 세트 효과 {}개 계열 로드, 오류 {}건", out.size(), bad);
            }
        };

    /** 클래스 로드만 (데이터 동기화 읽기 코드 등록) */
    public static void bootstrap() {
    }

    // ── 입은 상태 ──

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** 이 플레이어(또는 아무 생물)가 입은 계열별 세트 상태 */
    public static Map<ArmorFamily, SetRules.Worn> worn(LivingEntity e) {
        Map<ArmorFamily, List<Integer>> tiers = new EnumMap<>(ArmorFamily.class);
        for (EquipmentSlot s : ARMOR) {
            ItemStack st = e.getItemBySlot(s);
            if (st.getItem() instanceof FamilyArmorItem fa) tiers.computeIfAbsent(fa.family(), k -> new ArrayList<>()).add(GearRegistry.tierOf(st));
        }
        Map<ArmorFamily, SetRules.Worn> out = new EnumMap<>(ArmorFamily.class);
        for (var t : tiers.entrySet()) out.put(t.getKey(), SetRules.worn(t.getValue()));
        return out;
    }

    /** 플레이어별 상태 (서버) */
    private static final class State {
        Map<ArmorFamily, SetRules.Worn> worn = Map.of();
        String shown = "";
        long knightReflectAt, dragonReflectAt, dashAt, dashEnd;
        boolean wasSprinting, stance, nextBasicMagic;
        int still;
        double cdAcc;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static State st(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new State());
    }

    /** 2세트(four=false)·4세트 값 (꺼져 있으면 0) */
    public static double v(ServerPlayer p, ArmorFamily f, boolean four, String key) {
        SetRules.SetDef d = defs.get(f);
        SetRules.Worn w = st(p).worn.get(f);
        if (d == null || w == null) return 0;
        return four ? SetRules.value(d.four(), key, w.grade4()) : SetRules.value(d.two(), key, w.grade2());
    }

    private static boolean on(ServerPlayer p, ArmorFamily f, boolean four) {
        SetRules.Worn w = st(p).worn.get(f);
        return w != null && (four ? w.four() : w.two()) && defs.containsKey(f);
    }

    // ── 틱: 세트 갱신·속성·흡수 체력·자세·질주 ──

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        State s = st(p);
        long now = SkillRuntime.now(p);
        if (p.tickCount % 10 == 3) refresh(p, s);

        // 기사 4: 맞지 않고 일정 시간 → 흡수 체력
        if (on(p, ArmorFamily.KNIGHT, true)) {
            double amount = v(p, ArmorFamily.KNIGHT, true, "absorption");
            long delay = (long) v(p, ArmorFamily.KNIGHT, true, "absorb_delay_ticks");
            if (now - SkillRuntime.state(p).lastHurtTick >= delay && p.getAbsorptionAmount() < amount && p.tickCount % 20 == 0)
                p.setAbsorptionAmount((float) Math.min(p.getMaxAbsorption(), amount));
        }
        // 드워프 4: 웅크리고 가만히 → 자세
        if (on(p, ArmorFamily.DWARF, true)) {
            boolean still = p.isCrouching() && p.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
            s.still = still ? s.still + 1 : 0;
            boolean stance = s.still >= v(p, ArmorFamily.DWARF, true, "stance_ticks");
            if (stance != s.stance) {
                s.stance = stance;
                modifier(p, Attributes.KNOCKBACK_RESISTANCE, "dwarf/stance", stance ? 1.0 : 0, AttributeModifier.Operation.ADD_VALUE);
                if (stance) p.displayClientMessage(Component.literal("§6자세 — 넉백 무시, 다음 근접 공격 강화"), true);
            }
        } else if (s.stance) {
            s.stance = false;
            modifier(p, Attributes.KNOCKBACK_RESISTANCE, "dwarf/stance", 0, AttributeModifier.Operation.ADD_VALUE);
        }
        // 엘프 4: 질주 중 재사용 대기 가속, 질주 시작 가속
        if (on(p, ArmorFamily.ELF, true)) {
            if (p.isSprinting()) {
                s.cdAcc += v(p, ArmorFamily.ELF, true, "sprint_cooldown_extra");
                int whole = (int) s.cdAcc;
                if (whole > 0) {
                    s.cdAcc -= whole;
                    for (Map.Entry<String, Long> e : SkillRuntime.state(p).cooldownEnd.entrySet())
                        if (e.getValue() > now) e.setValue(Math.max(now, e.getValue() - whole));
                }
                if (p.tickCount % 20 == 0) SkillRuntime.resendCooldowns(p);
                if (!s.wasSprinting && now >= s.dashAt) {
                    s.dashAt = now + (long) v(p, ArmorFamily.ELF, true, "dash_cooldown");
                    s.dashEnd = now + (long) v(p, ArmorFamily.ELF, true, "dash_ticks");
                    modifier(p, Attributes.MOVEMENT_SPEED, "elf/dash", v(p, ArmorFamily.ELF, true, "dash_speed"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                }
            }
            s.wasSprinting = p.isSprinting();
        }
        if (s.dashEnd != 0 && now >= s.dashEnd) {
            s.dashEnd = 0;
            modifier(p, Attributes.MOVEMENT_SPEED, "elf/dash", 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
    }

    private static void refresh(ServerPlayer p, State s) {
        Map<ArmorFamily, SetRules.Worn> w = worn(p);
        if (w.equals(s.worn)) return;
        s.worn = w;
        // 상시 능력치
        modifier(p, Attributes.KNOCKBACK_RESISTANCE, "dwarf/2", v(p, ArmorFamily.DWARF, false, "knockback_resistance"), AttributeModifier.Operation.ADD_VALUE);
        modifier(p, Attributes.BURNING_TIME, "dragon/2", -Math.min(1.0, v(p, ArmorFamily.DRAGON, false, "burn_reduction")), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(p, Attributes.MOVEMENT_SPEED, "elf/2", v(p, ArmorFamily.ELF, false, "movement_speed"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(p, Attributes.MAX_ABSORPTION, "knight/4", v(p, ArmorFamily.KNIGHT, true, "absorption"), AttributeModifier.Operation.ADD_VALUE);
        if (p.getAbsorptionAmount() > p.getMaxAbsorption()) p.setAbsorptionAmount((float) p.getMaxAbsorption());
        // 켜진 세트 안내 (바뀔 때만)
        StringBuilder b = new StringBuilder();
        for (var e : w.entrySet()) {
            SetRules.Worn x = e.getValue();
            if (!x.two() || !defs.containsKey(e.getKey())) continue;
            if (b.length() > 0) b.append("  ");
            b.append(Component.translatable("set.fantasyrpg." + e.getKey().id()).getString()).append(' ').append(x.four() ? "4" : "2").append("세트");
        }
        String shown = b.toString();
        if (!shown.equals(s.shown)) {
            s.shown = shown;
            p.displayClientMessage(Component.literal(shown.isEmpty() ? "§7세트 효과 없음" : "§a세트 효과: " + shown), true);
        }
    }

    private static void modifier(ServerPlayer p, Holder<Attribute> a, String key, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "set/" + key);
        inst.removeModifier(id);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    // ── 피해 ──

    /** 플레이어가 받는 피해: 고룡 화염 감소, 기사 반격, 고룡 역린 / 플레이어가 주는 근접 피해: 드워프 자세 */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) hurtPlayer(event, p);
        if (event.getSource().getEntity() instanceof ServerPlayer att && event.getSource().getDirectEntity() == att) hitByPlayer(event, att);
    }

    private static void hurtPlayer(LivingIncomingDamageEvent event, ServerPlayer p) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (io.github.aswq0258.magiccity.api.MagicCityApi.isBossFixed(event.getSource())) return;      // 고정 피해는 세트도 통과
        State s = st(p);
        long now = SkillRuntime.now(p);
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && on(p, ArmorFamily.DRAGON, false))
            event.setAmount((float) (event.getAmount() * (1 - Math.min(0.5, v(p, ArmorFamily.DRAGON, false, "fire_reduction")))));
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker instanceof Player || attacker == p) return;
        float raw = event.getAmount();
        boolean melee = event.getSource().getDirectEntity() == attacker;
        if (melee && on(p, ArmorFamily.KNIGHT, false) && now >= s.knightReflectAt) {
            s.knightReflectAt = now + (long) v(p, ArmorFamily.KNIGHT, false, "reflect_cooldown");
            attacker.hurt(p.damageSources().thorns(p), (float) (raw * v(p, ArmorFamily.KNIGHT, false, "reflect_ratio")));
        }
        if (on(p, ArmorFamily.DRAGON, true) && now >= s.dragonReflectAt) {
            s.dragonReflectAt = now + (long) v(p, ArmorFamily.DRAGON, true, "reflect_cooldown");
            attacker.igniteForSeconds((float) v(p, ArmorFamily.DRAGON, true, "ignite_seconds"));
            attacker.hurt(p.damageSources().thorns(p), (float) (raw * v(p, ArmorFamily.DRAGON, true, "reflect_ratio")));
        }
    }

    private static void hitByPlayer(LivingIncomingDamageEvent event, ServerPlayer att) {
        State s = st(att);
        if (!s.stance || event.getEntity() == att || att.distanceTo(event.getEntity()) > 6) return;
        event.setAmount((float) (event.getAmount() * (1 + v(att, ArmorFamily.DWARF, true, "stance_bonus"))));
        s.stance = false;
        s.still = 0;
        modifier(att, Attributes.KNOCKBACK_RESISTANCE, "dwarf/stance", 0, AttributeModifier.Operation.ADD_VALUE);
    }

    // ── 드워프 2: 느려짐·채굴 피로 단축 ──

    private static final Set<String> SHORTENING = new HashSet<>();

    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !on(p, ArmorFamily.DWARF, false)) return;
        MobEffectInstance e = event.getEffectInstance();
        Holder<MobEffect> type = e.getEffect();
        if (!type.is(MobEffects.MOVEMENT_SLOWDOWN.unwrapKey().orElseThrow()) && !type.is(MobEffects.DIG_SLOWDOWN.unwrapKey().orElseThrow())) return;
        String key = p.getUUID() + "/" + type.getRegisteredName();
        if (!SHORTENING.add(key)) return;                                                    // 줄여서 다시 넣은 것
        int shortened = (int) Math.max(1, e.getDuration() * (1 - Math.min(0.9, v(p, ArmorFamily.DWARF, false, "slow_reduction"))));
        MobEffectInstance copy = new MobEffectInstance(type, shortened, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon());
        SkillRuntime.schedule(p.server, 1, () -> {
            try {
                if (p.hasEffect(type)) {
                    p.removeEffect(type);
                    p.addEffect(copy);
                }
            } finally {
                SHORTENING.remove(key);
            }
        });
    }

    // ── 마도: 코어 회복·스킬 마법 피해·다음 기본 공격 ──

    /** 코어 자원 자연 회복에 곱할 추가 비율 (마도 2) */
    public static double coreRegenBonus(ServerPlayer p) {
        return v(p, ArmorFamily.ARCANE, false, "core_regen");
    }

    /** 스킬 마법 피해 + (마도 4). 부르면 다음 기본 공격 마법 피해 표시 */
    public static double skillMagicBonus(ServerPlayer p) {
        if (!on(p, ArmorFamily.ARCANE, true)) return 0;
        st(p).nextBasicMagic = true;
        return v(p, ArmorFamily.ARCANE, true, "skill_magic");
    }

    /** 기본 공격(근접·화살·지팡이)에 마도 4 다음 기본 공격 마법 피해 (W × 값) */
    public static DamageInput basicBonus(ServerPlayer p, DamageInput in) {
        State s = st(p);
        if (!s.nextBasicMagic) return in;
        s.nextBasicMagic = false;
        double add = v(p, ArmorFamily.ARCANE, true, "next_basic_magic");
        return add > 0 ? in.withMagicCoef(in.magicCoef() + add) : in;
    }

    public static void forget(UUID player) {
        STATES.remove(player);
    }
}
