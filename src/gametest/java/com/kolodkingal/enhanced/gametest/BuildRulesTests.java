package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.UndoStep;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestArena;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestLane;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import com.kolodkingal.enhanced.ultron.SokoviaRecovery;
import com.kolodkingal.enhanced.ultron.SokoviaRelocations;
import com.kolodkingal.enhanced.ultron.SokoviaShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.boxSpell;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.count;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.set;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.steps;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Build Magic's rule book (D-B 19.4, spec 18.4 B55-B67): the claims asked once per refused section and before every
 * write, a preflight that posts nothing, the boss arenas (protected, unprotected, a fight that begins over a spell), an
 * undo into a place protected since, the Sokovia recovery, Adventure, the world border and a stand-in for spawn
 * protection, other players with PvP off and on, full chests spilled when asked, a Hex as a claim, the denied
 * dimensions.
 *
 * <p>Every test has a batch of its own ({@code build_rules_*} and the batches of 18.4): the jobs, histories and sessions
 * are server-wide, the fights and the border too, and several change a setting. A claim mod is a listener that cancels
 * the events, as in {@code UltronRitualTests}; it refuses whole 16-block sections of x where a test needs a section
 * refused, because claims are probed once per section (P9) and GameTest arenas do not sit on section lines.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BuildRulesTests {
    /** The middle of the test floor: every test fight's arena is a circle of radius 8 round it. */
    private static final Vec3 CENTRE = new Vec3(10.5, FLOOR, 10.5);

    /** A test fight round the middle of the floor (radius 8), protected or not, started by {@code starter}. */
    private static TestFight fight(TestKit.Fixture f, ServerPlayer starter, BossKind kind, boolean protectBlocks) {
        BossArena arena = new TestArena(f.level(), f.helper().absoluteVec(CENTRE), 8.0, BossArena.Containment.NONE, protectBlocks)
                .containAt(6.0).joinWithin(9.0).tractorDepth(0.0);
        TestFight fight = new TestFight(f.level(), kind, arena);
        BossFights.register(kind, new TestLane(kind, player -> fight));
        check(BossFights.start(kind, starter, false), "the test fight starts");
        return fight;
    }

    /** The first x (in the world) of a 16-block section that starts inside the arena's columns 2 to 17. */
    private static int sectionLine(TestKit.Fixture f) {
        int from = abs(f, 1, 0, 0).getX();
        return ((from >> 4) + 1) << 4;
    }

    /** Adds a listener for the test's length. */
    private static <T extends net.neoforged.bus.api.Event> void listen(TestKit.Fixture f, Class<T> type, Consumer<T> listener) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, listener);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(listener));
    }

    // B55

    /**
     * A claim refuses every break from a 16-block section line of x on: the Scarlet Witch's Vanish over the row leaves
     * the refused cells as they were and empties the rest; the claim was asked exactly once for each section of the
     * refused cells; {@code left.protected} counts them.
     */
    @GameTest(template = ARENA, batch = "build_rules_claims")
    public static void claimsAreAskedOncePerRefusedSection(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BoundingBox box = new BoundingBox(1, 2, 1, 19, 3, 3);
        set(f, box, Blocks.STONE);
        int line = sectionLine(f);
        BoundingBox world = abs(f, box);
        Map<Long, Integer> asked = new HashMap<>();
        listen(f, BlockEvent.BreakEvent.class, event -> {
            BlockPos pos = event.getPos();
            if (world.isInside(pos) && pos.getX() >= line) {
                asked.merge(SectionPos.asLong(pos), 1, Integer::sum);
                event.setCanceled(true);
            }
        });
        cast(wanda, boxSpell(f, Spell.VANISH, box));
        BuildJob job = working(wanda);
        finish(wanda);
        Set<Long> sections = new HashSet<>();
        int refused = 0;
        for (BlockPos pos : BlockPos.betweenClosed(world.minX(), world.minY(), world.minZ(), world.maxX(), world.maxY(), world.maxZ())) {
            if (pos.getX() >= line) {
                check(level.getBlockState(pos).is(Blocks.STONE), "a refused cell is untouched: " + pos);
                refused++;
                sections.add(SectionPos.asLong(pos));
            } else {
                check(level.getBlockState(pos).isAir(), "an allowed cell vanished: " + pos);
            }
        }
        check(asked.keySet().equals(sections) && asked.values().stream().allMatch(n -> n == 1),
                "asked once for each of the " + sections.size() + " refused sections: " + asked);
        check(job.left(Left.PROTECTED) == refused, "counted: " + job.left(Left.PROTECTED) + " of " + refused);
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DONE_PARTLY && seen.arg("message.enhanced.build.left.protected", 0).equals(String.valueOf(refused)),
                "and said: " + seen);
        helper.succeed();
    }

    // B56

    /**
     * A counting listener sees 27 place events and no break event for a 27-cell fill over air, and 27 of each over
     * stone; a place listener reading the cell sees the old block (air), so every event comes before its write.
     */
    @GameTest(template = ARENA, batch = "build_rules_events")
    public static void everyCellAsksTheClaimEventsBeforeTheWrite(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 4, 7);
        BoundingBox world = abs(f, box);
        int[] breaks = {0};
        int[] places = {0};
        List<BlockState> seenAtPlace = new ArrayList<>();
        listen(f, BlockEvent.BreakEvent.class, event -> {
            if (world.isInside(event.getPos())) {
                breaks[0]++;
            }
        });
        listen(f, BlockEvent.EntityPlaceEvent.class, event -> {
            if (world.isInside(event.getPos())) {
                places[0]++;
                seenAtPlace.add(level.getBlockState(event.getPos()));
            }
        });
        cast(wanda, boxSpell(f, Spell.FILL, box));
        finish(wanda);
        check(count(f, box, Blocks.STONE) == 27 && places[0] == 27 && breaks[0] == 0, "over air: 27 place events, no break: " + places[0] + ", " + breaks[0]);
        check(seenAtPlace.size() == 27 && seenAtPlace.stream().allMatch(BlockState::isAir), "each listener saw the cell still empty");
        breaks[0] = 0;
        places[0] = 0;
        seenAtPlace.clear();
        wanda.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_PLANKS));
        cast(wanda, boxSpell(f, Spell.FILL, box));
        finish(wanda);
        check(count(f, box, Blocks.OAK_PLANKS) == 27 && places[0] == 27 && breaks[0] == 27, "over stone: 27 of each: " + places[0] + ", " + breaks[0]);
        check(seenAtPlace.stream().allMatch(state -> state.is(Blocks.STONE)), "each listener saw the old stone");
        helper.succeed();
    }

    // B57

    /**
     * Billy's preflight of a fill across the edge of a running fight's arena, over a chest holding a diamond and a
     * player standing there: counting listeners see no event, the world and his meter are as before, and the marks
     * name the chest (keeps), the player (a body) and the arena's cells (protected).
     */
    @GameTest(template = ARENA, batch = "build_rules_preview")
    public static void aPreflightPostsNothingAndChangesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(10.5, FLOOR, 12.5);
        ServerPlayer billy = builder(f, Power.BILLY, 10.5, 6.5, Blocks.STONE);
        ServerPlayer other = f.player(19.5, FLOOR, 1.5);
        BlockPos chest = abs(f, 1, 2, 1);
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND));
        fight(f, starter, BossKind.ULTRON, true);
        BoundingBox box = new BoundingBox(0, 2, 0, 20, 3, 4);
        BoundingBox world = abs(f, box);
        int[] events = {0};
        listen(f, BlockEvent.BreakEvent.class, event -> events[0]++);
        listen(f, BlockEvent.EntityPlaceEvent.class, event -> events[0]++);
        Map<BlockPos, BlockState> before = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(world.minX(), world.minY(), world.minZ(), world.maxX(), world.maxY(), world.maxZ())) {
            before.put(pos.immutable(), level.getBlockState(pos));
        }
        BuildData meter = ModAttachments.BUILD_DATA.get(billy);
        BuildMagic.useClock(() -> 0L); // (its 2 ms are not this test's: a busy test server must not cut it short)
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, box), BuildCastPayload.PREFLIGHT);
        Seen seen = Seen.of(billy);
        BuildVerdictPayload verdict = seen.verdict();
        check(verdict != null && verdict.code() == BuildVerdictPayload.READY_PARTLY, "a verdict, ready in part: " + seen);
        check(events[0] == 0, "no event was posted: " + events[0]);
        check(before.entrySet().stream().allMatch(entry -> level.getBlockState(entry.getKey()) == entry.getValue()), "the world is as it was");
        check(ModAttachments.BUILD_DATA.get(billy).meter() == meter.meter(), "his meter is untouched");
        check(seen.bar.isEmpty(), "and nothing is said");
        boolean keeps = false;
        boolean body = false;
        boolean arena = false;
        for (int i = 0; i < verdict.marks().length; i++) {
            BlockPos mark = verdict.mark(i);
            byte kind = verdict.kind(i);
            keeps |= mark.equals(chest) && kind == BuildVerdictPayload.MARK_KEEPS;
            body |= mark.getX() == abs(f, 19, 2, 1).getX() && mark.getZ() == abs(f, 19, 2, 1).getZ() && kind == BuildVerdictPayload.MARK_BODY;
            arena |= kind == BuildVerdictPayload.MARK_PROTECTED && BossFights.protects(level, mark);
        }
        check(keeps && body && arena, "the marks name the chest, the player and the arena: " + keeps + ", " + body + ", " + arena + " (flags "
                + verdict.flags() + ")");
        int flags = verdict.flags();
        check((flags & BuildVerdictPayload.PROTECTED) != 0 && (flags & BuildVerdictPayload.KEEPS) != 0 && (flags & BuildVerdictPayload.BODIES) != 0,
                "and so do the flags: " + flags);
        check(other.isAlive(), "(the other player stands there)");
        helper.succeed();
    }

    // B57a

    /**
     * A box wholly inside a protected fight's arena (joint check finding 6): the preflight answers
     * {@code NOTHING_CAN_CHANGE} with the marks (the ghost goes grey), and the cast is refused with the same code, the
     * line of what was left and the fizzle, before any job, cast sound, effect, charge on Billy's meter or history step;
     * a box half in the arena is still cast in part.
     */
    @GameTest(template = ARENA, batch = "build_rules_nothing_can_change")
    public static void aSpellWhoseEveryCellIsProtectedFizzlesBeforeItIsCast(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerPlayer starter = f.player(10.5, FLOOR, 12.5);
        ServerPlayer billy = builder(f, Power.BILLY, 10.5, 6.5, Blocks.STONE);
        fight(f, starter, BossKind.ULTRON, true);
        BoundingBox inside = new BoundingBox(9, 2, 9, 11, 2, 11);
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 9, 2, 9), abs(f, 11, 2, 11))) {
            check(BossFights.inAnyArena(f.level(), pos), "the box lies in the arena: " + pos);
        }
        BuildData before = ModAttachments.BUILD_DATA.get(billy);
        BuildMagic.useClock(() -> 0L);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, inside), BuildCastPayload.PREFLIGHT);
        Seen seen = Seen.of(billy);
        BuildVerdictPayload answer = seen.verdict();
        check(answer != null && answer.code() == BuildVerdictPayload.NOTHING_CAN_CHANGE && answer.changed() == 0 && answer.marks().length > 0
                && (answer.flags() & BuildVerdictPayload.PROTECTED) != 0 && BuildVerdictPayload.isRefusal(answer.code()),
                "the preflight says nothing can change, with its marks: " + seen);
        check(seen.bar.isEmpty() && !seen.heard("build_fizzle"), "and says and sounds nothing: " + seen);

        cast(billy, boxSpell(f, Spell.FILL, inside));
        seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.NOTHING_CAN_CHANGE && seen.said("message.enhanced.build.left.protected")
                && seen.arg("message.enhanced.build.left.protected", 0).equals("9"), "the cast is refused with what was left: " + seen);
        check(seen.heard("build_fizzle") && !seen.heard("build_cast_billy") && seen.fx.isEmpty(), "it fizzles, with no cast sound and no effect: "
                + seen.sounds + ", " + seen.fx);
        check(working(billy) == null && steps(billy).isEmpty() && count(f, inside, Blocks.STONE) == 0, "no job, no step, no block");
        BuildData after = ModAttachments.BUILD_DATA.get(billy);
        check(after.meter() == before.meter() && after.practice() == before.practice(), "his meter and practice are as they were: " + after);

        BoundingBox across = new BoundingBox(0, 3, 0, 20, 3, 4);
        cast(billy, boxSpell(f, Spell.FILL, across), BuildCastPayload.PREFLIGHT);
        check(Seen.of(billy).code() == BuildVerdictPayload.READY_PARTLY, "a box half in the arena is ready in part");
        helper.succeed();
    }

    /**
     * The 2 ms a cast reads its preview by are wall-clock time (D-B 10.1): on a busy machine the preview of a bigger plan
     * is cut off, and a cut-off preview says nothing of the cells it did not reach, so it cannot refuse the cast. The
     * spell is then queued like any other and still ends with its own verdict (what was left, counted), never with
     * none; with a steady clock the same fill is refused before it is cast.
     */
    @GameTest(template = ARENA, batch = "build_rules_cut_off")
    public static void aPreviewCutOffByItsClockStillEndsInTheSpellsVerdict(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerPlayer starter = f.player(10.5, FLOOR, 12.5);
        ServerPlayer billy = builder(f, Power.BILLY, 10.5, 6.5, Blocks.STONE);
        fight(f, starter, BossKind.ULTRON, true);
        BoundingBox inside = new BoundingBox(7, 2, 7, 13, 3, 11);
        int cells = 7 * 2 * 5;
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 7, 2, 7), abs(f, 13, 3, 11))) {
            check(BossFights.inAnyArena(f.level(), pos), "the box lies in the arena: " + pos);
        }

        // A clock that is 3 ms later at every look: the preview's 2 ms are over at its first look.
        long[] now = {0L};
        BuildMagic.useClock(() -> now[0] += 3_000_000L);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, inside), BuildCastPayload.PREFLIGHT);
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.READY_PARTLY && (seen.verdict().flags() & BuildVerdictPayload.CUT_OFF) != 0,
                "the preflight is cut off and says so: " + seen);
        cast(billy, boxSpell(f, Spell.FILL, inside));
        seen = Seen.of(billy);
        check(working(billy) != null && seen.verdicts.isEmpty(), "a cut-off preview cannot refuse, so the spell is cast and waits: " + seen);
        finish(billy);
        seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.DONE_PARTLY && seen.said("message.enhanced.build.left.protected")
                && seen.arg("message.enhanced.build.left.protected", 0).equals(String.valueOf(cells)),
                "it ends with its verdict, every cell counted protected: " + seen);
        check(working(billy) == null && count(f, inside, Blocks.STONE) == 0 && steps(billy).isEmpty(), "and it built nothing");

        BuildMagic.useClock(() -> 0L);
        ModAttachments.BUILD_DATA.set(billy, ModAttachments.BUILD_DATA.get(billy).withPauseUntil(0L)); // (the pause after his spell)
        cast(billy, boxSpell(f, Spell.FILL, inside));
        seen = Seen.of(billy);
        check(working(billy) == null && seen.code() == BuildVerdictPayload.NOTHING_CAN_CHANGE, "with a steady clock it is refused up front: " + seen);
        helper.succeed();
    }

    // B58

    /**
     * A protected test fight: a fill across its arena's edge changes only the cells outside, and no
     * {@code boss.protected} line is sent; an unprotected (Thanos-like) arena is left alone too; once the fight stops,
     * the same fill changes them.
     */
    @GameTest(template = ARENA, batch = "build_rules_arenas")
    public static void spellsLeaveBossArenasAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerLevel level = f.level();
        ServerPlayer starter = f.player(10.5, FLOOR, 12.5);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.STONE);
        BoundingBox lower = new BoundingBox(0, 2, 0, 20, 2, 4);
        BoundingBox upper = new BoundingBox(0, 3, 0, 20, 3, 4);
        fight(f, starter, BossKind.ULTRON, true);
        int inside = filledOutsideOnly(f, wanda, lower);
        check(inside > 0, "the protected arena's cells stay empty: " + inside);
        BossFights.clear();

        fight(f, starter, BossKind.THANOS, false);
        check(filledOutsideOnly(f, wanda, upper) == inside, "so do the unprotected arena's");
        BossFights.clear();

        cast(wanda, boxSpell(f, Spell.FILL, lower));
        cast(wanda, boxSpell(f, Spell.FILL, upper));
        finish(wanda);
        check(count(f, lower, Blocks.STONE) == 105 && count(f, upper, Blocks.STONE) == 105, "after the fights the same fills change them");
        Seen seen = Seen.of(wanda);
        check(!seen.said("message.enhanced.boss.protected"), "and no boss.protected line was ever sent");
        check(level.getBlockState(abs(f, 10, 2, 4)).is(Blocks.STONE), "(an arena cell)");
        helper.succeed();
    }

    /** Fills {@code box} while a fight runs: the cells in its arena stay empty, the rest are stone; how many stayed. */
    private static int filledOutsideOnly(TestKit.Fixture f, ServerPlayer wanda, BoundingBox box) {
        ServerLevel level = f.level();
        cast(wanda, boxSpell(f, Spell.FILL, box));
        finish(wanda);
        BoundingBox world = abs(f, box);
        int inside = 0;
        for (BlockPos pos : BlockPos.betweenClosed(world.minX(), world.minY(), world.minZ(), world.maxX(), world.maxY(), world.maxZ())) {
            if (BossFights.inAnyArena(level, pos)) {
                check(level.getBlockState(pos).isAir(), "an arena cell is left: " + pos);
                inside++;
            } else {
                check(level.getBlockState(pos).is(Blocks.STONE), "a cell outside the arena is filled: " + pos);
            }
        }
        return inside;
    }

    // B59

    /** With {@code allArenasProtected} off, the unprotected arena is built in; the protected one still is not. */
    @GameTest(template = ARENA, batch = "build_arenas")
    public static void unprotectedArenasOpenWhenAsked(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(BuildConfig.ALL_ARENAS_PROTECTED, false);
        ServerPlayer starter = f.player(10.5, FLOOR, 12.5);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.STONE);
        BoundingBox lower = new BoundingBox(0, 2, 0, 20, 2, 4);
        BoundingBox upper = new BoundingBox(0, 3, 0, 20, 3, 4);
        fight(f, starter, BossKind.THANOS, false);
        cast(wanda, boxSpell(f, Spell.FILL, lower));
        finish(wanda);
        check(count(f, lower, Blocks.STONE) == 105, "the unprotected arena is built in: " + count(f, lower, Blocks.STONE));
        BossFights.clear();
        fight(f, starter, BossKind.ULTRON, true);
        check(filledOutsideOnly(f, wanda, upper) > 0, "the protected one still is not");
        helper.succeed();
    }

    // B60

    /**
     * A 2,888-cell fill stepped at 200 writes; after three steps a protected fight starts over its box: the next step
     * stops the spell with {@code stopped_fight}; the 600 cells written stay; after the fight, an undo puts them back.
     */
    @GameTest(template = ARENA, batch = "build_rules_fight")
    public static void aFightThatStartsStopsTheSpell(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        ServerPlayer starter = f.player(10.5, FLOOR, 0.5);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(1, 2, 1, 19, 9, 19);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        com.kolodkingal.enhanced.build.BuildScheduler.Budget budget = com.kolodkingal.enhanced.build.BuildScheduler.Budget.of(200, 4096, 1);
        for (int i = 0; i < 3; i++) {
            com.kolodkingal.enhanced.build.BuildScheduler.step(List.of(wanda.getUUID()), budget);
        }
        check(job.written() == 600, "600 written: " + job.written());
        fight(f, starter, BossKind.ULTRON, true);
        Seen.of(wanda);
        com.kolodkingal.enhanced.build.BuildScheduler.step(List.of(wanda.getUUID()), budget);
        check(job.finished() && job.endCode() == BuildVerdictPayload.STOPPED_FIGHT && job.written() == 600,
                "the next step stops it: " + job.endCode() + ", " + job.written());
        Seen seen = Seen.of(wanda);
        check(seen.said("message.enhanced.build.stopped_fight") && seen.arg("message.enhanced.build.stopped_fight", 0).equals("600"),
                "A boss fight began here: " + seen);
        check(count(f, box, Blocks.STONE) == 600, "the 600 cells stay");
        BossFights.clear();
        BuildMagic.undo(wanda);
        finish(wanda);
        check(count(f, box, Blocks.AIR) == 2888, "after the fight an undo puts them back: " + count(f, box, Blocks.AIR));
        helper.succeed();
    }

    // B61

    /**
     * A claim refusing 9 of 27 filled cells (the bottom layer) during the undo: 18 are put back and the step stays on
     * top, partly undone; with the claim gone, Ctrl + Z puts back the 9.
     */
    @GameTest(template = ARENA, batch = "build_rules_partly")
    public static void undoIntoAProtectedPlaceIsPartlyUndoneAndRetried(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 4, 7);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        finish(wanda);
        BoundingBox bottom = abs(f, new BoundingBox(5, 2, 5, 7, 2, 7));
        boolean[] claimed = {true};
        listen(f, BlockEvent.BreakEvent.class, event -> {
            if (claimed[0] && bottom.isInside(event.getPos())) {
                event.setCanceled(true);
            }
        });
        Seen.of(wanda);
        BuildMagic.undo(wanda);
        finish(wanda);
        Seen seen = Seen.of(wanda);
        UndoStep step = steps(wanda).get(steps(wanda).size() - 1);
        check(count(f, box, Blocks.AIR) == 18 && count(f, box, Blocks.STONE) == 9, "18 put back: " + count(f, box, Blocks.AIR));
        check(step.state() == UndoStep.State.PARTLY && step.applied() == 9, "the step is partly undone: " + step.state() + ", " + step.applied());
        check(seen.code() == BuildVerdictPayload.UNDONE_PARTLY && seen.said("message.enhanced.build.undo_partly"), "and said: " + seen);
        claimed[0] = false;
        BuildMagic.undo(wanda);
        finish(wanda);
        seen = Seen.of(wanda);
        check(count(f, box, Blocks.AIR) == 27 && step.state() == UndoStep.State.UNDONE, "the next Ctrl + Z puts back the 9");
        check(seen.code() == BuildVerdictPayload.UNDONE && seen.arg("message.enhanced.build.undone", 0).equals("9"), "and said: " + seen);
        helper.succeed();
    }

    // B62

    /**
     * With a Sokovia recovery clearing an island over part of the arena, a spell that touches the island's box is
     * {@code SOKOVIA}; one beside it is cast.
     */
    @GameTest(template = ARENA, batch = "build_rules_sokovia")
    public static void theSokoviaRecoveryIsOffLimits(GameTestHelper helper) throws IOException {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        MinecraftServer server = f.level().getServer();
        f.onClose(() -> {
            SokoviaRecovery.clear();
            SokoviaTestSupport.deleteJournals(server);
        });
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BlockPos centre = abs(f, 15, 6, 15);
        SokoviaShape.Spec spec = new SokoviaShape.Spec(1, 3, 1, 1, 1, 42L);
        SokoviaJournal.write(server, new SokoviaJournal.Header(UUID.randomUUID(), f.level().dimension(), centre, spec, centre.getY(),
                centre.getY() + 2, SokoviaJournal.PASTED, null, false, List.of()));
        check(SokoviaRecovery.recoverAll(server) >= 1 && SokoviaRecovery.pending() >= 1, "a recovery clears an island");
        Seen.of(wanda);
        cast(wanda, boxSpell(f, Spell.FILL, new BoundingBox(13, 7, 13, 17, 7, 17)));
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.SOKOVIA && seen.said("message.enhanced.build.sokovia") && working(wanda) == null,
                "a spell over the island waits until it is gone: " + seen);
        BoundingBox beside = new BoundingBox(1, 2, 1, 3, 2, 3);
        cast(wanda, boxSpell(f, Spell.FILL, beside));
        check(working(wanda) != null, "one beside it is cast");
        finish(wanda);
        check(count(f, beside, Blocks.STONE) == 9, "and done");
        helper.succeed();
    }

    /**
     * The note of an island that had landed, kept until two saves hold the landing (R48) and found at a start that
     * followed a stop that was not clean (lane TK's in-game check): its recovery warns of no island left in the sky,
     * moves nobody, and its empty column stays as it was, with the ground under it; a note of an island that was left
     * up there still warns.
     */
    @GameTest(template = ARENA, batch = "build_rules_sokovia_landed")
    public static void aLandedIslandsNoteIsRecoveredQuietly(GameTestHelper helper) throws IOException {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        MinecraftServer server = f.level().getServer();
        f.onClose(() -> {
            SokoviaRecovery.clear();
            SokoviaTestSupport.deleteJournals(server);
        });
        List<String> warnings = new ArrayList<>();
        org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(Enhanced.LOGGER.getName());
        AbstractAppender appender = new AbstractAppender("build-sokovia-landed", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                if (event.getLevel().isMoreSpecificThan(org.apache.logging.log4j.Level.WARN)) {
                    warnings.add(event.getMessage().getFormattedMessage());
                }
            }
        };
        appender.start();
        logger.addAppender(appender);
        f.onClose(() -> {
            logger.removeAppender(appender);
            appender.stop();
        });
        BlockPos centre = abs(f, 10, 6, 10);
        BoundingBox ground = new BoundingBox(8, 2, 8, 12, 5, 12);
        set(f, ground, Blocks.STONE);
        SokoviaShape.Spec spec = new SokoviaShape.Spec(1, 3, 1, 1, 1, 42L);
        UUID landed = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        SokoviaJournal.write(server, new SokoviaJournal.Header(landed, f.level().dimension(), centre, spec, centre.getY(), centre.getY() + 2,
                SokoviaJournal.LANDED, null, false, List.of(member)));
        check(SokoviaRecovery.recoverAll(server) == 1, "the landed note is recovered");
        SokoviaRecovery.finishAllNow();
        check(warnings.stream().noneMatch(line -> line.contains(landed.toString())), "with no warning: " + warnings);
        check(SokoviaRelocations.of(server).of(member) == null, "nobody is moved");
        check(count(f, ground, Blocks.STONE) == 100 && count(f, new BoundingBox(8, 6, 8, 12, 8, 12), Blocks.AIR) == 75,
                "its empty column and the ground under it are as they were");

        UUID left = UUID.randomUUID();
        SokoviaJournal.write(server, new SokoviaJournal.Header(left, f.level().dimension(), centre, spec, centre.getY(), centre.getY() + 2,
                SokoviaJournal.PASTED, null, false, List.of()));
        check(SokoviaRecovery.recoverAll(server) == 1, "(an island left in the sky)");
        SokoviaRecovery.finishAllNow();
        check(warnings.stream().anyMatch(line -> line.contains(left.toString()) && line.contains("left in the sky")),
                "an island left up there still warns: " + warnings);
        helper.succeed();
    }

    // B63

    /**
     * An Adventure Scarlet Witch is refused ({@code ADVENTURE}); with the world border moved through the box only the
     * cells inside it are built; the cells a NORMAL place-event canceller refuses (the stand-in for spawn protection,
     * which the GameTest server never has, R51) are left and counted. (Its own batch: the border is the level's.)
     */
    @GameTest(template = ARENA, batch = "build_border")
    public static void adventureSpawnProtectionAndTheBorder(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 10.5, 5.5, Blocks.STONE);
        BoundingBox row = new BoundingBox(1, 2, 1, 19, 2, 3);
        wanda.setGameMode(GameType.ADVENTURE);
        Seen.of(wanda);
        cast(wanda, boxSpell(f, Spell.FILL, row));
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.ADVENTURE && seen.said("message.enhanced.build.adventure") && working(wanda) == null,
                "Adventure: " + seen);
        wanda.setGameMode(GameType.SURVIVAL);
        BuildMagic.setMode(wanda, true);

        WorldBorder border = level.getWorldBorder();
        double centreX = border.getCenterX();
        double centreZ = border.getCenterZ();
        double size = border.getSize();
        f.onClose(() -> {
            border.setCenter(centreX, centreZ);
            border.setSize(size);
        });
        BlockPos middle = abs(f, 10, 2, 2);
        border.setCenter(middle.getX(), middle.getZ());
        border.setSize(10.0);
        cast(wanda, boxSpell(f, Spell.FILL, row));
        finish(wanda);
        int in = 0;
        int out = 0;
        BoundingBox world = abs(f, row);
        for (BlockPos pos : BlockPos.betweenClosed(world.minX(), world.minY(), world.minZ(), world.maxX(), world.maxY(), world.maxZ())) {
            if (border.isWithinBounds(pos)) {
                check(level.getBlockState(pos).is(Blocks.STONE), "inside the border it is built: " + pos);
                in++;
            } else {
                check(level.getBlockState(pos).isAir(), "outside the border it is not: " + pos);
                out++;
            }
        }
        check(in > 0 && out > 0, "the border cut the row: " + in + " in, " + out + " out");
        border.setCenter(centreX, centreZ);
        border.setSize(size);

        BoundingBox upper = new BoundingBox(1, 3, 1, 19, 3, 3);
        BoundingBox upperWorld = abs(f, upper);
        int line = sectionLine(f);
        listen(f, BlockEvent.EntityPlaceEvent.class, event -> {
            if (upperWorld.isInside(event.getPos()) && event.getPos().getX() >= line) {
                event.setCanceled(true);
            }
        });
        cast(wanda, boxSpell(f, Spell.FILL, upper));
        BuildJob job = working(wanda);
        finish(wanda);
        int refused = 0;
        for (BlockPos pos : BlockPos.betweenClosed(upperWorld.minX(), upperWorld.minY(), upperWorld.minZ(), upperWorld.maxX(), upperWorld.maxY(),
                upperWorld.maxZ())) {
            boolean left = pos.getX() >= line;
            check(left ? level.getBlockState(pos).isAir() : level.getBlockState(pos).is(Blocks.STONE), "the canceller's cells are left: " + pos);
            refused += left ? 1 : 0;
        }
        check(job.left(Left.PROTECTED) == refused && Seen.of(wanda).arg("message.enhanced.build.left.protected", 0).equals(String.valueOf(refused)),
                "and counted: " + job.left(Left.PROTECTED) + " of " + refused);
        helper.succeed();
    }

    // B64

    /** With PvP off, another Survival player's two cells stay air and he does not move. */
    @GameTest(template = ARENA, batch = "build_pvp_off")
    public static void nobodyElseIsBuriedWithPvpOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(false);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        ServerPlayer other = f.player(6.5, FLOOR, 6.5);
        Vec3 before = other.position();
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 3, 7);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        finish(wanda);
        check(f.level().getBlockState(abs(f, 6, 2, 6)).isAir() && f.level().getBlockState(abs(f, 6, 3, 6)).isAir(), "his two cells stay air");
        check(count(f, box, Blocks.STONE) == 16 && job.left(Left.BODIES) == 2, "the rest is built, his cells counted: " + job.left(Left.BODIES));
        check(other.position().distanceTo(before) < 1.0E-6, "and he did not move: " + other.position() + " from " + before);
        helper.succeed();
    }

    /** With PvP on, the other player is lifted onto the fill. */
    @GameTest(template = ARENA, batch = "build_pvp_on")
    public static void withPvpOnHeIsLiftedOntoTheFill(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(true);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        ServerPlayer other = f.player(6.5, FLOOR, 6.5);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 3, 7);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        finish(wanda);
        check(count(f, box, Blocks.STONE) == 18, "the whole fill is built");
        double y = other.getY() - f.helper().absoluteVec(Vec3.ZERO).y;
        check(Math.abs(y - 4.0) < 1.0E-6, "and he stands on it: " + y);
        helper.succeed();
    }

    // B65

    /**
     * With {@code fullContainers} spill, the Scarlet Witch's Vanish removes both chests and the 3 diamonds lie on the
     * spot; her undo puts both chests back, empty; the world holds 3 diamonds in all.
     */
    @GameTest(template = ARENA, batch = "build_spill")
    public static void fullChestsSpillWhenAsked(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.FULL_CONTAINERS, BuildConfig.FullContainers.SPILL);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        BlockPos full = abs(f, 5, 2, 5);
        BlockPos empty = abs(f, 7, 2, 5);
        level.setBlockAndUpdate(full, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(empty, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(full)).setItem(4, new ItemStack(Items.DIAMOND, 3));
        BoundingBox box = new BoundingBox(4, 2, 4, 8, 2, 6);
        cast(wanda, boxSpell(f, Spell.VANISH, box));
        finish(wanda);
        AABB around = AABB.of(abs(f, box)).inflate(4.0);
        check(level.getBlockState(full).isAir() && level.getBlockState(empty).isAir(), "both chests are gone");
        check(diamondsOnTheGround(level, around) == 3, "3 diamonds lie on the spot: " + diamondsOnTheGround(level, around));
        BuildMagic.undo(wanda);
        finish(wanda);
        check(level.getBlockState(full).is(Blocks.CHEST) && level.getBlockState(empty).is(Blocks.CHEST), "the undo puts both back");
        check(((ChestBlockEntity) level.getBlockEntity(full)).isEmpty() && ((ChestBlockEntity) level.getBlockEntity(empty)).isEmpty(), "empty");
        check(diamondsOnTheGround(level, around) == 3, "3 diamonds in all");
        helper.succeed();
    }

    private static int diamondsOnTheGround(ServerLevel level, AABB box) {
        int n = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.getItem().is(Items.DIAMOND)) {
                n += item.getItem().getCount();
            }
        }
        return n;
    }

    // B66

    /**
     * With {@code hexIsAClaim}, a Scarlet Witch raises a Hex over the arena: Billy's fill inside leaves every cell;
     * once she allows him, his fill is cast.
     */
    @GameTest(template = ARENA, batch = "build_hex")
    public static void aHexIsAClaimWhenAsked(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.HEX_IS_A_CLAIM, true);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        HexManager.create(wanda);
        ServerPlayer billy = builder(f, Power.BILLY, 3.5, 3.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 2, 7);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, box));
        Seen seen = Seen.of(billy);
        check(working(billy) == null && count(f, box, Blocks.AIR) == 9 && seen.code() == BuildVerdictPayload.NOTHING_CAN_CHANGE,
                "every cell is left, so the spell fizzles before it is cast: " + seen);
        check(seen.said("message.enhanced.build.left.protected") && seen.arg("message.enhanced.build.left.protected", 0).equals("9"),
                "and he is told how many: " + seen);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(billy.getUUID()), 1);
        // After his pause (castPause, 10 ticks).
        helper.runAfterDelay(15, () -> {
            cast(billy, boxSpell(f, Spell.FILL, box));
            finish(billy);
            check(count(f, box, Blocks.STONE) == 9, "allowed, his fill is cast: " + count(f, box, Blocks.STONE));
            helper.succeed();
        });
    }

    // B67

    /** With the overworld in {@code deniedDimensions}, a cast is {@code DIMENSION}. */
    @GameTest(template = ARENA, batch = "build_dimension")
    public static void deniedDimensions(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.DENIED_DIMENSIONS, List.of("minecraft:overworld"));
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        Seen.of(wanda);
        cast(wanda, boxSpell(f, Spell.FILL, new BoundingBox(5, 2, 5, 7, 2, 7)));
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DIMENSION && seen.said("message.enhanced.build.dimension") && working(wanda) == null,
                "the dimension is denied: " + seen);
        helper.succeed();
    }

    private BuildRulesTests() {
    }
}
