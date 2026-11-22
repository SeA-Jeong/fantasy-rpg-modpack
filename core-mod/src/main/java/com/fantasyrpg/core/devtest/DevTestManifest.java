package com.fantasyrpg.core.devtest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 개발자 전용 테스트 장비 목록 (순수 로직, Minecraft 클래스 없음).
 * <p>
 * 한 항목 = 아이템 id 하나 + 직업·등급·부위 + 리소스 경로(모델·텍스처·아이콘·애니메이션) + 활성 리비전.
 * 경로의 {@code {id}} 는 항목 id 로 바뀐다. 항목에 경로가 없으면 {@code defaults} 를 쓴다.
 * 형식이 틀린 항목은 건너뛰고 {@link #errors()} 에 이유를 남긴다 (게임은 계속 켜진다).
 * 같은 규칙을 {@code tools/devtest/test_armor.py} 가 파이썬으로 검사한다.
 */
public final class DevTestManifest {
    public static final Set<String> SLOTS = Set.of("helmet", "chestplate", "leggings", "boots");
    private static final Pattern ID = Pattern.compile("^[a-z0-9_]+$");

    /** 경로는 모두 네임스페이스 없는 리소스 경로 (예: geo/test_armor/x.geo.json). animation·idleAnimation 은 빈 문자열 가능 */
    public record Entry(String id, String cls, int grade, String slot, String model, String texture, String icon,
                        String animation, String idleAnimation, String revision, String note) {
    }

    private final String namespace;
    private final List<Entry> entries;
    private final List<String> errors;

    private DevTestManifest(String namespace, List<Entry> entries, List<String> errors) {
        this.namespace = namespace;
        this.entries = List.copyOf(entries);
        this.errors = List.copyOf(errors);
    }

    public static DevTestManifest empty() {
        return new DevTestManifest("fantasyrpg", List.of(), List.of());
    }

    public static DevTestManifest parse(String json) {
        List<String> errors = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            return new DevTestManifest("fantasyrpg", List.of(), List.of("목록 JSON 읽기 실패: " + e.getMessage()));
        }
        String ns = str(root, "namespace", "fantasyrpg");
        JsonObject defaults = root.has("defaults") && root.get("defaults").isJsonObject() ? root.getAsJsonObject("defaults") : new JsonObject();
        Set<String> seen = new LinkedHashSet<>();
        if (!root.has("entries") || !root.get("entries").isJsonArray()) {
            errors.add("entries 배열이 없음");
            return new DevTestManifest(ns, entries, errors);
        }
        int index = 0;
        for (JsonElement el : root.getAsJsonArray("entries")) {
            index++;
            if (!el.isJsonObject()) {
                errors.add("#" + index + ": 객체가 아님");
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String id = str(o, "id", "");
            String where = "#" + index + " " + (id.isEmpty() ? "(id 없음)" : id) + ": ";
            List<String> bad = new ArrayList<>();
            if (!ID.matcher(id).matches()) bad.add("id 는 소문자·숫자·_ 만");
            else if (!id.startsWith("test_")) bad.add("id 는 test_ 로 시작 (기존 아이템 id 와 겹치지 않게)");
            if (seen.contains(id)) bad.add("id 중복");
            String cls = str(o, "class", "");
            if (!ID.matcher(cls).matches()) bad.add("class 는 소문자·숫자·_ 만");
            int grade = o.has("grade") && o.get("grade").isJsonPrimitive() && o.get("grade").getAsJsonPrimitive().isNumber()
                    ? o.get("grade").getAsInt() : -1;
            if (grade < 1 || grade > 10) bad.add("grade 는 1~10");
            String slot = str(o, "slot", "");
            if (!SLOTS.contains(slot)) bad.add("slot 은 helmet·chestplate·leggings·boots");
            String model = path(o, defaults, "model", id);
            String texture = path(o, defaults, "texture", id);
            String icon = path(o, defaults, "icon", id);
            String animation = path(o, defaults, "animation", id);
            String idle = path(o, defaults, "idle_animation", id);
            if (!model.startsWith("geo/") || !model.endsWith(".geo.json")) bad.add("model 은 geo/…/*.geo.json (GeckoLib 규칙)");
            if (!texture.startsWith("textures/") || !texture.endsWith(".png")) bad.add("texture 는 textures/…/*.png");
            if (!icon.startsWith("textures/") || !icon.endsWith(".png")) bad.add("icon 은 textures/…/*.png");
            if (!animation.isEmpty() && !animation.startsWith("animations/")) bad.add("animation 은 animations/… (GeckoLib 규칙)");
            if (!idle.isEmpty() && animation.isEmpty()) bad.add("idle_animation 은 animation 파일이 있어야 함");
            if (!bad.isEmpty()) {
                errors.add(where + String.join(", ", bad));
                continue;
            }
            seen.add(id);
            entries.add(new Entry(id, cls, grade, slot, model, texture, icon, animation, idle,
                    str(o, "revision", ""), str(o, "note", "")));
        }
        return new DevTestManifest(ns, entries, errors);
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : fallback;
    }

    private static String path(JsonObject entry, JsonObject defaults, String key, String id) {
        String v = entry.has(key) ? str(entry, key, "") : str(defaults, key, "");
        return v.replace("{id}", id);
    }

    public String namespace() {
        return namespace;
    }

    public List<Entry> entries() {
        return entries;
    }

    public List<String> errors() {
        return errors;
    }

    public Optional<Entry> byId(String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    /** 직업·등급·부위로 찾기 (부위가 null 이면 그 직업·등급의 첫 항목) */
    public Optional<Entry> find(String cls, int grade, String slot) {
        return entries.stream()
                .filter(e -> e.cls().equals(cls) && e.grade() == grade && (slot == null || e.slot().equals(slot)))
                .findFirst();
    }

    public List<String> classes() {
        return entries.stream().map(Entry::cls).distinct().toList();
    }
}
