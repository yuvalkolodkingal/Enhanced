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
            .res(Resource.IRON_CHARGE).on(PowerData::shrapnelActive)),
    MAGNETO_DISARM(Power.MAGNETO, 3, "disarm", press().cost(2).group(Group.MIND).icon("minecraft:iron_sword")),
    MAGNETO_SHIELD(Power.MAGNETO, 4, "shield", toggle().cost(2).group(Group.ENERGY).icon("minecraft:lodestone")
            .fixed().on(PowerData::magneticShield)),

    // ===== scarlet_witch =====
    WANDA_CHAOS_BEAM(Power.SCARLET_WITCH, 1, "chaos_beam", hold().cost(2).group(Group.ENERGY).icon("minecraft:redstone")
            .res(Resource.BEAM_FLAG).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.BEAM).on(PowerData::beamActive)),
    WANDA_TELEKINESIS(Power.SCARLET_WITCH, 3, "telekinesis", press().cost(2).group(Group.MIND).icon("minecraft:amethyst_shard")
            .res(Resource.GRIP).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.TELEKINESIS)),
    WANDA_CHAOS_SHIELD(Power.SCARLET_WITCH, 4, "chaos_shield", hold().cost(2).group(Group.ENERGY).icon("minecraft:red_stained_glass")
            .res(Resource.SHIELD).traits(Trait.SORCERER, Trait.TELEPATH_STRONG).family(Family.SHIELD).on(PowerData::chaosShieldActive)),
    // (3.6: her reality-warping makes a custom hero a Bearer of the Infinity Stones.)
    WANDA_HEX(Power.SCARLET_WITCH, 5, "hex", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:crying_obsidian")
            .res(Resource.HEX).traits(Trait.SORCERER, Trait.TELEPATH_STRONG, Trait.STONE_BEARER).world()),

    // ===== quicksilver =====
    QUICKSILVER_TIME(Power.QUICKSILVER, 1, "time", toggle().alt(Act.TOGGLE).cost(3).group(Group.MOVE).icon("minecraft:recovery_compass")
            .fixed().res(Resource.SLOW_TIME, Resource.SPEED_RUN).family(Family.TIME).on(PowerData::quicksilverTime)),
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
            .fixed().res(Resource.PHASE).family(Family.PHASE).on(PowerData::visionPhased)),
    VISION_DENSITY(Power.VISION, 4, "density", toggle().cost(1).group(Group.BODY).icon("minecraft:iron_block")
            .fixed().traits(Trait.METAL_BODY).on(PowerData::dense)),
    VISION_DISGUISE(Power.VISION, 5, "disguise", toggle().cost(1).group(Group.BODY).icon("minecraft:leather_chestplate")
            .fixed().on(PowerData::disguise)),
    /** Vision's synthetic body (L2): more health and armour, never hungry nor out of breath, metal (Magneto may lift it). */
    VISION_BODY(Power.VISION, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:copper_ingot")
            .traits(Trait.METAL_BODY).family(Family.BODY)),

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
            .fixed().res(Resource.BUILD).traits(Trait.SORCERER, Trait.TELEPATH_STRONG)),

    // ===== tommy =====
    TOMMY_TIME(Power.TOMMY, 1, "time", toggle().alt(Act.TOGGLE).cost(3).group(Group.MOVE).icon("minecraft:clock")
            .fixed().res(Resource.SLOW_TIME).family(Family.TIME).on(d -> d.toggle(Toggle.TOMMY_TIME))),
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
            .fixed().res(Resource.FORM).traits(Trait.SORCERER).family(Family.SHAPE)),
    LOKI_VANISH(Power.LOKI, 4, "vanish", press().cost(2).group(Group.MOVE).icon("minecraft:ender_pearl").traits(Trait.SORCERER)),
    // (3.6: his Jotun-god nature makes a custom hero a Bearer of the Infinity Stones.)
    LOKI_FROST_GIANT(Power.LOKI, 5, "frost_giant", toggle().cost(1).group(Group.BODY).icon("minecraft:packed_ice")
            .fixed().traits(Trait.SORCERER, Trait.STONE_BEARER).family(Family.SHAPE)
                    .on(d -> d.toggle(Toggle.FROST_GIANT))),

    // ===== thor =====
    THOR_HAMMER(Power.THOR, 1, "hammer", press().alt(Act.PRESS).cost(2).group(Group.BODY).icon("minecraft:mace")
            .res(Resource.WEAPON).traits(Trait.WORTHY)),
    THOR_LIGHTNING(Power.THOR, 3, "lightning", hold().cost(3).group(Group.ELEMENT).icon("minecraft:lightning_rod")
            .family(Family.BEAM).on(d -> d.is(Live.CALLING_LIGHTNING))),
    THOR_THUNDER(Power.THOR, 4, "thunder", toggle().cost(2).group(Group.ELEMENT).icon("minecraft:trident")
            .on(d -> d.toggle(Toggle.GOD_OF_THUNDER))),
    THOR_BIFROST(Power.THOR, 5, "bifrost", press().alt(Act.PRESS).cost(2).group(Group.MOVE).icon("minecraft:end_crystal").world()),
    THOR_BODY(Power.THOR, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:netherite_chestplate")
            .traits(Trait.STONE_BEARER).family(Family.BODY)),

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
            .fixed().traits(Trait.FIRE_IMMUNE, Trait.STONE_BEARER).family(Family.BODY)),

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
            .res(Resource.SOUL_CHAIN).on(d -> d.is(Live.SOUL_DRAINING))),
    THANOS_TIME(Power.THANOS, 5, "time", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:emerald")
            .family(Family.TIME).world()),
    THANOS_BODY(Power.THANOS, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:golden_chestplate").family(Family.BODY)),

    // ===== ultron =====
    ULTRON_REPULSOR(Power.ULTRON, 1, "repulsor", hold().alt(Act.PRESS).cost(2).group(Group.ENERGY).icon("minecraft:redstone_torch")
            .res(Resource.BEAM_FLAG, Resource.IRON_CHARGE).family(Family.BEAM).on(PowerData::beamActive)),
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
            .traits(Trait.TELEPATH).on(d -> d.is(Live.CEREBRO))),

    // ===== heimdall (3.5) =====
    // (The rows of 3.5 are written by the skeleton from the plan's table; each lane fixes its own hero's data here.)
    HEIMDALL_HOFUND(Power.HEIMDALL, 1, "hofund", press().alt(Act.PRESS).cost(2).group(Group.BODY).icon("enhanced:hofund")
            .on(d -> d.is(Live.HEIMDALL_SLASHING))),
    HEIMDALL_SIGHT(Power.HEIMDALL, 2, "sight", toggle().alt(Act.HOLD).cost(2).group(Group.MIND).icon("minecraft:spyglass")
            .on(d -> d.toggle(Toggle.HEIMDALL_SIGHT))),
    HEIMDALL_BIFROST(Power.HEIMDALL, 3, "bifrost", press().alt(Act.PRESS).cost(2).group(Group.MOVE).icon("minecraft:beacon").world()),
    HEIMDALL_WARD(Power.HEIMDALL, 4, "ward", hold().alt(Act.PRESS).cost(2).group(Group.ENERGY).icon("minecraft:shield")
            .on(d -> d.is(Live.HEIMDALL_WARD))),
    HEIMDALL_STRIKE(Power.HEIMDALL, 5, "strike", press().alt(Act.PRESS).cost(3).group(Group.ELEMENT).icon("minecraft:amethyst_shard")
            .world()),
    HEIMDALL_BODY(Power.HEIMDALL, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:golden_leggings")
            .traits(Trait.STONE_BEARER).family(Family.BODY)),

    // ===== odin (3.5) =====
    ODIN_GUNGNIR(Power.ODIN, 1, "gungnir", press().alt(Act.PRESS).cost(3).group(Group.ENERGY).icon("enhanced:gungnir")
            .res(Resource.GUNGNIR).on(d -> d.is(Live.ODIN_THROWN))),
    ODIN_ODINFORCE(Power.ODIN, 2, "odinforce", hold().alt(Act.PRESS).cost(3).group(Group.ENERGY).icon("minecraft:golden_apple")
            .res(Resource.SHIELD).traits(Trait.SORCERER).family(Family.SHIELD).on(PowerData::chaosShieldActive)),
    ODIN_BANISH(Power.ODIN, 3, "banish", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:nether_star").world()),
    ODIN_RAVENS(Power.ODIN, 4, "ravens", press().alt(Act.PRESS).cost(2).group(Group.MIND).icon("minecraft:feather")
            .on(d -> d.is(Live.ODIN_RAVENS))),
    ODIN_COMMAND(Power.ODIN, 5, "command", press().alt(Act.TOGGLE).cost(3).group(Group.MIND).icon("minecraft:goat_horn").world()
            .on(d -> d.toggle(Toggle.ODINSLEEP))),
    ODIN_BODY(Power.ODIN, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:gold_ingot")
            .traits(Trait.STONE_BEARER).family(Family.BODY)),

    // ===== hela (3.5) =====
    HELA_NECROSWORDS(Power.HELA, 1, "necroswords", press().alt(Act.PRESS).cost(2).group(Group.ENERGY).icon("minecraft:netherite_sword")
            .traits(Trait.DARK_MAGIC)),
    HELA_BLADE_WALL(Power.HELA, 2, "blade_wall", press().alt(Act.PRESS).cost(2).group(Group.ELEMENT).icon("minecraft:pointed_dripstone")
            .traits(Trait.DARK_MAGIC)),
    HELA_GODDESS(Power.HELA, 3, "goddess", toggle().alt(Act.PRESS).cost(2).group(Group.BODY).icon("minecraft:wither_skeleton_skull")
            .traits(Trait.DARK_MAGIC).on(d -> d.toggle(Toggle.HELA_FORM))),
    HELA_ARMY(Power.HELA, 4, "army", press().alt(Act.PRESS).cost(3).group(Group.MIND).icon("minecraft:bone")
            .res(Resource.UNDEAD).traits(Trait.DARK_MAGIC).world().on(d -> d.is(Live.HELA_SUMMONING))),
    HELA_GRASP(Power.HELA, 5, "grasp", hold().cost(2).group(Group.ENERGY).icon("minecraft:crying_obsidian")
            .traits(Trait.DARK_MAGIC).on(d -> d.is(Live.HELA_GRASPING))),
    HELA_BODY(Power.HELA, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:netherite_helmet")
            .traits(Trait.DARK_MAGIC, Trait.STONE_BEARER).family(Family.BODY)),

    // ===== doctor_strange (3.6) =====
    // (The rows of 3.6, written by the skeleton from the plan's tables and fixed by lane L3. G is his flight key, not a
    // row: the Flight module's style "cloak". Y is the Eye: at most one Y row per custom hero. The Shield of the
    // Seraphim is its own state, Live.STRANGE_SHIELD, not the Chaos Shield's record: Resource.SHIELD keeps one shield.)
    STRANGE_WHIP(Power.DOCTOR_STRANGE, 1, "whip", press().alt(Act.PRESS).cost(2).group(Group.ENERGY)
            .icon("minecraft:blaze_rod")
            .traits(Trait.SORCERER)),
    STRANGE_SHIELDS(Power.DOCTOR_STRANGE, 3, "shields", hold().alt(Act.PRESS).cost(2).group(Group.ENERGY)
            .icon("minecraft:orange_stained_glass").res(Resource.SHIELD).traits(Trait.SORCERER).family(Family.SHIELD)
            .on(d -> d.is(Live.STRANGE_SHIELD))),
    STRANGE_BANDS(Power.DOCTOR_STRANGE, 4, "bands", press().alt(Act.TOGGLE).cost(3).group(Group.MIND)
            .icon("minecraft:red_wool")
            .altRes(Resource.PHASE).traits(Trait.SORCERER)),
    STRANGE_PORTAL(Power.DOCTOR_STRANGE, 5, "portal", press().alt(Act.TOGGLE).cost(3).group(Group.MOVE)
            .icon("enhanced:sling_ring")
            .traits(Trait.SORCERER).world()),
    /** The Eye of Agamotto on the Y key: it works only with an Eye in the inventory. */
    STRANGE_EYE(Power.DOCTOR_STRANGE, 6, "eye", toggle().alt(Act.PRESS).cost(3).group(Group.MIND)
            .icon("enhanced:eye_of_agamotto")
            .res(Resource.SLOW_TIME).traits(Trait.SORCERER).family(Family.TIME).on(d -> d.toggle(Toggle.TIME_SLOW))),
    STRANGE_BODY(Power.DOCTOR_STRANGE, 0, "body", body().cost(1).group(Group.BODY).icon("minecraft:red_banner")
            .traits(Trait.SORCERER).family(Family.BODY)),

    // ===== hulk (3.6) =====
    HULK_SMASH(Power.HULK, 1, "smash", press().alt(Act.PRESS).cost(2).group(Group.BODY)
            .icon("minecraft:cracked_stone_bricks")),
    HULK_CLAP(Power.HULK, 2, "clap", press().alt(Act.PRESS).cost(2).group(Group.BODY).icon("minecraft:bell")),
    HULK_LEAP(Power.HULK, 3, "leap", press().alt(Act.PRESS).cost(2).group(Group.MOVE).icon("minecraft:piston")
            .on(d -> d.is(Live.HULK_LEAPING))),
    HULK_BOULDER(Power.HULK, 4, "boulder", press().alt(Act.PRESS).cost(2).group(Group.ELEMENT)
            .icon("minecraft:mossy_cobblestone")),
    /**
     * The Transform: Bruce Banner and the Hulk (for a custom hero, the Human Form: every power off until N again); its
     * Sneak variant, Switch Hulk, is the built-in Hulk's only.
     */
    HULK_TRANSFORM(Power.HULK, 5, "transform", press().alt(Act.PRESS).cost(1).group(Group.BODY)
            .icon("minecraft:lime_dye")
            .fixed().res(Resource.FORM).on(d -> d.toggle(Toggle.HUMAN_FORM))),
    HULK_BODY(Power.HULK, 0, "body", body().cost(2).group(Group.BODY).icon("minecraft:emerald")
            .res(Resource.SCALE).family(Family.BODY)),

    // ===== wolverine (3.7) =====
    // (The rows of 3.7, written by the skeleton from the plan's tables (10.1) and fixed by lane L2. His healing factor
    // is his body's: a custom hero with wolverine.body has it if healing_factor.customHeroes.)
    WOLVERINE_SLASH(Power.WOLVERINE, 1, "slash", press().alt(Act.PRESS).cost(2).group(Group.BODY)
            .icon("minecraft:shears")),
    WOLVERINE_CLAWS(Power.WOLVERINE, 2, "claws", toggle().alt(Act.TOGGLE).cost(1).group(Group.BODY)
            .icon("minecraft:tripwire_hook").on(d -> d.toggle(Toggle.CLAWS_OUT))),
    WOLVERINE_POUNCE(Power.WOLVERINE, 3, "pounce", press().alt(Act.PRESS).cost(2).group(Group.MOVE)
            .icon("minecraft:rabbit_foot").on(d -> d.is(Live.WOLVERINE_POUNCING))),
    /** The Berserker Rage; its Sneak variant is the Healing Burst. */
    WOLVERINE_BERSERKER(Power.WOLVERINE, 4, "berserker", press().alt(Act.PRESS).cost(3).group(Group.BODY)
            .icon("minecraft:blaze_powder").on(d -> d.is(Live.WOLVERINE_BERSERK))),
    WOLVERINE_SENSES(Power.WOLVERINE, 5, "senses", press().alt(Act.TOGGLE).cost(2).group(Group.MIND)
            .icon("minecraft:spider_eye").on(d -> d.toggle(Toggle.SCENT_TRACK))),
    /** Adamantium bones and the healing factor: more health, armour and speed, heavy in water, metal for Magneto. */
    WOLVERINE_BODY(Power.WOLVERINE, 0, "body", body().cost(3).group(Group.BODY).icon("minecraft:iron_block")
            .traits(Trait.HEALING_FACTOR, Trait.METAL_BODY).family(Family.BODY)),

    // ===== deadpool (3.7) =====
    // (Fixed by lane L3. The Parry is his one shield: Resource.SHIELD.)
    DEADPOOL_KATANAS(Power.DEADPOOL, 1, "katanas", press().alt(Act.HOLD).cost(2).group(Group.BODY)
            .icon("minecraft:iron_sword").altRes(Resource.SHIELD).on(d -> d.is(Live.DEADPOOL_PARRYING))),
    DEADPOOL_PISTOLS(Power.DEADPOOL, 2, "pistols", press().alt(Act.PRESS).cost(2).group(Group.ENERGY)
            .icon("minecraft:crossbow").on(d -> d.is(Live.DEADPOOL_AIMING))),
    DEADPOOL_BELT(Power.DEADPOOL, 3, "belt", press().alt(Act.PRESS).cost(2).group(Group.MOVE)
            .icon("minecraft:ender_pearl")),
    /** The Taunt; its Sneak variant is the Chimichanga Break (the Healing Burst). */
    DEADPOOL_TAUNT(Power.DEADPOOL, 4, "taunt", press().alt(Act.PRESS).cost(1).group(Group.MIND)
            .icon("minecraft:cookie")),
    /** Dogpool (one per player); its Sneak variant is the Smoke Vanish. */
    DEADPOOL_DOGPOOL(Power.DEADPOOL, 5, "dogpool", press().alt(Act.PRESS).cost(3).group(Group.MIND)
            .icon("minecraft:bone")),
    DEADPOOL_BODY(Power.DEADPOOL, 0, "body", body().cost(3).group(Group.BODY).icon("minecraft:red_concrete")
            .traits(Trait.HEALING_FACTOR).family(Family.BODY)),

    // ===== captain_america (3.8) =====
    // (The rows of 3.8, written by the skeleton from the plan's table (11.1) and fixed by each hero's lane: lane L1
    // here. The shield's throw needs a shield in hand: for a custom hero any shield flies and returns, only the
    // Vibranium Shield ricochets.)
    CAPTAIN_SHIELD_THROW(Power.CAPTAIN_AMERICA, 1, "shield_throw", press().alt(Act.PRESS).cost(2).group(Group.BODY)
            .icon("minecraft:shield")),
    CAPTAIN_SHIELD_SLAM(Power.CAPTAIN_AMERICA, 2, "shield_slam", press().alt(Act.PRESS).cost(2).group(Group.BODY)
            .icon("minecraft:anvil").on(d -> d.is(Live.CAP_SLAMMING))),
    CAPTAIN_SHIELD_CHARGE(Power.CAPTAIN_AMERICA, 3, "shield_charge", press().alt(Act.PRESS).cost(2).group(Group.MOVE)
            .icon("minecraft:iron_boots").on(d -> d.is(Live.CAP_CHARGING))),
    /** Assemble; its Sneak variant is Inspire. */
    CAPTAIN_ASSEMBLE(Power.CAPTAIN_AMERICA, 4, "assemble", press().alt(Act.PRESS).cost(2).group(Group.MIND)
            .icon("minecraft:white_banner")),
    CAPTAIN_COMBO(Power.CAPTAIN_AMERICA, 5, "combo", press().alt(Act.PRESS).cost(1).group(Group.BODY)
            .icon("minecraft:leather_boots")),
    /** The super-soldier: Speed II, Strength II, Resistance I the mod's way; worthy of Mjolnir. */
    CAPTAIN_BODY(Power.CAPTAIN_AMERICA, 0, "body", body().cost(2).group(Group.BODY).icon("minecraft:blue_banner")
            .traits(Trait.WORTHY).family(Family.BODY)),

    // ===== red_hulk (3.8) =====
    // (Lane L2. The Energy Absorb is his one shield: Resource.SHIELD; Ross's form is his FORM.)
    RED_HULK_SMASH(Power.RED_HULK, 1, "smash", press().alt(Act.PRESS).cost(2).group(Group.BODY)
            .icon("minecraft:cracked_deepslate_bricks").on(d -> d.is(Live.RED_HULK_GRABBING))),
    RED_HULK_HEAT_WAVE(Power.RED_HULK, 2, "heat_wave", press().alt(Act.PRESS).cost(2).group(Group.ELEMENT)
            .icon("minecraft:magma_block")),
    RED_HULK_SUPER_JUMP(Power.RED_HULK, 3, "super_jump", hold().alt(Act.PRESS).cost(2).group(Group.MOVE)
            .icon("minecraft:slime_block").on(d -> d.is(Live.RED_HULK_LEAPING))),
    /** The Anger Burst; its Sneak variant is the Energy Absorb (held). */
    RED_HULK_BURST(Power.RED_HULK, 4, "burst", press().alt(Act.HOLD).cost(3).group(Group.ELEMENT)
            .icon("minecraft:fire_charge").altRes(Resource.SHIELD).on(d -> d.is(Live.RED_HULK_ABSORBING))),
    /** The Transform: Ross and Red Hulk (for a custom hero: every power off until N again, as hulk.transform). */
    RED_HULK_TRANSFORM(Power.RED_HULK, 5, "transform", press().cost(1).group(Group.BODY).icon("minecraft:red_dye")
            .fixed().res(Resource.FORM).on(d -> d.toggle(Toggle.ROSS_FORM))),
    /** 1.5 times his size, Strength IV, Slowness I, fire-proof, and the anger meter. */
    RED_HULK_BODY(Power.RED_HULK, 0, "body", body().cost(3).group(Group.BODY).icon("minecraft:redstone_block")
            .res(Resource.SCALE).family(Family.BODY)),

    // ===== iron_man (3.8) =====
    // (Lanes L3 and L4. A custom hero wears no suit item: these rows give him the Mark 85's core set, and the body row
    // is his "suit" (armour and the suit energy meter). G is flight, not a row.)
    /** Boosted repulsors; its Sneak variant is the Energy Blades (on and off). */
    IRON_MAN_REPULSOR(Power.IRON_MAN, 1, "repulsor", press().alt(Act.TOGGLE).cost(2).group(Group.ENERGY)
            .icon("minecraft:glowstone_dust").on(d -> d.is(Live.IRON_BLADES))),
    /** The Unibeam (for a custom hero, the Mark 85's giant chest laser). */
    IRON_MAN_UNIBEAM(Power.IRON_MAN, 3, "unibeam", hold().cost(2).group(Group.ENERGY).icon("minecraft:beacon")),
    /** The giant energy shield (held); its Sneak variant is the Lightning Refocuser. */
    IRON_MAN_SHIELD(Power.IRON_MAN, 4, "shield", hold().alt(Act.PRESS).cost(2).group(Group.ENERGY)
            .icon("minecraft:light_blue_stained_glass").res(Resource.SHIELD).on(d -> d.is(Live.IRON_SHIELD))),
    /** Homing mini-missiles; its Sneak variant is the nanotech drones. */
    IRON_MAN_MISSILES(Power.IRON_MAN, 5, "missiles", press().alt(Act.PRESS).cost(2).group(Group.ENERGY)
            .icon("minecraft:firework_rocket")),
    /** A custom hero's suit: +8 armour, +4 toughness and the suit energy meter (no item). */
    IRON_MAN_SUIT(Power.IRON_MAN, 0, "suit", body().cost(3).group(Group.BODY).icon("minecraft:netherite_chestplate")
            .res(Resource.SUIT_ENERGY).family(Family.BODY)),

    // ===== falcon (3.8) =====
    // (Lane L5. G is flight with the wings, not a row; a custom hero with these rows has Falcon's wings drawn.)
    FALCON_BLASTERS(Power.FALCON, 1, "blasters", press().alt(Act.PRESS).cost(2).group(Group.ENERGY)
            .icon("minecraft:redstone_torch")),
    FALCON_DIVE(Power.FALCON, 3, "dive", press().alt(Act.PRESS).cost(2).group(Group.MOVE).icon("minecraft:feather")
            .on(d -> d.is(Live.FALCON_DIVING))),
    /** The Wing Shield (held): his one shield. */
    FALCON_WING_SHIELD(Power.FALCON, 4, "wing_shield", hold().alt(Act.PRESS).cost(2).group(Group.BODY)
            .icon("minecraft:elytra").res(Resource.SHIELD).on(d -> d.is(Live.FALCON_WING_SHIELD))),
    /** Redwing, his drone; its Sneak variant is Redwing Strike. */
    FALCON_REDWING(Power.FALCON, 5, "redwing", press().alt(Act.PRESS).cost(2).group(Group.MIND)
            .icon("minecraft:red_stained_glass").on(d -> d.toggle(Toggle.REDWING_OUT))),
    FALCON_BODY(Power.FALCON, 0, "body", body().cost(2).group(Group.BODY).icon("minecraft:gray_banner")
            .family(Family.BODY)),

    // ===== love (3.8) =====
    // (Lane L8. Her axe is Thor's thrown weapon: Resource.WEAPON. G is flight while she holds Stormbreaker.)
    LOVE_AXE(Power.LOVE, 1, "axe", press().alt(Act.PRESS).cost(2).group(Group.BODY).icon("enhanced:stormbreaker")
            .res(Resource.WEAPON)),
    LOVE_LIGHTNING(Power.LOVE, 3, "lightning", hold().cost(3).group(Group.ELEMENT).icon("minecraft:lightning_rod")),
    /** The purple eye laser (held). */
    LOVE_EYE_LASER(Power.LOVE, 4, "eye_laser", hold().cost(2).group(Group.ENERGY).icon("minecraft:amethyst_shard")
            .on(d -> d.is(Live.BEAMING))),
    LOVE_BIFROST(Power.LOVE, 5, "bifrost", press().alt(Act.PRESS).cost(2).group(Group.MOVE)
            .icon("minecraft:end_crystal").world()),
    /** Thor's daughter: 32 health, worthy of Mjolnir. */
    LOVE_BODY(Power.LOVE, 0, "body", body().cost(2).group(Group.BODY).icon("minecraft:golden_chestplate")
            .traits(Trait.WORTHY).family(Family.BODY)),

    // ===== variants (3.5, the creator lane) =====
    // Every Sneak variant of a built-in hero's key as a card of its own (the user: "all abilities listed, like Billy's
    // shield"). A variant row is its parent's Sneak half: picked alone it sits on any key (plain or Sneak) and presses
    // the parent's key with the variant (HeroKit binds it as the parent's ALT), so nothing of the hero is copied; the
    // kit counts the parent as had ({@link HeroKit#has}) so the hero's code runs for it. Picked with its parent, the
    // parent keeps its main action only (the rules). A variant that only undoes or steers what its parent made needs
    // the parent.
    MAGNETO_MAGNETIC_PULL(MAGNETO_METAL, "magnetic_pull", variant(MAGNETO_METAL).icon("minecraft:lodestone")),
    WANDA_HEX_DISSOLVE(WANDA_HEX, "hex_dissolve", variant(WANDA_HEX).icon("minecraft:glass").requires("wanda.hex")),
    // (Not a variant: the Scarlet Witch's own Build Magic, with her numbers and her Instant switch, beside Billy's.)
    WANDA_BUILD_MAGIC(Power.SCARLET_WITCH, 6, "build_magic",
            press().cost(2).group(Group.ELEMENT).icon("minecraft:quartz_bricks")
            .fixed().res(Resource.BUILD).traits(Trait.SORCERER, Trait.TELEPATH_STRONG)),
    QUICKSILVER_WALKMAN(QUICKSILVER_TIME, "walkman", variant(QUICKSILVER_TIME).icon("minecraft:music_disc_cat")
            .requires("quicksilver.time").on(PowerData::walkman)),
    QUICKSILVER_DASH(QUICKSILVER_SPEED, "dash", variant(QUICKSILVER_SPEED).icon("minecraft:feather")),
    QUICKSILVER_PICKPOCKET(QUICKSILVER_BLITZ, "pickpocket", variant(QUICKSILVER_BLITZ).icon("minecraft:bundle")),
    BILLY_HEX_BARRIER(BILLY_FREEZE, "hex_barrier", variant(BILLY_FREEZE).icon("minecraft:light_blue_stained_glass")
            .family(Family.SHIELD).on(PowerData::chaosShieldActive)),
    TOMMY_WALL_RUN(TOMMY_TIME, "wall_run", variant(TOMMY_TIME).icon("minecraft:chainmail_boots")
            .requires("tommy.speed", "quicksilver.speed", "tommy.time", "quicksilver.time").on(d
                    -> !d.toggle(Toggle.TOMMY_WALL_RUN_OFF))),
    TOMMY_DASH(TOMMY_SPEED, "dash", variant(TOMMY_SPEED).icon("minecraft:wind_charge")),
    TOMMY_SONIC_BOOM(TOMMY_WHIRLWIND, "sonic_boom", variant(TOMMY_WHIRLWIND).icon("minecraft:firework_rocket")
            .on(d -> d.is(Live.SONIC_BOOM))),
    TOMMY_DISARM_BLITZ(TOMMY_VIBRATE, "disarm_blitz", variant(TOMMY_VIBRATE).icon("minecraft:golden_sword")),
    LOKI_DAGGER_FAN(LOKI_DAGGERS, "dagger_fan", variant(LOKI_DAGGERS).icon("minecraft:golden_sword")),
    THOR_SHATTER(THOR_HAMMER, "shatter", variant(THOR_HAMMER).icon("minecraft:heavy_core")),
    THOR_BIFROST_LOOK(THOR_BIFROST, "bifrost_look", variant(THOR_BIFROST).icon("minecraft:spyglass")),
    PHOENIX_MIND_SENSE(PHOENIX_MIND, "mind_sense", variant(PHOENIX_MIND).icon("minecraft:sculk_sensor")
            .on(d -> d.is(Live.MIND_SENSING))),
    PHOENIX_PSIONIC_FIELD(PHOENIX_CONTROL, "psionic_field", variant(PHOENIX_CONTROL).icon("minecraft:fire_charge")
            .on(d -> d.is(Live.PHOENIX_PSIONIC))),
    SPIDER_WEB_BURST(SPIDER_WEB, "web_burst", variant(SPIDER_WEB).icon("minecraft:white_wool")),
    SPIDER_WEB_SWING(SPIDER_ZIP, "web_swing", variant(SPIDER_ZIP).icon("minecraft:lead").on(d -> d.is(Live.SWINGING))),
    SPIDER_SENSE(SPIDER_CRAWL, "sense", variant(SPIDER_CRAWL).icon("minecraft:bell").family(Family.MIND)
            .on(d -> d.toggle(Toggle.SPIDER_SENSE))),
    SPIDER_INSTANT_KILL(SPIDER_LEGS, "instant_kill", variant(SPIDER_LEGS).icon("minecraft:iron_sword")
            .requires("spider.legs").on(d -> d.is(Live.INSTANT_KILL))),
    THANOS_TITAN_SLAM(THANOS_POWER, "titan_slam", variant(THANOS_POWER).icon("minecraft:mace")),
    THANOS_SPACE_PULL(THANOS_SPACE, "space_pull", variant(THANOS_SPACE).icon("minecraft:fishing_rod")),
    THANOS_REALITY_VEIL(THANOS_REALITY, "reality_veil", variant(THANOS_REALITY).icon("minecraft:red_stained_glass")
            .on(d -> d.toggle(Toggle.REALITY_VEIL))),
    THANOS_SOUL_SIGHT(THANOS_SOUL, "soul_sight", variant(THANOS_SOUL).icon("minecraft:ender_eye").on(d
            -> d.is(Live.SOUL_SIGHT))),
    THANOS_TIME_LOCK(THANOS_TIME, "time_lock", variant(THANOS_TIME).icon("minecraft:clock")),
    ULTRON_OVERCHARGE(ULTRON_REPULSOR, "overcharge", variant(ULTRON_REPULSOR).icon("minecraft:redstone_block")
            .on(d -> d.is(Live.ULTRON_CHARGING))),
    ULTRON_SLAM(ULTRON_GRAVITY, "slam", variant(ULTRON_GRAVITY).icon("minecraft:anvil")),
    ULTRON_HACK(ULTRON_UPLOAD, "hack", variant(ULTRON_UPLOAD).icon("minecraft:repeater")),
    ULTRON_COMMAND(ULTRON_ARMY, "command", variant(ULTRON_ARMY).icon("minecraft:target").requires("ultron.army")),
    APOCALYPSE_ENTOMB(APOCALYPSE_SAND, "entomb", variant(APOCALYPSE_SAND).icon("minecraft:sandstone")),
    APOCALYPSE_SANDSTORM(APOCALYPSE_BESTOW, "sandstorm", variant(APOCALYPSE_BESTOW).icon("minecraft:suspicious_sand")
            .on(d -> d.is(Live.SANDSTORM))),
    AGATHA_HEX_MARK(AGATHA_PURPLE_BEAM, "hex_mark", variant(AGATHA_PURPLE_BEAM).icon("minecraft:purple_dye")),
    CYCLOPS_RICOCHET(CYCLOPS_OPTIC_BLAST, "ricochet",
            variant(CYCLOPS_OPTIC_BLAST).icon("minecraft:prismarine_crystals")),
    CYCLOPS_OPEN_EYES(CYCLOPS_GLASSES, "open_eyes", variant(CYCLOPS_GLASSES).icon("minecraft:red_stained_glass")
            .family(Family.BEAM).requires("cyclops.glasses").on(d -> d.is(Live.CYCLOPS_UNLEASHED))),

    // ===== variants (3.6) =====
    // (The Sneak variants of the heroes of 3.6, as the creator lane of 3.5 made them: data only, their lanes fix them.
    // The Hulk's Switch Hulk has none: it is the built-in Hulk's own.)
    STRANGE_WHIP_GRAPPLE(STRANGE_WHIP, "whip_grapple", variant(STRANGE_WHIP).icon("minecraft:fishing_rod")),
    STRANGE_SERAPHIM_DOME(STRANGE_SHIELDS, "seraphim_dome", variant(STRANGE_SHIELDS)
            .icon("minecraft:orange_stained_glass_pane")
            .family(Family.SHIELD)),
    STRANGE_ASTRAL(STRANGE_BANDS, "astral", variant(STRANGE_BANDS).icon("minecraft:ghast_tear")
            .on(d -> d.toggle(Toggle.STRANGE_ASTRAL))),
    STRANGE_MIRROR(STRANGE_PORTAL, "mirror", variant(STRANGE_PORTAL).icon("minecraft:glass")
            .on(d -> d.toggle(Toggle.STRANGE_MIRROR))),
    /** The Eye's Rewind: needs the Eye too. */
    STRANGE_EYE_REWIND(STRANGE_EYE, "eye_rewind", variant(STRANGE_EYE).icon("minecraft:clock")),
    HULK_GRAB_SMASH(HULK_SMASH, "grab_smash", variant(HULK_SMASH).icon("minecraft:lead")
            .on(d -> d.is(Live.HULK_GRABBING))),
    HULK_ROAR(HULK_CLAP, "roar", variant(HULK_CLAP).icon("minecraft:goat_horn")),
    HULK_BOUND(HULK_LEAP, "bound", variant(HULK_LEAP).icon("minecraft:feather").on(d -> d.is(Live.HULK_LEAPING))),
    HULK_GAMMA_CHARGE(HULK_BOULDER, "gamma_charge", variant(HULK_BOULDER).icon("minecraft:lime_stained_glass")
            .on(d -> d.is(Live.HULK_CHARGING))),

    // ===== variants (3.7) =====
    // (The Sneak variants of Wolverine and Deadpool: data only, lanes L2 and L3 fix them. The two Healing Bursts work
    // for a custom hero without the healing factor too: a plain heal.)
    WOLVERINE_SPIN_SLASH(WOLVERINE_SLASH, "spin_slash", variant(WOLVERINE_SLASH).icon("minecraft:iron_axe")),
    /** Claw Climb needs the claws. */
    WOLVERINE_CLAW_CLIMB(WOLVERINE_CLAWS, "claw_climb", variant(WOLVERINE_CLAWS).icon("minecraft:ladder")
            .requires("wolverine.claws").on(d -> d.toggle(Toggle.CLAW_CLIMB))),
    WOLVERINE_FERAL_LEAP(WOLVERINE_POUNCE, "feral_leap", variant(WOLVERINE_POUNCE).icon("minecraft:feather")),
    WOLVERINE_HEALING_BURST(WOLVERINE_BERSERKER, "healing_burst", variant(WOLVERINE_BERSERKER)
            .icon("minecraft:golden_apple")),
    WOLVERINE_TRACK(WOLVERINE_SENSES, "track", variant(WOLVERINE_SENSES).icon("minecraft:compass")
            .on(d -> d.toggle(Toggle.SCENT_TRACK))),
    DEADPOOL_PARRY(DEADPOOL_KATANAS, "parry", variant(DEADPOOL_KATANAS).icon("minecraft:shield")
            .family(Family.SHIELD).on(d -> d.is(Live.DEADPOOL_PARRYING))),
    DEADPOOL_TRICK_SHOT(DEADPOOL_PISTOLS, "trick_shot", variant(DEADPOOL_PISTOLS).icon("minecraft:arrow")),
    DEADPOOL_DROP_IN(DEADPOOL_BELT, "drop_in", variant(DEADPOOL_BELT).icon("minecraft:pointed_dripstone")),
    DEADPOOL_CHIMICHANGA(DEADPOOL_TAUNT, "chimichanga", variant(DEADPOOL_TAUNT).icon("minecraft:bread")),
    DEADPOOL_SMOKE(DEADPOOL_DOGPOOL, "smoke", variant(DEADPOOL_DOGPOOL).icon("minecraft:gunpowder")),

    // ===== variants (3.8) =====
    // (The Sneak variants of the heroes of 3.8: data only, their lanes fix them.)
    CAPTAIN_RICOCHET(CAPTAIN_SHIELD_THROW, "ricochet", variant(CAPTAIN_SHIELD_THROW).icon("minecraft:target")),
    CAPTAIN_SUPER_LEAP(CAPTAIN_SHIELD_SLAM, "super_leap", variant(CAPTAIN_SHIELD_SLAM).icon("minecraft:rabbit_foot")),
    CAPTAIN_SHIELD_BASH(CAPTAIN_SHIELD_CHARGE, "shield_bash", variant(CAPTAIN_SHIELD_CHARGE)
            .icon("minecraft:iron_door")),
    CAPTAIN_INSPIRE(CAPTAIN_ASSEMBLE, "inspire", variant(CAPTAIN_ASSEMBLE).icon("minecraft:golden_apple")),
    CAPTAIN_FLIP_KICK(CAPTAIN_COMBO, "flip_kick", variant(CAPTAIN_COMBO).icon("minecraft:chainmail_boots")),
    RED_HULK_GRAB_SLAM(RED_HULK_SMASH, "grab_slam", variant(RED_HULK_SMASH).icon("minecraft:lead")
            .on(d -> d.is(Live.RED_HULK_GRABBING))),
    RED_HULK_ROAR(RED_HULK_HEAT_WAVE, "roar", variant(RED_HULK_HEAT_WAVE).icon("minecraft:goat_horn")),
    RED_HULK_BOUND(RED_HULK_SUPER_JUMP, "bound", variant(RED_HULK_SUPER_JUMP).icon("minecraft:piston")
            .on(d -> d.is(Live.RED_HULK_LEAPING))),
    /** The Energy Absorb (held): his one shield. */
    RED_HULK_ABSORB(RED_HULK_BURST, "absorb", variant(RED_HULK_BURST).icon("minecraft:respawn_anchor")
            .res(Resource.SHIELD).family(Family.SHIELD).on(d -> d.is(Live.RED_HULK_ABSORBING))),
    IRON_MAN_ENERGY_BLADES(IRON_MAN_REPULSOR, "energy_blades", variant(IRON_MAN_REPULSOR).icon("minecraft:echo_shard")
            .on(d -> d.is(Live.IRON_BLADES))),
    IRON_MAN_LIGHTNING_REFOCUSER(IRON_MAN_SHIELD, "lightning_refocuser", variant(IRON_MAN_SHIELD)
            .icon("minecraft:lightning_rod")),
    IRON_MAN_DRONES(IRON_MAN_MISSILES, "drones", variant(IRON_MAN_MISSILES).icon("minecraft:phantom_membrane")),
    FALCON_WRIST_ROCKET(FALCON_BLASTERS, "wrist_rocket", variant(FALCON_BLASTERS).icon("minecraft:firework_star")),
    FALCON_BOOST_CLIMB(FALCON_DIVE, "boost_climb", variant(FALCON_DIVE).icon("minecraft:wind_charge")),
    FALCON_WING_SLASH(FALCON_WING_SHIELD, "wing_slash", variant(FALCON_WING_SHIELD).icon("minecraft:iron_sword")),
    FALCON_REDWING_STRIKE(FALCON_REDWING, "redwing_strike", variant(FALCON_REDWING).icon("minecraft:arrow")),
    LOVE_THUNDER_STRIKE(LOVE_AXE, "thunder_strike", variant(LOVE_AXE).icon("minecraft:trident")),
    LOVE_BIFROST_LOOK(LOVE_BIFROST, "bifrost_look", variant(LOVE_BIFROST).icon("minecraft:spyglass"));

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
    /**
     * The key it is on for the hero himself (1-5, R G V B N), 0 for a body (a passive), 6 for the Y key (Billy's and
     * the Scarlet Witch's Build Magic, 3.6: Doctor Strange's Eye).
     */
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
    /** 3.5: the row whose Sneak variant this row is, or null for a row of its own. */
    @Nullable
    private final HeroAbility parent;

    /** A variant row (3.5): its parent's Sneak half, on its parent's hero and key. */
    HeroAbility(HeroAbility parent, String name, Spec spec) {
        this(parent.hero, parent.home, name, spec.parent(parent));
    }

    HeroAbility(Power hero, int home, String name, Spec spec) {
        this.hero = hero;
        this.home = home;
        this.name = name;
        this.id = hero.getSerializedName().replace("scarlet_witch", "wanda").replace("spider_man", "spider")
                .replace("professor_x", "xavier").replace("doctor_strange", "strange")
                .replace("captain_america", "captain") + "." + name;
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
        this.parent = spec.parent;
    }

    // ---- CatalogEntry ----

    /** {@code thor.lightning}: the id a hero's JSON names it by. */
    @Override
    public String id() {
        return id;
    }

    /**
     * The id of its hero in ids ({@code thor}, {@code wanda}, {@code spider}, {@code xavier}, 3.6: {@code strange},
     * 3.8: {@code captain}...).
     */
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

    /** Whether it goes on one of the five key boxes: a body needs no key, and Billy's Build Magic has its own Y key. */
    public boolean takesBox() {
        return home >= 1 && home <= CustomHero.KEYS;
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

    /**
     * 3.5: the row whose Sneak variant this row is (Billy's Hex Barrier is {@link #BILLY_FREEZE}'s), or null. A custom
     * hero who picks it has its parent's code run for it: its key presses the parent's with the variant.
     */
    @Nullable
    public HeroAbility parent() {
        return parent;
    }

    /** 3.5: whether it is a Sneak variant with a card of its own ({@link #parent}). */
    public boolean isVariant() {
        return parent != null;
    }

    /** 3.5: the variant row of a row with a Sneak variant, or null (its variant has no card, or it has none). */
    @Nullable
    public HeroAbility variant() {
        return Variants.OF.get(this);
    }

    /** The variant row of each parent (built once, after the table). */
    private static final class Variants {
        static final java.util.Map<HeroAbility, HeroAbility> OF = new java.util.EnumMap<>(HeroAbility.class);

        static {
            for (HeroAbility a : values()) {
                if (a.parent != null) {
                    OF.put(a.parent, a);
                }
            }
        }

        private Variants() {
        }
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

    /**
     * A variant row's data (3.5): it acts as its parent's Sneak variant acts, costs as its parent, scales as it, gives
     * its roles, is world-scale if it is, and holds every resource its parent holds (the parent's code runs for it).
     */
    private static Spec variant(HeroAbility parent) {
        Spec spec = new Spec(parent.alt).cost(parent.costPerPoint).group(parent.group).icon(parent.icon);
        spec.resources.addAll(parent.resources);
        spec.resources.addAll(parent.altResources);
        spec.traits.addAll(parent.traits);
        spec.scales = parent.scales.isEmpty() ? EnumSet.noneOf(Scales.class) : EnumSet.copyOf(parent.scales);
        spec.world = parent.world;
        return spec;
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
        HeroAbility parent;

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

        Spec parent(HeroAbility parent) {
            this.parent = parent;
            return this;
        }
    }
}
