package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.UltimateConfig;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;

/**
 * Who may use the ultimate versions of the heroes (3.18; now Cosmic Spider-Man): everyone while {@code [ultimate] enabled}
 * is on (off by default), and otherwise the players an operator granted them to. A grant is saved with the player, kept on
 * death and through a change of hero, and synced to that player alone (his HUD reads it).
 */
public final class Ultimate {
    private Ultimate() {
    }

    /** Whether the Ultimate switch is on for everyone. */
    public static boolean on() {
        return EnhancedConfig.SPEC.isLoaded() && UltimateConfig.ENABLED.get();
    }

    /** Whether this player was granted the ultimate versions by an operator. */
    public static boolean granted(Player player) {
        return Boolean.TRUE.equals(ModAttachments.ULTIMATE.get(player));
    }

    /**
     * The same for a holder known only as an object (a client class such as the local player, so that a common class
     * that asks never has to load it; the dedicated server and the GameTests load the hero plug-ins too).
     */
    public static boolean allowedHolder(Object holder) {
        return on() || Boolean.TRUE.equals(ModAttachments.ULTIMATE.get(holder));
    }

    /** Whether this player may use them now (both sides). */
    public static boolean allowed(Player player) {
        return on() || granted(player);
    }

    /** Grants or takes back the ultimate versions from one player. */
    public static void grant(Player player, boolean granted) {
        ModAttachments.ULTIMATE.set(player, granted);
    }
}
