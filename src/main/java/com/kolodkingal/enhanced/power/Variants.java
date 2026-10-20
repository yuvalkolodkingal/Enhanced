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
        // The hero of 3.3 (lane XAVIER): Charles Xavier young (First Class, Days of Future Past: he walks, thanks to
        // Hank's serum) and old (the professor in his wheelchair); he chooses one when he awakens.
        add(Power.PROFESSOR_X,
                new Variant("young", Power.PROFESSOR_X, Variant.suit("xavier_young"), null, false, 0x6F8CFF, 0xDCE4FF),
                new Variant("old", Power.PROFESSOR_X, Variant.suit("xavier_old"), null, false, 0x8FA6FF, 0xF2F5FF),
                own(Power.PROFESSOR_X));
        // 3.3. Agatha of WandaVision (her witch's dress, the brooch at the throat; Agnes no more) and of Agatha All Along
        // (the Witches' Road: dark green and black, and the hat, which AgathaHatLayer draws).
        add(Power.AGATHA,
                new Variant("wandavision", Power.AGATHA, Variant.suit("agatha_wandavision"), null, true, 0x9A3FE0, 0xE2C2FF),
                new Variant("all_along", Power.AGATHA, Variant.suit("agatha_all_along"), null, true, 0x8E4FD6, 0xB8E6A8),
                own(Power.AGATHA));

        // ===== heimdall (3.5) =====
        // The Guardian of the Bifrost in his golden armour (Thor, The Dark World; the horned helmet is a layer of his lane),
        // and the hooded cloak he wears in hiding (Ragnarok). Placeholder skins until lane L2's art lands.
        add(Power.HEIMDALL,
                new Variant("guardian", Power.HEIMDALL, Variant.suit("heimdall_guardian"), null, false, 0xE8B54A, 0xFFE7A3),
                new Variant("ragnarok", Power.HEIMDALL, Variant.suit("heimdall_ragnarok"), null, false, 0xB89A52, 0xFFE7A3),
                own(Power.HEIMDALL));
        // ===== odin (3.5) =====
        // The Allfather on his throne (armour, red cape, the winged helmet of his lane's layer) and the Wanderer in a long
        // dark coat. Placeholder skins until lane L3's art lands.
        add(Power.ODIN,
                new Variant("allfather", Power.ODIN, Variant.suit("odin_allfather"), Variant.suit("odin_cape"), false, 0xD9A63A, 0xFFF0C2),
                new Variant("wanderer", Power.ODIN, Variant.suit("odin_wanderer"), null, false, 0xC9A65A, 0xFFF0C2),
                own(Power.ODIN));
        // ===== hela (3.5) =====
        // The Goddess of Death (the black suit with emerald lines; the antlered headdress is a layer of her lane) and her
        // Arrival (a long black cloak). Placeholder skins until lane L4's art lands.
        add(Power.HELA,
                new Variant("goddess", Power.HELA, Variant.suit("hela_goddess"), null, true, 0x2E7D4F, 0x9CF2B5),
                new Variant("arrival", Power.HELA, Variant.suit("hela_arrival"), null, true, 0x24603D, 0x9CF2B5),
                own(Power.HELA));

        // ===== doctor_strange (3.6) =====
        // The Sorcerer Supreme of 2016 and Infinity War (navy tunic, the red Cloak of Levitation, the Eye on his
        // chest), the Multiverse of Madness look (the cloak with a navy patch) and the Defender, our own "other
        // universe" take. The Cloak and the Eye are layers of his lane. Placeholder skins until lane L3's art lands.
        add(Power.DOCTOR_STRANGE,
                new Variant("sorcerer_supreme", Power.DOCTOR_STRANGE, Variant.suit("strange_sorcerer_supreme"), null,
                        false, 0xF29A2E, 0xFFE0A8),
                new Variant("multiverse", Power.DOCTOR_STRANGE, Variant.suit("strange_multiverse"), null, false,
                        0xF29A2E, 0xFFE0A8),
                new Variant("defender", Power.DOCTOR_STRANGE, Variant.suit("strange_defender"), null, false, 0xE0802A,
                        0xFFD8A0),
                own(Power.DOCTOR_STRANGE));
        // ===== hulk (3.6) =====
        // The Hulk of Thor: Ragnarok (the gladiator, the default after the awakening), the bigger savage Hulk of
        // Spider-Man: Brand New Day, and two human looks: Bruce Banner, and the player's own skin as Banner
        // (HulkForms.isHuman). Lane L7 draws the Hulks with a model of their own (128 x 128 skins); these are
        // placeholder skins on the player model.
        add(Power.HULK,
                new Variant("ragnarok", Power.HULK, Variant.suit("hulk_ragnarok"), null, false, 0x5BA33A, 0xB6F27A),
                new Variant("brand_new_day", Power.HULK, Variant.suit("hulk_brand_new_day"), null, false, 0x4A8A2E,
                        0xB6F27A),
                new Variant("banner", Power.HULK, Variant.suit("hulk_banner"), null, false, 0x5BA33A, 0xB6F27A),
                own(Power.HULK));

        // ===== wolverine (3.7) =====
        // The classic yellow-and-blue suit (Deadpool & Wolverine, 2024: the comics' colours on screen at last) with the
        // cowl, and the black leather of X-Men (2000) with its orange-yellow piping (its colour, as black would not read
        // on the HUD); our own drawings by lane L2 (SCRATCH/v37/art/l2). Placeholder skins until then.
        add(Power.WOLVERINE,
                new Variant("classic", Power.WOLVERINE, Variant.suit("wolverine_classic"), null, false, 0xF2C230,
                        0xD9DEE3),
                new Variant("x_men_2000", Power.WOLVERINE, Variant.suit("wolverine_x_men_2000"), null, false, 0xE8A33A,
                        0xD9DEE3),
                own(Power.WOLVERINE));
        // ===== deadpool (3.7) =====
        // The red-and-black suit, the X-Men trainee's yellow-and-black with his red mask, and Kidpool (smaller, a short
        // red cape); our own drawings by lane L3 (SCRATCH/v37/art/l3/gen_deadpool.py), no logo of the films.
        add(Power.DEADPOOL,
                new Variant("classic", Power.DEADPOOL, Variant.suit("deadpool_classic"), null, false, 0xC8102E,
                        0xFF8A8A),
                new Variant("trainee", Power.DEADPOOL, Variant.suit("deadpool_trainee"), null, false, 0xE8C21E,
                        0xFF8A8A),
                new Variant("kidpool", Power.DEADPOOL, Variant.suit("deadpool_kidpool"),
                        Variant.suit("deadpool_kidpool_cape"), false, 0xC8102E,
                        0xFF8A8A),
                own(Power.DEADPOOL));

        // ===== captain (3.8) =====
        // The Avengers suit (Endgame) and the Winter Soldier's stealth suit; our own drawings by lane L1
        // (SCRATCH/v38/art/l1). Placeholder skins until then.
        add(Power.CAPTAIN_AMERICA,
                new Variant("classic", Power.CAPTAIN_AMERICA, Variant.suit("captain_america_classic"), null, false,
                        0x2A4BA0, 0xE8EEF5),
                new Variant("stealth", Power.CAPTAIN_AMERICA, Variant.suit("captain_america_stealth"), null, false,
                        0x4A5C80, 0xE8EEF5),
                own(Power.CAPTAIN_AMERICA));
        // ===== red_hulk (3.8) =====
        // Red Hulk (drawn by the Hulk's renderer, lane L2's seam); Ross's own form is the player's skin with his suit.
        add(Power.RED_HULK,
                new Variant("red_hulk", Power.RED_HULK, Variant.suit("red_hulk"), null, false, 0xC4231B, 0xFF7A3A),
                own(Power.RED_HULK));
        // ===== iron_man (3.8) =====
        // Tony under the suit (his look is his suit, drawn by SuitLayer over this skin; lane L3).
        add(Power.IRON_MAN,
                new Variant("tony", Power.IRON_MAN, Variant.suit("iron_man_tony"), null, false, 0xB3121B, 0x9FE8FF),
                own(Power.IRON_MAN));
        // ===== falcon (3.8) =====
        // Endgame's suit and the Winter Soldier's flight suit; our own drawings by lane L5 (SCRATCH/v38/art/l5).
        add(Power.FALCON,
                new Variant("endgame", Power.FALCON, Variant.suit("falcon_endgame"), null, false, 0x8A8F98, 0xE0313A),
                new Variant("winter_soldier", Power.FALCON, Variant.suit("falcon_winter_soldier"), null, false,
                        0x5A4E44,
                        0xE0313A),
                own(Power.FALCON));
        // ===== love (3.8) =====
        // Love's golden tunic and lilac cape (our own design by lane L8, SCRATCH/v38/art/l8).
        add(Power.LOVE,
                new Variant("love", Power.LOVE, Variant.suit("love"), Variant.suit("love_cape"), true, 0xB57BFF,
                        0xFFE59A),
                own(Power.LOVE));
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
