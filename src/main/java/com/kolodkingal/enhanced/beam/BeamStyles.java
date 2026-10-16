package com.kolodkingal.enhanced.beam;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every {@link BeamStyle} there is, by id: the client finds the style a {@code BeamPayload} names here, and the HUD
 * and the tests list them. A source of beams registers its style once, in a static initialiser the mod loads (the same
 * on both sides).
 */
public final class BeamStyles {
    private static final Map<String, BeamStyle> BY_ID = new LinkedHashMap<>();

    /** Billy's Hex Beam: blue, with a white-hot core (the colours of his magic, as {@code BillyAbilities} names them). */
    public static final BeamStyle BILLY = register(new BeamStyle("billy_hex", "beam.enhanced.billy_hex",
            0xFFFFFF, 0xCFEFFF, 0x5CC8FF, 0x2F8CFF, 0x1B4FCC, false));

    /** Adds a style (a second registration of the same id replaces the first) and returns it. */
    public static synchronized BeamStyle register(BeamStyle style) {
        BY_ID.put(style.id(), style);
        return style;
    }

    /** The style of this id, or null. */
    @Nullable
    public static synchronized BeamStyle byId(String id) {
        return BY_ID.get(id);
    }

    /** Every style, in the order they were registered. */
    public static synchronized Collection<BeamStyle> all() {
        return java.util.List.copyOf(BY_ID.values());
    }

    private BeamStyles() {
    }
}
