package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The server side of Heimdall, the Guardian of the Bifrost (3.5), as a {@link HeroServer} plug-in: it routes his keys,
 * ticks, body and what ends when he leaves to {@link HeimdallAbilities}; for every player it keeps the Guardian's Watch
 * ({@link GuardiansWatch}) and makes Heimdall of the worthy: a powerless player who won the Watch and holds his own Hofund
 * ({@link #awakens}). His events are {@link HeimdallEvents}, his words in the Observatory {@link HeimdallTalk}.
 */
public final class HeimdallServer implements HeroServer {
    public HeimdallServer() {
        HeimdallTalk.register();
    }

    @Override
    public Power power() {
        return Power.HEIMDALL;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        HeimdallAbilities.onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /** 3.4: the key with its Sneak variant said by the caller (a custom hero's binding). */
    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        HeimdallAbilities.onKey(player, data, ability, down, alt);
    }

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        HeimdallAbilities.tick(player, data);
    }

    /** Every player: the Watch he keeps goes on; and the worthy who hold their own Hofund become Heimdall. */
    @Override
    public void everyPlayerTick(ServerPlayer player, PowerData data) {
        GuardiansWatch.tick(player);
        if (!player.isSpectator() && player.isAlive() && awakens(player, data)) {
            awaken(player);
        }
    }

    /**
     * Whether this player becomes Heimdall now: he has no power, he won the Guardian's Watch, and he holds (either hand) a
     * Hofund bound to him; as the worthy become Thor holding their Mjolnir.
     */
    public static boolean awakens(ServerPlayer player, PowerData data) {
        return data.power() == Power.NONE && player.getData(ModAttachments.ASGARD).has(AsgardProgress.WATCH_WON)
                && HofundBond.holdsOwn(player);
    }

    /** He becomes Heimdall, in the light of the Bifrost. */
    static void awaken(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_ARRIVAL, player.getId(), List.of(player.position())));
        Bifrost.mark(level, player.position());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BIFROST_ARRIVE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        Powers.awaken(player, Power.HEIMDALL);
    }

    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        HeimdallAbilities.applyBody(player, data);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        HeimdallAbilities.endAll(player, data);
    }

    @Override
    public void stopHold(PowerData data) {
        HeimdallAbilities.stopHold(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        GuardiansWatch.lose(player, "message.enhanced.watch.lost");
        HeimdallAbilities.onGone(player);
    }

    @Override
    public void onDeath(ServerPlayer player) {
        GuardiansWatch.lose(player, "message.enhanced.watch.fallen");
        HeimdallAbilities.onGone(player);
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        GuardiansWatch.lose(player, "message.enhanced.watch.left");
        HeimdallAbilities.onGone(player);
    }

    @Override
    public void onServerStopped() {
        GuardiansWatch.clear();
        HeimdallAbilities.clear();
    }
}
