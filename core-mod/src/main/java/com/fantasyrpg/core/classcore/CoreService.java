package com.fantasyrpg.core.classcore;

import com.fantasyrpg.core.network.RpgNetwork;
import com.fantasyrpg.core.playerdata.RpgClass;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.fantasyrpg.core.skilltree.SkillData;
import com.fantasyrpg.core.skills.SkillPassives;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 직업 코어 자원 현재값 (서버, 저장하지 않음 — 전투 상태라 접속·부활·직업 변경 때 시작값).
 * 매 틱 자동 회복·감소, 기본 공격 적중 등 사건으로 얻음, 바뀌면 클라이언트로 보냄. 소비는 스킬이 생기면.
 */
public final class CoreService {
    private CoreService() {
    }

    /** 사건 이름 (코어 데이터 gain 키) */
    public static final String BASIC_ATTACK_HIT = "basic_attack_hit";

    private static final class State {
        double value;
        long lastGainTick;
        int lastSentTenths = Integer.MIN_VALUE;
        int lastSentMax = Integer.MIN_VALUE;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private static CoreDef def(ServerPlayer p) {
        RpgClass c = RpgProgress.get(p).classOrNull();
        CoreDef d = c == null ? null : SkillData.core(c.id());
        if (d == null) return null;
        double bonus = SkillPassives.coreMaxBonus(p); // 비전 이해 3랭크
        return bonus > 0 ? d.withMax(d.max() + bonus) : d;
    }

    public static void reset(ServerPlayer p) {
        CoreDef d = def(p);
        State s = new State();
        s.value = d == null ? 0 : d.start();
        s.lastGainTick = p.level().getGameTime();
        STATES.put(p.getUUID(), s);
        send(p, d, s, true);
    }

    public static double value(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        return s == null ? 0 : s.value;
    }

    public static void onEvent(ServerPlayer p, String event) {
        CoreDef d = def(p);
        if (d == null) return;
        double amount = d.gainFor(event);
        if (amount == 0) return;
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        s.value = d.add(s.value, amount > 0 ? amount * (1 + regenAttr(p)) : amount);   // 순환의 룬석: 늘어나는 양 +%
        s.lastGainTick = p.level().getGameTime();
        send(p, d, s, false);
    }

    /** 패시브·스킬이 코어를 직접 줄 때 (사건 이름 없이) */
    public static void gain(ServerPlayer p, double amount) {
        CoreDef d = def(p);
        if (d == null || amount <= 0) return;
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        s.value = d.add(s.value, amount > 0 ? amount * (1 + regenAttr(p)) : amount);   // 순환의 룬석: 늘어나는 양 +%
        s.lastGainTick = p.level().getGameTime();
        send(p, d, s, false);
    }

    /** 있는 만큼만 쓰기 (마력 방벽). 쓴 양 */
    public static double spendUpTo(ServerPlayer p, double amount) {
        CoreDef d = def(p);
        State s = STATES.get(p.getUUID());
        if (d == null || s == null || amount <= 0) return 0;
        double used = Math.min(s.value, amount);
        s.value = d.clamp(s.value - used);
        send(p, d, s, false);
        return used;
    }

    /** 스킬이 코어를 쓸 때. 모자라면 false */
    public static boolean spend(ServerPlayer p, double amount) {
        CoreDef d = def(p);
        State s = STATES.get(p.getUUID());
        if (d == null || s == null || s.value < amount) return false;
        s.value = d.clamp(s.value - amount);
        send(p, d, s, false);
        return true;
    }

    /** 능력치 fantasyrpg:core_regen (순환의 룬석) */
    private static double regenAttr(ServerPlayer p) {
        var inst = p.getAttribute(com.fantasyrpg.core.gear.RpgAttributes.CORE_REGEN);
        return inst == null ? 0 : Math.max(0, inst.getValue());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        CoreDef d = def(p);
        if (d == null) return;
        State s = STATES.computeIfAbsent(p.getUUID(), k -> {
            State n = new State();
            n.value = d.start();
            return n;
        });
        double before = s.value;
        s.value = d.tick(s.value, p.level().getGameTime() - s.lastGainTick);
        double setBonus = com.fantasyrpg.core.gear.armor.SetEffects.coreRegenBonus(p) + regenAttr(p);   // 마도 2세트(자연 회복) + 순환의 룬석
        if (setBonus > 0 && s.value > before) s.value = d.clamp(s.value + (s.value - before) * setBonus);
        double regen = SkillPassives.sum(p, "core_regen"); // 대마법사·정화자 마스터리
        if (regen > 0) s.value = d.clamp(s.value + regen / 20.0);
        send(p, d, s, false, d.max());
    }

    private static void send(ServerPlayer p, CoreDef d, State s, boolean force) {
        send(p, d, s, force, d == null ? 0 : d.max());
    }

    private static void send(ServerPlayer p, CoreDef d, State s, boolean force, double max) {
        if (p.connection == null) return;
        int tenths = (int) Math.round(s.value * 10);
        int maxTenths = (int) Math.round(max * 10);
        if (!force && tenths == s.lastSentTenths && maxTenths == s.lastSentMax) return;
        s.lastSentTenths = tenths;
        s.lastSentMax = maxTenths;
        PacketDistributor.sendToPlayer(p, new RpgNetwork.Core(d == null ? "" : d.id(), (float) s.value,
                d == null ? 0f : (float) d.max()));
    }

    public static void forget(ServerPlayer p) {
        STATES.remove(p.getUUID());
    }
}
