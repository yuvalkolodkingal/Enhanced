package com.kolodkingal.enhanced.healing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A healer's state (3.7): the attachment {@code ModAttachments.HEALING} (saved, not kept on death, synced to everyone
 * who sees him: only {@link #downUntil} is visual, and {@code Live.HEALING_DOWN} is the bit every client acts).
 * Immutable: every change makes a new record. Lane L1 drives it; a player who logs in while down rises at once (the
 * timer is not trusted across a logout).
 *
 * @param downUntil the game time he rises at (0: not down)
 * @param lastHarm  the game time he last took damage (the faster regeneration waits {@code outOfCombatTicks} after it)
 * @param flags     append-only bits: {@link #TIP_SHOWN}, {@link #WAS_DOWN_IN_HAZARD}
 */
public record HealingState(long downUntil, long lastHarm, int flags) {
    /** The one-time tip of his first DOWN was shown. */
    public static final int TIP_SHOWN = 1;
    /** He went down in a hazard (a fluid without air, lava, fire, powder snow, a wall, the bottom of the world). */
    public static final int WAS_DOWN_IN_HAZARD = 1 << 1;

    public static final HealingState EMPTY = new HealingState(0L, 0L, 0);

    public static final Codec<HealingState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("down_until", 0L).forGetter(HealingState::downUntil),
            Codec.LONG.optionalFieldOf("last_harm", 0L).forGetter(HealingState::lastHarm),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(HealingState::flags)
    ).apply(i, HealingState::new));

    public static final StreamCodec<ByteBuf, HealingState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, HealingState::downUntil,
            ByteBufCodecs.VAR_LONG, HealingState::lastHarm,
            ByteBufCodecs.VAR_INT, HealingState::flags,
            HealingState::new);

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public HealingState withDownUntil(long time) {
        return new HealingState(time, lastHarm, flags);
    }

    public HealingState withLastHarm(long time) {
        return new HealingState(downUntil, time, flags);
    }

    public HealingState with(int flag, boolean on) {
        return new HealingState(downUntil, lastHarm, on ? flags | flag : flags & ~flag);
    }
}
