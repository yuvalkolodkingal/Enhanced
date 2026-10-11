package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Jean Grey, the Phoenix, as a {@link HeroServer} plug-in (DESIGN section 13). The plug-in only
 * routes: the keys, the ticks, her body, and what ends when she logs out, dies, changes dimension or loses the power,
 * all go to {@link PhoenixAbilities}; her events (fire, targets, effects, the waves) are in {@code PhoenixEvents}.
 *
 * <p>A hero before Thor is routed by the {@code switch} statements of the shared code; this one is found by {@link
 * HeroServers}.
 */
public final class PhoenixServer implements HeroServer {
    @Override
    public Power power() {
        return Power.PHOENIX;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        PhoenixAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        PhoenixAbilities.tick(player, data);
    }

    /** Every player: a charging Phoenix is slow, and nobody else is. */
    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        PhoenixAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        PhoenixAbilities.endAll(player, data);
    }

    /** The charge and its flames are a held key: they end with a dead body. */
    @Override
    public void stopHold(PowerData data) {
        PhoenixAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        PhoenixAbilities.onGone(player, true);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        PhoenixAbilities.onGone(player, true);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        PhoenixAbilities.onGone(player, false);
    }

    @Override
    public void onServerStopped() {
        PhoenixAbilities.clear();
    }
}
