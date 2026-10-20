package com.kolodkingal.enhanced.captain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Captain America's state (3.8): the attachment {@code ModAttachments.CAPTAIN} (saved, not kept on death, synced to
 * him: his HUD's shield and combo). Immutable: every change makes a new record. Written by the 3.8 skeleton; lane L1
 * drives it.
 *
 * @param shieldEntity the thrown shield's entity id while it flies, or -1
 * @param comboStep    the Combat Combo's next blow (0, 1 or 2)
 * @param comboAt      the game time of the combo's last blow
 * @param flags        lane L1's bits
 */
public record CaptainState(int shieldEntity, int comboStep, long comboAt, int flags) {
    public static final CaptainState EMPTY = new CaptainState(-1, 0, 0L, 0);

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
}
