package com.kolodkingal.enhanced.beam;

/**
 * The beams of 3.8 (PLAN 12.2): Iron Man's Unibeam, the Mark 85's chest laser, the Hulkbuster's heavy Unibeam, the
 * drones' laser flash, Love's two violet eye beams and the Mark 1's flamethrower (drawn by particles, no beam body).
 * Registered by the 3.8 skeleton with placeholder palettes (their widths, 0.35 / 0.75 / 1.0, are the lanes' to draw);
 * lanes L3, L4 and L8 tune them. {@link #init} is called once at mod construction, on both sides.
 */
public final class AvengersBeams {
    public static final BeamStyle UNIBEAM = BeamStyles.register(new BeamStyle("unibeam", "beam.enhanced.unibeam",
            0xFFFFFF, 0xE8FBFF, 0x9FE8FF, 0x4FB8E8, 0x1E6FA0, false));
    public static final BeamStyle CHEST_LASER = BeamStyles.register(new BeamStyle("chest_laser",
            "beam.enhanced.chest_laser", 0xFFFFFF, 0xE0FFFF, 0x8FF0FF, 0x3FC8E8, 0x1A7FA8, false));
    public static final BeamStyle HULKBUSTER_UNIBEAM = BeamStyles.register(new BeamStyle("hulkbuster_unibeam",
            "beam.enhanced.hulkbuster_unibeam", 0xFFFFFF, 0xE8FBFF, 0xA8E8FF, 0x58B8E8, 0x2468A0, false));
    public static final BeamStyle DRONE_LASER = BeamStyles.register(new BeamStyle("drone_laser",
            "beam.enhanced.drone_laser", 0xFFFFFF, 0xFFE8B0, 0xFFB040, 0xD8323C, 0x8C1418, false));
    public static final BeamStyle LOVE_EYES = BeamStyles.register(new BeamStyle("love_eyes", "beam.enhanced.love_eyes",
            0xFFFFFF, 0xF0D8FF, 0xC08CFF, 0x9A5CE8, 0x5A2A9C, false));
    public static final BeamStyle FLAMETHROWER = BeamStyles.register(new BeamStyle("flamethrower",
            "beam.enhanced.flamethrower", 0xFFF8D0, 0xFFE070, 0xFFA030, 0xE85A10, 0x8C2A08, false));

    /** Loads the class (its styles register themselves). */
    public static void init() {
    }

    private AvengersBeams() {
    }
}
