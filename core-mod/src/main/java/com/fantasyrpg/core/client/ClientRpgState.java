package com.fantasyrpg.core.client;

import com.fantasyrpg.core.skilltree.PassiveNode;
import com.fantasyrpg.core.skilltree.SkillNode;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 클라이언트가 서버에서 받은 성장·스킬·코어 상태 (표시 전용, 판정에 쓰지 않음).
 * Minecraft 클라이언트 클래스를 참조하지 않아 공통 코드의 패킷 처리기에서 써도 안전하다.
 */
public final class ClientRpgState {
    private ClientRpgState() {
    }

    public static volatile String rpgClass = "";
    public static volatile int level = 1;
    public static volatile int xp;
    public static volatile int need;
    public static volatile int cap = 10;
    public static volatile int spTotal;
    public static volatile int spSpent;
    public static volatile int passivePoints;
    public static volatile int passiveSpent;
    public static volatile int freeResets;
    public static volatile Map<String, Integer> ranks = Map.of();
    public static volatile List<String> slots = List.of("", "", "", "", "");
    public static volatile Map<String, Integer> passiveRanks = Map.of();
    public static volatile Map<String, SkillNode> nodes = Map.of();
    public static volatile Map<String, PassiveNode> passives = Map.of();
    public static volatile String coreId = "";
    public static volatile float coreValue;
    public static volatile float coreMax;
    public static volatile boolean received;
    public static volatile String activeMastery = "";
    /** 장착 프리셋: 쓸 수 있는 수, 프리셋마다 장착칸 5개와 마스터리 */
    public static volatile int presetsUnlocked = 1;
    /** 유산 무기 (서버 계산 결과) */
    public static volatile CompoundTag heirloom = new CompoundTag();
    public static volatile List<List<String>> presetSlots = List.of(List.of(), List.of(), List.of());
    public static volatile List<String> presetMastery = List.of("", "", "");
    /** 노드 id → 쿨타임 끝나는 시각(ms) */
    public static final Map<String, Long> cooldownEnd = new java.util.concurrent.ConcurrentHashMap<>();

    /** 배운 패시브가 effect 의 key 에 더하는 양 (서버 SkillPassives.modSum 과 같은 계산, 표시용) */
    public static double modSum(String effect, String key) {
        double total = 0;
        for (SkillNode n : nodes.values()) {
            int r = ranks.getOrDefault(n.id(), 0);
            if (r <= 0 || (n.kind() == SkillNode.Kind.MASTERY && !n.id().equals(activeMastery))) continue;
            for (SkillNode.Mod m : n.mods()) if (m.effect().equals(effect) && m.key().equals(key)) total += m.at(r);
        }
        return total;
    }

    /** effect 를 강화하는 배운 패시브 이름 */
    public static List<String> modifiers(String effect) {
        List<String> out = new ArrayList<>();
        for (SkillNode n : nodes.values()) {
            int r = ranks.getOrDefault(n.id(), 0);
            if (r <= 0 || (n.kind() == SkillNode.Kind.MASTERY && !n.id().equals(activeMastery))) continue;
            for (SkillNode.Mod m : n.mods()) {
                if (m.effect().equals(effect) && m.at(r) != 0) {
                    out.add(n.name() + " " + r);
                    break;
                }
            }
        }
        return out;
    }

    /** 서장 NPC 직업 선택 화면 (서버가 연다) */
    public static void openClassSelect() {
        ClassSelectScreen.open();
    }

    public static void acceptCooldown(String nodeId, int ticks) {
        cooldownEnd.put(nodeId, System.currentTimeMillis() + ticks * 50L);
    }

    /** 남은 쿨타임(초), 없으면 0 */
    public static double cooldownLeft(String nodeId) {
        Long end = cooldownEnd.get(nodeId);
        return end == null ? 0 : Math.max(0, (end - System.currentTimeMillis()) / 1000.0);
    }

    public static void acceptState(CompoundTag t) {
        rpgClass = t.getString("class");
        level = t.getInt("level");
        xp = t.getInt("xp");
        need = t.getInt("need");
        cap = t.getInt("cap");
        spTotal = t.getInt("sp_total");
        spSpent = t.getInt("sp_spent");
        passivePoints = t.getInt("passive_points");
        passiveSpent = t.getInt("passive_spent");
        freeResets = t.getInt("free_resets");
        activeMastery = t.getString("active_mastery");
        ranks = readMap(t.getCompound("ranks"));
        passiveRanks = readMap(t.getCompound("passive_ranks"));
        List<String> s = new ArrayList<>();
        ListTag list = t.getList("slots", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) s.add(list.getString(i));
        while (s.size() < 5) s.add("");
        slots = List.copyOf(s);
        presetsUnlocked = t.getInt("presets_unlocked");
        heirloom = t.getCompound("heirloom");
        List<List<String>> ps = new ArrayList<>();
        List<String> pm = new ArrayList<>();
        ListTag pl = t.getList("presets", Tag.TAG_COMPOUND);
        for (int i = 0; i < 3; i++) {
            CompoundTag pt = i < pl.size() ? pl.getCompound(i) : new CompoundTag();
            List<String> one = new ArrayList<>();
            ListTag sl = pt.getList("slots", Tag.TAG_STRING);
            for (int j = 0; j < sl.size(); j++) one.add(sl.getString(j));
            ps.add(List.copyOf(one));
            pm.add(pt.getString("mastery"));
        }
        presetSlots = List.copyOf(ps);
        presetMastery = List.copyOf(pm);
        received = true;
    }

    private static Map<String, Integer> readMap(CompoundTag c) {
        Map<String, Integer> m = new HashMap<>();
        for (String k : c.getAllKeys()) m.put(k, c.getInt(k));
        return Map.copyOf(m);
    }

    public static void acceptDefs(String classId, List<String> nodeJson, List<String> passiveJson) {
        Map<String, SkillNode> n = new LinkedHashMap<>();
        for (String j : nodeJson) {
            try {
                SkillNode node = SkillNode.parse(classId, JsonParser.parseString(j).getAsJsonObject());
                n.put(node.id(), node);
            } catch (RuntimeException ignored) {
                // 서버에서 이미 검사한 데이터. 실패하면 표시만 빠짐
            }
        }
        Map<String, PassiveNode> p = new LinkedHashMap<>();
        for (String j : passiveJson) {
            try {
                PassiveNode node = PassiveNode.parse(JsonParser.parseString(j).getAsJsonObject());
                p.put(node.id(), node);
            } catch (RuntimeException ignored) {
            }
        }
        nodes = Map.copyOf(n);
        passives = Map.copyOf(p);
    }

    public static void acceptCore(String id, float value, float max) {
        coreId = id;
        coreValue = value;
        coreMax = max;
    }
}
