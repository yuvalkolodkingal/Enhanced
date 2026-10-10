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

    /** Blocks Wanda can never tear out of the ground. */
    public static final TagKey<Block> TELEKINESIS_IMMOVABLE = TagKey.create(Registries.BLOCK, Enhanced.id("telekinesis_immovable"));

    /** Damage Photon can absorb. */
    public static final TagKey<DamageType> ABSORBABLE_ENERGY = TagKey.create(Registries.DAMAGE_TYPE, Enhanced.id("absorbable_energy"));

    private ModTags() {
    }
}
