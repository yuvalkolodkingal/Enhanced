package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * What If... Ultron Won? (3.19.1), as in the series: Ultron took the six Infinity Stones and Vision's body and ended all
 * life. Nothing lives in this universe: the cities of {@link CityChunkGenerator} ({@code ultron}) lie in ruins with the
 * bones of their people, no creature comes to be, and Ultron's Sentries fly over them in fleets of three
 * ({@link UltronSentryEntity#isWild()}), hunting whoever is left. Now and then Ultron's voice is heard.
 * At most {@link #SENTRIES} Sentries round a player; far from every player they are gone.
 */
public final class UltronWorld {
    /** How many Sentries fly round a player at most, how far they are counted, and where a new fleet comes. */
    static final int SENTRIES = 6;
    static final int COUNT_RANGE = 72;
    static final int COME_MIN = 36;
    static final int COME_MAX = 56;
    /** How many fleets come a minute at most, and Ultron's lines. */
    static final int FLEET_EVERY = 300;
    static final int LINES = 6;

    private UltronWorld() {
    }

    static void clear() {
    }

    /** Once a second for each player in Ultron's world. */
    static void tick(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (player.getRandom().nextInt(60) == 0) {
            player.displayClientMessage(Component.translatable("ultron.enhanced.voice",
                    Component.translatable("ultron.enhanced.voice." + player.getRandom().nextInt(LINES))).withStyle(ChatFormatting.RED), true);
        }
        if (player.tickCount % FLEET_EVERY != 0 && !(player.tickCount % 100 == 0 && player.getRandom().nextInt(3) == 0)) {
            return;
        }
        int near = level.getEntitiesOfClass(UltronSentryEntity.class, player.getBoundingBox().inflate(COUNT_RANGE), UltronSentryEntity::isWild).size();
        if (near > SENTRIES - 3) {
            return;
        }
        fleet(level, player);
    }

    /** A fleet of three Sentries comes in over the ruins, some way from the player. */
    static void fleet(ServerLevel level, ServerPlayer player) {
        double angle = player.getRandom().nextDouble() * Mth.TWO_PI;
        double distance = COME_MIN + player.getRandom().nextDouble() * (COME_MAX - COME_MIN);
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        if (!level.isLoaded(new BlockPos(x, 0, z))) {
            return;
        }
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int i = 0; i < 3; i++) {
            Vec3 at = new Vec3(x + 0.5 + (i - 1) * 3.0, ground + 10 + i, z + 0.5 + ((i & 1) * 2.0));
            UltronSentryEntity sentry = UltronSentryEntity.army(level, at, true);
            sentry.setWild(true);
            level.addFreshEntity(sentry);
        }
    }
}
