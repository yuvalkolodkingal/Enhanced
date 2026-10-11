package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.ultron.LiftHook;
import com.kolodkingal.enhanced.ultron.LiftHooks;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import com.kolodkingal.enhanced.ultron.SokoviaLift;
import com.kolodkingal.enhanced.ultron.SokoviaRecovery;
import com.kolodkingal.enhanced.ultron.SokoviaRelocations;
import com.kolodkingal.enhanced.ultron.UltronFight;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the Sokovia tests share: the pad (a 43 x 42 x 43 field of natural ground with open sky over it, so the island
 * fits above it), a fixture that shrinks the island to fit and installs the real lift, a client for the fake players,
 * and the small things every lift test needs (a hash of the ground, a count of what is in the sky, the journal files).
 */
final class SokoviaTestSupport {
    static final String PAD = "sokovia_pad";
    /**
     * Where a player stands on the pad: the grass is the layer at structure y = 4, which is relative y = 5 (a test's
     * own coordinates start one block above the structure's).
     */
    static final double GROUND = 6.0;
    static final int SURFACE = 5;
    static final double MIDDLE_X = 21.5;
    static final double MIDDLE_Z = 21.5;
    static final int WIDTH = 43;
    static final int HEIGHT = 42;
    /** How far the island rises in the tests. */
    static final int LIFT = 20;

    /**
     * A boss fixture whose island fits the pad (nine blocks across the rim, {@link #LIFT} up, a short cone and thin
     * skin), with the real lift installed; when the test ends, the lift, the records and the journals it made are gone.
     */
    static TestKit.Fixture pad(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(UltronBossConfig.ISLAND_RADIUS, 9);
        f.config(UltronBossConfig.LIFT_HEIGHT, LIFT);
        f.config(UltronBossConfig.CONE_DEPTH, 6);
        f.config(UltronBossConfig.SKIN_DEPTH, 4);
        f.config(UltronBossConfig.COVER_HEIGHT, 8);
        f.config(UltronBossConfig.LIFT_TICKS, 40);
        f.config(UltronBossConfig.EDITS_PER_TICK, 1500);
        f.config(UltronBossConfig.SENTRIES_AT_START, 2);
        f.config(UltronBossConfig.LIFT_MODE, UltronBossConfig.LiftMode.SKY_ISLAND);
        f.config(UltronBossConfig.LIFT_ENABLED, true);
        LiftHook old = LiftHooks.install(new SokoviaLift());
        MinecraftServer server = helper.getLevel().getServer();
        SokoviaRelocations records = SokoviaRelocations.of(server);
        f.onClose(() -> {
            LiftHooks.install(old);
            SokoviaLift.clear();
            SokoviaRecovery.clear();
            SokoviaRelocations.store(server, records);
            deleteJournals(server);
        });
        return f;
    }

    static void deleteJournals(MinecraftServer server) {
        Path directory = SokoviaJournal.directory(server);
        if (Files.isDirectory(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** The journal files (finished ones: not the temporary or the set-aside) in the world now. */
    static List<Path> journals(ServerLevel level) {
        Path directory = SokoviaJournal.directory(level.getServer());
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".nbt")).toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    /**
     * A number that is the same for the same blocks from the bottom of the pad to relative y = 10 and different
     * otherwise.
     */
    static long groundHash(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long hash = 1125899906842597L;
        for (int y = 0; y <= 10; y++) {
            for (int z = 0; z < WIDTH; z++) {
                for (int x = 0; x < WIDTH; x++) {
                    hash = hash * 31 + Block.getId(level.getBlockState(abs(helper, x, y, z)));
                }
            }
        }
        return hash;
    }

    /**
     * How many blocks stand in the sky of the pad (relative y = 11 and up: the island's lowest block is 12 or higher).
     */
    static int blocksInTheSky(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int count = 0;
        for (int y = 11; y <= HEIGHT; y++) {
            for (int z = 0; z < WIDTH; z++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (!level.getBlockState(abs(helper, x, y, z)).isAir()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * What a connected client does while the lift has its player: it moves them by the velocity the server last gave
     * them.
     */
    static void client(ServerPlayer... players) {
        for (ServerPlayer player : players) {
            if (player.isAlive()) {
                player.setPos(player.position().add(player.getDeltaMovement()));
            }
        }
    }

    /** Where an entity is, in the test's own coordinates. */
    static Vec3 rel(GameTestHelper helper, Entity entity) {
        return helper.relativeVec(entity.position());
    }

    /**
     * Starts the fight as the ritual does (not an operator's start): the starter holds the Ultron Program and looks at
     * the ground at their feet, so the Program is used up and the lift's own refusals are asked.
     */
    static UltronFight ritual(TestKit.Fixture f, ServerPlayer starter) {
        starter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        TestKit.look(starter, 0.0F, 90.0F);
        check(BossFights.start(BossKind.ULTRON, starter, false), "the ritual starts the fight");
        return UltronTestSupport.fightOf(f);
    }

    /** Whether the lift of the fight is in this step. */
    static boolean in(UltronFight fight, SokoviaLift.Step step) {
        SokoviaLift.Lift lift = SokoviaLift.of(fight);
        return lift != null && lift.step() == step;
    }

    /** A test that goes step by step: each step runs every tick until it answers true, then the next begins. */
    static final class Script {
        private final List<String> names = new ArrayList<>();
        private final List<BooleanSupplier> steps = new ArrayList<>();
        private int current;

        Script step(String name, BooleanSupplier body) {
            names.add(name);
            steps.add(body);
            return this;
        }

        /** An action that happens once, in its own step. */
        Script then(String name, Runnable action) {
            return step(name, () -> {
                action.run();
                return true;
            });
        }

        /** Whether every step is done; runs the current one (and the ones after it that are done at once). */
        boolean tick(int tick, int limit) {
            try {
                while (current < steps.size() && steps.get(current).getAsBoolean()) {
                    current++;
                }
            } catch (GameTestAssertException e) {
                throw new GameTestAssertException("step '" + names.get(current) + "' at tick " + tick + ": " + e.getMessage());
            }
            if (current >= steps.size()) {
                return true;
            }
            if (tick >= limit) {
                throw new GameTestAssertException("stuck at step '" + names.get(current) + "' after " + limit + " ticks");
            }
            return false;
        }

        void run(TestKit.Fixture f, int limit) {
            f.everyTick(limit, tick -> tick(tick, limit));
        }
    }

    private SokoviaTestSupport() {
    }
}
