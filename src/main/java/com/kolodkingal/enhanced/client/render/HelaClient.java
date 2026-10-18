package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.HelaMusic;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.config.HelaConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hela.ArmyMob;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The client side of Hela, the Goddess of Death (3.5), as a {@link HeroClient} plug-in. It routes: her stances, her hands
 * in first person and the gestures her payloads tell are {@link HelaActing}, the keys of her gestures {@link
 * HelaGestures}, the headdress {@link HelaHeaddressLayer} and her blades {@link HelaBladesLayer} (player layers), her
 * effects and her boss bar {@link HelaEffects}, the music of her fight {@link HelaMusic} (ticked here: the plug-ins tick
 * on every client). Her HUD: the form on, her risen dead and the seconds left of the cooldown that brought them, the grasp's
 * seconds.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a client
 * class except in a signature of {@link HeroClient} (the eyes' render type is {@link HelaActing}'s).
 */
public final class HelaClient implements HeroClient {
    /** The local player's grasp began at this game time (for its seconds on the HUD), or -1. */
    private static long graspFrom = -1L;

    @Override
    public Power power() {
        return Power.HELA;
    }

    // ---- the HUD and the eyes ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        switch (ability) {
            case 3 -> {
                if (data.toggle(Toggle.HELA_FORM)) {
                    return Component.translatable("hud.enhanced.hela.form").withStyle(ChatFormatting.GREEN);
                }
            }
            case 4 -> {
                if (data.is(Live.HELA_SUMMONING)) {
                    int risen = player.level().getEntitiesOfClass(ArmyMob.class, player.getBoundingBox().inflate(48.0), ArmyMob::isAlly).size();
                    return Component.translatable("hud.enhanced.hela.allies", risen, String.format(Locale.ROOT, "%.0f",
                            Math.max(0, data.cooldownUntil(4) - now) / 20.0)).withStyle(ChatFormatting.DARK_GREEN);
                }
            }
            case 5 -> {
                if (data.is(Live.HELA_GRASPING)) {
                    if (graspFrom < 0) {
                        graspFrom = now;
                    }
                    int max = EnhancedConfig.SPEC.isLoaded() ? HelaConfig.GRASP_SECONDS.get() * 20 : 120;
                    long left = Math.max(0, graspFrom + max - now);
                    return Component.translatable("hud.enhanced.hela.grasp", String.format(Locale.ROOT, "%.1f", left / 20.0))
                            .withStyle(ChatFormatting.GREEN);
                }
                graspFrom = -1L;
            }
            default -> {
            }
        }
        return PowerHud.cooldown(data, ability, now);
    }

    /** Her eyes burn green while her form is on. */
    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return data.has(HeroAbility.HELA_GODDESS) && data.toggle(Toggle.HELA_FORM) ? HelaActing.eyes() : null;
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        HelaGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        on[Stance.HELA_BLADES.ordinal()] = data.has(HeroAbility.HELA_GODDESS) && data.toggle(Toggle.HELA_FORM);
        on[Stance.HELA_GRASP.ordinal()] = data.has(HeroAbility.HELA_GRASP) && data.is(Live.HELA_GRASPING);
    }

    @Override
    public void ambient(PoseContext c) {
        HelaActing.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        HelaActing.aimed(c);
    }

    @Override
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        return HelaGestures.predicted(data, ability, now, sneaking);
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case HELA_SUMMONING -> GestureId.HELA_RAISE;
            case HELA_GRASPING -> GestureId.HELA_GRASP;
            default -> null;
        };
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        // (Told by the payload of their effect, which names the caster: HelaActing.onFx.)
        return switch (id) {
            case HELA_THROW, HELA_RAIN, HELA_WALL, HELA_BURST, HELA_DASH, HELA_FENRIS -> true;
            default -> false;
        };
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        HelaActing.started(player, id);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        HelaActing.firstPerson(s, player, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        HelaMusic.tick(mc);
        HelaEffects.tick(mc);
    }

    @Override
    public void clear() {
        HelaMusic.stop();
        HelaEffects.clear();
        HelaHeaddressLayer.clear();
        graspFrom = -1L;
    }
}
