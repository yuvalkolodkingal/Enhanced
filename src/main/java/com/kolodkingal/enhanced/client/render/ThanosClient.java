package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.client.ThanosAtmosphere;
import com.kolodkingal.enhanced.client.ThanosBossBar;
import com.kolodkingal.enhanced.client.ThanosMusic;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The client side of Thanos, as a {@link HeroClient} plug-in (DESIGN section 15): the HUD lines of the five stones, the
 * Gauntlet's light and the life of everything the Thanos lane draws (the effects, the music, the air round the Mad
 * Titan, his bar), which this plug-in starts and stops with the client. The Gauntlet itself is {@link GauntletLayer};
 * the stones' flourishes and gestures are the animation's.
 */
public final class ThanosClient implements HeroClient {
    @Override
    public Power power() {
        return Power.THANOS;
    }

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        Component cooldown = PowerHud.cooldown(data, ability, now);
        return switch (ability) {
            // Reality: the Veil's switch, when the Transmute is ready.
            case 3 -> cooldown != null ? cooldown : onOff(data.toggle(Toggle.REALITY_VEIL));
            // Soul: the drain while it holds, the Sight while it looks, else the cooldown.
            case 4 -> data.is(Live.SOUL_DRAINING) ? Component.translatable("hud.enhanced.thanos.draining").withStyle(ChatFormatting.AQUA)
                    : data.is(Live.SOUL_SIGHT) ? Component.translatable("hud.enhanced.thanos.sight", seconds(sightLeft(data, now))).withColor(StoneKind.SOUL.color())
                    : cooldown;
            // Time: the seconds of history he could go back into, when the Rewind is ready.
            case 5 -> cooldown != null ? cooldown : Component.translatable("hud.enhanced.thanos.history", seconds(ThanosHistory.seconds()))
                    .withStyle(ThanosHistory.enough() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
            default -> cooldown;
        };
    }

    /** The seconds the Sight has left, from the cooldown that began with it. */
    private static double sightLeft(PowerData data, long now) {
        long cooldownLeft = data.cooldownUntil(4) - now;
        long total = ThanosConfig.SIGHT_COOLDOWN.get();
        long spent = total - cooldownLeft;
        return Math.max(0L, ThanosConfig.SIGHT_TICKS.get() - spent) / 20.0;
    }

    private static String seconds(double seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        // The Gauntlet is always at the lower left of the screen: the off hand is drawn even when it holds nothing.
        if (player.getOffhandItem().isEmpty() && !player.isInvisible()) {
            off.add(1.0F, 0.10F, 0.08F, 0.15F, -8.0F, 0.0F, 20.0F);
            off.ambient(1.0F, 0.0F, 0.005F * Mth.sin((float) s.time * 0.08F), 0.0F);
        }
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        ThanosHeroGlow.tick(mc.level);
        ThanosAtmosphere.tick(mc);
        ThanosMusic.tick(mc);
        ThanosHistory.tick(mc);
    }

    @Override
    public void clear() {
        ThanosHistory.clear();
        ThanosFx.clear();
        ThanosHeroGlow.clear();
        ThanosAtmosphere.clear();
        ThanosMusic.stop();
        ThanosBossBar.clear();
        ThanosClientView.clear();
    }
}
