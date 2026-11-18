package com.fantasyrpg.core.skills;

import com.fantasyrpg.core.network.RpgNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스킬 실행 중 상태 (서버 메모리, 저장하지 않음): 쿨타임, 연쇄, 지속 효과, 표식, 예약 실행.
 * 시간은 서버 틱 수({@link MinecraftServer#getTickCount()}) 기준이라 차원 이동과 무관하다.
 */
public final class SkillRuntime {
    private SkillRuntime() {
    }

    /** 플레이어마다 하나 */
    public static final class State {
        final Map<String, Long> cooldownEnd = new HashMap<>();
        // 주문 연쇄
        String lastCastNode = "";
        long lastCastTick = -1000;
        int chainCount;
        boolean chainBonusReady;
        // 연계 감각 3랭크
        long lastSkillHitTick = -1000;
        // 검술 숙련 5랭크
        UUID lastBasicTarget;
        int basicConsecutive;
        // 마탄 단련 5랭크
        int boltCount;
        // 불굴
        long indomitableEnd = -1;
        String indomitableNode = "";
        // 투지 3랭크
        long lowHealthReady;
        // 흡수 체력: 수정자 id → 끝나는 틱
        final Map<String, Long> absorptionEnd = new HashMap<>();
        // 결계 충격 5랭크 반사
        long barrierEnd = -1;
        long lastReflect = -1000;
        // 마력 베기 표식: 대상 → 끝나는 틱
        final Map<UUID, Long> marks = new HashMap<>();
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    public static State state(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new State());
    }

    public static void forget(ServerPlayer p) {
        STATES.remove(p.getUUID());
    }

    public static long now(ServerPlayer p) {
        return p.server.getTickCount();
    }

    // ── 쿨타임 ──

    public static int cooldownLeft(ServerPlayer p, String nodeId) {
        Long end = state(p).cooldownEnd.get(nodeId);
        return end == null ? 0 : (int) Math.max(0, end - now(p));
    }

    public static void setCooldown(ServerPlayer p, String nodeId, int ticks) {
        state(p).cooldownEnd.put(nodeId, now(p) + Math.max(0, ticks));
        if (p.connection != null) PacketDistributor.sendToPlayer(p, new RpgNetwork.Cooldown(nodeId, Math.max(0, ticks)));
    }

    /** 접속·부활 때 남은 쿨타임 다시 보내기 */
    public static void resendCooldowns(ServerPlayer p) {
        if (p.connection == null) return;
        for (Map.Entry<String, Long> e : state(p).cooldownEnd.entrySet()) {
            int left = (int) Math.max(0, e.getValue() - now(p));
            if (left > 0) PacketDistributor.sendToPlayer(p, new RpgNetwork.Cooldown(e.getKey(), left));
        }
    }

    // ── 예약 실행 (연타·마탄·잔향 폭발) ──

    private record Task(long due, Runnable run) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    public static synchronized void schedule(MinecraftServer server, int delayTicks, Runnable run) {
        TASKS.add(new Task(server.getTickCount() + Math.max(1, delayTicks), run));
    }

    /** ServerTickEvent.Post 에서 부름 */
    public static void runDue(MinecraftServer server) {
        List<Runnable> due = new ArrayList<>();
        synchronized (SkillRuntime.class) {
            long now = server.getTickCount();
            Iterator<Task> it = TASKS.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (t.due() <= now) {
                    due.add(t.run());
                    it.remove();
                }
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (RuntimeException e) {
                com.fantasyrpg.core.FantasyRpgCore.LOGGER.error("[스킬] 예약 실행 오류", e);
            }
        }
    }
}
