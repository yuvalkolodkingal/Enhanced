package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.21.4: armour materials are plain values (no registry), their look an equipment asset
 * ({@code assets/enhanced/equipment}) and their repair item a tag ({@code data/enhanced/tags/item/repairs_*}). The same
 * numbers as the 1.21.1 materials; held in {@link Holder#direct} so the items take them as before.
 */
public final class ModArmorMaterials {
    public static final Holder<ArmorMaterial> MAGNETO = material("magneto_helmet", 20, 15,
            SoundEvents.ARMOR_EQUIP_NETHERITE, "magneto", 12.0F, 0.4F);
    public static final Holder<ArmorMaterial> WANDA_CROWN = material("wanda_crown", 0, 25,
            SoundEvents.ARMOR_EQUIP_GOLD, "wanda_crown", 0.0F, 0.0F);
    public static final Holder<ArmorMaterial> QUICKSILVER = material("quicksilver_goggles", 2, 15,
            SoundEvents.ARMOR_EQUIP_IRON, "quicksilver", 0.0F, 0.0F);
    public static final Holder<ArmorMaterial> RUBY_QUARTZ = material("ruby_quartz_glasses", 1, 15,
            SoundEvents.ARMOR_EQUIP_GENERIC, "ruby_quartz", 0.0F, 0.0F);
    public static final Holder<ArmorMaterial> IRON_MARK_1 = material("iron_mark_1", 18, 9,
            SoundEvents.ARMOR_EQUIP_IRON, "iron_suit", 3.0F, 0.5F);
    public static final Holder<ArmorMaterial> IRON_MARK_3 = material("iron_mark_3", 16, 12,
            SoundEvents.ARMOR_EQUIP_IRON, "iron_suit", 2.0F, 0.2F);
    public static final Holder<ArmorMaterial> IRON_MARK_5 = material("iron_mark_5", 12, 12,
            SoundEvents.ARMOR_EQUIP_IRON, "iron_suit", 1.0F, 0.1F);
    public static final Holder<ArmorMaterial> HULKBUSTER = material("hulkbuster", 20, 9,
            SoundEvents.ARMOR_EQUIP_NETHERITE, "hulkbuster", 12.0F, 1.0F);
    public static final Holder<ArmorMaterial> NANO_REACTOR = material("nano_reactor", 0, 15,
            SoundEvents.ARMOR_EQUIP_NETHERITE, "nano_reactor", 0.0F, 0.0F);
    public static final Holder<ArmorMaterial> FALCON_WINGS = material("falcon_wings", 6, 12,
            SoundEvents.ARMOR_EQUIP_ELYTRA, "iron_suit", 1.0F, 0.0F);

    private static Holder<ArmorMaterial> material(String asset, int defense, int enchantment, Holder<SoundEvent> sound,
                                                  String repair, float toughness, float knockback) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        for (ArmorType type : ArmorType.values()) {
            map.put(type, defense);
        }
        TagKey<Item> repairs = TagKey.create(Registries.ITEM, Enhanced.id("repairs_" + repair));
        ResourceKey<EquipmentAsset> look = ResourceKey.create(EquipmentAssets.ROOT_ID, Enhanced.id(asset));
        // durability: each item sets its own (see mixin.ItemPropertiesMixin); 0 is what 1.21.1's materials had
        return Holder.direct(new ArmorMaterial(0, map, enchantment, sound, toughness, knockback, repairs, look));
    }

    private ModArmorMaterials() {
    }
}
