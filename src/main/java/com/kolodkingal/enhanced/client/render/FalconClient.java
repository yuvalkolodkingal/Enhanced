package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Falcon (3.8), as a {@link HeroClient} plug-in: his HUD lines (the wings' state, the Wing Shield's
 * time, Redwing's time and health or its repairs), his ten gestures ({@link FalconGestures}, started by the effects of
 * his moves, {@link FalconActing}), his three stances (the glide, the shield, the dive), his hands in first person, and
 * his light run ({@link RunCharacter#FALCON}). His wings are {@code client/falcon/FalconWingsLayer}'s, his effects (the
 * nozzles' flames, the darts' sparks, Redwing's marks and zaps) {@code client/falcon/FalconEffects}'.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a
 * client class except in a signature of {@link HeroClient}.
 */
public final class FalconClient implements HeroClient {
    @Override
    public Power power() {
        return Power.FALCON;
    }

    // ---- the HUD ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        // (In a class of its own: the game tests load this plug-in on a server, where no LocalPlayer may be named.)
        return com.kolodkingal.enhanced.client.falcon.FalconHud.status(player, data, ability, now);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        FalconGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        FalconActing.stanceTargets(s, p, data, on);
    }

    @Override
    public float lean(HeroAnimState st) {
        return FalconActing.lean(st);
    }

    @Override
    public void ambient(PoseContext c) {
        FalconActing.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        FalconActing.aimed(c);
    }

    // ---- which gesture stands for what ----

    @Override
    public boolean startedElsewhere(GestureId id) {
        // Every gesture of his is started by the effect of the move it acts (it names him, or his Redwing).
        return id.name().startsWith("FALCON_");
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main,
                            FirstPersonArms.Offset off) {
        FalconActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        if (mc.level != null) {
            FalconActing.tick(mc.level);
            com.kolodkingal.enhanced.client.falcon.FalconEffects.tick(mc);
        }
    }

    @Override
    public void clear() {
        FalconActing.clear();
        com.kolodkingal.enhanced.client.falcon.FalconEffects.clear();
    }
}
