package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Iron Man (3.8), as a {@link HeroServer} plug-in: his keys, his body and his tick are {@link
 * IronManAbilities} (the suits {@link IronSuits}, H {@link SuitControl}, the suit-up {@link SuitPhases}, the meter
 * {@link SuitEnergy}, each suit's {@link SuitMoves}); his event hooks {@link IronEvents}; how he is made, the Arc
 * Reactors ({@code item/ArcReactorItem}, {@code item/NanoArcReactorItem}). Lane L3; lane L4 adds the nanotech suits'
 * and the Hulkbuster's moves through {@link SuitMoves} and {@link SuitPhases.Listener}.
 */
public final class IronManServer implements HeroServer {
    @Override
    public Power power() {
        return Power.IRON_MAN;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        IronManAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        IronManAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        IronManAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        IronManAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        IronManAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        IronManAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        IronManAbilities.forget(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        IronManAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        IronManAbilities.onGone(player);
    }

    @Override
    public void onServerStopped() {
        IronManAbilities.clear();
    }
}
