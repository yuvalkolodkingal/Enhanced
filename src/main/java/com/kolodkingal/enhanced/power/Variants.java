package com.kolodkingal.enhanced.power;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every hero's looks, in the order the Change Look key cycles through them. The first one is the default. Heroes
 * without a list (Magneto, the Scarlet Witch, Quicksilver) keep their single suit, which they put on with their
 * helmet, crown or goggles.
 */
public final class Variants {
    private static final Map<Power, List<Variant>> BY_POWER = new EnumMap<>(Power.class);

    static {
        add(Power.VISION,
                new Variant("vision", Power.VISION, Variant.suit("vision"), Variant.suit("vision_cape"), false, Power.VISION.color, Power.VISION.glow),
                new Variant("white_vision", Power.VISION, Variant.suit("white_vision"), Variant.suit("white_vision_cape"), false, 0xA6E2F0, 0xE8FBFF));

        add(Power.BILLY,
                new Variant("wiccan", Power.BILLY, Variant.suit("billy_wiccan"), Variant.suit("billy_wiccan_cape"), false, Power.BILLY.color, Power.BILLY.glow),
                new Variant("teen", Power.BILLY, Variant.suit("billy_teen"), null, false, Power.BILLY.color, Power.BILLY.glow),
                new Variant("halloween", Power.BILLY, Variant.suit("billy_halloween"), Variant.suit("billy_halloween_cape"), false, Power.BILLY.color, Power.BILLY.glow),
                new Variant("comics", Power.BILLY, Variant.suit("billy_comics"), Variant.suit("billy_comics_cape"), false, Power.BILLY.color, Power.BILLY.glow),
                own(Power.BILLY));

        add(Power.TOMMY,
                new Variant("halloween", Power.TOMMY, Variant.suit("tommy_halloween"), null, false, Power.TOMMY.color, Power.TOMMY.glow),
                new Variant("tracksuit", Power.TOMMY, Variant.suit("tommy_tracksuit"), null, false, Power.TOMMY.color, Power.TOMMY.glow),
                new Variant("speed", Power.TOMMY, Variant.suit("tommy_speed"), null, false, Power.TOMMY.color, Power.TOMMY.glow),
                own(Power.TOMMY));

        add(Power.LOKI,
                new Variant("loki", Power.LOKI, Variant.suit("loki"), Variant.suit("loki_cape"), false, Power.LOKI.color, Power.LOKI.glow),
                new Variant("tva", Power.LOKI, Variant.suit("loki_tva"), null, false, 0xC9A56B, 0xC8824B),
                new Variant("god_of_stories", Power.LOKI, Variant.suit("loki_god_of_stories"), Variant.suit("loki_god_of_stories_cape"), false, 0x82A659, 0xC9A54A),
                own(Power.LOKI));

        add(Power.THOR,
                new Variant("thor", Power.THOR, Variant.suit("thor"), Variant.suit("thor_cape"), false, Power.THOR.color, Power.THOR.glow),
                new Variant("ragnarok", Power.THOR, Variant.suit("thor_ragnarok"), Variant.suit("thor_ragnarok_cape"), false, Power.THOR.color, Power.THOR.glow),
                new Variant("love_and_thunder", Power.THOR, Variant.suit("thor_love_and_thunder"), Variant.suit("thor_love_and_thunder_cape"), false, Power.THOR.color, Power.THOR.glow),
                new Variant("love_and_thunder_helmet", Power.THOR, Variant.suit("thor_love_and_thunder_helmet"), Variant.suit("thor_love_and_thunder_cape"), false, Power.THOR.color, Power.THOR.glow),
                new Variant("mighty_thor", Power.THOR, Variant.suit("thor_mighty"), Variant.suit("thor_mighty_cape"), true, Power.THOR.color, Power.THOR.glow),
                own(Power.THOR));

        // The heroes of 3.0. Their skins are flat placeholders until the art of each lane lands (see DESIGN section 13
        // to 17).
        add(Power.PHOENIX,
                new Variant("apocalypse", Power.PHOENIX, Variant.suit("phoenix_apocalypse"), null, true, 0xFF8A1F, 0xFFD65A),
                new Variant("brand_new_day", Power.PHOENIX, Variant.suit("phoenix_brand_new_day"), null, true, 0xF2B632, 0xC4E88A),
                own(Power.PHOENIX));

        add(Power.SPIDER_MAN,
                new Variant("homemade", Power.SPIDER_MAN, Variant.suit("spider_homemade"), null, true, 0xC9232D, 0xB9CDE8),
                new Variant("iron_spider", Power.SPIDER_MAN, Variant.suit("spider_iron"), null, true, 0xC9A66B, 0xE7D8A8),
                new Variant("brand_new_day", Power.SPIDER_MAN, Variant.suit("spider_bnd"), null, true, 0xD9381E, 0x5BB6D6),
                own(Power.SPIDER_MAN));

        add(Power.THANOS,
                new Variant("infinity_war", Power.THANOS, Variant.suit("thanos_infinity_war"), null, false, 0x9B4DEB, 0xE6D2FF),
                new Variant("endgame", Power.THANOS, Variant.suit("thanos_endgame"), null, false, 0x9B4DEB, 0xE6D2FF),
                own(Power.THANOS));

        add(Power.ULTRON,
                new Variant("prime", Power.ULTRON, Variant.suit("ultron_prime"), null, false, 0xFFA23A, 0xFFE9B8),
                new Variant("sentry", Power.ULTRON, Variant.suit("ultron_sentry"), null, false, 0x45B4F5, 0xD8F0FF),
                new Variant("human", Power.ULTRON, Variant.suit("ultron_human"), null, false, 0xE23B2E, 0xFFD6CF),
                own(Power.ULTRON));

        add(Power.APOCALYPSE,
                new Variant("apocalypse", Power.APOCALYPSE, Variant.suit("apocalypse"), Variant.suit("apocalypse_cape"), false, 0xF2B84B, 0xFFF0B8),
                new Variant("en_sabah_nur", Power.APOCALYPSE, Variant.suit("apocalypse_ancient"), null, false, 0xD9A93A, 0x7ADBC6),
                own(Power.APOCALYPSE));

        // ===== cyclops (3.3) =====
        // X-Men: Apocalypse's dark blue and black field suit, and the comics' blue and yellow. The visor is not in the skins:
        // it is his glasses, drawn by their own layer while they are on.
        add(Power.CYCLOPS,
                new Variant("apocalypse", Power.CYCLOPS, Variant.suit("cyclops_apocalypse"), null, false, 0xE8323A, 0xFFB3A8),
                new Variant("classic", Power.CYCLOPS, Variant.suit("cyclops_classic"), null, false, 0xF0303A, 0xFFD27A),
                own(Power.CYCLOPS));
    }

