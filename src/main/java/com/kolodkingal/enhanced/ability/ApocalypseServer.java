package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.apocalypse.SandHazards;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Apocalypse, as a {@link HeroServer} plug-in: the five abilities and the body of En Sabah Nur are
 * {@link ApocalypseAbilities} (the shield is {@link ApocalypseGuard}), and the events that belong to him (the shield's
 * damage hook, the healing factor) are in {@code ApocalypseEvents}. The boss he is won from is the
 * {@code apocalypse} package (DESIGN section 17).
 */
public final class ApocalypseServer implements HeroServer {
    @Override
    public Power power() {
        return Power.APOCALYPSE;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        ApocalypseAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        ApocalypseAbilities.tick(player, data);
    }

    @Override
    public void everyPlayerTick(ServerPlayer player, PowerData data) {
        ApocalypseAbilities.everyPlayerTick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        ApocalypseAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        ApocalypseAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        ApocalypseAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        ApocalypseAbilities.onGone(player, true);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        ApocalypseAbilities.onGone(player, false);
    }

    /** The sand that was rolling when the server stopped, and what the hero carried from tick to tick, are forgotten. */
    @Override
    public void onServerStopped() {
        SandHazards.clear();
        ApocalypseAbilities.clear();
    }
}
