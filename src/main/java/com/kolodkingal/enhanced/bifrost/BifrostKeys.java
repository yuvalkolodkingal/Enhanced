package com.kolodkingal.enhanced.bifrost;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.config.AsgardConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.item.HofundItem;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * The keys of the Bifrost (3.5): who may open it with his own key ({@link Bifrost.Source#KEY}). Pure and common (no
 * server types in {@link #holdsKey}), so the Bifrost screen on the client and the server ask the same thing.
 *
 * <ul>
 * <li>whoever holds Mjolnir or Stormbreaker (as before 3.5), and a custom hero with Thor's Bifrost (3.4);</li>
 * <li>whoever holds a Hofund, anyone's ({@code asgard.hofundOpensBifrost});</li>
 * <li>whoever holds his own Gungnir (Odin, or any owner);</li>
 * <li>whoever has Heimdall's Bifrost ({@code heimdall.bifrost}: Heimdall himself, or a custom hero with it): no
 * item.</li> </ul>
 */
public final class BifrostKeys {

    /** Whether the Bifrost answers this player's own key (both sides). */
    public static boolean holdsKey(Player player) {
        if (ThorAbilities.holdsWeapon(player)) {
            return true;
        }
        PowerData data = Powers.get(player);
        if (data.power() == Power.CUSTOM && data.has(HeroAbility.THOR_BIFROST)) {
            return true;
        }
        if (data.has(HeroAbility.HEIMDALL_BIFROST)) {
            return true;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (HofundItem.isHofund(player.getItemInHand(hand)) && hofundOpens()
                    || GungnirItem.isOwnGungnir(player, player.getItemInHand(hand))) {
                return true;
            }
        }
        return false;
    }

    /** The server's check before a journey by the player's own key (the same as {@link #holdsKey}). */
    public static boolean mayOpen(ServerPlayer player) {
        return holdsKey(player);
    }

    /** The lang key of the message to one the Bifrost does not answer. */
    public static String refusalKey(Player player) {
        return "message.enhanced.bifrost.needs_weapon";
    }

    private static boolean hofundOpens() {
        return !EnhancedConfig.SPEC.isLoaded() || AsgardConfig.HOFUND_OPENS_BIFROST.get();
    }

    /**
     * Which key press opens the Bifrost screen on the client instead of going to the server (3.4): for Thor his N key
     * without Sneak while he holds Mjolnir or Stormbreaker (as before); for Heimdall his V key without Sneak (3.5); for
     * a custom hero the key whose binding (with or without Sneak, as he holds it) is the main action of Thor's or
     * Heimdall's Bifrost. Sneak with the Bifrost's Sneak variant goes to the server (to where he looks).
     *
     * @param column 1-5, the key (R G V B N)
     * @param sneak  whether Sneak is held
     */
    public static boolean opensScreen(Player player, int column, boolean sneak) {
        PowerData data = Powers.get(player);
        if (data.power() == Power.THOR) {
            return column == HeroAbility.THOR_BIFROST.home && !sneak && ThorAbilities.holdsWeapon(player);
        }
        if (data.power() == Power.HEIMDALL) {
            return column == HeroAbility.HEIMDALL_BIFROST.home && !sneak;
        }
        // (3.8: Love's N without Sneak while she holds Stormbreaker, as Thor's; Sneak + N goes to the server.)
        if (data.power() == Power.LOVE) {
            return column == HeroAbility.LOVE_BIFROST.home && !sneak
                    && com.kolodkingal.enhanced.love.LoveAbilities.holdsStormbreaker(player);
        }
        HeroKit kit = data.kit();
        if (data.power() != Power.CUSTOM || kit == null) {
            return false;
        }
        HeroKit.Binding binding = kit.binding(column, sneak);
        return binding != null && (binding.ability() == HeroAbility.THOR_BIFROST || binding.ability() == HeroAbility.HEIMDALL_BIFROST)
                && !binding.isAlt() && holdsKey(player);
    }

    private BifrostKeys() {
    }
}
