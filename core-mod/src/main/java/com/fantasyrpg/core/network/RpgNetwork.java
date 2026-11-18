package com.fantasyrpg.core.network;

import com.fantasyrpg.core.FantasyRpgCore;
import com.fantasyrpg.core.client.ClientRpgState;
import com.fantasyrpg.core.skilltree.SkillService;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/**
 * 서버 ↔ 클라이언트 패킷. 클라이언트는 요청만 보내고 모든 판정은 서버(CLAUDE.md 코드 규칙).
 * <ul>
 *   <li>서버 → 클라: 성장·스킬 상태({@link State}), 노드 정의 원문({@link Defs}), 코어 값({@link Core})</li>
 *   <li>클라 → 서버: 스킬 요청({@link Action}: 투자·되돌리기·전체 초기화·장착·공통 패시브·사용)</li>
 * </ul>
 */
public final class RpgNetwork {
    private RpgNetwork() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FantasyRpgCore.MOD_ID, path);
    }

    public record State(CompoundTag tag) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(id("state"));
        public static final StreamCodec<ByteBuf, State> CODEC = ByteBufCodecs.COMPOUND_TAG.map(State::new, State::tag);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Defs(String classId, List<String> nodes, List<String> passives) implements CustomPacketPayload {
        public static final Type<Defs> TYPE = new Type<>(id("defs"));
        public static final StreamCodec<ByteBuf, Defs> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Defs::classId,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Defs::nodes,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Defs::passives,
                Defs::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Core(String coreId, float value, float max) implements CustomPacketPayload {
        public static final Type<Core> TYPE = new Type<>(id("core"));
        public static final StreamCodec<ByteBuf, Core> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Core::coreId,
                ByteBufCodecs.FLOAT, Core::value,
                ByteBufCodecs.FLOAT, Core::max,
                Core::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 클라 → 서버 요청. action: {@link SkillService.ActionType} 순서 번호 */
    public record Action(int action, String id, int slot) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id("action"));
        public static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::action,
                ByteBufCodecs.STRING_UTF8, Action::id,
                ByteBufCodecs.VAR_INT, Action::slot,
                Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, RpgNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(State.TYPE, State.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientRpgState.acceptState(p.tag())));
        r.playToClient(Defs.TYPE, Defs.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientRpgState.acceptDefs(p.classId(), p.nodes(), p.passives())));
        r.playToClient(Core.TYPE, Core.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientRpgState.acceptCore(p.coreId(), p.value(), p.max())));
        r.playToServer(Action.TYPE, Action.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) SkillService.handle(sp, p.action(), p.id(), p.slot());
        }));
    }
}
