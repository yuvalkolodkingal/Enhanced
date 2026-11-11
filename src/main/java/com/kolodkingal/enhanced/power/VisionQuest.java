package com.kolodkingal.enhanced.power;

/**
 * Vision's VisionQuest look (3.12.1): its id, and when its eyes glow (both sides read it: the client's
 * {@code VisionQuestEyesLayer} draws the glow).
 */
public final class VisionQuest {
    public static final String LOOK = "vision_visionquest";

    private VisionQuest() {
    }

    /** Whether this Vision's VisionQuest eyes glow now: the look, and a power of his in use (beam, Phase, Density). */
    public static boolean eyesLit(PowerData data) {
        return data.power() == Power.VISION && LOOK.equals(data.variant()) && !data.disguise()
                && (data.beamActive() || data.visionPhased() || data.dense());
    }
}
