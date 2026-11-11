package com.kolodkingal.enhanced.compat;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/** 1.21.5: there is no SwordItem; a sword is an item whose properties are a sword's of a material. */
public class SwordItem extends Item {
    public SwordItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties) {
        super(properties.sword(material, attackDamage, attackSpeed));
    }
}
