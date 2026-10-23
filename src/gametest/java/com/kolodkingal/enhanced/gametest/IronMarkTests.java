package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.Mark1Moves;
import com.kolodkingal.enhanced.ironman.Mark3Moves;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.gametest.IronCoreTests.data;
import static com.kolodkingal.enhanced.gametest.IronCoreTests.state;
import static com.kolodkingal.enhanced.gametest.IronCoreTests.step;
import static com.kolodkingal.enhanced.gametest.IronCoreTests.suited;
import static com.kolodkingal.enhanced.gametest.IronCoreTests.until;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The classic suits' moves (3.8, lane L3). Mark 1: the flamethrower burns, no block fire by default (with the option
 * and mobGriefing: fire); the boost and its crash (no harm to him, 30 durability); the arm rocket 8 (no block broken);
 * the brace (-60 %); the heavy punch 10. Mark 3: the repulsor hits a husk 19 blocks off the same tick for 6 (2 to the
 * one beside it); the double blast 10; the Unibeam 4 every 10 ticks; the flares (three husks lose him and cannot take
 * him back); four straight missiles of 3. Mark 5: the dash punch 8; the laser burst hits four husks round him. Energy
 * costs; wear, never breaking (offline), and the anvil's repair items. Batches {@code iron_marks} and {@code
 * iron_marks_cfg}.
 */
public final class IronMarkTests {
    private static final String BATCH = "iron_marks";
    private static final String CFG = "iron_marks_cfg";

    /** A husk with no armour and 200 health that stands where it is put (no AI unless {@code free}). */
    static Husk husk(GameTestHelper helper, double x, double z, boolean free) {
        Husk husk = free ? TestKit.spawn(helper, EntityType.HUSK, new Vec3(x, FLOOR, z))
                : TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    static float lost(LivingEntity e) {
        return e.getMaxHealth() - e.getHealth();
    }

    static void down(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        player.setShiftKeyDown(false);
    }

    static void up(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
    }

    static void press(ServerPlayer player, int ability, boolean sneak) {
        down(player, ability, sneak);
        up(player, ability);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mark 1

    /** The flamethrower: the husk before him is hurt and burns; no block catches fire by default. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theFlamethrowerBurnsButNotTheBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_1);
        Husk husk = husk(helper, 8.5, 5.5, false);
        TestKit.lookAt(tony, husk);
        down(tony, 1, false);
        check(Mark1Moves.flaming(tony), "it burns");
        step(helper, 12, () -> {
            check(lost(husk) >= 2.0F, "the husk is hurt: " + lost(husk));
            check(husk.isOnFire(), "and burning");
            check(noFire(helper), "no block caught fire");
            up(tony, 1);
            check(!Mark1Moves.flaming(tony), "let go: it stops");
            check(!data(tony).isReady(1, helper.getLevel().getGameTime()), "and waits");
            helper.succeed();
        }, tony);
    }

    /** With {@code mark1FlameSetsBlocks} and mobGriefing, the flame sets the floor before him alight. */
    @EnhancedTest(template = ARENA, batch = CFG, timeoutTicks = 100)
    public static void theFlameSetsBlocksOnlyWhenAllowed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(IronManConfig.MARK1_FLAME_SETS_BLOCKS, true);
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean was = rule.get();
        rule.set(true, helper.getLevel().getServer());
        f.onClose(() -> rule.set(was, helper.getLevel().getServer()));
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_1);
        TestKit.look(tony, TestKit.EAST, 45.0F);
        down(tony, 1, false);
        step(helper, 12, () -> {
            up(tony, 1);
            check(!noFire(helper), "fire on the floor before him");
            clearFire(helper);
            helper.succeed();
        }, tony);
    }

