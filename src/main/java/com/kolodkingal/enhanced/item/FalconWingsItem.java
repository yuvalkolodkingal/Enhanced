package com.kolodkingal.enhanced.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * Falcon's Exo-Wings (3.8, PLAN 8.2): a chest-slot item (an iron chestplate's armour and a bit), an elytra for Falcon,
 * folded wings for anyone else. Its armour texture is empty: the wings are drawn by their own layer. Registered by the
 * 3.8 skeleton as a plain chestplate; lane L5 owns the class (the elytra hooks, the layer).
 */
public class FalconWingsItem extends ArmorItem {
    public FalconWingsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }
}
