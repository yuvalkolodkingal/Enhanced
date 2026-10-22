package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModTags {
    /**
     * Metal armour (iron, chain, gold, netherite): the armour part of the metal items. A creature or player wearing a piece
     * is metal to Magneto; his Armor &amp; Weapon Strip and the Magnetic Pull read this tag and {@link #MAGNETIC_ITEMS}.
     */
    public static final TagKey<Item> MAGNETIC_ARMOR = TagKey.create(Registries.ITEM, Enhanced.id("magnetic_armor"));
    /**
     * Metal items: everything iron, gold, copper or netherite (ingots, tools, rails, buckets, the copper blocks, the
     * storage blocks...). Magneto lifts them lying on the ground, rips them off a creature, pulls them into his inventory,
     * and a creature holding one anywhere in its equipment is metal to his R.
     */
    public static final TagKey<Item> MAGNETIC_ITEMS = TagKey.create(Registries.ITEM, Enhanced.id("magnetic"));
    /** Weapons: what Tommy's Disarm Blitz snatches from players. */
    public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM, Enhanced.id("weapons"));
    /** Items no speedster can snatch (Mjolnir is far too heavy for anyone but its owner). */
    public static final TagKey<Item> CANNOT_BE_SNATCHED = TagKey.create(Registries.ITEM, Enhanced.id("cannot_be_snatched"));
    /** Extra items that can be melted into shrapnel (18 shards each). */
    public static final TagKey<Item> SHRAPNEL_AMMO = TagKey.create(Registries.ITEM, Enhanced.id("shrapnel_ammo"));

    /** Iron Golems, minecarts, the Sentries, the Hex's Vision: metal Magneto lifts even past telekinesis_immune. */
    public static final TagKey<EntityType<?>> MAGNETIC_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("magnetic_entities"));
    /**
     * Bodies whose gear is a copy (Loki's illusions): never metal, whatever they wear or hold. Magneto's strip and his
     * Magnetic Pull dissolve them instead of taking anything.
     */
    public static final TagKey<EntityType<?>> CONJURED_BODIES = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("conjured_bodies"));
    /** Projectiles the Magnetic Shield repels. */
    public static final TagKey<EntityType<?>> METALLIC_PROJECTILES = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("metallic_projectiles"));
    /**
     * Entities no telekinesis can lift (the Ender Dragon, a fragment of Mjolnir, the Hex's Vision, Apocalypse's cocoon, the
     * Vibranium Key). The bosses are not in it: telekinesis holds them as bosses.
     */
    public static final TagKey<EntityType<?>> TELEKINESIS_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("telekinesis_immune"));
    /**
     * Entities the villain heroes' pulls pass by (the Ultron hero's Gravity Pull and Slam, the Thanos hero's Space Pull,
     * the boss's Choke and grabs): what telekinesis cannot lift, the vanilla bosses it holds as bosses, and the three
     * villains and Thanos's weapons.
     */
    public static final TagKey<EntityType<?>> PULL_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("pull_immune"));
    /**
     * The vanilla bosses telekinesis holds as a boss, for a short time and with no arena: the Wither, the Warden and the
     * Elder Guardian. (The Ender Dragon is too vast to hold: it stays in telekinesis_immune.)
     */
    public static final TagKey<EntityType<?>> HELD_AS_BOSSES = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("held_as_bosses"));
    /** Entities the Hex wall cannot kill (they bounce off instead). */
    public static final TagKey<EntityType<?>> HEX_RESISTANT = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("hex_resistant"));
    /** Creatures Loki cannot shapeshift into: the bosses, and his own illusions. */
    public static final TagKey<EntityType<?>> SHAPESHIFT_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("shapeshift_immune"));
    /** Entities too powerful for Billy's Freeze. */
    public static final TagKey<EntityType<?>> SPELLBOUND_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("spellbound_immune"));
    /** The bosses: Mjolnir can't pin them down. */
    public static final TagKey<EntityType<?>> PIN_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("pin_immune"));
    /**
     * Creatures the ward of the Book of the Vishanti lets through although it keeps their kind out (monsters, bosses, boss
     * minions). Empty since 3.3, when the ward began to keep the bosses out too: a datapack's way to let a kind in.
     */
    public static final TagKey<EntityType<?>> WARD_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("ward_immune"));
    /** Entities too powerful for the confusion of Loki's Scepter. */
    public static final TagKey<EntityType<?>> CONFUSED_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("confused_immune"));

    /** Blocks Wanda can never tear out of the ground. */
    public static final TagKey<Block> TELEKINESIS_IMMOVABLE = TagKey.create(Registries.BLOCK, Enhanced.id("telekinesis_immovable"));
    /**
     * Metal blocks: what Magneto's R tears out of the ground and the Magnetic Pull takes. The metal ores, the storage
     * blocks, anvils, cauldrons, rails, chains, iron bars, doors and trapdoors, lanterns, bells, hoppers, pistons, the
     * copper blocks, and other mods' metal blocks when they are installed. An ore or a raw-metal block is natural metal
     * and comes through rock; every other one is built metal, which the pull takes only in sight.
     */
    public static final TagKey<Block> MAGNETIC_BLOCKS = TagKey.create(Registries.BLOCK, Enhanced.id("magnetic_blocks"));
    /**
     * The metal ores (iron, gold, copper, ancient debris, gilded blackstone, and other mods' metal ores when they are
     * installed): natural metal, which the Magnetic Pull draws out through rock.
     */
    public static final TagKey<Block> MAGNETIC_ORES = TagKey.create(Registries.BLOCK, Enhanced.id("magnetic_ores"));

    // ---- 3.0: what the five new heroes and the three bosses share, then one region per hero (the // ===== lines) ----

    /**
     * The bosses: the vanilla ones and Ultron, Thanos and Apocalypse. Webs, Instant Kill and the boss framework read
     * it.
     */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("bosses"));
    /**
     * What the villains' fights bring with them (Sentries, Phantoms): never one-shot, never spared by an area attack.
     */
    public static final TagKey<EntityType<?>> BOSS_MINIONS = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("boss_minions"));
    /** Minds too vast to read (the Phoenix's Mind Reading and Mind Control): the bosses. */
    public static final TagKey<EntityType<?>> TELEPATHY_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("telepathy_immune"));
    /**
     * Magic: what the retaliation of the Darkhold answers when it is used on the holder (the powers' own hits, the Infinity
     * Stones' and the game's magic damage).
     */
    public static final TagKey<DamageType> MAGIC_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, Enhanced.id("magic_damage"));
    /** Damage that is the power of a mind: Wanda's beam and slam, the Phoenix Burst. Breaks Apocalypse's shield. */
    public static final TagKey<DamageType> MIND_POWER = TagKey.create(Registries.DAMAGE_TYPE, Enhanced.id("mind_power"));
    /**
     * Blows struck with a hand, claw or weapon, which the Chaos Shield stops like an arrow (see
     * WandaAbilities.isMeleeHit).
     */
    public static final TagKey<DamageType> MELEE_BLOWS = TagKey.create(Registries.DAMAGE_TYPE, Enhanced.id("melee_blows"));

    // ===== phoenix =====

    // ===== spider =====
    /** Entities no web can touch: the bosses. */
    public static final TagKey<EntityType<?>> WEB_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("web_immune"));
    /** Entities the Iron Spider's Instant Kill spares: the bosses and their minions. */
    public static final TagKey<EntityType<?>> INSTANT_KILL_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("instant_kill_immune"));
    /** Blocks Spider-Man cannot stick to (empty: a server may list some). */
    public static final TagKey<Block> CANNOT_CLING = TagKey.create(Registries.BLOCK, Enhanced.id("cannot_cling"));

    // ===== thanos =====
    /** Projectiles a Reality Veil cannot turn into bubbles. */
    public static final TagKey<EntityType<?>> REALITY_PROOF = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("reality_proof"));
    /** Damage born of an Infinity Stone: what the boss's Barrier cannot stop. */
    public static final TagKey<DamageType> STONE_BORN = TagKey.create(Registries.DAMAGE_TYPE, Enhanced.id("stone_born"));

    // ===== ultron =====
    /** Ultron's bodies and the Sentries: robots (Vision's beam and lightning hurt them more). */
    public static final TagKey<EntityType<?>> ULTRON_ROBOTS = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("ultron_robots"));
    /** Blocks the island of Sokovia carries into the sky. */
    public static final TagKey<Block> SOKOVIA_LIFTABLE = TagKey.create(Registries.BLOCK, Enhanced.id("sokovia_liftable"));

    // ===== apocalypse =====
    /** Entities Apocalypse's sand cannot entomb. */
    public static final TagKey<EntityType<?>> ENTOMB_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("entomb_immune"));
    /** Blocks the Capstone can be planted on: sand and sandstone. */
    public static final TagKey<Block> RITUAL_GROUND = TagKey.create(Registries.BLOCK, Enhanced.id("ritual_ground"));

    // ===== agatha (3.3) =====
    /**
     * Dark magic: the Darkhold's Purple Beam (whoever fires it, Agatha or a sorcerer holding the book) and Agatha's Power
     * Drain and Rune Trap. What the ward of the Book of the Vishanti keeps out.
     */
    public static final TagKey<DamageType> DARK_MAGIC = TagKey.create(Registries.DAMAGE_TYPE, Enhanced.id("dark_magic"));

    // ===== asgard (3.5) =====
    /** Hela's army: her berserkers and Fenris (lane L4 fills it). The Asgardians fight it first; Hela's allies are it. */
    public static final TagKey<EntityType<?>> HELA_ARMY = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("hela_army"));
    /** The people of Asgard: the Asgardians (lane L5). */
    public static final TagKey<EntityType<?>> ASGARDIANS = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("asgardians"));

    // ===== stones (3.6) =====
    /** The six Infinity Stones' items (lane L1: what the Gauntlet takes, what a chest of a site never holds). */
    public static final TagKey<Item> INFINITY_STONES = TagKey.create(Registries.ITEM, Enhanced.id("infinity_stones"));
    /**
     * The structures Hydra's intel map leads to: Avengers Tower (lane L4 builds it; lane L8's officer drops the map).
     */
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> ON_AVENGERS_TOWER_MAPS =
            TagKey.create(Registries.STRUCTURE, Enhanced.id("on_avengers_tower_maps"));
    // ===== hulk (3.6) =====
    /**
     * What the Hulk breaks with {@code [hulk] blockBreaking soft}: glass and panes, leaves, snow, ice, flowers, grass,
     * vines, cobwebs.
     */
    public static final TagKey<Block> HULK_SOFT = TagKey.create(Registries.BLOCK, Enhanced.id("hulk_soft"));
    /** What he breaks too with {@code all}: hardness 3 or less (dirt, sand, gravel, stone, wood, bricks...). */
    public static final TagKey<Block> HULK_SMASHABLE = TagKey.create(Registries.BLOCK, Enhanced.id("hulk_smashable"));
    /**
     * What he never breaks: bedrock, obsidian, end portal frames, barriers, spawners, the mod's pedestals, cradle and
     * seals.
     */
    public static final TagKey<Block> HULK_UNBREAKABLE = TagKey.create(Registries.BLOCK,
            Enhanced.id("hulk_unbreakable"));
    /** What the Grab Smash cannot grab (one heavy punch instead): the bosses, the Stonekeeper, the Asgardians. */
    public static final TagKey<EntityType<?>> GRAB_IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            Enhanced.id("grab_immune"));
    // ===== hydra (3.6) =====
    /** The Hydra soldiers and officers: they never hurt one another (lane L8's ally rule). */
    public static final TagKey<EntityType<?>> HYDRA = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("hydra"));
    // ===== healing (3.7) =====
    /**
     * The damage types that defeat a healer (Wolverine, Deadpool) while his healing factor is on, one tag per group of
     * the plan's rule (2.3), each switched by its {@code [healing_factor]} option: the Infinity Stones, chaos magic
     * (the Scarlet Witch, Billy), the Allfather's power (the Odinforce, Gungnir), the Phoenix, and the Darkhold (off by
     * default). Data packs may add to them ({@code healing/HealingFactor} reads them).
     */
    public static final TagKey<DamageType> DEFEATS_HEALING_STONES = damage("defeats_healing/stones");
    public static final TagKey<DamageType> DEFEATS_HEALING_CHAOS = damage("defeats_healing/chaos");
    public static final TagKey<DamageType> DEFEATS_HEALING_ODIN = damage("defeats_healing/odin");
    public static final TagKey<DamageType> DEFEATS_HEALING_PHOENIX = damage("defeats_healing/phoenix");
    public static final TagKey<DamageType> DEFEATS_HEALING_DARKHOLD = damage("defeats_healing/darkhold");
    /** The effects that switch a healer's healing off while they last: Poison (The Wolverine, 2013: Viper's poison). */
    public static final TagKey<net.minecraft.world.effect.MobEffect> STOPS_HEALING =
            TagKey.create(Registries.MOB_EFFECT, Enhanced.id("stops_healing"));
    // ===== wolverine (3.7) =====
    /** What Wolverine's claws cannot bite into for Claw Climb: glass, ice, slime, honey (lane L2). */
    public static final TagKey<Block> CLAW_SLIPPERY = TagKey.create(
            Registries.BLOCK, Enhanced.id("claw_slippery"));
    // ===== deadpool (3.7) =====
    /** What fills a chimichanga besides the bread and the carrot: cooked beef, porkchop or chicken (lane L3). */
    public static final TagKey<Item> CHIMICHANGA_FILLINGS = TagKey.create(Registries.ITEM,
            Enhanced.id("chimichanga_fillings"));
    // ===== school_lab (3.7) =====
    // ===== finders (3.7) =====
    /**
     * What may sit in the centre of the Stone Locator's recipe: the six stones and their vessels (the Scepter, the
     * Tesseract, the Eye of Agamotto, the Orb) and the Infinity Gauntlet (one with a stone in it: the recipe checks).
     */
    public static final TagKey<Item> LOCATOR_CORES = TagKey.create(Registries.ITEM, Enhanced.id("locator_cores"));
    /** The sites of each stone the Locator looks for (8.4): one tag per kind, named by the kind. */
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> STONE_SITE_MIND = site("mind");
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> STONE_SITE_TIME = site("time");
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> STONE_SITE_SPACE = site("space");
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> STONE_SITE_POWER = site("power");
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> STONE_SITE_SOUL = site("soul");
    /** The Bifrost sites of the Overworld: the way to Asgard the Locator points to for the Reality Stone. */
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> BIFROST_SITES =
            TagKey.create(Registries.STRUCTURE, Enhanced.id("bifrost_sites"));
    // ===== cottage (3.7) =====
    // ===== captain (3.8) =====
    // ===== red_hulk (3.8) =====
    /**
     * The energy that Red Hulk's Energy Absorb takes in (with fire, lightning and explosions): every beam of the mod,
     * the repulsors, the lasers and the bolts.
     */
    public static final TagKey<DamageType> ENERGY_DAMAGE = damage("energy_damage");
    // ===== iron_man (3.8) =====
    /** Iron Man's suits as items (the six): the displays' and the HUD's. */
    public static final TagKey<Item> IRON_SUITS = TagKey.create(Registries.ITEM, Enhanced.id("iron_suits"));
    /** The nanotech housings (Mark 50, Mark 85). */
    public static final TagKey<Item> NANO_SUITS = TagKey.create(Registries.ITEM, Enhanced.id("nano_suits"));
    /** The Arc Reactor and the Nano Arc Reactor: either, carried, powers a classic suit. */
    public static final TagKey<Item> ARC_REACTORS = TagKey.create(Registries.ITEM, Enhanced.id("arc_reactors"));
    /**
     * 3.8.2: the beams, rays, lasers and bolts that never get through a complete Chaos Shield, even fired from inside
     * it at Wanda herself (as Thor's lightning): Iron Man's repulsors, Unibeams and chest lasers, the drones' lasers,
     * the Refocuser's lightning, Redwing's laser and Love's eye laser. (From outside, every beam already stops at the
     * sphere.)
     */
    public static final TagKey<DamageType> CHAOS_SHIELD_STOPS = damage("chaos_shield_stops");
    // ===== falcon (3.8) =====
    // ===== tower (3.8) =====
    // ===== eternity (3.8) =====
    /** The keys of the Bifrost that open Eternity's altar: Mjolnir, Stormbreaker, Hofund, Gungnir (one's own). */
    public static final TagKey<Item> BIFROST_KEYS = TagKey.create(Registries.ITEM, Enhanced.id("bifrost_keys"));
    /**
     * What Eternity never grants (hidden from the picker, refused by the server): the operators' blocks and items, and
     * the unbreakable or unobtainable ones.
     */
    public static final TagKey<Item> WISH_FORBIDDEN = TagKey.create(Registries.ITEM, Enhanced.id("wish_forbidden"));
    /** The biomes of the Shrine of the Cosmic Entities: the End's outer islands. */
    public static final TagKey<net.minecraft.world.level.biome.Biome> HAS_ETERNITY_SHRINE =
            TagKey.create(Registries.BIOME, Enhanced.id("has_structure/eternity_shrine"));
    // ===== love (3.8) =====

    // ===== 3.1 build =====
    /**
     * Blocks no Build Magic spell changes, vanishes or moves, and a copy leaves out: what the Wither cannot break
     * (bedrock, barriers, command blocks...), the portals, spawners, trial spawners, vaults, and Mjolnir.
     */
    public static final TagKey<Block> BUILD_UNTOUCHABLE = TagKey.create(Registries.BLOCK, Enhanced.id("build_untouchable"));
    /**
     * Blocks Build Magic never makes: the untouchable ones, and those that would be a cheat or cannot stand alone
     * (budding amethyst, the dragon egg, infested stone, piston heads, bubble columns, Spider-Man's webs...).
     */
    public static final TagKey<Block> BUILD_UNCONJURABLE = TagKey.create(Registries.BLOCK, Enhanced.id("build_unconjurable"));
    /** TNT, lava and fire: Billy's spells never make them, Wanda's at most build_magic_wanda.volatilePerSpell a spell. */
    public static final TagKey<Block> BUILD_VOLATILE = TagKey.create(Registries.BLOCK, Enhanced.id("build_volatile"));
    /**
     * The ground nature made (dirt, stone, sand, ores, ice...), which a double click on a built thing stops at. No
     * sandstone: desert houses are built of it.
     */
    public static final TagKey<Block> BUILD_GROUND = TagKey.create(Registries.BLOCK, Enhanced.id("build_ground"));
    /** What the Vanish style "Plants and trees" takes away: logs, leaves, flowers, saplings, grass, vines, mushrooms... */
    public static final TagKey<Block> BUILD_PLANTS = TagKey.create(Registries.BLOCK, Enhanced.id("build_plants"));
    /**
     * Block entities whose data a Build Magic copy keeps (signs, banners, heads, chests, furnaces...). Any other one (a
     * spawner, a command block, any modded block entity) is copied as the bare block; a Move always carries everything.
     */
    public static final TagKey<BlockEntityType<?>> BUILD_COPIES_DATA = TagKey.create(Registries.BLOCK_ENTITY_TYPE, Enhanced.id("build_copies_data"));
    /**
     * Those of {@link #BUILD_COPIES_DATA} that hold items: Billy's copies of them come out empty, Wanda's keep what is
     * inside while build_magic_wanda.copiesContents is on.
     */
    public static final TagKey<BlockEntityType<?>> BUILD_HOLDS_ITEMS = TagKey.create(Registries.BLOCK_ENTITY_TYPE, Enhanced.id("build_holds_items"));

    private static TagKey<DamageType> damage(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, Enhanced.id(name));
    }

    private static TagKey<net.minecraft.world.level.levelgen.structure.Structure> site(String kind) {
        return TagKey.create(Registries.STRUCTURE, Enhanced.id("stone_site/" + kind));
    }

    private ModTags() {
    }
}
