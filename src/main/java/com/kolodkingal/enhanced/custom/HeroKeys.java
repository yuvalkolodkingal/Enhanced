package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

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
     * 3.6: the number of the Y key, after the five ability keys ({@code AbilityKeyPayload} carries it). Build Magic's Y
     * is a key mapping of its own ({@code BuildKeys.MAGIC}); every other Y row (Doctor Strange's Eye) comes here as key
     * 6.
     */
    public static final int Y = 6;

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

    /**
     * 3.6, the Y-row generalisation: the Y key went down or up for a player whose Y row is not Build Magic (Doctor
     * Strange's Eye, or a custom hero's {@code strange.eye}): it goes to that row's hero's code as key {@link #Y}, with
     * the Sneak variant when Sneak was held or the row picked is the variant itself. A custom hero in his human form
     * ({@link Toggle#HUMAN_FORM}) is refused, as for his other keys.
     */
    public static void pressY(ServerPlayer player, PowerData data, boolean down, boolean alt) {
        HeroAbility row = yRow(data);
        if (row == null) {
            return;
        }
        if (down && data.toggle(Toggle.HUMAN_FORM)) {
            player.displayClientMessage(Component.translatable("message.enhanced.hulk.human_form")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        AbilityHandler.heroKey(row.hero, player, data, Y, down, alt || row.isVariant());
    }

    /**
     * 3.6: the player's Y row that is not Build Magic (whose Y is its own key mapping): a built-in hero's own ({@link
     * HeroAbility#STRANGE_EYE}), a custom hero's picked one (the row, or its variant row when only that is picked), or
     * null.
     */
    @Nullable
    public static HeroAbility yRow(PowerData data) {
        if (data.power() == Power.CUSTOM) {
            HeroKit kit = data.kit();
            if (kit == null) {
                return null;
            }
            HeroAbility found = null;
            for (HeroAbility a : kit.rows()) {
                if (a.home == Y && !a.resources(HeroAbility.BOTH).contains(Resource.BUILD) && (found == null
                        || found.isVariant())) {
                    found = a;
                }
            }
            return found;
        }
        HeroAbility own = HeroAbility.at(data.power(), Y);
        return own == null || own.resources(HeroAbility.BOTH).contains(Resource.BUILD) ? null : own;
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
