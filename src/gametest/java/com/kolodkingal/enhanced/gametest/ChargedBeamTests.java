package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.ChargedBeamConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.EarnedLooks;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** (3.17) Vision's Charged Beam on B: charge by holding, damage and reach grow, a tap is weak, a cooldown, the Source Code look burns and explodes. */
public final class ChargedBeamTests {
    /** Ticks a test holds the key for a full charge (the charge time is set to 5 ticks). */
    private static final int HOLD = 8;

    private static ServerPlayer vision(TestKit.Fixture f, boolean source) {
        f.config(ChargedBeamConfig.CHARGE_TICKS, 5);
        f.config(ChargedBeamConfig.TAP_TICKS, 2);
        ServerPlayer vision = f.player(3.5, TestKit.FLOOR, 10.5, Power.VISION);
        if (source) {
            EarnedLooks.earn(vision, Variants.get(Power.VISION, "source_code"));
            Powers.setVariant(vision, Variants.get(Power.VISION, "source_code"));
        }
        return vision;
    }

    @EnhancedTest(template = ARENA)
    public static void aTapIsWeakAndShort(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = vision(f, false);
        Zombie near = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 10.5));
        Zombie far = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(18.5, FLOOR, 10.5));
        TestKit.look(vision, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(vision, 4, true);
        AbilityHandler.onKey(vision, 4, false);
        float lost = near.getMaxHealth() - near.getHealth();
        check(lost > 0 && lost <= ChargedBeamConfig.MIN_DAMAGE.get() + 0.01, "a tap hurts a little: " + lost);
        check(far.getHealth() == far.getMaxHealth(), "and not as far as a charge reaches");
        check(Powers.get(vision).is(Live.BEAM_FIRING), "everyone sees the shot");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void aChargeHurtsMoreAndReachesFarther(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = vision(f, false);
        Zombie near = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 10.5));
        Zombie far = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(18.5, FLOOR, 10.5));
        TestKit.look(vision, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(vision, 4, true);
        PowerTicker.tick(vision);
        check(Powers.get(vision).is(Live.BEAM_CHARGING), "held: charging");
        check(near.getHealth() == near.getMaxHealth(), "nothing is struck while it charges");
        helper.runAfterDelay(HOLD, () -> {
            AbilityHandler.onKey(vision, 4, false);
            check(!near.isAlive() || near.getHealth() <= 0.5F, "a full charge kills the zombie in its path");
            check(far.getMaxHealth() - far.getHealth() >= 15.0F || !far.isAlive(), "and strikes the one 15 blocks away: " + far.getHealth());
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void thenItNeedsToRecharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = vision(f, false);
        lookAt(vision, helper.absoluteVec(new Vec3(12.5, FLOOR + 1.5, 10.5)));
        AbilityHandler.onKey(vision, 4, true);
        AbilityHandler.onKey(vision, 4, false);
        check(Powers.get(vision).cooldownUntil(Powers.get(vision).slot(com.kolodkingal.enhanced.custom.HeroAbility.VISION_CHARGED_BEAM))
                > helper.getLevel().getGameTime(), "a cooldown runs");
        AbilityHandler.onKey(vision, 4, true);
        check(!Powers.get(vision).is(Live.BEAM_CHARGING), "no new charge until it ends");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void notWhilePhased(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = vision(f, false);
        Powers.get(vision).setVisionPhased(true);
        AbilityHandler.onKey(vision, 4, true);
        check(!Powers.get(vision).is(Live.BEAM_CHARGING), "a phased Vision cannot charge a beam");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void sourceCodeBurnsAndExplodesWhereItStrikes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = vision(f, true);
        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                for (int dz = 0; dz < 3; dz++) {
                    helper.setBlock(new BlockPos(14 + dx, (int) FLOOR + dy, 9 + dz), Blocks.DIRT);
                }
            }
        }
        Zombie by = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 12.5));
        TestKit.look(vision, TestKit.EAST, 0.0F);
        float health = vision.getHealth();
        AbilityHandler.onKey(vision, 4, true);
        helper.runAfterDelay(HOLD, () -> {
            AbilityHandler.onKey(vision, 4, false);
            BlockPos hitFace = helper.absolutePos(new BlockPos(14, (int) FLOOR + 1, 10));
            check(helper.getLevel().getBlockState(hitFace).isAir(), "the block it struck is blown away: "
                    + helper.getLevel().getBlockState(hitFace));
            int broken = 0;
            for (int dx = 0; dx < 3; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    for (int dz = 0; dz < 3; dz++) {
                        if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(14 + dx, (int) FLOOR + dy, 9 + dz))).isAir()) {
                            broken++;
                        }
                    }
                }
            }
            check(broken >= 4, "a crater, not one block: " + broken);
            check(by.getHealth() < by.getMaxHealth() || by.isOnFire(), "the blast hurts what is near");
            check(vision.getHealth() >= health, "never the shooter");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "mob_griefing_off")
    public static void withMobGriefingOffNoBlockBreaks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = vision(f, true);
        HulkTests.mobGriefing(f, false);
        helper.setBlock(new BlockPos(14, (int) FLOOR + 1, 10), Blocks.DIRT);
        TestKit.look(vision, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(vision, 4, true);
        helper.runAfterDelay(HOLD, () -> {
            AbilityHandler.onKey(vision, 4, false);
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(14, (int) FLOOR + 1, 10))).is(Blocks.DIRT),
                    "nothing is broken with mobGriefing off");
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(13, (int) FLOOR + 1, 10))).isAir(), "and nothing is lit");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void otherLooksDoNotExplode(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = vision(f, false);
        helper.setBlock(new BlockPos(14, (int) FLOOR + 1, 10), Blocks.DIRT);
        TestKit.look(vision, TestKit.EAST, 0.0F);
        AbilityHandler.onKey(vision, 4, true);
        helper.runAfterDelay(HOLD, () -> {
            AbilityHandler.onKey(vision, 4, false);
            check(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(14, (int) FLOOR + 1, 10))).is(Blocks.DIRT),
                    "plain Vision breaks no block");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA)
    public static void anOldSaveOfADenseVisionLoadsAsANormalOne(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putString("power", Power.VISION.getSerializedName());
        tag.putBoolean("dense", true);
        PowerData data = PowerData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        check(data.power() == Power.VISION, "it loads");
        check(!PowerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow().toString().contains("dense:1b"),
                "and is saved without the density");
        helper.succeed();
    }
}
