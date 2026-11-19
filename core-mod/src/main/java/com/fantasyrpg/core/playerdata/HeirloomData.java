package com.fantasyrpg.core.playerdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 유산 무기의 진짜 상태 (플레이어 데이터에 저장, 아이템은 겉모습). 기획 v0.4 6.1, claude/유산무기_초안.md 5절.
 *
 * @param owned 유산 무기를 받았는지
 * @param tier 실제 등급 1~10 (재료로 올림)
 * @param awakened 1장 납골당에서 깨어났는지
 * @param traits 각성 특성: 방향 id → 고른 횟수
 * @param serial 일련번호. 다시 부르거나 모양이 바뀌면 +1 되고, 번호가 다른 옛 아이템은 효과 없음
 */
public record HeirloomData(boolean owned, int tier, boolean awakened, Map<String, Integer> traits, int serial) {
    public static final HeirloomData NONE = new HeirloomData(false, 1, false, Map.of(), 0);

    public static final Codec<HeirloomData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("owned", false).forGetter(HeirloomData::owned),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(HeirloomData::tier),
            Codec.BOOL.optionalFieldOf("awakened", false).forGetter(HeirloomData::awakened),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("traits", Map.of()).forGetter(HeirloomData::traits),
            Codec.INT.optionalFieldOf("serial", 0).forGetter(HeirloomData::serial)
    ).apply(i, HeirloomData::new));

    public HeirloomData {
        tier = Math.max(1, Math.min(10, tier));
        traits = Map.copyOf(traits);
    }

    public HeirloomData withTier(int t) {
        return new HeirloomData(owned, t, awakened, traits, serial);
    }

    public HeirloomData awaken() {
        return new HeirloomData(owned, tier, true, traits, serial);
    }

    public HeirloomData nextSerial() {
        return new HeirloomData(owned, tier, awakened, traits, serial + 1);
    }

    public HeirloomData pick(String direction) {
        Map<String, Integer> m = new HashMap<>(traits);
        m.merge(direction, 1, Integer::sum);
        return new HeirloomData(owned, tier, awakened, m, serial);
    }

    public HeirloomData clearTraits() {
        return new HeirloomData(owned, tier, awakened, Map.of(), serial);
    }

    /** 처음 받음: 1급, 잠든 상태 */
    public static HeirloomData fresh(int serial) {
        return new HeirloomData(true, 1, false, Map.of(), serial);
    }
}
