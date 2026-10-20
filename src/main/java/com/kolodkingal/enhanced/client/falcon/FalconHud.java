package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.falcon.FalconState;
import com.kolodkingal.enhanced.falcon.FalconWings;
import com.kolodkingal.enhanced.falcon.RedwingEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Falcon's HUD lines (3.8, PLAN 8.6, after each key's name): G the wings (worn and how worn, broken, not on; gliding);
 * B the Wing Shield's time while it is up; N Redwing's time and health while it is out, or its repairs; the cooldowns
 * as every hero's (R's is the Wrist Rocket's).
 */
public final class FalconHud {
    /** The status after key {@code ability}'s line on his HUD. */
    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        Component cooldown = PowerHud.cooldown(data, ability, now);
        return switch (ability) {
            case 2 -> wings(player, data);
            case 4 -> {
                if (data.is(Live.FALCON_WING_SHIELD)) {
                    long up = FalconEffects.shieldUpAt(player);
                    int max = EnhancedConfig.SPEC.isLoaded() ? FalconConfig.SHIELD_MAX_TICKS.get() : 100;
                    float left = up < 0 ? max / 20.0F : Math.max(0.0F, (max - (now - up)) / 20.0F);
                    yield Component.translatable("hud.enhanced.falcon.shield",
                            String.format(Locale.ROOT, "%.1f", left)).withStyle(ChatFormatting.AQUA);
                }
                yield cooldown;
            }
            case 5 -> {
                FalconState state = player.getData(ModAttachments.FALCON);
                if (data.toggle(Toggle.REDWING_OUT) && state.redwingUntil() > now) {
                    RedwingEntity drone = FalconEffects.drone(player);
                    String health = drone == null ? "?" : String.valueOf(Math.round(drone.getHealth()));
                    String line = clock(state.redwingUntil() - now) + " · " + health + "♥";
                    MutableComponent out = Component.translatable("hud.enhanced.falcon.redwing", line)
                            .withStyle(ChatFormatting.RED);
                    yield cooldown == null ? out : out.append(" ").append(cooldown);
                }
                if (state.redwingDownUntil() > now) {
                    yield Component.translatable("hud.enhanced.falcon.repairs", clock(state.redwingDownUntil() - now))
                            .withStyle(ChatFormatting.GOLD);
                }
                yield cooldown;
            }
            default -> cooldown;
        };
    }

    /** G's line: the wings worn (how much of them is left), broken, or not on; and the glide. */
    static Component wings(LocalPlayer player, PowerData data) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        MutableComponent state;
        if (!FalconWings.worn(player)) {
            state = Component.translatable("hud.enhanced.falcon.wings_none").withStyle(ChatFormatting.DARK_GRAY);
        } else if (!FalconWings.usable(chest)) {
            state = Component.translatable("hud.enhanced.falcon.wings_broken").withStyle(ChatFormatting.RED);
        } else {
            int max = Math.max(1, chest.getMaxDamage());
            int percent = Math.round(100.0F * (max - chest.getDamageValue()) / max);
            state = Component.literal(percent + "%").withStyle(percent > 25 ? ChatFormatting.WHITE
                    : ChatFormatting.GOLD);
        }
        MutableComponent line = Component.translatable("hud.enhanced.falcon.wings", state)
                .withStyle(ChatFormatting.GRAY);
        if (data.is(Live.FALCON_GLIDING)) {
            line.append(" ").append(Component.translatable("hud.enhanced.falcon.gliding")
                    .withStyle(ChatFormatting.AQUA));
        }
        return line;
    }

    /** Ticks as "m:ss". */
    static String clock(long ticks) {
        long seconds = Math.max(0L, (ticks + 19) / 20);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private FalconHud() {
    }
}
