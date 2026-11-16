package com.kolodkingal.enhanced.power;

/**
 * Vision's VisionQuest look (3.12.1): its id, and when its eyes glow (both sides read it: the client's
 * {@code VisionQuestEyesLayer} draws the glow).
 */
public final class VisionQuest {
    public static final String LOOK = "vision_visionquest";

    private VisionQuest() {
    }

    /** Whether {@code variant} wears the VisionQuest suit: the look itself, and (3.15) the Source Code. */
    public static boolean suit(String variant) {
        return LOOK.equals(variant) || "source_code".equals(variant);
    }

    /**
     * (3.16) Whether this is Solar Jewel Vision: Vision as his Source Code, powered by the Solar Jewel in his brow, when the
     * server turns it on ({@code vision.solar_jewel.enabled}); otherwise the Source Code has Vision's powers, stronger.
     */
    public static boolean solar(PowerData data) {
        return data.power() == Power.VISION && "source_code".equals(data.variant())
                && com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded()
                && com.kolodkingal.enhanced.config.SolarJewelConfig.ENABLED.get();
    }

    /** Whether this Vision's VisionQuest eyes glow now: the look, and a power of his in use (beam, Phase, Density, the solar ones). */
    public static boolean eyesLit(PowerData data) {
        return data.power() == Power.VISION && suit(data.variant()) && !data.disguise()
                && (data.beamActive() || data.visionPhased() || data.dense() || data.is(Live.SOLAR_EYES)
                || data.is(Live.SOLAR_GATHERING) || data.is(Live.SOLAR_OVERLOAD));
    }
}
