package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What the hero shows about himself on the HUD and on his face: the five lines of the list, the eyes that glow while the
 * shield is up or the storm rages, and the look of his shield. The maths behind them is in {@link ApocalypseHeroMath}.
 */
public final class ApocalypseView {
    /** The look the eyes of the ancient suit take: the mask of this variant. */
    public static final String ANCIENT_LOOK = "en_sabah_nur";
    /** The eye masks: white-gold for the film's look, a hotter gold for the ancient one. */
    public static final ResourceLocation EYES = Enhanced.id("textures/entity/apocalypse_eyes.png");
    public static final ResourceLocation EYES_ANCIENT = Enhanced.id("textures/entity/apocalypse_eyes_ancient.png");
    /** The golden lattice of his shield (and of the glimmer on his body): {@link ShieldFx#WEB}, said again here because the game tests cannot load that class. */
    public static final ResourceLocation WEB = Enhanced.id("textures/entity/golden_web.png");

    /** The gold of his shield (veil, then the lines of the lattice). */
    public static final int SHIELD_VEIL = 0xF5C84A;
    public static final int SHIELD_LINES = 0xFFF0B8;

    private static ShieldStyle shield;

    // ---- the config, with the defaults for a client that has not loaded it ----

    static int stormTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.STORM_TICKS.get() : 120;
    }

    static int stormCooldown() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.STORM_COOLDOWN.get() : 600;
    }

    static int waveCooldown() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.WAVE_COOLDOWN.get() : 60;
    }

    static int entombCooldown() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.ENTOMB_COOLDOWN.get() : 240;
    }

    // ---- the HUD ----

    /** The status after the name of an ability: the cooldown unless the ability means something else. */
    @Nullable
    public static Component status(PowerData data, int ability, long now) {
        return switch (ability) {
            case 2 -> onOff(data.flying());
            case 4 -> {
                Component line = ShieldStyles.hudLine(data, now, ApocalypseGuard.maxStrength(), ApocalypseGuard.maxTicks());
                yield line != null ? line : cooldown(data, ability, now);
            }
            case 5 -> {
                if (data.is(Live.SANDSTORM)) {
                    long left = ApocalypseHeroMath.stormLeft(data.cooldownUntil(5) - now, stormTicks(), stormCooldown());
                    yield Component.translatable("hud.enhanced.apocalypse_storm", String.format(Locale.ROOT, "%.1fs", left / 20.0))
                            .withStyle(ChatFormatting.GOLD);
                }
                yield cooldown(data, ability, now);
            }
            default -> cooldown(data, ability, now);
        };
    }

    /** The ability's cooldown as the HUD shows it (grey seconds), or null when it is ready. */
    @Nullable
    public static Component cooldown(PowerData data, int ability, long now) {
        long left = data.cooldownUntil(ability) - now;
        return left > 0 ? Component.literal(String.format(Locale.ROOT, "%.1fs", left / 20.0)).withStyle(ChatFormatting.GRAY) : null;
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    // ---- the eyes and the shield ----

    /** The mask of his eyes right now, or null when they do not glow. */
    @Nullable
    public static RenderType eyes(AbstractClientPlayer player, PowerData data) {
        if (!ApocalypseHeroMath.eyesGlow(data.chaosShieldActive(), data.is(Live.SANDSTORM))) {
            return null;
        }
        return RenderType.eyes(ANCIENT_LOOK.equals(Variants.current(data).id()) ? EYES_ANCIENT : EYES);
    }

    /** The look of his shield: gold, the golden web, the radius of the config (the style is remade when it changes). */
    public static ShieldStyle shieldStyle() {
        float radius = (float) ApocalypseGuard.radius();
        if (shield == null || shield.radius() != radius) {
            shield = new ShieldStyle(SHIELD_VEIL, SHIELD_LINES, WEB, radius, ApocalypseGuard::maxStrength, ApocalypseGuard::maxTicks);
        }
        return shield;
    }

    private ApocalypseView() {
    }
}
