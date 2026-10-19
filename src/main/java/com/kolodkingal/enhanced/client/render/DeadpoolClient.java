package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;


/**
 * The client side of Deadpool (3.7), as a {@link HeroClient} plug-in: his HUD lines (the magazine "16/16" and its
 * reload, the Parry's time, the chimichangas in his bag, Dogpool's time and health), his sixteen gestures ({@link
 * DeadpoolGestures}, started by the effects of his moves, {@link DeadpoolActing}), his two stances (the spinning
 * swords, the drawn pistols), his hands in first person, and his jaunty jog ({@link RunCharacter#DEADPOOL}). His swords
 * and pistols are {@link DeadpoolGearLayer}'s, his jokes' bubbles {@code client/deadpool/LineBubbles}', his effects
 * {@code client/deadpool/DeadpoolEffects}'.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a
 * client class except in a signature of {@link HeroClient}.
 */
public final class DeadpoolClient implements HeroClient {
    @Override
    public Power power() {
        return Power.DEADPOOL;
    }

    // ---- the HUD ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        // (In a class of its own: the game tests load this plug-in on a server, where no LocalPlayer may be named.)
        return com.kolodkingal.enhanced.client.deadpool.DeadpoolHud.status(player, data, ability, now);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        DeadpoolGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        DeadpoolActing.stanceTargets(s, p, data, on);
    }

    @Override
    public void aimed(PoseContext c) {
        DeadpoolActing.aimed(c);
    }

    // ---- which gesture stands for what ----

    @Override
    public boolean startedElsewhere(GestureId id) {
        // Every gesture of his is started by the effect of the move it acts (it names him).
        return id.name().startsWith("DEADPOOL_");
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main,
                            FirstPersonArms.Offset off) {
        DeadpoolActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        if (mc.level != null) {
            DeadpoolActing.tick(mc.level);
        }
    }

    @Override
    public void clear() {
        DeadpoolActing.clear();
    }
}
