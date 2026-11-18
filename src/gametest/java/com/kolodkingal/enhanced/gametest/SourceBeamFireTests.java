package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.SolarJewelConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.EarnedLooks;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** (3.17) Source Code Vision's beam burns: creatures catch fire, the ground gets fire, nothing is deleted or lit against the rules. */
public final class SourceBeamFireTests {
    private static final int WALL_X = 10;

    private static ServerPlayer source(TestKit.Fixture f, boolean sourceLook) {
        ServerPlayer vision = f.player(3.5, FLOOR, 10.5, Power.VISION);
        if (sourceLook) {
            EarnedLooks.earn(vision, Variants.get(Power.VISION, "source_code"));
            Powers.setVariant(vision, Variants.get(Power.VISION, "source_code"));
        }
        return vision;
    }

    /** Holds R at the block {@code WALL_X} blocks east for a few ticks; returns the cell its struck face looks onto. */
    private static BlockPos fireAtWall(GameTestHelper helper, ServerPlayer vision, net.minecraft.world.level.block.Block wall) {
        helper.setBlock(new BlockPos(WALL_X, (int) FLOOR + 1, 10), wall);
        lookAt(vision, helper.absoluteVec(new Vec3(WALL_X + 0.5, FLOOR + 1.5, 10.5)));
        AbilityHandler.onKey(vision, 1, true);
        for (int i = 0; i < 8; i++) {
            vision.tickCount = 8 + i;
            PowerTicker.tick(vision);
        }
        AbilityHandler.onKey(vision, 1, false);
        return helper.absolutePos(new BlockPos(WALL_X - 1, (int) FLOOR + 1, 10));
    }

    @EnhancedTest(template = ARENA)
    public static void theBeamSetsCreaturesAlight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = source(f, true);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.setRemainingFireTicks(0);
        lookAt(vision, zombie);
        AbilityHandler.onKey(vision, 1, true);
        PowerTicker.tick(vision);
        check(zombie.isOnFire() || zombie.getRemainingFireTicks() > 0, "what the beam strikes burns");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void theBeamLeavesFireOnTheGroundAndDeletesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = source(f, true);
        BlockPos fire = fireAtWall(helper, vision, Blocks.OAK_PLANKS);
        check(helper.getLevel().getBlockState(fire).is(Blocks.FIRE), "fire stands on the air at the face it hit: "
                + helper.getLevel().getBlockState(fire));
        check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(WALL_X, (int) FLOOR + 1, 10))).is(Blocks.OAK_PLANKS),
                "the block it hit is still there");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "mob_griefing_off_source_beam")
    public static void withMobGriefingOffNothingIsLit(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = source(f, true);
        HulkTests.mobGriefing(f, false);
        BlockPos fire = fireAtWall(helper, vision, Blocks.OAK_PLANKS);
        check(helper.getLevel().getBlockState(fire).isAir(), "no fire with mobGriefing off: " + helper.getLevel().getBlockState(fire));
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        zombie.setRemainingFireTicks(0);
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void theServerCanSwitchTheGroundFireOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = source(f, true);
        f.config(SolarJewelConfig.BEAM_FIRE, false);
        BlockPos fire = fireAtWall(helper, vision, Blocks.OAK_PLANKS);
        check(helper.getLevel().getBlockState(fire).isAir(), "beamFire off: no fire on the ground");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void burnSecondsAreASetting(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = source(f, true);
        f.config(SolarJewelConfig.SOURCE_CODE_BURN_SECONDS, 0);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.setRemainingFireTicks(0);
        lookAt(vision, zombie);
        AbilityHandler.onKey(vision, 1, true);
        PowerTicker.tick(vision);
        check(zombie.getRemainingFireTicks() <= 0, "0 seconds: not set alight");
        f.config(SolarJewelConfig.SOURCE_CODE_BURN_SECONDS, 9);
        PowerTicker.tick(vision);
        check(zombie.getRemainingFireTicks() > 100, "9 seconds: burning for long: " + zombie.getRemainingFireTicks());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void withTheJewelOnTheSolarBeamBurnsTheGroundToo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SolarJewelConfig.ENABLED, true);
        ServerPlayer vision = source(f, true);
        Powers.get(vision).setIronCharge(1000);
        BlockPos fire = fireAtWall(helper, vision, Blocks.OAK_PLANKS);
        check(helper.getLevel().getBlockState(fire).is(Blocks.FIRE), "the Solar Beam leaves fire too");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void otherVisionLooksDoNotBurn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = source(f, false);
        BlockPos fire = fireAtWall(helper, vision, Blocks.OAK_PLANKS);
        check(helper.getLevel().getBlockState(fire).isAir(), "plain Vision lights nothing");
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.setRemainingFireTicks(0);
        lookAt(vision, zombie);
        AbilityHandler.onKey(vision, 1, true);
        PowerTicker.tick(vision);
        check(zombie.getRemainingFireTicks() <= 0, "nor does he set creatures alight");
        helper.succeed();
    }
}
