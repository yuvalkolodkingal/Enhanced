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

    /** The fixed colour of each eye style that has one (the others take the layer's colour). */
    public static int eyeColor(String style) {
        return switch (style) {
            case "lightning" -> 0xCFEFFF;
            case "fire" -> 0xFFA43A;
            case "optic" -> 0xFF2A2A;
            case "machine" -> 0xFF5A1F;
            case "black" -> 0x000000;
            default -> -1;
        };
    }

    /**
     * The textures a style draws with (paths under {@code assets/enhanced/}), from the mod's own art and the few made for
     * the layers ({@code textures/entity/custom/}); a style drawn as geometry only (Billy's crown) has none.
     */
    public static List<String> textures(int kind, String style) {
        if (CustomHero.NONE.equals(style)) {
            return List.of();
        }
        return switch (kind) {
            case 0 -> List.of("plain".equals(style) ? CUSTOM + "cape_plain.png" : ENTITY + "suit/" + style + "_cape.png");
            case 1 -> switch (style) {
                case "witch_hat" -> List.of(CUSTOM + "witch_hat.png");
                case "cerebro" -> List.of(ENTITY + "cerebro_helmet.png");
                case "magneto_helmet", "wanda_crown" -> List.of(ARMOR + style + "_layer_1.png");
                default -> List.of();
            };
            case 2 -> switch (style) {
                case "cyclops_classic", "cyclops_apocalypse" -> List.of(ENTITY + "cyclops_visor_" + style.substring(8) + ".png",
                        ENTITY + "cyclops_visor_" + style.substring(8) + "_glow.png");
                case "ruby_quartz" -> List.of(ENTITY + "ruby_quartz_glasses_worn.png", ENTITY + "ruby_quartz_glasses_worn_glow.png");
                case "quicksilver_goggles" -> List.of(ARMOR + "quicksilver_goggles_layer_1.png");
                case "domino_mask" -> List.of(CUSTOM + "mask_domino.png");
                default -> List.of(CUSTOM + "mask_full.png", CUSTOM + "mask_full_lenses.png");
            };
            case 3 -> List.of(CUSTOM + "eyes_row0.png", CUSTOM + "eyes_row1.png", CUSTOM + "eyes_row2.png", CUSTOM + "eyes_row3.png");
            default -> switch (style) {
                case "flames" -> List.of(ENTITY + "phoenix_flame.png");
                case "sand" -> List.of(ENTITY + "sand_swirl.png");
                default -> List.of(ENTITY + "energy_swirl.png");
            };
        };
    }

    private static final String ENTITY = "textures/entity/";
    private static final String CUSTOM = ENTITY + "custom/";
    private static final String ARMOR = "textures/models/armor/";

    /** Whether the layer kind {@code i} has a "when" (the eyes and the aura). */
    public static boolean hasWhen(int kind) {
        return kind == 3 || kind == 4;
    }

    private LookCatalog() {
    }
}
