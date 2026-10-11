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
     * Plays the fight to a moment ({@code when}, with the fight won as soon as the island is up if {@code win}), then
     * stops the server: the island is cleared and everyone is on the ground where they stood, flags and journal and
     * lift are gone, all in the same tick, and the Program that started the fight is waiting for the summoner as a
     * gift.
     */
    private static void stopWhen(GameTestHelper helper, Predicate<UltronFight> when, boolean win) {
        TestKit.Fixture f = pad(helper);
        f.config(UltronBossConfig.EDITS_PER_TICK, 400);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = f.player(MIDDLE_X, GROUND, MIDDLE_Z);
        ServerPlayer other = f.player(MIDDLE_X + 4.0, GROUND, MIDDLE_Z - 3.0);
        tank(starter);
        long ground = groundHash(helper);
        UltronFight fight = ritual(f, starter);
        calm(fight);
        Vec3[] where = new Vec3[2];
        Script script = new Script();
        script.step("the second fighter joins", () -> fight.participants().size() == 2);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
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

    // ------------------------------------------------------------------------------------------------------------
    // A crash

    @GameTest(template = PAD, batch = "ultron_lift_crash_rising", timeoutTicks = 400)
    public static void aCrashWhileTheIslandRisesLeavesNothingToClear(GameTestHelper helper) {
        crash(helper, fight -> in(fight, SokoviaLift.Step.RISING) && SokoviaLift.of(fight).altitude() > 8.0, false, null);
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
     * @param note the state the journal says at that moment, or null if there is none
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
        calm(fight);
        boolean descent = SokoviaJournal.CLEARING.equals(note);
        Script script = new Script();
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
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
            if (note == null) {
                check(journals(level).isEmpty(), "no journal was written before the island was placed");
            } else {
                List<SokoviaJournal.Header> notes = SokoviaJournal.readAll(server);
                check(notes.size() == 1 && notes.get(0).state().equals(note), "the journal says " + note + ": " + notes.stream().map(SokoviaJournal.Header::state).toList());
                check(notes.get(0).minY() <= abs(helper, 0, SURFACE + 8 + LIFT, 0).getY() && notes.get(0).members().contains(starter.getUUID()), "and lists the fighter");
            }
            // The process dies: nothing is cleaned up, the journal stays, the world stays.
            SokoviaLift.clear();
            BossFights.clear();
            check(blocksInTheSky(helper) == placed, "nothing of the island was touched by the end of the fight");
            int began = SokoviaRecovery.recoverAll(server);
            check(began == (note == null ? 0 : 1), "the next start finds " + began + " journals");
            check(lift != null, "(there was a lift)");
        });
        script.step("the next start clears the island", () -> SokoviaRecovery.pending() == 0 && blocksInTheSky(helper) == 0);
        script.then("and puts things right", () -> {
            check(groundHash(helper) == ground, "the ground is as it was");
            check(journals(level).isEmpty(), "the journal is deleted");
            if (note != null) {
                check(BossGifts.pending(server, starter.getUUID()).stream().anyMatch(stack -> stack.is(ModItems.ULTRON_PROGRAM.get())),
                        "the Program is owed to the summoner");
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
