package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.ability.WaterRun;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The speedsters on water (3.6.1: Tommy runs as his uncle does). A pool one block deep is built on the arena's floor
 * (stone round it, so the water stays); the speedster stands just over it and his client's movement is simulated as in
 * {@code SpeedsterSurfaceTests}: each tick his run along his facing and gravity, the water's hold ({@link WaterRun},
 * what his client does before it moves him), then the move with the game's collision. Each test has a batch of its own.
 */
public final class SpeedsterWaterTests {
    /** The run speed along +x (blocks per tick). */
    private static final double SPEED = 0.5;
    /** The pool: water from x 2 to 18 at z 8..12, layer 2 (feet over it at y 3). */
    private static final int Y = 2;

    private static void pool(GameTestHelper helper) {
        for (int x = 1; x <= 19; x++) {
            for (int z = 7; z <= 13; z++) {
                boolean rim = x == 1 || x == 19 || z == 7 || z == 13;
                helper.setBlock(new BlockPos(x, Y, z), rim ? Blocks.STONE : Blocks.WATER);
                helper.setBlock(new BlockPos(x, Y + 1, z), Blocks.AIR);
            }
        }
    }

    /**
     * {@code ticks} ticks of his client: the hero's own tick (as his connection's), then his run and gravity, the
     * water's hold, the move. Returns the lowest his feet went (relative y).
     */
    private static double run(GameTestHelper helper, ServerPlayer player, int ticks) {
        double lowest = Double.MAX_VALUE;
        for (int t = 0; t < ticks; t++) {
            PowerTicker.tick(player);
            Vec3 last = player.getDeltaMovement();
            player.setDeltaMovement(new Vec3(SPEED, (last.y - 0.08) * 0.98, 0.0));
            WaterRun.hold(player);
            player.move(MoverType.SELF, player.getDeltaMovement());
            lowest = Math.min(lowest, helper.relativeVec(player.position()).y);
        }
        return lowest;
    }

    private static ServerPlayer onThePool(TestKit.Fixture f, GameTestHelper helper, Power power) {
        pool(helper);
        ServerPlayer player = f.player(2.5, Y + 1, 10.5, power);
        player.setYRot(-90.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(true);
        return player;
    }

    /**
     * Tommy at super speed with his Wall &amp; Water Run on (from the start) runs across the pool and never sinks; with
     * it switched off (Sneak + R), or not running, he sinks.
     */
    @EnhancedTest(template = ARENA, batch = "speedster_water")
    public static void tommyRunsOnWaterWhileSpeeding(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = onThePool(f, helper, Power.TOMMY);
        PowerData data = Powers.get(tommy);
        data.setToggle(Toggle.SUPER_SPEED, true);
        PowerTicker.tick(tommy);
        check(TommyAbilities.isRunning(tommy) && data.is(Live.SPEEDING), "Tommy runs at super speed");
        check(WaterRun.mayRun(tommy), "his Wall & Water Run is on from the start: he may run on water");
        double lowest = run(helper, tommy, 20);
        Vec3 at = helper.relativeVec(tommy.position());
        check(lowest >= Y + 1 - 1.0E-6, "he never sinks: lowest " + lowest);
        check(at.x > 9.0 && at.x < 18.0, "he ran across the water: " + at);
        check(tommy.fallDistance == 0.0F, "and has no fall to take");

        tommy.setShiftKeyDown(true);
        AbilityHandler.onKey(tommy, 1, true);
        tommy.setShiftKeyDown(false);
        check(data.toggle(Toggle.TOMMY_WALL_RUN_OFF) && !WaterRun.mayRun(tommy), "Sneak + R: off, for water too");
        tommy.setDeltaMovement(Vec3.ZERO);
        double sank = run(helper, tommy, 6);
        check(sank < Y + 1 - 0.3, "then he sinks: " + sank);
        helper.succeed();
    }

    /** Walking (not at super speed) Tommy sinks; running again on the water's top, the water holds him from then on. */
    @EnhancedTest(template = ARENA, batch = "speedster_water_walk")
    public static void tommyStopsSinkingOnlyWhileHeRuns(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = onThePool(f, helper, Power.TOMMY);
        PowerData data = Powers.get(tommy);
        data.setToggle(Toggle.SUPER_SPEED, false);
        PowerTicker.tick(tommy);
        check(!TommyAbilities.isRunning(tommy) && !WaterRun.mayRun(tommy), "Super Speed off: no run");
        check(run(helper, tommy, 3) < Y + 1, "walking, he sinks into it");
        Vec3 top = helper.absoluteVec(new Vec3(6.5, Y + 1, 10.5));
        tommy.teleportTo(top.x, top.y, top.z);
        tommy.setDeltaMovement(Vec3.ZERO);
        data.setToggle(Toggle.SUPER_SPEED, true);
        check(run(helper, tommy, 10) >= Y + 1 - 1.0E-6, "at super speed on its top again, he stops sinking");
        check(tommy.getAttributeValue(NeoForgeMod.SWIM_SPEED) > 1.0 + 1.0E-6,
                "and with his Super Speed on he swims faster, as his uncle does");
        helper.succeed();
    }

    /** Quicksilver's Wall &amp; Water Run is the same rule: on, he runs on water; off (B), he sinks. */
    @EnhancedTest(template = ARENA, batch = "speedster_water_qs")
    public static void quicksilverRunsOnWaterByTheSameRule(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer peter = onThePool(f, helper, Power.QUICKSILVER);
        PowerData data = Powers.get(peter);
        data.setSuperSpeed(true);
        if (!data.wallRun()) {
            AbilityHandler.onKey(peter, 4, true);
        }
        PowerTicker.tick(peter);
        check(QuicksilverAbilities.isRunning(peter) && WaterRun.mayRun(peter), "Quicksilver runs, Wall & Water Run on");
        check(run(helper, peter, 12) >= Y + 1 - 1.0E-6, "he never sinks");
        AbilityHandler.onKey(peter, 4, true);
        check(!data.wallRun() && !WaterRun.mayRun(peter), "B: off");
        peter.setDeltaMovement(Vec3.ZERO);
        check(run(helper, peter, 6) < Y + 1 - 0.3, "then he sinks");
        helper.succeed();
    }

    private SpeedsterWaterTests() {
    }
}
