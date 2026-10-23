package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronMissileEntity;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.MissileGuidance;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.nano.EnergyShield;
import com.kolodkingal.enhanced.ironman.nano.HomingMissiles;
import com.kolodkingal.enhanced.ironman.nano.NanoDroneEntity;
import com.kolodkingal.enhanced.ironman.nano.NanoDrones;
import com.kolodkingal.enhanced.ironman.nano.NanoHousings;
import com.kolodkingal.enhanced.ironman.nano.WingFormation;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;

/**
 * The nanotech suits (3.8, lane L4, PLAN 6.1-6.3, 14): H deploys the selected housing and Sneak + H switches it; the
 * suit's armour while it is out and not after; the wear goes to the housing (never the Nano Arc Reactor), a stowed
 * housing rebuilds, the reactor taken off retracts it; the Mark 85's energy blades (10 / 10 / 14 and the empty hand's
 * 12), its chest laser through three creatures, its giant energy shield (a creeper's blast from the front stopped
 * whole, from behind not), the Lightning Refocuser's chain (10 / 8 / 6 / 5 / 4; charged by a bolt: free and × 1.5), the
 * homing missiles (six locks, a moving target hit, a new target when one dies, their ten seconds), the drones (they
 * shoot a creature, never a player without PvP, and go at 400 ticks; since 3.8.3 their wing blades' formation); the
 * Mark 50's nano blade (9), nano shield (−80 %) and its cannon released early (refused, half the energy back). Batch
 * {@code nano}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class NanoTechTests {
    private static final String BATCH = "nano";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Iron Man at full health with his energy full (one tick). */
    static ServerPlayer ironMan(TestKit.Fixture f, double x, double z) {
        ServerPlayer tony = f.player(x, FLOOR, z, Power.IRON_MAN);
        f.onClose(() -> HeroLifecycle.onLogout(tony));
        tony.doTick();
        tony.setHealth(tony.getMaxHealth());
        return tony;
    }

    /** The Nano Arc Reactor on his chest and the housings he carries (in his pack, not his hands). */
    static void housings(ServerPlayer tony, SuitKind... kinds) {
        tony.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NANO_ARC_REACTOR.get()));
        for (int i = 0; i < kinds.length; i++) {
            tony.getInventory().setItem(9 + i, new ItemStack(IronSuits.item(kinds[i])));
        }
    }

    /** A nanotech suit out at once (no suit-up: its state set as the end of one leaves it). */
    static ServerPlayer deployed(TestKit.Fixture f, double x, double z, SuitKind kind) {
        ServerPlayer tony = ironMan(f, x, z);
        housings(tony, kind);
        IronSuits.setState(tony, IronSuits.state(tony).withNanoSuit(kind).with(IronManState.NANO_OUT, true)
                .withSuit(kind));
        check(IronSuits.worn(tony) == kind && IronSuits.powered(tony), kind + " out and working");
        return tony;
    }

    /** A husk with no armour and 200 health that stands where it is put (no blow moves it). */
    static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    static float lost(LivingEntity e) {
        return e.getMaxHealth() - e.getHealth();
    }

    static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    /** A key pressed and let go (with Sneak or not). */
    static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** A key held down (with Sneak or not). */
    static void hold(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
    }

    static void release(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** Every cooldown over (a new move may start at once). */
    static void ready(ServerPlayer player) {
        PowerData data = data(player);
        long now = player.level().getGameTime();
        for (int slot = 1; slot <= PowerData.ABILITIES; slot++) {
            data.startCooldown(slot, now, 0);
        }
    }

    /**
     * Ticks the players once a server tick until {@code done} (at most {@code max} ticks), then runs {@code then}; one
     * entry at a time in the test's schedule (as {@code WolverineTests.until}).
     */
    static void until(GameTestHelper helper, int max, BooleanSupplier done, Runnable then, ServerPlayer... players) {
        untilStep(helper, 1, max, done, then, players);
    }

    private static void untilStep(GameTestHelper helper, int tick, int max, BooleanSupplier done, Runnable then,
            ServerPlayer... players) {
        helper.runAfterDelay(1, () -> {
            for (ServerPlayer p : players) {
                p.doTick();
            }
            if (done.getAsBoolean()) {
                then.run();
            } else if (tick >= max) {
                throw new GameTestAssertException("not done within " + max + " ticks");
            } else {
                untilStep(helper, tick + 1, max, done, then, players);
            }
        });
    }

    /** Ticks the players once a server tick for {@code n} ticks, then runs {@code then}. */
    static void step(GameTestHelper helper, int n, Runnable then, ServerPlayer... players) {
        int[] count = {0};
        until(helper, n + 1, () -> ++count[0] > n, then, players);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The framework: H, the armour, the housings

    /** H deploys the selected housing (the Mark 85), Sneak + H switches to the Mark 50 he carries. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void hDeploysTheSelectedSuitAndSneakSwitches(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = ironMan(f, 5.5, 5.5);
        housings(tony, SuitKind.MARK_85, SuitKind.MARK_50);
        tony.doTick();
        IronSuits.onSuitKey(tony, 1);
        check(IronSuits.state(tony).phaseKind() == SuitKind.MARK_85, "the Mark 85 flows out");
        until(helper, 40, () -> !IronSuits.state(tony).inPhase(), () -> {
            check(IronSuits.worn(tony) == SuitKind.MARK_85 && IronSuits.powered(tony), "the Mark 85 is out");
            step(helper, 41, () -> {
                IronSuits.onSuitKey(tony, -1);
                check(SuitKind.of(IronSuits.state(tony).nanoSuit()) == SuitKind.MARK_50, "Sneak + H: the Mark 50");
                until(helper, 40, () -> !IronSuits.state(tony).inPhase(), () -> {
                    check(IronSuits.worn(tony) == SuitKind.MARK_50, "the Mark 50 is out now");
                    helper.succeed();
                }, tony);
            }, tony);
        }, tony);
    }

    /** The suit's armour while it is out (the Mark 85's 20), none after it went back in. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theArmourIsOnWhileOutAndOffAfter(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 5.5, 5.5, SuitKind.MARK_85);
        step(helper, 2, () -> {
            assertNear(tony.getAttributeValue(Attributes.ARMOR), 20.0, 0.01, "the Mark 85's armour");
            IronSuits.setState(tony, IronSuits.state(tony).with(IronManState.NANO_OUT, false).withSuit(null));
            step(helper, 2, () -> {
                assertNear(tony.getAttributeValue(Attributes.ARMOR), 0.0, 0.01, "no armour once it is in");
                helper.succeed();
            }, tony);
        }, tony);
    }

    /** A blow wears the deployed suit's housing (a quarter of it), never the Nano Arc Reactor. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void damageWearsTheHousingNotTheReactor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 5.5, 5.5, SuitKind.MARK_85);
        Husk husk = husk(helper, 7.5, 5.5);
        ItemStack housing = NanoHousings.housing(tony, SuitKind.MARK_85);
        ItemStack reactor = tony.getItemBySlot(EquipmentSlot.CHEST);
        tony.invulnerableTime = 0;
        tony.hurt(helper.getLevel().damageSources().mobAttack(husk), 12.0F);
        check(housing.getDamageValue() >= 1, "the housing wore: " + housing.getDamageValue());
        check(!reactor.isDamageableItem() && reactor.getDamageValue() == 0, "the reactor never wears");
        helper.succeed();
    }

    /** A stowed housing rebuilds a point every 40 ticks; the deployed one does not. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aStowedHousingRebuilds(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 5.5, 5.5, SuitKind.MARK_85);
        ItemStack out = NanoHousings.housing(tony, SuitKind.MARK_85);
        out.setDamageValue(10);
        ItemStack stowed = new ItemStack(IronSuits.item(SuitKind.MARK_50));
        stowed.setDamageValue(10);
        tony.getInventory().setItem(20, stowed);
        step(helper, 81, () -> {
            check(stowed.getDamageValue() <= 8 && stowed.getDamageValue() >= 7, "the stowed one rebuilt: "
                    + stowed.getDamageValue());
            check(out.getDamageValue() == 10, "the deployed one did not: " + out.getDamageValue());
            helper.succeed();
        }, tony);
    }

    /** The Nano Arc Reactor taken off the chest: the suit goes back at once. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theReactorTakenOffRetractsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 5.5, 5.5, SuitKind.MARK_85);
        tony.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        step(helper, 1, () -> {
            check(IronSuits.worn(tony) == null && !IronSuits.state(tony).has(IronManState.NANO_OUT), "retracted");
            helper.succeed();
        }, tony);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Mark 85

    /** Sneak + R: the blades out; R slashes 10, 10, then 14; the empty hand hits 12. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theEnergyBladesComboAndTheEmptyHand(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 5.5, 5.5, SuitKind.MARK_85);
        look(tony, EAST, 0.0F);
        Husk husk = husk(helper, 7.5, 5.5);
        press(tony, 1, true);
        check(data(tony).is(Live.IRON_BLADES), "the blades are out");
        press(tony, 1, false);
        assertNear(lost(husk), 10.0, 0.01, "the first slash");
        step(helper, 9, () -> {
            press(tony, 1, false);
            assertNear(lost(husk), 20.0, 0.01, "the second slash");
            step(helper, 9, () -> {
                press(tony, 1, false);
                assertNear(lost(husk), 34.0, 0.01, "the third slash");
                float before = lost(husk);
                TestKit.chargeAttack(tony);
                husk.invulnerableTime = 0;
                tony.attack(husk);
                assertNear(lost(husk) - before, 12.0, 0.01, "the empty hand with the blades");
                helper.succeed();
            }, tony);
        }, tony);
    }

    /** V: the giant chest laser goes through three creatures in a line (7 each). */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theChestLaserHitsThreeInALine(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 3.5, 10.5, SuitKind.MARK_85);
        look(tony, EAST, 0.0F);
        List<Husk> husks = List.of(husk(helper, 7.5, 10.5), husk(helper, 10.5, 10.5), husk(helper, 13.5, 10.5));
        hold(tony, 3, false);
        check(data(tony).is(Live.BEAMING), "the laser burns");
        step(helper, 2, () -> {
            for (Husk h : husks) {
                assertNear(lost(h), 7.0, 0.01, "the laser through each");
            }
            release(tony, 3);
            check(!data(tony).is(Live.BEAMING), "and it stops with the key");
            helper.succeed();
        }, tony);
    }

    /** B: the energy shield takes a creeper's blast from the front whole; from behind it does not. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theEnergyShieldStopsACreeperFromTheFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer front = deployed(f, 8.5, 10.5, SuitKind.MARK_85);
        ServerPlayer back = deployed(f, 12.5, 10.5, SuitKind.MARK_85);
        look(front, EAST, 0.0F);
        look(back, EAST, 0.0F);
        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new Vec3(10.5, FLOOR, 10.5));
        hold(front, 4, false);
        hold(back, 4, false);
        check(EnergyShield.up(front) && EnergyShield.up(back), "both shields up");
        Vec3 at = creeper.position();
        helper.getLevel().explode(creeper, at.x, at.y, at.z, 3.0F, Level.ExplosionInteraction.NONE);
        check(front.getHealth() == front.getMaxHealth(), "from the front: nothing, " + front.getHealth());
        check(back.getHealth() < back.getMaxHealth(), "from behind: hurt");
        release(front, 4);
        release(back, 4);
        helper.succeed();
    }

    /** Sneak + B: the Lightning Refocuser chains through five creatures, 10 / 8 / 6 / 5 / 4; charged: free, × 1.5. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theRefocuserChainsAndLightningChargesIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 2.5, 10.5, SuitKind.MARK_85);
        look(tony, EAST, 0.0F);
        List<Husk> husks = List.of(husk(helper, 6.5, 10.5), husk(helper, 9.5, 10.5), husk(helper, 12.5, 10.5),
                husk(helper, 15.5, 10.5), husk(helper, 18.5, 10.5));
        press(tony, 4, true);
        double[] expected = {10, 8, 6, 5, 4};
        for (int i = 0; i < husks.size(); i++) {
            assertNear(lost(husks.get(i)), expected[i], 0.01, "link " + i);
        }
        assertNear(SuitEnergy.get(tony), 75.0, 0.01, "25 energy");
        for (Husk h : husks) {
            h.setHealth(h.getMaxHealth());
        }
        ready(tony);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        bolt.moveTo(tony.position());
        helper.getLevel().addFreshEntity(bolt);
        until(helper, 20, () -> IronSuits.state(tony).has(IronManState.CHARGED), () -> {
            SuitEnergy.set(tony, 10.0);
            tony.setHealth(tony.getMaxHealth());
            press(tony, 4, true);
            for (int i = 0; i < husks.size(); i++) {
                assertNear(lost(husks.get(i)), expected[i] * 1.5, 0.01, "charged link " + i);
            }
            check(SuitEnergy.get(tony) >= 9.9F, "charged: free");
            check(!IronSuits.state(tony).has(IronManState.CHARGED), "the charge is used up");
            helper.succeed();
        }, tony);
    }

    /** N: six missiles lock six different creatures and each homes on its own; a moving one is hit. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 260)
    public static void homingMissilesLockSixAndHitAMovingOne(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 2.5, 10.5, SuitKind.MARK_85);
        look(tony, EAST, 0.0F);
        Husk mover = husk(helper, 10.5, 3.5);
        List<Husk> husks = List.of(mover, husk(helper, 16.5, 10.5), husk(helper, 16.5, 14.5), husk(helper, 18.5, 8.5),
                husk(helper, 18.5, 16.5), husk(helper, 13.5, 17.5));
        press(tony, 5, false);
        List<IronMissileEntity> missiles = helper.getLevel().getEntitiesOfClass(IronMissileEntity.class,
                tony.getBoundingBox().inflate(4.0));
        check(missiles.size() == 6, "six missiles: " + missiles.size());
        Set<Integer> targets = new HashSet<>();
        for (IronMissileEntity m : missiles) {
            MissileGuidance.Homing homing = MissileGuidance.homing(m);
            check(homing != null && homing.target() >= 0, "each is locked");
            targets.add(homing.target());
            check(m.maxLife() == 200, "it homes for ten seconds: " + m.maxLife());
        }
        check(targets.size() == 6, "six different locks: " + targets.size());
        int[] t = {0};
        until(helper, 220, () -> {
            t[0]++;
            // The mover walks round in a circle.
            Vec3 c = helper.absoluteVec(new Vec3(10.5, FLOOR, 3.5));
            mover.teleportTo(c.x + Math.cos(t[0] * 0.15) * 2.0, c.y, c.z + Math.sin(t[0] * 0.15) * 2.0);
            return lost(mover) > 0.0F;
        }, () -> {
            check(lost(mover) >= 4.0F, "the moving one was hit: " + lost(mover));
            helper.succeed();
        }, tony);
    }

    /** A missile whose target dies picks the nearest other; one with no lock flies 32 blocks and pops. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aMissileRetargetsWhenItsTargetDies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 2.5, 10.5, SuitKind.MARK_85);
        look(tony, EAST, 0.0F);
        Husk first = husk(helper, 16.5, 10.5);
        Husk second = husk(helper, 16.5, 14.5);
        // (Already near the two: the nearest other hostile is the second husk, not one of a neighbouring test's.)
        IronMissileEntity missile = IronMissileEntity.create(helper.getLevel(), tony, IronMissileEntity.HOMING,
                helper.absoluteVec(new Vec3(12.5, FLOOR + 1.5, 10.5)), new Vec3(1, 0, 0), HomingMissiles.SPEED, 4.0F,
                0.6F, 239.0);
        MissileGuidance.guide(missile, tony, first, 200, 12.0, HomingMissiles.SPEED);
        helper.getLevel().addFreshEntity(missile);
        first.discard();
        Vec3 steered = MissileGuidance.steer(missile);
        check(MissileGuidance.homing(missile).target() == second.getId(), "it turns to the other one");
        check(steered.z > 0.0, "towards it");
        IronMissileEntity loose = IronMissileEntity.create(helper.getLevel(), tony, IronMissileEntity.HOMING,
                tony.getEyePosition(), new Vec3(1, 0, 0), HomingMissiles.SPEED, 4.0F, 0.6F,
                MissileGuidance.UNLOCKED_RANGE);
        check(loose.maxLife() == (int) Math.ceil(MissileGuidance.UNLOCKED_RANGE / HomingMissiles.SPEED),
                "no lock: 32 blocks of life");
        Vec3 turned = MissileGuidance.turn(new Vec3(1.2, 0, 0), new Vec3(0, 0, 1), Math.toRadians(12.0), 1.2);
        assertNear(Math.toDegrees(Math.atan2(turned.z, turned.x)), 12.0, 0.01, "12 degrees a tick at most");
        assertNear(turned.length(), 1.2, 1.0E-6, "at its speed");
        missile.discard();
        helper.succeed();
    }

    /** Sneak + N: two drones shoot a creature (6 a shot), never a player without PvP, and go at 400 ticks. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 480)
    public static void theDronesShootCreaturesNotPlayersAndGo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 10.5, 10.5, SuitKind.MARK_85);
        ServerPlayer peter = f.player(6.5, FLOOR, 10.5);
        Husk husk = husk(helper, 15.5, 10.5);
        press(tony, 5, true);
        List<NanoDroneEntity> drones = NanoDrones.of(tony);
        check(drones.size() == 2, "two drones: " + drones.size());
        step(helper, 40, () -> {
            check(lost(husk) >= 6.0F && lost(husk) % 6.0F < 0.01F, "the creature was shot: " + lost(husk));
            check(peter.getHealth() == peter.getMaxHealth(), "the player was not");
            until(helper, 400, () -> NanoDrones.of(tony).isEmpty(), () -> {
                check(!IronSuits.state(tony).has(IronManState.DRONES), "their time is over");
                long left = IronSuits.state(tony).dronesUntil();
                check(left == 0L, "and forgotten");
                helper.succeed();
            }, tony, peter);
        }, tony, peter);
    }

    /**
     * (3.8.3) The drones are the Endgame wing units, two crescent blades each: the inner blades beside his arms at
     * shoulder height, the outer ones further out, further back and a little lower, mirrored on his two sides; the
     * formation turns with his body and grows with his size; the bob is small; the inner blade grows first and folds
     * last. Live: the two drones fly to their side's place and their lasers leave from a blade beside him.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void theWingBladesFanOutBesideHim(GameTestHelper helper) {
        for (double t : new double[] {0.0, 7.3, 41.0, 133.5, 977.0}) {
            Vec3 rightIn = WingFormation.local(0, 0, t);
            Vec3 rightOut = WingFormation.local(0, 1, t);
            Vec3 leftIn = WingFormation.local(1, 0, t);
            Vec3 leftOut = WingFormation.local(1, 1, t);
            check(rightIn.x < -1.0 && leftIn.x > 1.0, "the inner blades beside his arms: " + rightIn + " " + leftIn);
            check(-rightOut.x > -rightIn.x + 0.4 && leftOut.x > leftIn.x + 0.4, "the outer blades further out");
            check(rightOut.z < rightIn.z - 0.1 && leftOut.z < leftIn.z - 0.1, "and further back");
            for (Vec3 v : List.of(rightIn, rightOut, leftIn, leftOut)) {
                check(v.y > 0.9 && v.y < 1.5, "at shoulder-to-hand height: " + v);
            }
            check(rightOut.y < rightIn.y + 2.0 * WingFormation.BOB && leftOut.y < leftIn.y + 2.0 * WingFormation.BOB,
                    "the outer ones a little lower");
            assertNear(-rightIn.x, leftIn.x, 2.0 * WingFormation.DRIFT + 1.0E-9, "the two sides mirrored");
            assertNear(rightIn.y, leftIn.y, 2.0 * WingFormation.BOB + 1.0E-9, "the bob is small");
        }
        Vec3 base = new Vec3(10.0, 64.0, 10.0);
        Vec3 local = WingFormation.local(0, 0, 0.0);
        Vec3 south = WingFormation.world(base, 0.0F, 1.0F, local).subtract(base);
        check(south.x < -1.0 && Math.abs(south.z) < 0.2, "facing south, his right wing is to the west: " + south);
        Vec3 west = WingFormation.world(base, 90.0F, 1.0F, local).subtract(base);
        check(west.z < -1.0 && Math.abs(west.x) < 0.2, "turned west, it is to the north: " + west);
        assertNear(west.length(), south.length(), 1.0E-6, "the same place in his frame");
        assertNear(WingFormation.world(base, 0.0F, 2.0F, local).subtract(base).length(), 2.0 * south.length(), 1.0E-6,
                "twice as far for a giant");
        check(WingFormation.grown(0, 0.0F) == 0.0F && WingFormation.grown(0, WingFormation.GROW_TICKS) == 1.0F
                && WingFormation.grown(1, WingFormation.GROW_TICKS) < 1.0F, "the inner blade grows first");
        check(WingFormation.left(1, WingFormation.RETRACT_TICKS * 0.6F) == 0.0F
                && WingFormation.left(0, WingFormation.RETRACT_TICKS * 0.6F) > 0.0F
                && WingFormation.left(0, WingFormation.RETRACT_TICKS) == 0.0F, "and folds last");

        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 10.5, 10.5, SuitKind.MARK_85);
        press(tony, 5, true);
        List<NanoDroneEntity> drones = NanoDrones.of(tony);
        check(drones.size() == 2 && drones.get(0).slot() != drones.get(1).slot(), "one wing unit on each side");
        step(helper, 12, () -> {
            long now = helper.getLevel().getGameTime();
            Vec3 right = Vec3.directionFromRotation(0.0F, tony.yBodyRot + 90.0F);
            for (NanoDroneEntity drone : NanoDrones.of(tony)) {
                Vec3 place = WingFormation.centre(tony, drone.slot(), now);
                check(drone.position().distanceTo(place) < 0.35, "the drone flies in its place: "
                        + drone.position().distanceTo(place));
                double towards = WingFormation.side(drone.slot()) == 0 ? 1.0 : -1.0;
                for (int blade = 0; blade < WingFormation.BLADES; blade++) {
                    Vec3 from = drone.muzzle(tony, blade).subtract(tony.position());
                    double out = from.dot(right) * towards;
                    check(out > 1.0 && out < 2.4, "blade " + blade + " fires from beside him: " + out);
                    check(from.y > 0.9 && from.y < 1.5, "at shoulder-to-hand height: " + from.y);
                }
            }
            helper.succeed();
        }, tony);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Mark 50

    /** Sneak + R: the nano blade (the empty hand hits 9); B: the nano shield cuts a blow from the front by 80 %. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theNanoBladeAndTheNanoShield(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 5.5, 5.5, SuitKind.MARK_50);
        look(tony, EAST, 0.0F);
        Husk husk = husk(helper, 7.5, 5.5);
        press(tony, 1, true);
        check(data(tony).is(Live.IRON_BLADES), "the blade formed");
        TestKit.chargeAttack(tony);
        tony.attack(husk);
        assertNear(lost(husk), 9.0, 0.01, "the nano blade");
        hold(tony, 4, false);
        check(EnergyShield.up(tony), "the nano shield is up");
        DamageSource fromFront = helper.getLevel().damageSources().mobAttack(husk);
        assertNear(EnergyShield.absorbed(tony, fromFront, 10.0F), 8.0, 0.01, "80 % of a blow from the front");
        Husk behind = husk(helper, 3.5, 5.5);
        assertNear(EnergyShield.absorbed(tony, helper.getLevel().damageSources().mobAttack(behind), 10.0F), 0.0,
                0.01, "nothing from behind");
        release(tony, 4);
        check(!EnergyShield.up(tony), "down with the key");
        helper.succeed();
    }

    /** N held and let go too soon: the cannon is refused and half its energy comes back. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theCannonReleasedEarlyIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tony = deployed(f, 5.5, 10.5, SuitKind.MARK_50);
        look(tony, EAST, 0.0F);
        Husk husk = husk(helper, 10.5, 10.5);
        hold(tony, 5, false);
        check(data(tony).is(Live.IRON_CHARGING), "charging");
        step(helper, 10, () -> {
            release(tony, 5);
            check(!data(tony).is(Live.IRON_CHARGING), "the charge ends");
            check(lost(husk) == 0.0F, "nothing fired");
            assertNear(SuitEnergy.get(tony), 100.0 - 25.0 * 0.5, 0.01,
                    "half the energy back");
            check(data(tony).isReady(HeroAbility.IRON_MAN_MISSILES, tony.level().getGameTime()), "no cooldown");
            hold(tony, 5, false);
            step(helper, 21, () -> {
                release(tony, 5);
                assertNear(lost(husk), 14.0, 0.01, "charged: the cannon's 14");
                helper.succeed();
            }, tony);
        }, tony);
    }
}
