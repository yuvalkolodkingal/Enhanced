package com.kolodkingal.enhanced.healing;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Who wants to know when a healer goes DOWN and rises (3.7): Deadpool's lines (lane L3), Wolverine's growl (lane L2).
 * Lane L1 calls {@link #down} and {@link #rise} from {@code Downed}; the others register a {@link Listener} at mod
 * setup. Written by the 3.7 skeleton so the lanes compile against each other's absence.
 */
public final class HealingHooks {

    /** One who listens. Both default to nothing. */
    public interface Listener {
        /** {@code player} has just gone DOWN (a lethal blow cut to 1 health). */
        default void down(ServerPlayer player) {
        }

        /** {@code player} has just risen; {@code rescued}: moved out of a hazard to safe ground. */
        default void rise(ServerPlayer player, boolean rescued) {
        }
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    public static void register(Listener listener) {
        LISTENERS.add(listener);
    }

    /** Lane L1: a healer went down. */
    public static void down(ServerPlayer player) {
        for (Listener listener : LISTENERS) {
            listener.down(player);
        }
    }

    /** Lane L1: a healer rose. */
    public static void rise(ServerPlayer player, boolean rescued) {
        for (Listener listener : LISTENERS) {
            listener.rise(player, rescued);
        }
    }

    private HealingHooks() {
    }
}
