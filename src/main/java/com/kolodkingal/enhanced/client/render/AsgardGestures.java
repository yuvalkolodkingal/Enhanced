package com.kolodkingal.enhanced.client.render;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.WEAPON;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.WEAPON_LAST;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the gestures of the two Asgardians. Loki is a trickster: daggers flicked from over the shoulder, a raised
 * hand to cast, the Jotun's roar. Thor is a god: he throws his weapon with the whole arm, holds out his hand for it,
 * is slapped by it as it comes home, kicks back as the lightning leaves his hand and roars as the God of Thunder.
 *
 * <p>Thor's arm gestures name the arm that holds his weapon {@code M} (see {@link PoseContext}); their world-elevation keys
 * ({@code mWorld}) keep the weapon pointing the same way in the world whatever the lean of his body, and {@code lead()}
 * asks for the weapon to be thrust ahead as an extension of the arm (the lead grip of {@link GripMath}).
 */
final class AsgardGestures {

    static void register() {
        loki();
        thor();
    }

    private static void loki() {
        // One dagger: the follow-through of a throw from over the shoulder.
        define(GestureId.LOKI_DAGGER)
                .key(0, S).m(-170, 0, 0).o(0, 0, -10)
                .key(2, O).mAim(LOOK, -8, -9, 0).o(20, 0, -20)
                .key(8, S).mAim(LOOK, -8, -9, 0);

        // Three in a fan: a sweep across 44 degrees, the other hand thrown with it.
        define(GestureId.LOKI_FAN)
                .key(0, S).m(-150, 22, 0).o(-20, 0, -30)
                .key(4, O).mAim(LOOK, -4, -22, 0).o(-60, 15, -30)
                .key(12, S).mAim(LOOK, -4, -22, 0);

        // A spell: a hand raised, fingers spread, trembling.
        define(GestureId.LOKI_CAST)
                .key(0, S).m(-10, 0, 6)
                .key(3, O).m(-125, 14, 17).mTremble(2.3F, 0.9F).o(-20, 0, -25)
                .key(9, S).m(-125, 14, 17).mTremble(2.3F, 0.9F)
                .key(14, S).m(-10, 0, 6);

        // The Jotun roars: arms flung wide, chest out, head back, then the wide stance.
        define(GestureId.LOKI_FROST_ON)
                .key(0, S).m(-10, 0, 8).o(-10, 0, -8).head(0, 0, 0).lean(0)
                .key(6, O).m(-20, 0, 50).o(-20, 0, -50).head(-12, 0, 0).lean(-6)
                .key(14, S).m(-20, 0, 50).o(-20, 0, -50)
                .key(20, S).m(-5, 0, StancePoses.LOKI_FROST_Z).o(-5, 0, -StancePoses.LOKI_FROST_Z).head(0, 0, 0).lean(0);

        // He shrinks back: a hunch.
        define(GestureId.LOKI_FROST_OFF)
                .key(0, S).m(-5, 0, StancePoses.LOKI_FROST_Z).o(-5, 0, -StancePoses.LOKI_FROST_Z).head(0, 0, 0)
                .key(5, O).m(5, 0, 4).o(5, 0, -4).head(6, 0, 0).lean(4)
                .key(12, S).m(0, 0, 6).o(0, 0, -6).head(0, 0, 0).lean(0);
    }

    private static void thor() {
        // The throw: the weapon arm hauled back over the shoulder, then whipped forward with the whole body.
        define(GestureId.THOR_THROW)
                .key(0, S).mWorld(150, -6, 0).lead().twist(-14).lean(0)
                .key(2, O).mAim(LOOK, 0, 0, 0).twist(10).lean(6)
                .key(5, S).mAim(LOOK, 0, 0, 0)
                .key(12, S).mWorld(60, 0, 0).twist(0).lean(0);

        // Calling the weapon: the hand opens toward it, the other arm out for balance.
        define(GestureId.THOR_SUMMON)
                .key(0, S).mAim(WEAPON, -10, 0, 0).oAdd(0, 0, -12)
                .key(10, S).mAim(WEAPON, 0, 0, 0).oAdd(0, 0, -12);

        // The weapon out of his inventory: drawn from the hip and swung up.
        define(GestureId.THOR_RECALL_DRAW)
                .key(0, S).m(10, 0, 10)
                .key(4, O).mWorld(120, 0, 0).lead()
                .key(8, S).mWorld(60, 0, 0);

        // The catch: the weapon slaps into his hand and the arm absorbs it (sparks at the hand: GestureEffects).
        define(GestureId.THOR_CATCH)
                .key(0, S).mAim(WEAPON_LAST, 0, 0, 0).lead().lean(0).twist(0)
                .key(2, O).mAim(WEAPON_LAST, 25, 0, 0).lean(-4).twist(-6)
                .key(5, S).mAim(WEAPON_LAST, 8, 0, 0).lean(0).twist(0)
                .key(14, S).mWorld(60, 0, 0);

        // The lightning leaves his hand: the arm kicks up, the body twists, then the arm points down the look.
        kick(GestureId.THOR_BEAM_KICK, 1.7F);
        kick(GestureId.THOR_BEAM_KICK_BARE, 1.0F);

        // "I am the God of Thunder!": arms flung out, chest out, head back.
        define(GestureId.THOR_UNLEASH_ON)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8).head(0, 0, 0).lean(0)
                .key(6, O).m(-35, 0, 70).o(-35, 0, -70).head(-18, 0, 0).lean(-8)
                .key(14, S).m(-35, 0, 70).o(-35, 0, -70).head(-18, 0, 0).lean(-8)
                .key(20, S).m(-6, 0, StancePoses.THOR_UNLEASHED_Z).o(-6, 0, -StancePoses.THOR_UNLEASHED_Z).head(-4, 0, 0).lean(0);
        // An exhale: the arms drop.
        define(GestureId.THOR_UNLEASH_OFF)
                .key(0, S).m(-6, 0, StancePoses.THOR_UNLEASHED_Z).o(-6, 0, -StancePoses.THOR_UNLEASHED_Z).head(-4, 0, 0)
                .key(5, O).m(8, 0, 6).o(8, 0, -6).head(4, 0, 0)
                .key(10, S).m(8, 0, 6).o(8, 0, -6).head(4, 0, 0);
    }

    /** The kick of the beam: {@code pw} is the beam's strength (1.7 with the weapon, 1 bare-handed). */
    private static void kick(GestureId id, float pw) {
        define(id)
                .key(0, S).mAim(LOOK, 10 * pw, 0, 0).twist(-12 * pw).lean(-5 * pw)
                .key(2, O).mAim(LOOK, 0, 0, 0).twist(StancePoses.THOR_BEAM_TWIST).lean(0)
                .key(8, S).mAim(LOOK, 0, 0, 0).twist(StancePoses.THOR_BEAM_TWIST);
    }

    private AsgardGestures() {
    }
}
