package com.fantasyrpg.core.playerdata;

import com.fantasyrpg.core.network.RpgNetwork;
import io.github.aswq0258.magiccity.api.MagicCityApi;
import io.github.aswq0258.magiccity.api.NpcActionEvent;
import io.github.aswq0258.magiccity.api.QuestStepStartEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 서장 NPC 직업 선택 (프로젝트 문서 claude/서장NPC_직업선택_초안.md 2·4절, A안 승인 2026-11-19).
 * <ol>
 *   <li>magiccity 가 NPC 선택지 {@value #ACTION}(NPC 표식·6칸·퀘스트 단계 확인 뒤)를 {@link NpcActionEvent} 로 알림</li>
 *   <li>무직이면 직업 선택 화면을 열고, 서버는 "화면을 열어 준 플레이어"를 {@value #OPEN_TICKS}틱 동안 기억</li>
 *   <li>클라이언트가 고른 직업을 보내면 기억이 있고 무직일 때만 선택 + 유산 무기(1급, 잠든 상태) 지급</li>
 *   <li>직업이 정해지면 magiccity 퀘스트 신호 {@value #SIGNAL} (명령으로 고른 경우도)</li>
 * </ol>
 * 모드 사이 방향은 fantasyrpg → magiccity 한 방향만 (magiccity 는 fantasyrpg 를 모름).
 */
public final class ClassSelection {
    private ClassSelection() {}

    public static final String ACTION = "choose_class";
    public static final String SIGNAL = "fantasyrpg:class_chosen";
    /** 화면을 열어 준 뒤 선택을 받는 시간 (5분) */
    public static final int OPEN_TICKS = 6000;

    private static final Map<UUID, Integer> OPEN = new HashMap<>();

    /** 시종장 [유산을 다시 부른다] */
    public static final String RECALL = "recall_heirloom";

    public static void onNpcAction(NpcActionEvent event) {
        if (RECALL.equals(event.getAction())) {
            com.fantasyrpg.core.gear.heirloom.HeirloomService.recall(event.getPlayer());
            return;
        }
        if (!ACTION.equals(event.getAction())) return;
        ServerPlayer p = event.getPlayer();
        if (RpgProgress.get(p).classOrNull() != null) {
            p.sendSystemMessage(Component.literal("§7[성장] 이미 직업이 정해져 있습니다."));
            notifyChosen(p);
            return;
        }
        OPEN.put(p.getUUID(), p.server.getTickCount() + OPEN_TICKS);
        PacketDistributor.sendToPlayer(p, new RpgNetwork.OpenClassSelect());
    }

    /** 직업 신호를 기다리는 단계가 시작(또는 접속)됐는데 이미 직업이 있으면 바로 넘김 */
    public static void onQuestStepStart(QuestStepStartEvent event) {
        if (SIGNAL.equals(event.getSignal()) && RpgProgress.get(event.getPlayer()).classOrNull() != null) {
            MagicCityApi.questSignal(event.getPlayer(), SIGNAL);
        }
    }

    /** 클라이언트 선택 (패킷) */
    public static void handleChoose(ServerPlayer p, String classId) {
        Integer until = OPEN.remove(p.getUUID());
        if (until == null || p.server.getTickCount() > until) {
            p.sendSystemMessage(Component.literal("§c[성장] 왕실 시종장과 다시 이야기해 직업 선택을 시작하세요."));
            return;
        }
        RpgClass c = RpgClass.byId(classId);
        if (c == null) return;
        RpgProgress.ClassResult r = RpgProgress.chooseClass(p, c);
        if (r != RpgProgress.ClassResult.CHOSEN) {
            p.sendSystemMessage(Component.literal("§7[성장] 이미 직업이 정해져 있습니다."));
            notifyChosen(p);
            return;
        }
        // 유산 무기 (기획 6.1, 2026-11-19): 서장에서 받는 낡은 직업 무기가 초대 왕의 유산
        ItemStack weapon = com.fantasyrpg.core.gear.heirloom.HeirloomService.grant(p);
        p.sendSystemMessage(Component.literal("§6[성장] §f직업 선택: ")
                .append(Component.translatable("class.fantasyrpg." + c.id()))
                .append(Component.literal(" §7(시작 SP +1)"))
                .append(Component.literal(" §8K 키: 스킬트리")));
        if (!weapon.isEmpty()) p.sendSystemMessage(Component.translatable("message.fantasyrpg.heirloom.granted", weapon.getHoverName())
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        notifyChosen(p);
    }

    /** 직업이 정해졌음을 magiccity 퀘스트에 알림 (기다리는 단계가 없으면 아무 일 없음) */
    public static void notifyChosen(ServerPlayer p) {
        MagicCityApi.questSignal(p, SIGNAL);
    }

    public static void forget(ServerPlayer p) {
        OPEN.remove(p.getUUID());
    }
}
