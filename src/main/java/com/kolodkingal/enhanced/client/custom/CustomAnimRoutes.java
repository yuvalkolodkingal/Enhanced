package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.custom.AnimCatalog;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * What a custom hero's bindings play (3.4): the gesture of a cast marker ({@code CustomState.castSeq}) and the stance of a
 * held or switched-on power, and how he flies, walks, speeds and stands (his definition's {@code anim}). Pure (no client
 * state), so it can be tested. A binding plays its move ({@link CustomHero#move}) when the definition chose one; else a
 * hero ability plays its own gesture ({@link HeroAbilityAnims}: on the marker only what the hero acts by a cooldown, the
 * rest by the hero's own routes) and a generic module the default of its option (Appendix A of the plan).
 */
public final class CustomAnimRoutes {

    /**
     * The gesture binding ({@code column} 1-5, plain or with Sneak) plays when it acts, or null: none, or the hero
     * ability's own route plays it (a flag, a toggle, a live state). A generic switch counts as going on.
     */
    @Nullable
    public static GestureId cast(CustomHero hero, HeroKit kit, int column, boolean sneak) {
        return cast(hero, kit, column, sneak, true);
    }

    /** As {@link #cast(CustomHero, HeroKit, int, boolean)}; {@code on}: whether a switch the binding moved is on now. */
    @Nullable
    public static GestureId cast(CustomHero hero, HeroKit kit, int column, boolean sneak, boolean on) {
        HeroKit.Binding binding = sneak ? kit.sneak(column) : kit.plain(column);
        if (binding == null) {
            return null;
        }
        String move = hero.move(column, sneak);
        if (!CustomHero.AUTO.equals(move)) {
            return gesture(move);
        }
        HeroAbility ability = binding.ability();
        if (ability != null) {
            return binding.isAlt() ? HeroAbilityAnims.castAlt(ability) : HeroAbilityAnims.castMain(ability);
        }
        PowerModule module = binding.module();
        return module == null ? null : defaultGesture(module, option(hero, binding), on);
    }

    /** Whether the definition chose the move of a binding (anything but {@link CustomHero#AUTO}, "none" too). */
    public static boolean chosen(CustomHero hero, int column, boolean sneak) {
        return !CustomHero.AUTO.equals(hero.move(column, sneak));
    }

    /** The option a module's binding acts in: the one it names, or the pick's own for its main cell. */
    public static String option(CustomHero hero, HeroKit.Binding binding) {
        if (!HeroKit.MAIN.equals(binding.action())) {
            return binding.action();
        }
        CustomHero.Pick pick = hero.pick(binding.entry());
        return pick == null ? binding.action() : pick.option();
    }

    /** The gesture a move names ({@code "billy_bolt"}), or null for {@code auto}, {@code none} or a name unknown here. */
    @Nullable
    public static GestureId gesture(String move) {
        if (move == null || CustomHero.AUTO.equals(move) || CustomHero.NONE.equals(move)) {
            return null;
        }
        try {
            return GestureId.valueOf(move.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The stance a name names ({@code "beam_hand"}), or null for {@code auto}, {@code none} or a name unknown here. */
    @Nullable
    public static Stance stance(String name) {
        if (name == null || CustomHero.AUTO.equals(name) || CustomHero.NONE.equals(name)) {
            return null;
        }
        try {
            return Stance.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * The default gesture of a generic module's option ({@code on}: a switch going on, else off; a press or a key held
     * down plays it whatever {@code on} says), or null. Appendix A of the plan.
     */
    @Nullable
    public static GestureId defaultGesture(PowerModule module, String option, boolean on) {
        return switch (module) {
            case SUPER_STRENGTH -> switch (option) {
                case "slam" -> GestureId.THANOS_SLAM;
                case "leap" -> GestureId.CYCLOPS_LEAP;
                default -> GestureId.SPEED_DASH;
            };
            case SUPER_SPEED -> "blitz".equals(option) ? GestureId.SPEED_DASH : on ? GestureId.TOMMY_READY : null;
            case HEALING -> "burst".equals(option) ? GestureId.APOC_BESTOW : null;
            case ENERGY_BLAST -> "burst".equals(option) ? GestureId.PHOENIX_PSIONIC_CAST : GestureId.BILLY_BOLT;
            case SUPER_SENSES -> "xray".equals(option) ? (on ? GestureId.PHOENIX_READ_ON : GestureId.PHOENIX_READ_OFF) : null;
            case TELEPATHY -> "read".equals(option) ? GestureId.XAVIER_THOUGHTS_ON : on ? GestureId.PHOENIX_SENSE : GestureId.XAVIER_THOUGHTS_OFF;
            case TELEPORT -> "behind".equals(option) ? GestureId.APOC_PORTAL : GestureId.THANOS_STEP;
            case WALL_CRAWL -> on ? GestureId.SPIDER_CRAWL_ON : GestureId.SPIDER_CRAWL_OFF;
            case FORCE_FIELD -> "reflect".equals(option) ? GestureId.WANDA_SHIELD_SNAP : GestureId.AGATHA_SHIELD_RAISE;
            case PHASING -> on ? GestureId.VISION_PHASE_IN : GestureId.VISION_PHASE_OUT;
            case ANIMAL_CONTROL -> "calm".equals(option) ? GestureId.APOC_BESTOW : GestureId.ULTRON_COMMAND;
            case MIND_CONTROL -> "calm".equals(option) ? GestureId.XAVIER_FREEZE : GestureId.PHOENIX_MIND_CAST;
            case MAGIC -> switch (option) {
                case "ward" -> GestureId.AGATHA_RUNES;
                case "curse" -> GestureId.AGATHA_DRAIN;
                default -> GestureId.AGATHA_HEX;
            };
            case ICE -> switch (option) {
                case "ice_path" -> on ? GestureId.LOKI_FROST_ON : GestureId.LOKI_FROST_OFF;
                case "blizzard" -> GestureId.APOC_STORM_START;
                default -> GestureId.BILLY_BOLT;
            };
            case WEATHER -> "cycle".equals(option) ? GestureId.THOR_SUMMON : GestureId.APOC_STORM_START;
            case LIGHTNING -> "chain".equals(option) ? GestureId.BILLY_BOLT : GestureId.THOR_BEAM_KICK_BARE;
            case SHAPESHIFT -> "creature".equals(option) ? GestureId.LOKI_CAST : on ? GestureId.LOKI_FROST_ON : GestureId.LOKI_FROST_OFF;
            case INVISIBILITY -> on ? GestureId.LOKI_CAST : GestureId.VISION_PHASE_OUT;
            case ABSORPTION -> "power".equals(option) ? GestureId.AGATHA_DRAIN : GestureId.PHOENIX_PSIONIC_CAST;
            case FIRE -> switch (option) {
                case "flamethrower" -> GestureId.AGATHA_BEAM;
                case "aura" -> on ? GestureId.THOR_UNLEASH_ON : GestureId.THOR_UNLEASH_OFF;
                default -> GestureId.BILLY_BOLT;
            };
            case DUPLICATION -> GestureId.ULTRON_ARMY;
            case WATER -> "wave".equals(option) ? GestureId.APOC_SAND_WAVE : GestureId.AGATHA_BEAM;
            case TIME -> switch (option) {
                case "stop" -> GestureId.THANOS_TIMELOCK;
                case "rewind" -> GestureId.THANOS_REWIND;
                default -> on ? GestureId.SPEED_TIME : null;
            };
            case TECHNOPATHY -> "command".equals(option) ? GestureId.ULTRON_COMMAND : GestureId.ULTRON_HACK;
            case SONIC_SCREAM -> GestureId.TOMMY_BOOM;
            case EARTH -> "spikes".equals(option) ? GestureId.APOC_SAND_WAVE : GestureId.ULTRON_SLAM;
            case WIND -> "updraft".equals(option) ? GestureId.APOC_STORM_START : GestureId.WANDA_BEAM_KICK;
            // Passive, or acted elsewhere: Flight by the take-off of its style, Telekinesis by the grab and the throw.
            case DURABILITY, FLIGHT, TELEKINESIS -> null;
        };
    }

    /** The default stance of a generic module's option while it is held or on, or null. Appendix A of the plan. */
    @Nullable
    public static Stance defaultStance(PowerModule module, String option) {
        return switch (module) {
            case ENERGY_BLAST -> "beam".equals(option) ? Stance.BEAM_HAND : null;
            case SUPER_SENSES -> "xray".equals(option) ? Stance.PHOENIX_READ : null;
            case TELEPATHY -> "sense".equals(option) ? Stance.NOISE : null;
            case FORCE_FIELD -> "reflect".equals(option) ? Stance.SHIELD_WANDA : Stance.AGATHA_SHIELD;
            case PHASING -> Stance.VISION_PHASED;
            case ICE -> "ice_path".equals(option) ? Stance.LOKI_FROST : null;
            case FIRE -> "flamethrower".equals(option) ? Stance.BEAM_HAND : "aura".equals(option) ? Stance.THOR_UNLEASHED : null;
            case WATER -> "jet".equals(option) ? Stance.BEAM_HAND : null;
            // (Telekinesis holds by the shared TK_HOLD and TK_STRAIN, wall-crawling by the cling of the surface, the
            // super speed by its run: none needs a stance of its own.)
            default -> null;
        };
    }

    /** Whether a pick's power is held or on now (its stance is up): a generic module's switch, or a hero ability's state. */
    public static boolean isActive(CustomHero.Pick pick, PowerData data, CustomState state) {
        PowerModule module = pick.module();
        if (module != null) {
            PowerModule.Act act = pick.act();
            if (act != PowerModule.Act.HOLD && act != PowerModule.Act.TOGGLE) {
                return false;
            }
            return module == PowerModule.FORCE_FIELD ? state.field() : state.isOn(module);
        }
        HeroAbility ability = pick.ability();
        return ability != null && ability.isOn(data);
    }

    /**
     * The stance a pick raises while it is held or on: its own choice, or for a generic module the default of its
     * option; null for none, and for a hero ability on {@code auto} (it raises its hero's stances itself).
     */
    @Nullable
    public static Stance stanceOf(CustomHero.Pick pick) {
        if (CustomHero.NONE.equals(pick.stance())) {
            return null;
        }
        if (!CustomHero.AUTO.equals(pick.stance())) {
            return stance(pick.stance());
        }
        PowerModule module = pick.module();
        return module == null ? null : defaultStance(module, pick.option());
    }

    // ---- how he flies, walks, speeds and stands (the definition's anim block) ----

    /** The heroes whose flight a custom hero with one of their abilities flies with when his style is {@code auto}. */
    private static final List<Power> FLIERS = List.of(Power.THOR, Power.SCARLET_WITCH, Power.VISION, Power.MAGNETO, Power.BILLY,
            Power.PHOENIX, Power.ULTRON, Power.APOCALYPSE, Power.AGATHA, Power.DOCTOR_STRANGE,
            Power.IRON_MAN, Power.FALCON); // (3.8)

    /**
     * His flight style ({@code AnimCatalog.FLIGHT_STYLES}): his choice; on {@code auto} the flier one of his abilities
     * comes from, else by his Flight's option (hover like Vision, jet like Ultron, wings like the Phoenix), else plain.
     */
    public static String flight(CustomHero hero, HeroKit kit) {
        String chosen = hero.anim().flight();
        if (!CustomHero.AUTO.equals(chosen) && AnimCatalog.FLIGHT_STYLES.contains(chosen)) {
            return chosen;
        }
        for (Power flier : FLIERS) {
            if (kit.acts(flier)) {
                return style(flier);
            }
        }
        CustomHero.Pick flight = hero.pick(PowerModule.FLIGHT);
        if (flight != null) {
            return switch (flight.option()) {
                case "jet" -> "ultron";
                case "wings" -> "phoenix";
                case "cloak" -> "doctor_strange"; // (3.6: the Cloak of Levitation)
                default -> "vision";
            };
        }
        return "plain";
    }

    /** The flight style of a built-in flier ({@code "wanda"} for the Scarlet Witch). */
    public static String style(Power flier) {
        return switch (flier) {
            case SCARLET_WITCH -> "wanda";
            case NONE -> "plain";
            default -> flier.getSerializedName();
        };
    }

    /** The hero whose flight profile a style is ({@code NONE} for {@code plain} and anything unknown). */
    public static Power flightPower(String style) {
        return switch (style) {
            case "magneto" -> Power.MAGNETO;
            case "wanda" -> Power.SCARLET_WITCH;
            case "vision", "white_vision" -> Power.VISION;
            case "billy" -> Power.BILLY;
            case "thor" -> Power.THOR;
            case "phoenix" -> Power.PHOENIX;
            case "ultron" -> Power.ULTRON;
            case "apocalypse" -> Power.APOCALYPSE;
            case "agatha" -> Power.AGATHA;
            case "doctor_strange" -> Power.DOCTOR_STRANGE;
            case "iron_man" -> Power.IRON_MAN; // (3.8)
            case "falcon" -> Power.FALCON; // (3.8)
            default -> Power.NONE;
        };
    }

    /** Whether a style is White Vision's (Vision's profile, made stiff and mechanical). */
    public static boolean flightWhite(String style) {
        return "white_vision".equals(style);
    }

    /**
     * His run character ({@code AnimCatalog.RUN_CHARACTERS}): his choice; on {@code auto} that of the first hero his
     * abilities come from who has one, else the default.
     */
    public static String run(CustomHero hero, HeroKit kit) {
        String chosen = hero.anim().run();
        if (!CustomHero.AUTO.equals(chosen) && AnimCatalog.RUN_CHARACTERS.contains(chosen)) {
            return chosen;
        }
        for (Power source : Power.values()) {
            if (kit.acts(source)) {
                String name = switch (source) {
                    case SCARLET_WITCH -> "wanda";
                    case SPIDER_MAN -> "spider";
                    case PROFESSOR_X -> "xavier";
                    case QUICKSILVER, TOMMY -> "";
                    default -> source.getSerializedName();
                };
                if (AnimCatalog.RUN_CHARACTERS.contains(name)) {
                    return name;
                }
            }
        }
        return "default";
    }

    /**
     * His speedster run while he speeds: 1 Quicksilver in his Time, 2 Quicksilver's Super Speed, 3 Tommy's. His choice;
     * on {@code auto} Tommy's with Tommy's speed, Quicksilver's Time while time is slowed, else Super Speed.
     */
    public static int speedRun(CustomHero hero, HeroKit kit, boolean slowMo) {
        int chosen = AnimCatalog.SPEED_RUNS.indexOf(hero.anim().speedRun());
        if (chosen >= 0) {
            return chosen + 1;
        }
        if (kit.has(HeroAbility.TOMMY_SPEED)) {
            return 3;
        }
        return slowMo ? 1 : 2;
    }

    /** The idle stances of {@code AnimCatalog.IDLES}, after {@code none}. */
    private static final Stance[] IDLES = {Stance.NOISE, Stance.WANDA_READY, Stance.MAG_FIELD, Stance.VISION_DENSE, Stance.LOKI_FROST,
            Stance.THOR_UNLEASHED, Stance.SPIDER_KILL};

    /** The stance he stands in at rest ({@code anim.idle}), or null. */
    @Nullable
    public static Stance idle(CustomHero hero) {
        return idle(hero.anim().idle());
    }

    /** The stance of an idle name ({@code "regal"}), or null for {@code none}, {@code auto} and anything unknown. */
    @Nullable
    public static Stance idle(String name) {
        int i = AnimCatalog.IDLES.indexOf(name);
        return i <= 0 || i > IDLES.length ? null : IDLES[i - 1];
    }

    /**
     * The stance a "hold" of the editor's preview raises for an entry of the definition: its pick's own, else the first of
     * its hero ability's, else its module's default (null: none).
     */
    @Nullable
    public static Stance previewStance(CustomHero hero, CatalogEntry entry) {
        CustomHero.Pick pick = hero.pick(entry);
        if (pick != null && !CustomHero.AUTO.equals(pick.stance())) {
            return stanceOf(pick);
        }
        if (entry instanceof HeroAbility a) {
            return HeroAbilityAnims.stances(a).stream().findFirst().orElse(null);
        }
        return entry instanceof PowerModule m ? defaultStance(m, pick == null ? m.defaultOption().id() : pick.option()) : null;
    }

    private CustomAnimRoutes() {
    }
}
