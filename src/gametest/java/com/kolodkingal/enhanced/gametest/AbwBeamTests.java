package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.network.BeamPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * 3.2, Billy's continuous Hex Beam (requirement 3a, {@code billy.beam}, default true): R held fires a beam along his look
 * that hurts what the crosshair is on every few ticks, costs hunger, stops when R is released or he cannot go on, grows
 * with his Build Magic rank, is stopped by a Chaos Shield, follows the PvP rules, and does nothing to Apocalypse's shield;
 * the beam infrastructure takes a style so another source can use it. With {@code billy.beam} false R is the old Hex Bolt
 * (the shared fixture switches the beam off, so the old tests keep running). Every test has a batch of its own.
 */
public final class AbwBeamTests {
    private static void hold(ServerPlayer billy) {
        AbilityHandler.onKey(billy, 1, true);
    }

    private static void release(ServerPlayer billy) {
        AbilityHandler.onKey(billy, 1, false);
    }

    private static Pig dummy(GameTestHelper helper, double x, double z) {
        // (A pig: it has no armour to take a part of the damage.)
        return helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(x, FLOOR, z));
    }

    private static TestKit.Fixture beamFixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.BILLY_BEAM, true);
        return f;
    }

    private static void setRank(ServerPlayer billy, int rank) {
        ModAttachments.BUILD_DATA.set(billy, BuildData.EMPTY.withRank(rank));
    }

    /** Held, it hurts what the crosshair is on, 1.5 every 5 ticks at rank 1; a block stops it; released, it stops. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_aim", timeoutTicks = 100)
    public static void beamHurtsWhatTheCrosshairIsOnAndStopsOnRelease(GameTestHelper helper) {
        TestKit.Fixture f = beamFixture(helper);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        PowerData data = Powers.get(billy);
        Pig target = dummy(helper, 10.5, 10.5);
        Pig aside = dummy(helper, 10.5, 14.5);
        lookAt(billy, target);
        hold(billy);
        check(data.is(Live.BEAMING) && ContinuousBeam.isFiring(billy), "R held: the beam burns");
        float perHit = EnhancedConfig.BILLY_BEAM_DAMAGE_PER_SECOND.get().floatValue() * EnhancedConfig.BILLY_BEAM_INTERVAL.get() / 20.0F;
        assertNear(perHit, 1.5, 1.0E-4, "a hit of the default beam is 1.5");
        f.everyTick(70, tick -> {
            PowerTicker.tick(billy);
            if (tick == 20) {
                float lost = target.getMaxHealth() - target.getHealth();
                double hits = lost / perHit;
                check(Math.abs(hits - Math.round(hits)) < 0.01 && hits >= 4 && hits <= 5, "four or five hits in a second (6 a second): " + lost);
                check(aside.getHealth() == aside.getMaxHealth(), "what the crosshair is not on is not hurt");
                check(target.getLastDamageSource() != null && target.getLastDamageSource().is(ModDamageTypes.HEX_BEAM)
                        && target.getLastDamageSource().getEntity() == billy, "it is the Hex Beam's damage and Billy gets the credit");
                // A wall in the way stops it.
                helper.setBlock(7, (int) FLOOR + 1, 10, Blocks.STONE);
                helper.setBlock(7, (int) FLOOR, 10, Blocks.STONE);
                target.setHealth(target.getMaxHealth());
            }
            if (tick == 40) {
                check(target.getHealth() == target.getMaxHealth(), "a wall in the beam's way takes it all");
                release(billy);
                check(!data.is(Live.BEAMING) && !ContinuousBeam.isFiring(billy), "R released: it stops");
                helper.setBlock(7, (int) FLOOR + 1, 10, Blocks.AIR);
                helper.setBlock(7, (int) FLOOR, 10, Blocks.AIR);
            }
            if (tick > 40 && tick < 70) {
                check(target.getHealth() == target.getMaxHealth(), "and it hurts nothing after (tick " + tick + ")");
            }
            return tick == 70;
        });
    }

    /** It burns hunger, refuses an empty stomach, dies when he starves or becomes a spectator, and the clients hear of it. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_cost", timeoutTicks = 60)
    public static void beamCostsHungerAndNeedsFood(GameTestHelper helper) {
        TestKit.Fixture f = beamFixture(helper);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        PowerData data = Powers.get(billy);
        billy.getFoodData().setFoodLevel(0);
        hold(billy);
        check(!data.is(Live.BEAMING), "on an empty stomach it cannot start");
        billy.getFoodData().setFoodLevel(20);
        TestKit.sentPackets(billy);
        hold(billy);
        check(data.is(Live.BEAMING), "fed, it starts");
        float exhaustion = billy.getFoodData().getExhaustionLevel();
        for (int i = 0; i < 20; i++) {
            PowerTicker.tick(billy);
        }
        float spent = billy.getFoodData().getExhaustionLevel() - exhaustion;
        assertNear(spent, EnhancedConfig.BILLY_BEAM_EXHAUSTION_PER_SECOND.get(), 0.2, "a second of beam costs its exhaustion: " + spent);
        List<BeamPayload> told = beamPayloads(billy);
        check(!told.isEmpty() && told.get(0).on() && told.get(0).style().equals("billy_hex") && told.get(0).shooter() == billy.getId(),
                "the clients near are told: " + told);
        check(told.size() >= 2 && told.stream().allMatch(BeamPayload::on), "the start, and the first tick of the beam: " + told.size());
        assertNear(told.get(0).range(), BillyAbilities.beamRange(BuildData.EMPTY), 1.0E-3, "with the beam's range");
        billy.getFoodData().setFoodLevel(0);
        PowerTicker.tick(billy);
        check(!data.is(Live.BEAMING) && !ContinuousBeam.isFiring(billy), "starving ends it");
        check(beamPayloads(billy).stream().anyMatch(p -> !p.on()), "and the clients are told it stopped");
        billy.getFoodData().setFoodLevel(20);
        hold(billy);
        check(data.is(Live.BEAMING), "fed again, it can");
        billy.setGameMode(GameType.SPECTATOR);
        PowerTicker.tick(billy);
        check(!data.is(Live.BEAMING), "a spectator fires nothing");
        helper.succeed();
    }

    /** The server repeats itself every few ticks while R is held, so a client that comes into range later still draws it. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_keepalive", timeoutTicks = 60)
    public static void beamRepeatsItselfWhileHeld(GameTestHelper helper) {
        TestKit.Fixture f = beamFixture(helper);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        TestKit.sentPackets(billy);
        hold(billy);
        List<BeamPayload> told = new ArrayList<>();
        f.everyTick(33, tick -> {
            PowerTicker.tick(billy);
            told.addAll(beamPayloads(billy));
            if (tick < 33) {
                return false;
            }
            check(told.size() >= 4 && told.stream().allMatch(BeamPayload::on), "payloads about every " + ContinuousBeam.KEEP_ALIVE_TICKS + " ticks: " + told.size());
            release(billy);
            told.addAll(beamPayloads(billy));
            check(!told.get(told.size() - 1).on(), "and one that says it stopped");
            return true;
        });
    }

    private static List<BeamPayload> beamPayloads(ServerPlayer player) {
        List<BeamPayload> found = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof BeamPayload beam) {
                found.add(beam);
            }
        }
        return found;
    }

    /** Damage and range grow evenly from the first rank to the highest (maxRank); rank 1 is the base. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_rank")
    public static void beamGrowsWithBuildMagicRank(GameTestHelper helper) {
        TestKit.Fixture f = beamFixture(helper);
        f.config(BuildConfig.BILLY_MAX_RANK, 4);
        double base = EnhancedConfig.BILLY_BEAM_DAMAGE_PER_SECOND.get();
        double bonus = EnhancedConfig.BILLY_BEAM_RANK_DAMAGE_BONUS.get();
        double baseRange = EnhancedConfig.BILLY_BEAM_RANGE.get();
        double rangeBonus = EnhancedConfig.BILLY_BEAM_RANK_RANGE_BONUS.get();
        float previous = 0.0F;
        for (int rank = 1; rank <= 4; rank++) {
            BuildData build = BuildData.EMPTY.withRank(rank);
            double progress = (rank - 1) / 3.0;
            assertNear(BillyAbilities.beamProgress(build), progress, 1.0E-9, "rank " + rank + " is " + progress + " of the way");
            assertNear(BillyAbilities.beamDamagePerSecond(build), base * (1.0 + bonus * progress), 1.0E-4, "damage at rank " + rank);
            assertNear(BillyAbilities.beamRange(build), baseRange * (1.0 + rangeBonus * progress), 1.0E-6, "range at rank " + rank);
            check(BillyAbilities.beamDamagePerSecond(build) > previous, "it grows with every rank");
            previous = BillyAbilities.beamDamagePerSecond(build);
        }
        assertNear(BillyAbilities.beamDamagePerSecond(BuildData.EMPTY), base, 1.0E-6, "an Apprentice's beam is the base");
        f.config(BuildConfig.BILLY_MAX_RANK, 1);
        assertNear(BillyAbilities.beamProgress(BuildData.EMPTY.withRank(4)), 0.0, 1.0E-9, "with maxRank 1 he never grows");
        helper.succeed();
    }

    /** The range really is the rank's: a target 9 blocks away is out of reach at rank 1 (range 6) and in reach at the top (12). */
    @EnhancedTest(template = ARENA, batch = "abw_beam_range", timeoutTicks = 60)
    public static void rankLengthensTheBeamAndStrengthensIt(GameTestHelper helper) {
        TestKit.Fixture f = beamFixture(helper);
        f.config(EnhancedConfig.BILLY_BEAM_RANGE, 6);
        f.config(BuildConfig.BILLY_MAX_RANK, 4);
        ServerPlayer billy = f.player(1.5, FLOOR, 10.5, Power.BILLY);
        Pig target = dummy(helper, 10.5, 10.5);
        lookAt(billy, target);
        hold(billy);
        for (int i = 0; i < 10; i++) {
            PowerTicker.tick(billy);
        }
        check(target.getHealth() == target.getMaxHealth(), "9 blocks away is out of reach of a 6-block beam");
        release(billy);
        setRank(billy, 4);
        hold(billy);
        float perHit = BillyAbilities.beamDamagePerSecond(ModAttachments.BUILD_DATA.get(billy)) * EnhancedConfig.BILLY_BEAM_INTERVAL.get() / 20.0F;
        PowerTicker.tick(billy);
        assertNear(target.getMaxHealth() - target.getHealth(), perHit, 1.0E-3, "at the top rank it reaches and hits harder: " + perHit);
        check(perHit > 1.5F, "harder than the first rank's 1.5");
        helper.succeed();
    }

    /** PvP rules: another player is hurt only when PvP is on. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_pvp", timeoutTicks = 60)
    public static void beamFollowsThePvpRules(GameTestHelper helper) {
        TestKit.Fixture f = beamFixture(helper);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        ServerPlayer other = f.player(10.5, FLOOR, 10.5);
        lookAt(billy, other);
        f.pvp(false);
        hold(billy);
        PowerTicker.tick(billy);
        check(other.getHealth() == other.getMaxHealth(), "with PvP off the beam hurts no player");
        release(billy);
        f.pvp(true);
        // (the beam's next hit waits its interval, whatever the key did: let it pass)
        helper.runAfterDelay(EnhancedConfig.BILLY_BEAM_INTERVAL.get() + 1, () -> {
            hold(billy);
            PowerTicker.tick(billy);
            check(other.getHealth() < other.getMaxHealth(), "with PvP on it does");
            helper.succeed();
        });
    }

    /** A Chaos Shield is solid to the beam: it ends on the sphere and wears the shield, and nothing behind it is hurt. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_shield", timeoutTicks = 80)
    public static void chaosShieldStopsTheBeam(GameTestHelper helper) {
        TestKit.Fixture f = beamFixture(helper);
        f.pvp(true);
        ServerPlayer billy = f.player(1.5, FLOOR, 10.5, Power.BILLY);
        ServerPlayer wanda = f.player(8.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Pig behind = dummy(helper, 18.5, 10.5);
        AbilityHandler.onKey(wanda, 4, true);
        helper.runAfterDelay(EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() + 2, () -> {
            check(Powers.get(wanda).chaosShieldUp(helper.getLevel().getGameTime()), "her shield is up");
            lookAt(billy, behind);
            float strength = Powers.get(wanda).shieldStrength();
            hold(billy);
            PowerTicker.tick(billy);
            check(behind.getHealth() == behind.getMaxHealth(), "the creature behind the shield is not touched");
            check(Powers.get(wanda).shieldStrength() < strength, "the shield paid for the hit: " + Powers.get(wanda).shieldStrength());
            check(wanda.getHealth() == wanda.getMaxHealth(), "and she is not hurt");
            helper.succeed();
        });
    }

    /** Apocalypse's shield turns it away like anything but mind power: no damage, the shield is not worn. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_boss", timeoutTicks = 80)
    public static void apocalypseShieldTakesNothingFromTheBeam(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.BILLY);
        d.f().config(EnhancedConfig.BILLY_BEAM, true);
        ServerPlayer billy = d.hero();
        ApocalypseEntity boss = d.boss();
        float health = boss.getHealth();
        float strength = boss.shield().strength();
        hold(billy);
        for (int i = 0; i < 20; i++) {
            PowerTicker.tick(billy);
        }
        check(ContinuousBeam.isFiring(billy), "the beam burns");
        check(boss.getHealth() == health && boss.shield().strength() == strength, "it does exactly nothing to him: " + boss.getHealth() + ", " + boss.shield().strength());
        helper.succeed();
    }

    /** With billy.beam false R is the old Hex Bolt: a shot with a cooldown, no beam. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_off", timeoutTicks = 40)
    public static void withTheOptionOffRIsTheOldHexBolt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.BILLY_BEAM, false);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        Pig target = dummy(helper, 10.5, 10.5);
        lookAt(billy, target);
        hold(billy);
        check(!Powers.get(billy).is(Live.BEAMING) && !ContinuousBeam.isFiring(billy), "no beam");
        assertNear(target.getMaxHealth() - target.getHealth(), EnhancedConfig.HEX_BOLT_DAMAGE.get(), 1.0E-3, "the bolt struck at once");
        check(!Powers.get(billy).isReady(1, helper.getLevel().getGameTime()), "and R is on its short cooldown");
        helper.succeed();
    }

    /** The infrastructure takes a style: a second source with its own palette fires through the same driver and payload. */
    @EnhancedTest(template = ARENA, batch = "abw_beam_style", timeoutTicks = 60)
    public static void aSecondStyleUsesTheSameInfrastructure(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BeamStyle purple = BeamStyles.register(new BeamStyle("abw_test_purple", "beam.enhanced.billy_hex", 0xFFFFFF, 0xF0D8FF, 0xB45CFF, 0x7A2FD6, 0x3A127A, true));
        f.onClose(() -> BeamStyles.register(BeamStyles.BILLY));
        check(BeamStyles.byId("billy_hex") == BeamStyles.BILLY && BeamStyles.byId("abw_test_purple") == purple, "both are found by id");
        check(BeamStyles.all().contains(BeamStyles.BILLY) && BeamStyles.all().contains(purple), "and listed");
        assertNear(BeamStyles.BILLY.main(), 0x5CC8FF, 0, "Billy's is blue");
        check(!BeamStyles.BILLY.offHand() && purple.offHand(), "the hand is part of the style");
        ServerPlayer shooter = f.player(3.5, FLOOR, 10.5);
        Pig target = dummy(helper, 10.5, 10.5);
        lookAt(shooter, target);
        TestKit.sentPackets(shooter);
        ContinuousBeam.start(shooter, purple, 20.0);
        check(Powers.get(shooter).is(Live.BEAMING), "a player with no power can fire a beam");
        ContinuousBeam.tick(shooter, new BeamSpec(purple, 20.0, 3.0F, 5, 0.0F, ModDamageTypes.HEX_BEAM, null));
        assertNear(target.getMaxHealth() - target.getHealth(), 3.0, 1.0E-3, "it hurts what the crosshair is on, with the spec's damage");
        ContinuousBeam.stop(shooter);
        List<BeamPayload> told = beamPayloads(shooter);
        check(told.size() >= 2 && told.get(0).style().equals("abw_test_purple") && told.get(0).on() && !told.get(told.size() - 1).on(),
                "the payload names the style, then says it stopped: " + told);
        check(!Powers.get(shooter).is(Live.BEAMING), "and the flag is gone");
        helper.succeed();
    }
}
