package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BillyMagic;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildRules;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.Limits;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.UndoHistory;
import com.kolodkingal.enhanced.build.VanishStyle;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.boxSpell;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.building;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.count;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.set;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The limits of Build Magic, and what each hero is told of them (D-B 8, 9, 19.3; spec 18.4 B41-B54): Billy's meter
 * that pays and refills, his "not yet" and "never", his side and reach, one spell at a time, his ranks and practice,
 * Mom's help, the meter given back by an undo and paid again by a redo, his ten undos; the Scarlet Witch with no limit
 * but the technical ones (her ceiling, her cap of TNT, lava and fire, the long hold for a huge spell); operators only;
 * and blocks paid with items ({@code requireMaterials}).
 *
 * <p>Every test has a batch of its own ({@code build_limits_*} and the batches of 18.4): Billy's meter, the jobs, the
 * histories and the sessions are server-wide, and several tests change a setting. Between Billy's spells a test ends
 * his pause itself, as a test's time stands still while it runs (B44 waits it out instead).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BuildLimitsTests {
    /** The Scarlet Witch's spells (and undos) in B49: one more than the 100 undos she once kept (R39). */
    private static final int SPELLS = 101;

    // ------------------------------------------------------------------------------------------------------------
    // Billy (D-B 8)

    // B41

    /**
     * Billy's meter holds 512; a 140-block fill leaves 372, and a second later it holds 388 (16 a second); a Billy in
     * Creative pays the same (his limits apply in Creative); a Scarlet Witch's spell touches no meter.
     */
    @GameTest(template = ARENA, batch = "build_limits_meter")
    public static void billysMeterPaysAndRefills(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        long time = now(f);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        check(BillyMagic.meter(data(billy), time) == 512 && BillyMagic.top(data(billy)) == 512,
                "his meter holds 512: " + BillyMagic.meter(data(billy), time));
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(1, 2, 1, 7, 5, 5)));
        finish(billy);
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.DONE && seen.verdict().changed() == 140 && seen.verdict().cost() == 140,
                "140 changed and paid: " + seen);
        check(BillyMagic.meter(data(billy), time) == 372, "372 are left: " + BillyMagic.meter(data(billy), time));
        check(BillyMagic.meter(data(billy), time + 20) == 388, "a second later, 388: " + BillyMagic.meter(data(billy), time + 20));

        ServerPlayer creative = f.player(0.5, FLOOR, 20.5, GameType.CREATIVE);
        Powers.setPower(creative, Power.BILLY);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
        BuildMagic.setMode(creative, true);
        BuildScheduler.manual(creative.getUUID(), null);
        cast(creative, boxSpell(f, Spell.FILL, new BoundingBox(1, 2, 13, 7, 5, 17)));
        finish(creative);
        check(count(f, new BoundingBox(1, 2, 13, 7, 5, 17), Blocks.STONE) == 140 && BillyMagic.meter(data(creative), time) == 372,
                "a Billy in Creative pays the same: " + BillyMagic.meter(data(creative), time));

        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 20.5, 20.5, Blocks.OAK_PLANKS);
        Seen.of(wanda);
        cast(wanda, boxSpell(f, Spell.FILL, new BoundingBox(10, 2, 1, 16, 5, 5)));
        finish(wanda);
        seen = Seen.of(wanda);
        BuildData hers = data(wanda);
        check(seen.code() == BuildVerdictPayload.DONE && seen.verdict().cost() == 0 && hers.meter() == BuildData.FULL && hers.practice() == 0,
                "her spell touches no meter: " + seen + ", " + hers.meter());
        helper.succeed();
    }

    // B42

    /**
     * Billy with 100 in his meter: a 140-block spell is {@code REFILLING}, "3 more seconds"; a 600-block spell is
     * {@code TOO_BIG}, "600 of 512"; neither changes anything in the world, and his meter stays at 100.
     */
    @GameTest(template = ARENA, batch = "build_limits_not_yet")
    public static void billyHearsNotYetOrNever(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        long time = now(f);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        ModAttachments.BUILD_DATA.set(billy, data(billy).withMeter(100.0, time));
        BoundingBox small = new BoundingBox(1, 2, 1, 7, 5, 5);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, small));
        finish(billy);
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.REFILLING && seen.verdict().extra() == 3 && seen.verdict().cost() == 140
                && seen.said("message.enhanced.build.refilling") && seen.arg("message.enhanced.build.refilling", 0).equals("3"),
                "140 of 100: refilling, 3 more seconds: " + seen);
        check(count(f, small, Blocks.STONE) == 0, "nothing changed");

        BoundingBox big = new BoundingBox(1, 2, 8, 10, 7, 17);
        cast(billy, boxSpell(f, Spell.FILL, big));
        finish(billy);
        seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.TOO_BIG && seen.verdict().cost() == 600 && seen.verdict().extra() == 512
                && seen.arg("message.enhanced.build.too_big", 0).equals("600") && seen.arg("message.enhanced.build.too_big", 1).equals("512"),
                "600 of 512: too big: " + seen);
        check(count(f, big, Blocks.STONE) == 0, "nothing changed");
        check(BillyMagic.meter(data(billy), time) == 100, "his meter is still 100: " + BillyMagic.meter(data(billy), time));
        helper.succeed();
    }

    // B43

    /** A box 25 blocks wide is {@code TOO_WIDE} (at most 24); a corner 30 blocks away is {@code TOO_FAR} (24). */
    @GameTest(template = ARENA, batch = "build_limits_side")
    public static void billysSideAndReach(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer billy = builder(f, Power.BILLY, 10.5, 10.5, Blocks.STONE);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(-2, 2, 10, 22, 2, 10)));
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.TOO_WIDE && seen.verdict().extra() == 24
                && seen.arg("message.enhanced.build.too_wide", 0).equals("24") && working(billy) == null, "25 wide: too wide, at most 24: " + seen);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(10, 2, 40, 11, 2, 41)));
        seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.TOO_FAR && seen.verdict().extra() == 24 && seen.arg("message.enhanced.build.too_far", 0).equals("24")
                && working(billy) == null, "30 blocks away: too far, at most 24: " + seen);
        helper.succeed();
    }

    // B44

    /**
     * Billy casts one spell at a time: a second cast while the first works is {@code WAIT} ("Wait for your spell to
     * finish"); one in the 10 ticks after it is {@code WAIT} too, with the fizzle alone; after the pause he casts.
     */
    @GameTest(template = ARENA, batch = "build_limits_one_at_a_time")
    public static void billyCastsOneSpellAtATime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(2, 2, 2, 4, 4, 4)));
        BuildJob first = working(billy);
        check(first != null, "the first spell works");
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(8, 2, 8, 8, 2, 8)));
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.WAIT && seen.said("message.enhanced.build.wait") && working(billy) == first,
                "while it works: wait: " + seen);
        finish(billy);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(8, 2, 8, 8, 2, 8)));
        seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.WAIT && !seen.said("message.enhanced.build.wait") && working(billy) == null,
                "in the pause after it: wait, the fizzle alone: " + seen);
        helper.runAfterDelay(BuildConfig.BILLY_CAST_PAUSE.get() + 2, () -> {
            cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(8, 2, 8, 8, 2, 8)));
            check(working(billy) != null, "after the pause he casts");
            finish(billy);
            check(Seen.of(billy).code() == BuildVerdictPayload.DONE && count(f, new BoundingBox(8, 2, 8, 8, 2, 8), Blocks.STONE) == 1,
                    "and it is done");
            helper.succeed();
        });
    }

    // B45

    /**
     * Billy with 1,990 blocks of practice fills 20: he reaches rank 2 (the line, his crown's moment for those near),
     * his meter holds 1,024 and is full, his side is 32; an undo of that fill takes the 20 back, and his rank stays 2.
     */
    @GameTest(template = ARENA, batch = "build_limits_rank")
    public static void billyLearnsAndRanksUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        long time = now(f);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        ModAttachments.BUILD_DATA.set(billy, data(billy).withPractice(1990L));
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(2, 2, 2, 6, 2, 5)));
        finish(billy);
        Seen seen = Seen.of(billy);
        BuildData data = data(billy);
        check(data.rank() == 2 && data.practice() == 2010 && seen.said("message.enhanced.build.rank_up")
                && seen.arg("message.enhanced.build.rank_up", 0).equals("2") && seen.arg("message.enhanced.build.rank_up", 2).equals("1024"),
                "rank 2, and he is told: " + data.rank() + ", " + data.practice() + ", " + seen);
        check(seen.fx.stream().anyMatch(fx -> fx.kind() == BuildFxPayload.RANK_UP && fx.casterId() == billy.getId() && fx.cells() == 2),
                "his rank-up is seen: " + seen.fx.stream().map(BuildFxPayload::kind).toList());
        check(BillyMagic.top(data) == 1024 && BillyMagic.meter(data, time) == 1024, "a meter of 1,024, full: " + BillyMagic.meter(data, time));
        check(Limits.of(billy).side() == 32, "a side of 32: " + Limits.of(billy).side());
        BuildMagic.undo(billy);
        finish(billy);
        data = data(billy);
        check(data.practice() == 1990 && data.rank() == 2 && count(f, new BoundingBox(2, 2, 2, 6, 2, 5), Blocks.AIR) == 20,
                "the undo takes the 20 back, the rank stays: " + data.practice() + ", rank " + data.rank());
        helper.succeed();
    }

    // B46

    /**
     * A Scarlet Witch 5 blocks from Billy: his 100-block fill costs 50 and teaches 100, its START carries her id and he
     * is told "Mom is near"; 20 blocks away she does not help: it costs 100. Back near him, his badge learns it.
     */
    @GameTest(template = ARENA, batch = "build_limits_mom")
    public static void momsHelpHalvesTheCost(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        long time = now(f);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        ServerPlayer wanda = f.player(5.5, FLOOR, 0.5, Power.SCARLET_WITCH);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(5, 2, 5, 14, 2, 14)));
        finish(billy);
        Seen seen = Seen.of(billy);
        check(seen.verdict().changed() == 100 && seen.verdict().cost() == 50 && BillyMagic.meter(data(billy), time) == 462
                && data(billy).practice() == 100, "near her, 100 blocks cost 50 and teach 100: " + seen + ", " + data(billy).practice());
        check(seen.fx.stream().anyMatch(fx -> fx.kind() == BuildFxPayload.START && fx.momId() == wanda.getId()), "the START carries her id");
        check(seen.said("message.enhanced.build.mom"), "and he is told Mom is near: " + seen);

        Vec3 far = helper.absoluteVec(new Vec3(20.5, FLOOR, 0.5));
        wanda.teleportTo(far.x, far.y, far.z);
        unpause(billy);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(5, 3, 5, 14, 3, 14)));
        finish(billy);
        seen = Seen.of(billy);
        check(seen.verdict().cost() == 100 && BillyMagic.meter(data(billy), time) == 362
                && seen.fx.stream().anyMatch(fx -> fx.kind() == BuildFxPayload.START && fx.momId() == BuildFxPayload.NO_MOM),
                "20 blocks away she does not help: " + seen);

        Vec3 near = helper.absoluteVec(new Vec3(5.5, FLOOR, 0.5));
        wanda.teleportTo(near.x, near.y, near.z);
        f.everyTick(30, tick -> data(billy).momNear());
    }

    // B47

    /**
     * Billy fills 27 (his meter 27 less), undoes it (27 back), redoes it (27 less again); a new spell empties the redo
     * list.
     */
    @GameTest(template = ARENA, batch = "build_limits_undo_meter")
    public static void undoRefundsAndRedoPays(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        long time = now(f);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(3, 2, 3, 5, 4, 5);
        cast(billy, boxSpell(f, Spell.FILL, box));
        finish(billy);
        check(BillyMagic.meter(data(billy), time) == 485, "the fill: 27 less: " + BillyMagic.meter(data(billy), time));
        BuildMagic.undo(billy);
        finish(billy);
        check(BillyMagic.meter(data(billy), time) == 512 && count(f, box, Blocks.AIR) == 27,
                "the undo: 27 back: " + BillyMagic.meter(data(billy), time));
        Seen.of(billy);
        BuildMagic.redo(billy);
        finish(billy);
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.REDONE && seen.verdict().cost() == 27 && BillyMagic.meter(data(billy), time) == 485
                && count(f, box, Blocks.STONE) == 27, "the redo: 27 less again: " + seen + ", " + BillyMagic.meter(data(billy), time));
        BuildMagic.undo(billy);
        finish(billy);
        check(UndoHistory.of(billy.getUUID()).redoCount() == 1, "undone again, it can be redone");
        unpause(billy);
        cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(10, 2, 10, 10, 2, 10)));
        finish(billy);
        check(UndoHistory.of(billy.getUUID()).redoCount() == 0, "a new spell empties the redo list");
        Seen.of(billy);
        BuildMagic.redo(billy);
        check(Seen.of(billy).code() == BuildVerdictPayload.NOTHING_TO_REDO, "nothing to redo");
        helper.succeed();
    }

    // B48

    /**
     * Eleven one-block fills of Billy's: "Your oldest spell can no longer be undone" is said once (at the eleventh);
     * ten undos work, the eleventh is "Nothing to undo", and the first fill stays.
     */
    @GameTest(template = ARENA, batch = "build_limits_ten_undos")
    public static void billyKeepsTenUndos(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        Seen.of(billy);
        int forgotten = 0;
        for (int i = 0; i < 11; i++) {
            unpause(billy);
            cast(billy, boxSpell(f, Spell.FILL, new BoundingBox(2 + i, 2, 4, 2 + i, 2, 4)));
            finish(billy);
            Seen seen = Seen.of(billy);
            check(seen.code() == BuildVerdictPayload.DONE, "fill " + (i + 1) + " is done: " + seen);
            forgotten += (int) seen.bar.stream().filter(line -> Seen.key(line).equals("message.enhanced.build.forgotten")).count();
        }
        check(forgotten == 1, "the oldest is forgotten once: " + forgotten);
        for (int i = 0; i < 10; i++) {
            BuildMagic.undo(billy);
            finish(billy);
            check(Seen.of(billy).code() == BuildVerdictPayload.UNDONE, "undo " + (i + 1) + " works");
        }
        BuildMagic.undo(billy);
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.NOTHING_TO_UNDO && seen.said("message.enhanced.build.nothing_to_undo"), "the eleventh: " + seen);
        check(count(f, new BoundingBox(2, 2, 4, 2, 2, 4), Blocks.STONE) == 1 && count(f, new BoundingBox(3, 2, 4, 12, 2, 4), Blocks.AIR) == 10,
                "the first fill stays, the ten are undone");
        helper.succeed();
    }

    /**
     * Whole or nothing, beyond the register: Billy's redo he cannot pay now is {@code REFILLING} and redoes nothing; a
     * stamp of the TNT he copied is {@code VOLATILE} ("too dangerous for your magic") and places nothing.
     */
    @GameTest(template = ARENA, batch = "build_limits_redo")
    public static void billysRedosAndCopiesAreWholeOrNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        long time = now(f);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(3, 2, 3, 5, 4, 5);
        cast(billy, boxSpell(f, Spell.FILL, box));
        finish(billy);
        BuildMagic.undo(billy);
        finish(billy);
        ModAttachments.BUILD_DATA.set(billy, data(billy).withMeter(10.0, time));
        Seen.of(billy);
        BuildMagic.redo(billy);
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.REFILLING && seen.verdict().cost() == 27 && seen.verdict().extra() == 2
                && seen.said("message.enhanced.build.refilling") && working(billy) == null, "a redo of 27 with 10: not yet: " + seen);
        check(count(f, box, Blocks.AIR) == 27 && BillyMagic.meter(data(billy), time) == 10 && UndoHistory.of(billy.getUUID()).redoCount() == 1,
                "nothing redone, nothing paid, still to redo");

        BlockPos tnt = abs(f, 8, 2, 8);
        f.level().setBlockAndUpdate(tnt, Blocks.TNT.defaultBlockState());
        unpause(billy);
        cast(billy, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(tnt, tnt).at(tnt, Direction.UP).build());
        finish(billy);
        check(Seen.of(billy).code() == BuildVerdictPayload.COPIED, "he copies the TNT");
        cast(billy, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(abs(f, 12, 1, 8), Direction.UP).build());
        finish(billy);
        seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.VOLATILE && seen.said("message.enhanced.build.volatile")
                && f.level().getBlockState(abs(f, 12, 2, 8)).isAir(), "its stamp is too dangerous for his magic: " + seen);
        helper.succeed();
    }

    /**
     * A spell of Billy's that outlives him (he logs out before it is counted): it is checked against the meter he left
     * with and finished; at his next login it is paid (27 less) and its practice booked.
     */
    @GameTest(template = ARENA, batch = "build_limits_away")
    public static void billyPaysForASpellThatOutlivesHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        long time = now(f);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        UUID id = billy.getUUID();
        BoundingBox box = new BoundingBox(3, 2, 3, 5, 4, 5);
        cast(billy, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(billy);
        f.level().getServer().getPlayerList().remove(billy);
        finish(BuildTestSupport.ENDLESS, id);
        check(job.endCode() == BuildVerdictPayload.DONE && job.cost() == 27 && count(f, box, Blocks.STONE) == 27,
                "the spell finished without him, at its price: " + job.endCode() + ", " + job.cost());
        ServerPlayer back = f.player(0.5, FLOOR, 0.5, GameType.SURVIVAL, id);
        BuildData data = data(back);
        check(BillyMagic.meter(data, time) == 485 && data.practice() == 27, "at his login it is paid and learned: " + BillyMagic.meter(data, time)
                + ", practice " + data.practice());
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Scarlet Witch (D-B 9)

    // B49

    /**
     * The Scarlet Witch has no limit (R39): her double click on a 20 x 2 x 20 slab of stone selects all 800 cells,
     * where Billy's (512 at most) selects nothing and says why; 101 spells cast, then 101 undos all undo; a Repeat of
     * 300 copies is cast (and undone); a 21 x 8 x 21 fill (3,528) is cast and done, and she has no meter. The 101
     * spells and undos go one a tick, as a client sends them.
     */
    @GameTest(template = ARENA, batch = "build_limits_wanda", timeoutTicks = 2 * SPELLS + 40)
    public static void wandaHasNoLimit(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 10.5, 10.5, Blocks.STONE);
        ServerPlayer billy = builder(f, Power.BILLY, 3.5, 3.5, Blocks.STONE);
        // Both float above everything the test builds (nobody ticks them, so nobody falls).
        for (ServerPlayer player : new ServerPlayer[]{wanda, billy}) {
            Vec3 above = helper.absoluteVec(new Vec3(player == wanda ? 10.5 : 3.5, FLOOR + 8, player == wanda ? 10.5 : 3.5));
            player.teleportTo(above.x, above.y, above.z);
        }

        // Her double click: the whole slab; his: nothing, and why.
        BoundingBox slab = new BoundingBox(0, 2, 0, 19, 3, 19);
        set(f, slab, Blocks.STONE);
        SpellRecipe vanishSlab = recipe(Spell.VANISH).style(VanishStyle.ALL).mask(abs(f, 9, 3, 9)).build();
        Seen.of(billy);
        cast(billy, vanishSlab);
        finish(billy);
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.TOO_BIG_TO_SELECT && seen.verdict().extra() == 512
                && seen.arg("message.enhanced.build.too_big_to_select", 0).equals("512") && count(f, slab, Blocks.STONE) == 800,
                "Billy's double click selects nothing, and says why: " + seen);
        Seen.of(wanda);
        cast(wanda, vanishSlab);
        finish(wanda);
        seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DONE && seen.verdict().changed() == 800 && count(f, slab, Blocks.AIR) == 800,
                "hers selects all 800: " + seen);

        // 101 spells, then 101 undos: one a tick (her client's messages share one bucket of 40 a second, C6).
        BoundingBox cells = new BoundingBox(0, 2, 0, 20, 2, 4);
        f.everyTick(SPELLS * 2 + 1, tick -> {
            if (tick <= SPELLS) {
                int i = tick - 1;
                cast(wanda, boxSpell(f, Spell.FILL, new BoundingBox(i % 21, 2, i / 21, i % 21, 2, i / 21)));
                finish(wanda);
                return false;
            }
            if (tick == SPELLS + 1) {
                check(count(f, cells, Blocks.STONE) == SPELLS && UndoHistory.of(wanda.getUUID()).undoCount() == SPELLS + 1,
                        "101 spells cast: " + count(f, cells, Blocks.STONE));
                Seen.of(wanda);
            }
            if (tick <= SPELLS * 2) {
                BuildMagic.undo(wanda);
                finish(wanda);
                check(Seen.of(wanda).code() == BuildVerdictPayload.UNDONE, "undo " + (tick - SPELLS) + " undoes");
                return false;
            }
            int redo = UndoHistory.of(wanda.getUUID()).redoCount();
            check(count(f, cells, Blocks.STONE) == 0 && redo == SPELLS, "the 101 are all undone: " + count(f, cells, Blocks.STONE) + " stone, " + redo
                    + " to redo");
            repeatAndFill(f, wanda);
            return true;
        });
    }

    /** The spells of B49 that follow the 101 undos: a Repeat of 300 copies (undone again), then the 3,528 fill. */
    private static void repeatAndFill(TestKit.Fixture f, ServerPlayer wanda) {
        // A Repeat of 300 copies: no count but the spell's cells (the copies go up the arena's own column).
        BlockPos pillar = abs(f, 10, 2, 10);
        f.level().setBlockAndUpdate(pillar, Blocks.STONE.defaultBlockState());
        SpellRecipe repeat = recipe(Spell.REPEAT).box(pillar, pillar).at(pillar, Direction.UP).size(300).size2(0).build();
        check(repeat.clamped(Limits.of(wanda)).size() == 300, "300 copies are hers to ask for");
        Seen.of(wanda);
        cast(wanda, repeat);
        BuildJob row = working(wanda);
        finish(wanda);
        Seen seen = Seen.of(wanda);
        int column = 0;
        for (int y = 1; y <= 300; y++) {
            column += f.level().getBlockState(pillar.above(y)).is(Blocks.STONE) ? 1 : 0;
        }
        check(row != null && row.changed() + row.left(Left.BEYOND) == 300 && column == row.changed() && column >= 299,
                "the Repeat of 300 is cast: " + column + " copies, " + seen);
        BuildMagic.undo(wanda);
        finish(wanda);
        check(f.level().getBlockState(pillar.above(300)).isAir() && f.level().getBlockState(pillar.above(20)).isAir(), "and undone");
        f.level().setBlockAndUpdate(pillar, Blocks.AIR.defaultBlockState());

        // The 3,528 fill.
        BoundingBox all = new BoundingBox(0, 2, 0, 20, 9, 20);
        Seen.of(wanda);
        cast(wanda, boxSpell(f, Spell.FILL, all));
        finish(wanda);
        seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DONE && seen.verdict().changed() == 3528 && seen.verdict().cost() == 0,
                "the 21 x 8 x 21 fill is done: " + seen);
        check(count(f, all, Blocks.STONE) == 3528 && data(wanda).meter() == BuildData.FULL, "3,528 stone, and she has no meter");
    }

    // B50

    /** With {@code wanda.maxBlocks} 1,000, her 3,528 fill is {@code CEILING} 1,000, and nothing changes. */
    @GameTest(template = ARENA, batch = "build_ceiling")
    public static void wandasCeiling(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.WANDA_MAX_BLOCKS, 1000);
        ServerPlayer wanda = above(f, builder(f, Power.SCARLET_WITCH, 10.5, 10.5, Blocks.STONE));
        BoundingBox all = new BoundingBox(0, 2, 0, 20, 9, 20);
        Seen.of(wanda);
        cast(wanda, boxSpell(f, Spell.FILL, all));
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.CEILING && seen.verdict().extra() == 1000
                && seen.arg("message.enhanced.build.ceiling", 0).equals("1000")
                && working(wanda) == null && count(f, all, Blocks.STONE) == 0, "the server's ceiling: " + seen);
        helper.succeed();
    }

    // B51

    /** With {@code wanda.volatilePerSpell} 10, her 27 TNT are {@code VOLATILE_CAP} "27 of 10": nothing is placed. */
    @GameTest(template = ARENA, batch = "build_volatile")
    public static void wandasVolatileCap(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.WANDA_VOLATILE_PER_SPELL, 10);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.TNT);
        BoundingBox box = new BoundingBox(4, 2, 4, 6, 4, 6);
        Seen.of(wanda);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        finish(wanda);
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.VOLATILE_CAP && seen.verdict().changed() == 27 && seen.verdict().extra() == 10
                && seen.arg("message.enhanced.build.volatile_cap", 0).equals("27") && seen.arg("message.enhanced.build.volatile_cap", 1).equals("10"),
                "27 of 10: " + seen);
        check(count(f, box, Blocks.TNT) == 0 && UndoHistory.of(wanda.getUUID()).undoCount() == 0, "nothing placed");
        helper.succeed();
    }

    // B52

    /** With {@code hugeSpellBlocks} 1,000, a 1,500-cell fill without the long hold is {@code HOLD}; held, it is done. */
    @GameTest(template = ARENA, batch = "build_huge")
    public static void aHugeSpellNeedsTheLongHold(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.HUGE_SPELL_BLOCKS, 1000);
        ServerPlayer wanda = above(f, builder(f, Power.SCARLET_WITCH, 10.5, 10.5, Blocks.STONE));
        BoundingBox box = new BoundingBox(0, 2, 0, 14, 6, 19);
        Seen.of(wanda);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.HOLD && seen.verdict().extra() == 1500 && seen.arg("message.enhanced.build.huge", 0).equals("1500")
                && working(wanda) == null && count(f, box, Blocks.STONE) == 0, "without the long hold: hold: " + seen);
        cast(wanda, boxSpell(f, Spell.FILL, box), BuildCastPayload.HELD_LONG);
        finish(wanda);
        seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.DONE && count(f, box, Blocks.STONE) == 1500, "held: done: " + seen);
        helper.succeed();
    }

    // B53

    /**
     * With {@code operatorsOnly}, a player who is no operator turning build mode on is {@code OPERATORS} ("Only
     * operators may use Build Magic here") and so is his cast; an operator's build mode goes on.
     */
    @GameTest(template = ARENA, batch = "build_ops")
    public static void operatorsOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.OPERATORS_ONLY, true);
        ServerPlayer wanda = f.player(0.5, FLOOR, 0.5, Power.SCARLET_WITCH);
        Seen.of(wanda);
        BuildMagic.action(wanda, BuildActionPayload.MODE, BlockPos.ZERO, BlockPos.ZERO, 1);
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.OPERATORS && seen.said("message.enhanced.build.operators") && !building(wanda),
                "no operator: operators only: " + seen);
        cast(wanda, boxSpell(f, Spell.FILL, new BoundingBox(4, 2, 4, 4, 2, 4)));
        check(Seen.of(wanda).code() == BuildVerdictPayload.OPERATORS && working(wanda) == null, "nor a cast");

        ServerPlayer op = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PlayerList players = f.level().getServer().getPlayerList();
        players.getOps().add(new ServerOpListEntry(op.getGameProfile(), 2, false));
        f.onClose(() -> players.getOps().remove(op.getGameProfile()));
        BuildMagic.action(op, BuildActionPayload.MODE, BlockPos.ZERO, BlockPos.ZERO, 1);
        check(building(op), "an operator's build mode goes on");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Magic or materials (D-B 5.6)

    // B54

    /**
     * With {@code requireMaterials}, a Billy in Survival with 10 stone: a 12-block fill is {@code MATERIALS} ("You need
     * 2 more Stone"); a 9-block fill takes 9 and its undo gives them back; a Vanish of 3 stone puts 3 in his inventory
     * and its undo takes them back; a Billy in Creative pays nothing.
     */
    @GameTest(template = ARENA, batch = "build_materials")
    public static void materialsArePaidAndGivenBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.REQUIRE_MATERIALS, true);
        long time = now(f);
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, null);
        billy.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE, 10));
        BoundingBox twelve = new BoundingBox(3, 2, 3, 6, 2, 5);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, twelve));
        finish(billy);
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.MATERIALS && seen.verdict().extra() == 2
                && seen.arg("message.enhanced.build.materials", 0).equals("2")
                && seen.arg("message.enhanced.build.materials", 1).equals(Items.STONE.getDescription().getString()),
                "12 with 10 stone: you need 2 more: " + seen);
        check(count(f, twelve, Blocks.STONE) == 0 && stone(billy) == 10 && BillyMagic.meter(data(billy), time) == 512,
                "nothing taken, nothing built");

        BoundingBox nine = new BoundingBox(3, 2, 3, 5, 2, 5);
        unpause(billy);
        cast(billy, boxSpell(f, Spell.FILL, nine));
        finish(billy);
        check(count(f, nine, Blocks.STONE) == 9 && stone(billy) == 1, "9 built, 9 taken: " + stone(billy));
        BuildMagic.undo(billy);
        finish(billy);
        check(count(f, nine, Blocks.AIR) == 9 && stone(billy) == 10, "the undo gives them back: " + stone(billy));

        BoundingBox three = new BoundingBox(10, 2, 10, 12, 2, 10);
        set(f, three, Blocks.STONE);
        unpause(billy);
        cast(billy, boxSpell(f, Spell.VANISH, three));
        finish(billy);
        check(count(f, three, Blocks.AIR) == 3 && stone(billy) == 13, "the vanished 3 are in his inventory: " + stone(billy));
        BuildMagic.undo(billy);
        finish(billy);
        check(count(f, three, Blocks.STONE) == 3 && stone(billy) == 10, "the undo takes them back: " + stone(billy));

        ServerPlayer creative = f.player(20.5, FLOOR, 20.5, GameType.CREATIVE);
        Powers.setPower(creative, Power.BILLY);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE, 10));
        BuildMagic.setMode(creative, true);
        BuildScheduler.manual(creative.getUUID(), null);
        BoundingBox his = new BoundingBox(14, 2, 14, 16, 2, 16);
        cast(creative, boxSpell(f, Spell.FILL, his));
        finish(creative);
        check(count(f, his, Blocks.STONE) == 9 && stone(creative) == 10 && BuildRules.plainCount(creative, Items.STONE) == 10,
                "a Billy in Creative pays nothing: " + stone(creative));
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------

    private static long now(TestKit.Fixture f) {
        return f.level().getServer().overworld().getGameTime();
    }

    private static BuildData data(ServerPlayer player) {
        return ModAttachments.BUILD_DATA.get(player);
    }

    /** Billy's pause after his last spell is over (the test's time stands still while it runs). */
    private static void unpause(ServerPlayer billy) {
        ModAttachments.BUILD_DATA.set(billy, data(billy).withPauseUntil(0L));
    }

    /** The builder floats above what the test builds (nobody ticks her, so she does not fall). */
    private static ServerPlayer above(TestKit.Fixture f, ServerPlayer builder) {
        Vec3 at = builder.position().add(0.0, 8.0, 0.0);
        builder.teleportTo(at.x, at.y, at.z);
        return builder;
    }

    /** The plain stone the player carries. */
    private static int stone(ServerPlayer player) {
        return BuildRules.plainCount(player, Items.STONE);
    }

    private BuildLimitsTests() {
    }
}
