package com.fantasyrpg.core;

import com.fantasyrpg.core.command.RpgCommands;
import com.fantasyrpg.core.gear.vanilla.VanillaArmorTuning;
import com.fantasyrpg.core.resist.MobResistanceManager;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

/**
 * 판타지 RPG 코어 모드 진입점.
 * 각 하위 패키지의 등록(레지스트리, 이벤트, 데이터 로더)은 여기서 연결한다.
 */
@Mod(FantasyRpgCore.MOD_ID)
public final class FantasyRpgCore {
    public static final String MOD_ID = "fantasyrpg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FantasyRpgCore(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Fantasy RPG Core 로딩");
        modContainer.registerConfig(ModConfig.Type.COMMON, RpgConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(FantasyRpgCore::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(FantasyRpgCore::onRegisterCommands);

        // 바닐라 다이아·네더라이트 갑옷 능력치 조정 (아이템은 그대로)
        NeoForge.EVENT_BUS.addListener(VanillaArmorTuning::onItemAttributes);
        // TODO: playerdata 첨부 데이터, skilltree 데이터 로더 등록
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MobResistanceManager.INSTANCE);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        RpgCommands.register(event.getDispatcher());
    }
}
