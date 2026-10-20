package com.kolodkingal.enhanced.client.love;

import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.love.LoveAbilities;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Love's HUD lines (3.8, for {@code LoveClient.hudStatus}): where her axe is (in hand, flying, away), flight on or off
 * (or "needs the axe"), her lightning, the Eye Laser's seconds left while it burns (then its rest), and the Bifrost's
 * need of the axe.
 */
public final class LoveHud {
    /** When the local player's Eye Laser started (game time), seen from her client's copy of the state. */
    private static long eyesFrom = Long.MIN_VALUE;

    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        boolean axe = LoveAbilities.holdsStormbreaker(player);
        boolean flying = data.hammerEntityId() != -1;
        return switch (ability) {
            case 1 -> Component.translatable("hud.enhanced.love.axe", Component.translatable(axe
                    ? "hud.enhanced.love.axe.hand" : flying ? "hud.enhanced.love.axe.flying"
                            : "hud.enhanced.love.axe.away"))
                    .withStyle(axe || flying ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY);
            case 2 -> !player.mayFly() ? Component.translatable("hud.enhanced.love.needs_axe")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    : Component.translatable(data.flying() ? "hud.enhanced.on" : "hud.enhanced.off")
                    .withStyle(data.flying() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
            case 3 -> data.is(Live.CALLING_LIGHTNING) ? Component.translatable("hud.enhanced.active")
                    .withStyle(ChatFormatting.LIGHT_PURPLE) : null;
            case 4 -> eyes(data, now);
            case 5 -> !axe ? Component.translatable("hud.enhanced.love.needs_axe").withStyle(ChatFormatting.DARK_GRAY)
                    : null;
            default -> null;
        };
    }

    /** The laser's seconds left while it burns (violet), else its cooldown. */
    @Nullable
    private static Component eyes(PowerData data, long now) {
        if (!data.is(Live.BEAMING)) {
            eyesFrom = Long.MIN_VALUE;
            return PowerHud.cooldown(data, 4, now);
        }
        if (eyesFrom == Long.MIN_VALUE || eyesFrom > now) {
            eyesFrom = now;
        }
        int max = EnhancedConfig.SPEC.isLoaded() ? EternityConfig.LOVE_EYE_LASER_MAX_TICKS.get() : 100;
        long left = Math.max(0, max - (now - eyesFrom));
        return Component.translatable("hud.enhanced.love.eyes", String.format(Locale.ROOT, "%.1f", left / 20.0))
                .withStyle(s -> s.withColor(0xC08CFF));
    }

    private LoveHud() {
    }
}
