package com.kolodkingal.enhanced.road;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * One trial of the Witches' Road. To add a trial: implement this interface, add it to {@link RoadChallenges} (the order
 * there is the order of the trials), add its options to {@code WitchesRoadConfig} and its text to the lang files
 * ({@code road.enhanced.challenge.<id>}), and draw its course with {@link RoadPalette}. Everything else is the framework's:
 * the cell, the chunk tickets, the saved progress, checkpoints, falls, death, the time limit, the gate to the next trial,
 * leaving, logging in, the rules, the commands.
 *
 * <p>A challenge object has no state of its own (it is shared by every player); what belongs to one player's attempt lives
 * in the {@link RoadTrial} it is handed, or in {@link RoadProgress#data} for what must be kept between attempts.
 */
public interface RoadChallenge {

    /** Why a trial ended. */
    enum End {
        /** The player won it. */
        WON,
        /** The player left the Road (or was taken out of it). */
        LEFT,
        /** The player went on to the next trial (the old one is taken down). */
        MOVED_ON,
        /** The server took it down (logout, shutdown, command). */
        REMOVED
    }

    /** The key saved with the player's progress; never changes. */
    String id();

    /** The name shown to the player ({@code road.enhanced.challenge.<id>}). */
    default Component title() {
        return Component.translatable("road.enhanced.challenge." + id());
    }

    /**
     * Draws the course on {@code build.canvas()} and says where things are. Pure: the same seed gives the same course.
     * The course may use at most {@link RoadCells#HALF_EXTENT} blocks round the origin.
     */
    RoadLayout build(RoadBuild build);

    /** The player has been put at the start (also when he comes back to a trial he left): a good time to place entities. */
    default void onStart(RoadTrial trial, ServerPlayer player) {
    }

    /** Every tick while the player is in the trial and it is not won yet. */
    default void tick(RoadTrial trial, ServerPlayer player) {
    }

    /** Whether the player may place a block here (false everywhere by default: nothing can be built in the Road). */
    default boolean mayPlace(RoadTrial trial, ServerPlayer player, BlockPos pos) {
        return false;
    }

    /** Whether the player may break the block here. */
    default boolean mayBreak(RoadTrial trial, ServerPlayer player, BlockPos pos) {
        return false;
    }

    /** The player has been put back at a checkpoint (he fell, died, or ran out of time). */
    default void onReset(RoadTrial trial, ServerPlayer player, int checkpoint) {
    }

    /** The trial ends: the place to remove what it spawned (what {@link RoadTrial#track} knows is removed anyway). */
    default void onEnd(RoadTrial trial, End reason) {
    }

    /**
     * Whether the trial can be taken up again where the player left off after he logged out (it is built again, and he is put
     * at his checkpoint). A trial that cannot (it keeps something the player built) is started over from the beginning.
     */
    default boolean resumable() {
        return true;
    }

    /** Convenience for the framework: the next trial's number, 1-based, for messages. */
    @Nullable
    default RoadChallenge next() {
        int index = RoadChallenges.indexOf(this);
        return index < 0 ? null : RoadChallenges.get(index + 1);
    }
}
