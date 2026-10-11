package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.render.Gestures.Target;
import net.minecraft.util.Mth;

/**
 * The pose rows of the Phoenix's two stances ({@link Stance#PHOENIX_READ}, {@link Stance#PHOENIX_CHARGE}) and her own
 * way of holding what her telekinesis holds. Written for a right-handed player (see {@link PoseContext}). The poses the
 * gestures hand over to are in {@link PhoenixGestures}, which the game tests can read.
 */
final class PhoenixPoses {

    /** Mind Reading: the head tilted and a little lowered, as if listening. */
    static void ambient(PoseContext c) {
        float w = c.st.stance(Stance.PHOENIX_READ);
        if (w > 0.001F) {
            c.head(2, 0, 5 + 1.5F * Mth.sin(0.05F * c.age), w);
        }
    }

    /**
     * Her telekinesis in an open, steady palm with the other hand raised a little (elegant, where the shared row is the
     * claw of the films; this runs after it, so it wins), and the V of arms thrown up and out that gathers the Burst:
     * trembling, the head thrown back, the legs hanging. While the gesture that starts the charge or lets it go plays, it
     * carries the head itself, so the stance gives its head up by the weight of the gesture.
     */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w = st.stance(Stance.TK_HOLD);
        if (w > 0.001F) {
            c.aim(true, Target.HELD, -8, -4, 0, w);
            c.arm(false, -14, 0, -24, w);
            c.occupy(true, w);
        }
        w = st.stance(Stance.PHOENIX_CHARGE);
        if (w > 0.001F) {
            c.arm(true, PhoenixGestures.CHARGE_M[0], PhoenixGestures.CHARGE_M[1], PhoenixGestures.CHARGE_M[2], w);
            c.arm(false, PhoenixGestures.CHARGE_O[0], PhoenixGestures.CHARGE_O[1], PhoenixGestures.CHARGE_O[2], w);
            c.tremble(true, 2.9F, 1.7F, w);
            c.tremble(false, 2.9F, 1.7F, w);
            c.head(PhoenixGestures.CHARGE_HEAD, 0, 0, w * (1.0F - PhoenixGestures.owningTheV(st)));
            float ground = w * (1.0F - st.wAir);
            c.leg(true, 0, 4, ground);
            c.leg(false, 0, -4, ground);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    private PhoenixPoses() {
    }
}
