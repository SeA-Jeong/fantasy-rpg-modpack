package com.fantasyrpg.core.network;

import com.fantasyrpg.core.FantasyRpgCore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 전용 서버 → 클라이언트 데이터 동기화 (2026-11-21).
 * 서버가 읽은 데이터 폴더(armor_stats, runes, armor_sets, grid_recipes, armor_crafting, combat)의 원본 JSON 을
 * 접속·/reload 때 보내고, 클라이언트는 같은 읽기 코드로 적용한다 → 툴팁 수치·JEI 작업대 조합이 전용 서버에서도 보임.
 * 판정은 언제나 서버 값. 혼자 하기(내장 서버)는 이미 같은 값을 쓰므로 받지 않는다.
 */
public final class DataSync {
    private DataSync() {}

    /** 클라이언트에 보낼 최대 크기 (바닐라 사용자 패킷 상한 1MiB 안) */
    public static final int MAX_CHARS = 900_000;

    private static final Map<String, Consumer<Map<ResourceLocation, JsonElement>>> APPLIERS = new ConcurrentHashMap<>();
    private static final Map<String, Map<ResourceLocation, JsonElement>> RAW = new ConcurrentHashMap<>();

    /** 모드 생성자에서: 리스너 클래스를 미리 올려 읽기 코드를 등록 (클라이언트는 쓰기 전까지 안 올라오므로) */
    public static void bootstrap() {
        Object[] touch = {com.fantasyrpg.core.gear.armor.ArmorStatManager.INSTANCE, com.fantasyrpg.core.gear.rune.RuneStatManager.INSTANCE,
                com.fantasyrpg.core.combat.CombatManager.INSTANCE, com.fantasyrpg.core.craft.CraftData.RECIPES, com.fantasyrpg.core.craft.CraftData.ARMOR};
        com.fantasyrpg.core.gear.armor.SetEffects.bootstrap();
        FantasyRpgCore.LOGGER.debug("[동기화] 읽기 코드 {}개 등록 ({}개 리스너)", APPLIERS.size(), touch.length);
    }

    /** 읽기 코드 등록 (리스너를 만들 때, 서버·클라이언트 양쪽) */
    public static void register(String dir, Consumer<Map<ResourceLocation, JsonElement>> applier) {
        APPLIERS.put(dir, applier);
    }

    /** 서버가 읽은 원본 기억 (apply 첫 줄에서) */
    public static void remember(String dir, Map<ResourceLocation, JsonElement> files) {
        RAW.put(dir, Map.copyOf(files));
    }

    public record Payload(String dir, String json) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, "data_sync"));
        public static final StreamCodec<ByteBuf, Payload> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Payload::dir,
                ByteBufCodecs.stringUtf8(MAX_CHARS), Payload::json,
                Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 원본 → 보낼 문자열 */
    public static String encode(Map<ResourceLocation, JsonElement> files) {
        JsonObject o = new JsonObject();
        for (var e : files.entrySet()) o.add(e.getKey().toString(), e.getValue());
        return o.toString();
    }

    public static Map<ResourceLocation, JsonElement> decode(String json) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        for (var e : JsonParser.parseString(json).getAsJsonObject().entrySet()) out.put(ResourceLocation.parse(e.getKey()), e.getValue());
        return out;
    }

    /** 접속(그 사람) 또는 /reload(모두) */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        List<ServerPlayer> to = event.getRelevantPlayers().toList();
        if (to.isEmpty()) return;
        int sent = 0;
        for (var e : RAW.entrySet()) {
            String json = encode(e.getValue());
            if (json.length() > MAX_CHARS) {
                FantasyRpgCore.LOGGER.warn("[동기화] {} 가 너무 커서 보내지 않음 ({}자) — 클라이언트 툴팁만 빠짐", e.getKey(), json.length());
                continue;
            }
            Payload p = new Payload(e.getKey(), json);
            for (ServerPlayer sp : to) PacketDistributor.sendToPlayer(sp, p);
            sent++;
        }
        FantasyRpgCore.LOGGER.info("[동기화] 데이터 {}개 폴더를 {}명에게 보냄", sent, to.size());
    }

    /** 클라이언트: 받은 원본을 같은 읽기 코드로 적용 (내장 서버면 건너뜀은 호출하는 쪽에서) */
    public static void apply(String dir, String json) {
        Consumer<Map<ResourceLocation, JsonElement>> a = APPLIERS.get(dir);
        if (a == null) {
            FantasyRpgCore.LOGGER.warn("[동기화] 모르는 데이터 폴더 {}", dir);
            return;
        }
        try {
            a.accept(decode(json));
        } catch (RuntimeException ex) {
            FantasyRpgCore.LOGGER.error("[동기화] {} 적용 실패: {}", dir, ex.toString());
        }
    }
}
