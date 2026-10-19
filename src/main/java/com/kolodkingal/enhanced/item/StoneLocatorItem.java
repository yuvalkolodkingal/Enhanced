package com.kolodkingal.enhanced.item;

import net.minecraft.world.item.Item;

/**
 * The Stone Locator (3.7): a gold-and-black disc with a glass lens over a compass needle, tuned to the Infinity Stone
 * of its recipe; its needle points to the nearest site of a stone its holder has not got ({@code enhanced:locator},
 * {@code finders/LocatorState}). Registered by the 3.7 skeleton as a plain item; lane L5 owns the class (the search,
 * the needle, the HUD, the spark trail, the filter).
 */
public class StoneLocatorItem extends Item {
    public StoneLocatorItem(Properties properties) {
        super(properties);
    }
}
