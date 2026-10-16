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
     * Creatures the ward of the Book of the Vishanti lets through although they are monsters: the bosses and what a boss
     * fight makes. (Only monsters, in the sense of the spawn category, are kept out of a ward at all.)
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

    private ModTags() {
    }
}
