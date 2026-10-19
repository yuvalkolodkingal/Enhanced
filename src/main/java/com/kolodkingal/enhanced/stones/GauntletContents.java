package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * What an Infinity Gauntlet holds (3.6): the data component {@code ModDataComponents.GAUNTLET} ({@code
 * enhanced:gauntlet}). The item's default is {@link #TITAN}, so every Gauntlet already in a world, the boss's reward
 * and "Choose a hero → Thanos" are the Titan's Gauntlet, which behaves exactly as in 3.4 (it awakens Thanos and is used
 * up). A crafted Gauntlet starts {@link #EMPTY} and takes real stones (lane L1). Immutable.
 *
 * @param stones   a bit per {@link StoneKind} (by ordinal: {@link StoneProgress#bit}): the stones in its sockets
 * @param titan    whether it is the Titan's Gauntlet (his five stones, not items)
 * @param selected the ordinal of the selected stone (the one its use is), from the Stone Wheel
 */
public record GauntletContents(int stones, boolean titan, int selected) {
    /** The Titan's Gauntlet: the five stones Thanos gathered (Power to Time), not items; the item's default. */
    public static final GauntletContents TITAN = new GauntletContents(0b11111, true, 0);
    /** A crafted Gauntlet, its sockets empty. */
    public static final GauntletContents EMPTY = new GauntletContents(0, false, 0);

    public static final Codec<GauntletContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("stones", 0).forGetter(GauntletContents::stones),
            Codec.BOOL.optionalFieldOf("titan", false).forGetter(GauntletContents::titan),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(GauntletContents::selected)
    ).apply(i, GauntletContents::new));

    public static final StreamCodec<ByteBuf, GauntletContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GauntletContents::stones,
            ByteBufCodecs.BOOL, GauntletContents::titan,
            ByteBufCodecs.VAR_INT, GauntletContents::selected,
            GauntletContents::new);

    /** Whether the stone is in a socket. */
    public boolean has(StoneKind kind) {
        return (stones & StoneProgress.bit(kind)) != 0;
    }

    public GauntletContents with(StoneKind kind) {
        return new GauntletContents(stones | StoneProgress.bit(kind), titan, selected);
    }

    public GauntletContents without(StoneKind kind) {
        return new GauntletContents(stones & ~StoneProgress.bit(kind), titan, selected);
    }

    /** How many stones are in it. */
    public int count() {
        return Integer.bitCount(stones);
    }

    /** Whether all six are in it (the Snap). */
    public boolean full() {
        return count() == StoneKind.values().length;
    }

    /** The selected stone, or null when it is not in a socket. */
    @Nullable
    public StoneKind selectedStone() {
        StoneKind[] kinds = StoneKind.values();
        return selected >= 0 && selected < kinds.length && has(kinds[selected]) ? kinds[selected] : null;
    }

    public GauntletContents withSelected(StoneKind kind) {
        return new GauntletContents(stones, titan, kind.ordinal());
    }
}
