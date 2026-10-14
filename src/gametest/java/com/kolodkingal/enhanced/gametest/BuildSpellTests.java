package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.BuildScheduler.Budget;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Build Magic's spells as a hero casts them (D-B 19.2, spec 18.4 B14-B40 and B86): each through the payload's entry,
 * stepped by the test. Every test has a batch of its own (the jobs, histories and sessions are server-wide).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BuildSpellTests {

    // B86

    /**
     * A Move of a box holding a chest with 5 diamonds and two iron blocks, one cell a tick: after it read its source and
     * before those cells' turn, another player takes the 5 diamonds, takes one iron block, and places a block into a
     * source cell. Afterwards there are exactly 5 diamonds and 2 iron blocks in all (inventories, the world, items on the
     * ground); the emptied chest is where it was; the other iron block moved; the placed block stays; {@code
     * kept_changed} 3 (C12, R49). An overlapping Move by one cell (B29) still comes out exact.
     */
    @GameTest(template = ARENA, batch = "build_move_cas", timeoutTicks = 400)
    public static void aMoveNeverDuplicatesWhatChangedUnderIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        ServerPlayer sister = f.player(20.5, FLOOR, 20.5);
        BlockPos chest = abs(f, 3, 4, 3);
        BlockPos taken = abs(f, 2, 4, 2);
        BlockPos stays = abs(f, 4, 4, 4);
        BlockPos placed = abs(f, 4, 4, 2);
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 5));
        level.setBlockAndUpdate(taken, Blocks.IRON_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(stays, Blocks.IRON_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(abs(f, 3, 2, 3), Blocks.STONE.defaultBlockState());
        // Put down 8 blocks east: standing on the floor, centred on the looked block.
        SpellRecipe move = recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 2, 2, 2), abs(f, 4, 4, 4)).at(abs(f, 11, 1, 3), Direction.UP).build();
        cast(wanda, move);
        BuildJob job = working(wanda);
        check(job != null, "the Move is at work");
        Budget slow = Budget.of(1, 4096, 1);
        for (int tick = 0; tick < 300 && job.phase() != BuildJob.Phase.APPLY; tick++) {
            BuildScheduler.step(List.of(wanda.getUUID()), slow);
        }
        check(job.phase() == BuildJob.Phase.APPLY && job.written() <= 2, "its source is read, the cells' turns have begun: " + job.written());
        // The sister, before the top layer's turn.
        ChestBlockEntity box = (ChestBlockEntity) level.getBlockEntity(chest);
        sister.getInventory().add(box.removeItemNoUpdate(0));
        level.setBlockAndUpdate(taken, Blocks.AIR.defaultBlockState());
        sister.getInventory().add(new ItemStack(Items.IRON_BLOCK));
        level.setBlockAndUpdate(placed, Blocks.DIRT.defaultBlockState());
        Seen.of(wanda);
        for (int tick = 0; tick < 300 && !job.finished(); tick++) {
            BuildScheduler.step(List.of(wanda.getUUID()), slow);
        }
        check(job.finished(), "the Move ends");
        AABB around = AABB.of(abs(f, new BoundingBox(0, 1, 0, 20, 10, 20))).inflate(2.0);
        check(total(level, sister, around, Items.DIAMOND, Blocks.AIR) == 5, "exactly 5 diamonds: " + total(level, sister, around, Items.DIAMOND, Blocks.AIR));
        check(total(level, sister, around, Items.IRON_BLOCK, Blocks.IRON_BLOCK) == 2,
                "exactly 2 iron blocks: " + total(level, sister, around, Items.IRON_BLOCK, Blocks.IRON_BLOCK));
        check(level.getBlockState(chest).is(Blocks.CHEST) && ((ChestBlockEntity) level.getBlockEntity(chest)).isEmpty()
                && !level.getBlockState(chest.east(8)).is(Blocks.CHEST), "the emptied chest stays where it was");
        check(level.getBlockState(stays).isAir() && level.getBlockState(stays.east(8)).is(Blocks.IRON_BLOCK), "the other iron block moved");
        check(level.getBlockState(taken.east(8)).isAir(), "the taken one did not reappear");
        check(level.getBlockState(placed).is(Blocks.DIRT) && level.getBlockState(placed.east(8)).isAir(), "the placed block stays, uncopied");
        check(level.getBlockState(abs(f, 3, 2, 3)).isAir() && level.getBlockState(abs(f, 11, 2, 3)).is(Blocks.STONE), "the stone moved");
        Seen seen = Seen.of(wanda);
        check(job.left(Left.CHANGED) == 3 && seen.arg("message.enhanced.build.kept_changed", 0).equals("3"), "kept_changed 3: " + seen);

        // B29: a 3 x 3 x 3 pattern moved one block east comes out exact.
        Map<BlockPos, BlockState> pattern = new HashMap<>();
        Block[] kinds = {Blocks.STONE, Blocks.OAK_PLANKS, Blocks.GLASS, Blocks.DIRT};
        int i = 0;
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 2, 2, 12), abs(f, 4, 4, 14))) {
            BlockState state = kinds[i++ % kinds.length].defaultBlockState();
            level.setBlockAndUpdate(pos, state);
            pattern.put(pos.immutable(), state);
        }
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 2, 2, 12), abs(f, 4, 4, 14)).at(abs(f, 4, 1, 13), Direction.UP).build());
        finish(wanda);
        for (Map.Entry<BlockPos, BlockState> cell : pattern.entrySet()) {
            check(level.getBlockState(cell.getKey().east()) == cell.getValue(), "the pattern moved one east: " + cell.getKey());
        }
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 2, 2, 12), abs(f, 2, 4, 14))) {
            check(level.getBlockState(pos).isAir(), "its old west side is empty: " + pos);
        }
        check(Seen.of(wanda).code() == BuildVerdictPayload.DONE, "and nothing was kept");
        helper.succeed();
    }

    /** How many of {@code item} there are in the sister's inventory, on the ground, in chests, and as {@code block}s. */
    private static int total(ServerLevel level, ServerPlayer sister, AABB box, Item item, Block block) {
        int n = sister.getInventory().countItem(item);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.getItem().is(item)) {
                n += entity.getItem().getCount();
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed((int) box.minX, (int) box.minY, (int) box.minZ, (int) box.maxX, (int) box.maxY, (int) box.maxZ)) {
            BlockState state = level.getBlockState(pos);
            if (block != Blocks.AIR && state.is(block)) {
                n++;
            }
            if (state.is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    if (chest.getItem(slot).is(item)) {
                        n += chest.getItem(slot).getCount();
                    }
                }
            }
        }
        return n;
    }

    private BuildSpellTests() {
    }
}
