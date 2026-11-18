package com.fantasyrpg.core.skilltree;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.classcore.CoreDef;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 스킬 관련 데이터 로더 3종 (/reload로 갱신, '_'로 시작하는 파일 무시):
 * <ul>
 *   <li>{@code skill_nodes/<직업>/*.json} → 직업별 노드 (원문 JSON도 보관해 클라이언트로 보냄)</li>
 *   <li>{@code passive_nodes/*.json} → 공통 패시브 노드</li>
 *   <li>{@code class_cores/*.json} → 직업 코어 자원 (파일 안의 "class")</li>
 * </ul>
 * 실제 노드·코어 내용은 다음 단계. 지금은 테스트 데이터팩에 시험값만 있다(claude/스킬트리_설계.md 7절).
 */
public final class SkillData {
    private SkillData() {
    }

    private static volatile Map<String, Map<String, SkillNode>> nodesByClass = Map.of();
    private static volatile Map<String, List<String>> rawNodesByClass = Map.of();
    private static volatile Map<String, PassiveNode> passives = Map.of();
    private static volatile List<String> rawPassives = List.of();
    private static volatile Map<String, CoreDef> coresByClass = Map.of();

    public static Map<String, SkillNode> nodes(String classId) {
        return nodesByClass.getOrDefault(classId, Map.of());
    }

    public static List<String> rawNodes(String classId) {
        return rawNodesByClass.getOrDefault(classId, List.of());
    }

    public static Map<String, PassiveNode> passives() {
        return passives;
    }

    public static List<String> rawPassives() {
        return rawPassives;
    }

    public static CoreDef core(String classId) {
        return coresByClass.get(classId);
    }

    private static boolean skip(ResourceLocation file) {
        String path = file.getPath();
        return path.substring(path.lastIndexOf('/') + 1).startsWith("_");
    }

    public static final SimpleJsonResourceReloadListener NODES = new SimpleJsonResourceReloadListener(new Gson(), "skill_nodes") {
        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, Map<String, SkillNode>> byClass = new HashMap<>();
            Map<String, List<String>> raw = new HashMap<>();
            int errors = 0;
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                if (skip(e.getKey())) continue;
                String path = e.getKey().getPath();
                int slash = path.indexOf('/');
                String classId = slash > 0 ? path.substring(0, slash) : null;
                try {
                    SkillNode n = SkillNode.parse(classId, e.getValue().getAsJsonObject());
                    byClass.computeIfAbsent(n.classId(), k -> new HashMap<>()).put(n.id(), n);
                    raw.computeIfAbsent(n.classId(), k -> new ArrayList<>()).add(withClass(e.getValue(), n.classId()));
                } catch (RuntimeException ex) {
                    errors++;
                    FantasyRpgCore.LOGGER.error("[스킬] {} 읽기 실패: {}", e.getKey(), ex.getMessage());
                }
            }
            Map<String, Map<String, SkillNode>> frozen = new HashMap<>();
            byClass.forEach((k, v) -> frozen.put(k, Map.copyOf(v)));
            nodesByClass = Map.copyOf(frozen);
            Map<String, List<String>> frozenRaw = new HashMap<>();
            raw.forEach((k, v) -> frozenRaw.put(k, List.copyOf(v)));
            rawNodesByClass = Map.copyOf(frozenRaw);
            int total = byClass.values().stream().mapToInt(Map::size).sum();
            FantasyRpgCore.LOGGER.info("[스킬] 스킬 노드 {}개({}직업) 로드, 오류 {}건", total, byClass.size(), errors);
        }
    };

    private static String withClass(JsonElement json, String classId) {
        com.google.gson.JsonObject copy = json.getAsJsonObject().deepCopy();
        copy.addProperty("class", classId);
        return copy.toString();
    }

    public static final SimpleJsonResourceReloadListener PASSIVES = new SimpleJsonResourceReloadListener(new Gson(), "passive_nodes") {
        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, PassiveNode> loaded = new HashMap<>();
            List<String> raw = new ArrayList<>();
            int errors = 0;
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                if (skip(e.getKey())) continue;
                try {
                    PassiveNode n = PassiveNode.parse(e.getValue().getAsJsonObject());
                    loaded.put(n.id(), n);
                    raw.add(e.getValue().toString());
                } catch (RuntimeException ex) {
                    errors++;
                    FantasyRpgCore.LOGGER.error("[스킬] 공통 패시브 {} 읽기 실패: {}", e.getKey(), ex.getMessage());
                }
            }
            passives = Map.copyOf(loaded);
            rawPassives = List.copyOf(raw);
            FantasyRpgCore.LOGGER.info("[스킬] 공통 패시브 노드 {}개 로드, 오류 {}건", loaded.size(), errors);
        }
    };

    public static final SimpleJsonResourceReloadListener CORES = new SimpleJsonResourceReloadListener(new Gson(), "class_cores") {
        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, CoreDef> loaded = new HashMap<>();
            int errors = 0;
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                if (skip(e.getKey())) continue;
                try {
                    CoreDef c = CoreDef.parse(e.getValue().getAsJsonObject());
                    loaded.put(c.classId(), c);
                } catch (RuntimeException ex) {
                    errors++;
                    FantasyRpgCore.LOGGER.error("[스킬] 코어 {} 읽기 실패: {}", e.getKey(), ex.getMessage());
                }
            }
            coresByClass = Map.copyOf(loaded);
            FantasyRpgCore.LOGGER.info("[스킬] 직업 코어 {}개 로드, 오류 {}건", loaded.size(), errors);
        }
    };
}
