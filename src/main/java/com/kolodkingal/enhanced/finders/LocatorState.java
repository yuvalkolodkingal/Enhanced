package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;
import java.util.Optional;

/**
 * What a Stone Locator knows (3.7): the data component {@code enhanced:locator} ({@code ModDataComponents.LOCATOR}),
 * saved with the item and sent to clients (the needle and the HUD read it). The recipe writes {@link #tuned}; the
 * server (lane L5) writes the rest on the held stack every 20 ticks, only when it changed. Immutable.
 *
 * @param tuned    the stone it was tuned to (the core of its recipe): its colour and lens
 * @param filter   the one kind it looks for (Sneak + use cycles it), or empty: every kind the holder lacks
 * @param target   the site it points to, or empty (it spins)
 * @param status   what the HUD says (a constant of this record)
 * @param distance the distance to the target, rounded to 10 blocks
 * @param pointing the stone whose site (or way) it points to, or names in its status (lane L5: the HUD's icon and
 *                 name when the filter is "every stone")
 */
public record LocatorState(Optional<StoneKind> tuned, Optional<StoneKind> filter, Optional<GlobalPos> target,
                           byte status, int distance, Optional<StoneKind> pointing) {
    public static final byte SEARCHING = 0;
    public static final byte FOUND = 1;
    public static final byte OTHER_REALM = 2;
    public static final byte SOMEONE_ELSE = 3;
    public static final byte TOO_FAR = 4;
    public static final byte HAS_ALL = 5;
    public static final byte HIDDEN = 6;

    public static final LocatorState EMPTY = new LocatorState(Optional.empty(), Optional.empty(), Optional.empty(),
            SEARCHING, 0);

    /** The skeleton's five fields: points at no particular stone. */
    public LocatorState(Optional<StoneKind> tuned, Optional<StoneKind> filter, Optional<GlobalPos> target, byte status,
                        int distance) {
        this(tuned, filter, target, status, distance, Optional.empty());
    }

    /** A stone by its name ({@code time}). */
    public static final Codec<StoneKind> KIND = Codec.STRING.xmap(s -> StoneKind.valueOf(s.toUpperCase(Locale.ROOT)),
            k -> k.name().toLowerCase(Locale.ROOT));
    private static final StreamCodec<ByteBuf, StoneKind> KIND_STREAM = ByteBufCodecs.VAR_INT.map(
            i -> StoneKind.values()[Math.floorMod(i, StoneKind.values().length)], StoneKind::ordinal);

    public static final Codec<LocatorState> CODEC = RecordCodecBuilder.create(i -> i.group(
            KIND.optionalFieldOf("tuned").forGetter(LocatorState::tuned),
            KIND.optionalFieldOf("filter").forGetter(LocatorState::filter),
            GlobalPos.CODEC.optionalFieldOf("target").forGetter(LocatorState::target),
            Codec.BYTE.optionalFieldOf("status", SEARCHING).forGetter(LocatorState::status),
            Codec.INT.optionalFieldOf("distance", 0).forGetter(LocatorState::distance),
            KIND.optionalFieldOf("pointing").forGetter(LocatorState::pointing)
    ).apply(i, LocatorState::new));

    public static final StreamCodec<ByteBuf, LocatorState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(KIND_STREAM), LocatorState::tuned,
            ByteBufCodecs.optional(KIND_STREAM), LocatorState::filter,
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), LocatorState::target,
            ByteBufCodecs.BYTE, LocatorState::status,
            ByteBufCodecs.VAR_INT, LocatorState::distance,
            ByteBufCodecs.optional(KIND_STREAM), LocatorState::pointing,
            LocatorState::new);

    /** A Locator fresh from the recipe, tuned to {@code kind}. */
    public static LocatorState tunedTo(StoneKind kind) {
        return new LocatorState(Optional.of(kind), Optional.empty(), Optional.empty(), SEARCHING, 0);
    }

    /** The same Locator with another filter (the stone it looks for; empty: every stone its holder lacks). */
    public LocatorState withFilter(Optional<StoneKind> kind) {
        return new LocatorState(tuned, kind, target, status, distance, pointing);
    }

    /** The same Locator (tuned, filter) with what the search says now. */
    public LocatorState withReading(Optional<GlobalPos> to, byte said, int blocks, Optional<StoneKind> at) {
        return new LocatorState(tuned, filter, to, said, blocks, at);
    }
}
