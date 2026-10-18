package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.client.render.CustomActing;
import com.kolodkingal.enhanced.client.render.FirstPersonArms;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.ShieldStyle;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of the custom hero (3.3), as a hero plug-in. His keys are whatever powers he put on them. 3.4: he acts
 * as well as the built-in heroes: every binding plays its gesture by the cast marker, a borrowed ability raises its
 * hero's stances and plays its hero's routes, he flies, walks and speeds in the styles of his definition, and his hands
 * move in first person ({@code CustomActing} in the render package does the work, where the animator's hooks are; the
 * tables are {@link CustomAnimRoutes} and {@link HeroAbilityAnims}). His HUD, skin, field and senses are drawn by the
 * other classes of this package.
 */
public final class CustomClient implements HeroClient {
    @Override
    public Power power() {
        return Power.CUSTOM;
    }

    /**
     * A custom hero's cooldowns are not routed (his cast marker plays each binding: a strength that changes a cooldown's
     * length would fool a route that reads it), but the completeness check of the routes asks every hero acted by a
     * cooldown for a gesture: the cast of a generic press, Billy's hand thrust out, is what a press plays by default.
     */
    @Nullable
    @Override
    public GestureId cooldownGesture(int ability, long left) {
        return GestureId.BILLY_BOLT;
    }

    @Nullable
    @Override
    public GestureId grabGesture() {
        return GestureId.TK_GRAB_WANDA;
    }

    @Nullable
    @Override
    public GestureId flingGesture() {
        return GestureId.TK_FLING_WANDA;
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        CustomActing.stanceTargets(s, p, data, on, now);
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        CustomActing.edges(s, p, data, now);
    }

    @Override
    public float lean(HeroAnimState st) {
        return CustomActing.lean(st);
    }

    @Override
    public float drop(HeroAnimState st) {
        return CustomActing.drop(st);
    }

    @Nullable
    @Override
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : CustomActing.predicted(data, ability, now, sneaking, player);
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        CustomActing.gestureStarted(player, state, id, param);
    }

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        CustomActing.firstPerson(s, player, main, off);
    }

    @Nullable
    @Override
    public ShieldStyle shieldStyle(PowerData data) {
        return CustomActing.shieldStyle(data);
    }

    @Override
    public void tick(Minecraft mc) {
        CustomFx.tick(mc);
    }

    @Override
    public void clear() {
        CustomFx.clear();
    }
}
