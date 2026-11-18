package com.kolodkingal.enhanced.client.jim;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Jim Hammond's HUD lines (3.17, for {@code JimClient.hudStatus}): the Fire Beam's charge is the shared one ({@code PowerHud}
 * asks {@code ChargedBeamFx} for R), the Nova Flame's gathering shows its percent on B, and then the cooldowns; G and N say ON or OFF.
 */
public final class JimHud {
    /** When each player's Nova began gathering, as the HUD saw it. */
    private static final Map<Integer, Long> NOVA_SINCE = new HashMap<>();

    private JimHud() {
    }

    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        if (ability == 4 && data.is(Live.JIM_NOVA_CHARGING)) {
            long since = NOVA_SINCE.computeIfAbsent(player.getId(), k -> now);
            int full = EnhancedConfig.SPEC.isLoaded() ? JimConfig.NOVA_CHARGE_TICKS.get() : 60;
            int percent = Math.min(100, Math.round(100.0F * (now - since) / Math.max(1, full)));
            return Component.translatable("hud.enhanced.charged", percent).withStyle(percent >= 100 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
        }
        NOVA_SINCE.remove(player.getId());
        Component cooldown = PowerHud.cooldown(data, ability, now);
        if (cooldown != null) {
            return cooldown;
        }
        return switch (ability) {
            case 2 -> onOff(data.flying());
            case 5 -> onOff(data.disguise());
            default -> null;
        };
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }
}
