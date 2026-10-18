package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Heimdall, the Guardian of the Bifrost (3.5), as a {@link HeroClient} plug-in. It only routes: the
 * keys of his gestures and the poses of his stances are {@link HeimdallGestures}; his stances' rows, his hands in first
 * person, the blows of his combo and the motes of his gestures {@link HeimdallActing}; the arcs, the Strike's column, the
 * Ward's dome {@link HeimdallEffects}; what his Sight shows {@link HeimdallSightView}; his horned helmet
 * {@link HeimdallHelmLayer}. Here: his HUD lines and the amber of his eyes while he looks with the Sight.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a client
 * class except in a signature of {@link HeroClient}.
 */
public final class HeimdallClient implements HeroClient {
    @Override
    public Power power() {
        return Power.HEIMDALL;
    }

    // ---- the HUD, the eyes ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return HeimdallActing.status(player, data, ability, now);
    }

    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return data.has(HeroAbility.HEIMDALL_SIGHT) && (data.toggle(Toggle.HEIMDALL_SIGHT) || data.is(Live.HEIMDALL_FARSIGHT))
                ? HeimdallActing.eyes() : null;
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        HeimdallGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        HeimdallActing.stanceTargets(s, p, data, on, now);
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        HeimdallActing.edges(s, p, data, now);
    }

    @Override
    public float lean(HeroAnimState st) {
        return HeimdallActing.lean(st);
    }

    @Override
    public float drop(HeroAnimState st) {
        return HeimdallActing.drop(st);
    }

    @Override
    public void ambient(PoseContext c) {
        HeimdallActing.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        HeimdallActing.aimed(c);
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return HeimdallGestures.cooldown(ability, left);
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return live == Live.BIFROST ? GestureId.HEIMDALL_BIFROST : null;
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        return switch (id) {
            // (the blows by his edges; the rest by the payloads of their light)
            case HEIMDALL_SLASH_1, HEIMDALL_SLASH_2, HEIMDALL_SLASH_3, HEIMDALL_CALL, HEIMDALL_STRIKE, HEIMDALL_SEND, HEIMDALL_RETURN -> true;
            default -> false;
        };
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        HeimdallActing.started(player, id);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        HeimdallActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        HeimdallEffects.tick(mc);
        HeimdallSightView.tick(mc);
    }

    @Override
    public void clear() {
        HeimdallEffects.clear();
        HeimdallActing.clear();
        HeimdallSightView.clear();
    }
}
