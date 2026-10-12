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
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The client side of Thanos, as a {@link HeroClient} plug-in (DESIGN section 15): the HUD lines of the five stones, the
 * Gauntlet's light and the life of everything the Thanos lane draws (the effects, the music, the air round the Mad
 * Titan, his bar), which this plug-in starts and stops with the client. The Gauntlet itself is {@link GauntletLayer}.
 *
 * <p>His animation (DESIGN section 15.12): the keys of his gestures are {@link ThanosGestures} (one for each use of each
 * stone, plus the Veil closing and opening), the rows of his two stances {@link ThanosPoses}, the routes that start the
 * gestures the lines of his own in {@link GestureRoutes} and the methods below; what the client adds (the key press that
 * acts before the server answers, the landing of a Titan, the Gauntlet hand in first person) is {@link ThanosActing}
 * and the flourishes at the hand {@link ThanosGestureFx}. He does not fly, so he has no flight profile; his walk is
 * {@code RunPoses.THANOS}.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which a dedicated server does too when it lists the heroes, so
 * nothing in this class names a client class except in a signature of {@link HeroClient}: each method hands its arguments
 * on as they came to the classes above, loaded only on a client.
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

    // ---- the animation (the keys are ThanosGestures, the stances' rows ThanosPoses, the rest ThanosActing) ----

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        ThanosGestures.stances(data, on);
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        ThanosActing.edges(s, p);
    }

    @Override
    public float lean(HeroAnimState st) {
        return ThanosGestures.DRAIN_LEAN * st.stance(Stance.THANOS_DRAIN);
    }

    @Override
    public void ambient(PoseContext c) {
        ThanosPoses.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        ThanosPoses.aimed(c);
    }

    @Override
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        return ThanosActing.predicted(data, ability, now, sneaking);
    }

    @Override
    public void registerGestures() {
        ThanosGestures.register();
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return ThanosGestures.cooldown(ability, left);
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case SOUL_DRAINING -> GestureId.THANOS_DRAIN_CAST;
            case SOUL_SIGHT -> GestureId.THANOS_SIGHT;
            default -> null;
        };
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        // The Slam, the Pull and the Rewind are the second use of a key, told from the first by the length of the
        // cooldown it starts (a length the completeness check's two small probes never reach).
        return ThanosGestures.toldByCooldown(id);
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ThanosGestureFx.started(player, id);
    }

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        ThanosActing.firstPerson(s, player, main, off);
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        ThanosGestureFx.tick(mc.level);
        ThanosHeroGlow.tick(mc.level);
        ThanosAtmosphere.tick(mc);
        ThanosMusic.tick(mc);
        ThanosHistory.tick(mc);
    }

    @Override
    public void clear() {
        ThanosGestureFx.clear();
        ThanosHistory.clear();
        ThanosFx.clear();
        ThanosHeroGlow.clear();
        ThanosAtmosphere.clear();
        ThanosMusic.stop();
        ThanosBossBar.clear();
        ThanosClientView.clear();
    }
}
