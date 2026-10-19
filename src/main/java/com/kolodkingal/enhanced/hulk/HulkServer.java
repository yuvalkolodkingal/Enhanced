package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of the Hulk (3.6), as a {@link com.kolodkingal.enhanced.ability.HeroServer} plug-in: his keys, his
 * body and his tick are {@link HulkAbilities} (with {@link HulkForms}, Bruce Banner and the two Hulks; {@link
 * HulkRage}, the rage and Banner's anger; {@link HulkTerrain}, the blocks he breaks; the moves {@link ThunderClap},
 * {@link SuperLeap}, {@link GrabSmash}, {@link GammaCharge} and the {@link HulkBoulderEntity}); his damage hooks {@link
 * HulkEvents}; how he is made, the Gamma Emitter ({@link GammaExposure}).
 */
public final class HulkServer implements HeroServer {
    @Override
    public Power power() {
        return Power.HULK;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        HulkAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        HulkAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        HulkAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        HulkAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        HulkAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        HulkAbilities.clearData(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        HulkAbilities.onGone(player, true);
        HulkEvents.forget(player);
        GammaExposure.forget(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        HulkAbilities.onGone(player, false);
        HulkEvents.forget(player);
        GammaExposure.forget(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        HulkAbilities.onGone(player, true);
    }

    @Override
    public void onServerStopped() {
        HulkAbilities.clear();
        HulkEvents.clear();
        GammaExposure.clear();
    }
}
