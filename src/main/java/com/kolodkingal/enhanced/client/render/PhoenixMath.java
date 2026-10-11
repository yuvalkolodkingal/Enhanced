package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import net.minecraft.util.Mth;

/**
 * The numbers behind what the Phoenix looks like, kept apart from the drawing so they can be tested without a window:
 * how far the Burst's shell has grown, how bright it is, how large the firebird is and how it beats its wings, how the
 * Psionic Field's dome and Mind Sense's ring grow, where the Mind Reading panel sits, and the fire's colours. Times are
 * ticks since the thing began (a fraction of a tick when the frame is between two).
 */
public final class PhoenixMath {
    /** The shell of the Burst is at full strength until its last this many ticks, then fades. */
    public static final float SHELL_FADE_TICKS = 10.0F;
    /** How bright the shell is at the start (its alpha scale). */
    public static final float SHELL_ALPHA = 0.5F;
    /** How long the firebird of a Burst lives, the radial streaks and the whole show. */
    public static final int BIRD_TICKS = 24;
    public static final int STREAK_TICKS = 30;
    public static final int BURST_TICKS = 40;
    /** The gold flash on the screen of everyone in range. */
    public static final int FLASH_TICKS = 6;
    /** The Psionic Field's dome grows for 8 ticks, holds for 4 and fades over 12. */
    public static final int DOME_GROW_TICKS = 8;
    public static final int DOME_HOLD_TICKS = 4;
    public static final int DOME_FADE_TICKS = 12;
    public static final int DOME_TICKS = DOME_GROW_TICKS + DOME_HOLD_TICKS + DOME_FADE_TICKS;
    /** The ring of Mind Sense spreads this many blocks a tick. */
    public static final float SENSE_RING_SPEED = 2.0F;
    /** The Mind Reading panel gives up on a snapshot this many ticks after the last. */
    public static final int PANEL_TIMEOUT_TICKS = 15;
    /** The panel's gap to the crosshair, and to the edge of the screen. */
    public static final int PANEL_GAP = 14;
    public static final int PANEL_MARGIN = 4;

    // ---- the fire of the Force, from the white-hot core to the deep red ----
    public static final int FIRE_CORE = PhoenixAbilities.FIRE_CORE;
    public static final int FIRE_GOLD = PhoenixAbilities.FIRE_GOLD;
    public static final int FIRE_MAIN = PhoenixAbilities.FIRE_MAIN;
    public static final int FIRE_EMBER = PhoenixAbilities.FIRE_EMBER;
    public static final int FIRE_DEEP = PhoenixAbilities.FIRE_DEEP;

    // ------------------------------------------------------------------------------------------------------------
    // The charge

    /** How charged the Burst is: the ticks since N went down over the ticks of a full charge, 0 to 1. */
    public static float charge(float ticks, int fullTicks) {
        return fullTicks <= 0 ? 1.0F : Mth.clamp(ticks / fullTicks, 0.0F, 1.0F);
    }

    /** The whole percent the HUD says ("charging 63%"). */
    public static int percent(float charge) {
        return Mth.clamp(Math.round(charge * 100.0F), 0, 100);
    }

    /** Above this much of the charge the HUD turns orange and the eyes, the body flame and the screen edge brighten. */
    public static final float BRIGHT_CHARGE = 0.9F;

    // ------------------------------------------------------------------------------------------------------------
    // The Burst

    /**
     * The radius of the wave {@code age} ticks after the release: the server's, which grows {@code speed} blocks a
     * tick.
     */
    public static float shellRadius(float radius, float speed, float age) {
        return Mth.clamp(speed * (age + 1.0F), 0.0F, radius);
    }

    /** The ticks the wave takes to reach its radius. */
    public static float shellTicks(float radius, float speed) {
        return speed <= 0.0F ? 0.0F : radius / speed;
    }

