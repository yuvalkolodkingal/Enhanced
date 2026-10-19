package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the Gamma Emitter's beam does to the players in it (3.6; the default of decision 18.1). A <b>powerless</b>
 * player who stays in it for {@code [gamma] exposureTicks} becomes the Hulk: the screen flashes green, the title "The
 * Hulk", and the first change comes at once, Banner into the Ragnarok Hulk (the accident). A Hulk in it is healed
 * ({@code healPerSecond}, {@code healsHulk}) and his rage rises. Any other hero hears "The gamma does nothing to you."
 * once a beam. Animals and mobs: nothing. Creative players may awaken too, as in the other rituals. Leaving the beam
 * starts the count again.
 */
public final class GammaExposure {
    /** Each exposed player's count: the ticks in the beam so far, and the last tick he was in it. */
    private static final Map<UUID, long[]> COUNTS = new HashMap<>();
    /** The heroes told this beam that it does nothing to them (by emitter). */
    private static final Map<BlockPos, Set<UUID>> TOLD = new HashMap<>();

    /** The ticks a powerless player stays in the beam to become the Hulk. */
    public static int exposureTicks() {
        return HulkAbilities.cfg(HulkConfig.EXPOSURE_TICKS, 60);
    }

    /** How many ticks in a row the player has stood in a beam (0 if none, or he left it). */
    public static int exposure(ServerPlayer player) {
        long[] count = COUNTS.get(player.getUUID());
        return count == null || player.level().getGameTime() - count[1] > 1 ? 0 : (int) count[0];
    }

    public static void beamStarts(ServerLevel level, BlockPos emitter) {
        TOLD.remove(emitter.immutable());
    }

    public static void beamEnds(ServerLevel level, BlockPos emitter) {
        TOLD.remove(emitter.immutable());
    }

    /** One tick of the beam of the emitter at {@code emitter}, over {@code box}. */
    public static void tick(ServerLevel level, BlockPos emitter, AABB box, long now) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, p -> p.isAlive()
                && !p.isSpectator())) {
            PowerData data = Powers.get(player);
            if (data.power() == Power.NONE) {
                long[] count = COUNTS.computeIfAbsent(player.getUUID(), k -> new long[]{0, Long.MIN_VALUE / 2});
                if (count[1] == now) {
                    continue; // (two beams over him count one tick)
                }
                count[0] = now - count[1] == 1 ? count[0] + 1 : 1;
                count[1] = now;
                if (count[0] >= exposureTicks()) {
                    COUNTS.remove(player.getUUID());
                    awaken(player);
                }
            } else if (data.power() == Power.HULK) {
                if (now % 10 == 0) {
                    if (HulkAbilities.cfg(HulkConfig.HEALS_HULK, true)) {
                        player.heal((float) (HulkAbilities.cfg(HulkConfig.HEAL_PER_SECOND, 2.0) / 2.0));
                    }
                    HulkRage.add(player, data, 2);
                }
            } else if (TOLD.computeIfAbsent(emitter.immutable(), k -> new HashSet<>()).add(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.enhanced.hulk.gamma_nothing")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    /** The accident: he is the Hulk. */
    public static void awaken(ServerPlayer player) {
        Powers.awaken(player, Power.HULK, HulkAbilities.BANNER);
        ServerLevel level = player.serverLevel();
        HulkAbilities.sound(level, player.position(), ModSounds.GAMMA_EXPOSURE.get(), 1.5F, 1.0F);
        PacketDistributor.sendToPlayer(player, new HulkFxPayload(HulkFxPayload.GAMMA_BEAM, player.getId(),
                List.of(player.position()), 1));
        HulkForms.accident(player);
    }

    static void forget(ServerPlayer player) {
        COUNTS.remove(player.getUUID());
    }

    static void clear() {
        COUNTS.clear();
        TOLD.clear();
    }

    private GammaExposure() {
    }
}
