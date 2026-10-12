package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.resources.ResourceLocation;

/**
 * The lights of the Ultron bodies and the masks they are drawn through: which texture, and in what colour and how bright
 * a light burns for a pose. The boss and the Sentries share the model and the layers; the Sentry's lights are cyan and
 * take the colour of its eyes.
 */
final class UltronLights {
    /** The masks (the size of the skin, white where the light is): the eyes and mouth, the light in the torso, the fingertips, the palms. */
    static final ResourceLocation EYES = Enhanced.id("textures/entity/ultron_eyes.png");
    static final ResourceLocation TORSO = Enhanced.id("textures/entity/ultron_torso.png");
    static final ResourceLocation FINGERS = Enhanced.id("textures/entity/ultron_fingers.png");
    static final ResourceLocation PALMS = Enhanced.id("textures/entity/ultron_palms.png");
    static final ResourceLocation SENTRY_EYES = Enhanced.id("textures/entity/ultron_sentry_eyes.png");

    /** The skins of Prime, the Hybrid and the Sentry-Ultrons, and of a Sentry. */
    static final ResourceLocation[] BOSS_SKINS = {
            Enhanced.id("textures/entity/ultron.png"),
            Enhanced.id("textures/entity/ultron_form1.png"),
            Enhanced.id("textures/entity/ultron_form2.png")};
    static final ResourceLocation SENTRY_SKIN = Enhanced.id("textures/entity/ultron_sentry.png");

    static int eyes(UltronPose p) {
        return UltronGlowLayer.color(p.get(UltronPose.EYE_R), p.get(UltronPose.EYE_G), p.get(UltronPose.EYE_B), p.get(UltronPose.EYES));
    }

    /** The light in his torso: red, as red as the lights of his eyes get when he is angry. */
    static int torso(UltronPose p) {
        return UltronGlowLayer.color(1.0F, 0.25F, 0.15F, p.get(UltronPose.TORSO_GLOW));
    }

    /** The Sentry's lines: the colour of its eyes, dimly. */
    static int sentryTorso(UltronPose p) {
        return UltronGlowLayer.color(p.get(UltronPose.EYE_R), p.get(UltronPose.EYE_G), p.get(UltronPose.EYE_B), 0.6F * p.get(UltronPose.EYES));
    }

    /** Fingertips: amber-white when an attack is armed. */
    static int fingers(UltronPose p) {
        return UltronGlowLayer.color(1.0F, 0.82F, 0.45F, p.get(UltronPose.FINGER_GLOW));
    }

    /** The Sentry's palm cannon: cyan. */
    static int sentryFingers(UltronPose p) {
        return UltronGlowLayer.color(0.27F, 0.71F, 0.96F, p.get(UltronPose.FINGER_GLOW));
    }

    /** Palms: the pale blue of the anti-gravity tech. */
    static int palms(UltronPose p) {
        return UltronGlowLayer.color(0.62F, 0.85F, 1.0F, p.get(UltronPose.PALM_GLOW));
    }

    private UltronLights() {
    }
}
