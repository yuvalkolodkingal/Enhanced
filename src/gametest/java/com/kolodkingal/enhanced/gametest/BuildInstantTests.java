package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.BuildScheduler.Budget;
import com.kolodkingal.enhanced.build.BuildSession;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.UndoStep;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.boxSpell;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.count;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.listen;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.steps;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Wanda's Instant level (3.2): with her Instant switch on in build mode, a spell, an undo or a redo is written whole in
 * the tick it is cast (no pace, no slicing, no budget share, a flash instead of the sweeping ripple), up to
 * {@code build_magic_wanda.instantMaxBlocks}; above it, or when {@code instantMaxMillis} runs out, it goes at normal speed
 * and she is told; every rule of the cells still holds; Billy has no such switch.
 *
 * <p>Jobs, histories and sessions are server-wide, so every test has a batch of its own ({@code build_instant_*}).
 */
public final class BuildInstantTests {
    /** A 19 x 8 x 19 box: 2,888 cells. */
    private static final BoundingBox BIG = new BoundingBox(1, 2, 1, 19, 9, 19);

    /**
     * The switch is hers: on, it shows in her synced data; her fill of 2,888 cells is all written, the job over and the
     * step kept, when the cast returns, in the same game tick; the client is sent the instant pop (and no progress); with
     * the switch off the same fill has written nothing yet.
     */
    @EnhancedTest(template = ARENA, batch = "build_instant_cast", timeoutTicks = 200)
    public static void anInstantSpellIsWrittenInTheTickItIsCast(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        long tick = f.level().getGameTime();

        // Off: the normal pace (a first step writes only the pace's share).
        cast(wanda, boxSpell(f, Spell.FILL, BIG));
        BuildJob slow = working(wanda);
        check(slow != null && slow.written() == 0 && !slow.instant(), "off: the fill waits for the server's ticks");
        finish(wanda);
        BuildMagic.undo(wanda);
        finish(wanda);
        check(count(f, BIG, Blocks.STONE) == 0, "(undone again)");

        BuildMagic.setInstant(wanda, true);
        check(BuildSession.of(wanda.getUUID()).instant() && ModAttachments.BUILD_DATA.get(wanda).instant(), "the switch is on, and her data says so");
        check(Seen.of(wanda).said("message.enhanced.build.instant_on"), "she is told it is on");
        cast(wanda, boxSpell(f, Spell.FILL, BIG));
        check(working(wanda) == null, "the fill is not at work any more: it is done");
        check(count(f, BIG, Blocks.STONE) == 2888, "all 2,888 cells are stone: " + count(f, BIG, Blocks.STONE));
        check(f.level().getGameTime() == tick, "in the same game tick");
        List<UndoStep> kept = steps(wanda);
        check(kept.size() == 1 && kept.get(0).changed() == 2888, "one undo step of 2,888 cells: " + kept.size());
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DONE, "told it is done: " + seen);
        check(seen.fx.stream().map(BuildFxPayload::kind).toList().equals(List.of(BuildFxPayload.START, BuildFxPayload.DONE))
                && seen.fx.stream().allMatch(fx -> fx.order() == BuildFxPayload.POP), "the client is sent START and DONE with the pop, no progress: "
                + seen.fx.stream().map(fx -> fx.kind() + "/" + fx.order()).toList());

        // Turning it off again, with the key's action.
        BuildMagic.action(wanda, BuildActionPayload.INSTANT, BlockPos.ZERO, BlockPos.ZERO, 0);
        check(!BuildSession.of(wanda.getUUID()).instant() && !ModAttachments.BUILD_DATA.get(wanda).instant(), "the action turns it off");
        check(Seen.of(wanda).said("message.enhanced.build.instant_off"), "and she is told");
        BuildMagic.action(wanda, BuildActionPayload.INSTANT, BlockPos.ZERO, BlockPos.ZERO, 1);
        check(BuildSession.of(wanda.getUUID()).instant() && ModAttachments.BUILD_DATA.get(wanda).instant(), "and on");

