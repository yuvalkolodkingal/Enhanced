package com.kolodkingal.enhanced.asgard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A player's progress in Asgard (3.5): the attachment {@code ModAttachments.ASGARD} (saved, kept on death, synced to
 * that player only). Immutable: every change makes a new record ({@link #with}, {@link #without}, the {@code with...}
 * setters), which the caller puts back with {@code player.setData}.
 *
 * @param flags        the bits of the flags below
 * @param cameFrom     where the player last entered Asgard from (a rune stone, or the place the Bifrost took him from),
 * or null: where Heimdall (NPC) can send him back
 * @param hofundBond   the bond of his newest Hofund ({@code ModDataComponents.BOND}): an older copy crumbles
 * @param gungnirBond  the bond of his newest Gungnir
 * @param watchReadyAt the game time from which he may take the Guardian's Watch again (after a loss)
 */
public record AsgardProgress(int flags, @Nullable GlobalPos cameFrom, int hofundBond, int gungnirBond, long watchReadyAt) {
    // The flags. Append only (the bit is saved): at most 31.
    /** He has been to Asgard. */
    public static final int VISITED = 1;
    /** He has talked to Heimdall in the Observatory. */
    public static final int MET_HEIMDALL = 1 << 1;
    /** He won the Guardian's Watch (and was given Hofund). */
    public static final int WATCH_WON = 1 << 2;
    /** He was rewarded for beating Hela (the right to Gungnir). */
    public static final int HELA_VICTOR = 1 << 3;
    /** He has taken Gungnir from its stand. */
    public static final int GUNGNIR_TAKEN = 1 << 4;
    /** He was crowned on the Throne of Asgard (he became Odin). */
    public static final int CROWNED = 1 << 5;
    /** He has talked to Frigga. */
    public static final int TALKED_FRIGGA = 1 << 6;
    /** He has talked to Sif. */
    public static final int TALKED_SIF = 1 << 7;
    /** He has talked to Valkyrie. */
    public static final int TALKED_VALKYRIE = 1 << 8;
    /** He has talked to the Warriors Three. */
    public static final int TALKED_WARRIORS = 1 << 9;
    /** How many flags there are (the bits above are 0 to {@code FLAG_COUNT - 1}). */
    public static final int FLAG_COUNT = 10;

    public static final AsgardProgress EMPTY = new AsgardProgress(0, null, 0, 0, 0L);

    public static final Codec<AsgardProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("flags", 0).forGetter(AsgardProgress::flags),
            GlobalPos.CODEC.optionalFieldOf("came_from").forGetter(p -> Optional.ofNullable(p.cameFrom())),
            Codec.INT.optionalFieldOf("hofund_bond", 0).forGetter(AsgardProgress::hofundBond),
            Codec.INT.optionalFieldOf("gungnir_bond", 0).forGetter(AsgardProgress::gungnirBond),
            Codec.LONG.optionalFieldOf("watch_ready_at", 0L).forGetter(AsgardProgress::watchReadyAt)
    ).apply(i, (flags, from, hofund, gungnir, watch) -> new AsgardProgress(flags, from.orElse(null), hofund, gungnir, watch)));

    public static final StreamCodec<ByteBuf, AsgardProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AsgardProgress::flags,
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), p -> Optional.ofNullable(p.cameFrom()),
            ByteBufCodecs.VAR_INT, AsgardProgress::hofundBond,
            ByteBufCodecs.VAR_INT, AsgardProgress::gungnirBond,
            ByteBufCodecs.VAR_LONG, AsgardProgress::watchReadyAt,
            (flags, from, hofund, gungnir, watch) -> new AsgardProgress(flags, from.orElse(null), hofund, gungnir, watch));

    /** Whether every bit of {@code flag} is set. */
    public boolean has(int flag) {
        return (flags & flag) == flag;
    }

    public AsgardProgress with(int flag) {
        return new AsgardProgress(flags | flag, cameFrom, hofundBond, gungnirBond, watchReadyAt);
    }

    public AsgardProgress without(int flag) {
        return new AsgardProgress(flags & ~flag, cameFrom, hofundBond, gungnirBond, watchReadyAt);
    }

    public AsgardProgress withCameFrom(@Nullable GlobalPos from) {
        return new AsgardProgress(flags, from, hofundBond, gungnirBond, watchReadyAt);
    }

    public AsgardProgress withHofundBond(int bond) {
        return new AsgardProgress(flags, cameFrom, bond, gungnirBond, watchReadyAt);
    }

    public AsgardProgress withGungnirBond(int bond) {
        return new AsgardProgress(flags, cameFrom, hofundBond, bond, watchReadyAt);
    }

    public AsgardProgress withWatchReadyAt(long time) {
        return new AsgardProgress(flags, cameFrom, hofundBond, gungnirBond, time);
    }
}
