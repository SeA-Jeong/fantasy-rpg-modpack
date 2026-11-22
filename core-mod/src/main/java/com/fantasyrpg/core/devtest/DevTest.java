package com.fantasyrpg.core.devtest;

import com.fantasyrpg.core.FantasyRpgCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 개발자 전용 테스트 장비 진입점 (2026-11-23, 장비 모델링 반복 테스트 환경).
 * <p>
 * 일반 게임에는 영향이 없다: 꺼져 있으면 아이템·명령어·창작 탭을 하나도 등록하지 않는다.
 * 켜져 있어도 테스트 장비는 능력치 0 이고 제작법·드롭이 없으며 기존 장비 코드는 이 아이템을 모른다.
 * GeckoLib 이 없으면 켜지 않는다 (GeckoLib 클래스는 {@link DevTestRegistry} 이후에서만 쓴다).
 * <p>
 * 목록: {@code config/fantasyrpg-devtest/armor_manifest.json} 이 있으면 그것, 없으면 모드 안의 {@code /fantasyrpg_devtest/armor_manifest.json}.
 */
public final class DevTest {
    public static final String SYSTEM_PROPERTY = "fantasyrpg.devtest";
    private static boolean enabled;
    private static DevTestManifest manifest = DevTestManifest.empty();
    private static String source = "없음";

    private DevTest() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static DevTestManifest manifest() {
        return manifest;
    }

    public static String source() {
        return source;
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get().resolve("fantasyrpg-devtest");
    }

    public static void bootstrap(IEventBus modBus) {
        Path dir = configDir();
        enabled = DevTestSwitch.enabled(FMLEnvironment.production, System.getProperty(SYSTEM_PROPERTY), Files.exists(dir.resolve("ENABLE")));
        if (!enabled) {
            FantasyRpgCore.LOGGER.info("[개발 테스트] 꺼짐 (켜기: config/fantasyrpg-devtest/ENABLE 파일 또는 -D{}=true)", SYSTEM_PROPERTY);
            return;
        }
        if (!ModList.get().isLoaded("geckolib")) {
            enabled = false;
            FantasyRpgCore.LOGGER.warn("[개발 테스트] GeckoLib 이 없어 테스트 장비를 켜지 않음");
            return;
        }
        manifest = loadManifest(dir.resolve("armor_manifest.json"));
        DevTestRegistry.register(modBus, manifest);
        NeoForge.EVENT_BUS.addListener(DevTestCommands::onRegisterCommands);
        FantasyRpgCore.LOGGER.info("[개발 테스트] 테스트 장비 {}개 등록 (목록: {}), 오류 {}건",
                manifest.entries().size(), source, manifest.errors().size());
        for (String e : manifest.errors()) FantasyRpgCore.LOGGER.warn("[개발 테스트] 목록 오류: {}", e);
    }

    private static DevTestManifest loadManifest(Path override) {
        try {
            if (Files.isRegularFile(override)) {
                source = "config/fantasyrpg-devtest/armor_manifest.json";
                return DevTestManifest.parse(Files.readString(override, StandardCharsets.UTF_8));
            }
            try (InputStream in = DevTest.class.getResourceAsStream("/fantasyrpg_devtest/armor_manifest.json")) {
                if (in == null) {
                    source = "없음";
                    return DevTestManifest.empty();
                }
                source = "모드 기본";
                return DevTestManifest.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            source = "읽기 실패";
            FantasyRpgCore.LOGGER.error("[개발 테스트] 목록 읽기 실패", e);
            return DevTestManifest.empty();
        }
    }
}
