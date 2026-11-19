package com.fantasyrpg.core.gear.heirloom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** 아이템 컴포넌트 {@code fantasyrpg:heirloom}: 이 직업 무기가 누구의 몇 번째 유산 무기인지 */
public record HeirloomTag(UUID owner, int serial) {
    public static final Codec<HeirloomTag> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(HeirloomTag::owner),
            Codec.INT.fieldOf("serial").forGetter(HeirloomTag::serial)
    ).apply(i, HeirloomTag::new));

    public static final StreamCodec<ByteBuf, HeirloomTag> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, HeirloomTag::owner,
            ByteBufCodecs.VAR_INT, HeirloomTag::serial,
            HeirloomTag::new);
}