        // Leaving build mode turns it off.
        BuildMagic.setMode(wanda, false);
        check(!BuildSession.of(wanda.getUUID()).instant() && !ModAttachments.BUILD_DATA.get(wanda).instant(), "leaving build mode switches it off");
        BuildMagic.setInstant(wanda, true);
        check(!BuildSession.of(wanda.getUUID()).instant(), "and it cannot be switched on outside build mode");
        helper.succeed();
    }

    /**
     * Above {@code instantMaxBlocks} the spell goes at normal speed and she is told how big it is and what the most is; a
     * spell within it is instant at the same time; with {@code instantMaxBlocks} 0 the switch is refused and she is told.
     */
    @EnhancedTest(template = ARENA, batch = "build_instant_limit", timeoutTicks = 200)
    public static void aSpellAboveTheLimitGoesAtNormalSpeed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.WANDA_INSTANT_MAX_BLOCKS, 1000);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BuildMagic.setInstant(wanda, true);
        Seen.of(wanda);

        BoundingBox small = new BoundingBox(1, 2, 1, 9, 6, 9); // 405 cells
        cast(wanda, boxSpell(f, Spell.FILL, small));
        check(working(wanda) == null && count(f, small, Blocks.STONE) == 405, "405 cells are under the limit: instant");
        Seen small405 = Seen.of(wanda);
        check(!small405.said("message.enhanced.build.instant_big"), "and she is not told anything is too big");

        cast(wanda, boxSpell(f, Spell.FILL, BIG));
        BuildJob job = working(wanda);
        check(job != null && !job.instant() && job.written() == 0, "2,888 cells are above the limit: the spell waits for the ticks");
        Seen seen = Seen.of(wanda);
        check(seen.said("message.enhanced.build.instant_big") && seen.arg("message.enhanced.build.instant_big", 0).equals("2888")
                && seen.arg("message.enhanced.build.instant_big", 1).equals("1000"), "she is told: 2,888 blocks, the most 1,000: " + seen);
        check(seen.fx.stream().noneMatch(fx -> fx.order() == BuildFxPayload.POP), "and it grows with the usual ripple, not the pop");
        Budget writes = Budget.of(200, 4096, 1);
        BuildScheduler.step(List.of(wanda.getUUID()), writes);
        check(job.written() > 0 && job.written() <= 200 && !job.finished(), "it is sliced as always: " + job.written());
        finish(writes, wanda.getUUID());
        check(job.finished() && count(f, BIG, Blocks.STONE) == 2888, "and it ends all the same");

        // Off for the whole server.
        f.config(BuildConfig.WANDA_INSTANT_MAX_BLOCKS, 0);
        check(Limits.of(wanda).instantMax() == 0, "0 is off");
        BuildMagic.setInstant(wanda, false);
        Seen.of(wanda);
        BuildMagic.setInstant(wanda, true);
        check(!BuildSession.of(wanda.getUUID()).instant() && Seen.of(wanda).said("message.enhanced.build.instant_disabled"),
                "the switch is refused, and she is told why");
        helper.succeed();
    }

    /**
     * Her undo and her redo are instant too while the switch is on: one call and every cell is back (and gone again); with
     * the switch off the undo waits for the ticks; an undo of more cells than the limit goes at normal speed.
     */
    @EnhancedTest(template = ARENA, batch = "build_instant_undo", timeoutTicks = 200)
    public static void undoAndRedoAreInstantToo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BuildMagic.setInstant(wanda, true);
        cast(wanda, boxSpell(f, Spell.FILL, BIG));
        check(count(f, BIG, Blocks.STONE) == 2888 && working(wanda) == null, "the instant fill is there");
        Seen.of(wanda);

        BuildMagic.undo(wanda);
        check(working(wanda) == null && count(f, BIG, Blocks.STONE) == 0, "one undo and it is all gone: " + count(f, BIG, Blocks.STONE));
        Seen undone = Seen.of(wanda);
        check(undone.verdicts.stream().anyMatch(v -> v.code() == BuildVerdictPayload.UNDONE) && undone.fx.stream().allMatch(fx -> fx.order() == BuildFxPayload.POP),
                "told it is undone, with the pop: " + undone);
        BuildMagic.redo(wanda);
        check(working(wanda) == null && count(f, BIG, Blocks.STONE) == 2888, "one redo and it is all back: " + count(f, BIG, Blocks.STONE));

        // Off: the undo is sliced.
        BuildMagic.setInstant(wanda, false);
        BuildMagic.undo(wanda);
        BuildJob slow = working(wanda);
        check(slow != null && !slow.instant() && count(f, BIG, Blocks.STONE) == 2888, "with the switch off the undo waits for the ticks");
        finish(wanda);
        check(count(f, BIG, Blocks.STONE) == 0, "and ends all the same");

        // On, but the undo has more cells than the limit allows.
        BuildMagic.setInstant(wanda, true);
        f.config(BuildConfig.WANDA_INSTANT_MAX_BLOCKS, 1000);
        Seen.of(wanda);
        BuildMagic.redo(wanda);
        BuildJob big = working(wanda);
        check(big != null && !big.instant() && Seen.of(wanda).said("message.enhanced.build.instant_big"),
                "a redo of 2,888 cells is above 1,000: normal speed, and she is told");
        finish(wanda);
        check(count(f, BIG, Blocks.STONE) == 2888, "and it ends");
        helper.succeed();
    }

    /**
     * Every rule still holds: a claim mod that refuses the place event of half the box leaves that half empty; a chunk that
     * is not loaded is left, whole, and counted; the arena of a running protected fight is left alone.
     */
    @EnhancedTest(template = ARENA, batch = "build_instant_rules", timeoutTicks = 200)
    public static void protectedAreasAndUnloadedChunksAreStillLeftAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BuildMagic.setInstant(wanda, true);

        // A claim: the cells with x >= 10 (arena) belong to someone else.
        BoundingBox world = abs(f, BIG);
        int claimX = abs(f, 10, 2, 1).getX();
        listen(f, BlockEvent.EntityPlaceEvent.class, event -> {
            if (world.isInside(event.getPos()) && event.getPos().getX() >= claimX) {
                event.setCanceled(true);
            }
        });
        // A chunk that is not loaded: the one of the arena's cell 4, 2, 4 (the spell's chunks are those loaded when it is cast).
        ChunkPos dark = new ChunkPos(abs(f, 4, 2, 4));
        BuildScheduler.manual(wanda.getUUID(), (x, z) -> x != dark.x || z != dark.z);
        cast(wanda, boxSpell(f, Spell.FILL, BIG));
        check(working(wanda) == null, "the spell is done in the cast");
        int expected = 0;
        int darkCells = 0;
        for (BlockPos pos : BlockPos.betweenClosed(world.minX(), world.minY(), world.minZ(), world.maxX(), world.maxY(), world.maxZ())) {
            boolean claimed = pos.getX() >= claimX;
            boolean unloaded = new ChunkPos(pos).equals(dark);
            boolean stone = level.getBlockState(pos).is(Blocks.STONE);
            check(stone == (!claimed && !unloaded), "cell " + pos.toShortString() + (claimed ? " (claimed)" : "") + (unloaded ? " (unloaded)" : "")
                    + " is " + level.getBlockState(pos).getBlock());
            expected += !claimed && !unloaded ? 1 : 0;
            darkCells += unloaded && !claimed ? 1 : 0;
        }
        check(expected > 0 && expected < 2888 && darkCells > 0, "some cells were written, some not: " + expected + " of 2888, " + darkCells + " unloaded");
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DONE_PARTLY, "told it was done partly: " + seen);
        helper.succeed();
    }

    /** Billy has no Instant level: the switch says it is hers, stays off, and his spell goes at the usual pace. */
    @EnhancedTest(template = ARENA, batch = "build_instant_billy", timeoutTicks = 200)
    public static void billyCannotUseInstant(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        check(Limits.of(billy).instantMax() == 0, "his limits have no instant size");
        Seen.of(billy);
        BuildMagic.setInstant(billy, true);
        check(!BuildSession.of(billy.getUUID()).instant() && !ModAttachments.BUILD_DATA.get(billy).instant(), "the switch stays off");
        check(Seen.of(billy).said("message.enhanced.build.instant_wanda"), "he is told it is the Scarlet Witch's alone");
        BuildMagic.action(billy, BuildActionPayload.INSTANT, BlockPos.ZERO, BlockPos.ZERO, 1);
        check(!BuildSession.of(billy.getUUID()).instant(), "the action does not turn it on either");

        BoundingBox box = new BoundingBox(1, 2, 1, 5, 4, 5); // 75 cells, within his side
        cast(billy, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(billy);
        check(job != null && !job.instant() && count(f, box, Blocks.STONE) < 75, "his fill still grows over the ticks");
        finish(billy);
        check(count(f, box, Blocks.STONE) == 75, "and ends");

        // Wanda's switch does not touch his session, and a power change takes it away from her.
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 6.5, 0.5, Blocks.STONE);
        BuildMagic.setInstant(wanda, true);
        check(BuildSession.of(wanda.getUUID()).instant() && !BuildSession.of(billy.getUUID()).instant(), "hers is hers alone");
        BuildMagic.onPowerChanged(wanda, Power.SCARLET_WITCH, Power.BILLY);
        check(!BuildSession.of(wanda.getUUID()).instant() && !ModAttachments.BUILD_DATA.get(wanda).instant(), "a change of power switches it off");
        helper.succeed();
    }

    /**
     * The tick's time is the safety net: with {@code instantMaxMillis} at its least (100) a spell of 512,000 cells cannot be
     * written in one go; it stops writing when the time is up, she is told, and the rest goes at normal speed to the end.
     */
    @EnhancedTest(template = ARENA, batch = "build_instant_time", timeoutTicks = 400)
    public static void aSpellThatTakesTooLongFinishesAtNormalSpeed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.WANDA_INSTANT_MAX_MILLIS, 100);
        f.config(BuildConfig.WANDA_INSTANT_MAX_BLOCKS, 100_000_000);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BuildMagic.setInstant(wanda, true);
        BlockPos o = abs(f, 0, 0, 0);
        BoundingBox box = new BoundingBox(o.getX() + 24, 200, o.getZ() + 24, o.getX() + 103, 279, o.getZ() + 103);
        for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++) {
            for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) {
                level.getChunk(cx, cz);
            }
        }
        Seen.of(wanda);
        SpellRecipe fill = recipe(Spell.FILL).box(box).at(new BlockPos(box.minX(), box.minY(), box.minZ()), Direction.UP).build();
        BuildMagic.cast(wanda, fill, 1, BuildCastPayload.HELD_SHORT | BuildCastPayload.HELD_LONG);
        BuildJob job = working(wanda);
        Seen seen = Seen.of(wanda);
        check(job != null && !job.instant() && !job.finished(), "the spell is still at work: it took more than 100 ms");
        check(job.written() > 0 && job.written() < 512_000, "it wrote some, not all: " + job.written());
        check(seen.said("message.enhanced.build.instant_slow"), "she is told it was too slow for Instant: " + seen);
        finish(wanda);
        check(job.finished() && job.endCode() == BuildVerdictPayload.DONE && job.written() == 512_000, "and the rest is written at normal speed: " + job.written());
        // (put the sky back as it was: the next test's world)
        BuildMagic.undo(wanda);
        finish(wanda);
        helper.succeed();
    }
}
