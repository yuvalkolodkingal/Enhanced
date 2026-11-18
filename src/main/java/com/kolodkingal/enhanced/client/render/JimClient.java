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
    public void registerGestures() {
        // The throw of a fireball (V): the arm wound back behind him, the body leaning away; then hurled forward and up along
        // the look as the ball is released (tick 5), and brought back. (A positive x turns the arm backward, a negative forward.)
        Gestures.define(GestureId.JIM_THROW)
                .key(0, AnimMath.Ease.S).m(0, 0, 0).lean(0).twist(0)
                .key(3, AnimMath.Ease.O).m(62, -6, 16).lean(-7).twist(-16)
                .key(5, AnimMath.Ease.I).m(-112, 0, -5).lean(9).twist(14)
                .key(9, AnimMath.Ease.S).m(-96, 0, -4).lean(4).twist(6)
                .key(14, AnimMath.Ease.S).m(0, 0, 0).lean(0).twist(0);
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return live == Live.JIM_THROW ? GestureId.JIM_THROW : null;
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        on[Stance.VISION_BEAM.ordinal()] = data.is(Live.BEAM_FIRING);
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
