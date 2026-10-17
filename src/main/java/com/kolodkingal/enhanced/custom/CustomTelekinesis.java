package com.kolodkingal.enhanced.custom;

import net.minecraft.world.entity.player.Player;

/**
 * The numbers a custom hero's Telekinesis gives the shared {@code Telekinesis} (3.3): how far it reaches and how hard it
 * throws, at the power's strength. The grip, the carry, the throw and every rule of what may be held are the shared
 * code's, in the modes {@code CUSTOM} and {@code CUSTOM_BLOCKS}.
 */
public final class CustomTelekinesis {
    /** The reach of his telekinesis, in blocks. */
    public static double range(Player player) {
        CustomHero.Pick pick = CustomHeroes.hero(player).pick(PowerModule.TELEKINESIS);
        return ModuleStats.TK_RANGE.value(pick == null ? 1 : pick.strength());
    }

    /** What the shared throw speed is multiplied by. */
    public static double throwFactor(Player player) {
        CustomHero.Pick pick = CustomHeroes.hero(player).pick(PowerModule.TELEKINESIS);
        return ModuleStats.TK_THROW.value(pick == null ? 1 : pick.strength());
    }

    private CustomTelekinesis() {
    }
}
