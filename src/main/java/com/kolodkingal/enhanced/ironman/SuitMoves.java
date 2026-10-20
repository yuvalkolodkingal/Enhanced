package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * One suit's own moves (3.8, PLAN 5.7, 6): what its R, V, B and N do (and G, for the Mark 1 whose G is the Rocket
 * Boost, not flight), what runs every tick while it is worn and working, and how its held moves end. {@link
 * IronManAbilities} hands a key to the moves of the suit he wears once the suit works ({@link IronSuits#powered}); a
 * custom hero with Iron Man's rows has the Mark 85's (PLAN 11.1). Lane L3 registers the Mark 1, 3 and 5 ({@link
 * Mark1Moves}, {@link Mark3Moves}, {@link Mark5Moves}); lane L4 the Mark 50, the Mark 85 and the Hulkbuster, from its
 * own setup.
 */
public interface SuitMoves {
    /**
     * A key of his went down or up while the suit works.
     *
     * @param ability 1 (R), 3 (V), 4 (B), 5 (N); 2 (G) only when {@link #ownsG}
     * @param alt     Sneak (or a custom hero's variant binding)
     */
    void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt);

    /** Every server tick while the suit is worn and works (and for a custom hero with Iron Man's rows). */
    default void tick(ServerPlayer player, PowerData data, long now) {
    }

    /**
     * The suit stops working (taken off, a phase begins, the energy or the reactor gone, the power lost, death, a
     * logout, a new dimension): its held moves end, its {@code Live} bits and flags are cleared.
     */
    default void end(ServerPlayer player, PowerData data) {
    }

    /** Whether G is this suit's move ({@link #onKey} with ability 2) instead of flight (the Mark 1's Rocket Boost). */
    default boolean ownsG() {
        return false;
    }

    /** The moves of a suit (from the mod's setup; a later call replaces an earlier one). */
    static void register(SuitKind kind, SuitMoves moves) {
        Registry.MOVES.put(kind, moves);
    }

    /** The moves of a suit, or null when no lane has registered them yet. */
    @Nullable
    static SuitMoves of(SuitKind kind) {
        return Registry.MOVES.get(kind);
    }

    /** The registered moves. */
    final class Registry {
        static final Map<SuitKind, SuitMoves> MOVES = new EnumMap<>(SuitKind.class);

        static {
            MOVES.put(SuitKind.MARK_1, Mark1Moves.INSTANCE);
            MOVES.put(SuitKind.MARK_3, Mark3Moves.INSTANCE);
            MOVES.put(SuitKind.MARK_5, Mark5Moves.INSTANCE);
        }

        private Registry() {
        }
    }
}
