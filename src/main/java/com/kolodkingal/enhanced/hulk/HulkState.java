package com.kolodkingal.enhanced.hulk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The Hulk's state (3.6): the attachment {@code ModAttachments.HULK} (saved, kept on death, synced to that player
 * only). Immutable: every change makes a new record. Lane L7 drives it (rage and anger are zero on death).
 *
 * @param rage           the Rage meter, 0-100 (a Hulk)
 * @param anger          Bruce Banner's anger, 0-100 (at 100 he Hulks out by himself)
 * @param lastHulk       the last Hulk look ({@code ragnarok} or {@code brand_new_day}): Banner Hulks out into it
 * @param armBurnedUntil the game time until which his arm is burned (after his Snap)
 * @param flags          append-only bits (lane L7's)
 */
public record HulkState(int rage, int anger, String lastHulk, long armBurnedUntil, int flags) {
    /** The look a first awakening (and an empty state) Hulks out into. */
    public static final String FIRST_HULK = "ragnarok";

    public static final HulkState EMPTY = new HulkState(0, 0, FIRST_HULK, 0L, 0);

    public static final Codec<HulkState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("rage", 0).forGetter(HulkState::rage),
            Codec.INT.optionalFieldOf("anger", 0).forGetter(HulkState::anger),
            Codec.STRING.optionalFieldOf("last_hulk", FIRST_HULK).forGetter(HulkState::lastHulk),
            Codec.LONG.optionalFieldOf("arm_burned_until", 0L).forGetter(HulkState::armBurnedUntil),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(HulkState::flags)
    ).apply(i, HulkState::new));

    public static final StreamCodec<ByteBuf, HulkState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HulkState::rage,
            ByteBufCodecs.VAR_INT, HulkState::anger,
            ByteBufCodecs.stringUtf8(32), HulkState::lastHulk,
            ByteBufCodecs.VAR_LONG, HulkState::armBurnedUntil,
            ByteBufCodecs.VAR_INT, HulkState::flags,
            HulkState::new);

    public HulkState withRage(int value) {
        return new HulkState(Math.max(0, Math.min(100, value)), anger, lastHulk, armBurnedUntil, flags);
    }

    public HulkState withAnger(int value) {
        return new HulkState(rage, Math.max(0, Math.min(100, value)), lastHulk, armBurnedUntil, flags);
    }

    public HulkState withLastHulk(String look) {
        return new HulkState(rage, anger, look, armBurnedUntil, flags);
    }

    public HulkState withArmBurnedUntil(long time) {
        return new HulkState(rage, anger, lastHulk, time, flags);
    }

    public HulkState withFlags(int value) {
        return new HulkState(rage, anger, lastHulk, armBurnedUntil, value);
    }
}
