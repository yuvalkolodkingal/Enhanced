package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/** The server side of Jim Hammond (3.17), as a {@link HeroServer} plug-in: his keys, body and tick are {@link JimAbilities}. */
public final class JimServer implements HeroServer {
    @Override
    public Power power() {
        return Power.JIM_HAMMOND;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        JimAbilities.onKey(player, data, ability, down, false);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        JimAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        JimAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        JimAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        JimAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        data.set(com.kolodkingal.enhanced.power.Live.JIM_NOVA_CHARGING, false);
        data.set(com.kolodkingal.enhanced.power.Live.JIM_FIREBALL_CHARGING, false);
        data.set(com.kolodkingal.enhanced.power.Live.BEAM_FIRING, false);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        JimAbilities.forget(player.getUUID());
    }

    @Override
    public void onDeath(ServerPlayer player) {
        JimAbilities.forget(player.getUUID());
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        JimFireballs.forget(player.getUUID());
    }

    @Override
    public void onServerStopped() {
        JimFireballs.clear();
    }
}
