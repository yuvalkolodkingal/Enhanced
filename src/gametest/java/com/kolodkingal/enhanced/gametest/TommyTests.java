package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** Tommy Maximoff (Speed). */
public final class TommyTests {

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    @EnhancedTest(template = ARENA)
    public static void sprintingIsSuperSpeed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = f.player(10.5, FLOOR, 10.5, Power.TOMMY);
        PowerData data = Powers.get(tommy);
        check(data.toggle(Toggle.SUPER_SPEED), "super speed is on from the start");
        check(!tommy.mayFly(), "Tommy runs, he doesn't fly");
        double walking = tommy.getAttributeValue(Attributes.MOVEMENT_SPEED);

        tommy.setSprinting(true);
        PowerTicker.tick(tommy);
        check(data.is(Live.SPEEDING), "sprinting starts super speed");
        double running = tommy.getAttributeValue(Attributes.MOVEMENT_SPEED);
        check(running > walking * 3.0, "he runs at least three times as fast: " + walking + " -> " + running);
        check(tommy.getAttributeValue(Attributes.STEP_HEIGHT) >= 1.0, "he steps up whole blocks while running");
        tommy.fallDistance = 30.0F;
        check(!tommy.hurt(tommy.damageSources().fall(), 20.0F), "no fall damage while running");

        tommy.setSprinting(false);
        PowerTicker.tick(tommy);
        check(!data.is(Live.SPEEDING), "walking ends super speed");
        check(Math.abs(tommy.getAttributeValue(Attributes.MOVEMENT_SPEED) - walking) < 1.0E-6, "back to normal speed");

