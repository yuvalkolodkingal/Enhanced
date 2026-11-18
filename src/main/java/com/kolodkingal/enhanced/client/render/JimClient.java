package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Jim Hammond (3.17), as a {@link HeroClient} plug-in: his HUD lines ({@code client/jim/JimHud}), his
 * stances (the Fire Beam holds the arm out as Vision's beam does; the Nova Flame's gathering opens the arms to the heat and its
 * blast throws them back, as the Solar Jewel's do) and, every tick, the flames that stream from his burning body ({@link JimFx}).
 * His flight is Vision's. The flames themselves are {@link JimFlameLayer}'s; the beam and the Nova are drawn with the other
 * beams ({@link WorldEffectsRenderer}).
 */
public final class JimClient implements HeroClient {
    @Override
    public Power power() {
        return Power.JIM_HAMMOND;
    }

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        // (In a class of the client's own: the game tests load this plug-in on a server.)
        return com.kolodkingal.enhanced.client.jim.JimHud.status(player, data, ability, now);
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        on[Stance.VISION_BEAM.ordinal()] = data.is(Live.BEAM_CHARGING) || data.is(Live.BEAM_FIRING);
        on[Stance.SOLAR_GATHER.ordinal()] = data.is(Live.JIM_NOVA_CHARGING);
        on[Stance.SOLAR_OVERLOAD.ordinal()] = data.is(Live.JIM_NOVA_BLAST);
    }

    @Override
    public void tick(Minecraft mc) {
        JimFx.tick(mc);
    }

    @Override
    public void clear() {
        JimFx.clear();
    }
}
