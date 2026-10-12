package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.B;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.ANCHOR;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of Spider-Man's fifteen gestures (DESIGN section 14, "Animation"): the thwip of a Web Shot, the burst flung
 * wide, the arm shot out at the anchor of a zip and thrown up at the anchor of a swing, the hands that spread to climb
 * and shake themselves loose, the finger to the temple, the guard that goes up when his sense tingles, the shoulders
 * that roll back as the legs unfold, the menace of Instant Kill, the clutched head of the Black Eyes, and the three the
 * animator starts from what it sees of his motion (the landing after a fast fall, the push off a wall, the grab of a
 * wall at the end of a zip). A hero of the web moves lightly: his poses are quick and end on the stance they hand over
 * to.
 *
 * <p>The poses the gestures hand over to are here too ({@link #ALERT_M}, {@link #KILL_M}, ...), because the stance row
 * ({@link SpiderPoses}) and the last key of the gesture that leads into it must be the same numbers. No Minecraft
 * client imports: the tables, and {@link #predicted}, are checked by the game tests.
 */
public final class SpiderGestures {
    // ---- the stances' arms, as the gestures that hand over to them end ----

    /** Clinging to a wall: both hands reach up the wall (the sway of the crawl is added to the x of these). */
    public static final float[] CLING_WALL_M = {-160, -10, 24};
    public static final float[] CLING_WALL_O = {-160, 10, -24};
    /** The knees, out and up (the right leg and the left leg: x), and the head. */
    public static final float CLING_WALL_LEGS = -58;
    public static final float CLING_WALL_HEAD = -10;

    /** The spider-sense tingles: both forearms up in front of him. */
    public static final float[] ALERT_M = {-70, -30, 18};
    public static final float[] ALERT_O = {-70, 30, -18};
    public static final float ALERT_HEAD = -6;
    public static final float ALERT_DROP = 0.08F;

    /** Instant Kill: a menacing stillness, the arms a little out. */
    public static final float[] KILL_M = {-12, 0, 14};
    public static final float[] KILL_O = {-12, 0, -14};
    public static final float KILL_HEAD = 8;
    public static final float KILL_LEAN = 6;

    /** The Black Eyes: a predatory hunch, the hands hanging forward and out, clawed. */
    public static final float[] SURGE_M = {-28, 8, 22};
    public static final float[] SURGE_O = {-28, -8, -22};
    public static final float SURGE_HEAD = 14;
    public static final float SURGE_LEAN = 12;
    public static final float SURGE_DROP = 0.06F;

    /** A zip: the web arm aims at the anchor (these degrees under it), the other is swept back, the legs trail. */
    public static final float[] ZIP_AIM = {0, -4, 0};
    public static final float[] ZIP_O = {22, 0, -34};
    public static final float[] ZIP_LEGS = {10, -16};
    public static final float ZIP_HEAD = -6;
    public static final float ZIP_LEAN = 16;

    /** A swing: the web arm along the web, up; the other back and out; the knees tucked. */
    public static final float[] SWING_AIM = {0, 0, 0};
    public static final float[] SWING_O = {28, 0, -38};
    public static final float[] SWING_LEGS = {-38, -52};
    public static final float SWING_HEAD = -10;
    public static final float SWING_LEAN = 12;

    /** The lean and the dip of clinging to a wall: the chest against it, a little low. */
    public static final float CLING_WALL_LEAN = -4.0F;
    public static final float CLING_WALL_DROP = 0.10F;

    /** How deep a superhero landing sinks the body (blocks) and how far it pitches over the front foot (degrees). */
    public static final float LAND_DROP = 0.30F;
    public static final float LAND_LEAN = 28.0F;

    static void register() {
        web();
        body();
        senses();
        motion();
    }

    // ---- The web: the shot, the burst, the arm of a zip and of a swing ----

    private static void web() {
        // The thwip: the arm snaps to the aim, the wrist flicks, the other arm balances.
        define(GestureId.SPIDER_SHOOT)
                .key(0, S).m(-35, -8, 14).o(10, 0, -12).lean(0).twist(0)
                .key(2, O).mAim(LOOK, -4, -6, 0).o(14, 0, -22).lean(3).twist(-6)
                .key(4, S).mAim(LOOK, -2, -6, 0).mTremble(1.1F, 2.1F).lean(2)
                .key(8, S).m(-20, 0, 8).o(0, 0, -8).lean(0).twist(0);

        // The burst: the arms gather across the chest in a crouch, then are flung wide as he straightens.
        define(GestureId.SPIDER_BURST)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8).drop(0).lean(0).head(0, 0, 0).legs(0, 0)
                .key(6, E).m(-72, -36, 0).o(-72, 36, 0).drop(0.12F).lean(6).head(8, 0, 0).legs(-14, 12)
                .key(9, S).m(-76, -38, 0).o(-76, 38, 0).drop(0.14F).lean(7)
                .key(12, B).m(-100, 55, 40).o(-100, -55, -40).drop(0.02F).lean(-6).head(-10, 0, 0).legs(-4, 4)
                .key(20, S).m(-20, 0, 18).o(-20, 0, -18).drop(0).lean(0).head(0, 0, 0).legs(0, 0);

        // The zip: the arm shoots out at the anchor, the body tips toward it, then it is the stance SPIDER_ZIP.
        define(GestureId.SPIDER_ZIP_START)
                .key(0, S).m(-40, -10, 12).o(0, 0, -10).lean(0)
                .key(2, O).mAim(ANCHOR, -6, -4, 0).o(16, 0, -26).lean(8)
                .key(10, S).mAim(ANCHOR, ZIP_AIM[0], ZIP_AIM[1], ZIP_AIM[2]).o(ZIP_O).lean(ZIP_LEAN);

        // The swing: the arm is thrown up at the anchor, then it is the stance SPIDER_SWING.
        define(GestureId.SPIDER_SWING_START)
                .key(0, S).m(-60, -6, 10).o(0, 0, -10).lean(0)
                .key(2, O).mAim(ANCHOR, -4, 0, 0).o(20, 0, -30).lean(6)
                .key(10, S).mAim(ANCHOR, SWING_AIM[0], SWING_AIM[1], SWING_AIM[2]).o(SWING_O).lean(SWING_LEAN).legs(SWING_LEGS[0], SWING_LEGS[1]);
    }

    // ---- The body: the hands that climb, the shoulders that roll back, the claw ----

    private static void body() {
        // Wall Crawl on: both hands up and spread, the fingers flexing; off: shaking the hands loose.
        define(GestureId.SPIDER_CRAWL_ON)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8)
                .key(3, O).m(-120, 18, 14).o(-120, -18, -14)
                .key(8, S).m(-20, 0, 10).o(-20, 0, -10);
        define(GestureId.SPIDER_CRAWL_OFF)
                .key(0, S).m(-30, 0, 10).o(-30, 0, -10)
                .key(2, E).m(-45, 0, 14).o(-45, 0, -14)
                .key(4, E).m(-30, 0, 6).o(-30, 0, -6)
                .key(6, S).m(-8, 0, 8).o(-8, 0, -8);

        // The Iron Spider's legs: the shoulders roll back, the arms open and the head lifts as they unfold.
        define(GestureId.SPIDER_LEGS_OUT)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8).lean(0).head(0, 0, 0)
                .key(5, E).m(10, -10, 42).o(10, 10, -42).lean(-6).head(-12, 0, 0)
                .key(16, S).m(-12, 0, 14).o(-12, 0, -14).lean(0).head(0, 0, 0);
        define(GestureId.SPIDER_LEGS_IN)
                .key(0, S).m(-12, 0, 14).o(-12, 0, -14).lean(0)
                .key(4, E).m(10, 0, 30).o(10, 0, -30).lean(-3)
                .key(10, S).m(-8, 0, 8).o(-8, 0, -8).lean(0);

        // Instant Kill: the head drops, the arms go to the claw, then it is the stance SPIDER_KILL.
        define(GestureId.SPIDER_KILL_ON)
                .key(0, S).m(-12, 0, 14).o(-12, 0, -14).head(0, 0, 0).lean(0)
                .key(4, O).m(-6, 0, 18).o(-6, 0, -18).head(KILL_HEAD * 1.75F, 0, 0).lean(8)
                .key(14, S).m(KILL_M).o(KILL_O).head(KILL_HEAD, 0, 0).lean(KILL_LEAN);

        // The Black Eyes: he clutches his head, shudders, and snaps up into the hunch of the stance SPIDER_SURGE.
        define(GestureId.SPIDER_SURGE_ON)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8).head(0, 0, 0).drop(0).lean(0)
                .key(4, O).m(-150, -40, 0).mTremble(3.0F, 2.4F).o(-150, 40, 0).oTremble(3.0F, 2.4F).head(10, 0, 0).drop(0.10F)
                .key(12, S).m(-150, -40, 0).mTremble(4.0F, 2.8F).o(-150, 40, 0).oTremble(4.0F, 2.8F).head(16, 0, 0).drop(0.14F)
                .key(16, B).m(-28, 8, 22).o(-28, -8, -22).head(-8, 0, 0).drop(0.02F).lean(10)
                .key(22, S).m(SURGE_M).o(SURGE_O).head(SURGE_HEAD, 0, 0).lean(SURGE_LEAN).drop(SURGE_DROP);
    }

    // ---- The senses: a finger to the temple, the guard ----

    private static void senses() {
        // The spider-sense switched on or off: a finger to the temple.
        define(GestureId.SPIDER_SENSE_TOGGLE)
                .key(0, S).m(-8, 0, 8).head(0, 0, 0)
                .key(3, O).m(-150, -30, 0).head(-6, 8, 0)
                .key(8, S).m(-8, 0, 8).head(0, 0, 0);

        // The sense tingles: the guard goes up, then it is the stance SPIDER_ALERT.
        define(GestureId.SPIDER_ALERT_START)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8).head(0, 0, 0).drop(0)
                .key(2, O).m(ALERT_M).o(ALERT_O).head(ALERT_HEAD, 0, 0).drop(ALERT_DROP)
                .key(12, S).m(ALERT_M).o(ALERT_O).head(ALERT_HEAD, 0, 0).drop(ALERT_DROP);
    }

    // ---- Motion: the landing, the push off a wall, the grab of a wall ----

    private static void motion() {
        // The superhero landing after a fast fall: the body sinks and pitches over the front foot, one fist hangs to
        // the ground, the back leg trails with its toe on the floor.
        define(GestureId.SPIDER_LAND)
                .key(0, S).drop(0).lean(0).legs(0, 0)
                .key(2, O).drop(LAND_DROP).lean(LAND_LEAN).legs(-52, 62).mWorld(0, 0, 14).o(-20, 0, -18).head(-12, 0, 0)
                .key(10, S).drop(LAND_DROP * 0.93F).lean(LAND_LEAN * 0.93F)
                .key(22, S).drop(0).lean(0).legs(0, 0).m(-8, 0, 8).o(-8, 0, -8).head(0, 0, 0);

        // The push off a wall or a ceiling: both arms thrown up, the legs kicking off.
        define(GestureId.SPIDER_LEAP)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8)
                .key(2, O).m(-150, -8, 12).o(-150, 8, -12).lean(10).legs(-30, -30)
                .key(10, S).m(-20, 0, 10).o(-20, 0, -10).lean(0);

        // A zip ends on a wall: the hands reach for it, then it is the stance SPIDER_CLING_WALL.
        define(GestureId.SPIDER_ARRIVE)
                .key(0, S).m(-150, -10, 24).o(-150, 10, -24).legs(-58, -58)
                .key(4, O).drop(0.06F)
                .key(12, S).m(CLING_WALL_M).o(CLING_WALL_O).drop(0.0F).legs(CLING_WALL_LEGS, CLING_WALL_LEGS);
    }

    // ---- What the stances add to the body, and what the gestures that lead into them take over ----

    /**
     * The gesture whose last key is the pose of this stance (the one that hands over to it), or null. While it plays it
     * carries the stance's lean, dip and head itself.
     */
    @Nullable
    public static GestureId leading(Stance stance) {
        return switch (stance) {
            case SPIDER_ZIP -> GestureId.SPIDER_ZIP_START;
            case SPIDER_SWING -> GestureId.SPIDER_SWING_START;
            case SPIDER_KILL -> GestureId.SPIDER_KILL_ON;
            case SPIDER_SURGE -> GestureId.SPIDER_SURGE_ON;
            case SPIDER_ALERT -> GestureId.SPIDER_ALERT_START;
            default -> null;
        };
    }

    /**
     * How much the gesture that leads into a stance is playing now (0 to 1). Lean, dip and head of gestures and stances
     * are added to the body, and the gesture's last key holds the stance's own numbers, so while the two overlap the
     * body would be thrown twice as far as either says: the stance gives up this much of them, and the pose crosses
     * from the one to the other without a jump.
     */
    public static float owning(HeroAnimState st, Stance stance) {
        GestureId lead = leading(stance);
        float own = 0.0F;
        if (lead != null) {
            for (HeroAnimState.Slot slot : st.slots) {
                if (slot.active && slot.id == lead && slot.weight > own) {
                    own = slot.weight;
                }
            }
        }
        return Math.min(own, 1.0F);
    }

    /**
     * How much of a stance's lean, dip and head is its own: its weight, less what the gesture leading into it carries.
     */
    public static float kept(HeroAnimState st, Stance stance) {
        return st.stance(stance) * (1.0F - owning(st, stance));
    }

    /** The lean the stances add to the body, degrees (summed with the others'). */
    public static float lean(HeroAnimState st) {
        return ZIP_LEAN * kept(st, Stance.SPIDER_ZIP) + SWING_LEAN * kept(st, Stance.SPIDER_SWING) + SURGE_LEAN * kept(st, Stance.SPIDER_SURGE)
                + KILL_LEAN * kept(st, Stance.SPIDER_KILL) + CLING_WALL_LEAN * st.stance(Stance.SPIDER_CLING_WALL);
    }

    /** The dip the stances add to the body, blocks (positive sinks). */
    public static float drop(HeroAnimState st) {
        return CLING_WALL_DROP * st.stance(Stance.SPIDER_CLING_WALL) + SURGE_DROP * kept(st, Stance.SPIDER_SURGE)
                + ALERT_DROP * kept(st, Stance.SPIDER_ALERT);
    }

    // ---- What the key presses start, before the server has answered ----

    /**
     * What a client can see of the world and of the hero that the synced data does not say, for {@link #predicted}.
     *
     * @param shotReady   his own timer of the (hidden) Web Shot says another can go
     * @param shotTarget  a creature or a block stands in his sight within the shot's range
     * @param burstTarget a creature stands within the burst's range and in sight
     * @param iron        his look is the Iron Spider
     * @param stripped    Magneto has scattered his nanites
     * @param confirming  the first press of Instant Kill is waiting for its confirmation
     * @param fedForKill  he is not starving (Instant Kill refuses a hero with an empty stomach)
     * @param fedForEyes  he has enough food for the Black Eyes
     */
    public record Sight(boolean shotReady, boolean shotTarget, boolean burstTarget, boolean iron, boolean stripped, boolean confirming,
                        boolean fedForKill, boolean fedForEyes) {
    }

    /**
     * The gesture the local Spider-Man's key press starts at once, or null: R a Web Shot (a target in sight and the
     * hidden timer out), Sneak + R a burst (rested, with someone in range), V the Wall Crawl switch, Sneak + V the
     * spider-sense switch, B the legs (Iron Spider look and whole nanites), Sneak + B Instant Kill (only the press that
     * confirms), N the Black Eyes. G is not here: a zip or a swing begins in his own movement, which starts its gesture
     * the moment it begins ({@link SpiderClient#edges}). The server's other refusals (a boss in the way, a shield, an
     * Entombed hero) cannot be seen from here; the gesture then plays out and costs nothing.
     */
    @Nullable
    public static GestureId predicted(PowerData data, int ability, long now, boolean sneaking, Sight sight) {
        return switch (ability) {
            case 1 -> sneaking ? (data.isReady(1, now) && sight.burstTarget() ? GestureId.SPIDER_BURST : null)
                    : sight.shotReady() && sight.shotTarget() ? GestureId.SPIDER_SHOOT : null;
            case 3 -> sneaking ? GestureId.SPIDER_SENSE_TOGGLE : data.toggle(Toggle.WALL_CRAWL) ? GestureId.SPIDER_CRAWL_OFF : GestureId.SPIDER_CRAWL_ON;
            case 4 -> {
                if (!sight.iron() || sight.stripped()) {
                    yield null;
                }
                if (!sneaking) {
                    yield data.toggle(Toggle.SPIDER_LEGS) ? GestureId.SPIDER_LEGS_IN : GestureId.SPIDER_LEGS_OUT;
                }
                boolean starts = !data.is(Live.INSTANT_KILL) && data.toggle(Toggle.SPIDER_LEGS) && data.isReady(4, now) && sight.fedForKill()
                        && !sight.confirming();
                yield starts ? GestureId.SPIDER_KILL_ON : null;
            }
            case 5 -> !data.is(Live.BLACK_EYES) && data.isReady(5, now) && sight.fedForEyes() ? GestureId.SPIDER_SURGE_ON : null;
            default -> null;
        };
    }

    private SpiderGestures() {
    }
}
