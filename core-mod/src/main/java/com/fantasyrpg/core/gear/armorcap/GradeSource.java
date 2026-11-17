package com.fantasyrpg.core.gear.armorcap;

import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어의 장비 사용 가능 등급(1~10).
 *
 * <p>설계상 기준은 {@code MagicCityApi.gearGrade}(원정 전 1~6, 원정 후 7~10)다. 그 API는 magiccity에 아직 없으므로
 * 지금은 오버월드 등급 {@link MagicCityApi#grade}(1~6)를 쓴다. 원정 전에는 두 값이 같다.
 * magiccity에 gearGrade가 추가되면 이 메서드 한 곳만 바꾼다.
 *
 * <p>테스트용 덮어쓰기: {@code /rpg armorcap grade <플레이어> <1~10|clear>}. 메모리에만 있고 서버를 재시작하면 사라진다.
 */
public final class GradeSource {
    public static final int MIN = 1;
    public static final int MAX = 10;

    private static final Map<UUID, Integer> OVERRIDES = new ConcurrentHashMap<>();

    private GradeSource() {
    }

    public static int grade(ServerPlayer player) {
        Integer forced = OVERRIDES.get(player.getUUID());
        int g = forced != null ? forced : MagicCityApi.grade(player);
        return Math.max(MIN, Math.min(MAX, g));
    }

    public static boolean isOverridden(ServerPlayer player) {
        return OVERRIDES.containsKey(player.getUUID());
    }

    public static void override(ServerPlayer player, int grade) {
        OVERRIDES.put(player.getUUID(), Math.max(MIN, Math.min(MAX, grade)));
    }

    public static void clearOverride(ServerPlayer player) {
        OVERRIDES.remove(player.getUUID());
    }
}
