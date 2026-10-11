package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The client side of Jean Grey, the Phoenix, as a {@link HeroClient} plug-in (DESIGN section 13). It only routes: what
 * her HUD shows and which eyes glow is {@link PhoenixView}; how her stances stand (the head tilt of Mind Reading, the V
 * of arms that gathers the Burst, an open palm for her telekinesis) is {@link PhoenixPoses}; the embers and wing
 * streams of her flight are {@link PhoenixFlight}; what she draws in the world is {@link PhoenixFx}; the glow of Mind
 * Sense is {@code MindSense}, the panel beside her crosshair {@code MindReadingOverlay} and the flames on her body
 * {@link PhoenixFlameLayer}.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server, so nothing in this class
 * names a client class except in a signature of {@link HeroClient} and each method hands its arguments on as they came
 * (the logic lives in the classes above, loaded only on a client).
 *
 * <p>Her gestures (the take-off, the cast of each ability, the release of the Burst), their routes, her flight profile
 * and run, and her first-person hands are the animation's, not yet written here.
 */
public final class PhoenixClient implements HeroClient {
    @Override
    public Power power() {
        return Power.PHOENIX;
    }

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return PhoenixView.status(player, data, ability, now);
    }

    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return PhoenixView.eyes(player, data);
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        on[Stance.PHOENIX_READ.ordinal()] = data.toggle(Toggle.MIND_READING);
        on[Stance.PHOENIX_CHARGE.ordinal()] = data.is(Live.PHOENIX_CHARGING);
    }

    @Override
    public float lean(HeroAnimState st) {
        return -4.0F * st.stance(Stance.PHOENIX_CHARGE) + 1.0F * st.stance(Stance.PHOENIX_READ);
    }

    @Override
    public void ambient(PoseContext c) {
        PhoenixPoses.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        PhoenixPoses.aimed(c);
    }

    @Override
    public void flightParticles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density,
                                boolean local) {
        PhoenixFlight.particles(level, player, st, fx, density, local);
    }

    @Override
    public void flightRibbons(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, HeroAnimState st,
                              FlightEffects.Fx fx, float time) {
        PhoenixFlight.ribbons(pose, vc, camera, player, st, fx, time);
    }

    @Override
    public void tick(Minecraft mc) {
        PhoenixView.tick(mc);
    }

    @Override
    public void clear() {
        PhoenixView.clear();
    }
}
