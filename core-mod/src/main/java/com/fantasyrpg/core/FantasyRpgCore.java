package com.fantasyrpg.core;

import com.fantasyrpg.core.classcore.CoreService;
import com.fantasyrpg.core.combat.CombatEvents;
import com.fantasyrpg.core.combat.CombatManager;
import com.fantasyrpg.core.command.RpgCommands;
import com.fantasyrpg.core.gear.weapon.WeaponRegistry;
import com.fantasyrpg.core.gear.armor.ArmorStatManager;
import com.fantasyrpg.core.gear.armor.FamilyArmorAttributes;
import com.fantasyrpg.core.gear.armor.GearRegistry;
import com.fantasyrpg.core.gear.rune.RuneEvents;
import com.fantasyrpg.core.gear.rune.RuneRegistry;
import com.fantasyrpg.core.gear.rune.RuneStatManager;
import com.fantasyrpg.core.playerdata.ProgressionManager;
import com.fantasyrpg.core.playerdata.RpgProgress;
import com.fantasyrpg.core.network.RpgNetwork;
import com.fantasyrpg.core.resist.MobResistanceManager;
import com.fantasyrpg.core.skilltree.SkillData;
import com.fantasyrpg.core.skilltree.SkillService;
import com.fantasyrpg.core.skills.ArcherSkills;
import com.fantasyrpg.core.skills.ClericSkills;
import com.fantasyrpg.core.skills.KnightSkills;
import com.fantasyrpg.core.skills.MageSkills;
import com.fantasyrpg.core.skills.RogueSkills;
import com.fantasyrpg.core.skills.SkillEvents;
import com.fantasyrpg.core.skills.SkillRuntime;
import com.fantasyrpg.core.skills.SkillSettings;
import com.fantasyrpg.core.skills.UltimateSkills;
import com.fantasyrpg.core.skills.WarriorSkills;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
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
        com.fantasyrpg.core.craft.CraftRegistry.register(modEventBus);   // 확장 작업대·부품·소재 (2026-11-20)
        NeoForge.EVENT_BUS.addListener(FamilyArmorAttributes::onItemAttributes);
        // 룬석: 아이템·소켓 컴포넌트·제작대 조합 등록, 모루 보호, 툴팁(클라이언트에서만 불림)
        RuneRegistry.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RuneEvents::onAnvil);
        NeoForge.EVENT_BUS.addListener(RuneEvents::onTooltip);
        // 바닐라 전투 인챈트 정리 (정책 P2, 2026-11-21)
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.gear.enchant.CombatEnchants::onLogin);
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.gear.enchant.CombatEnchants::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.gear.enchant.CombatEnchants::onServerStarted);

        // 플레이어 성장: 데이터 첨부(직업·레벨·경험치·SP), 몬스터 처치·챕터 첫 완료 경험치, 접속 때 보너스 SP 맞추기
        RpgProgress.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onChapterFirstClear);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onQuestStepComplete);
        // 유산 무기: 각성, 죽어도 떨어뜨리지 않음
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.gear.heirloom.HeirloomService::onChapterFirstClear);
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.gear.heirloom.HeirloomService::onDrops);
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.gear.heirloom.HeirloomService::onClone);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onLogin);
        // 실제 전투: 직업 무기 6종, 근접·활·지팡이 기본 공격 → RPG 피해, 다른 원거리 무기 막기, PvP 차단
        WeaponRegistry.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(WeaponRegistry::onItemAttributes);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onAttack);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onUseStart);
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.combat.GuardEvents::onUseStart);   // 기사 기본 가드·바닐라 방패 금지
        NeoForge.EVENT_BUS.addListener(CombatEvents::onIncomingDamage);
        // 스킬트리 틀: 노드·공통 패시브·코어 데이터, 서버↔클라 패킷, 코어 회복·감소, /reload 뒤 다시 보내기
        RpgNetwork.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onRespawn);
        NeoForge.EVENT_BUS.addListener(RpgProgress::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(CoreService::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(FantasyRpgCore::onLogout);
        // 서장 NPC 직업 선택 (magiccity 퀘스트 NPC 선택지·단계 신호)
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.playerdata.ClassSelection::onNpcAction);
        NeoForge.EVENT_BUS.addListener(com.fantasyrpg.core.playerdata.ClassSelection::onQuestStepStart);
        NeoForge.EVENT_BUS.addListener(FantasyRpgCore::onDatapackSync);
        // 6직업 1~2단 스킬: 효과 등록, 받는 피해·틱·예약 실행
        WarriorSkills.register();
        MageSkills.register();
        KnightSkills.register();
        RogueSkills.register();
        ArcherSkills.register();
        ClericSkills.register();
        UltimateSkills.register();
        NeoForge.EVENT_BUS.addListener(SkillEvents::onKnockback);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SkillEvents::onDamagePost);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MobResistanceManager.INSTANCE);
        event.addListener(ArmorStatManager.INSTANCE);
        event.addListener(RuneStatManager.INSTANCE);
        event.addListener(ProgressionManager.INSTANCE);
        event.addListener(com.fantasyrpg.core.gear.heirloom.HeirloomManager.INSTANCE);
        event.addListener(CombatManager.INSTANCE);
        event.addListener(SkillData.NODES);
        event.addListener(SkillData.PASSIVES);
        event.addListener(SkillData.CORES);
        event.addListener(SkillSettings.INSTANCE);
        event.addListener(com.fantasyrpg.core.craft.CraftData.RECIPES);
        event.addListener(com.fantasyrpg.core.craft.CraftData.ARMOR);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            com.fantasyrpg.core.playerdata.ClassSelection.forget(p);
            CoreService.forget(p);
            SkillRuntime.forget(p);
        }
    }

    /** /reload 뒤(플레이어 null): 접속한 모두에게 정의·상태·패시브를 다시. 접속 때는 onLogin 이 처리 */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) event.getPlayerList().getPlayers().forEach(SkillService::refreshAll);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        RpgCommands.register(event.getDispatcher());
    }
}