    /**
     * How bright the shell is: half bright, fading to nothing over the last {@link #SHELL_FADE_TICKS} ticks of its
     * growth (a shell that is there for less time than that fades from the first tick), and gone once it has reached
     * its radius.
     */
    public static float shellAlpha(float radius, float speed, float age) {
        float total = shellTicks(radius, speed);
        if (age < 0.0F || age >= total) {
            return 0.0F;
        }
        float fadeFrom = Math.max(0.0F, total - SHELL_FADE_TICKS);
        return SHELL_ALPHA * (1.0F - Mth.clamp((age - fadeFrom) / Math.max(total - fadeFrom, 1.0E-3F), 0.0F, 1.0F));
    }

    /** The firebird of the Burst: from scale 1 to a sixth of the radius over its life. */
    public static float burstBirdScale(float radius, float age) {
        float t = Mth.clamp(age / BIRD_TICKS, 0.0F, 1.0F);
        return Mth.lerp(t * (2.0F - t), 1.0F, Math.max(1.0F, radius / 6.0F));
    }

    /** How far the firebird has flown out along her facing, in blocks: it ends at three quarters of the radius. */
    public static float burstBirdDistance(float radius, float age) {
        float t = Mth.clamp(age / BIRD_TICKS, 0.0F, 1.0F);
        return 0.75F * radius * (1.0F - (1.0F - t) * (1.0F - t));
    }

    /** How much of the firebird shows: whole until its last third, then it dissolves into embers. */
    public static float burstBirdAlpha(float age) {
        return 1.0F - Mth.clamp((age - BIRD_TICKS * 0.6F) / (BIRD_TICKS * 0.4F), 0.0F, 1.0F);
    }

    /**
     * The angle (degrees above level) of the wings of the Burst's firebird: flung open and down (-20) at the release,
     * rising again as it flies off to 50.
     */
    public static float burstWingAngle(float age) {
        float t = Mth.clamp(age / BIRD_TICKS, 0.0F, 1.0F);
        return Mth.lerp(t, -20.0F, 50.0F);
    }

    /** The flash of the first two ticks: a disc of this radius at the centre. */
    public static float flashRadius(float charge, float age) {
        return age < 0.0F || age > 2.0F ? 0.0F : 1.5F * Math.max(0.3F, charge) * (1.0F - age / 3.0F);
    }

    /** The gold on the screen of someone in range: full for the first tick, gone after {@link #FLASH_TICKS}. */
    public static float screenFlash(float age) {
        return age < 0.0F || age >= FLASH_TICKS ? 0.0F : 1.0F - age / FLASH_TICKS;
    }

    /**
     * A creature at this distance from the centre sees the flash (a Burst reaches the radius, and a few blocks more).
     */
    public static boolean flashReaches(double distance, float radius) {
        return distance <= radius + 4.0;
    }

