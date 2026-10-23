package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.HeimdallSightPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Heimdall's All-Seeing Sight, the server's half (3.5): who it shows him of the players his client cannot see by itself.
 * Every {@link #EVERY} ticks while it is open (his G, his Far Sight, or the Sight the Guardian's Watch lends), he alone is
 * sent {@link HeimdallSightPayload}: every other player of his dimension farther than {@link #NEAR} blocks (any distance),
 * and with {@code heimdall.seeOtherRealms} those of the other dimensions, the nearest first, at most
 * {@link HeimdallSightPayload#MAX_PLAYERS}. The creatures near him are seen by the client from what it tracks (the glow is
 * the client's, {@code HeimdallSightView}).
 */
public final class HeimdallSight {
    /** How often the list is sent (ticks). */
    public static final int EVERY = 10;
    /** Players closer than this are tracked by his client already. */
    public static final double NEAR = 48.0;

    /** Whether the Sight is open for this player: his own (G or Far Sight) or the one his Watch lends. */
    public static boolean open(Player player) {
        PowerData data = Powers.get(player);
        boolean own = data.has(HeroAbility.HEIMDALL_SIGHT) && (data.toggle(Toggle.HEIMDALL_SIGHT) || data.is(Live.HEIMDALL_FARSIGHT));
        return own || player instanceof ServerPlayer server && GuardiansWatch.watching(server);
    }

    /** The far players the Sight shows {@code seer} now. */
    public static List<HeimdallSightPayload.Far> far(ServerPlayer seer) {
        boolean otherRealms = !EnhancedConfig.SPEC.isLoaded() || HeimdallConfig.SEE_OTHER_REALMS.get();
        List<ServerPlayer> seen = new ArrayList<>();
        for (ServerPlayer other : seer.server.getPlayerList().getPlayers()) {
            if (other == seer || other.isSpectator()) {
                continue;
            }
            boolean here = other.level() == seer.level();
            if (here ? other.distanceToSqr(seer) > NEAR * NEAR : otherRealms) {
                seen.add(other);
            }
        }
        seen.sort(Comparator.comparing((ServerPlayer p) -> p.level() != seer.level()).thenComparingDouble(p -> p.level() == seer.level()
                ? p.distanceToSqr(seer) : 0.0));
        List<HeimdallSightPayload.Far> out = new ArrayList<>();
        for (ServerPlayer p : seen) {
            if (out.size() >= HeimdallSightPayload.MAX_PLAYERS) {
                break;
            }
            String name = p.getGameProfile().getName();
            out.add(new HeimdallSightPayload.Far(p.getUUID(), name.length() > 64 ? name.substring(0, 64) : name,
                    p.level().dimension().location(), p.position()));
        }
        return out;
    }

    /** Sends him what the Sight shows. */
    public static void send(ServerPlayer seer) {
        Net.sendToPlayer(seer, new HeimdallSightPayload(far(seer)));
    }

    /** The Sight closed: his client forgets the far players. */
    public static void stop(ServerPlayer seer) {
        Net.sendToPlayer(seer, new HeimdallSightPayload(List.of()));
    }

    private HeimdallSight() {
    }
}
