package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossLane;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * How the Mad Titan is called, as the boss framework asks it ({@link BossLane}): the checks of the ritual's site, made
 * before anything starts (an operator's forced start skips them), and the fight. The ritual consumes nothing, so there is
 * nothing to refund: {@link BossFight#refundOnFailure} is never called.
 *
 * <p>The checks of DESIGN section 15.3: the server allows the call, the Overworld only (unless the server says otherwise),
 * and the summoner stands on solid ground with the open sky above and room all round ({@link ArenaCheck}).
 */
public final class ThanosLane implements BossLane {
    /** How high above the landing the portal hangs, at most. */
    static final double PORTAL_HEIGHT = 30.0;

    /** The tests run under a barrier roof, which no site would pass: they switch the check of the site off. */
    @VisibleForTesting
    public static boolean checkSite = true;

    @Nullable
    @Override
    public Component refusal(ServerPlayer starter, boolean force) {
        if (force) {
            return null;
        }
        return siteRefusal(starter);
    }

    /** Why the site is no good (the message the player reads), or null if it is. */
    @Nullable
    static Component siteRefusal(ServerPlayer summoner) {
        ServerLevel level = summoner.serverLevel();
        if (!ThanosBossConfig.SUMMON_ENABLED.get()) {
            return Component.translatable("message.enhanced.thanos.summon.disabled");
        }
        if (ThanosBossConfig.SUMMON_OVERWORLD_ONLY.get() && level.dimension() != Level.OVERWORLD) {
            return Component.translatable("message.enhanced.thanos.summon.wrong_dimension");
        }
        if (!checkSite) {
            return null;
        }
        ArenaCheck.Verdict verdict = ArenaCheck.verdict(ArenaCheck.of(level), summoner.blockPosition(), ThanosBossConfig.SUMMON_SKY_BLOCKS.get(),
                ThanosBossConfig.SUMMON_OPEN_FRACTION.get(), ThanosBossConfig.ARENA_RADIUS.get());
        return verdictMessage(verdict);
    }

    /** The message for a verdict, or null for OK. */
    @Nullable
    static Component verdictMessage(ArenaCheck.Verdict verdict) {
        return switch (verdict) {
            case OK -> null;
            case NO_SKY -> Component.translatable("message.enhanced.thanos.summon.no_sky");
            case NO_GROUND, TOO_CLOSED, BORDER -> Component.translatable("message.enhanced.thanos.summon.no_room");
        };
    }

    @Nullable
    @Override
    public BossFight create(ServerPlayer starter, boolean force) {
        ServerLevel level = starter.serverLevel();
        Vec3 center = starter.position();
        BlockPos spot = ArenaCheck.arrivalSpot(ArenaCheck.of(level), starter.blockPosition());
        Vec3 landing = spot != null ? ArenaCheck.standAt(spot) : center;
        return ThanosFight.create(level, center, landing, ThanosBossConfig.SUMMON_COUNTDOWN_TICKS.get(), fallHeight(level, landing));
    }

    /**
     * How far above the landing he is made: the portal's height, or less where the column above the landing is not
     * free (a roof, a cave, a forced start indoors), so that he is never made inside a block.
     */
    static double fallHeight(ServerLevel level, Vec3 landing) {
        BlockPos base = BlockPos.containing(landing);
        int free = 0;
        for (int dy = 1; dy <= PORTAL_HEIGHT + 6 && base.getY() + dy < level.getMaxBuildHeight(); dy++) {
            BlockPos pos = base.above(dy);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                break;
            }
            free++;
        }
        return Math.max(0.0, Math.min(PORTAL_HEIGHT, free - 5.0));
    }
}
