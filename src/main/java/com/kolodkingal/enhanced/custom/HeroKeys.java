package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * From a custom hero's hero abilities to the heroes' own code (3.4): a binding of {@link HeroAbility} {@code a} calls
 * hero {@code a.hero}'s key entry with the key {@code a} has for that hero ({@link HeroAbility#home}) and whether the
 * binding is its Sneak variant; the hero's tick and its end run once per hero the custom hero has abilities of
 * ({@link HeroKit#sources}). Nothing of a hero is copied: the old heroes go through the shared switches
 * ({@link AbilityHandler#heroKey}, {@link PowerTicker#heroTick}, {@link PowerTicker#heroEnd}), the plug-ins through their
 * {@code HeroServer}. The hero's lifecycle (login, logout, death, respawn, a new dimension) already reaches every hero for
 * every player ({@code HeroLifecycle}, {@code CommonEvents}).
 */
public final class HeroKeys {

    /**
     * A binding of a hero ability went down or up.
     *
     * @param alt whether the binding is the ability's Sneak variant
     */
    public static void press(ServerPlayer player, PowerData data, HeroAbility a, boolean down, boolean alt) {
        if (!a.takesBox()) {
            return; // (a body has no key; Build Magic's Y key is its own)
        }
        AbilityHandler.heroKey(a.hero, player, data, a.home, down, alt);
    }

    /** Every tick of a custom hero: the upkeep of every hero he has abilities of (each guards its parts by {@code has}). */
    public static void tick(ServerPlayer player, PowerData data, HeroKit kit) {
        for (Power hero : kit.sources()) {
            PowerTicker.heroTick(hero, player, data);
        }
    }

    /** His powers end: everything every one of his heroes' code left in the world ends. */
    public static void endAll(ServerPlayer player, PowerData data, HeroKit kit) {
        for (Power hero : kit.sources()) {
            PowerTicker.heroEnd(hero, player, data);
        }
    }

    private HeroKeys() {
    }
}
