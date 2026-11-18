package com.kolodkingal.enhanced.client.hud;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import net.minecraft.client.Minecraft;

/**
 * Remembers, for the local player, what the passive HUD lines last said and since when, so that in the "Briefly" setting they
 * show for 8 seconds when the player joins, becomes the hero or changes loadout or look ({@code identity} changes), and then
 * fade out. One line of state: it needs no clean-up (a new world is a new identity).
 */
public final class PassiveHud {
    private static Object level;
    private static String identity;
    private static long until;

    private PassiveHud() {
    }

    /**
     * How visible the passive lines of the hero {@code who} are now (0 to 1). {@code who} names the hero, his loadout and look:
     * when it differs from the last call (or the world is a new one) the brief showing starts again.
     */
    public static float visibility(Minecraft mc, String who) {
        if (mc.level == null) {
            return 0.0F;
        }
        long now = mc.level.getGameTime();
        if (level != mc.level || !who.equals(identity)) {
            level = mc.level;
            identity = who;
            until = now + PassiveHudRules.SHOWN_TICKS;
        }
        return PassiveHudRules.visibility(EnhancedClientConfig.passiveHud(), until, now);
    }
}
