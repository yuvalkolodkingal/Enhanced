package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Red Hulk (3.8), as a {@link HeroServer} plug-in: his keys, his body and scale and his tick are
 * {@link RedHulkAbilities} (with {@link RossForm}, Ross and Red Hulk; {@link Anger} and its {@link RedHulkBurst}; the
 * moves {@link HeatWave}, {@link SuperJump}, {@link EnergyAbsorb}, and the Hulk's Bound and grab through their 3.8
 * overloads); his damage hooks {@link RedHulkEvents}. He is made by the Super Soldier Serum's Red Hulk card (lane L1)
 * or "Choose a hero".
 */
public final class RedHulkServer implements HeroServer {
    @Override
    public Power power() {
        return Power.RED_HULK;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        RedHulkAbilities.onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        RedHulkAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        RedHulkAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        RedHulkAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        RedHulkAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        RedHulkAbilities.clearData(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        RedHulkAbilities.onGone(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        RedHulkAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        RedHulkAbilities.endAll(player, com.kolodkingal.enhanced.power.Powers.get(player));
    }

    @Override
    public void onServerStopped() {
        RedHulkAbilities.clear();
    }
}
