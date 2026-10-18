package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Every ability of every built-in hero as a module a custom hero can pick (3.4): one row per key of a hero (with its Sneak
 * variant), one per hero body (the passives), and Billy's Build Magic. A row is data: the hero, the key it sits on for
 * the hero himself ({@link #home}), how its key acts and how its Sneak variant acts, its cost, the shared state it
 * drives ({@link Resource}), the roles it gives ({@link Trait}), what its strength scales. The code of a row is the hero's
 * own: {@code HeroKeys.press} calls the hero's key entry with the row's home key and the explicit Sneak flag, so nothing
 * of a hero is copied. The fliers' flight keys are not rows: they are the flight styles of the Flight module (4.3 of the
 * plan).
 *
 * <p>The rows sit in one region per hero ({@code // ===== hero =====}), in the order of {@link Power}; a lane changes
 * only the data of its own heroes' rows. Ids are {@code <hero>.<name>} ({@link #id}) and are what a hero's JSON names, so
 * the order of the constants is never saved.
 */
public enum HeroAbility implements CatalogEntry {
    // ===== magneto =====
    MAGNETO_METAL(Power.MAGNETO, 1, "metal", press().alt(Act.PRESS).cost(2).group(Group.MIND).icon("minecraft:iron_ingot")
            .res(Resource.GRIP).family(Family.TELEKINESIS)),
    MAGNETO_SHRAPNEL(Power.MAGNETO, 2, "shrapnel", hold().cost(2).group(Group.ENERGY).icon("minecraft:iron_nugget")
            .on(PowerData::shrapnelActive)),
    MAGNETO_DISARM(Power.MAGNETO, 3, "disarm", press().cost(2).group(Group.MIND).icon("minecraft:iron_sword")),
    MAGNETO_SHIELD(Power.MAGNETO, 4, "shield", toggle().cost(2).group(Group.ENERGY).icon("minecraft:lodestone")
            .on(PowerData::magneticShield)),

    // ===== scarlet_witch =====
    WANDA_CHAOS_BEAM(Power.SCARLET_WITCH, 1, "chaos_beam", hold().cost(2).group(Group.ENERGY).icon("minecraft:redstone")
            .res(Resource.BEAM_FLAG).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.BEAM).on(PowerData::beamActive)),
    WANDA_TELEKINESIS(Power.SCARLET_WITCH, 3, "telekinesis", press().cost(2).group(Group.MIND).icon("minecraft:amethyst_shard")
            .res(Resource.GRIP).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.TELEKINESIS)),
    WANDA_CHAOS_SHIELD(Power.SCARLET_WITCH, 4, "chaos_shield", hold().cost(2).group(Group.ENERGY).icon("minecraft:red_stained_glass")
            .res(Resource.SHIELD).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.SHIELD).on(PowerData::chaosShieldActive)),
    WANDA_HEX(Power.SCARLET_WITCH, 5, "hex", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:crying_obsidian")
            .res(Resource.HEX).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).world()),

    // ===== quicksilver =====
    QUICKSILVER_TIME(Power.QUICKSILVER, 1, "time", toggle().alt(Act.TOGGLE).cost(3).group(Group.MOVE).icon("minecraft:recovery_compass")
            .res(Resource.SLOW_TIME, Resource.SPEED_RUN).family(Family.TIME).on(PowerData::quicksilverTime)),
    QUICKSILVER_SPEED(Power.QUICKSILVER, 2, "speed", toggle().alt(Act.PRESS).cost(2).group(Group.MOVE).icon("minecraft:rabbit_foot")
            .res(Resource.SPEED_RUN).family(Family.SPEED).on(PowerData::superSpeed)),
    QUICKSILVER_BLITZ(Power.QUICKSILVER, 3, "blitz", press().alt(Act.PRESS).cost(2).group(Group.BODY).icon("minecraft:stone_sword")),
    QUICKSILVER_WALL_RUN(Power.QUICKSILVER, 4, "wall_run", toggle().cost(1).group(Group.MOVE).icon("minecraft:leather_boots")
            .fixed().requires("quicksilver.speed", "quicksilver.time").on(PowerData::wallRun)),
    QUICKSILVER_BARRAGE(Power.QUICKSILVER, 5, "barrage", press().cost(2).group(Group.BODY).icon("minecraft:piston")),

    // ===== vision =====
    VISION_MIND_BEAM(Power.VISION, 1, "mind_beam", hold().cost(2).group(Group.ENERGY).icon("minecraft:glowstone_dust")
            .res(Resource.BEAM_FLAG).family(Family.BEAM).on(PowerData::beamActive)),
    VISION_PHASE(Power.VISION, 3, "phase", toggle().cost(3).group(Group.BODY).icon("minecraft:phantom_membrane")
            .res(Resource.PHASE).family(Family.PHASE).on(PowerData::visionPhased)),
    VISION_DENSITY(Power.VISION, 4, "density", toggle().cost(1).group(Group.BODY).icon("minecraft:iron_block")
            .traits(Trait.METAL_BODY).on(PowerData::dense)),
    VISION_DISGUISE(Power.VISION, 5, "disguise", toggle().cost(1).group(Group.BODY).icon("minecraft:leather_chestplate")
            .fixed().on(PowerData::disguise)),

    // ===== billy =====
    BILLY_HEX_BEAM(Power.BILLY, 1, "hex_beam", hold().cost(2).group(Group.ENERGY).icon("minecraft:light_blue_dye")
            .res(Resource.CONT_BEAM).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.BEAM).on(d -> d.is(Live.BEAMING))),
    BILLY_TELEKINESIS(Power.BILLY, 3, "telekinesis", press().cost(2).group(Group.MIND).icon("minecraft:prismarine_shard")
            .res(Resource.GRIP).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.TELEKINESIS)),
    BILLY_FREEZE(Power.BILLY, 4, "freeze", press().alt(Act.HOLD).cost(3).group(Group.MIND).icon("minecraft:blue_ice")
            .altRes(Resource.SHIELD).traits(Trait.SORCERER, Trait.TELEPATH_STRONG)),
    BILLY_NOISE(Power.BILLY, 5, "noise", toggle().cost(1).group(Group.MIND).icon("minecraft:sculk_sensor")
            .fixed().traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.MIND).on(d -> d.toggle(Toggle.THE_NOISE))),
    /** Billy's Build Magic: the sixth key (Y) and its wheel, with Billy's rules (the meter, no Instant). */
    BILLY_BUILD_MAGIC(Power.BILLY, 6, "build_magic", press().cost(2).group(Group.ELEMENT).icon("minecraft:bricks")
            .fixed().traits(Trait.SORCERER, Trait.TELEPATH_STRONG)),

    // ===== tommy =====
    TOMMY_TIME(Power.TOMMY, 1, "time", toggle().alt(Act.TOGGLE).cost(3).group(Group.MOVE).icon("minecraft:clock")
            .res(Resource.SLOW_TIME).family(Family.TIME).on(d -> d.toggle(Toggle.TOMMY_TIME))),
    TOMMY_SPEED(Power.TOMMY, 2, "speed", toggle().alt(Act.PRESS).cost(2).group(Group.MOVE).icon("minecraft:sugar")
            .res(Resource.SPEED_RUN).family(Family.SPEED).on(d -> d.toggle(Toggle.SUPER_SPEED))),
    TOMMY_WHIRLWIND(Power.TOMMY, 3, "whirlwind", press().alt(Act.PRESS).cost(2).group(Group.ELEMENT).icon("minecraft:breeze_rod")),
    TOMMY_VIBRATE(Power.TOMMY, 4, "vibrate", press().alt(Act.PRESS).cost(2).group(Group.BODY).icon("minecraft:note_block")),
    TOMMY_ACCELERATE(Power.TOMMY, 5, "accelerate", press().cost(2).group(Group.ENERGY).icon("minecraft:tnt")),

    // ===== loki =====
    LOKI_DAGGERS(Power.LOKI, 1, "daggers", press().alt(Act.PRESS).cost(2).group(Group.ENERGY).icon("minecraft:iron_sword")
            .traits(Trait.SORCERER)),
    LOKI_DUPLICATES(Power.LOKI, 2, "duplicates", press().cost(2).group(Group.MIND).icon("minecraft:armor_stand")
            .traits(Trait.SORCERER)),
    LOKI_SHAPESHIFT(Power.LOKI, 3, "shapeshift", press().cost(1).group(Group.BODY).icon("minecraft:slime_ball")
            .res(Resource.FORM).traits(Trait.SORCERER).family(Family.SHAPE)),
    LOKI_VANISH(Power.LOKI, 4, "vanish", press().cost(2).group(Group.MOVE).icon("minecraft:ender_pearl").traits(Trait.SORCERER)),
    LOKI_FROST_GIANT(Power.LOKI, 5, "frost_giant", toggle().cost(1).group(Group.BODY).icon("minecraft:packed_ice")
            .res(Resource.SCALE).traits(Trait.SORCERER).family(Family.SHAPE).on(d -> d.toggle(Toggle.FROST_GIANT))),

    // ===== thor =====
    THOR_HAMMER(Power.THOR, 1, "hammer", press().alt(Act.PRESS).cost(2).group(Group.BODY).icon("minecraft:mace")
            .res(Resource.WEAPON).traits(Trait.WORTHY)),
    THOR_LIGHTNING(Power.THOR, 3, "lightning", hold().cost(3).group(Group.ELEMENT).icon("minecraft:lightning_rod")
            .family(Family.BEAM).on(d -> d.is(Live.CALLING_LIGHTNING))),
    THOR_THUNDER(Power.THOR, 4, "thunder", toggle().cost(2).group(Group.ELEMENT).icon("minecraft:trident")
            .on(d -> d.toggle(Toggle.GOD_OF_THUNDER))),
    THOR_BIFROST(Power.THOR, 5, "bifrost", press().alt(Act.PRESS).cost(2).group(Group.MOVE).icon("minecraft:end_crystal").world()),
    THOR_BODY(Power.THOR, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:netherite_chestplate").family(Family.BODY)),

    // ===== phoenix =====
    PHOENIX_MIND(Power.PHOENIX, 1, "mind", toggle().alt(Act.PRESS).cost(1).group(Group.MIND).icon("minecraft:ender_eye")
            .traits(Trait.TELEPATH_STRONG).family(Family.MIND).on(d -> d.toggle(Toggle.MIND_READING))),
    PHOENIX_TELEKINESIS(Power.PHOENIX, 3, "telekinesis", press().cost(2).group(Group.MIND).icon("minecraft:amethyst_cluster")
            .res(Resource.GRIP).family(Family.TELEKINESIS)),
    PHOENIX_CONTROL(Power.PHOENIX, 4, "control", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:name_tag")
            .traits(Trait.TELEPATH_STRONG).family(Family.MIND)),
    PHOENIX_BURST(Power.PHOENIX, 5, "burst", hold().cost(3).group(Group.ENERGY).icon("minecraft:blaze_powder")
            .on(d -> d.is(Live.PHOENIX_CHARGING))),
    PHOENIX_BODY(Power.PHOENIX, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:blaze_rod")
            .traits(Trait.FIRE_IMMUNE).family(Family.BODY)),

    // ===== spider_man =====
    SPIDER_WEB(Power.SPIDER_MAN, 1, "web", press().alt(Act.PRESS).cost(2).group(Group.ENERGY).icon("minecraft:cobweb")),
    SPIDER_ZIP(Power.SPIDER_MAN, 2, "zip", press().alt(Act.HOLD).cost(2).group(Group.MOVE).icon("minecraft:string")),
    SPIDER_CRAWL(Power.SPIDER_MAN, 3, "crawl", toggle().alt(Act.TOGGLE).cost(1).group(Group.MOVE).icon("minecraft:spider_eye")
            .fixed().res(Resource.WALL).family(Family.WALL).on(d -> d.toggle(Toggle.WALL_CRAWL))),
    SPIDER_LEGS(Power.SPIDER_MAN, 4, "legs", toggle().alt(Act.PRESS).cost(2).group(Group.BODY).icon("minecraft:chain")
            .traits(Trait.IRON_LEGS).on(d -> d.toggle(Toggle.SPIDER_LEGS))),
    SPIDER_BLACK_EYES(Power.SPIDER_MAN, 5, "black_eyes", toggle().cost(2).group(Group.BODY).icon("minecraft:black_dye")
            .on(d -> d.is(Live.BLACK_EYES))),
    SPIDER_BODY(Power.SPIDER_MAN, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:rabbit_hide").family(Family.BODY)),

    // ===== thanos =====
    THANOS_POWER(Power.THANOS, 1, "power", press().alt(Act.PRESS).cost(3).group(Group.ENERGY).icon("minecraft:purple_dye")),
    THANOS_SPACE(Power.THANOS, 2, "space", press().alt(Act.PRESS).cost(2).group(Group.MOVE).icon("minecraft:lapis_lazuli")),
    THANOS_REALITY(Power.THANOS, 3, "reality", press().alt(Act.TOGGLE).cost(2).group(Group.MIND).icon("minecraft:red_dye")
            .on(d -> d.toggle(Toggle.REALITY_VEIL))),
    THANOS_SOUL(Power.THANOS, 4, "soul", hold().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:orange_dye")
            .on(d -> d.is(Live.SOUL_DRAINING))),
    THANOS_TIME(Power.THANOS, 5, "time", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:emerald")
            .family(Family.TIME).world()),
    THANOS_BODY(Power.THANOS, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:golden_chestplate").family(Family.BODY)),

    // ===== ultron =====
    ULTRON_REPULSOR(Power.ULTRON, 1, "repulsor", hold().alt(Act.PRESS).cost(2).group(Group.ENERGY).icon("minecraft:redstone_torch")
            .res(Resource.BEAM_FLAG).family(Family.BEAM).on(PowerData::beamActive)),
    ULTRON_GRAVITY(Power.ULTRON, 3, "gravity", press().alt(Act.PRESS).cost(2).group(Group.ELEMENT).icon("minecraft:heavy_core")),
    ULTRON_UPLOAD(Power.ULTRON, 4, "upload", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:comparator")
            .res(Resource.DRONES).world()),
    ULTRON_ARMY(Power.ULTRON, 5, "army", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:observer")
            .res(Resource.DRONES).world()),
    ULTRON_BODY(Power.ULTRON, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:iron_chestplate")
            .traits(Trait.METAL_BODY).family(Family.BODY)),

    // ===== apocalypse =====
    APOCALYPSE_SAND(Power.APOCALYPSE, 1, "sand", press().alt(Act.PRESS).cost(2).group(Group.ELEMENT).icon("minecraft:sand")),
    APOCALYPSE_PORTAL(Power.APOCALYPSE, 3, "portal", press().cost(2).group(Group.MOVE).icon("minecraft:chorus_fruit")),
    APOCALYPSE_SHIELD(Power.APOCALYPSE, 4, "shield", hold().cost(2).group(Group.ENERGY).icon("minecraft:gold_block")
            .res(Resource.SHIELD).family(Family.SHIELD).on(PowerData::chaosShieldActive)),
    APOCALYPSE_BESTOW(Power.APOCALYPSE, 5, "bestow", press().alt(Act.PRESS).cost(2).group(Group.MIND).icon("minecraft:totem_of_undying")
            .on(d -> d.is(Live.SANDSTORM))),
    APOCALYPSE_BODY(Power.APOCALYPSE, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:golden_helmet").family(Family.BODY)),

    // ===== agatha =====
    AGATHA_PURPLE_BEAM(Power.AGATHA, 1, "purple_beam", hold().alt(Act.PRESS).cost(2).group(Group.ENERGY).icon("minecraft:amethyst_block")
            .res(Resource.CONT_BEAM).traits(Trait.SORCERER, Trait.DARK_MAGIC).family(Family.BEAM).on(d -> d.is(Live.BEAMING))),
    AGATHA_DRAIN(Power.AGATHA, 3, "drain", press().cost(3).group(Group.MIND).icon("minecraft:wither_rose")
            .traits(Trait.SORCERER, Trait.DARK_MAGIC)),
    AGATHA_SHIELD(Power.AGATHA, 4, "shield", hold().cost(2).group(Group.ENERGY).icon("minecraft:purple_stained_glass")
            .res(Resource.SHIELD).traits(Trait.SORCERER, Trait.DARK_MAGIC).family(Family.SHIELD).on(PowerData::chaosShieldActive)),
    AGATHA_RUNES(Power.AGATHA, 5, "runes", press().cost(2).group(Group.MIND).icon("minecraft:lectern")
            .traits(Trait.SORCERER, Trait.DARK_MAGIC)),
    AGATHA_BODY(Power.AGATHA, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:book")
            .traits(Trait.SORCERER, Trait.DARK_MAGIC).family(Family.BODY)),

    // ===== cyclops =====
    CYCLOPS_OPTIC_BLAST(Power.CYCLOPS, 1, "optic_blast", hold().alt(Act.PRESS).cost(2).group(Group.ENERGY)
            .icon("minecraft:red_stained_glass_pane").res(Resource.CONT_BEAM).family(Family.BEAM)),
    CYCLOPS_CONCUSSION(Power.CYCLOPS, 2, "concussion", press().cost(2).group(Group.ENERGY).icon("minecraft:redstone_block")),
    CYCLOPS_LEAP(Power.CYCLOPS, 3, "leap", press().cost(1).group(Group.MOVE).icon("minecraft:rabbit_foot")),
    CYCLOPS_RALLY(Power.CYCLOPS, 4, "rally", press().cost(1).group(Group.MIND).icon("minecraft:bell")),
    CYCLOPS_GLASSES(Power.CYCLOPS, 5, "glasses", press().alt(Act.PRESS).cost(3).group(Group.ENERGY).icon("minecraft:tinted_glass")
            .res(Resource.CONT_BEAM).on(d -> d.is(Live.CYCLOPS_UNLEASHED))),
    CYCLOPS_BODY(Power.CYCLOPS, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:chainmail_chestplate").family(Family.BODY)),

    // ===== professor_x =====
    XAVIER_MENTAL_BLAST(Power.PROFESSOR_X, 1, "mental_blast", press().cost(2).group(Group.MIND).icon("minecraft:echo_shard")
            .traits(Trait.TELEPATH)),
    XAVIER_THOUGHTS(Power.PROFESSOR_X, 2, "thoughts", toggle().cost(1).group(Group.MIND).icon("minecraft:sculk")
            .fixed().traits(Trait.TELEPATH).family(Family.MIND).on(d -> d.toggle(Toggle.HEAR_THOUGHTS))),
    XAVIER_TAKE_OVER(Power.PROFESSOR_X, 3, "take_over", press().cost(3).group(Group.MIND).icon("minecraft:player_head")
            .traits(Trait.TELEPATH).world().on(d -> d.is(Live.MIND_LINKED))),
    XAVIER_FREEZE_MINDS(Power.PROFESSOR_X, 4, "freeze_minds", press().cost(3).group(Group.MIND).icon("minecraft:snowball")
            .traits(Trait.TELEPATH)),
    XAVIER_CEREBRO(Power.PROFESSOR_X, 5, "cerebro", press().cost(2).group(Group.MIND).icon("minecraft:conduit")
            .traits(Trait.TELEPATH).on(d -> d.is(Live.CEREBRO)));

    /** The table's short names of how a Sneak variant acts (the generic modules' kinds). */
    private static final class Act {
        static final PowerModule.Act PRESS = PowerModule.Act.PRESS;
        static final PowerModule.Act HOLD = PowerModule.Act.HOLD;
        static final PowerModule.Act TOGGLE = PowerModule.Act.TOGGLE;

        private Act() {
        }
    }

    /** What the strength slider of a row changes (none: the slider is hidden and the row costs as at strength 5). */
    public enum Scales {
        /** Its damage ({@code HeroDamage}). */
        DAMAGE,
        /** Its cooldown ({@code PowerData.startCooldown(HeroAbility, ...)}). */
        COOLDOWN,
        /** The amounts of the hero's body ({@code PowerData.bodyScale}). */
        BODY
    }

    /** The two choices of a picked row: its Sneak variant too ({@link #BOTH}, the default), or the main action only. */
    public static final String BOTH = "both";
    public static final String MAIN = "main";

    /** The strength a row without {@link Scales} is always at (for its cost). */
    public static final int FIXED_STRENGTH = 5;

    /** The hero whose ability it is. */
    public final Power hero;
    /** The key it is on for the hero himself (1-5, R G V B N), 0 for a body (a passive), 6 for Billy's Y key. */
    public final int home;
    /** Its name within the hero ({@code lightning}). */
    public final String name;
    private final String id;
    private final PowerModule.Act act;
    @Nullable
    private final PowerModule.Act alt;
    public final int costPerPoint;
    public final PowerModule.Group group;
    public final String icon;
    private final Set<Resource> resources;
    private final Set<Resource> altResources;
    private final Set<Trait> traits;
    private final Set<Scales> scales;
    /** A world-scale ability ({@code custom_hero.worldPowers}). */
    public final boolean world;
    private final List<String> requires;
    @Nullable
    private final Predicate<PowerData> on;
    @Nullable
    private final Family family;

    HeroAbility(Power hero, int home, String name, Spec spec) {
        this.hero = hero;
        this.home = home;
        this.name = name;
        this.id = hero.getSerializedName().replace("scarlet_witch", "wanda").replace("spider_man", "spider")
                .replace("professor_x", "xavier") + "." + name;
        this.act = spec.act;
        this.alt = spec.alt;
        this.costPerPoint = spec.cost;
        this.group = spec.group;
        this.icon = spec.icon;
        this.resources = Collections.unmodifiableSet(spec.resources);
        this.altResources = Collections.unmodifiableSet(spec.altResources);
        this.traits = Collections.unmodifiableSet(spec.traits);
        this.scales = Collections.unmodifiableSet(spec.scales == null
                ? spec.act == PowerModule.Act.NONE ? EnumSet.of(Scales.BODY) : EnumSet.of(Scales.DAMAGE, Scales.COOLDOWN) : spec.scales);
        this.world = spec.world;
        this.requires = List.copyOf(spec.requires);
        this.on = spec.on;
        this.family = spec.family;
    }

    // ---- CatalogEntry ----

    /** {@code thor.lightning}: the id a hero's JSON names it by. */
    @Override
    public String id() {
        return id;
    }

    /** The id of its hero in ids ({@code thor}, {@code wanda}, {@code spider}, {@code xavier}...). */
    public String heroId() {
        return id.substring(0, id.indexOf('.'));
    }

    /** {@code hero_module.enhanced.thor.lightning}; {@code .alt} is its Sneak variant's name, {@code .desc} its line. */
    @Override
    public String nameKey() {
        return "hero_module.enhanced." + id;
    }

    /** The lang key of its Sneak variant's name. */
    public String altKey() {
        return nameKey() + ".alt";
    }

    @Override
    public String descKey() {
        return nameKey() + ".desc";
    }

    @Override
    @Nullable
    public Family family() {
        return family;
    }

    @Override
    public PowerModule.Group group() {
        return group;
    }

    @Override
    public String icon() {
        return icon;
    }

    /** Points for each point of strength; a row without {@link Scales} always costs as at {@link #FIXED_STRENGTH}. */
    @Override
    public int cost(int strength) {
        return costPerPoint * (scales.isEmpty() ? FIXED_STRENGTH : Math.max(1, strength));
    }

    /** A row has no generic options: its choice is {@link #BOTH} or {@link #MAIN}. */
    @Override
    public List<PowerModule.Option> options() {
        return List.of();
    }

    /** How its main action acts ({@link PowerModule.Act#NONE} for a body). The option is ignored. */
    @Override
    public PowerModule.Act act(String option) {
        return act;
    }

    public PowerModule.Act act() {
        return act;
    }

    /** How its Sneak variant acts, or null when it has none. */
    @Override
    @Nullable
    public PowerModule.Act alt() {
        return alt;
    }

    public boolean hasAlt() {
        return alt != null;
    }

    /** The resources its main action drives, and its Sneak variant's when {@code option} is {@link #BOTH}. */
    @Override
    public Set<Resource> resources(String option) {
        if (altResources.isEmpty() || MAIN.equals(option)) {
            return resources;
        }
        EnumSet<Resource> all = EnumSet.noneOf(Resource.class);
        all.addAll(resources);
        all.addAll(altResources);
        return all;
    }

    @Override
    @Nullable
    public Power hero() {
        return hero;
    }

    @Override
    public boolean world() {
        return world;
    }

    @Override
    public boolean scales() {
        return !scales.isEmpty();
    }

    public boolean scales(Scales what) {
        return scales.contains(what);
    }

    /** The roles a custom hero with this row has ({@link Traits}). */
    public Set<Trait> traits() {
        return traits;
    }

    /** Ids of the rows of which one at least must be picked with this one (none: it needs nothing). */
    public List<String> requires() {
        return requires;
    }

    /** A body (a passive): no key. */
    public boolean passive() {
        return act == PowerModule.Act.NONE;
    }

    /** Whether the HUD shows it as on (a toggle on, a held ability active), or false when it has no such state. */
    public boolean isOn(PowerData data) {
        return on != null && on.test(data);
    }

    /** Whether it has an on/active state the HUD can show. */
    public boolean hasOnState() {
        return on != null;
    }

    // ---- lookup ----

    /** The row of this id ({@code thor.lightning}, any case), or null. */
    @Nullable
    public static HeroAbility byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (HeroAbility a : values()) {
            if (a.id.equals(wanted)) {
                return a;
            }
        }
        return null;
    }

    /** The rows of one hero, in order. */
    public static List<HeroAbility> of(Power hero) {
        List<HeroAbility> list = new ArrayList<>();
        for (HeroAbility a : values()) {
            if (a.hero == hero) {
                list.add(a);
            }
        }
        return list;
    }

    /** The row of a hero's key {@code home} (1-5; 0 is his body), or null (a flight key, or none). */
    @Nullable
    public static HeroAbility at(Power hero, int home) {
        for (HeroAbility a : values()) {
            if (a.hero == hero && a.home == home) {
                return a;
            }
        }
        return null;
    }

    // ---- the table's builder ----

    private static Spec press() {
        return new Spec(PowerModule.Act.PRESS);
    }

    private static Spec hold() {
        return new Spec(PowerModule.Act.HOLD);
    }

    private static Spec toggle() {
        return new Spec(PowerModule.Act.TOGGLE);
    }

    private static Spec body() {
        return new Spec(PowerModule.Act.NONE);
    }

    private static final class Group {
        static final PowerModule.Group BODY = PowerModule.Group.BODY;
        static final PowerModule.Group MOVE = PowerModule.Group.MOVE;
        static final PowerModule.Group ENERGY = PowerModule.Group.ENERGY;
        static final PowerModule.Group ELEMENT = PowerModule.Group.ELEMENT;
        static final PowerModule.Group MIND = PowerModule.Group.MIND;

        private Group() {
        }
    }

    /** One row's data while the table is built. */
    private static final class Spec {
        final PowerModule.Act act;
        PowerModule.Act alt;
        int cost = 2;
        PowerModule.Group group = PowerModule.Group.ENERGY;
        String icon = "minecraft:nether_star";
        final EnumSet<Resource> resources = EnumSet.noneOf(Resource.class);
        final EnumSet<Resource> altResources = EnumSet.noneOf(Resource.class);
        final EnumSet<Trait> traits = EnumSet.noneOf(Trait.class);
        EnumSet<Scales> scales;
        boolean world;
        final List<String> requires = new ArrayList<>();
        Predicate<PowerData> on;
        Family family;

        Spec(PowerModule.Act act) {
            this.act = act;
        }

        Spec alt(PowerModule.Act alt) {
            this.alt = alt;
            return this;
        }

        Spec cost(int cost) {
            this.cost = cost;
            return this;
        }

        Spec group(PowerModule.Group group) {
            this.group = group;
            return this;
        }

        Spec icon(String icon) {
            this.icon = icon;
            return this;
        }

        Spec res(Resource... resources) {
            this.resources.addAll(List.of(resources));
            return this;
        }

        Spec altRes(Resource... resources) {
            this.altResources.addAll(List.of(resources));
            return this;
        }

        Spec traits(Trait... traits) {
            this.traits.addAll(List.of(traits));
            return this;
        }

        /** No strength slider: nothing of it has a number to scale. */
        Spec fixed() {
            this.scales = EnumSet.noneOf(Scales.class);
            return this;
        }

        Spec world() {
            this.world = true;
            return this;
        }

        Spec requires(String... ids) {
            this.requires.addAll(List.of(ids));
            return this;
        }

        Spec on(Predicate<PowerData> on) {
            this.on = on;
            return this;
        }

        Spec family(Family family) {
            this.family = family;
            return this;
        }
    }
}
