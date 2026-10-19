package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;

/**
 * The looks of the stones' beams (3.6) on the shared beam system ({@code ContinuousBeam}, {@code BeamPayload}, the
 * renderer): the Mind Stone's pale gold beam (Vision's) and the Power Stone's thick purple one. Both sides load this
 * class when the mod starts (the server's stone events, the client's setup), so a beam named on the wire is known.
 */
public final class StoneBeams {
    /** The Mind Stone's beam: Vision's pale gold, a white-gold core. */
    public static final BeamStyle MIND = BeamStyles.register(new BeamStyle("mind_stone", "beam.enhanced.mind_stone",
            0xFFF6D5, 0xFFEBA8, 0xF5D77A, 0xD9A93A, 0x8A6418, false));
    /** The Power Stone's beam: violet with a pale lilac core, half as wide again as the others. */
    public static final BeamStyle POWER = BeamStyles.register(new BeamStyle("power_stone", "beam.enhanced.power_stone",
            0xE6D2FF, 0xC9A2FF, 0x9B4DEB, 0x6A22B8, 0x330D66, false));

    /** Loads the styles (they register themselves). */
    public static void init() {
    }

    private StoneBeams() {
    }
}
