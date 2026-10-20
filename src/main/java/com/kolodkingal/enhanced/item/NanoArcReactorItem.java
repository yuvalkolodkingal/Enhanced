package com.kolodkingal.enhanced.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * The Nano Arc Reactor (3.8, PLAN 5.2): the Infinity War housing, a chest-slot item (no armour of its own: the deployed
 * nanotech suit's armour is its own). Held or put on the chest, it makes the player Iron Man; on the chest it is the
 * nanotech suits' home. Registered by the 3.8 skeleton as a plain chest item; lane L3 owns the class (L4 the deploy).
 */
public class NanoArcReactorItem extends ArmorItem {
    public NanoArcReactorItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }
}
