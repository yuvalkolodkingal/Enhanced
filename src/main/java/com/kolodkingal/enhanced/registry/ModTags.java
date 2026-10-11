package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Armor that makes its wearer a valid target for Magneto's Metal Telekinesis. */
    public static final TagKey<Item> MAGNETIC_ARMOR = TagKey.create(Registries.ITEM, Enhanced.id("magnetic_armor"));
    /** Metal items Magneto can rip off a creature (and lift when dropped). */
    public static final TagKey<Item> MAGNETIC_ITEMS = TagKey.create(Registries.ITEM, Enhanced.id("magnetic"));
    /** Weapons: what Tommy's Disarm Blitz snatches from players. */
    public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM, Enhanced.id("weapons"));
    /** Items no speedster can snatch (Mjolnir is far too heavy for anyone but its owner). */
    public static final TagKey<Item> CANNOT_BE_SNATCHED = TagKey.create(Registries.ITEM, Enhanced.id("cannot_be_snatched"));
    /** Extra items that can be melted into shrapnel (18 shards each). */
    public static final TagKey<Item> SHRAPNEL_AMMO = TagKey.create(Registries.ITEM, Enhanced.id("shrapnel_ammo"));

    /** Entities Magneto can always lift: Iron Golems, minecarts. */
    public static final TagKey<EntityType<?>> MAGNETIC_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("magnetic_entities"));
    /** Projectiles the Magnetic Shield repels. */
    public static final TagKey<EntityType<?>> METALLIC_PROJECTILES = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("metallic_projectiles"));
    /** Entities too powerful for telekinesis. */
    public static final TagKey<EntityType<?>> TELEKINESIS_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("telekinesis_immune"));
    /** Entities the Hex wall cannot kill (they bounce off instead). */
    public static final TagKey<EntityType<?>> HEX_RESISTANT = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("hex_resistant"));
    /** Creatures Loki cannot shapeshift into: the bosses, and his own illusions. */
    public static final TagKey<EntityType<?>> SHAPESHIFT_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("shapeshift_immune"));
    /** Entities too powerful for Billy's Freeze. */
    public static final TagKey<EntityType<?>> SPELLBOUND_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("spellbound_immune"));
    /** The bosses: Mjolnir can't pin them down. */
    public static final TagKey<EntityType<?>> PIN_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("pin_immune"));
    /** Entities too powerful for the confusion of Loki's Scepter. */
    public static final TagKey<EntityType<?>> CONFUSED_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("confused_immune"));

    /** Blocks Wanda can never tear out of the ground. */
    public static final TagKey<Block> TELEKINESIS_IMMOVABLE = TagKey.create(Registries.BLOCK, Enhanced.id("telekinesis_immovable"));

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

    private ModTags() {
    }
}
