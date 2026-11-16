package com.fantasyrpg.core;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
        // TODO: playerdata 첨부 데이터, skilltree 데이터 로더, combat 이벤트 등록
    }
}
