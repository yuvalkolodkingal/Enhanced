package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AgathaAbilities;
import com.kolodkingal.enhanced.ability.AgathaShield;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.config.AgathaConfig;
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
 * What the client shows of Agatha besides her body: the lines of her HUD (the beam burning, flight, the drain's recharge,
 * her shield's strength and time, how long her runes still burn), the violet glow of her eyes while she works magic
 * ("It's been Agatha all along": her eyes burn purple in WandaVision), and the look of her shield.
 */
final class AgathaView {
    /** Her shield: a violet veil with a lattice of runes. */
    private static final ResourceLocation RUNE_LATTICE = Enhanced.id("textures/entity/agatha_shield.png");
    private static final int SHIELD_VEIL = 0x7A2FD6;
    private static final int SHIELD_LINES = 0xC48CFF;
    /** The mask of her glowing eyes: the eyes of the skin (and of every ordinary skin), on the face's fifth row. */
    private static final RenderType EYES = RenderType.eyes(Enhanced.id("textures/entity/agatha_eyes.png"));

    @Nullable
    private static ShieldStyle shield;

    private AgathaView() {
    }

    // ---- the HUD ----

    @Nullable
    static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        return switch (ability) {
            case 1 -> data.is(Live.BEAMING) ? Component.translatable("hud.enhanced.active").withStyle(ChatFormatting.LIGHT_PURPLE) : null;
            case 2 -> Component.translatable(data.flying() ? "hud.enhanced.on" : "hud.enhanced.off")
                    .withStyle(data.flying() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
            case 3 -> PowerHud.cooldown(data, 3, now);
            case 4 -> {
                Component line = ShieldStyles.hudLine(data, now, AgathaShield.maxStrength(), AgathaShield.maxTicks());
                yield line != null ? line : PowerHud.cooldown(data, 4, now);
            }
            case 5 -> runes(player, data, now);
            default -> null;
        };
    }

    /** "runes 3.2s" in violet while her circle burns; else the recharge. */
    @Nullable
    private static Component runes(LocalPlayer player, PowerData data, long now) {
        long left = AgathaFx.runesLeft(player.getId(), now);
        Component cooldown = PowerHud.cooldown(data, 5, now);
        if (left <= 0) {
            return cooldown;
        }
        Component burning = Component.translatable("hud.enhanced.agatha_runes", String.format(Locale.ROOT, "%.1f", left / 20.0))
                .withStyle(s -> s.withColor(AgathaAbilities.LILAC));
        return cooldown == null ? burning : burning.copy().append(Component.literal("  ")).append(cooldown);
    }

    // ---- the eyes ----

    /** Her eyes burn violet while the beam burns, the shield is up, the hex is cast, and a moment after a drain or the runes. */
    @Nullable
    static RenderType eyes(AbstractClientPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        return AgathaGestures.eyesGlow(data, now, fullCooldown(3), fullCooldown(5)) ? EYES : null;
    }

    /** The cooldowns her drain and runes start with (for the rule of the eyes: the ticks just after a cast). */
    private static long fullCooldown(int ability) {
        if (!EnhancedConfig.SPEC.isLoaded()) {
            return 0;
        }
        return ability == 3 ? AgathaConfig.DRAIN_COOLDOWN.get() : AgathaConfig.RUNE_COOLDOWN.get();
    }

    // ---- the shield ----

    /** The look of her shield: violet, the lattice of runes, the radius of the config (the style is remade when it changes). */
    static ShieldStyle shieldStyle() {
        float radius = (float) AgathaShield.radius();
        if (shield == null || shield.radius() != radius) {
            shield = new ShieldStyle(SHIELD_VEIL, SHIELD_LINES, RUNE_LATTICE, radius, AgathaShield::maxStrength, AgathaShield::maxTicks);
        }
        return shield;
    }
}
