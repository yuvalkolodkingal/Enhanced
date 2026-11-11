package com.kolodkingal.enhanced.compat;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

/** 1.21.5: there is no ArmorItem; armour is an item whose properties are humanoid armour of a material and a type. */
public class ArmorItem extends Item {
    private final ArmorType type;

    public ArmorItem(ArmorMaterial material, ArmorType type, Properties properties) {
        super(properties.humanoidArmor(material, type));
        this.type = type;
    }

    public ArmorType getType() {
        return type;
    }
}
