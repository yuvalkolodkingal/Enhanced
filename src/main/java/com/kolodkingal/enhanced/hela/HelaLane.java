package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.BossLane;
import com.kolodkingal.enhanced.config.HelaBossConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How Hela is called (3.5, PLAN 6.1): by breaking Hela's Seal behind the Throne of Asgard (the block, lane L1's, calls
 * {@link #onSeal}: a first click warns, a second within five seconds breaks it), or by an operator's {@code /enhanced boss
 * start hela [force]} from anywhere. The framework ({@code BossFights.start}) has checked what every boss shares (a lane,
 * the difficulty, the number of fights, the rest); this lane checks what is hers, before anything happens:
 *
 * <ol>
 *     <li>{@code hela_boss.summonEnabled};</li>
 *     <li>the starter is in Asgard, and not in Creative or Spectator;</li>
 *     <li>the seal is not still healing from the last fight ({@code AsgardState.sealReadyAt}).</li>
 * </ol>
 *
 * <p>An operator's forced start skips them all: he may be anywhere, in any mode. Nothing is consumed (no refund). The
 * fight is always in Asgard's throne room (its chunks held by the fight's tickets), except in the GameTests, which place
 * it with {@link #testSite}.
 */
public final class HelaLane implements BossLane {
    /** A second click on the seal within this many ticks breaks it. */
    public static final int CONFIRM_TICKS = 100;

    /** Player -> the game time of their first click on the seal. */
    private static final Map<UUID, Long> ASKED = new HashMap<>();

    /**
     * The GameTests place the fight in their own arena: centred on this point of the starter's level, with
     * {@link #testRadius} (null: the throne room of Asgard).
     */
    @VisibleForTesting
    @Nullable
    public static Vec3 testSite;
    @VisibleForTesting
    public static double testRadius = 9.0;

    // ---------------------------------------------------------------------------------------------------------
    // The seal

    /**
     * A player clicks Hela's Seal at {@code pos}: the first click tells him what it is, a second within five seconds breaks
     * it and calls her (if the framework and the lane allow: what refuses is told in the action bar).
     *
     * @return whether a fight began
     */
    public static boolean onSeal(ServerPlayer player, BlockPos pos) {
        long now = player.serverLevel().getGameTime();
        Long asked = ASKED.get(player.getUUID());
        if (asked == null || now - asked > CONFIRM_TICKS) {
            ASKED.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable("message.enhanced.hela.seal_ask").withStyle(ChatFormatting.DARK_GREEN), true);
            return false;
        }
        ASKED.remove(player.getUUID());
        return BossFights.start(BossKind.HELA, player, false);
    }

    /** Forgets the clicks (the server stopped). */
    public static void clear() {
        ASKED.clear();
    }

    // ---------------------------------------------------------------------------------------------------------
    // The lane

    @Nullable
    @Override
    public Component refusal(ServerPlayer starter, boolean force) {
        if (force) {
            return testSite == null && AsgardDimension.level(starter.server) == null
                    ? Component.translatable("message.enhanced.hela.no_asgard") : null;
        }
        if (!HelaBossConfig.SUMMON_ENABLED.get()) {
            return Component.translatable("message.enhanced.hela.disabled");
        }
        if (testSite == null && !AsgardDimension.in(starter)) {
            return Component.translatable("message.enhanced.hela.not_asgard");
        }
        if (starter.isCreative() || starter.isSpectator()) {
            return Component.translatable("message.enhanced.hela.creative");
        }
        long ready = AsgardState.get(starter.server).sealReadyAt();
        long now = starter.serverLevel().getGameTime();
        if (ready > now) {
            return Component.translatable("message.enhanced.hela.seal_healing", (ready - now + 1199) / 1200);
        }
        return null;
    }

    @Nullable
    @Override
    public BossFight create(ServerPlayer starter, boolean force) {
        if (testSite != null) {
            return HelaFight.create(starter.serverLevel(), testSite, testRadius, testSite.y, testSite.y + 16.0, testSite,
                    testSite.add(0.0, 2.0, -testRadius * 0.5));
        }
        ServerLevel asgard = AsgardDimension.level(starter.server);
        if (asgard == null) {
            return null;
        }
        BlockPos c = CityPlan.THRONE_ROOM_CENTER;
        Vec3 center = new Vec3(c.getX() + 0.5, CityPlan.THRONE_ROOM_FLOOR, c.getZ() + 0.5);
        // (An operator may start her from far away: the hall's chunks are made and loaded before she forms in it.)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                asgard.getChunk((c.getX() >> 4) + dx, (c.getZ() >> 4) + dz);
            }
        }
        Vec3 dais = dais(asgard);
        BlockPos s = CityPlan.HELA_SEAL;
        Vec3 seal = new Vec3(s.getX() + 0.5, s.getY() + 0.5, s.getZ() + 1.0);
        return HelaFight.create(asgard, center, HelaFight.THRONE_RADIUS, CityPlan.THRONE_ROOM_FLOOR, CityPlan.THRONE_ROOM_CEILING, dais, seal);
    }

    /**
     * Where she forms: on the dais before the throne (the top of the floor there, found by looking down from over it, so
     * that it is right whatever the steps' height in the realm's template; the hall's floor if nothing is built there).
     */
    static Vec3 dais(ServerLevel asgard) {
        BlockPos throne = CityPlan.THRONE;
        double x = throne.getX() + 0.5;
        double z = throne.getZ() + 4.5;
        double y = HelaRules.groundAt(asgard, x, throne.getY() + 2.0, z);
        if (y < CityPlan.THRONE_ROOM_FLOOR - 1 || y > throne.getY() + 2) {
            y = CityPlan.THRONE_ROOM_FLOOR;
        }
        return new Vec3(x, y, z);
    }
}
