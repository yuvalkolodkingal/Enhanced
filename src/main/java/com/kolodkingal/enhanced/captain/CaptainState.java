package com.kolodkingal.enhanced.captain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Captain America's state (3.8): the attachment {@code ModAttachments.CAPTAIN} (saved, not kept on death, synced to
 * everyone who sees him since lane L1: his HUD's shield and combo, and the shield drawn on his back). Immutable: every
 * change makes a new record. Written by the 3.8 skeleton; lane L1 drives it.
 *
 * @param shieldEntity the thrown shield's entity id while it flies, or -1
 * @param comboStep    the Combat Combo's next blow (0, 1 or 2)
 * @param comboAt      the game time of the combo's last blow
 * @param flags        lane L1's bits: {@link #BACK}, {@link #OFF_HAND}, {@link #RICOCHET}, and the ricochets the flying
 *                     shield has left ({@link #bouncesLeft})
 */
public record CaptainState(int shieldEntity, int comboStep, long comboAt, int flags) {
    public static final CaptainState EMPTY = new CaptainState(-1, 0, 0L, 0);
    /** He carries a Vibranium Shield he does not hold (and none flies): it is drawn on his back. */
    public static final int BACK = 1;
    /** The flying shield left his off hand (it comes back to it). */
    public static final int OFF_HAND = 2;
    /** The flying shield was thrown to ricochet. */
    public static final int RICOCHET = 4;
    /** The ricochets the flying shield has left, in bits 8-11. */
    private static final int BOUNCES_SHIFT = 8;
    private static final int BOUNCES_MASK = 0xF << BOUNCES_SHIFT;

    public static final Codec<CaptainState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("shield_entity", -1).forGetter(CaptainState::shieldEntity),
            Codec.INT.optionalFieldOf("combo_step", 0).forGetter(CaptainState::comboStep),
            Codec.LONG.optionalFieldOf("combo_at", 0L).forGetter(CaptainState::comboAt),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(CaptainState::flags)
    ).apply(i, CaptainState::new));

    public static final StreamCodec<ByteBuf, CaptainState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CaptainState::shieldEntity,
            ByteBufCodecs.VAR_INT, CaptainState::comboStep,
            ByteBufCodecs.VAR_LONG, CaptainState::comboAt,
            ByteBufCodecs.VAR_INT, CaptainState::flags,
            CaptainState::new);

    public CaptainState withShield(int id) {
        return new CaptainState(id, comboStep, comboAt, flags);
    }

    public CaptainState withCombo(int step, long at) {
        return new CaptainState(shieldEntity, step, at, flags);
    }

    public CaptainState withFlags(int value) {
        return new CaptainState(shieldEntity, comboStep, comboAt, value);
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public CaptainState with(int flag, boolean on) {
        return withFlags(on ? flags | flag : flags & ~flag);
    }

    /** The ricochets the flying shield has left (0-15). */
    public int bouncesLeft() {
        return (flags & BOUNCES_MASK) >>> BOUNCES_SHIFT;
    }

    public CaptainState withBouncesLeft(int bounces) {
        return withFlags((flags & ~BOUNCES_MASK) | (Math.max(0, Math.min(15, bounces)) << BOUNCES_SHIFT));
    }
}
