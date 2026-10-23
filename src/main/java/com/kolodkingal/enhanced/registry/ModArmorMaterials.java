package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.reg.Registrar;
import com.kolodkingal.enhanced.platform.reg.Registrars;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
    public static final Registrar<ArmorMaterial> ARMOR_MATERIALS =
            Registrars.create(Registries.ARMOR_MATERIAL, Enhanced.MODID);

    /**
     * Magneto's helmet: one piece that protects like a whole Netherite set
     * (20 armor, 12 toughness, 40% knockback resistance).
     */
    public static final Holder<ArmorMaterial> MAGNETO = ARMOR_MATERIALS.register("magneto", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    map.put(type, 20);
                }
            }),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(Items.NETHERITE_BLOCK),
            List.of(new ArmorMaterial.Layer(Enhanced.id("magneto_helmet"))),
            12.0F,
            0.4F));

    /**
     * Wanda's crown. It has no protection of its own: the Scarlet Witch gets her Diamond-level protection and
     * 40 max health from {@link com.kolodkingal.enhanced.ability.WandaAbilities} while she wears it.
     */
    public static final Holder<ArmorMaterial> WANDA_CROWN = ARMOR_MATERIALS.register("wanda_crown", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    map.put(type, 0);
                }
            }),
            25,
            SoundEvents.ARMOR_EQUIP_GOLD,
            () -> Ingredient.of(Items.GOLD_INGOT),
            List.of(new ArmorMaterial.Layer(Enhanced.id("wanda_crown"))),
            0.0F,
            0.0F));

    /** Quicksilver's goggles: light protection, like an iron helmet. */
    public static final Holder<ArmorMaterial> QUICKSILVER = ARMOR_MATERIALS.register("quicksilver", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    map.put(type, 2);
                }
            }),
            15,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(Enhanced.id("quicksilver_goggles"))),
            0.0F,
            0.0F));

    /**
     * Cyclops's ruby-quartz glasses (3.3): a little protection, like a leather cap. Their armour texture is empty: they are
     * drawn on the head by their own layer (the visor of his look, or red sunglasses on anyone else).
     */
    public static final Holder<ArmorMaterial> RUBY_QUARTZ = ARMOR_MATERIALS.register("ruby_quartz", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    map.put(type, 1);
                }
            }),
            15,
            SoundEvents.ARMOR_EQUIP_GENERIC,
            () -> Ingredient.of(Items.QUARTZ),
            List.of(new ArmorMaterial.Layer(Enhanced.id("ruby_quartz_glasses"))),
            0.0F,
            0.0F));

    // ===== iron_man (3.8) =====
    // (Iron Man's classic suits: one chest item is the whole suit, so its chest value is the set's (PLAN 5.3). Their
    // armour textures are empty: SuitLayer draws the suit. Lane L3 owns them.)
    /** The Mark 1: 18 armour, 3 toughness, knockback resistance 0.5 (heavy and crude). */
    public static final Holder<ArmorMaterial> IRON_MARK_1 = ARMOR_MATERIALS.register("iron_mark_1",
            () -> suit("iron_mark_1", 18, 9, SoundEvents.ARMOR_EQUIP_IRON, Items.IRON_INGOT, 3.0F, 0.5F));
    /** The Mark 3: 16 armour, 2 toughness, knockback resistance 0.2. */
    public static final Holder<ArmorMaterial> IRON_MARK_3 = ARMOR_MATERIALS.register("iron_mark_3",
            () -> suit("iron_mark_3", 16, 12, SoundEvents.ARMOR_EQUIP_IRON, Items.IRON_INGOT, 2.0F, 0.2F));
    /** The Mark 5 (the suitcase suit, light): 12 armour, 1 toughness, knockback resistance 0.1. */
    public static final Holder<ArmorMaterial> IRON_MARK_5 = ARMOR_MATERIALS.register("iron_mark_5",
            () -> suit("iron_mark_5", 12, 12, SoundEvents.ARMOR_EQUIP_IRON, Items.IRON_INGOT, 1.0F, 0.1F));
    /** The Hulkbuster: 20 armour, 12 toughness, full knockback resistance. */
    public static final Holder<ArmorMaterial> HULKBUSTER = ARMOR_MATERIALS.register("hulkbuster",
            () -> suit("hulkbuster", 20, 9, SoundEvents.ARMOR_EQUIP_NETHERITE, Items.IRON_BLOCK, 12.0F, 1.0F));
    /**
     * The Nano Arc Reactor and the nanotech housings: no armour of their own (a deployed nanotech suit's armour is
     * applied by {@code IronSuits} while it is out, PLAN 5.3).
     */
    public static final Holder<ArmorMaterial> NANO_REACTOR = ARMOR_MATERIALS.register("nano_reactor",
            () -> suit("nano_reactor", 0, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, Items.DIAMOND, 0.0F, 0.0F));
    // ===== falcon (3.8) =====
    /**
     * Falcon's Exo-Wings: an iron chestplate's armour and a bit (6, toughness 1). Drawn by their own layer (lane L5).
     */
    public static final Holder<ArmorMaterial> FALCON_WINGS = ARMOR_MATERIALS.register("falcon_wings",
            () -> suit("falcon_wings", 6, 12, SoundEvents.ARMOR_EQUIP_ELYTRA, Items.IRON_INGOT, 1.0F, 0.0F));

    /** 3.8: a material whose one item (a chest item) is the whole set: {@code defense} for every slot. */
    private static ArmorMaterial suit(String name, int defense, int enchantment,
            Holder<net.minecraft.sounds.SoundEvent> sound, net.minecraft.world.item.Item repair, float toughness,
            float knockback) {
        return new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                map.put(type, defense);
            }
        }), enchantment, sound, () -> Ingredient.of(repair), List.of(new ArmorMaterial.Layer(Enhanced.id(name))),
                toughness, knockback);
    }

    private ModArmorMaterials() {
    }
}