    private static boolean noFire(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 2, 0)),
                helper.absolutePos(new BlockPos(20, 6, 20)))) {
            if (helper.getLevel().getBlockState(pos).is(Blocks.FIRE)) {
                return false;
            }
        }
        return true;
    }

    private static void clearFire(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 2, 0)),
                helper.absolutePos(new BlockPos(20, 6, 20)))) {
            if (helper.getLevel().getBlockState(pos).is(Blocks.FIRE)) {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** The Rocket Boost: up, forward, a fall and a crash landing that hurts him nothing and costs the suit 30. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theBoostEndsInASoftCrash(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 3.5, 10.5, SuitKind.MARK_1);
        TestKit.look(tony, TestKit.EAST, 0.0F);
        float health = tony.getHealth();
        float energy = SuitEnergy.get(tony);
        press(tony, 2, false);
        check(Mark1Moves.boosting(tony) && data(tony).is(Live.IRON_DASHING), "the boots fire");
        assertNear(energy - SuitEnergy.get(tony), 30.0, 1.0E-3, "30 energy");
        ItemStack suit = tony.getItemBySlot(EquipmentSlot.CHEST);
        until(helper, 150, () -> !Mark1Moves.crashing(tony) && tony.onGround(), () -> {
            check(tony.getHealth() >= health - 1.0E-3F, "no harm to him: " + tony.getHealth());
            check(suit.getDamageValue() == 30, "the suit takes the crash: " + suit.getDamageValue());
            check(!data(tony).is(Live.IRON_DASHING), "the boost is over");
            helper.succeed();
        }, tony);
    }

    /** The Arm Rocket: 8 to the husk it strikes, a small boom, no block broken. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theArmRocketDoesEight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 3.5, 5.5, SuitKind.MARK_1);
        Husk husk = husk(helper, 11.5, 5.5, false);
        TestKit.lookAt(tony, husk);
        press(tony, 3, false);
        until(helper, 40, () -> lost(husk) > 0.0F, () -> step(helper, 3, () -> {
            assertNear(lost(husk), 8.0, 1.0E-3, "8");
            check(helper.getLevel().getBlockState(husk.blockPosition().below()).is(Blocks.SMOOTH_STONE),
                    "the floor is whole");
            helper.succeed();
        }, tony), tony);
    }

    /** The Brace: a blow loses 60 %; he does not move. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBraceCutsSixtyPercent(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_1);
        down(tony, 4, false);
        tony.doTick();
        check(state(tony).has(IronManState.BRACING), "braced");
        float before = tony.getHealth();
        tony.hurt(tony.damageSources().magic(), 10.0F);
        assertNear(before - tony.getHealth(), 4.0, 1.0E-3, "10 becomes 4");
        check(tony.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 1.0E-6, "rooted");
        up(tony, 4);
        tony.doTick();
        check(!state(tony).has(IronManState.BRACING) && tony.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.05,
                "let go: free");
        helper.succeed();
    }

    /** The Heavy Punch: 10 to the husk before him. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theHeavyPunchDoesTen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_1);
        Husk husk = husk(helper, 7.0, 5.5, false);
        TestKit.lookAt(tony, husk);
        press(tony, 5, false);
        assertNear(lost(husk), 10.0, 1.0E-3, "10");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mark 3

    /** The repulsor: the husk 19 blocks off is hit the same tick for 6, the one beside it splashed for 2; 4 energy. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRepulsorHitsAtOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 0.5, 10.5, SuitKind.MARK_3);
        Husk target = husk(helper, 19.5, 10.5, false);
        Husk beside = husk(helper, 19.5, 11.7, false);
        TestKit.lookAt(tony, target);
        float energy = SuitEnergy.get(tony);
        press(tony, 1, false);
        assertNear(lost(target), 6.0, 1.0E-3, "6, at once");
        assertNear(lost(beside), 2.0, 1.0E-3, "the boom's 2 beside it");
        assertNear(energy - SuitEnergy.get(tony), 4.0, 1.0E-3, "4 energy");
        press(tony, 1, false);
        assertNear(lost(target), 6.0, 1.0E-3, "a 10-tick gap before the next");
        helper.succeed();
    }

    /** Sneak + R: the Double-Hand Blast, 10. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theDoubleBlastDoesTen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 3.5, 5.5, SuitKind.MARK_3);
        Husk husk = husk(helper, 10.5, 5.5, false);
        TestKit.lookAt(tony, husk);
        press(tony, 1, true);
        assertNear(lost(husk), 10.0, 1.0E-3, "10");
        helper.succeed();
    }

    /** The Unibeam: 4 every 10 ticks to the husk in its path while V is held. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theUnibeamDoesFourEveryTenTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 3.5, 5.5, SuitKind.MARK_3);
        Husk husk = husk(helper, 9.5, 5.5, false);
        TestKit.lookAt(tony, husk.position().add(0, husk.getBbHeight() * 0.5, 0));
        down(tony, 3, false);
        check(Mark3Moves.unibeaming(tony) && data(tony).is(Live.BEAMING), "it burns");
        step(helper, 25, () -> {
            assertNear(lost(husk), 12.0, 1.0E-3, "three hits of 4 in 25 ticks");
            up(tony, 3);
            check(!Mark3Moves.unibeaming(tony) && !data(tony).is(Live.BEAMING), "let go: off");
            helper.succeed();
        }, tony);
    }

    /** The flares: three husks after him lose him, and cannot take him back for a while. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theFlaresShakeOffThreeHusks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 10.5, 10.5, SuitKind.MARK_3);
        Husk[] husks = {husk(helper, 4.5, 4.5, true), husk(helper, 16.5, 4.5, true), husk(helper, 10.5, 17.5, true)};
        for (Husk h : husks) {
            h.setTarget(tony);
            check(h.getTarget() == tony, "after him");
        }
        press(tony, 4, false);
        for (Husk h : husks) {
            check(h.getTarget() == null, "lost him");
            h.setTarget(tony);
            check(h.getTarget() == null, "and cannot pick him again");
        }
        helper.succeed();
    }

    /** N: four straight missiles of 3 each into the husk he aims at. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void fourMissilesOfThree(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 3.5, 5.5, SuitKind.MARK_3);
        Husk husk = husk(helper, 11.5, 5.5, false);
        TestKit.lookAt(tony, husk);
        press(tony, 5, false);
        check(helper.getLevel().getEntitiesOfClass(com.kolodkingal.enhanced.ironman.IronMissileEntity.class,
                new AABB(tony.blockPosition()).inflate(4)).size() == 4, "four missiles");
        until(helper, 40, () -> lost(husk) >= 12.0F - 1.0E-3F, () -> step(helper, 3, () -> {
            assertNear(lost(husk), 12.0, 1.0E-3, "4 x 3");
            helper.succeed();
        }, tony), tony);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mark 5

    /** B: the Dash Punch throws him forward and the husk in his way takes 8. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theDashPunchDoesEight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 3.5, 5.5, SuitKind.MARK_5);
        Husk husk = husk(helper, 7.5, 5.5, false);
        TestKit.lookAt(tony, husk);
        press(tony, 4, false);
        check(data(tony).is(Live.IRON_DASHING), "he dashes");
        until(helper, 10, () -> lost(husk) > 0.0F, () -> {
            assertNear(lost(husk), 8.0, 1.0E-3, "8");
            helper.succeed();
        }, tony);
    }

    /** N: the Laser Burst cuts the four husks round him for 6 each. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theLaserBurstHitsAllRound(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 10.5, 10.5, SuitKind.MARK_5);
        Husk[] husks = {husk(helper, 6.5, 10.5, false), husk(helper, 14.5, 10.5, false),
                husk(helper, 10.5, 6.5, false), husk(helper, 10.5, 14.5, false)};
        float energy = SuitEnergy.get(tony);
        press(tony, 5, false);
        for (Husk h : husks) {
            assertNear(lost(h), 6.0, 1.0E-3, "6 to each");
        }
        assertNear(energy - SuitEnergy.get(tony), 20.0, 1.0E-3, "20 energy");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Wear and repair

    /**
     * Blows wear the suit; it never breaks (at its last point it is offline: armour on, moves off); the anvil's items.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void suitsWearButNeverBreak(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = suited(f, 5.5, 5.5, SuitKind.MARK_3);
        Husk husk = husk(helper, 7.5, 5.5, false);
        ItemStack suit = tony.getItemBySlot(EquipmentSlot.CHEST);
        tony.hurt(tony.damageSources().mobAttack(husk), 8.0F);
        check(suit.getDamageValue() > 0, "a blow wears it");
        suit.setDamageValue(suit.getMaxDamage() - 2);
        tony.invulnerableTime = 0;
        tony.hurt(tony.damageSources().mobAttack(husk), 12.0F);
        check(!tony.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "never broken");
        check(suit.getDamageValue() == suit.getMaxDamage() - 1, "stopped at its last point");
        tony.doTick();
        check(IronSuits.broken(tony) && !IronSuits.powered(tony), "offline");
        assertNear(tony.getAttributeValue(Attributes.ARMOR), 16.0, 1.0E-6, "its armour still protects");
        check(suit.getItem().isValidRepairItem(suit, new ItemStack(Items.IRON_INGOT)), "iron mends a Mark 3");
        ItemStack m85 = new ItemStack(ModItems.IRON_MAN_MARK_85.get());
        check(m85.getItem().isValidRepairItem(m85, new ItemStack(Items.NETHERITE_SCRAP))
                && !m85.getItem().isValidRepairItem(m85, new ItemStack(Items.IRON_INGOT)), "netherite scrap a Mark 85");
        ItemStack m50 = new ItemStack(ModItems.IRON_MAN_MARK_50.get());
        check(m50.getItem().isValidRepairItem(m50, new ItemStack(Items.DIAMOND)), "diamonds a Mark 50");
        ItemStack buster = new ItemStack(ModItems.IRON_MAN_HULKBUSTER.get());
        check(buster.getItem().isValidRepairItem(buster, new ItemStack(Items.IRON_BLOCK)), "iron blocks a Hulkbuster");
        check(suit.getMaxDamage() == 1100 && m85.getMaxDamage() == 2035, "the durabilities are the plan's");
        helper.succeed();
    }

    /** Every suit's armour on the chest is the server's ({@code [mark_*] armor}): the whole set in one item. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theClassicSuitsArmour(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        double[][] expected = {{18, 3}, {16, 2}, {12, 1}};
        SuitKind[] kinds = {SuitKind.MARK_1, SuitKind.MARK_3, SuitKind.MARK_5};
        for (int i = 0; i < kinds.length; i++) {
            ServerPlayer tony = suited(f, 3.5 + 5 * i, 5.5, kinds[i]);
            assertNear(tony.getAttributeValue(Attributes.ARMOR), expected[i][0], 1.0E-6, kinds[i] + " armour");
            assertNear(tony.getAttributeValue(Attributes.ARMOR_TOUGHNESS), expected[i][1], 1.0E-6,
                    kinds[i] + " toughness");
        }
        helper.succeed();
    }

    private IronMarkTests() {
    }
}
