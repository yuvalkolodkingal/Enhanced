package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.item.LokisScepterItem;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** Vision: the Mind Stone in a synthetic body. */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class VisionTests {

    @GameTest(template = ARENA)
    public static void synthezoidCoreAndMindStoneMakeVision(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SYNTHEZOID_CORE.get()));

        LokisScepterItem.touchMindStone(player, InteractionHand.MAIN_HAND);
        check(Powers.is(player, Power.VISION), "the Mind Stone moves into the synthezoid body: Vision");
        check(player.getOffhandItem().isEmpty(), "the core is used up");
        check(player.isAlive(), "and nobody explodes");

        PowerTicker.tick(player);
        assertNear(player.getAttributeValue(Attributes.MAX_HEALTH), 30.0, 1.0E-6, "a synthetic body: 15 hearts");
        assertNear(player.getAttributeValue(Attributes.ARMOR), 8.0, 1.0E-6, "with vibranium skin");
        check(player.mayFly(), "Vision flies");

        // Touching the stone again is safe for Vision.
        LokisScepterItem.touchMindStone(player, InteractionHand.MAIN_HAND);
        check(player.isAlive() && Powers.is(player, Power.VISION), "the Mind Stone recognises Vision");

        // A synthetic body never gets hungry or runs out of air.
        player.getFoodData().setFoodLevel(3);
        player.setAirSupply(0);
        PowerTicker.tick(player);
        check(player.getFoodData().getFoodLevel() == 20, "Vision doesn't need food");
        check(player.getAirSupply() == player.getMaxAirSupply(), "or air");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mindStoneBeamHurts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(3.5, FLOOR, 10.5, Power.VISION);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(15.5, FLOOR, 10.5));
        lookAt(vision, zombie);
        float health = zombie.getHealth();
        AbilityHandler.onKey(vision, 1, true);
        check(Powers.get(vision).beamActive(), "R fires the Mind Stone beam");
        PowerTicker.tick(vision);
        check(zombie.getHealth() < health, "the beam hurts what it hits");
        AbilityHandler.onKey(vision, 1, false);
        check(!Powers.get(vision).beamActive(), "releasing R stops it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void phasingPassesThroughWallsAndHarm(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(10.5, FLOOR, 10.5, Power.VISION);
        PowerData data = Powers.get(vision);

        AbilityHandler.onKey(vision, 3, true);
        check(data.visionPhased(), "V makes Vision intangible");
        vision.doTick();
        check(vision.noPhysics, "a phased Vision has no collisions, so he walks through walls");
        check(vision.getAbilities().flying, "and floats instead of sinking through the floor");

        float health = vision.getHealth();
        vision.hurt(helper.getLevel().damageSources().generic(), 10.0F);
        assertNear(vision.getHealth(), health, 1.0E-6, "nothing can hurt a phased Vision");

        // Inside a wall he can't become solid again.
        BlockPos inside = BlockPos.containing(vision.position());
        helper.getLevel().setBlockAndUpdate(inside, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(inside.above(), Blocks.STONE.defaultBlockState());
        AbilityHandler.onKey(vision, 3, true);
        check(data.visionPhased(), "Vision can't solidify inside stone");
        vision.doTick();
        check(vision.isAlive() && vision.getHealth() == health, "and doesn't suffocate in it");

        helper.getLevel().setBlockAndUpdate(inside, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(inside.above(), Blocks.AIR.defaultBlockState());
        AbilityHandler.onKey(vision, 3, true);
        check(!data.visionPhased(), "out in the open he becomes solid again");
        vision.doTick();
        check(!vision.noPhysics, "with collisions back on");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void densityMakesHimHeavyAndHard(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(10.5, FLOOR, 10.5, Power.VISION);
        PowerTicker.tick(vision);
        double armor = vision.getAttributeValue(Attributes.ARMOR);
        double damage = vision.getAttributeValue(Attributes.ATTACK_DAMAGE);

        AbilityHandler.onKey(vision, 4, true);
        PowerTicker.tick(vision);
        check(Powers.get(vision).dense(), "B makes Vision dense");
        assertNear(vision.getAttributeValue(Attributes.ARMOR), armor + 12.0, 1.0E-6, "dense: much harder");
        assertNear(vision.getAttributeValue(Attributes.ATTACK_DAMAGE), damage + 6.0, 1.0E-6, "and his punches land like a falling building");
        assertNear(vision.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 1.0, 1.0E-6, "he can't be pushed");
        check(!vision.mayFly(), "too heavy to fly");

        AbilityHandler.onKey(vision, 4, true);
        PowerTicker.tick(vision);
        assertNear(vision.getAttributeValue(Attributes.ARMOR), armor, 1.0E-6, "normal density again");
        check(vision.mayFly(), "and he can fly again");

        // Phasing while dense simply switches from one to the other.
        AbilityHandler.onKey(vision, 4, true);
        AbilityHandler.onKey(vision, 3, true);
        check(Powers.get(vision).visionPhased() && !Powers.get(vision).dense(), "phasing ends the density");
        // But he can't turn dense while phased.
        AbilityHandler.onKey(vision, 4, true);
        check(!Powers.get(vision).dense(), "can't be dense while phased");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void humanDisguiseToggles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(10.5, FLOOR, 10.5, Power.VISION);
        AbilityHandler.onKey(vision, 5, true);
        check(Powers.get(vision).disguise(), "N puts on the WandaVision human disguise");
        AbilityHandler.onKey(vision, 5, true);
        check(!Powers.get(vision).disguise(), "and takes it off");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreDoesNotBondToOtherHeroes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(10.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        magneto.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SYNTHEZOID_CORE.get()));
        LokisScepterItem.touchMindStone(magneto, InteractionHand.MAIN_HAND);
        check(Powers.is(magneto, Power.MAGNETO), "powers never stack");
        check(magneto.getOffhandItem().is(ModItems.SYNTHEZOID_CORE.get()), "the core is not used up");
        check(magneto.isAlive(), "holding the core protects him from the stone");
        helper.succeed();
    }
}
