package com.fantasyrpg.core.gear.rune;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * 갑옷에 끼운 룬석 목록 (데이터 컴포넌트 {@code fantasyrpg:sockets}). 끼운 순서대로 저장한다.
 * 인챈트와 따로 저장하므로 인챈트 테이블·숫돌·모루가 룬석을 건드리지 않는다.
 * 칸 수는 아이템이 아니라 수치표(armor_stats의 sockets)와 갑옷 등급으로 정한다.
 */
public record RuneSockets(List<Entry> runes) {
    public static final RuneSockets EMPTY = new RuneSockets(List.of());

    /** 끼운 룬석 하나: 종류 id(life 등)와 등급(1~4) */
    public record Entry(String rune, int grade) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("rune").forGetter(Entry::rune),
                Codec.intRange(RuneDefinition.MIN_GRADE, RuneDefinition.MAX_GRADE).fieldOf("grade").forGetter(Entry::grade)
        ).apply(i, Entry::new));
    }

    public static final Codec<RuneSockets> CODEC = Entry.CODEC.listOf().xmap(RuneSockets::new, RuneSockets::runes);
    public static final StreamCodec<ByteBuf, RuneSockets> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public RuneSockets {
        runes = List.copyOf(runes);
    }

    public boolean isEmpty() {
        return runes.isEmpty();
    }

    /** 순수 규칙용 목록 (모르는 종류는 빼고) */
    public List<RuneRules.Socketed> toRules() {
        List<RuneRules.Socketed> out = new ArrayList<>();
        for (Entry e : runes) {
            RuneType t = RuneType.byId(e.rune());
            if (t != null) out.add(new RuneRules.Socketed(t, e.grade()));
        }
        return out;
    }

    public static RuneSockets fromRules(List<RuneRules.Socketed> list) {
        List<Entry> out = new ArrayList<>();
        for (RuneRules.Socketed s : list) out.add(new Entry(s.type().id(), s.grade()));
        return new RuneSockets(out);
    }
}
