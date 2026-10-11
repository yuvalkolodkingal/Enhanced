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
 * <p>Her animation: the keys of her gestures are {@link PhoenixGestures} (the take-off, the grab and the throw of her
 * telekinesis, the cast of each ability, the release of the Burst); the routes that start them are the lines of her
 * own in {@link GestureRoutes} and the methods below; what the client adds (the key press that acts before the server
 * answers, the take-off the charge must not show, her hands in first person, the glints of the mind) is
 * {@link PhoenixActing}; her flight profile is {@code FlightProfile.PHOENIX} and her walk {@code RunPoses.PHOENIX}.
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
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        PhoenixActing.edges(s, p, data);
    }

    @Override
    @Nullable
    public GestureId takeoffGesture() {
        return GestureId.TAKEOFF_PHOENIX;
    }

    @Override
    public float lean(HeroAnimState st) {
        // (The gesture that carries the V holds the lean itself: the stance's gives way by its weight.)
        return PhoenixGestures.CHARGE_LEAN * st.stance(Stance.PHOENIX_CHARGE) * (1.0F - PhoenixGestures.owningTheV(st))
                + 1.0F * st.stance(Stance.PHOENIX_READ);
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
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        return PhoenixActing.predicted(data, ability, now, sneaking);
    }

    @Override
    public void registerGestures() {
        PhoenixGestures.register();
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case CASTING -> GestureId.PHOENIX_MIND_CAST;
            case PHOENIX_CHARGING -> GestureId.PHOENIX_CHARGE_START;
            case MIND_SENSING -> GestureId.PHOENIX_SENSE;
            case PHOENIX_PSIONIC -> GestureId.PHOENIX_PSIONIC_CAST;
            default -> null;
        };
    }

    @Override
    @Nullable
    public GestureId grabGesture() {
        return GestureId.TK_GRAB_PHOENIX;
    }

    @Override
    @Nullable
    public GestureId flingGesture() {
        return GestureId.TK_FLING_PHOENIX;
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        // The take-off is the animator's own edge; the release of the Burst is told by its payload (PhoenixFx).
        return id == GestureId.TAKEOFF_PHOENIX || id == GestureId.PHOENIX_BURST;
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        PhoenixActing.started(player, id);
    }

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        PhoenixActing.firstPerson(s, player, main, off);
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
