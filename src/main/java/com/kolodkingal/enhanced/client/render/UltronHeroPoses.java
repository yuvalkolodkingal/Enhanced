package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.render.Gestures.Target;
import net.minecraft.util.Mth;

/**
 * The pose rows of the hero Ultron's two stances: {@link Stance#ULTRON_FIRE} (R held: both palms along the look, firing
 * in turn) and {@link Stance#ULTRON_CHARGE} (the Overcharge builds in one raised palm that the other hand braces). Written
 * for a right-handed player (see {@link PoseContext}). The poses the gestures hand over to are in {@link UltronGestures}.
 */
final class UltronHeroPoses {
    /** A bolt leaves every four ticks, from alternate hands (the server's {@code boltInterval}, whose default this is). */
    private static final float SHOT_PERIOD = 8.0F;

    /**
     * Both palms along the look and a little in toward each other, each kicked up a few degrees as its bolt leaves (the
     * main hand on the beat, the off hand half a period later), the legs planted; or, for the Overcharge, the main palm
     * along the look a little raised, the off hand across to hold its wrist, both trembling.
     */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w = st.stance(Stance.ULTRON_FIRE);
        if (w > 0.001F) {
            c.aim(true, Target.LOOK, UltronGestures.FIRE_M_AIM[0], UltronGestures.FIRE_M_AIM[1], UltronGestures.FIRE_M_AIM[2], w);
            c.aim(false, Target.LOOK, UltronGestures.FIRE_O_AIM[0], UltronGestures.FIRE_O_AIM[1], UltronGestures.FIRE_O_AIM[2], w);
            c.armAdd(true, -kick(c.age, 0.0F), 0, 0, w);
            c.armAdd(false, -kick(c.age, SHOT_PERIOD * 0.5F), 0, 0, w);
            c.tremble(true, 0.8F, 2.1F, w);
            c.tremble(false, 0.8F, 2.5F, w);
            c.head(-2, 0, 0, w);
            float ground = w * (1.0F - st.wAir);
            c.leg(true, 0, 4, ground);
            c.leg(false, 0, -4, ground);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        w = st.stance(Stance.ULTRON_CHARGE);
        if (w > 0.001F) {
            c.aim(true, Target.LOOK, UltronGestures.CHARGE_M_AIM[0], UltronGestures.CHARGE_M_AIM[1], UltronGestures.CHARGE_M_AIM[2], w);
            c.arm(false, UltronGestures.CHARGE_O[0], UltronGestures.CHARGE_O[1], UltronGestures.CHARGE_O[2], w);
            c.tremble(true, 1.6F, 2.3F, w);
            c.tremble(false, 1.0F, 2.7F, w);
            c.head(UltronGestures.CHARGE_HEAD, 0, 0, w);
            float ground = w * (1.0F - st.wAir);
            c.leg(true, 0, 5, ground);
            c.leg(false, 0, -5, ground);
            c.twist(-6, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** The kick of the shot that left {@code age - phase} ticks ago: sharp, then gone (degrees). */
    private static float kick(float age, float phase) {
        float x = Math.max(0.0F, Mth.cos(Mth.TWO_PI * (age - phase) / SHOT_PERIOD));
        x *= x;
        return 4.0F * x * x;
    }

    private UltronHeroPoses() {
    }
}
