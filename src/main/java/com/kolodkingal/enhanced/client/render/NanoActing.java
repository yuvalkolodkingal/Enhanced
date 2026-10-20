package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * How the nanotech suits and the Hulkbuster act what they do (3.8, lane L4): four stances from the synced bits ({@link
 * Live#IRON_SHIELD}: the left arm up before him behind the shield; {@link Live#IRON_BLADES}: a low guard, the fists
 * out; {@link Live#IRON_CHARGING}: both forearms forward, merged into the nano cannon; {@link Live#IRON_GRABBING}: the
 * Hulkbuster's right fist held up before him with what it grabbed), the chest-out pose of the Unibeam's stance for the
 * chest laser and the heavy Unibeam too, and what they add to the hands in first person. The slashes, the punches and
 * the throws swing the arms by the game's own swing. Lane L3's {@link IronManClient} asks this class beside its own
 * {@link IronManActing}.
 */
public final class NanoActing {
    /** The arms' poses (degrees: x forward-up is negative, y, z). */
    public static final float[] SHIELD_O = {-82, 12, -6};
    public static final float[] SHIELD_M = {-14, 0, 12};
    public static final float[] GUARD_M = {-34, -12, 14};
    public static final float[] GUARD_O = {-34, 12, -14};
    public static final float[] CANNON_M = {-86, -22, 0};
    public static final float[] CANNON_O = {-86, 22, 0};
    public static final float[] GRAB_M = {-96, -6, 8};

    private NanoActing() {
    }

    /** Whether the player's beam is one of the wide chest beams of lane L4 (the chest laser, the heavy Unibeam). */
    public static boolean chestBeam(AbstractClientPlayer player, PowerData data) {
        if (!data.is(Live.BEAMING)) {
            return false;
        }
        BeamStyle style = BeamRenderer.styleOf(player.getId());
        return style == AvengersBeams.CHEST_LASER || style == AvengersBeams.HULKBUSTER_UNIBEAM;
    }

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        if (data.power() != Power.IRON_MAN && data.power() != Power.CUSTOM) {
            return;
        }
        boolean phase = IronSuits.state(p).inPhase();
        on[Stance.NANO_SHIELD.ordinal()] = !phase && data.is(Live.IRON_SHIELD);
        on[Stance.NANO_BLADES.ordinal()] = !phase && data.is(Live.IRON_BLADES) && !data.is(Live.IRON_SHIELD);
        on[Stance.NANO_CANNON.ordinal()] = !phase && data.is(Live.IRON_CHARGING);
        on[Stance.HULKBUSTER_GRAB.ordinal()] = !phase && data.is(Live.IRON_GRABBING)
                && IronSuits.worn(p) == SuitKind.HULKBUSTER;
        if (!phase && chestBeam(p, data)) {
            on[Stance.IRON_UNIBEAM.ordinal()] = true;
        }
    }

    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.NANO_SHIELD);
        if (w > 0.001F) {
            c.arm(false, SHIELD_O[0], SHIELD_O[1], SHIELD_O[2], w);
            c.arm(true, SHIELD_M[0], SHIELD_M[1], SHIELD_M[2], w * 0.6F);
            c.occupy(false, w);
        }
        if ((w = c.st.stance(Stance.NANO_BLADES)) > 0.001F) {
            float sway = 3.0F * Mth.sin(c.age * 0.12F);
            c.arm(true, GUARD_M[0] + sway, GUARD_M[1], GUARD_M[2], w);
            c.arm(false, GUARD_O[0] - sway, GUARD_O[1], GUARD_O[2], w);
        }
        if ((w = c.st.stance(Stance.NANO_CANNON)) > 0.001F) {
            float hum = 1.5F * Mth.sin(c.age * 1.7F);
            c.arm(true, CANNON_M[0] + hum, CANNON_M[1], CANNON_M[2], w);
            c.arm(false, CANNON_O[0] + hum, CANNON_O[1], CANNON_O[2], w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        if ((w = c.st.stance(Stance.HULKBUSTER_GRAB)) > 0.001F) {
            c.arm(true, GRAB_M[0], GRAB_M[1], GRAB_M[2], w);
            c.occupy(true, w);
        }
    }

    /** What the stances add to his hands in first person. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.NANO_SHIELD)) > 0.001F) {
            o.add(w, 0.18F, 0.12F, -0.12F, -20, -25, 10);
        }
        if ((w = s.stance(Stance.NANO_CANNON)) > 0.001F) {
            m.add(w, -0.12F, 0.08F, -0.10F, -8, 12, 0);
            o.add(w, -0.12F, 0.08F, -0.10F, -8, 12, 0);
        }
        if ((w = s.stance(Stance.HULKBUSTER_GRAB)) > 0.001F) {
            m.add(w, -0.05F, 0.15F, -0.10F, -15, 0, 0);
        }
    }
}
