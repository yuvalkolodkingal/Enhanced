package com.kolodkingal.enhanced.client.captain;

import com.kolodkingal.enhanced.captain.CaptainState;
import com.kolodkingal.enhanced.captain.ShieldRules;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.client.healing.HealingHud;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;

/**
 * Captain America's HUD (3.8, PLAN 3.6): his lines on the powers' panel ({@link #status}, for {@code
 * CaptainClient.hudStatus}): R the shield (in hand, flying with its ricochets left, on his back, or none) and the
 * throw's cooldown; G the slam in the air; V the charge; B Assemble's seconds while the rally lasts; N the combo's
 * step. And under the panel (this layer) the super-soldier's line with a little shield: "Super-soldier: Speed II ·
 * Strength II · Resistance I".
 */
public final class CaptainHud implements LayeredDraw.Layer {
    /** How long after a blow of the combo its step stays on the HUD (the combo's reset). */
    static final int COMBO_SHOWN = 30;
    /**
     * The little shield of the line, 7 x 7: red ({@code r}), silver ({@code s}), blue ({@code b}), the star ({@code
     * w}).
     */
    private static final String[] ICON = {
            "..rrr..",
            ".rsssr.",
            "rsbbbsr",
            "rsbwbsr",
            "rsbbbsr",
            ".rsssr.",
            "..rrr.."
    };

    /** The status after an ability's line on his HUD. */
    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        CaptainState cs = ModAttachments.CAPTAIN.get(player);
        switch (ability) {
            case 1 -> {
                Component shield = shieldLine(player, cs);
                Component cd = PowerHud.cooldown(data, ability, now);
                return cd == null ? shield : shield.copy().append(" ").append(cd);
            }
            case 2 -> {
                if (data.is(Live.CAP_SLAMMING)) {
                    return Component.translatable("hud.enhanced.captain.slam").withStyle(ChatFormatting.GOLD);
                }
            }
            case 3 -> {
                if (data.is(Live.CAP_CHARGING)) {
                    return Component.translatable("hud.enhanced.captain.charging").withStyle(ChatFormatting.GOLD);
                }
            }
            case 4 -> {
                MobEffectInstance rally = player.getEffect(MobEffects.DAMAGE_BOOST);
                Component cd = PowerHud.cooldown(data, ability, now);
                if (rally != null && cd != null) {
                    return Component.translatable("hud.enhanced.captain.rally", (rally.getDuration() + 19) / 20)
                            .withStyle(ChatFormatting.AQUA).append(" ").append(cd);
                }
            }
            case 5 -> {
                if (now - cs.comboAt() <= COMBO_SHOWN && now >= cs.comboAt() && cs.comboAt() > 0) {
                    int struck = (cs.comboStep() + 2) % 3 + 1;
                    return Component.translatable("hud.enhanced.captain.combo", struck)
                            .withStyle(ChatFormatting.GOLD);
                }
            }
            default -> {
            }
        }
        return PowerHud.cooldown(data, ability, now);
    }

    /** Where his shield is. */
    static Component shieldLine(LocalPlayer player, CaptainState cs) {
        if (cs.shieldEntity() >= 0) {
            MutableComponent line = Component.translatable("hud.enhanced.captain.shield_flying");
            if (cs.has(CaptainState.RICOCHET)) {
                line = Component.translatable("hud.enhanced.captain.shield_ricochets", cs.bouncesLeft());
            }
            return line.withStyle(ChatFormatting.AQUA);
        }
        if (ShieldRules.shieldHand(player) != null) {
            return Component.translatable("hud.enhanced.captain.shield_hand").withStyle(ChatFormatting.WHITE);
        }
        if (cs.has(CaptainState.BACK)) {
            return Component.translatable("hud.enhanced.captain.shield_back").withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("hud.enhanced.captain.shield_none").withStyle(ChatFormatting.DARK_GRAY);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || player.isDeadOrDying()
                || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        PowerData data = Powers.get(player);
        if (!data.has(HeroAbility.CAPTAIN_BODY)) {
            return;
        }
        Component line = Component.translatable("hud.enhanced.captain.body");
        Font font = mc.font;
        int x = 4;
        int y = HealingHud.bottom(data) - (HealingFactor.has(player) ? 0 : HealingHud.HEIGHT + 2);
        int width = 11 + font.width(line) + 2;
        g.fill(x - 2, y - 2, x + width + 2, y + HealingHud.HEIGHT - 2, 0x70000000);
        icon(g, x, y + 1);
        g.drawString(font, line, x + 10, y + 1, 0xFFE8EEF5, true);
    }

    static void icon(GuiGraphics g, int x, int y) {
        for (int r = 0; r < ICON.length; r++) {
            for (int c = 0; c < ICON[r].length(); c++) {
                int color = switch (ICON[r].charAt(c)) {
                    case 'r' -> 0xFFC8323A;
                    case 's' -> 0xFFE8EEF5;
                    case 'b' -> 0xFF2A4BA0;
                    case 'w' -> 0xFFFFFFFF;
                    default -> 0;
                };
                if (color != 0) {
                    g.fill(x + c, y + r, x + c + 1, y + r + 1, color);
                }
            }
        }
    }
}
