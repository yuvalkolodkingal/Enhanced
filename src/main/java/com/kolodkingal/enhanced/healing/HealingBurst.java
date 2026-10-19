package com.kolodkingal.enhanced.healing;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.network.HealingFxPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * The Healing Burst (3.7, Sneak + B for both healers: the rows {@code wolverine.healing_burst} and {@code
 * deadpool.chimichanga}): a share of his max health at once, the game's harmful effects cleared (not Poison, not the
 * mod's spells: those the shrug leaves too, {@link Suppression#shrugs}), a soft green-white shimmer everyone sees and
 * hears; refused while poisoned ("The poison stops your healing!"), and then no cooldown should be spent. Lanes L2 and
 * L3 call it from their keys with {@link #fraction()} (Deadpool's chimichanga passes more) and spend {@link
 * #cooldown()} on a true answer.
 */
public final class HealingBurst {

    /** The share of his max health a plain Burst heals ({@code burstFraction}). */
    public static double fraction() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.BURST_FRACTION.get() : 0.5;
    }

    /** The Burst's cooldown in ticks ({@code burstCooldown}), for the keys that call it. */
    public static int cooldown() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.BURST_COOLDOWN.get() : 400;
    }

    /**
     * Heals {@code player} by {@code fraction} of his max health now (Deadpool's chimichanga passes more). Returns
     * whether it healed: false when refused (poisoned, DOWN, dead), and then no cooldown should be spent. A player
     * without the healing factor (a custom hero with the row alone) is healed plainly (never refused for poison).
     */
    public static boolean burst(ServerPlayer player, double fraction) {
        if (!player.isAlive() || HealingFactor.isDown(player)) {
            return false;
        }
        if (HealingFactor.has(player) && HealingFactor.suppressed(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.healing.poisoned")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        player.heal((float) (player.getMaxHealth() * Math.max(0.0, fraction)));
        List<Holder<MobEffect>> cleared = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (Suppression.shrugs(effect.getEffect())) {
                cleared.add(effect.getEffect());
            }
        }
        for (Holder<MobEffect> effect : cleared) {
            player.removeEffect(effect);
        }
        Downed.fx(player, HealingFxPayload.BURST, (int) Math.round(fraction * 100));
        Downed.sound(player, ModSounds.HEALING_BURST.get(), 1.0F);
        return true;
    }

    private HealingBurst() {
    }
}