    private static void add(Power power, Variant... variants) {
        BY_POWER.put(power, List.of(variants));
    }

    /** "Your own skin": the hero's powers and colours, the player's own look. */
    private static Variant own(Power power) {
        return new Variant("own", power, null, null, false, power.color, power.glow);
    }

    /** The hero's looks, or an empty list if it has only its one suit. */
    public static List<Variant> of(Power power) {
        return BY_POWER.getOrDefault(power, List.of());
    }

    /** Whether the Change Look key does anything for this hero. */
    public static boolean hasChoice(Power power) {
        return of(power).size() > 1;
    }

    /** The look with this id, falling back to the hero's default look (or a plain one for heroes without looks). */
    public static Variant get(Power power, String id) {
        List<Variant> list = of(power);
        for (Variant v : list) {
            if (v.id().equals(id)) {
                return v;
            }
        }
        return list.isEmpty() ? plain(power) : list.get(0);
    }

    /** The look {@code step} places after (or, negative, before) {@code id} in the cycle. */
    public static Variant step(Power power, String id, int step) {
        List<Variant> list = of(power);
        if (list.isEmpty()) {
            return plain(power);
        }
        int index = list.indexOf(get(power, id));
        return list.get(Math.floorMod(index + step, list.size()));
    }

    /** The current look of a player's power data. */
    public static Variant current(PowerData data) {
        return get(data.power(), data.variant());
    }

    /** A single plain look per hero without looks (built up front: both the server and the client thread read it). */
    private static final Map<Power, Variant> PLAIN = new EnumMap<>(Power.class);

    static {
        for (Power power : Power.values()) {
            PLAIN.put(power, new Variant("default", power, null, null, false, power.color, power.glow));
        }
    }

    private static Variant plain(Power power) {
        return PLAIN.get(power);
    }

    private Variants() {
    }
}
