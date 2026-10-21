package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Iron Man (3.8, lane L3), as a {@link HeroClient} plug-in: his HUD lines (each key's suit move and
 * its wait; the suit, its energy and durability, the helmet frame are {@code client/ironman/IronHud}'s), his gestures
 * ({@link IronManGestures}, started by the effects of his moves and suit-ups, {@link IronManActing}) and four stances,
 * his hands in first person, his heavy metallic walk ({@link RunCharacter#IRON_MAN}). The suits themselves are {@code
 * client/ironman/SuitLayer}'s, their effects {@code client/ironman/IronClientFx}'s; the nanotech suits' and the
 * Hulkbuster's own moves are lane L4's.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a
 * client class except in a signature of {@link HeroClient}.
 */
public final class IronManClient implements HeroClient {
    @Override
    public Power power() {
        return Power.IRON_MAN;
    }

    // ---- the HUD ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        // (In a class of the client's own: the game tests load this plug-in on a server.)
        return com.kolodkingal.enhanced.client.ironman.IronHud.status(player, data, ability, now);
    }

    // ---- the reactor in his chest (3.8.1) ----

    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return com.kolodkingal.enhanced.client.ironman.TonyGlow.eyes(player, data);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        IronManGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        IronManActing.stanceTargets(s, p, data, on);
        NanoActing.stanceTargets(s, p, data, on); // (lane L4: the nanotech suits and the Hulkbuster)
    }

    @Override
    public float lean(HeroAnimState st) {
        return IronManActing.lean(st);
    }

    @Override
    public void aimed(PoseContext c) {
        IronManActing.aimed(c);
        NanoActing.aimed(c);
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return live == Live.BEAMING ? GestureId.IRON_UNIBEAM : null;
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        // His moves' gestures are started by their effects (which name him), the Unibeam's by BEAMING (liveGesture).
        return IronManGestures.ours(id);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main,
                            FirstPersonArms.Offset off) {
        IronManActing.firstPerson(s, main, off);
        NanoActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        if (mc.level != null) {
            IronManActing.tick(mc.level);
            com.kolodkingal.enhanced.client.ironman.IronClientFx.tick(mc);
        }
    }

    @Override
    public void clear() {
        IronManActing.clear();
        com.kolodkingal.enhanced.client.ironman.IronClientFx.clear();
    }
}