        press(tommy, 2);
        check(!data.toggle(Toggle.SUPER_SPEED), "G turns super speed off (3.3: R is his slow time)");
        tommy.setSprinting(true);
        PowerTicker.tick(tommy);
        check(!data.is(Live.SPEEDING), "with super speed off, sprinting is just sprinting");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void disarmBlitzSnatchesEveryWeaponNearby(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = f.player(10.5, FLOOR, 10.5, Power.TOMMY);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        zombie.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        Skeleton skeleton = TestKit.spawnWithNoFreeWill(helper, EntityType.SKELETON, new Vec3(6.5, FLOOR, 12.5));
        skeleton.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        skeleton.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        ServerPlayer swordsman = f.player(10.5, FLOOR, 4.5);
        swordsman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        ServerPlayer builder = f.player(10.5, FLOOR, 16.5);
        builder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 16));
        Pillager far = TestKit.spawnWithNoFreeWill(helper, EntityType.PILLAGER, new Vec3(10.5, FLOOR, 10.5 + TommyAbilities.BLITZ_RADIUS + 3));
        far.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CROSSBOW));

        tommy.setShiftKeyDown(true); // (3.3: Sneak + B)
        press(tommy, 4);
        tommy.setShiftKeyDown(false);

        check(zombie.getMainHandItem().isEmpty(), "the zombie's sword is gone");
        check(skeleton.getMainHandItem().isEmpty() && skeleton.getOffhandItem().isEmpty(), "the skeleton lost its bow and shield");
        check(swordsman.getMainHandItem().isEmpty(), "the player's sword is gone");
        check(builder.getMainHandItem().is(Items.STONE), "a player keeps what isn't a weapon");
        check(far.getMainHandItem().is(Items.CROSSBOW), "the pillager out of range keeps its crossbow");
        for (var item : new net.minecraft.world.item.Item[]{Items.IRON_SWORD, Items.BOW, Items.SHIELD, Items.DIAMOND_SWORD}) {
            check(tommy.getInventory().countItem(item) == 1, "Tommy now has the " + item);
        }
        check(!Powers.get(tommy).isReady(4, helper.getLevel().getGameTime()), "the blitz has a cooldown");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, timeoutTicks = 200)
    public static void whirlwindSpinsCreaturesAndFlingsThem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = f.player(10.5, FLOOR, 10.5, Power.TOMMY);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        Vec3 center = tommy.position();
        press(tommy, 3);
        check(Powers.get(tommy).is(Live.WHIRLWIND), "the whirlwind starts");
        double[] highest = {0};
        f.everyTick(TommyAbilities.WHIRLWIND_TICKS + 40, tick -> {
            PowerTicker.tick(tommy);
            highest[0] = Math.max(highest[0], zombie.getY() - center.y);
            if (tick == TommyAbilities.WHIRLWIND_TICKS / 2) {
                double ring = zombie.position().subtract(center).multiply(1, 0, 1).length();
                check(ring < 4.0, "the zombie is pulled in towards the ring, " + ring + " blocks out");
            }
            if (tick > TommyAbilities.WHIRLWIND_TICKS + 2) {
                check(!Powers.get(tommy).is(Live.WHIRLWIND), "the whirlwind ends");
                check(highest[0] > 1.5, "the zombie was lifted off the ground, up to " + highest[0]);
                double out = zombie.position().subtract(center).multiply(1, 0, 1).length();
                return zombie.onGround() && out > 5.0;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA)
    public static void vibratesThroughWallsButNotThickOrUnbreakableOnes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = f.player(4.5, FLOOR, 10.5, Power.TOMMY);
        look(tommy, EAST, 0.0F);
        // A 3-thick stone wall one block in front of him.
        for (int x = 5; x <= 7; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 10), Blocks.STONE);
            }
        }
        press(tommy, 4);
        Vec3 rel = helper.relativeVec(tommy.position());
        check(Math.abs(rel.x - 8.5) < 0.01 && Math.abs(rel.z - 10.5) < 0.01, "he is on the other side of the wall: " + rel);

        // Five blocks of stone is too thick.
        ServerPlayer second = f.player(4.5, FLOOR, 14.5, Power.TOMMY);
        look(second, EAST, 0.0F);
        for (int x = 5; x <= 9 + 1; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 14), Blocks.STONE);
            }
        }
        press(second, 4);
        check(helper.relativeVec(second.position()).x < 5.0, "a wall thicker than 4 blocks stops him");

        // Bedrock can't be vibrated through.
        ServerPlayer third = f.player(4.5, FLOOR, 6.5, Power.TOMMY);
        look(third, EAST, 0.0F);
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(new BlockPos(5, y, 6), Blocks.BEDROCK);
        }
        press(third, 4);
        check(helper.relativeVec(third.position()).x < 5.0, "bedrock stops him");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA)
    public static void molecularAccelerationShattersBlocksAndHurts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = f.player(6.5, FLOOR, 10.5, Power.TOMMY);
        BlockPos stone = new BlockPos(9, 2, 10);
        helper.setBlock(stone, Blocks.STONE);
        lookAt(tommy, helper.absoluteVec(Vec3.atCenterOf(stone)));
        press(tommy, 5);
        helper.assertBlockPresent(Blocks.AIR, stone);
        check(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, tommy.getBoundingBox().inflate(6),
                e -> e.getItem().is(Items.COBBLESTONE)).isEmpty(), "the stone breaks into cobblestone");

        BlockPos bedrock = new BlockPos(9, 2, 12);
        helper.setBlock(bedrock, Blocks.BEDROCK);
        Powers.get(tommy).startCooldown(5, 0, 0);
        lookAt(tommy, helper.absoluteVec(Vec3.atCenterOf(bedrock)));
        press(tommy, 5);
        helper.assertBlockPresent(Blocks.BEDROCK, bedrock);

        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(9.5, FLOOR, 8.5));
        float before = zombie.getHealth();
        lookAt(tommy, zombie);
        press(tommy, 5);
        check(zombie.getHealth() < before, "a creature is hurt by the vibration");
        helper.succeed();
    }

    private TommyTests() {
    }
}
