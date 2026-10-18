package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.CyclopsAbilities;
import com.kolodkingal.enhanced.ability.CyclopsGlasses;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What Cyclops shows about himself on the HUD and on his face (3.3): the five lines of the list (the blasts of his eyes say
 * when they need the glasses; N says whether the glasses are on, and in red when his eyes are open) and his eyes, which
 * blaze red while the giant beam pours out (a mask at the eyes of both his skins, and of an ordinary skin).
 */
public final class CyclopsView {
    /** The mask of his blazing eyes. */
    public static final ResourceLocation EYES = Enhanced.id("textures/entity/cyclops_eyes.png");

    /** The status after the name of an ability: the cooldown unless the ability means something else. */
    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        boolean open = data.is(Live.CYCLOPS_UNLEASHED);
        boolean wearing = CyclopsGlasses.wears(player);
        return switch (ability) {
            case 1 -> {
                if (data.is(Live.BEAMING) && !open) {
                    yield Component.translatable("hud.enhanced.cyclops_firing").withStyle(ChatFormatting.RED);
                }
                yield wearing && !open ? cooldown(data, ability, now) : needsGlasses();
            }
            case 2, 3 -> wearing && !open ? cooldown(data, ability, now) : needsGlasses();
            case CyclopsAbilities.GLASSES_SLOT -> open
                    ? Component.translatable("hud.enhanced.cyclops_eyes_open").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    : Component.translatable(wearing ? "hud.enhanced.cyclops_glasses_on" : "hud.enhanced.cyclops_glasses_off")
                    .withStyle(wearing ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
            default -> cooldown(data, ability, now);
        };
    }

    private static Component needsGlasses() {
        return Component.translatable("hud.enhanced.cyclops_needs_glasses").withStyle(ChatFormatting.DARK_GRAY);
    }

    /** The ability's cooldown as the HUD shows it (grey seconds), or null when it is ready. */
    @Nullable
    private static Component cooldown(PowerData data, int ability, long now) {
        long left = data.cooldownUntil(ability) - now;
        return left > 0 ? Component.literal(String.format(Locale.ROOT, "%.1fs", left / 20.0)).withStyle(ChatFormatting.GRAY) : null;
    }

    /** The mask of his eyes right now, or null when they do not blaze (only while the giant beam pours out). */
    @Nullable
    public static RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return data.is(Live.CYCLOPS_UNLEASHED) ? RenderType.eyes(EYES) : null;
    }

    private CyclopsView() {
    }
}
