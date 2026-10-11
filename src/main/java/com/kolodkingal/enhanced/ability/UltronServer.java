package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Ultron, as a {@link HeroServer} plug-in (DESIGN section 16): the five abilities and the body are
 * {@link UltronAbilities} and {@link com.kolodkingal.enhanced.ultron.UltronDrones}; what happens to him in a fight is
 * in {@link com.kolodkingal.enhanced.ultron.UltronEvents}. This class only hangs them on the plug-in: his keys, his
 * tick, his body, and the end of anything of his when the power goes or the player does.
 */
public final class UltronServer implements HeroServer {
    @Override
    public Power power() {
        return Power.ULTRON;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        UltronAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        UltronAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        UltronAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        UltronAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        UltronAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        goneIfUltron(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        goneIfUltron(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        goneIfUltron(player);
    }

    @Override
    public void onServerStopped() {
        UltronAbilities.clear();
    }

    /** His army dies with him, with his session and with his world: it is his only as long as he is here and Ultron. */
    private static void goneIfUltron(ServerPlayer player) {
        PowerData data = Powers.get(player);
        if (data.power() == Power.ULTRON) {
            UltronAbilities.gone(player, data);
        }
    }
}
