package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the hero Ultron's gestures: the take-off on the anti-gravity of his palms, the snap of both hands up that
 * starts the plasma, the Overcharge gathering in one raised palm and leaving it with a kick, the Pull's open palm and
 * the Slam's stamp, the Upload's arms drawn in and thrown wide, the Hack's held-out hand, the conductor's arms of the
 * Sentry Army and the order of Command. He is a machine: the arms go to a pose and stop there, nothing sways, and what
 * is held is held dead still, so the keys are few, the eases sharp and the tremble small.
 *
 * <p>The poses the gestures hand over to are here too ({@link #CHARGE_M_AIM}, {@link #CHARGE_O}, {@link #FIRE_M_AIM},
 * {@link #FIRE_O_AIM}), because the stance rows ({@link UltronHeroPoses}) and the last keys of the gestures must be the
 * same numbers. No Minecraft client imports: the tables, and {@link #predicted}, are checked by the game tests.
 */
public final class UltronGestures {
    /** The Overcharge's stance: the main palm along the look, a little raised, and the other hand across to brace its wrist. */
    public static final float[] CHARGE_M_AIM = {-10, -4, 0};
    public static final float[] CHARGE_O = {-60, 30, -6};
    /** How far the head is thrown back and the body leans (degrees) while the Overcharge builds. */
    public static final float CHARGE_HEAD = -4;
    public static final float CHARGE_LEAN = -3;
    /** R held: both palms along the look, converging a little (offsets of an AIM row). */
    public static final float[] FIRE_M_AIM = {0, -8, 0};
    public static final float[] FIRE_O_AIM = {0, 8, 0};
    public static final float FIRE_LEAN = -2;

    /** The gestures that carry the Overcharge's raised palm in their own keys: while one plays, the stance's lean gives way. */
    private static final GestureId[] OWNS_THE_CHARGE = {GestureId.ULTRON_CHARGE_START, GestureId.ULTRON_BLAST};

    static void register() {
        flight();
        plasma();
        gravity();
        network();
    }

    // ---- Flight ----

    private static void flight() {
        // The palms press the air down: the arms go back and out and the knees tuck, then settle on the hover pose
        // (FlightProfile).
        define(GestureId.TAKEOFF_ULTRON)
                .key(0, S).m(0, 0, 8).o(0, 0, -8).legs(0, 0).head(0, 0, 0).drop(0)
                .key(5, O).m(30, 0, 26).o(30, 0, -26).legs(-4, -4).head(2, 0, 0).drop(0.05F)
                .key(11, E).m(20, 0, 36).o(20, 0, -36).legs(8, 8).head(-8, 0, 0).drop(0)
                .key(22, S).m(12, 0, 16).o(12, 0, -16).legs(3, 3).head(0, 0, 0).drop(0);
    }

    // ---- Plasma: R ----

    private static void plasma() {
        // Both hands snap up along the look as the first bolt leaves (the stance holds them there, with the head and the lean).
        define(GestureId.ULTRON_FIRE_START)
                .key(0, S).m(0, 0, 8).o(0, 0, -8)
                .key(2, O).mAim(LOOK, FIRE_M_AIM[0], FIRE_M_AIM[1], FIRE_M_AIM[2]).oAim(LOOK, FIRE_O_AIM[0], FIRE_O_AIM[1], FIRE_O_AIM[2])
                .key(8, S).mAim(LOOK, FIRE_M_AIM[0], FIRE_M_AIM[1], FIRE_M_AIM[2]).oAim(LOOK, FIRE_O_AIM[0], FIRE_O_AIM[1], FIRE_O_AIM[2]);
        // The Overcharge: one palm rises along the look, the other hand comes across to hold its wrist.
        define(GestureId.ULTRON_CHARGE_START)
                .key(0, S).m(0, 0, 8).o(0, 0, -8)
                .key(5, O).m(-62, -4, 8).o(CHARGE_O[0], CHARGE_O[1], CHARGE_O[2]).head(CHARGE_HEAD, 0, 0).lean(CHARGE_LEAN)
                .key(12, S).mAim(LOOK, CHARGE_M_AIM[0], CHARGE_M_AIM[1], CHARGE_M_AIM[2]).o(CHARGE_O[0], CHARGE_O[1], CHARGE_O[2])
                .head(CHARGE_HEAD, 0, 0).lean(CHARGE_LEAN);
        // The blast leaves the palm: the arm is thrown up by it, the head and the body shoved back, then everything settles.
        define(GestureId.ULTRON_BLAST)
                .key(0, S).mAim(LOOK, CHARGE_M_AIM[0], CHARGE_M_AIM[1], CHARGE_M_AIM[2]).o(CHARGE_O[0], CHARGE_O[1], CHARGE_O[2])
                .head(CHARGE_HEAD, 0, 0).lean(CHARGE_LEAN)
                .key(2, O).mAim(LOOK, -30, -4, 0).o(-44, 20, -14).head(-9, 0, 0).lean(-8).drop(0.04F)
                .key(5, E).mAim(LOOK, -22, -4, 0).o(-44, 20, -14).head(-6, 0, 0).lean(-5).drop(0.02F)
                .key(12, S).m(0, 0, 8).o(0, 0, -8).head(0, 0, 0).lean(0).drop(0);
    }

    // ---- Gravity: V ----

    private static void gravity() {
        // The Pull: an open palm thrust at the creature, then drawn back to the shoulder as it comes.
        define(GestureId.ULTRON_PULL)
                .key(0, S).m(0, 0, 8)
                .key(3, O).mAim(LOOK, -6, 0, 0).head(-2, 0, 0).twist(-6)
                .key(9, E).m(-44, -10, 16).head(0, 0, 0).twist(8).lean(3)
                .key(16, S).m(0, 0, 8).head(0, 0, 0).twist(0).lean(0);
        // The Slam: both arms over the head, then brought down with the stamp of a foot.
        define(GestureId.ULTRON_SLAM)
                .key(0, S).m(-6, 0, 10).o(-6, 0, -10).legs(0, 0).head(0, 0, 0).lean(0).drop(0)
                .key(6, O).m(-168, 0, 26).o(-168, 0, -26).legs(4, 4).head(-12, 0, 0).lean(-4).drop(0.04F)
                .key(11, O).m(8, 0, 38).o(8, 0, -38).legs(-16, 8).head(8, 0, 0).lean(9).drop(0.12F)
                .key(22, S).m(0, 0, 10).o(0, 0, -10).legs(0, 0).head(0, 0, 0).lean(0).drop(0);
    }

    // ---- The network: B and N ----

    private static void network() {
        // The Upload: the arms drawn in across the chest, the head bowed, then thrown wide as he becomes data.
        define(GestureId.ULTRON_UPLOAD)
                .key(0, S).m(0, 0, 8).o(0, 0, -8)
                .key(5, O).m(-62, -46, 4).o(-62, 46, -4).head(10, 0, 0)
                .key(10, O).m(-6, 0, 86).o(-6, 0, -86).head(-14, 0, 0).lean(-4)
                .key(18, S).m(0, 0, 10).o(0, 0, -10).head(0, 0, 0).lean(0);
        // The Hack: one hand held out at the machine, trembling a little, the head forward.
        define(GestureId.ULTRON_HACK)
                .key(0, S).m(0, 0, 8)
                .key(4, O).mAim(LOOK, -4, 0, 0).head(3, 0, 0).twist(-4)
                .key(16, S).mAim(LOOK, -4, 0, 0).mTremble(1.6F, 2.1F).head(3, 0, 0).twist(-4)
                .key(22, S).m(0, 0, 8).head(0, 0, 0).twist(0);
        // The Sentry Army: both arms raised and swept out and down like a conductor's.
        define(GestureId.ULTRON_ARMY)
                .key(0, S).m(0, 0, 8).o(0, 0, -8)
                .key(7, O).m(-150, 0, 30).o(-150, 0, -30).head(-10, 0, 0).lean(-3)
                .key(14, E).m(-24, 0, 74).o(-24, 0, -74).head(0, 0, 0).lean(2)
                .key(22, S).m(0, 0, 10).o(0, 0, -10).head(0, 0, 0).lean(0);
        // Command: one arm thrown out along the look, a sharp point, then dropped.
        define(GestureId.ULTRON_COMMAND)
                .key(0, S).m(0, 0, 8)
                .key(3, O).mAim(LOOK, 0, -2, 0).head(-2, 0, 0).twist(-8)
                .key(9, S).mAim(LOOK, 0, -2, 0).head(-2, 0, 0).twist(-8)
                .key(14, S).m(0, 0, 8).head(0, 0, 0).twist(0);
    }

    // ---- What the key presses start, before the server has answered ----

    /** What only the client can see about the press (the server's refusals are all in these). */
    public record Guards(boolean plasma, boolean overcharge, boolean pullTarget, boolean drone, boolean room) {
        /** Nothing is in the way of anything. */
        public static final Guards ALL = new Guards(true, true, true, true, true);
    }

    /**
     * The gesture the local Ultron's own key press starts at once, or null: R with plasma in the pool (Sneak: the
     * Overcharge, when it is rested and the pool pays for it), V at a creature in his sight (Sneak: the Slam, when it
     * is rested), B with a drone to jump into and N with room for more drones, each when rested. The server's other
     * refusals (a Vision's cut, a Hex, a spectator) cannot be seen, and then the gesture plays out and costs nothing.
     */
    @Nullable
    public static GestureId predicted(PowerData data, int ability, long now, boolean sneaking, Guards guards) {
        switch (ability) {
            case 1 -> {
                if (data.is(Live.ULTRON_CHARGING)) {
                    return null;
                }
                if (sneaking) {
                    return data.isReady(1, now) && guards.overcharge ? GestureId.ULTRON_CHARGE_START : null;
                }
                return guards.plasma && !data.beamActive() ? GestureId.ULTRON_FIRE_START : null;
            }
            case 3 -> {
                if (!data.isReady(3, now)) {
                    return null;
                }
                if (sneaking) {
                    return GestureId.ULTRON_SLAM;
                }
                return guards.pullTarget ? GestureId.ULTRON_PULL : null;
            }
            case 4 -> {
                return !sneaking && data.isReady(4, now) && guards.drone ? GestureId.ULTRON_UPLOAD : null;
            }
            case 5 -> {
                return !sneaking && data.isReady(5, now) && guards.room ? GestureId.ULTRON_ARMY : null;
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * How much a gesture that carries the raised palm of the Overcharge in its own keys is playing now (0 to 1): the
     * stance gives up its lean by this much, because the gesture's lean is added to the stance's and the body would
     * otherwise be thrown back twice as far while the one hands over to the other.
     */
    public static float owningTheCharge(HeroAnimState st) {
        float own = 0.0F;
        for (HeroAnimState.Slot slot : st.slots) {
            if (slot.active && slot.weight > own) {
                for (GestureId id : OWNS_THE_CHARGE) {
                    if (slot.id == id) {
                        own = slot.weight;
                    }
                }
            }
        }
        return Math.min(own, 1.0F);
    }

    private UltronGestures() {
    }
}
