package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.platform.Platform;

/**
 * Playing with Enlightened installed too. Both mods put their abilities on the same keys and their panel in the same
 * corner, so a player who is both a hero and a bender would fire both at once. The two mods agree, without either
 * needing the other's classes, through a few JVM properties (client side only):
 * <ul>
 *   <li>{@code kolodkingal.powers.<mod>}: "1" while the local player has that mod's powers;</li>
 *   <li>{@code kolodkingal.powers.active}: the mod the ability keys go to while he has both ("enhanced" or
 *   "enlightened"). This mod owns the key that switches it ({@code KeyBindings.SWITCH_POWERS}) and publishes its
 *   name in {@code kolodkingal.powers.switchKey};</li>
 *   <li>{@code kolodkingal.hud.bottom}: where the active mod's panel ends this frame ("R:96": right corner, y 96), so
 *   the other mod's one-line reminder goes under it.</li>
 * </ul>
 * With Enlightened absent nothing changes.
 */
public final class Coexist {
    private static final String SELF = "enhanced";
    private static final String OTHER = "enlightened";
    private static final boolean OTHER_LOADED = Platform.get().isModLoaded(OTHER);

    private Coexist() {
    }

    /** Whether Enlightened is installed. */
    public static boolean otherLoaded() {
        return OTHER_LOADED;
    }

    /** Says whether the local player is a hero, and what the switching key is called (every client tick). */
    public static void publish(boolean hasPowers, String switchKey) {
        if (OTHER_LOADED) {
            System.setProperty("kolodkingal.powers." + SELF, hasPowers ? "1" : "0");
            System.setProperty("kolodkingal.powers.switchKey", switchKey);
            if (System.getProperty("kolodkingal.powers.active") == null) {
                System.setProperty("kolodkingal.powers.active", SELF);
            }
        }
    }

    /** Whether the local player is also one of Enlightened's benders. */
    public static boolean otherHasPowers() {
        return OTHER_LOADED && "1".equals(System.getProperty("kolodkingal.powers." + OTHER));
    }

    /** Whether the ability keys and the panel are this mod's now (always, unless he bends too and switched to it). */
    public static boolean active() {
        return !otherHasPowers() || !OTHER.equals(System.getProperty("kolodkingal.powers.active"));
    }

    /** Gives the keys to the other mod, or takes them back; returns whether they are this mod's now. */
    public static boolean toggle() {
        boolean mine = OTHER.equals(System.getProperty("kolodkingal.powers.active"));
        System.setProperty("kolodkingal.powers.active", mine ? SELF : OTHER);
        return mine;
    }

    /** Says where this mod's panel ended this frame. */
    public static void hudBottom(int y, boolean right) {
        if (OTHER_LOADED) {
            System.setProperty("kolodkingal.hud.bottom", (right ? "R:" : "L:") + y);
        }
    }

    /** Where the other mod's panel ended ({@code [y, right ? 1 : 0]}), or null. */
    public static int[] otherHudBottom() {
        String value = System.getProperty("kolodkingal.hud.bottom");
        if (value == null || value.length() < 3) {
            return null;
        }
        try {
            return new int[] {Integer.parseInt(value.substring(2)), value.charAt(0) == 'R' ? 1 : 0};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
