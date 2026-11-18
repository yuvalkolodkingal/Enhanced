package com.kolodkingal.enhanced.client.hud;

/**
 * When the HUD's passive lines (a custom hero's "Always on" list, Captain America's super-soldier line) show (3.17): the client
 * setting "Show always-on powers on the HUD". Pure (no client classes), so the game tests can check it.
 */
public final class PassiveHudRules {
    /** Always: as before. Briefly: for {@link #SHOWN_TICKS} when you become the hero, change loadout or look, or join, then faded out. Never: hidden. */
    public enum Mode {
        ALWAYS, BRIEFLY, NEVER
    }

    /** How long (ticks) the lines show in {@link Mode#BRIEFLY}: 8 seconds. */
    public static final int SHOWN_TICKS = 160;
    /** The last ticks of that time over which they fade out. */
    public static final int FADE_TICKS = 20;

    private PassiveHudRules() {
    }

    /** How visible the lines are, 0 (hidden) to 1, {@code until} being the game time at which the brief showing ends. */
    public static float visibility(Mode mode, long until, long now) {
        return switch (mode) {
            case ALWAYS -> 1.0F;
            case NEVER -> 0.0F;
            case BRIEFLY -> now >= until ? 0.0F : Math.min(1.0F, (until - now) / (float) FADE_TICKS);
        };
    }
}
