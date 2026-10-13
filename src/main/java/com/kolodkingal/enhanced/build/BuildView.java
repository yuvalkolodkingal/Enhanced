package com.kolodkingal.enhanced.build;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * What everyone who sees a builder knows of her Build Magic (attachment {@code BUILD_VIEW}): whether she is in build mode,
 * the spell she has chosen, and the box she is about to build in. Never saved (build mode is off after a login), synced
 * to the builder and to everyone tracking her, when it changes and at most 4 times a second. It is what makes the Scarlet
 * Witch's eyes glow and keeps Billy's crown on while they build, and what draws a family member's box (D-B 7.1); it takes
 * the place of a new {@code Live} bit, so {@code PowerData} is not changed.
 *
 * @param active whether she is in build mode
 * @param spell  the spell chosen on her wheel
 * @param box    the box she has marked, if any
 */
public record BuildView(boolean active, Spell spell, Optional<Box> box) {
    /** Not building, no box: every player before he first presses Y. */
    public static final BuildView EMPTY = new BuildView(false, Spell.FILL, Optional.empty());

    /** A box of blocks, both corners inside it. */
    public record Box(BlockPos min, BlockPos max) {
        public static final StreamCodec<ByteBuf, Box> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Box::min,
                BlockPos.STREAM_CODEC, Box::max,
                Box::new);

        /** The box between two opposite corners, in any order. */
        public Box {
            BlockPos low = new BlockPos(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
            BlockPos high = new BlockPos(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
            min = low;
            max = high;
        }
    }

    public static final StreamCodec<ByteBuf, BuildView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BuildView::active,
            ByteBufCodecs.VAR_INT.map(Spell::byId, Spell::id), BuildView::spell,
            ByteBufCodecs.optional(Box.STREAM_CODEC), BuildView::box,
            BuildView::new);

    /** The same view, in or out of build mode (out of it, the box is let go). */
    public BuildView withActive(boolean active) {
        return new BuildView(active, spell, active ? box : Optional.empty());
    }

    /** The same mode, with this spell and box (or none). */
    public BuildView withSpell(Spell spell, Optional<Box> box) {
        return new BuildView(active, spell, box);
    }
}
