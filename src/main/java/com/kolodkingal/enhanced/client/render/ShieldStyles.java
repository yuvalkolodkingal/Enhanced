package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Whose shield the client draws, and how: the Scarlet Witch's Chaos Shield, and the shield of any hero whose {@link
 * HeroClient#shieldStyle} says one. Used by the world renderer, which draws the dome of every player whose shield is
 * up.
 */
public final class ShieldStyles {
    /** The Chaos Shield: a translucent crimson dome with a hot pink rim. */
    public static final ShieldStyle CHAOS_SHIELD = new ShieldStyle(0xC72D4B, Power.SCARLET_WITCH.color,
            Enhanced.id("textures/entity/hex_wall.png"), (float) WandaAbilities.SHIELD_RADIUS, WandaAbilities::maxShieldStrength,
            WandaAbilities::maxShieldTicks);

    /**
     * The style of the shield of this player's data: his power's ({@link #of(Power)}), or (3.4) for a custom hero the style
     * of the hero whose shield ability he has (the Chaos Shield, Agatha's, Apocalypse's; one at most, {@code Resource.SHIELD});
     * null for Billy's Hex Barrier, which {@code BarrierRenderer} draws.
     */
    @Nullable
    public static ShieldStyle of(PowerData data) {
        if (data.power() != Power.CUSTOM) {
            return of(data.power());
        }
        for (HeroAbility row : SHIELDS) {
            if (data.has(row)) {
                return of(row.hero);
            }
        }
        return null;
    }

    /**
     * 3.5: the style of this player's shield in the colour his pick of the shield ability chose ({@link
     * AbilityColors}), or {@link #of(PowerData)} as it is (a built-in hero, or no colour chosen).
     */
    @Nullable
    public static ShieldStyle of(Player player, PowerData data) {
        ShieldStyle style = of(data);
        if (style == null || data.power() != Power.CUSTOM) {
            return style;
        }
        for (HeroAbility row : SHIELDS) {
            if (data.has(row)) {
                int chosen = AbilityColors.chosen(player, row);
                return chosen < 0 ? style : style.tinted(chosen);
            }
        }
        return style;
    }

    /** The heroes' shield abilities that draw a dome (3.4: what a custom hero's shield looks like). */
    private static final HeroAbility[] SHIELDS = {HeroAbility.WANDA_CHAOS_SHIELD, HeroAbility.AGATHA_SHIELD, HeroAbility.APOCALYPSE_SHIELD};

    /** The style of a hero's own shield, or null. */
    public static ShieldStyle of(Power power) {
        if (power == Power.SCARLET_WITCH) {
            return CHAOS_SHIELD;
        }
        HeroClient hero = HeroClients.of(power);
        return hero == null ? null : hero.shieldStyle();
    }

    /**
     * The HUD line of a hero's shield: "forming" while it grows, then what is left of its strength (green, then yellow,
     * then red as it wears down) and the seconds it can still be held; null while there is no shield (the cooldown then
     * shows instead). Kept here, with no client class in it, so that it can be tested like the rest of the shield.
     *
     * @param max      the strength a fresh shield of this hero has
     * @param maxTicks how long a complete shield of this hero can be held
     */
    @Nullable
    public static Component hudLine(PowerData data, long now, float max, int maxTicks) {
        if (!data.chaosShieldActive()) {
            return null;
        }
        if (!data.chaosShieldUp(now)) {
            return Component.translatable("hud.enhanced.shield_forming").withStyle(ChatFormatting.GOLD);
        }
        if (Float.isInfinite(max)) {
            return Component.translatable("hud.enhanced.shield_unlimited").withStyle(ChatFormatting.GREEN);
        }
        float left = data.shieldStrength();
        ChatFormatting color = left > max * 0.5F ? ChatFormatting.GREEN : left > max * 0.25F ? ChatFormatting.YELLOW : ChatFormatting.RED;
        double seconds = Math.max(0, data.shieldFormedAt() + maxTicks - now) / 20.0;
        return Component.translatable("hud.enhanced.shield_strength", (int) Math.ceil(left), (int) Math.ceil(max)).withStyle(color)
                .append(Component.literal(String.format(Locale.ROOT, "  %.1fs", seconds)).withStyle(ChatFormatting.GRAY));
    }

    private ShieldStyles() {
    }
}
