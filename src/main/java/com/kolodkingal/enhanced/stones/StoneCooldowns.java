package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Infinity Stones' cooldowns (3.6): per player and per stone, whatever holds the stone (bare, the Gauntlet, its
 * vessel, a custom hero's row), so that changing the item in hand does not reset them and a vessel shares its stone's
 * cooldown. The stone's own use and its Sneak use share one cooldown, as Thanos's keys do. Kept on the server in a
 * transient map (a relog forgets it, as the heroes' state); each one is also put on the stone's item ({@code
 * ItemCooldowns}, synced by the game), so the client draws the sweep on the stone, in a Gauntlet's socket and on the
 * HUD without a packet of its own. Server thread only.
 */
public final class StoneCooldowns {
    /** Player -> the game time each stone (by ordinal) is ready again. */
    private static final Map<UUID, long[]> READY = new HashMap<>();

    /** Whether the stone is ready for the player now. */
    public static boolean ready(Player player, StoneKind kind, long now) {
        return remaining(player, kind, now) <= 0;
    }

    /** Ticks until the stone is ready for the player (0 when it is). */
    public static long remaining(Player player, StoneKind kind, long now) {
        long[] ready = READY.get(player.getUUID());
        return ready == null ? 0 : Math.max(0, ready[kind.ordinal()] - now);
    }

    /** Starts the stone's cooldown for the player ({@code ticks} 0 or less clears it). */
    public static void start(ServerPlayer player, StoneKind kind, int ticks) {
        long now = player.level().getGameTime();
        READY.computeIfAbsent(player.getUUID(), id -> new long[StoneKind.values().length])[kind.ordinal()] =
                now + Math.max(0, ticks);
        if (ticks > 0) {
            player.getCooldowns().addCooldown(ModItems.stone(kind), ticks);
        } else {
            player.getCooldowns().removeCooldown(ModItems.stone(kind));
        }
    }

    /** His cooldowns are forgotten (he logged out). */
    public static void forget(Player player) {
        READY.remove(player.getUUID());
    }

    /** The server stopped. */
    public static void clear() {
        READY.clear();
    }

    private StoneCooldowns() {
    }
}
