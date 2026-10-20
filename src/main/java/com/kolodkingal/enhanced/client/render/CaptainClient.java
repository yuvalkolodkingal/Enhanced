package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Captain America (3.8), as a {@link HeroClient} plug-in: his HUD lines (the shield in hand, in
 * flight with its ricochets left, on his back or gone; the slam; Assemble's seconds; the combo's step), his sixteen
 * gestures ({@link CaptainGestures}, every one started by the payload of his move, which names him) and his guard
 * stance, his first-person hands ({@link CaptainActing}). The shield is drawn by {@code client/captain}: the item
 * ({@code ShieldItemRenderer}), on his back ({@code CapShieldLayer}), in flight ({@code ThrownShieldRenderer}); his
 * moves' effects by {@code CaptainEffects}; the super-soldier's line under the powers' panel by {@code CaptainHud}. He
 * runs as a soldier ({@link RunCharacter#CAPTAIN}); he does not fly.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a
 * client class except in a signature of {@link HeroClient}.
 */
public final class CaptainClient implements HeroClient {
    @Override
    public Power power() {
        return Power.CAPTAIN_AMERICA;
    }

    // ---- the HUD ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        // (In a class of the client's own: the game tests load this plug-in on a server.)
        return com.kolodkingal.enhanced.client.captain.CaptainHud.status(player, data, ability, now);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        CaptainGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        CaptainActing.stanceTargets(s, p, data, on);
    }

    @Override
    public float lean(HeroAnimState st) {
        return CaptainActing.lean(st);
    }

    @Override
    public float drop(HeroAnimState st) {
        return CaptainActing.drop(st);
    }

    @Override
    public void ambient(PoseContext c) {
        CaptainActing.ambient(c);
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return null; // (the charge's and the slam's gestures are their payloads')
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        return id.name().startsWith("CAP_");
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        CaptainActing.gestureStarted(player, state, id, param);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main,
            FirstPersonArms.Offset off) {
        CaptainActing.firstPerson(s, main, off);
    }
}
