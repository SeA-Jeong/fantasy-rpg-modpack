package com.fantasyrpg.core.playerdata;

import com.fantasyrpg.core.FantasyRpgCore;
import io.github.aswq0258.magiccity.api.ChapterFirstClearEvent;
import io.github.aswq0258.magiccity.api.MagicCityApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 플레이어 성장(직업·레벨·경험치·SP)의 서버 쪽 처리. 기준: claude/플레이어성장_설계.md, 연동작업지침 4~8절.
 *
 * <ul>
 *   <li>레벨 상한 = min(60, {@link MagicCityApi#levelCap})</li>
 *   <li>완료 보너스 SP는 {@link #reconcileClearBonus} 한 함수로 맞춘다(챕터 첫 완료 이벤트·접속 때). 회수하지 않음</li>
 *   <li>경험치: 적대 몬스터 처치(마지막 일격), 챕터 최초 완료(등급 × 값), 퀘스트({@link #grantXp})</li>
 * </ul>
 */
public final class RpgProgress {
    private RpgProgress() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FantasyRpgCore.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RpgPlayerData>> PLAYER =
            ATTACHMENTS.register("player", () -> AttachmentType.builder(() -> RpgPlayerData.DEFAULT)
                    .serialize(RpgPlayerData.CODEC)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static RpgPlayerData get(ServerPlayer p) {
        return p.getData(PLAYER);
    }

    private static void set(ServerPlayer p, RpgPlayerData d) {
        p.setData(PLAYER, d);
    }

    private static ProgressionSettings settings() {
        return ProgressionManager.INSTANCE.settings();
    }

    public static int levelCap(ServerPlayer p) {
        return ProgressionMath.effectiveCap(MagicCityApi.levelCap(p), settings());
    }

    /** 경험치 지급 (퀘스트 보상 등에서도 이 함수를 쓴다). 레벨업하면 알림 */
    public static void grantXp(ServerPlayer p, long amount, String reason) {
        if (amount <= 0) return;
        apply(p, amount, reason);
    }

    /** 상한이 오른 뒤 보관 경험치 적용 */
    public static void applyHeldXp(ServerPlayer p) {
        apply(p, 0, null);
    }

    private static void apply(ServerPlayer p, long amount, String reason) {
        RpgPlayerData d = get(p);
        int cap = levelCap(p);
        ProgressionMath.Gain g = ProgressionMath.addXp(new ProgressionMath.LevelState(d.level(), d.xp()), amount, cap, settings());
        if (g.state().level() == d.level() && g.state().xp() == d.xp()) {
            if (amount > 0 && d.level() >= cap && d.level() < settings().maxLevel()) {
                p.displayClientMessage(Component.translatable("message.fantasyrpg.xp_capped", cap)
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        set(p, d.withLevel(g.state().level(), g.state().xp()));
        if (g.levelsGained() > 0) {
            p.sendSystemMessage(Component.translatable("message.fantasyrpg.level_up", g.state().level(), g.levelsGained())
                    .withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.0F);
        } else if (amount > 0 && reason != null) {
            p.displayClientMessage(Component.translatable("message.fantasyrpg.xp_gain", amount,
                    g.state().xp(), settings().need(g.state().level())).withStyle(ChatFormatting.GREEN), true);
        }
    }

    /** 완료 보너스 SP 맞추기 (멱등). 줄어도 회수하지 않음 */
    public static void reconcileClearBonus(ServerPlayer p) {
        RpgPlayerData d = get(p);
        int target = ProgressionMath.bonusTarget(MagicCityApi.completed(p));
        int delta = target - d.clearBonusGranted();
        if (delta <= 0) return;
        set(p, d.withClearBonus(target));
        p.sendSystemMessage(Component.translatable("message.fantasyrpg.clear_bonus_sp", delta).withStyle(ChatFormatting.AQUA));
    }

    public enum ClassResult { CHOSEN, CHANGED, SAME, ALREADY_CHOSEN }

    /** 직업 선택(무직일 때만). 시작 SP 1 지급 */
    public static ClassResult chooseClass(ServerPlayer p, RpgClass c) {
        RpgPlayerData d = get(p);
        if (d.classOrNull() != null) return d.classOrNull() == c ? ClassResult.SAME : ClassResult.ALREADY_CHOSEN;
        set(p, d.withClass(c, false));
        return ClassResult.CHOSEN;
    }

    /** 직업 변경: 레벨·경험치 유지, 쓴 SP 전부 반환. 비용은 [미정](지금은 시험 명령에서만 무료로 부름) */
    public static ClassResult changeClass(ServerPlayer p, RpgClass c) {
        RpgPlayerData d = get(p);
        if (d.classOrNull() == null) {
            set(p, d.withClass(c, false));
            return ClassResult.CHOSEN;
        }
        if (d.classOrNull() == c) return ClassResult.SAME;
        set(p, d.withClass(c, true));
        return ClassResult.CHANGED;
    }

    public static void reset(ServerPlayer p) {
        set(p, RpgPlayerData.DEFAULT);
    }

    // ── 이벤트 ──

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (dead instanceof ServerPlayer) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
        int xp = settings().killXp(id, dead instanceof Enemy);
        if (xp > 0) grantXp(p, xp, "kill");
    }

    public static void onChapterFirstClear(ChapterFirstClearEvent event) {
        ServerPlayer p = event.getPlayer();
        reconcileClearBonus(p);
        // 상한이 먼저 열린 뒤 경험치를 주므로 보관 경험치와 함께 레벨업된다
        grantXp(p, ProgressionMath.chapterClearXp(event.getGrade(), settings()), "chapter");
        if (event.levelCapChanged()) applyHeldXp(p);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            reconcileClearBonus(p);
            applyHeldXp(p);
        }
    }
}
