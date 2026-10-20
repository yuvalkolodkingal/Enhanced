package com.kolodkingal.enhanced.item;

import net.minecraft.world.item.ShieldItem;

/**
 * Captain America's Vibranium Shield (3.8, PLAN 3.3): a shield anyone may block with; for Captain America it blocks
 * everything from the front, is never disabled by an axe, bounces projectiles back, and he throws it (R). Unbreakable
 * (the Unbreakable component). Registered by the 3.8 skeleton as a plain shield; lane L1 owns the class, its round
 * model ({@code IClientItemExtensions}) and its rules.
 */
public class VibraniumShieldItem extends ShieldItem {
    public VibraniumShieldItem(Properties properties) {
        super(properties);
    }
}
