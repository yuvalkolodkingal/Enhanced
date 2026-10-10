package com.kolodkingal.enhanced.power;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every hero's looks, in the order the Change Look key cycles through them. The first one is the default. Heroes
 * without a list (Magneto, the Scarlet Witch, Photon, Quicksilver) keep their single suit, which they put on with
 * their helmet, crown or goggles.
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
                new Variant("mighty_thor", Power.THOR, Variant.suit("thor_mighty"), Variant.suit("thor_mighty_cape"), true, Power.THOR.color, Power.THOR.glow),
                own(Power.THOR));
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
