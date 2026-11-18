package com.fantasyrpg.core;

import com.fantasyrpg.core.command.RpgCommands;
import com.fantasyrpg.core.gear.armor.ArmorStatManager;
import com.fantasyrpg.core.gear.armor.FamilyArmorAttributes;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.fantasyrpg.core.gear.rune.RuneEvents;
import com.fantasyrpg.core.gear.rune.RuneRegistry;
import com.fantasyrpg.core.gear.rune.RuneStatManager;
import com.fantasyrpg.core.playerdata.ProgressionManager;
import com.fantasyrpg.core.playerdata.RpgProgress;
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

        // 계열 방어구: 등급 컴포넌트·재질·아이템·창작 탭 등록, 수치표로 능력치 붙이기
        GearRegistry.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(FamilyArmorAttributes::onItemAttributes);
        // 룬석: 아이템·소켓 컴포넌트·제작대 조합 등록, 모루 보호, 툴팁(클라이언트에서만 불림)
        RuneRegistry.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RuneEvents::onAnvil);
        NeoForge.EVENT_BUS.addListener(RuneEvents::onTooltip);

        // 플레이어 성장: 데이터 첨부(직업·레벨·경험치·SP), 몬스터 처치·챕터 첫 완료 경험치, 접속 때 보너스 SP 맞추기
        RpgProgress.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onChapterFirstClear);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onLogin);
        // TODO: skilltree 데이터 로더 등록
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MobResistanceManager.INSTANCE);
        event.addListener(ArmorStatManager.INSTANCE);
        event.addListener(RuneStatManager.INSTANCE);
        event.addListener(ProgressionManager.INSTANCE);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        RpgCommands.register(event.getDispatcher());
    }
}
