package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
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
 * The client side of the hero Ultron, as a {@link HeroClient} plug-in. It only routes: what his HUD shows and which eyes
 * glow is {@link UltronView}; how his two stances stand (both palms along the look for the plasma, the raised palm of the
 * Overcharge) is {@link UltronHeroPoses}; the motes and ribbons of his flight are {@link UltronFlight}; what the key
 * presses and the payloads of his effects do besides (the gesture that acts before the server answers, his hands in first
 * person) is {@link UltronActing}. The boss, his Sentries and the fight are drawn by {@link UltronRenderer},
 * {@link UltronEffects} and the rest of the Ultron classes of this package, not by this plug-in.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server, so nothing in this class
 * names a client class except in a signature of {@link HeroClient} and each method hands its arguments on as they came
 * (the logic lives in the classes above, loaded only on a client).
 *
 * <p>His animation: the keys of his gestures are {@link UltronGestures} (the take-off, the plasma, the Overcharge and its
 * blast, the Pull, the Slam, the Upload, the Hack, the Army and Command); the routes that start them are the lines of his
 * own in {@link GestureRoutes} and the methods below, and the payloads of the Pull, the Slam and Command
 * ({@link UltronActing#onFx}); his flight profile is {@code FlightProfile.ULTRON} and his walk {@code RunPoses.ULTRON}.
 */
public final class UltronClient implements HeroClient {
    @Override
    public Power power() {
        return Power.ULTRON;
    }

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return UltronView.status(player, data, ability, now);
    }

    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return UltronView.eyes(player, data);
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        boolean charging = data.is(Live.ULTRON_CHARGING);
        on[Stance.ULTRON_CHARGE.ordinal()] = charging;
        on[Stance.ULTRON_FIRE.ordinal()] = data.beamActive() && !charging;
    }

    @Override
    @Nullable
    public GestureId takeoffGesture() {
        return GestureId.TAKEOFF_ULTRON;
    }

    @Override
    public float lean(HeroAnimState st) {
        // (The gesture that carries the raised palm holds the lean itself: the stance's gives way by its weight.)
        return UltronGestures.FIRE_LEAN * st.stance(Stance.ULTRON_FIRE)
                + UltronGestures.CHARGE_LEAN * st.stance(Stance.ULTRON_CHARGE) * (1.0F - UltronGestures.owningTheCharge(st));
    }

    @Override
    public void aimed(PoseContext c) {
        UltronHeroPoses.aimed(c);
    }

    @Override
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        return UltronActing.predicted(data, ability, now, sneaking);
    }

    @Override
    public void registerGestures() {
        UltronGestures.register();
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return switch (ability) {
            case 1 -> GestureId.ULTRON_BLAST;      // the Overcharge's cooldown starts when its blast flies
            case 4 -> GestureId.ULTRON_UPLOAD;     // the Upload, and Second Life
            case 5 -> GestureId.ULTRON_ARMY;
            default -> null;
        };
    }

    @Override
    @Nullable
    public GestureId flagGesture(GestureRoutes.Flag flag, boolean on) {
        return flag == GestureRoutes.Flag.BEAM && on ? GestureId.ULTRON_FIRE_START : null;
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case ULTRON_CHARGING -> GestureId.ULTRON_CHARGE_START;
            case CASTING -> GestureId.ULTRON_HACK;
            default -> null;
        };
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        // The take-off is the animator's own edge; the Pull, the Slam and Command are told by the payload of their effect.
        return id == GestureId.TAKEOFF_ULTRON || id == GestureId.ULTRON_PULL || id == GestureId.ULTRON_SLAM || id == GestureId.ULTRON_COMMAND;
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        UltronActing.started(player, id);
    }

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        UltronActing.firstPerson(s, player, main, off);
    }

    @Override
    public void flightParticles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density, boolean local) {
        UltronFlight.particles(level, player, st, fx, density, local);
    }

    @Override
    public void flightRibbons(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx,
                              float time) {
        UltronFlight.ribbons(pose, vc, camera, player, st, fx, time);
    }

    @Override
    public void tick(Minecraft mc) {
        UltronView.tick(mc);
    }

    @Override
    public void clear() {
        UltronView.clear();
        UltronEffects.clear();
        UltronAnimation.clear();
        UltronTracker.clear();
    }
}