    /** The camera shake of a Burst: a fraction of a degree at the centre, none beyond 24 blocks or after 10 ticks. */
    public static float shake(float charge, double distance, float age) {
        if (age < 0.0F || age > 10.0F || distance > 24.0) {
            return 0.0F;
        }
        return 1.2F * Math.max(0.3F, charge) * (float) (1.0 - distance / 24.0) * (1.0F - age / 10.0F);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The charge: the firebird forming behind her

    /** The firebird that forms behind a charging Phoenix: half size at first, a bit over one as the charge fills. */
    public static float chargeBirdScale(float charge) {
        return 0.5F + 0.8F * Mth.clamp(charge, 0.0F, 1.0F);
    }

    /** The wings only come out once a fifth of the charge is in. */
    public static boolean chargeBirdHasWings(float charge) {
        return charge > 0.2F;
    }

    /** The slow flap of the charging firebird (14 ticks to the beat): the wings swing between 15 and 55 degrees. */
    public static float chargeWingAngle(float time) {
        return 35.0F + 20.0F * Mth.sin(time * Mth.TWO_PI / 14.0F);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Psionic Field and Mind Sense

    /**
     * The radius of the field's dome {@code age} ticks after it closed: it grows to {@code full} in 8 ticks and stays.
     */
    public static float domeRadius(float full, float age) {
        float t = Mth.clamp(age / DOME_GROW_TICKS, 0.0F, 1.0F);
        return full * (1.0F - (1.0F - t) * (1.0F - t));
    }

    /** How strong the dome is: building up while it grows, full while it holds, fading over the last 12 ticks. */
    public static float domeAlpha(float age) {
        if (age < 0.0F || age >= DOME_TICKS) {
            return 0.0F;
        }
        float rise = Mth.clamp(age / DOME_GROW_TICKS, 0.0F, 1.0F);
        float fall = 1.0F - Mth.clamp((age - DOME_GROW_TICKS - DOME_HOLD_TICKS) / DOME_FADE_TICKS, 0.0F, 1.0F);
        return rise * fall;
    }

    /** The radius of Mind Sense's ring {@code age} ticks after the ping, at two blocks a tick up to the range. */
    public static float senseRadius(float range, float age) {
        return Mth.clamp(SENSE_RING_SPEED * age, 0.0F, range);
    }

    /** Whether a mind at this distance from the ping has been passed by the ring. */
    public static boolean senseReaches(double distance, float range, float age) {
        return age >= 0.0F && distance <= range && distance <= SENSE_RING_SPEED * age;
    }

    /** The ticks the ring takes to spread over its whole range. */
    public static int senseRingTicks(float range) {
        return (int) Math.ceil(range / SENSE_RING_SPEED);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Mind Reading panel

    /**
     * Where the top left corner of the panel goes: {@link #PANEL_GAP} pixels to the right of the crosshair and
     * vertically centred on it, flipped to its left when it would run off the right edge, and always kept on the
     * screen.
     *
     * @return {x, y}
     */
    public static int[] panelOrigin(int screenWidth, int screenHeight, int panelWidth, int panelHeight, int crosshairX, int crosshairY) {
        int x = crosshairX + PANEL_GAP;
        if (x + panelWidth > screenWidth - PANEL_MARGIN) {
            x = crosshairX - PANEL_GAP - panelWidth;
        }
        x = Mth.clamp(x, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenWidth - PANEL_MARGIN - panelWidth));
        int y = Mth.clamp(crosshairY - panelHeight / 2, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenHeight - PANEL_MARGIN - panelHeight));
        return new int[]{x, y};
    }

    /**
     * Whether the panel is up: the snapshot is at most {@link #PANEL_TIMEOUT_TICKS} ticks old (and not from the
     * future).
     */
    public static boolean panelFresh(long now, long receivedAt) {
        return receivedAt != Long.MIN_VALUE && now - receivedAt >= 0 && now - receivedAt <= PANEL_TIMEOUT_TICKS;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Colours

    /** Mixes two {@code 0xRRGGBB} colours; {@code t} 0 is {@code a}, 1 is {@code b}. */
    public static int mix(int a, int b, float t) {
        float u = Mth.clamp(t, 0.0F, 1.0F);
        int r = Math.round(Mth.lerp(u, (a >> 16) & 0xFF, (b >> 16) & 0xFF));
        int g = Math.round(Mth.lerp(u, (a >> 8) & 0xFF, (b >> 8) & 0xFF));
        int bl = Math.round(Mth.lerp(u, a & 0xFF, b & 0xFF));
        return (r << 16) | (g << 8) | bl;
    }

    /** A ribbon of the firebird from its root (0) to its tip (1): core white, gold, orange, ember red. */
    public static int fireGradient(float t) {
        float u = Mth.clamp(t, 0.0F, 1.0F);
        if (u < 1.0F / 3.0F) {
            return mix(FIRE_CORE, FIRE_GOLD, u * 3.0F);
        }
        if (u < 2.0F / 3.0F) {
            return mix(FIRE_GOLD, FIRE_MAIN, (u - 1.0F / 3.0F) * 3.0F);
        }
        return mix(FIRE_MAIN, FIRE_EMBER, (u - 2.0F / 3.0F) * 3.0F);
    }

    /** A ribbon of the firebird fades from 0.85 at the root to 0.1 at the tip. */
    public static float fireAlpha(float t) {
        return Mth.lerp(Mth.clamp(t, 0.0F, 1.0F), 0.85F, 0.1F);
    }

    private PhoenixMath() {
    }
}
