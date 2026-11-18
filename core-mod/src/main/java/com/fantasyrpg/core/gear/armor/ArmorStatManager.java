package com.fantasyrpg.core.gear.armor;

import com.fantasyrpg.core.FantasyRpgCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code data/<ns>/armor_stats/*.json}을 읽어 계열별 수치표를 보관한다. /reload로 갱신된다.
 * 기본값은 모드 jar 안 {@code data/fantasyrpg/armor_stats/}에 있고, 데이터팩의 같은 경로 파일이 덮어쓴다.
 * 파일 이름이 '_'로 시작하면 무시한다. 검사에 걸린 파일은 거부하고 오류 로그를 남긴다.
 *
 * <p>서버에서 읽은 값이다. 혼자 하기(내장 서버)에서는 클라이언트 툴팁도 같은 값을 쓴다.
 * 전용 서버에 접속한 클라이언트에는 표가 없어서 툴팁에 수치가 안 보일 수 있다(실제 판정은 서버 값).
 */
public final class ArmorStatManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "armor_stats";
    public static final ArmorStatManager INSTANCE = new ArmorStatManager();

    private volatile Map<String, ArmorStatTable> tables = Map.of();

    private ArmorStatManager() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, ArmorStatTable> loaded = new HashMap<>();
        int errors = 0;
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            ResourceLocation file = e.getKey();
            String name = file.getPath().substring(file.getPath().lastIndexOf('/') + 1);
            if (name.startsWith("_")) continue;
            try {
                ArmorStatTable t = ArmorStatParser.parse(e.getValue().getAsJsonObject());
                if (ArmorFamily.byId(t.family()) == null) {
                    FantasyRpgCore.LOGGER.warn("[방어구] {}: 모르는 계열 '{}' (아이템이 없어 쓰이지 않음)", file, t.family());
                }
                if (loaded.put(t.family(), t) != null) {
                    FantasyRpgCore.LOGGER.warn("[방어구] 계열 {}가 여러 파일에 있음, {} 기준 사용", t.family(), file);
                }
            } catch (RuntimeException ex) {
                errors++;
                FantasyRpgCore.LOGGER.error("[방어구] {} 읽기 실패: {}", file, ex.getMessage());
            }
        }
        for (ArmorFamily f : ArmorFamily.values()) {
            if (!loaded.containsKey(f.id())) {
                FantasyRpgCore.LOGGER.error("[방어구] 계열 {}의 수치표가 없음 — 그 계열 갑옷 방어력이 0이 됨", f.id());
            }
        }
        tables = Map.copyOf(loaded);
        FantasyRpgCore.LOGGER.info("[방어구] 계열 수치표 {}개 로드, 오류 {}건", loaded.size(), errors);
    }

    /** 표가 없으면 null */
    public ArmorStatTable get(ArmorFamily family) {
        return tables.get(family.id());
    }
}
