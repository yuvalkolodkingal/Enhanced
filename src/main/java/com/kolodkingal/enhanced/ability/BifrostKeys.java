package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.world.entity.player.Player;

/**
 * Which key press opens the Bifrost screen on the client instead of going to the server (3.4): for Thor his N key without
 * Sneak while he holds Mjolnir or Stormbreaker (as before); for a custom hero the key whose binding (with or without
 * Sneak, as he holds it) is the main action of Thor's Bifrost, whose power calls it without a weapon. Sneak with the
 * Bifrost's Sneak variant goes to the server (to where he looks), as Thor's Sneak + N does. Pure and common, so the
 * client's key handler and the tests ask the same thing.
 */
public final class BifrostKeys {

    /**
     * @param column 1-5, the key (R G V B N)
     * @param sneak  whether Sneak is held
     */
    public static boolean opensScreen(Player player, int column, boolean sneak) {
        PowerData data = Powers.get(player);
        if (data.power() == Power.THOR) {
            return column == HeroAbility.THOR_BIFROST.home && !sneak && ThorAbilities.holdsWeapon(player);
        }
        HeroKit kit = data.kit();
        if (data.power() != Power.CUSTOM || kit == null) {
            return false;
        }
        HeroKit.Binding binding = kit.binding(column, sneak);
        return binding != null && binding.ability() == HeroAbility.THOR_BIFROST && !binding.isAlt()
                && ThorAbilities.callsBifrost(player);
    }

    private BifrostKeys() {
    }
}
