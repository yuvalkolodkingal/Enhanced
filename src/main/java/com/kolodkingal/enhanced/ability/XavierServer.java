package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server side of Professor X (3.3), as a {@link HeroServer} plug-in: it only routes, to {@link XavierAbilities}
 * (the keys, the ticks, what ends) and {@link MindLink} (a body he steers). The lifecycle calls come for every player:
 * a player he steers who logs out, dies or changes dimension ends the link as surely as he does.
 */
public final class XavierServer implements HeroServer {
    @Override
    public Power power() {
        return Power.PROFESSOR_X;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        XavierAbilities.onKey(player, data, ability, down);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        XavierAbilities.tick(player, data);
    }

    /** Every player: the thoughts Billy and he hear, and the watchdog that leaves nobody stuck in a link. */
    @Override
    public void everyPlayerTick(ServerPlayer player, PowerData data) {
        XavierAbilities.everyPlayerTick(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        XavierAbilities.endAll(player, data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        XavierAbilities.onGone(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        XavierAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        XavierAbilities.onGone(player);
    }

    @Override
    public void onServerStopped() {
        XavierAbilities.clear();
    }
}
