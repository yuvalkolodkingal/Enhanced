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
 * The client side of Doctor Strange (3.6), as a {@link HeroClient} plug-in. It only routes: the keys of his gestures
 * and the poses of his stances are {@link StrangeGestures}; his stances' rows, his hands in first person, the sparks of
 * his gestures and his HUD {@link StrangeActing}; the spells everyone sees (the whip, the bands, the Shield, the Dome,
 * the Eye's rings) {@link StrangeSpellFx}; the Mirror Dimension's warp {@code client/strange/MirrorView}; the Cloak of
 * Levitation, the Eye on his chest and the spirit's glow are layers ({@link CloakLayer}, {@link EyeAmuletLayer}, {@link
 * AstralLayer}); the portals and the kneeling body their renderers.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a
 * client class except in a signature of {@link HeroClient}.
 */
public final class StrangeClient implements HeroClient {
    @Override
    public Power power() {
        return Power.DOCTOR_STRANGE;
    }

    // ---- the HUD ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return StrangeActing.status(player, data, ability, now);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        StrangeGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        StrangeActing.stanceTargets(s, p, data, on, now);
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        StrangeActing.edges(s, p, data, now);
    }

    @Override
    public GestureId takeoffGesture() {
        return GestureId.TAKEOFF_STRANGE;
    }

    @Override
    public float lean(HeroAnimState st) {
        return StrangeActing.lean(st);
    }

    @Override
    public float drop(HeroAnimState st) {
        return StrangeActing.drop(st);
    }

    @Override
    public void ambient(PoseContext c) {
        StrangeActing.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        StrangeActing.aimed(c);
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return live == Live.STRANGE_SHIELD ? GestureId.STRANGE_SHIELD_UP : null;
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        return switch (id) {
            // (the take-off by the animator's own edge; the lashes, the grapple, the bands, the dome, the circle of the
            // portals and the Rewind by the payloads of their sparks, which name the caster)
            case TAKEOFF_STRANGE, STRANGE_WHIP_1, STRANGE_WHIP_2, STRANGE_WHIP_3, STRANGE_GRAPPLE, STRANGE_BANDS,
                 STRANGE_DOME, STRANGE_SLING, STRANGE_REWIND -> true;
            default -> false;
        };
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        StrangeActing.started(player, id);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        StrangeActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        StrangeSpellFx.tick(mc);
    }

    @Override
    public void clear() {
        StrangeSpellFx.clear();
        StrangeActing.clear();
    }
}
