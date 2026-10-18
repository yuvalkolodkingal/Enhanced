package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Hela, the Goddess of Death (3.5), the hero, as a {@link HeroServer} plug-in. It only routes: her keys,
 * her upkeep and her body are {@link HelaAbilities}, her dead {@link HelaArmy}, her events (the strength of Asgard, the
 * form's blades) {@link HelaEvents}.
 */
public final class HelaServer implements HeroServer {
    @Override
    public Power power() {
        return Power.HELA;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        HelaAbilities.onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        HelaAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        HelaAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        HelaAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        HelaAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        HelaAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        HelaAbilities.onGone(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        HelaAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        HelaArmy.dismiss(player);
    }

    @Override
    public void onServerStopped() {
        HelaAbilities.clear();
        HelaLane.clear();
    }
}
