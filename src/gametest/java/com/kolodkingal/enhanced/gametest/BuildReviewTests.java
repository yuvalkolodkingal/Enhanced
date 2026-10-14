package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.BuildScheduler.Budget;
import com.kolodkingal.enhanced.build.BuildSession;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.build.Pocket;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.UndoCaps;
import com.kolodkingal.enhanced.build.UndoFiles;
import com.kolodkingal.enhanced.build.UndoHistory;
import com.kolodkingal.enhanced.build.UndoStep;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

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
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What the reviews of lane BS found (part 2): a container a Move carried is never in two places after its undo or redo, a
 * Move never destroys a chest it lands on, a redo does not wipe what was put in a chest since, a step too big for memory
 * lives on its records, a job that throws is stopped and not the tick, creatures go along only with blocks that moved,
 * a Copy does not read a claim's vault, a body is not burned or drowned with PvP off, a sign beside a vanished wall stays
 * with its words, an unloaded valley is not walked cell by cell, the ripple of an undo and a redo, the VIEW's pace, and the
 * pairs of spell and op no client sends. Each test has a batch of its own ({@code build_review_*}).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BuildReviewTests {
    private static final int GAP = 6;

    private static int diamonds(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ChestBlockEntity chest ? chest.countItem(Items.DIAMOND) : 0;
    }

    // ------------------------------------------------------------------------------------------------------------

    /**
     * A Move carries a chest of 5 diamonds to another cell. The contents are never in two places: if the moved chest is
     * emptied, the undo brings the source chest back bare; and after an undo, if the restored chest is emptied, the redo
     * leaves both cells alone (the diamonds are in the player's hands only).
     */
    @GameTest(template = ARENA, batch = "build_review_move_links", timeoutTicks = 300)
    public static void aMovedChestIsNeverInTwoPlacesAfterUndoOrRedo(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BlockPos source = abs(f, 3, 3, 3);
        BlockPos target = source.east(8);
        for (int round = 0; round < 2; round++) {
            level.setBlockAndUpdate(source, Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) level.getBlockEntity(source)).setItem(0, new ItemStack(Items.DIAMOND, 5));
            level.setBlockAndUpdate(abs(f, 3, 2, 3), Blocks.STONE.defaultBlockState());
            cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 2, 2, 2), abs(f, 4, 4, 4)).at(abs(f, 11, 1, 3), Direction.UP).build());
            finish(wanda);
            check(level.getBlockState(target).is(Blocks.CHEST) && diamonds(level, target) == 5 && !level.getBlockState(source).is(Blocks.CHEST),
                    "round " + round + ": the chest moved with its diamonds");
            if (round == 0) {
                // The player empties the moved chest; the undo brings the source back, bare.
                ((ChestBlockEntity) level.getBlockEntity(target)).clearContent();
                BuildMagic.undo(wanda);
                finish(wanda);
                check(level.getBlockState(source).is(Blocks.CHEST) && diamonds(level, source) == 0 && diamonds(level, target) == 0,
                        "the undo gives no diamonds back to the source when the moved chest was emptied: " + diamonds(level, source));
                level.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(abs(f, 3, 2, 3), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(abs(f, 11, 2, 3), Blocks.AIR.defaultBlockState());
            } else {
                // An undo with nothing touched gives all 5 back; then the restored chest is emptied and the redo leaves both alone.
                BuildMagic.undo(wanda);
                finish(wanda);
                check(diamonds(level, source) == 5 && diamonds(level, target) == 0, "an untouched undo gives them back: " + diamonds(level, source));
                ((ChestBlockEntity) level.getBlockEntity(source)).clearContent();
                BuildMagic.redo(wanda);
                finish(wanda);
                int total = diamonds(level, source) + diamonds(level, target);
                check(total == 0, "the redo does not make the emptied diamonds again: " + total);
            }
        }
        helper.succeed();
    }

    /** A Move that lands on a chest with things inside leaves it standing (skip), and its block stays where it was. */
    @GameTest(template = ARENA, batch = "build_review_move_onto_chest", timeoutTicks = 200)
    public static void aMoveNeverDestroysAFullChestItLandsOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BlockPos stone = abs(f, 3, 3, 3);
        BlockPos chest = stone.east(8);
        level.setBlockAndUpdate(stone, Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 7));
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 3, 3, 3), abs(f, 3, 3, 3)).at(abs(f, 11, 2, 3), Direction.UP).build());
        // (the block stands on the floor at the looked cell's top: put it down at relative y 3)
        Seen seen = Seen.of(wanda);
        // Its one cell would be left (the full chest): the spell fizzles before it is cast, and says what stayed.
        check(working(wanda) == null && seen.code() == BuildVerdictPayload.NOTHING_CAN_CHANGE && seen.said("message.enhanced.build.left.containers"),
                "nothing can change, so the Move is refused: " + seen);
        check(level.getBlockState(chest).is(Blocks.CHEST) && diamonds(level, chest) == 7, "the full chest stands with its diamonds: "
                + level.getBlockState(chest) + " " + diamonds(level, chest));
        check(level.getBlockState(stone).is(Blocks.GOLD_BLOCK), "and the gold stays where it was");
        helper.succeed();
    }

    /** With {@code fullContainers} spill, the same Move spills the chest's diamonds on the spot and puts its block down. */
    @GameTest(template = ARENA, batch = "build_review_move_spill", timeoutTicks = 200)
    public static void aMoveSpillsAFullChestItLandsOnWhenAsked(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.FULL_CONTAINERS, BuildConfig.FullContainers.SPILL);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BlockPos stone = abs(f, 3, 3, 3);
        BlockPos chest = stone.east(8);
        level.setBlockAndUpdate(stone, Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 7));
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 3, 3, 3), abs(f, 3, 3, 3)).at(abs(f, 11, 2, 3), Direction.UP).build());
        finish(wanda);
        int onTheGround = 0;
        for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(chest).inflate(3.0))) {
            onTheGround += item.getItem().is(Items.DIAMOND) ? item.getItem().getCount() : 0;
        }
        check(level.getBlockState(chest).is(Blocks.GOLD_BLOCK) && onTheGround == 7, "the gold is down and the diamonds lie on the spot: " + onTheGround);
        helper.succeed();
    }

    /**
     * A Fill replaced an empty chest; its undo brings the empty chest back; someone puts diamonds in; the redo leaves the
     * chest standing with them (it does not wipe a container).
     */
    @GameTest(template = ARENA, batch = "build_review_redo_chest", timeoutTicks = 200)
    public static void aRedoDoesNotWipeWhatWasPutInAChest(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BlockPos chest = abs(f, 6, 3, 6);
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        cast(wanda, boxSpell(f, Spell.FILL, new BoundingBox(5, 2, 5, 7, 4, 7)));
        finish(wanda);
        check(level.getBlockState(chest).is(Blocks.STONE), "the fill replaced the empty chest");
        BuildMagic.undo(wanda);
        finish(wanda);
        check(level.getBlockState(chest).is(Blocks.CHEST), "the undo brought it back");
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(1, new ItemStack(Items.DIAMOND, 64));
        BuildMagic.redo(wanda);
        finish(wanda);
        check(level.getBlockState(chest).is(Blocks.CHEST) && diamonds(level, chest) == 64, "the redo leaves the chest and its diamonds: " + level.getBlockState(chest));
        helper.succeed();
    }

    /**
     * A step too big to hold in memory while it works (the cap at 60,000 bytes) is kept as bits and records: it weighs
     * almost nothing, is listed when its spell ends, and an undo reads it back from its records and puts every cell back.
     */
    @GameTest(template = ARENA, batch = "build_review_lite", timeoutTicks = 400)
    public static void aStepTooBigForMemoryLivesOnItsRecords(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        BuildUndoTests.folder(f);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        UUID id = wanda.getUUID();
        long huge = Long.MAX_VALUE / 4;
        UndoCaps.use(60_000L, huge, huge, huge);
        BoundingBox box = new BoundingBox(0, 2, 0, 19, 10, 19);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        check(job != null, "the fill is at work");
        UndoStep step = job.step();
        int steps = 0;
        while (working(wanda) != null && steps++ < 2000) {
            UndoFiles.drain();
            BuildScheduler.step(List.of(id), Budget.of(400, 4096, 1));
            check(step.memoryBytes() < 60_000L, "while it works the step weighs little: " + step.memoryBytes());
        }
        UndoFiles.drain();
        int stone = count(f, box, Blocks.STONE);
        check(stone == 3600 && steps(wanda).size() == 1, "3,600 cells built, one step kept: " + stone + ", " + steps(wanda).size());
        UndoStep kept = steps(wanda).get(0);
        check(!kept.loaded() && kept.changed() == 3600 && kept.undoable(), "it is only listed, with its cells counted: " + kept.loaded() + ", " + kept.changed());
        UndoCaps.use(huge, huge, huge, huge);
        BuildMagic.undo(wanda);
        helper.succeedWhen(() -> {
            if (BuildScheduler.working(id) != null) {
                BuildScheduler.step(List.of(id), BuildTestSupport.ENDLESS);
            }
            check(count(f, box, Blocks.STONE) == 0 && count(f, box, Blocks.AIR) == 3600, "read back from its records, the undo put every cell back: "
                    + count(f, box, Blocks.STONE));
        });
    }

    /** A claim mod's listener throws while a spell works: the spell stops as one undoable step, the tick goes on. */
    @GameTest(template = ARENA, batch = "build_review_throws", timeoutTicks = 100)
    public static void aJobThatThrowsStopsAloneAndStaysUndoable(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        ServerPlayer sister = builder(f, Power.SCARLET_WITCH, 20.5, 20.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 4, 7);
        set(f, box);
        int[] calls = {0};
        BoundingBox world = abs(f, box);
        listen(f, BlockEvent.BreakEvent.class, event -> {
            if (world.isInside(event.getPos()) && ++calls[0] > 10) {
                throw new IllegalStateException("a claim mod's bug");
            }
        });
        cast(wanda, boxSpell(f, Spell.FILL, box));
        cast(sister, boxSpell(f, Spell.FILL, new BoundingBox(12, 2, 12, 14, 4, 14)));
        BuildJob job = working(wanda);
        BuildScheduler.step(List.of(wanda.getUUID(), sister.getUUID()), BuildTestSupport.ENDLESS);
        check(job.finished() && job.endCode() == BuildVerdictPayload.STOPPED, "the failing job ended stopped: " + job.endCode());
        check(count(f, new BoundingBox(12, 2, 12, 14, 4, 14), Blocks.STONE) == 27, "and the other builder's spell was built all the same");
        check(steps(wanda).size() == 1 && steps(wanda).get(0).state() == UndoStep.State.INTERRUPTED && steps(wanda).get(0).undoable(), "what it did is one undoable step");
        helper.succeed();
    }

    private static void set(TestKit.Fixture f, BoundingBox box) {
        BuildTestSupport.set(f, box, Blocks.DIRT);
    }

    /** A Move whose blocks the claims refuse leaves the creatures where they stand. */
    @GameTest(template = ARENA, batch = "build_review_riders", timeoutTicks = 200)
    public static void creaturesGoAlongOnlyWithBlocksThatMoved(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 3, 2, 3), abs(f, 5, 2, 5))) {
            level.setBlockAndUpdate(pos, Blocks.GOLD_BLOCK.defaultBlockState());
        }
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(4.5, 3, 4.5));
        Vec3 before = cow.position();
        BoundingBox everything = abs(f, new BoundingBox(0, 0, 0, 20, 12, 20));
        listen(f, BlockEvent.EntityPlaceEvent.class, event -> {
            if (everything.isInside(event.getPos())) {
                event.setCanceled(true); // (a claim refuses every block that would be put down)
            }
        });
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 2, 2, 2), abs(f, 6, 4, 6)).at(abs(f, 14, 1, 14), Direction.UP).build());
        BuildJob job = working(wanda);
        finish(wanda);
        check(cow.isAlive() && cow.position().distanceTo(before) < 1.0E-6, "the cow stayed where it stood: " + cow.position() + " from " + before);
        check(count(f, new BoundingBox(3, 2, 3, 5, 2, 5), Blocks.GOLD_BLOCK) == 9 && job.left(Left.PROTECTED) > 0, "and so did the gold the claim kept");
        helper.succeed();
    }

    /** A Copy of a chest in a claim keeps the bare block: the vault's diamonds are not in the pocket. */
    @GameTest(template = ARENA, batch = "build_review_copy_claim", timeoutTicks = 200)
    public static void aCopyDoesNotReadAClaimedVault(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BlockPos vault = abs(f, 4, 3, 4);
        BlockPos open = abs(f, 4, 3, 12);
        for (BlockPos pos : List.of(vault, open)) {
            level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 9));
        }
        BoundingBox claim = abs(f, new BoundingBox(2, 2, 2, 6, 5, 6));
        listen(f, BlockEvent.BreakEvent.class, event -> {
            if (claim.isInside(event.getPos())) {
                event.setCanceled(true);
            }
        });
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(abs(f, 3, 3, 3), abs(f, 5, 3, 5)).at(abs(f, 3, 2, 3), Direction.UP).build());
        finish(wanda);
        Pocket claimed = Pocket.held(wanda.getUUID());
        check(claimed != null && claimed.state(1, 0, 1) != null && claimed.state(1, 0, 1).is(Blocks.CHEST) && claimed.data(1, 0, 1) == null,
                "the claimed vault was copied bare");
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(abs(f, 3, 3, 11), abs(f, 5, 3, 13)).at(abs(f, 3, 2, 11), Direction.UP).build());
        finish(wanda);
        Pocket free = Pocket.held(wanda.getUUID());
        check(free != null && free.data(1, 0, 1) != null, "a chest outside the claim keeps its diamonds");
        helper.succeed();
    }

    /** With PvP off, no lava, water or fire is made on another player either. */
    @GameTest(template = ARENA, batch = "build_review_hazards", timeoutTicks = 200)
    public static void nobodyElseStandsInNewLavaWithPvpOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(false);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        wanda.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        ServerPlayer other = f.player(6.5, FLOOR, 6.5);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 3, 7);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        finish(wanda);
        check(f.level().getBlockState(abs(f, 6, 2, 6)).isAir() && f.level().getBlockState(abs(f, 6, 3, 6)).isAir(), "his two cells stay air: "
                + f.level().getBlockState(abs(f, 6, 2, 6)));
        check(job.left(Left.BODIES) == 2 && count(f, box, Blocks.LAVA) == 16, "the lava is made round him: " + job.left(Left.BODIES));
        helper.succeed();
    }

    /** A wall sign beside a wall that vanishes keeps its words (the settle pass leaves it to vanilla, with its drop). */
    @GameTest(template = ARENA, batch = "build_review_sign", timeoutTicks = 200)
    public static void aSignBesideAVanishedWallKeepsItsWords(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        BlockPos wall = abs(f, 8, 3, 8);
        BlockPos sign = wall.south();
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(sign, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, Direction.SOUTH));
        SignBlockEntity entity = (SignBlockEntity) level.getBlockEntity(sign);
        entity.setText(entity.getFrontText().setMessage(0, net.minecraft.network.chat.Component.literal("Eilon")), true);
        entity.setChanged();
        cast(wanda, boxSpell(f, Spell.VANISH, new BoundingBox(8, 3, 8, 8, 3, 8)));
        finish(wanda);
        check(level.getBlockState(wall).isAir(), "the wall vanished");
        check(level.getBlockState(sign).is(Blocks.OAK_WALL_SIGN) && level.getBlockEntity(sign) instanceof SignBlockEntity kept
                && kept.getFrontText().getMessage(0, false).getString().equals("Eilon"), "the sign beside it keeps its words: " + level.getBlockState(sign));
        helper.succeed();
    }

    /** A fill across 80 x 80 blocks of which one chunk is loaded ends in one step: the unloaded columns are given up whole. */
    @GameTest(template = ARENA, batch = "build_review_columns", timeoutTicks = 100)
    public static void unloadedColumnsAreGivenUpWhole(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = f.player(0.5, FLOOR, 0.5, Power.SCARLET_WITCH);
        wanda.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
        BuildMagic.setMode(wanda, true);
        int here = abs(f, 0, 0, 0).getX() >> 4;
        int there = abs(f, 0, 0, 0).getZ() >> 4;
        BuildScheduler.manual(wanda.getUUID(), (x, z) -> x == here && z == there);
        BlockPos low = abs(f, 0, 2, 0);
        cast(wanda, recipe(Spell.FILL).box(low, low.offset(79, 1, 79)).at(low, Direction.UP).build());
        BuildJob job = working(wanda);
        check(job != null, "the fill is at work");
        BuildScheduler.step(List.of(wanda.getUUID()), Budget.of(400, 4096, 1));
        BuildScheduler.step(List.of(wanda.getUUID()), Budget.of(400, 4096, 1));
        check(job.finished(), "it is over in two steps: " + job.phase() + ", walked " + job.walked());
        check(job.left(Left.UNLOADED) > 10_000, "the unloaded cells are counted: " + job.left(Left.UNLOADED));
        helper.succeed();
    }

    /** The ripple of an undo is the spell's, played backwards by the client; a redo's swaps rise and melt. */
    @GameTest(template = ARENA, batch = "build_review_ripple", timeoutTicks = 100)
    public static void undoAndRedoSendTheOrderTheClientExpects(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 4, 7);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        finish(wanda);
        Seen.of(wanda);
        BuildMagic.undo(wanda);
        finish(wanda);
        int undo = Seen.of(wanda).fx.stream().filter(fx -> fx.kind() == BuildFxPayload.UNDO_START).mapToInt(BuildFxPayload::order).findFirst().orElse(-1);
        BuildMagic.redo(wanda);
        finish(wanda);
        int redo = Seen.of(wanda).fx.stream().filter(fx -> fx.kind() == BuildFxPayload.UNDO_START).mapToInt(BuildFxPayload::order).findFirst().orElse(-1);
        check(undo == BuildFxPayload.RISE && redo == BuildFxPayload.MELT, "undo rise, redo melt: " + undo + ", " + redo);
        helper.succeed();
    }

    /**
     * The box the others see is shown at most every 4 ticks, but the last one is never lost, and its corners are clamped
     * to the world.
     */
    @GameTest(template = ARENA, batch = "build_review_view", timeoutTicks = 100)
    public static void theViewKeepsTheLastBoxAndClampsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BlockPos a = abs(f, 2, 2, 2);
        BlockPos b = abs(f, 4, 4, 4);
        BuildMagic.action(wanda, BuildActionPayload.VIEW, a, b, Spell.FILL.id() + 1);
        BlockPos a2 = abs(f, 6, 2, 6);
        BlockPos b2 = new BlockPos(2_000_000_000, 2_000_000_000, 2_000_000_000);
        BuildMagic.action(wanda, BuildActionPayload.VIEW, a2, b2, Spell.FILL.id() + 1);
        BuildView first = wanda.getData(ModAttachments.BUILD_VIEW);
        check(first.box().isPresent() && first.box().get().min().equals(a), "the first box is shown at once");
        helper.runAfterDelay(GAP, () -> {
            BuildView later = wanda.getData(ModAttachments.BUILD_VIEW);
            check(later.box().isPresent() && later.box().get().min().equals(a2), "and the newest, held back, is shown after: " + later.box());
            BlockPos far = later.box().get().max();
            check(far.getX() <= 29_999_999 && far.getY() <= 319 && far.getZ() <= 29_999_999, "its far corner is clamped to the world: " + far);
            helper.succeed();
        });
    }

    /** A pair of spell and op no client sends (a Fill's dabs, a Vanish's take) is dropped. */
    @GameTest(template = ARENA, batch = "build_review_ops", timeoutTicks = 100)
    public static void aSpellAndOpThatDoNotGoTogetherAreDropped(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(5, 2, 5, 7, 4, 7);
        BoundingBox world = abs(f, box);
        BlockPos low = new BlockPos(world.minX(), world.minY(), world.minZ());
        cast(wanda, recipe(Spell.FILL).dab(9).box(abs(f, 5, 2, 5), abs(f, 7, 4, 7)).at(low, Direction.UP).build());
        check(working(wanda) == null && BuildSession.existing(wanda.getUUID()).stroke() == null && count(f, box, Blocks.STONE) == 0, "a Fill's dab does nothing");
        cast(wanda, recipe(Spell.VANISH).op(SpellRecipe.Op.TAKE).box(abs(f, 5, 2, 5), abs(f, 7, 4, 7)).at(low, Direction.UP).build());
        check(working(wanda) == null, "a Vanish's take does nothing");
        helper.succeed();
    }
}
