package com.kolodkingal.enhanced.custom;

import java.util.List;

/**
 * The styles of the optional layers of a custom hero's look (3.4), as plain strings for the rules and the tests; the
 * client's {@code CustomLooksLayer} draws them from the mod's existing art. Every list starts with {@link CustomHero#NONE}.
 * Names: {@code look.enhanced.<kind>.<style>}.
 */
public final class LookCatalog {
    public static final List<String> CAPES = List.of(
            "none", "plain", "magneto", "scarlet_witch", "thor", "thor_ragnarok", "thor_mighty", "thor_love_and_thunder", "loki",
            "loki_god_of_stories", "vision", "white_vision", "billy_wiccan", "billy_comics", "billy_halloween", "apocalypse");
    public static final List<String> HATS = List.of("none", "witch_hat", "billy_crown", "cerebro", "magneto_helmet", "wanda_crown");
    public static final List<String> VISORS = List.of(
            "none", "cyclops_classic", "cyclops_apocalypse", "ruby_quartz", "quicksilver_goggles", "domino_mask", "full_mask");
    public static final List<String> EYES = List.of("none", "glow", "lightning", "fire", "optic", "machine", "black");
    public static final List<String> AURAS = List.of("none", "energy", "flames", "sand", "sparks", "mist");
    /** When the eyes and the aura show. */
    public static final List<String> WHEN = List.of("always", "powers", "flying");
    /** The styles that take the layer's colour (the others have their own). */
    public static final List<String> TINTED = List.of(
            "plain", "witch_hat", "billy_crown", "domino_mask", "full_mask", "glow", "energy", "flames", "sparks", "mist");
    /** The rows of the face the eyes may sit on (0-3). */
    public static final int EYE_ROWS = 4;

    /** The styles of the layer kind {@code i} (the order of {@link CustomHero.Layers#KINDS}). */
    public static List<String> styles(int kind) {
        return switch (kind) {
            case 0 -> CAPES;
            case 1 -> HATS;
            case 2 -> VISORS;
            case 3 -> EYES;
            default -> AURAS;
        };
    }

    /** Whether the layer kind {@code i} has a "when" (the eyes and the aura). */
    public static boolean hasWhen(int kind) {
        return kind == 3 || kind == 4;
    }

    private LookCatalog() {
    }
}
