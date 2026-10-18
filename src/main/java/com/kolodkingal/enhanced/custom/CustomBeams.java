package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The beam of a custom hero (3.3) in his own colours, on the shared beam system ({@code ContinuousBeam}, the
 * {@code BeamPayload} and the renderer): a {@link BeamStyle} for each pair of colours, named on the wire
 * {@code custom_RRGGBB_RRGGBB}. The server registers it when the beam starts, and a client that meets a name it has
 * not seen makes the same style from the name ({@link #parse}, which {@code BeamStyles.byId} asks), so any colour works
 * without a list.
 */
public final class CustomBeams {
    private static final String PREFIX = "custom_";

    /** The style of a beam of {@code color} with a {@code glow} core, registered (once) and returned. */
    public static BeamStyle style(int color, int glow) {
        String id = String.format(Locale.ROOT, PREFIX + "%06x_%06x", color & 0xFFFFFF, glow & 0xFFFFFF);
        BeamStyle known = BeamStyles.byId(id);
        return known != null ? known : BeamStyles.register(make(id, color, glow));
    }

    /** The style a {@code custom_RRGGBB_RRGGBB} name stands for, or null if it is not one. */
    @Nullable
    public static BeamStyle parse(String id) {
        if (!id.startsWith(PREFIX) || id.length() != PREFIX.length() + 13 || id.charAt(PREFIX.length() + 6) != '_') {
            return null;
        }
        try {
            int color = Integer.parseInt(id.substring(PREFIX.length(), PREFIX.length() + 6), 16);
            int glow = Integer.parseInt(id.substring(PREFIX.length() + 7), 16);
            return make(id, color, glow);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A palette from two colours: a core whitened from the glow, the glow, the colour, and two deeper shades of it. */
    private static BeamStyle make(String id, int color, int glow) {
        return new BeamStyle(id, "beam.enhanced.custom", mix(glow, 0xFFFFFF, 0.65F), glow, color, mix(color, 0x000000, 0.3F),
                mix(color, 0x000000, 0.55F), false);
    }

    /** {@code a} moved {@code t} of the way to {@code b}. */
    public static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private CustomBeams() {
    }
}
