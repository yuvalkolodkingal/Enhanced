package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGifts;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import com.kolodkingal.enhanced.ultron.SokoviaLift;
import com.kolodkingal.enhanced.ultron.SokoviaRecovery;
import com.kolodkingal.enhanced.ultron.SokoviaRelocations;
import com.kolodkingal.enhanced.ultron.SokoviaTemplate;
import com.kolodkingal.enhanced.ultron.UltronFight;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.GROUND;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.LIFT;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.MIDDLE_X;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.MIDDLE_Z;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.PAD;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.Script;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.SURFACE;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.blocksInTheSky;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.client;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.groundHash;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.in;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.journals;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.pad;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.rel;
import static com.kolodkingal.enhanced.gametest.SokoviaTestSupport.ritual;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.tank;

/**
 * When the lift does not get to finish: a server that is stopped puts everything back in the same tick, a crash leaves
 * a journal and a floating island that the next start clears, and whoever was up there is set down on the ground when
 * they log in. Also what happens to the blocks that are not the island's when it comes down.
 *
 * <p>A crash is played by throwing the fight and its lift away with no clean-up (as the dead process would), and the
 * recovery of the next start by calling it on the journals the "dead" fight left. Each fight has a batch of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SokoviaRecoveryTests {

    // ------------------------------------------------------------------------------------------------------------
    // A clean stop

    /** The server stops with the island rising: nothing was placed, everyone is set down where they were. */
    @GameTest(template = PAD, batch = "ultron_lift_stop_rising", timeoutTicks = 300)
    public static void stoppingTheServerWhileTheIslandRisesSetsEveryoneDown(GameTestHelper helper) {
        stopWhen(helper, fight -> in(fight, SokoviaLift.Step.RISING) && SokoviaLift.of(fight).altitude() > 6.0, false);
    }

    /** ... half placed: what was placed is cleared. */
    @GameTest(template = PAD, batch = "ultron_lift_stop_placing", timeoutTicks = 300)
    public static void stoppingTheServerWhileTheIslandIsBeingPlacedClearsWhatWasPlaced(GameTestHelper helper) {
        stopWhen(helper, fight -> in(fight, SokoviaLift.Step.PLACING) && SokoviaLift.of(fight).job() != null && SokoviaLift.of(fight).job().edits() > 200, false);
    }

    /** ... up: it comes down at once, with everyone on it. */
    @GameTest(template = PAD, batch = "ultron_lift_stop_placed", timeoutTicks = 300)
    public static void stoppingTheServerWhileTheIslandIsUpBringsEveryoneDown(GameTestHelper helper) {
        stopWhen(helper, fight -> fight.phase() == UltronFight.Phase.ISLAND && in(fight, SokoviaLift.Step.PLACED), false);
    }

    /** ... coming down: everyone is put on the ground at once. */
    @GameTest(template = PAD, batch = "ultron_lift_stop_falling", timeoutTicks = 600)
    public static void stoppingTheServerWhileTheIslandComesDownPutsEveryoneOnTheGround(GameTestHelper helper) {
        stopWhen(helper, fight -> in(fight, SokoviaLift.Step.FALLING) && SokoviaLift.of(fight).altitude() < LIFT - 6.0, true);
    }

    /**
     * ... and the one who was knocked up (or whose client lags: the lift lets a carried player drift a few blocks before
     * it puts them back) is put on the ground where the lift says they are, not where the knock left them.
     */
    @GameTest(template = PAD, batch = "ultron_lift_stop_knocked", timeoutTicks = 600)
    public static void stoppingTheServerWhileTheIslandComesDownPutsAKnockedUpPlayerOnTheGround(GameTestHelper helper) {
        stopWhen(helper, fight -> in(fight, SokoviaLift.Step.FALLING) && SokoviaLift.of(fight).altitude() < LIFT - 6.0, true, 4.5);
    }

    private static void stopWhen(GameTestHelper helper, Predicate<UltronFight> when, boolean win) {
        stopWhen(helper, when, win, 0.0);
    }

    /**
     * Plays the fight to a moment ({@code when}, with the fight won as soon as the island is up if {@code win}), then
     * stops the server: the island is cleared and everyone is on the ground where they stood, flags and journal and
     * lift are gone, all in the same tick, and the Program that started the fight is waiting for the summoner as a
     * gift. The starter is {@code knock} blocks higher than the lift says they are when it stops.
     */
    private static void stopWhen(GameTestHelper helper, Predicate<UltronFight> when, boolean win, double knock) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.EDITS_PER_TICK, 400);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer other = f.player(MIDDLE_X + 4.0, GROUND, MIDDLE_Z - 3.0);
        tank(starter);
        long ground = groundHash(helper);
        UltronFight fight = ritual(f, starter);
        Vec3[] where = new Vec3[2];
        Script script = new Script();
        script.step("the second fighter joins", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> liftBegins(fight));
        script.step("the moment comes", () -> {
            client(starter, other);
            // (Where they stand sideways: the boss pushes them about a little on the island.)
            where[0] = rel(helper, starter);
            where[1] = rel(helper, other);
            if (win && fight.phase() == UltronFight.Phase.ISLAND && !fight.isOver()) {
                fight.end(BossEnd.VICTORY);
            }
            return when.test(fight);
        });
        script.then("the server stops", () -> {
            starter.setPos(starter.position().add(0.0, knock, 0.0));
            BossFights.stopAll(server, StopReason.SERVER_STOPPING);
            check(blocksInTheSky(helper) == 0, "the sky is empty in the same tick: " + blocksInTheSky(helper));
            check(groundHash(helper) == ground, "the ground is as it was");
            for (int i = 0; i < 2; i++) {
                ServerPlayer player = i == 0 ? starter : other;
                Vec3 now = rel(helper, player);
                assertNear(now.y, GROUND, 0.6, "player " + i + " is on the ground");
                assertNear(now.x, where[i].x, 1.0, "player " + i + " has not moved sideways");
                assertNear(now.z, where[i].z, 1.0, "player " + i + " has not moved sideways");
                check(!player.isNoGravity() && !Powers.get(player).is(Live.LIFTED), "and is free");
            }
            check(journals(level).isEmpty(), "the journal is gone");
            check(!SokoviaLift.any() && SokoviaLift.of(fight) == null, "the lift is gone");
            check(fight.isFinished() && fight.island().islandOffset() == 0.0 && !fight.island().isIslandUp(), "the fight is finished and the arena is the ground's");
            // (A fight that was won is not owed back: only one that the players did not lose is.)
            check(win || BossGifts.pending(server, starter.getUUID()).stream().anyMatch(stack -> stack.is(ModItems.ULTRON_PROGRAM.get())),
                    "the Program is waiting for whoever used it");
        });
        script.run(f, 280);
    }

    /**
     * The lift begins after a ritual: the warning, which makes his body appear (the test does not wait out the
     * awakening, where it would), then he is calmed ({@link UltronTestSupport#calm}: before his body is there that does
     * nothing to him, and he would choose attacks on the island and through the pause after a win), then the lock.
     */
    private static void liftBegins(UltronFight fight) {
        fight.forceState(UltronFight.Phase.LIFT_WARN);
        check(fight.boss() != null, "(his body is there, to be calmed)");
        calm(fight);
        fight.forceState(UltronFight.Phase.LIFT_LOCK);
    }

    /**
     * Whoever is in the air over the island when it goes (Ultron's Gravity Pull holds a fighter at his chest, about four
     * blocks up; a jump; a knock) is set down on the ground below, not as high over it: by a stop while it comes down
     * (the lift carried them down that high), by a stop while it is up (they stand on nothing), and when it lands. A hero
     * who is flying flies on at his height.
     */
    @GameTest(template = PAD, batch = "ultron_lift_air_falling", timeoutTicks = 600)
    public static void stoppingTheServerWhileTheIslandComesDownPutsAFighterInTheAirOnTheGround(GameTestHelper helper) {
        inTheAir(helper, true, true);
    }

    @GameTest(template = PAD, batch = "ultron_lift_air_placed", timeoutTicks = 400)
    public static void stoppingTheServerWhileTheIslandIsUpPutsAFighterInTheAirOnTheGround(GameTestHelper helper) {
        inTheAir(helper, false, true);
    }

    @GameTest(template = PAD, batch = "ultron_lift_air_landing", timeoutTicks = 600)
    public static void theIslandLandsAFighterWhoWasInTheAirOnTheGround(GameTestHelper helper) {
        inTheAir(helper, true, false);
    }

    /** How high over the island the fighter is: as high as the Gravity Pull holds one, at Ultron's chest. */
    private static final double IN_THE_AIR = 4.4;
    /** How high over it the hero flies. */
    private static final double FLYING = 3.0;

    /**
     * The island goes up; the fighter is put {@link #IN_THE_AIR} blocks over it and a flying hero {@link #FLYING} blocks
     * over it, both at rest. If {@code won}, the fight is won at once (nothing of Ultron fights on) and the island comes
     * down with them; the server stops then ({@code stop}: in the fall, or at once while the island is up if it was not
     * won), or the island lands.
     */
    private static void inTheAir(GameTestHelper helper, boolean won, boolean stop) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.EDITS_PER_TICK, 400);
        MinecraftServer server = f.level().getServer();
        ServerPlayer fighter = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z + 2.0);
        ServerPlayer flyer = f.player(MIDDLE_X + 4.0, GROUND, MIDDLE_Z - 3.0);
        flyer.getAbilities().mayfly = true;
        flyer.getAbilities().flying = true;
        flyer.onUpdateAbilities();
        tank(fighter);
        tank(flyer);
        UltronFight fight = UltronTestSupport.onTheGround(f, fighter);
        calm(fight);
        Vec3[] where = new Vec3[2];
        Script script = new Script();
        script.step("the flyer joins", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island is up", () -> {
            client(fighter, flyer);
            return fight.phase() == UltronFight.Phase.ISLAND && in(fight, SokoviaLift.Step.PLACED);
        });
        script.then("one is in the air over it, the other flies", () -> {
            if (won) {
                fight.end(BossEnd.VICTORY);
                check(fight.boss() != null && !fight.boss().brain().isEnabled() && fight.sentries().isEmpty(),
                        "the fight is won while a body still stands: nothing of his fights on (his mind is off, his Sentries are gone)");
            }
            fighter.setPos(fighter.position().add(0.0, IN_THE_AIR, 0.0));
            flyer.setPos(flyer.position().add(0.0, FLYING, 0.0));
            fighter.setDeltaMovement(Vec3.ZERO);
            flyer.setDeltaMovement(Vec3.ZERO);
        });
        script.step(won ? (stop ? "the island comes down with them" : "the island lands") : "the moment comes", () -> {
            client(fighter, flyer);
            if (!fight.isFinished()) {
                where[0] = rel(helper, fighter);
                where[1] = rel(helper, flyer);
            }
            if (!won) {
                return true;
            }
            return stop ? in(fight, SokoviaLift.Step.FALLING) && SokoviaLift.of(fight).altitude() < LIFT - 6.0 : fight.isFinished();
        });
        script.then(stop ? "the server stops" : "they are down", () -> {
            if (stop) {
                SokoviaLift.Lift lift = SokoviaLift.of(fight);
                assertNear(where[0].y - (GROUND + lift.altitude()), IN_THE_AIR, 0.6, "(the fighter is " + IN_THE_AIR + " blocks over the island)");
                check(lift.carried().contains(fighter.getUUID()) == won, won ? "(and the lift carries them down that high)" : "(and stands on nothing)");
                BossFights.stopAll(server, StopReason.SERVER_STOPPING);
                check(blocksInTheSky(helper) == 0 && fight.isFinished(), "the island is gone");
            }
            Vec3 now = rel(helper, fighter);
            assertNear(now.y, GROUND, 0.01, "the fighter is on the ground");
            assertNear(now.x, where[0].x, 0.01, "under where they were");
            assertNear(now.z, where[0].z, 0.01, "under where they were");
            Vec3 flying = rel(helper, flyer);
            assertNear(flying.y, GROUND + FLYING, 0.6, "the flying hero flies on at his height");
            assertNear(flying.x, where[1].x, 0.01, "over where he was");
            check(flyer.getAbilities().flying, "and still flies");
            for (ServerPlayer player : List.of(fighter, flyer)) {
                check(!player.isNoGravity() && !Powers.get(player).is(Live.LIFTED), player.getGameProfile().getName() + " is free");
            }
        });
        script.run(f, won ? 580 : 380);
    }

    /**
     * Whoever stands on a ledge of the island, their middle past its edge and their body still over it (the game lets
     * anyone stand so), is left on it, not dropped down the column under their middle: when the island lands, when the
     * server stops while it is up, and when the rise is cut short (where a fighter who was in the air at the lock is put
     * on the ground, as one in the air is at the other two).
     */
    @GameTest(template = PAD, batch = "ultron_lift_ledge_landing", timeoutTicks = 600)
    public static void theIslandLandsOneWhoStandsOnALedgeOnTheLedge(GameTestHelper helper) {
        onALedge(helper, Ledge.LANDING);
    }

    @GameTest(template = PAD, batch = "ultron_lift_ledge_placed", timeoutTicks = 400)
    public static void stoppingTheServerWhileTheIslandIsUpLeavesOneWhoStandsOnALedgeOnTheLedge(GameTestHelper helper) {
        onALedge(helper, Ledge.STOP_UP);
    }

    @GameTest(template = PAD, batch = "ultron_lift_ledge_rising", timeoutTicks = 400)
    public static void aRiseCutShortLeavesOneWhoStandsOnALedgeOnTheLedgeAndPutsOneWhoJumpedOnTheGround(GameTestHelper helper) {
        onALedge(helper, Ledge.RISE_CUT);
    }

    /** When the one on the ledge is set down: the island lands, the server stops while it is up, the rise is cut short. */
    private enum Ledge {
        LANDING, STOP_UP, RISE_CUT
    }

    /** The top of the ledge: a stone block 3 x 3 and 4 high on the pad (relative x 26 to 28, z 20 to 22). */
    private static final double LEDGE_TOP = GROUND + 4.0;
    /** Where one stands on it: their middle 0.2 past its west face (x = 26), their body (0.6 wide) over it by 0.1. */
    private static final double ON_THE_LEDGE_X = 25.8;
    /** How high over the ground a fighter who jumps is. */
    private static final double JUMP = 1.2;

    /**
     * A fighter stands on a ledge on the island, their middle past its edge; the island goes up, and at the {@code
     * moment} they are set down: they are on the ledge, free of it, as they stood (not four blocks down the column of
     * their middle, in its side), and the starter, in the middle, on the ground. At a cut-short rise the starter jumped at
     * the lock, so the lift took them in the air.
     */
    private static void onALedge(GameTestHelper helper, Ledge moment) {
        for (int x = 26; x <= 28; x++) {
            for (int z = 20; z <= 22; z++) {
                for (int y = SURFACE + 1; y < SURFACE + 5; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.EDITS_PER_TICK, 400);
        // (A lower cover over the ground, so that the island, with the ledge on it, still fits under the pad's ceiling.)
        f.config(UltronBossConfig.COVER_HEIGHT, 4);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer ledge = f.player(ON_THE_LEDGE_X, LEDGE_TOP, MIDDLE_Z);
        tank(starter);
        tank(ledge);
        UltronFight fight = UltronTestSupport.onTheGround(f, starter);
        calm(fight);
        Vec3 start = rel(helper, ledge);
        Vec3[] where = new Vec3[1];
        Script script = new Script();
        script.step("the one on the ledge joins", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            check(start.x < 26.0 && start.x + ledge.getBbWidth() / 2.0 > 26.0 && Math.abs(start.y - LEDGE_TOP) < 1.0E-6
                            && level.noCollision(ledge, ledge.getBoundingBox().deflate(0.001)),
                    "(they stand on the ledge with their middle past its edge, free of it): " + start);
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            if (moment == Ledge.RISE_CUT) {
                starter.setPos(starter.position().add(0.0, JUMP, 0.0));
            }
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the moment comes", () -> {
            client(starter, ledge);
            if (!fight.isOver()) {
                where[0] = rel(helper, starter);
            }
            switch (moment) {
                case LANDING -> {
                    if (fight.phase() == UltronFight.Phase.ISLAND && !fight.isOver()) {
                        fight.end(BossEnd.VICTORY);
                    }
                    return fight.isFinished();
                }
                case STOP_UP -> {
                    if (fight.phase() == UltronFight.Phase.ISLAND && in(fight, SokoviaLift.Step.PLACED)) {
                        BossFights.stopAll(server, StopReason.SERVER_STOPPING);
                        return true;
                    }
                    return false;
                }
                default -> {
                    if (in(fight, SokoviaLift.Step.RISING) && SokoviaLift.of(fight).altitude() > 5.0 && !fight.isOver()) {
                        check(SokoviaLift.of(fight).carried().contains(starter.getUUID()), "(the lift took the starter in the air)");
                        fight.end(BossEnd.COMMAND);
                    }
                    return fight.isFinished();
                }
            }
        });
        script.then("they are set down", () -> {
            check(blocksInTheSky(helper) == 0 && fight.isFinished(), "the island is gone");
            Vec3 now = rel(helper, ledge);
            assertNear(now.y, LEDGE_TOP, 0.01, "the one on the ledge is still on it (they stood at " + start + ")");
            assertNear(now.x, start.x, 0.01, "where they stood on it");
            assertNear(now.z, start.z, 0.01, "where they stood on it");
            Vec3 middle = rel(helper, starter);
            assertNear(middle.y, GROUND, 0.01, moment == Ledge.RISE_CUT ? "the one who jumped is on the ground" : "the starter is on the ground");
            assertNear(middle.x, where[0].x, 1.0, "the starter has not moved sideways");
            assertNear(middle.z, where[0].z, 1.0, "the starter has not moved sideways");
            for (ServerPlayer player : List.of(starter, ledge)) {
                String name = player.getGameProfile().getName();
                check(level.noCollision(player, player.getBoundingBox().deflate(0.001)), name + " is free of the blocks");
                check(!player.isNoGravity() && !Powers.get(player).is(Live.LIFTED), name + " is free");
            }
        });
        script.run(f, moment == Ledge.LANDING ? 580 : 380);
    }

    /**
     * The island is nearly down when the server stops, and someone stands on the real ground under it, inside its rim (a
     * passer-by; a fighter who logs in while it comes down is set there). Its blocks are gone by then, and in the last
     * five blocks of the fall the region the stop gathers reaches down to the real ground: they are left where they stand,
     * not set as far under it as the island is still up (into the ground).
     */
    @GameTest(template = PAD, batch = "ultron_lift_stop_settling", timeoutTicks = 600)
    public static void stoppingTheServerAsTheIslandSettlesLeavesWhoeverStandsUnderItWhereTheyStand(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.EDITS_PER_TICK, 400);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        UltronFight fight = UltronTestSupport.onTheGround(f, starter);
        calm(fight);
        Script script = new Script();
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the fight is won and the island is nearly down", () -> {
            client(starter);
            if (fight.phase() == UltronFight.Phase.ISLAND && !fight.isOver()) {
                fight.end(BossEnd.VICTORY);
            }
            return in(fight, SokoviaLift.Step.FALLING) && SokoviaLift.of(fight).altitude() < 5.0;
        });
        script.then("someone stands under it when the server stops", () -> {
            double altitude = SokoviaLift.of(fight).altitude();
            ServerPlayer passerBy = f.player(MIDDLE_X + 3.0, GROUND, MIDDLE_Z + 3.0);
            check(!fight.isParticipant(passerBy.getUUID()) && level.noCollision(passerBy, passerBy.getBoundingBox().deflate(0.001)),
                    "(they are not a fighter, and stand free on the ground)");
            Vec3 before = rel(helper, passerBy);
            BossFights.stopAll(server, StopReason.SERVER_STOPPING);
            check(blocksInTheSky(helper) == 0 && fight.isFinished(), "the island is gone");
            Vec3 now = rel(helper, passerBy);
            assertNear(now.y, GROUND, 0.01, "the one under it is on the ground where they stood (the island was " + altitude + " up)");
            assertNear(now.x, before.x, 0.01, "where they stood");
            assertNear(now.z, before.z, 0.01, "where they stood");
            check(level.noCollision(passerBy, passerBy.getBoundingBox().deflate(0.001)), "and not in the ground");
            assertNear(rel(helper, starter).y, GROUND, 0.01, "the fighter it carried is on the ground");
        });
        script.run(f, 580);
    }

    // ------------------------------------------------------------------------------------------------------------
    // A crash

    @GameTest(template = PAD, batch = "ultron_lift_crash_rising", timeoutTicks = 400)
    public static void aCrashWhileTheIslandRisesLeavesNothingToClear(GameTestHelper helper) {
        crash(helper, fight -> in(fight, SokoviaLift.Step.RISING) && SokoviaLift.of(fight).altitude() > 8.0, false, SokoviaJournal.RITUAL);
    }

    @GameTest(template = PAD, batch = "ultron_lift_crash_placing", timeoutTicks = 400)
    public static void aCrashWhileTheIslandIsBeingPlacedLeavesAHalfIslandThatIsCleared(GameTestHelper helper) {
        crash(helper, fight -> in(fight, SokoviaLift.Step.PLACING) && SokoviaLift.of(fight).job() != null && SokoviaLift.of(fight).job().edits() > 600,
                true, SokoviaJournal.PASTED);
    }

    @GameTest(template = PAD, batch = "ultron_lift_crash_placed", timeoutTicks = 400)
    public static void aCrashWhileTheIslandIsUpLeavesAnIslandThatIsCleared(GameTestHelper helper) {
        crash(helper, fight -> in(fight, SokoviaLift.Step.PLACED) && fight.phase() == UltronFight.Phase.ISLAND, true, SokoviaJournal.PASTED);
    }

    @GameTest(template = PAD, batch = "ultron_lift_crash_clearing", timeoutTicks = 600)
    public static void aCrashWhileTheIslandIsTakenDownLeavesWhatIsLeftToBeCleared(GameTestHelper helper) {
        crash(helper, fight -> in(fight, SokoviaLift.Step.CLEARING) && SokoviaLift.of(fight).job() != null && SokoviaLift.of(fight).job().edits() > 300,
                true, SokoviaJournal.CLEARING);
    }

    /**
     * The lift runs to a moment, then the process "dies" (the fight and the lift are thrown away with no clean-up), and
     * the next start recovers: it clears whatever of the island is in the sky, a slice a tick, pays back the Program,
     * gives everyone who was up there a record to be set down when they log in, and deletes the note. Then a player
     * logs in (still marked as carried, up in the sky) and is set down on the ground.
     *
     * @param note the state the journal says at that moment (before the island is placed it is the ritual's note, which
     *             only says who is owed the Program)
     */
    private static void crash(GameTestHelper helper, Predicate<UltronFight> when, boolean island, String note) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.EDITS_PER_TICK, 400);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        long ground = groundHash(helper);
        UltronFight fight = ritual(f, starter);
        boolean descent = SokoviaJournal.CLEARING.equals(note);
        Script script = new Script();
        script.then("the lift begins", () -> liftBegins(fight));
        if (descent) {
            script.step("the island goes up and the fight is won", () -> {
                client(starter);
                if (fight.phase() == UltronFight.Phase.ISLAND && !fight.isOver()) {
                    fight.end(BossEnd.VICTORY);
                }
                return fight.isOver();
            });
        }
        script.step("the moment comes", () -> {
            client(starter);
            return when.test(fight);
        });
        script.then("the process dies", () -> {
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            int placed = blocksInTheSky(helper);
            check((placed > 0) == island, "the island is in the sky or not, as the case wants: " + placed + " blocks");
            List<SokoviaJournal.Header> notes = SokoviaJournal.readAll(server);
            check(notes.size() == 1 && notes.get(0).state().equals(note), "the journal says " + note + ": " + notes.stream().map(SokoviaJournal.Header::state).toList());
            boolean ritual = SokoviaJournal.RITUAL.equals(note);
            if (ritual) {
                check(!island && notes.get(0).refund() && starter.getUUID().equals(notes.get(0).summoner()), "before the island is placed the note only says who is owed the Program");
            } else {
                check(notes.get(0).minY() <= abs(helper, 0, SURFACE + 8 + LIFT, 0).getY() && notes.get(0).members().contains(starter.getUUID()), "and lists the fighter");
                // A fight that was won has settled its Program (the reward is the Core): the note owes nothing.
                check(notes.get(0).refund() == !descent, "the note says whether the Program is still owed: " + notes.get(0).refund());
            }
            // The process dies: nothing is cleaned up, the journal stays, the world stays. (Letting the fight go here is
            // how the test throws it away; the note it deletes is put back, as a crash would have left it.)
            SokoviaLift.clear();
            BossFights.clear();
            for (SokoviaJournal.Header header : notes) {
                try {
                    SokoviaJournal.write(server, header);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            check(blocksInTheSky(helper) == placed, "nothing of the island was touched by the end of the fight");
            int began = SokoviaRecovery.recoverAll(server);
            check(began == (ritual ? 0 : 1), "the next start finds " + began + " islands to clear");
            check(lift != null, "(there was a lift)");
        });
        script.step("the next start clears the island", () -> SokoviaRecovery.pending() == 0 && blocksInTheSky(helper) == 0);
        script.then("and puts things right", () -> {
            check(groundHash(helper) == ground, "the ground is as it was");
            check(journals(level).isEmpty(), "the journal is deleted");
            // (A crash in the wind-down of a won fight gives no Program on top of the Core.)
            check(BossGifts.pending(server, starter.getUUID()).stream().anyMatch(stack -> stack.is(ModItems.ULTRON_PROGRAM.get())) == !descent,
                    descent ? "no Program is owed after a won fight" : "the Program is owed to the summoner");
            if (!SokoviaJournal.RITUAL.equals(note)) {
                SokoviaRelocations.Entry entry = SokoviaRelocations.of(server).of(starter.getUUID());
                check(entry != null && entry.down() == LIFT, "and the fighter has a record to be set down");
            }
            // The fighter logs in: saved up in the sky (and, if the process died while they were carried, still
            // marked).
            SokoviaRecovery.onLogin(starter);
            assertNear(rel(helper, starter).y, GROUND, 1.0, "the fighter is set down on the ground");
            check(!starter.isNoGravity() && !Powers.get(starter).is(Live.LIFTED), "and is free");
            check(SokoviaRelocations.of(server).of(starter.getUUID()) == null, "and the record is used up");
        });
        script.run(f, 380);
    }

    /**
     * The Program is used up by the ritual, so a crash on the ground (no island was ever placed) must not keep it: a note
     * written at the ritual says who is owed it, a crash leaves it, and the next start gives the Program back and
     * deletes the note. A fight that ends by itself leaves no note.
     */
    @GameTest(template = PAD, batch = "ultron_crash_ground", timeoutTicks = 300)
    public static void aCrashOnTheGroundGivesTheProgramBackAndAnEndedFightLeavesNoNote(GameTestHelper helper) throws java.io.IOException {
        TestKit.Fixture f = pad(helper);
        MinecraftServer server = f.level().getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        UltronFight fight = ritual(f, starter);
        calm(fight);
        List<SokoviaJournal.Header> notes = SokoviaJournal.readAll(server);
        check(notes.size() == 1 && notes.get(0).isRitual() && notes.get(0).fight().equals(fight.id()), "the ritual left a note");
        check(starter.getInventory().countItem(ModItems.ULTRON_PROGRAM.get()) == 0, "the Program is used up");
        // The process dies on the ground: the note stays; the next start gives the Program back.
        SokoviaLift.clear();
        BossFights.clear();
        check(journals(f.level()).isEmpty(), "(letting the fight go in the test deletes its note, as an end does)");
        SokoviaJournal.write(server, notes.get(0));
        check(SokoviaRecovery.recoverAll(server) == 0, "there is no island to clear");
        check(journals(f.level()).isEmpty(), "the note is deleted");
        check(BossGifts.pending(server, starter.getUUID()).stream().anyMatch(stack -> stack.is(ModItems.ULTRON_PROGRAM.get())), "and the Program is owed to the summoner");
        // A fight that ends by itself (an operator's stop: the Program comes back at once) leaves no note.
        UltronFight second = ritual(f, starter);
        calm(second);
        check(journals(f.level()).size() == 1, "the second ritual wrote its note");
        second.end(BossEnd.COMMAND);
        f.everyTick(200, tick -> {
            if (!second.isFinished()) {
                return false;
            }
            check(journals(f.level()).isEmpty(), "an ended fight leaves no note");
            check(starter.getInventory().countItem(ModItems.ULTRON_PROGRAM.get()) == 1, "and the Program came back to the starter");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Who was away

    /**
     * A fighter who is offline when the island goes: they get a record, and when they log in where they were saved, up
     * in the sky, they are set down on the ground; one who logs in somewhere else is left where they are. A record is
     * used up either way.
     */
    @GameTest(template = PAD, batch = "ultron_lift_away", timeoutTicks = 600)
    public static void aFighterWhoWasOfflineIsSetDownWhenTheyLogIn(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer away = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z + 2.0);
        ServerPlayer elsewhere = f.player(MIDDLE_X + 4.0, GROUND, MIDDLE_Z - 2.0);
        tank(starter);
        UltronFight fight = UltronTestSupport.onTheGround(f, starter);
        calm(fight);
        Script script = new Script();
        script.step("the fighters join", () -> fight.participants().size() == 3);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island is up", () -> {
            client(starter, away, elsewhere);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("two of them log out up there", () -> {
            SokoviaLift.onLogout(away);
            SokoviaLift.onLogout(elsewhere);
            check(!SokoviaLift.of(fight).carried().contains(away.getUUID()), "(they are not carried any more)");
            fight.end(BossEnd.VICTORY);
        });
        script.step("the island comes down", () -> {
            client(starter);
            return fight.isFinished();
        });
        script.then("and they logged in again", () -> {
            SokoviaRelocations data = SokoviaRelocations.of(server);
            check(data.of(away.getUUID()) != null && data.of(elsewhere.getUUID()) != null, "both have a record");
            check(data.of(starter.getUUID()) == null, "the one who stayed does not");
            // `away` is saved where the island was; `elsewhere` is saved somewhere else.
            Vec3 savedUp = helper.absoluteVec(new Vec3(MIDDLE_X - 4.0, GROUND + LIFT, MIDDLE_Z + 2.0));
            away.setPos(savedUp);
            elsewhere.setPos(helper.absoluteVec(new Vec3(MIDDLE_X + 14.0, GROUND, MIDDLE_Z)));
            Vec3 elsewhereAt = elsewhere.position();
            SokoviaRecovery.onLogin(away);
            SokoviaRecovery.onLogin(elsewhere);
            assertNear(rel(helper, away).y, GROUND, 1.0, "the one saved in the sky is on the ground");
            assertNear(rel(helper, away).x, MIDDLE_X - 4.0, 0.5, "below where they were");
            check(elsewhere.position().equals(elsewhereAt), "the other was left where they are");
            check(SokoviaRelocations.of(server).of(away.getUUID()) == null && SokoviaRelocations.of(server).of(elsewhere.getUUID()) == null, "both records are used up");
        });
        script.run(f, 580);
    }

    /**
     * A fighter who logs out while the island is still rising is saved in the air, between the ground and the sky,
     * where the island will never be. When they log in after the fight they are set down on the ground at once, in the
     * login itself (a client that takes a few seconds to load the terrain is kicked by the game for "flying" if it is
     * left hanging in the air; the real game did exactly that to a player in the soak test).
     */
    @GameTest(template = PAD, batch = "ultron_lift_away_rise_gone", timeoutTicks = 600)
    public static void aFighterWhoLeftDuringTheRiseIsSetDownWhenTheyLogInAfterTheFight(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        MinecraftServer server = f.level().getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer away = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z + 2.0);
        tank(starter);
        UltronFight fight = UltronTestSupport.onTheGround(f, starter);
        calm(fight);
        Vec3[] saved = new Vec3[1];
        Script script = new Script();
        script.step("the fighters join", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island is on its way up", () -> {
            client(starter, away);
            double altitude = SokoviaLift.of(fight).altitude();
            return in(fight, SokoviaLift.Step.RISING) && altitude > 8.0 && altitude < LIFT - 8.0;
        });
        script.then("one logs out in the air", () -> {
            saved[0] = away.position();
            SokoviaLift.onLogout(away);
            check(rel(helper, away).y > GROUND + 6.0, "(they are well above the ground)");
        });
        script.step("the island is up", () -> {
            client(starter);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("the fight ends", () -> fight.end(BossEnd.VICTORY));
        script.step("and the island is gone", () -> {
            client(starter);
            return fight.isFinished();
        });
        script.then("they log in where they were saved, in the air", () -> {
            check(SokoviaRelocations.of(server).of(away.getUUID()) != null, "there is a record for them");
            away.setPos(saved[0]);
            SokoviaRecovery.onLogin(away);
            SokoviaLift.onLogin(away);
            assertNear(rel(helper, away).y, GROUND, 1.0, "they are on the ground");
            assertNear(rel(helper, away).x, MIDDLE_X - 4.0, 1.0, "under where they were");
            check(SokoviaRelocations.of(server).of(away.getUUID()) == null, "and the record is used up");
        });
        script.run(f, 580);
    }

    /**
     * The same fighter, logging in while the island is up: they are put on it, not left hanging under it.
     */
    @GameTest(template = PAD, batch = "ultron_lift_away_rise_up", timeoutTicks = 600)
    public static void aFighterWhoLeftDuringTheRiseIsPutOnTheIslandWhenTheyLogInWhileItIsUp(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer away = f.player(MIDDLE_X - 4.0, GROUND, MIDDLE_Z + 2.0);
        tank(starter);
        UltronFight fight = UltronTestSupport.onTheGround(f, starter);
        calm(fight);
        Vec3[] saved = new Vec3[1];
        Script script = new Script();
        script.step("the fighters join", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island is on its way up", () -> {
            client(starter, away);
            double altitude = SokoviaLift.of(fight).altitude();
            return in(fight, SokoviaLift.Step.RISING) && altitude > 8.0 && altitude < LIFT - 8.0;
        });
        script.then("one logs out in the air", () -> {
            saved[0] = away.position();
            SokoviaLift.onLogout(away);
        });
        script.step("the island is up", () -> {
            client(starter);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("they log in where they were saved, in the air under it", () -> {
            away.setPos(saved[0]);
            SokoviaRecovery.onLogin(away);
            SokoviaLift.onLogin(away);
            assertNear(rel(helper, away).y, GROUND + LIFT, 1.0, "they are on the island");
            check(SokoviaLift.of(fight).islanders().contains(away.getUUID()), "and they are one of its islanders");
        });
        script.run(f, 580);
    }

    // ------------------------------------------------------------------------------------------------------------
    // What is not the island's

    /**
     * What a hero's ability put on the island when the arena's protection could not stop it (Mjolnir set down as a
     * block, a block placed by hand) is not lost when the island is taken down: Mjolnir is lifted with its owner and
     * dropped, the other block drops what it would, and the island's own blocks vanish.
     */
    @GameTest(template = PAD, batch = "ultron_lift_foreign", timeoutTicks = 700)
    public static void blocksThatAreNotTheIslandsAreDroppedAsItemsWhenItComesDown(GameTestHelper helper) {
        TestKit.Fixture f = pad(helper);
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        tank(starter);
        UltronFight fight = UltronTestSupport.onTheGround(f, starter);
        calm(fight);
        ItemStack hammer = new ItemStack(ModItems.MJOLNIR.get());
        Worthiness.claim(starter, hammer);
        BlockPos mjolnir = abs(helper, 25, SURFACE + 1 + LIFT, 21);
        BlockPos table = abs(helper, 17, SURFACE + 1 + LIFT, 21);
        Script script = new Script();
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island is up", () -> {
            client(starter);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("the fight is won with two blocks on the island that are not its own", () -> {
            check(MjolnirBlock.setDown(level, mjolnir, hammer), "Mjolnir is set down on the island");
            level.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            fight.end(BossEnd.VICTORY);
        });
        script.step("the island comes down", () -> {
            client(starter);
            return fight.isFinished();
        });
        int[] waited = {0};
        script.step("and what fell is lying on the ground", () -> {
            AABB box = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(43, 45, 43);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);
            boolean table1 = items.stream().anyMatch(item -> item.getItem().is(Items.CRAFTING_TABLE));
            boolean hammer1 = items.stream().anyMatch(item -> ItemStack.isSameItemSameComponents(item.getItem(), hammer))
                    || level.getBlockStates(box).anyMatch(state -> state.getBlock() instanceof MjolnirBlock);
            if (++waited[0] > 100) {
                check(table1 && hammer1, "the table " + table1 + ", Mjolnir " + hammer1 + "; items: " + items.stream().map(item -> item.getItem().toString()).toList());
            }
            return table1 && hammer1 && blocksInTheSky(helper) == 0;
        });
        script.run(f, 680);
    }
}
