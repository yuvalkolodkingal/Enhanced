package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.thanos.ThanosSummon;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Thanos, as a {@link HeroServer} plug-in (DESIGN section 15): the five stones and the Titan's body
 * are {@link ThanosAbilities}; what the stones do in the world is {@link com.kolodkingal.enhanced.thanos.StonePowers},
 * which the boss shares; the rules around them (a petrified creature, the Veil, the damage he takes) are {@link
 * com.kolodkingal.enhanced.thanos.ThanosEvents}. This class only hangs them on the plug-in: his keys, his tick, his
 * body, and the end of anything of his when the power goes or the player does. The boss's housekeeping is here too:
 * when the server stops, the summoning forgets who was charging.
 */
public final class ThanosServer implements HeroServer {
    @Override
    public Power power() {
        return Power.THANOS;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        ThanosAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        ThanosAbilities.tick(player, data);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        ThanosAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        ThanosAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        ThanosAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        goneIfThanos(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        goneIfThanos(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        goneIfThanos(player);
    }

    @Override
    public void onServerStopped() {
        ThanosAbilities.clear();
        ThanosSummon.clear();
    }

    /** What stands in the world for him ends with his session, his life and his dimension (the power stays). */
    private static void goneIfThanos(ServerPlayer player) {
        PowerData data = Powers.get(player);
        if (data.power() == Power.THANOS) {
            ThanosAbilities.gone(player, data);
        }
    }
}
