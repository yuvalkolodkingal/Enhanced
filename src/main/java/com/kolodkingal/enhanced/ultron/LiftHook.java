package com.kolodkingal.enhanced.ultron;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The seam between the Ultron fight and the Sokovia lift: the fight knows <em>when</em> the island rises and lands, and
 * the lift (a lane of its own) knows <em>how</em>. The fight calls these at the moments of its phases, in this order:
 * {@link #refusal} at the ritual, {@link #warn} when the lift's warning begins (it answers whether the island will really
 * rise), {@link #lock} when everybody is locked and carried, {@link #rise} every tick of the rise until it answers true,
 * then, after the victory, {@link #beginDescent} and {@link #descend} until the island has landed. {@link #finish} is the
 * last word, forced when the server is stopping (everything is put back at once).
 *
 * <p>Every method does the Tremor by default: nothing moves, no site is refused, and the island is "up" as soon as its
 * time has passed. {@link LiftHooks} holds the one that is installed; until the Sokovia lane installs its own, the fight
 * is played as the Tremor in every mode.
 */
public interface LiftHook {
    /** The Tremor: nothing moves. */
    LiftHook TREMOR = new LiftHook() {
    };

    /**
     * Why the ritual cannot be done at this site for the lift (headroom to lift into, the sky above, what the ground
     * carries), or null if it can. Asked at the ritual, with the island's radius; a refusal is shown to the starter and
     * uses nothing up. The Tremor wants none of it.
     */
    @Nullable
    default Component refusal(ServerLevel level, BlockPos centre, int radius) {
        return null;
    }

    /**
     * The lift's warning begins (ten seconds): check the site again, take what will be needed (the island's shape, a
     * journal, region tickets).
     *
     * @return true if the island will rise; false if the fight goes on as the Tremor
     */
    default boolean warn(UltronFight fight) {
        return false;
    }

    /** The warning is over: lock and carry everyone and everything on the island, and begin the rise. */
    default void lock(UltronFight fight) {
    }

    /**
     * One tick of the rise.
     *
     * @param tick the ticks since the rise began
     * @return true when the island is up (placed) and everybody is released on it
     */
    default boolean rise(UltronFight fight, int tick) {
        return true;
    }

    /** The fight is won or ended and the pause is over: lock everyone again and begin to lower the island. */
    default void beginDescent(UltronFight fight) {
    }

    /**
     * One tick of the descent.
     *
     * @param tick the ticks since {@link #beginDescent}
     * @return true when the island has landed, the ground is as it was and everybody is released on it
     */
    default boolean descend(UltronFight fight, int tick) {
        return true;
    }

    /**
     * The fight is gone. When {@code forced} (the server is stopping, an operator cleans up) everything must be put back
     * <em>now</em>, synchronously: the island removed, the people set down on the ground, the journal deleted.
     */
    default void finish(UltronFight fight, boolean forced) {
    }
}
