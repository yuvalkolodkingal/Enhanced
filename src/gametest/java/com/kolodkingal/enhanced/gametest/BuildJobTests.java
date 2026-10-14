package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildEvents;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.BuildScheduler.Budget;
import com.kolodkingal.enhanced.build.BuildSession;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.UndoHistory;
import com.kolodkingal.enhanced.build.UndoStep;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.ENDLESS;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.boxSpell;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.count;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.set;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.steps;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Build Magic's engine (D-B 19.5, spec 18.4 B68-B76): a spell sliced by writes, sections and time; the pace that makes
 * every spell grow; fair shares between builders and the Hex boost; only the chunks loaded at the cast; Ctrl + Z on
 * waiting and working spells; a spell that outlives its caster; Adventure or Spectator stopping it; the server's stop;
 * and an undo that leaves later changes alone.
 *
 * <p>Jobs, histories and sessions are server-wide, and {@code TestKit.bossFixture} forgets them all when a test ends, so
 * every test has a batch of its own ({@code build_jobs_*}). Each builder's jobs are kept out of the server's ticks
 * ({@code BuildScheduler.manual}) and stepped by the test with a budget of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BuildJobTests {

    // B68

    /**
     * A 19 x 8 x 19 fill (2,888) with a budget of 200 writes and the pace of 1 tick writes 200 after the first step and
     * every cell by the fifteenth, never more than 200 in a tick (its settling and its end follow at the next); a
     * vanish of a 19 x 1 x 19 layer with a budget of 2 sections changes at most 2 sections a step (and spans 4); with
     * the clock reporting 60 ms ticks, a step of a 200-write budget writes 100.
     */
    @GameTest(template = ARENA, batch = "build_jobs_sliced", timeoutTicks = 200)
    public static void spellsAreSlicedByWritesSectionsAndTime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(1, 2, 1, 19, 9, 19);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        check(job != null, "the fill is at work");
        Budget writes = Budget.of(200, 4096, 1);
        int steps = 0;
        while (job.written() < 2888 && steps < 100) {
            BuildScheduler.step(List.of(wanda.getUUID()), writes);
            steps++;
            if (steps == 1) {
                check(job.written() == 200, "200 cells after the first step: " + job.written());
            }
        }
        check(steps == 15 && job.written() == 2888, "every cell by the fifteenth step: " + steps + " steps, " + job.written());
        check(job.mostInATick() <= 200, "never more than 200 in a tick: " + job.mostInATick());
        finish(writes, wanda.getUUID());
        check(job.finished() && job.endCode() == BuildVerdictPayload.DONE && count(f, box, Blocks.STONE) == 2888,
                "the fill is done: " + job.endCode() + ", " + count(f, box, Blocks.STONE));

        BoundingBox layer = new BoundingBox(1, 9, 1, 19, 9, 19);
        cast(wanda, boxSpell(f, Spell.VANISH, layer));
        BuildJob melt = working(wanda);
        Budget sections = Budget.of(1_000_000, 2, 1);
        int sliced = 0;
        while (working(wanda) == melt && sliced < 100) {
            BuildScheduler.Tick tick = BuildScheduler.step(List.of(wanda.getUUID()), sections);
            check(tick.sectionsChanged() <= 2, "at most 2 new sections a step: " + tick.sectionsChanged());
            sliced++;
        }
        check(melt.step().sectionKeys().length >= 4 && sliced >= 2 && count(f, layer, Blocks.AIR) == 361,
                "the layer of " + melt.step().sectionKeys().length + " sections vanished in " + sliced + " steps");

        BoundingBox rest = new BoundingBox(1, 2, 1, 19, 8, 19);
        cast(wanda, boxSpell(f, Spell.VANISH, rest));
        BuildJob slow = working(wanda);
        BuildScheduler.step(List.of(wanda.getUUID()), writes.withClock(System::nanoTime, () -> 60_000_000L));
        check(slow.written() == 100, "a slow server halves the writes: " + slow.written());
        helper.succeed();
    }

    // B69

    /** 27 cells with the pace of 10 ticks are not done after 8 ticks, and are done by the tenth. */
    @GameTest(template = ARENA, batch = "build_jobs_pace")
    public static void smallSpellsStillTakeMinSpellTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 4, 7);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        Budget pace = Budget.of(20_000, 4096, 10);
        for (int tick = 1; tick <= 8; tick++) {
            BuildScheduler.step(List.of(wanda.getUUID()), pace);
        }
        check(!job.finished() && job.written() == 24, "after 8 ticks it still grows: " + job.written() + " of 27");
        BuildScheduler.step(List.of(wanda.getUUID()), pace);
        BuildScheduler.step(List.of(wanda.getUUID()), pace);
        check(job.finished() && count(f, box, Blocks.STONE) == 27, "done by the tenth tick: " + count(f, box, Blocks.STONE));
        helper.succeed();
    }

    // B69a

    /**
     * The cast and the hum are the caster's (D-B 7.3, joint check finding 8): the cast sound is played where she stands,
     * not at the middle of the box, so that a fill whose middle is far from the corner she stands at is heard by her; the
     * hum of the working spell is heard at her too (every 20 ticks, the front being within 16 of her), and so is her undo.
     */
    @GameTest(template = ARENA, batch = "build_jobs_sounds")
    public static void theCastAndTheHumAreHeardAtTheCaster(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 19, 4, 19);
        TestKit.heardSounds(wanda);
        // (A sound packet keeps a position to an eighth of a block, in single precision: far from the origin, to half a block.)
        cast(wanda, boxSpell(f, Spell.FILL, box));
        List<TestKit.HeardSound> heard = TestKit.heardSounds(wanda);
        check(heard.stream().anyMatch(sound -> sound.id().getPath().equals("build_cast_wanda") && sound.at().distanceTo(wanda.position()) < 1.0),
                "the cast is heard where she stands (" + wanda.position() + "), not at the box's middle: " + heard);
        Budget pace = Budget.of(20_000, 4096, 30);
        for (int tick = 1; tick <= 22; tick++) {
            BuildScheduler.step(List.of(wanda.getUUID()), pace);
        }
        heard = TestKit.heardSounds(wanda);
        check(heard.stream().anyMatch(sound -> sound.id().getPath().equals("build_hum"))
                && heard.stream().filter(sound -> sound.id().getPath().equals("build_hum")).allMatch(sound -> sound.at().distanceTo(wanda.position()) < 1.0),
                "the hum of her working spell is heard, always at her: " + heard);
        finish(wanda);
        TestKit.heardSounds(wanda);
        BuildMagic.undo(wanda);
        heard = TestKit.heardSounds(wanda);
        check(heard.stream().anyMatch(sound -> sound.id().getPath().equals("build_undo") && sound.at().distanceTo(wanda.position()) < 1.0),
                "and so is her undo: " + heard);
        helper.succeed();
    }

    // B70

    /**
     * Two Scarlet Witches' 1,000-cell fills under a 400-write budget each write 200 in the first step; a third
     * builder's 50-cell spell (pace 1) joins and finishes in its first step; a Scarlet Witch inside her own Hex gets 200
     * to the other's 100 under 300.
     */
    @GameTest(template = ARENA, batch = "build_jobs_fair")
    public static void twoBuildersShareFairly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer first = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.STONE);
        ServerPlayer second = builder(f, Power.SCARLET_WITCH, 20.5, 20.5, Blocks.STONE);
        ServerPlayer third = builder(f, Power.SCARLET_WITCH, 10.5, 20.5, Blocks.STONE);
        cast(first, boxSpell(f, Spell.FILL, new BoundingBox(0, 2, 0, 9, 6, 19)));
        cast(second, boxSpell(f, Spell.FILL, new BoundingBox(11, 2, 0, 20, 6, 19)));
        BuildJob a = working(first);
        BuildJob b = working(second);
        List<UUID> both = List.of(first.getUUID(), second.getUUID());
        BuildScheduler.step(both, Budget.of(400, 4096, 1));
        check(a.written() == 200 && b.written() == 200, "each writes 200 of 400: " + a.written() + ", " + b.written());

        cast(third, boxSpell(f, Spell.FILL, new BoundingBox(0, 7, 0, 4, 8, 4)));
        BuildJob small = working(third);
        check(small != null && small.plan() != null && small.plan().size() == 50, "the small spell of 50");
        BuildScheduler.step(List.of(first.getUUID(), second.getUUID(), third.getUUID()), Budget.of(400, 4096, 1));
        check(small.finished() && small.endCode() == BuildVerdictPayload.DONE, "the small spell finishes in its first step: " + small.endCode() + " " + small.phase() + " "
                + small.written() + " " + small.walked() + " " + a.written() + " " + b.written());

        // The Hex boost: the first Scarlet Witch raises her Hex round her spell.
        HexManager.create(first);
        check(a.weight() == BuildConfig.WANDA_HEX_BOOST.get() && b.weight() == 1.0, "her spell has the boost: " + a.weight() + ", " + b.weight());
        long beforeA = a.written();
        long beforeB = b.written();
        BuildScheduler.step(both, Budget.of(300, 4096, 1));
        check(a.written() - beforeA == 200 && b.written() - beforeB == 100,
                "200 to 100 under 300: " + (a.written() - beforeA) + ", " + (b.written() - beforeB));
        BuildScheduler.step(List.of(second.getUUID(), first.getUUID()), Budget.of(300, 4096, 1));
        check(a.written() - beforeA == 400 && b.written() - beforeB == 200, "whoever goes first");
        helper.succeed();
    }

    // B71

    /**
     * With the "loaded" predicate false for one chunk at the cast, the cells of that chunk are untouched and counted
     * {@code left.unloaded}, no ticket is ever added for it, and the job's tickets are all gone when it ends.
     */
    @GameTest(template = ARENA, batch = "build_jobs_chunks")
    public static void aSpellTouchesOnlyTheChunksLoadedAtCast(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(1, 2, 1, 19, 2, 19);
        BlockPos far = abs(f, 19, 2, 19);
        int fx = SectionPos.blockToSectionCoord(far.getX());
        int fz = SectionPos.blockToSectionCoord(far.getZ());
        BiPredicate<Integer, Integer> loaded = (x, z) -> !(x == fx && z == fz) && level.hasChunk(x, z);
        BuildScheduler.manual(wanda.getUUID(), loaded);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        check(job != null && !job.chunks().contains(ChunkPos.asLong(fx, fz)), "the chunk not loaded is not the spell's");
        finish(wanda);
        int outside = 0;
        int untouched = 0;
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 1, 2, 1), far)) {
            if (SectionPos.blockToSectionCoord(pos.getX()) == fx && SectionPos.blockToSectionCoord(pos.getZ()) == fz) {
                outside++;
                untouched += level.getBlockState(pos).isAir() ? 1 : 0;
            } else {
                check(level.getBlockState(pos).is(Blocks.STONE), "every other cell is filled: " + pos);
            }
        }
        check(outside > 0 && untouched == outside && job.left(Left.UNLOADED) == outside,
                "its " + outside + " cells untouched and counted: " + untouched + ", " + job.left(Left.UNLOADED));
        LongSet ticketed = job.everTicketed();
        check(!ticketed.isEmpty() && !ticketed.contains(ChunkPos.asLong(fx, fz)), "no ticket was ever added for it: " + ticketed.size());
        check(job.ticketed().isEmpty(), "and every ticket is gone");
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DONE_PARTLY && seen.said("message.enhanced.build.left.unloaded")
                && seen.arg("message.enhanced.build.left.unloaded", 0).equals(String.valueOf(outside)), "and the builder is told: " + seen);
        helper.succeed();
    }

    // B72

    /**
     * Three casts of a Scarlet Witch: one works, two wait ({@code BuildData.queued} 2); Ctrl + Z drops the newest
     * waiting one, which never happens. Then her 1,000-cell fill stepped at 256 writes for 2 steps, and Ctrl + Z: it
     * stops, every cell is back, the job is gone, and "Spell stopped and taken back" is said.
     */
    @GameTest(template = ARENA, batch = "build_jobs_ctrl_z")
    public static void ctrlZDropsTheWaitingThenStopsAndRewinds(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 20.5, 20.5, Blocks.STONE);
        BoundingBox first = new BoundingBox(11, 2, 1, 13, 2, 3);
        BoundingBox second = new BoundingBox(15, 2, 1, 17, 2, 3);
        BoundingBox dropped = new BoundingBox(15, 3, 1, 17, 3, 3);
        cast(wanda, boxSpell(f, Spell.FILL, first));
        cast(wanda, boxSpell(f, Spell.FILL, second));
        cast(wanda, boxSpell(f, Spell.FILL, dropped));
        check(BuildScheduler.jobs(wanda.getUUID()).size() == 3 && wanda.getData(ModAttachments.BUILD_DATA).queued() == 2,
                "one at work, two waiting: " + wanda.getData(ModAttachments.BUILD_DATA).queued());
        Seen.of(wanda);
        BuildMagic.undo(wanda);
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DROPPED && seen.said("message.enhanced.build.dropped")
                && wanda.getData(ModAttachments.BUILD_DATA).queued() == 1, "Ctrl + Z drops the newest waiting spell: " + seen);
        finish(wanda);
        check(count(f, first, Blocks.STONE) == 9 && count(f, second, Blocks.STONE) == 9, "the other two are done");
        check(count(f, dropped, Blocks.AIR) == 9, "nothing of the dropped spell ever happened");

        BoundingBox big = new BoundingBox(0, 2, 0, 9, 6, 19);
        cast(wanda, boxSpell(f, Spell.FILL, big));
        BuildJob fill = working(wanda);
        Budget budget = Budget.of(256, 4096, 1);
        BuildScheduler.step(List.of(wanda.getUUID()), budget);
        BuildScheduler.step(List.of(wanda.getUUID()), budget);
        check(fill.written() == 512 && count(f, big, Blocks.STONE) == 512, "the big fill wrote 512: " + fill.written());
        Seen.of(wanda);
        BuildMagic.undo(wanda);
        check(fill.finished() && working(wanda) != fill, "Ctrl + Z stops it");
        BuildJob rewind = working(wanda);
        check(rewind != null && rewind.kind() == BuildJob.Kind.UNDO, "and its undo runs at once");
        finish(wanda);
        check(count(f, big, Blocks.AIR) == 1000, "every cell is back: " + count(f, big, Blocks.AIR));
        seen = Seen.of(wanda);
        check(seen.said("message.enhanced.build.stopped") && rewind.endCode() == BuildVerdictPayload.STOPPED, "Spell stopped and taken back: " + seen);
        check(BuildScheduler.jobs(wanda.getUUID()).isEmpty(), "the job is gone");
        helper.succeed();
    }

    // B73

    /**
     * A Scarlet Witch casts and logs out: her spell finishes with a fake player of her profile, the step is in her
     * history, and her next login says "While you were away, your spell finished".
     */
    @GameTest(template = ARENA, batch = "build_jobs_away")
    public static void aSpellOutlivesItsCaster(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        MinecraftServer server = f.level().getServer();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        UUID id = wanda.getUUID();
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 4, 7);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        BuildScheduler.step(List.of(id), Budget.of(5, 4096, 1));
        server.getPlayerList().remove(wanda);
        check(server.getPlayerList().getPlayer(id) == null && !BuildSession.of(id).mode(), "she has left, and her build mode with her");
        finish(ENDLESS, id);
        check(job.finished() && job.endCode() == BuildVerdictPayload.DONE && count(f, box, Blocks.STONE) == 27,
                "the spell finished without her: " + job.endCode() + ", " + count(f, box, Blocks.STONE));
        check(UndoHistory.of(id).steps().size() == 1 && UndoHistory.of(id).steps().get(0).changed() == 27, "and it is in her history");
        ServerPlayer back = f.player(0.5, TestKit.FLOOR, 0.5, GameType.SURVIVAL, id);
        Seen seen = Seen.of(back);
        check(seen.said("message.enhanced.build.finished_away") && seen.arg("message.enhanced.build.finished_away", 0).equals("27"),
                "her next login says it: " + seen);
        helper.succeed();
    }

    // B74

    /**
     * A Scarlet Witch switched to Adventure (and another to Spectator) in the middle of her spell: build mode ends, the
     * spell stops at its next step, and what it did is one undoable step.
     */
    @GameTest(template = ARENA, batch = "build_jobs_mode")
    public static void adventureOrSpectatorMidSpellStopsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        for (GameType mode : List.of(GameType.ADVENTURE, GameType.SPECTATOR)) {
            int z = mode == GameType.ADVENTURE ? 1 : 11;
            ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 20.5, z + 0.5, Blocks.STONE);
            BoundingBox box = new BoundingBox(1, 2, z, 9, 4, z + 8);
            cast(wanda, boxSpell(f, Spell.FILL, box));
            BuildJob job = working(wanda);
            BuildScheduler.step(List.of(wanda.getUUID()), Budget.of(50, 4096, 1));
            wanda.setGameMode(mode);
            check(!BuildSession.of(wanda.getUUID()).mode() && !wanda.getData(ModAttachments.BUILD_VIEW).active(), mode + ": build mode ends");
            BuildScheduler.step(List.of(wanda.getUUID()), Budget.of(50, 4096, 1));
            check(job.finished() && job.endCode() == BuildVerdictPayload.STOPPED && job.written() == 50,
                    mode + ": the spell stops at its next step: " + job.endCode() + ", " + job.written());
            check(count(f, box, Blocks.STONE) == 50, mode + ": what it did stays");
            List<UndoStep> history = steps(wanda);
            check(history.size() == 1 && history.get(0).changed() == 50 && history.get(0).undoable(), mode + ": one undoable step");
            wanda.setGameMode(GameType.SURVIVAL);
            BuildMagic.undo(wanda);
            finish(wanda);
            check(count(f, box, Blocks.AIR) == box.getXSpan() * box.getYSpan() * box.getZSpan(), mode + ": and it is undone");
        }
        helper.succeed();
    }

    // B75

    /**
     * The server stops ({@code BuildEvents.onServerStopping}, {@code finishOnStopBlocks} 500): with spells of 50 and
     * 1,000 cells left, the 50 is finished, the 1,000 interrupted and undoable; with three of 300 left, one is
     * finished and two interrupted, the total finished within the cap (R55).
     */
    @GameTest(template = ARENA, batch = "build_stop")
    public static void serverStopFinishesSmallSpellsAndInterruptsBigOnes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.FINISH_ON_STOP_BLOCKS, 500);
        MinecraftServer server = f.level().getServer();
        ServerPlayer small = builder(f, Power.SCARLET_WITCH, 20.5, 0.5, Blocks.STONE);
        ServerPlayer big = builder(f, Power.SCARLET_WITCH, 20.5, 20.5, Blocks.STONE);
        BoundingBox fifty = new BoundingBox(1, 2, 1, 5, 3, 5);
        BoundingBox thousand = new BoundingBox(0, 4, 0, 9, 8, 19);
        cast(small, boxSpell(f, Spell.FILL, fifty));
        cast(big, boxSpell(f, Spell.FILL, new BoundingBox(10, 2, 0, 19, 6, 19)));
        BuildJob fiftyJob = working(small);
        BuildJob bigJob = working(big);
        BuildScheduler.step(List.of(big.getUUID()), Budget.of(100, 4096, 1));
        check(bigJob.remaining() == 900 && fiftyJob.remaining() == 50, "50 and 900 left: " + fiftyJob.remaining() + ", " + bigJob.remaining());
        BuildEvents.onServerStopping(new ServerStoppingEvent(server));
        check(fiftyJob.finished() && fiftyJob.endCode() == BuildVerdictPayload.DONE && count(f, fifty, Blocks.STONE) == 50, "the 50 is finished");
        check(bigJob.finished() && bigJob.endCode() == BuildVerdictPayload.STOPPED && bigJob.written() == 100, "the big one is interrupted");
        List<UndoStep> history = steps(big);
        check(history.size() == 1 && history.get(0).state() == UndoStep.State.INTERRUPTED && history.get(0).undoable(),
                "its step is sealed interrupted and can be undone");
        check(BuildSession.of(big.getUUID()).takeLoginLines().stream().anyMatch(line -> Seen.key(line).equals("message.enhanced.build.interrupted")),
                "and its builder hears of it at his next login");

        List<ServerPlayer> three = new ArrayList<>();
        List<BuildJob> jobs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5 + i, 20.5, Blocks.STONE);
            cast(wanda, boxSpell(f, Spell.FILL, new BoundingBox(i * 3, 4, 0, i * 3 + 2, 8, 19)));
            three.add(wanda);
            jobs.add(working(wanda));
        }
        check(jobs.stream().allMatch(job -> job.remaining() == 300), "three spells of 300 left");
        BuildEvents.onServerStopping(new ServerStoppingEvent(server));
        long done = jobs.stream().filter(job -> job.endCode() == BuildVerdictPayload.DONE).count();
        long stopped = jobs.stream().filter(job -> job.endCode() == BuildVerdictPayload.STOPPED).count();
        long finishedCells = jobs.stream().filter(job -> job.endCode() == BuildVerdictPayload.DONE).mapToLong(BuildJob::written).sum();
        check(done == 1 && stopped == 2 && finishedCells == 300 && finishedCells <= 500,
                "one finished, two interrupted, within the cap: " + done + ", " + stopped + ", " + finishedCells);
        check(BuildScheduler.all().isEmpty(), "nothing is left at work");
        check(count(f, thousand, Blocks.STONE) == 300, "only the finished one built");
        helper.succeed();
    }

    // B76

    /**
     * A Scarlet Witch fills 27; another player sets one cell to glass; her undo puts back 26 and says
     * {@code kept_changed} 1. A chest she stamped and someone then filled is left standing by her undo, with what it
     * holds.
     */
    @GameTest(template = ARENA, batch = "build_jobs_safe_undo")
    public static void undoLeavesLaterChangesAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 4, 7);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        finish(wanda);
        check(count(f, box, Blocks.STONE) == 27, "27 filled");
        level.setBlockAndUpdate(abs(f, 6, 4, 6), Blocks.GLASS.defaultBlockState());
        Seen.of(wanda);
        BuildMagic.undo(wanda);
        finish(wanda);
        Seen seen = Seen.of(wanda);
        check(count(f, box, Blocks.AIR) == 26 && count(f, box, Blocks.GLASS) == 1, "26 back, the glass stays");
        check(seen.said("message.enhanced.build.kept_changed") && seen.arg("message.enhanced.build.kept_changed", 0).equals("1")
                && seen.code() == BuildVerdictPayload.UNDONE && seen.verdict().changed() == 26, "kept_changed 1: " + seen);

        // A chest copied, stamped, then filled by someone: her undo leaves it.
        BlockPos source = abs(f, 2, 2, 15);
        level.setBlockAndUpdate(source, Blocks.CHEST.defaultBlockState());
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(source, source).at(source, Direction.UP).build());
        finish(wanda);
        seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.COPIED && seen.said("message.enhanced.build.copied"), "the chest is copied: " + seen);
        BlockPos floor = abs(f, 12, 1, 15);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(floor, Direction.UP).build());
        finish(wanda);
        BlockPos stamped = floor.above();
        check(level.getBlockState(stamped).is(Blocks.CHEST), "the chest is stamped on the floor: " + level.getBlockState(stamped));
        ((ChestBlockEntity) level.getBlockEntity(stamped)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        BuildMagic.undo(wanda);
        finish(wanda);
        seen = Seen.of(wanda);
        check(level.getBlockState(stamped).is(Blocks.CHEST) && ((ChestBlockEntity) level.getBlockEntity(stamped)).getItem(0).getCount() == 3,
                "the filled chest is left standing with its diamonds");
        check(seen.said("message.enhanced.build.kept_changed"), "and said: " + seen);
        check(level.getBlockState(source).is(Blocks.CHEST), "the copied chest never moved");
        helper.succeed();
    }

    private BuildJobTests() {
    }
}
