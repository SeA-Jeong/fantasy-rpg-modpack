package com.fantasyrpg.core.devtest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 개발 테스트 장비 목록 규칙과 배포 리소스 일치 (2026-11-23) */
class DevTestManifestTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final Path BUILT_IN = RES.resolve("fantasyrpg_devtest/armor_manifest.json");
    private static final Path SOURCE = Path.of("../source_assets/test_armor/manifest.json");

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    void builtInManifestHasThreeValidEntries() throws IOException {
        DevTestManifest m = DevTestManifest.parse(read(BUILT_IN));
        assertEquals(java.util.List.of(), m.errors());
        assertEquals(3, m.entries().size());
        DevTestManifest.Entry mage = m.find("mage", 5, "chestplate").orElseThrow();
        assertEquals("test_mage_chest_t05", mage.id());
        assertEquals("geo/test_armor/test_mage_chest_t05.geo.json", mage.model());
        assertEquals("textures/armor/test_armor/test_mage_chest_t05.png", mage.texture());
        assertEquals("textures/item/test_armor/test_mage_chest_t05.png", mage.icon());
        assertEquals("", mage.animation());
        assertTrue(m.find("rogue", 10, null).isPresent());
        assertTrue(m.find("rogue", 9, null).isEmpty());
    }

    @Test
    void builtInManifestMatchesSourceAssets() throws IOException {
        assertEquals(JsonParser.parseString(read(SOURCE)), JsonParser.parseString(read(BUILT_IN)),
                "source_assets 목록을 바꿨으면 python3 tools/devtest/test_armor.py deploy repo");
    }

    /** 항목마다 모델·텍스처·아이콘·아이템 모델이 모드 리소스에 있고, 본 좌우가 GeckoLib 규칙(x<0 = 오른쪽)과 맞음 */
    @Test
    void everyEntryHasResourcesWithCorrectBoneSides() throws IOException {
        DevTestManifest m = DevTestManifest.parse(read(BUILT_IN));
        Path ns = RES.resolve("assets").resolve(m.namespace());
        for (DevTestManifest.Entry e : m.entries()) {
            for (String p : new String[]{e.model(), e.texture(), e.icon(), "models/item/" + e.id() + ".json"}) {
                assertTrue(Files.isRegularFile(ns.resolve(p)), e.id() + ": " + p + " 없음");
            }
            JsonObject geo = JsonParser.parseString(read(ns.resolve(e.model()))).getAsJsonObject()
                    .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            boolean hasBody = false;
            for (JsonElement be : geo.getAsJsonArray("bones")) {
                JsonObject b = be.getAsJsonObject();
                String name = b.get("name").getAsString();
                hasBody |= name.equals("armorBody");
                if (!b.has("cubes") || !(name.startsWith("armorRight") || name.startsWith("armorLeft"))) continue;
                JsonArray cubes = b.getAsJsonArray("cubes");
                double cx = 0;
                for (JsonElement ce : cubes) {
                    JsonObject c = ce.getAsJsonObject();
                    cx += c.getAsJsonArray("origin").get(0).getAsDouble() + c.getAsJsonArray("size").get(0).getAsDouble() / 2;
                }
                cx /= cubes.size();
                if (name.startsWith("armorRight")) assertTrue(cx < 0, e.id() + " " + name + " 큐브가 왼쪽에 있음 (x " + cx + ")");
                else assertTrue(cx > 0, e.id() + " " + name + " 큐브가 오른쪽에 있음 (x " + cx + ")");
            }
            assertTrue(hasBody, e.id() + ": armorBody 본 없음");
        }
    }

    @Test
    void badEntriesAreSkippedWithReasons() {
        String json = """
                {"namespace":"fantasyrpg","defaults":{"model":"geo/test_armor/{id}.geo.json","texture":"textures/armor/test_armor/{id}.png","icon":"textures/item/test_armor/{id}.png"},
                 "entries":[
                  {"id":"test_ok","class":"knight","grade":1,"slot":"chestplate"},
                  {"id":"test_ok","class":"knight","grade":2,"slot":"chestplate"},
                  {"id":"knight_chestplate","class":"knight","grade":1,"slot":"chestplate"},
                  {"id":"test_bad_slot","class":"knight","grade":1,"slot":"cape"},
                  {"id":"test_bad_grade","class":"knight","grade":11,"slot":"boots"},
                  {"id":"test_bad_model","class":"mage","grade":3,"slot":"helmet","model":"models/x.json"},
                  {"id":"test_bad_anim","class":"mage","grade":3,"slot":"helmet","idle_animation":"idle"},
                  {"id":"test_anim","class":"mage","grade":4,"slot":"leggings","animation":"animations/test_armor/{id}.animation.json","idle_animation":"idle"}
                 ]}""";
        DevTestManifest m = DevTestManifest.parse(json);
        assertEquals(java.util.List.of("test_ok", "test_anim"), m.entries().stream().map(DevTestManifest.Entry::id).toList());
        assertEquals(6, m.errors().size(), m.errors().toString());
        assertTrue(m.errors().stream().anyMatch(s -> s.contains("중복")));
        assertTrue(m.errors().stream().anyMatch(s -> s.contains("test_")));
        assertEquals("animations/test_armor/test_anim.animation.json", m.byId("test_anim").orElseThrow().animation());
    }

    @Test
    void brokenJsonGivesEmptyManifestNotCrash() {
        DevTestManifest m = DevTestManifest.parse("{ 이건 JSON 아님");
        assertTrue(m.entries().isEmpty());
        assertEquals(1, m.errors().size());
    }

    @Test
    void switchRules() {
        assertTrue(DevTestSwitch.enabled(false, null, false), "개발 환경은 기본으로 켜짐");
        assertFalse(DevTestSwitch.enabled(true, null, false), "일반 배포는 기본으로 꺼짐");
        assertTrue(DevTestSwitch.enabled(true, null, true), "ENABLE 파일이 있으면 켜짐");
        assertFalse(DevTestSwitch.enabled(false, "false", true), "시스템 속성이 가장 우선");
        assertTrue(DevTestSwitch.enabled(true, "true", false));
    }
}
