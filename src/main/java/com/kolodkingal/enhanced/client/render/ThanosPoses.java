package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.Gestures.Target;
import net.minecraft.util.Mth;

/**
 * The pose rows of the playing Thanos's two stances ({@link Stance#THANOS_VEIL}, {@link Stance#THANOS_DRAIN}). Written
 * for a right-handed player (see {@link PoseContext}): {@code M} is the main arm and {@code O} the arm the Infinity
 * Gauntlet is on. The poses the gestures hand over to are in {@link ThanosGestures}, which the game tests can read.
 */
final class ThanosPoses {

    /**
     * The Reality Veil: the main arm spread a little (added to what it does, so he can walk and fight with the Veil
     * up), the Gauntlet hand held out and up as if it held a thin film, trembling with it, the head a little back, and
     * a slow sway of a degree and a half.
     */
    static void ambient(PoseContext c) {
        float w = c.st.stance(Stance.THANOS_VEIL);
        if (w > 0.001F) {
            float sway = EnhancedClientConfig.reducedMotion() ? 0.0F : 1.5F * Mth.sin(0.05F * c.age);
            c.armAdd(true, 0, 0, ThanosGestures.VEIL_M_Z + sway, w);
            float[] o = ThanosGestures.VEIL_O;
            c.arm(false, o[0] + sway, o[1], o[2], w);
            c.tremble(false, 0.8F, 1.6F, w);
            c.head(ThanosGestures.VEIL_HEAD, 0, 0, w);
        }
    }

    /**
     * The Soul Drain: the Gauntlet arm along his look (the one he drains is where he looks), trembling with the chain's
     * pull; the main arm braced back and out; the head down a little; the legs braced wide. The Gauntlet arm is held by
     * it, so the swing of an attack is mostly muted on that arm.
     */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w = st.stance(Stance.THANOS_DRAIN);
        if (w > 0.001F) {
            c.aim(false, Target.LOOK, 0, 0, 0, w);
            c.tremble(false, 1.4F, 1.9F, w);
            float[] m = ThanosGestures.DRAIN_M;
            c.arm(true, m[0], m[1], m[2], w);
            c.head(ThanosGestures.DRAIN_HEAD, 0, 0, w);
            float ground = w * (1.0F - st.wAir);
            c.leg(true, ThanosGestures.DRAIN_LEG[0], ThanosGestures.DRAIN_LEG[1], ground);
            c.leg(false, -ThanosGestures.DRAIN_LEG[0], -ThanosGestures.DRAIN_LEG[1], ground);
            c.occupy(false, w);
        }
    }

    private ThanosPoses() {
    }
}
