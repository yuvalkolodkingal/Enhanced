package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.block.MjolnirBlockEntity;
import com.kolodkingal.enhanced.build.BillyMagic;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.BuildScheduler.Budget;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.BuildSession;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.build.ChangeStyle;
import com.kolodkingal.enhanced.build.FillStyle;
import com.kolodkingal.enhanced.build.Pocket;
import com.kolodkingal.enhanced.build.ShapeKind;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellMath;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.UndoHistory;
import com.kolodkingal.enhanced.build.VanishStyle;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildPocketPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.Events;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.road.WitchesRoad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.ENDLESS;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.boxSpell;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.building;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.count;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.listen;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.set;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.steps;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Build Magic's spells as a hero casts them (D-B 19.2, spec 18.4 B14-B40 and B86): each through the payload's entry,
 * stepped by the test. Every test has a batch of its own (the jobs, histories and sessions are server-wide).
 */
public final class BuildSpellTests {

    // ------------------------------------------------------------------------------------------------------------
    // Build mode (D-B 2.2, 11.9)

    // B14

    /**
     * The Scarlet Witch and Billy turn build mode on (as everyone who sees them knows it); Magneto and a player with no
     * power are ignored and told nothing; a Scarlet Witch in Adventure is told {@code adventure}; turning it off lets
     * go of the box the others saw.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_mode")
    public static void buildModeIsForWandaAndBillyOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = f.player(2.5, FLOOR, 2.5, Power.SCARLET_WITCH);
        ServerPlayer billy = f.player(5.5, FLOOR, 2.5, Power.BILLY);
        ServerPlayer magneto = f.player(8.5, FLOOR, 2.5, Power.MAGNETO);
        ServerPlayer nobody = f.player(11.5, FLOOR, 2.5);
        for (ServerPlayer player : List.of(wanda, billy, magneto, nobody)) {
            Seen.of(player);
            BuildMagic.action(player, BuildActionPayload.MODE, BlockPos.ZERO, BlockPos.ZERO, 1);
        }
        check(building(wanda) && building(billy), "the Scarlet Witch and Billy are in build mode");
        check(!building(magneto) && !building(nobody), "Magneto and a player with no power are not");
        check(Seen.of(magneto).verdicts.isEmpty() && Seen.of(nobody).verdicts.isEmpty(), "and are told nothing");

        ServerPlayer adventurer = f.player(14.5, FLOOR, 2.5, GameType.ADVENTURE);
        Powers.setPower(adventurer, Power.SCARLET_WITCH);
        Seen.of(adventurer);
        BuildMagic.action(adventurer, BuildActionPayload.MODE, BlockPos.ZERO, BlockPos.ZERO, 1);
        Seen seen = Seen.of(adventurer);
        check(!building(adventurer) && seen.code() == BuildVerdictPayload.ADVENTURE && seen.said("message.enhanced.build.adventure"),
                "in Adventure she is told why: " + seen);

        BuildMagic.action(wanda, BuildActionPayload.VIEW, abs(f, 3, 2, 3), abs(f, 5, 2, 5), Spell.FILL.id() + 1);
        check(ModAttachments.BUILD_VIEW.get(wanda).box().isPresent(), "the others see her box");
        BuildMagic.action(wanda, BuildActionPayload.MODE, BlockPos.ZERO, BlockPos.ZERO, 0);
        BuildView view = ModAttachments.BUILD_VIEW.get(wanda);
        check(!building(wanda) && !view.active() && view.box().isEmpty(), "off clears the view: " + view);
        helper.succeed();
    }

    // B15

    /**
     * A death, a respawn and a change of power each end build mode, and the spell at work still changes its last cell
     * and seals its step (the waiting one is dropped at a change of power); a change of dimension keeps build mode on,
     * lets go of the box, and the spell goes on in its own dimension.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_life", timeoutTicks = 200)
    public static void buildModeEndsOnDeathRespawnAndPowerChangeButTheSpellGoesOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        MinecraftServer server = f.level().getServer();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        UUID id = wanda.getUUID();
        Budget five = Budget.of(5, 4096, 1);

        BoundingBox first = new BoundingBox(2, 2, 2, 4, 4, 4);
        cast(wanda, boxSpell(f, Spell.FILL, first));
        BuildJob job = working(wanda);
        BuildScheduler.step(List.of(id), five);
        wanda.kill();
        check(!wanda.isAlive() && !mode(id), "death ends build mode");
        finish(ENDLESS, id);
        check(job.endCode() == BuildVerdictPayload.DONE && count(f, first, Blocks.STONE) == 27 && UndoHistory.of(id).steps().size() == 1,
                "the spell still changes its last cell and seals its step: " + job.endCode() + ", " + count(f, first, Blocks.STONE));

        ServerPlayer reborn = server.getPlayerList().respawn(wanda, false, Entity.RemovalReason.KILLED);
        f.onClose(() -> server.getPlayerList().remove(reborn));
        Vec3 home = f.helper().absoluteVec(new Vec3(0.5, FLOOR, 0.5));
        reborn.teleportTo(home.x, home.y, home.z);
        reborn.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
        if (Powers.get(reborn).power() != Power.SCARLET_WITCH) {
            Powers.setPower(reborn, Power.SCARLET_WITCH);
        }
        check(!mode(id), "she comes back out of build mode");
        BuildMagic.setMode(reborn, true);
        BoundingBox second = new BoundingBox(6, 2, 2, 8, 4, 4);
        cast(reborn, boxSpell(f, Spell.FILL, second));
        job = working(reborn);
        BuildScheduler.step(List.of(id), five);
        Events.post(new PlayerEvent.PlayerRespawnEvent(reborn, false));
        check(!mode(id) && !building(reborn), "a respawn ends build mode");
        finish(ENDLESS, id);
        check(job.endCode() == BuildVerdictPayload.DONE && count(f, second, Blocks.STONE) == 27 && UndoHistory.of(id).steps().size() == 2,
                "and the spell goes on");

        BuildMagic.setMode(reborn, true);
        BoundingBox third = new BoundingBox(10, 2, 2, 12, 4, 4);
        BoundingBox waits = new BoundingBox(14, 2, 2, 16, 4, 4);
        cast(reborn, boxSpell(f, Spell.FILL, third));
        cast(reborn, boxSpell(f, Spell.FILL, waits));
        job = working(reborn);
        BuildScheduler.step(List.of(id), five);
        Powers.setPower(reborn, Power.NONE);
        check(!mode(id) && !building(reborn), "a change of power ends build mode");
        check(BuildScheduler.jobs(id).size() == 1 && working(reborn) == job, "the waiting spell is dropped, the working one goes on");
        finish(ENDLESS, id);
        check(job.endCode() == BuildVerdictPayload.DONE && count(f, third, Blocks.STONE) == 27 && count(f, waits, Blocks.AIR) == 27
                && UndoHistory.of(id).steps().size() == 3, "it finishes and seals its step; the waiting one never happened");

        Powers.setPower(reborn, Power.SCARLET_WITCH);
        BuildMagic.setMode(reborn, true);
        BuildMagic.action(reborn, BuildActionPayload.VIEW, abs(f, 2, 2, 8), abs(f, 4, 2, 10), Spell.FILL.id() + 1);
        BoundingBox fourth = new BoundingBox(2, 2, 8, 4, 4, 10);
        cast(reborn, boxSpell(f, Spell.FILL, fourth));
        job = working(reborn);
        BuildScheduler.step(List.of(id), five);
        ServerLevel nether = server.getLevel(Level.NETHER);
        reborn.teleportTo(nether, 0.5, 128.0, 0.5, 0.0F, 0.0F);
        BuildView view = ModAttachments.BUILD_VIEW.get(reborn);
        check(reborn.level() == nether && mode(id) && view.active() && view.box().isEmpty(),
                "a change of dimension keeps build mode and lets go of the box");
        finish(ENDLESS, id);
        check(job.level() == f.level() && job.endCode() == BuildVerdictPayload.DONE && count(f, fourth, Blocks.STONE) == 27,
                "the spell goes on in its own dimension");
        helper.succeed();
    }

    // B16

    /**
     * In build mode the Scarlet Witch still takes to the air with ability 2, and Billy still Hex-Bolts a zombie with 1.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_keys")
    public static void abilityKeysStillWorkInBuildMode(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 2.5, 2.5, null);
        ServerPlayer billy = builder(f, Power.BILLY, 4.5, 10.5, null);
        check(building(wanda) && building(billy), "both in build mode");
        AbilityHandler.onKey(wanda, 2, true);
        AbilityHandler.onKey(wanda, 2, false);
        check(wanda.getAbilities().flying, "the Scarlet Witch flies");
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        TestKit.lookAt(billy, zombie);
        AbilityHandler.onKey(billy, 1, true);
        AbilityHandler.onKey(billy, 1, false);
        check(zombie.getHealth() < zombie.getMaxHealth(), "Billy's Hex Bolt strikes the zombie: " + zombie.getHealth());
        check(building(wanda) && building(billy), "and both are still in build mode");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Fill, Vanish, Change, Paint, Pull & Push, Shapes (D-B 4.2-4.7)

    // B17

    /**
     * Billy fills a 3 x 3 x 3 box of 9 stone and 18 air with stone: 27 stone, 18 changed, his meter down by exactly 18;
     * the same fill again changes nothing ({@code NOTHING_CHANGES}) and costs nothing.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_fill")
    public static void fillChangesOnlyWhatDiffers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        long time = f.level().getServer().overworld().getGameTime();
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        BoundingBox box = new BoundingBox(4, 2, 4, 6, 4, 6);
        set(f, new BoundingBox(4, 2, 4, 6, 2, 6), Blocks.STONE);
        int before = BillyMagic.meter(ModAttachments.BUILD_DATA.get(billy), time);
        Seen.of(billy);
        cast(billy, boxSpell(f, Spell.FILL, box));
        finish(billy);
        Seen seen = Seen.of(billy);
        int after = BillyMagic.meter(ModAttachments.BUILD_DATA.get(billy), time);
        check(count(f, box, Blocks.STONE) == 27 && seen.code() == BuildVerdictPayload.DONE && seen.verdict().changed() == 18
                && seen.verdict().cost() == 18, "27 stone, 18 changed: " + seen);
        check(before - after == 18, "his meter is down by exactly 18: " + before + " to " + after);
        // (His pause after a spell is B44's.)
        ModAttachments.BUILD_DATA.set(billy, ModAttachments.BUILD_DATA.get(billy).withPauseUntil(0L));
        cast(billy, boxSpell(f, Spell.FILL, box));
        finish(billy);
        seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.NOTHING_CHANGES && seen.said("message.enhanced.build.nothing_changes"),
                "again: nothing changes: " + seen);
        check(BillyMagic.meter(ModAttachments.BUILD_DATA.get(billy), time) == after, "and nothing is paid");
        helper.succeed();
    }

    // B18

    /**
     * Room over a 5 x 4 x 5 box with stone inside: the shell is planks (82) and the 18 cells inside are air; Only the
     * empty over a floor with scattered stone keeps the stone and fills the air with planks.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_room")
    public static void roomEmptiesTheInsideAndOnlyTheEmptyKeepsWhatIsThere(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.OAK_PLANKS);
        BoundingBox box = new BoundingBox(3, 2, 3, 7, 5, 7);
        BoundingBox inside = new BoundingBox(4, 3, 4, 6, 4, 6);
        set(f, inside, Blocks.STONE);
        cast(wanda, styled(f, Spell.FILL, FillStyle.ROOM, box));
        finish(wanda);
        check(count(f, box, Blocks.OAK_PLANKS) == 82 && count(f, inside, Blocks.AIR) == 18,
                "a shell of 82 planks, 18 air inside: " + count(f, box, Blocks.OAK_PLANKS) + ", " + count(f, inside, Blocks.AIR));

        BoundingBox floor = new BoundingBox(10, 2, 10, 14, 2, 14);
        List<BlockPos> stones = List.of(abs(f, 10, 2, 10), abs(f, 12, 2, 11), abs(f, 14, 2, 14), abs(f, 11, 2, 13), abs(f, 13, 2, 12));
        for (BlockPos pos : stones) {
            f.level().setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        cast(wanda, styled(f, Spell.FILL, FillStyle.EMPTY, floor));
        finish(wanda);
        check(stones.stream().allMatch(pos -> f.level().getBlockState(pos).is(Blocks.STONE)) && count(f, floor, Blocks.OAK_PLANKS) == 20,
                "the stone stays and the air becomes planks: " + count(f, floor, Blocks.OAK_PLANKS));
        helper.succeed();
    }

    // B19

    /**
     * Everything empties the box; Hollow out on a 5 x 5 x 5 stone cube keeps exactly its 98 outer cells; Plants and
     * trees takes logs, leaves and a flower and keeps the stone; This kind aimed at dirt leaves all the stone.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_vanish")
    public static void vanishStyles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        BoundingBox all = new BoundingBox(2, 2, 2, 4, 4, 4);
        set(f, all, Blocks.STONE);
        cast(wanda, styled(f, Spell.VANISH, VanishStyle.ALL, all));
        finish(wanda);
        check(count(f, all, Blocks.AIR) == 27, "Everything empties the box");

        BoundingBox cube = new BoundingBox(8, 2, 2, 12, 6, 6);
        set(f, cube, Blocks.STONE);
        cast(wanda, styled(f, Spell.VANISH, VanishStyle.HOLLOW, cube));
        finish(wanda);
        check(count(f, cube, Blocks.STONE) == 98 && count(f, new BoundingBox(9, 3, 3, 11, 5, 5), Blocks.AIR) == 27,
                "Hollow out keeps the 98 outer cells: " + count(f, cube, Blocks.STONE));

        BoundingBox garden = new BoundingBox(2, 2, 10, 5, 3, 12);
        set(f, garden, Blocks.STONE);
        BlockPos log = abs(f, 3, 2, 11);
        BlockPos leaves = abs(f, 3, 3, 11);
        BlockPos dirt = abs(f, 5, 2, 11);
        BlockPos flower = abs(f, 5, 3, 11);
        level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
        level.setBlock(dirt, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(flower, Blocks.POPPY.defaultBlockState(), Block.UPDATE_CLIENTS);
        cast(wanda, styled(f, Spell.VANISH, VanishStyle.PLANTS, garden));
        finish(wanda);
        check(level.getBlockState(log).isAir() && level.getBlockState(leaves).isAir() && level.getBlockState(flower).isAir(),
                "the log, the leaves and the flower are gone");
        check(count(f, garden, Blocks.STONE) == 20 && level.getBlockState(dirt).is(Blocks.DIRT),
                "the stone and the dirt stay: " + count(f, garden, Blocks.STONE));

        BoundingBox mixed = new BoundingBox(14, 2, 10, 16, 2, 12);
        set(f, mixed, Blocks.STONE);
        List<BlockPos> dirts = List.of(abs(f, 14, 2, 10), abs(f, 15, 2, 11), abs(f, 16, 2, 12));
        for (BlockPos pos : dirts) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        cast(wanda, recipe(Spell.VANISH).style(VanishStyle.KIND).box(abs(f, mixed)).at(dirts.get(1), Direction.UP).build());
        finish(wanda);
        check(dirts.stream().allMatch(pos -> level.getBlockState(pos).isAir()) && count(f, mixed, Blocks.STONE) == 6,
                "This kind takes the dirt and leaves all the stone");
        helper.succeed();
    }

    // B20

    /** A waterlogged slab vanishes into a water source; a slab filled into a water source is waterlogged. */
    @EnhancedTest(template = ARENA, batch = "build_magic_water")
    public static void aWaterloggedBlockLeavesItsWater(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.OAK_SLAB);
        basin(f, 4, 4);
        BlockPos slab = abs(f, 4, 2, 4);
        level.setBlock(slab, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true), Block.UPDATE_CLIENTS);
        cast(wanda, styled(f, Spell.VANISH, VanishStyle.ALL, new BoundingBox(4, 2, 4, 4, 2, 4)));
        finish(wanda);
        check(level.getBlockState(slab).is(Blocks.WATER) && level.getFluidState(slab).isSource(),
                "the slab leaves a water source: " + level.getBlockState(slab));

        basin(f, 10, 4);
        BlockPos water = abs(f, 10, 2, 4);
        level.setBlock(water, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        cast(wanda, recipe(Spell.FILL).box(water, water).at(water, Direction.UP).build());
        finish(wanda);
        BlockState placed = level.getBlockState(water);
        check(placed.is(Blocks.OAK_SLAB) && placed.getValue(SlabBlock.WATERLOGGED), "a slab filled into a water source is waterlogged: " + placed);
        helper.succeed();
    }

    /** A ring of stone round the floor cell {@code x, 2, z}, so that water or lava put there stays there. */
    private static void basin(TestKit.Fixture f, int x, int z) {
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, x - 1, 2, z - 1), abs(f, x + 1, 2, z + 1))) {
            if (pos.getX() != abs(f, x, 2, z).getX() || pos.getZ() != abs(f, x, 2, z).getZ()) {
                f.level().setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    // B21

    /**
     * Change aimed at oak stairs (east, top) with spruce planks makes spruce stairs (east, top), and aimed at an oak
     * slab (top) a spruce slab (top), the stone beside them untouched; Only the outside on a 3 x 3 x 3 stone cube
     * changes its 26 outer cells, the centre still stone.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_change")
    public static void changeKeepsShapesAndOnlyTheOutsideRepaintsTheSkin(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.SPRUCE_PLANKS);
        BlockPos stairs = abs(f, 3, 2, 3);
        BlockPos slab = abs(f, 5, 2, 3);
        BlockPos stone = abs(f, 4, 2, 3);
        level.setBlock(stairs, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP),
                Block.UPDATE_CLIENTS);
        level.setBlock(slab, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), Block.UPDATE_CLIENTS);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        BoundingBox row = abs(f, new BoundingBox(3, 2, 3, 5, 2, 3));
        cast(wanda, recipe(Spell.CHANGE).style(ChangeStyle.KIND).box(row).at(stairs, Direction.UP).build());
        finish(wanda);
        cast(wanda, recipe(Spell.CHANGE).style(ChangeStyle.KIND).box(row).at(slab, Direction.UP).build());
        finish(wanda);
        BlockState newStairs = level.getBlockState(stairs);
        BlockState newSlab = level.getBlockState(slab);
        check(newStairs.is(Blocks.SPRUCE_STAIRS) && newStairs.getValue(StairBlock.FACING) == Direction.EAST
                && newStairs.getValue(StairBlock.HALF) == Half.TOP,
                "spruce stairs, east, top: " + newStairs);
        check(newSlab.is(Blocks.SPRUCE_SLAB) && newSlab.getValue(SlabBlock.TYPE) == SlabType.TOP, "a spruce slab, top: " + newSlab);
        check(level.getBlockState(stone).is(Blocks.STONE), "the stone is untouched");

        BoundingBox cube = new BoundingBox(10, 4, 10, 12, 6, 12);
        set(f, cube, Blocks.STONE);
        cast(wanda, styled(f, Spell.CHANGE, ChangeStyle.OUTSIDE, cube));
        finish(wanda);
        check(count(f, cube, Blocks.SPRUCE_PLANKS) == 26 && level.getBlockState(abs(f, 11, 5, 11)).is(Blocks.STONE),
                "the outside's 26 change, the centre is still stone: " + count(f, cube, Blocks.SPRUCE_PLANKS));
        helper.succeed();
    }

    // B22

    /**
     * Three dabs of one stroke over three oak stairs facing north with spruce planks: three spruce stairs facing north,
     * and nothing sealed until the stroke ends; STROKE_END seals one step, which one undo puts back whole. A stroke
     * left without its release is sealed 40 ticks after its last dab.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_paint", timeoutTicks = 200)
    public static void aPaintStrokeIsOneUndoStep(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.SPRUCE_PLANKS);
        List<BlockPos> stairs = List.of(abs(f, 3, 2, 5), abs(f, 6, 2, 5), abs(f, 9, 2, 5));
        BlockState north = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (BlockPos pos : stairs) {
            level.setBlock(pos, north, Block.UPDATE_CLIENTS);
        }
        for (BlockPos pos : stairs) {
            cast(wanda, recipe(Spell.PAINT).dab(7).at(pos, Direction.UP).size(0).build());
        }
        finish(wanda);
        for (BlockPos pos : stairs) {
            BlockState painted = level.getBlockState(pos);
            check(painted.is(Blocks.SPRUCE_STAIRS) && painted.getValue(StairBlock.FACING) == Direction.NORTH, "spruce stairs facing north: " + painted);
        }
        check(steps(wanda).isEmpty(), "the stroke is still open");
        Seen.of(wanda);
        BuildMagic.action(wanda, BuildActionPayload.STROKE_END, BlockPos.ZERO, BlockPos.ZERO, 7);
        Seen seen = Seen.of(wanda);
        check(steps(wanda).size() == 1 && steps(wanda).get(0).changed() == 3 && seen.code() == BuildVerdictPayload.DONE && seen.verdict().changed() == 3,
                "its release seals one step of 3: " + seen);
        BuildMagic.undo(wanda);
        finish(wanda);
        check(stairs.stream().allMatch(pos -> level.getBlockState(pos).equals(north)), "one undo puts back all three");

        // A stroke whose dabs are refused (nothing in her hands) says why once, not at every dab.
        wanda.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Seen.of(wanda);
        for (int dab = 0; dab < 3; dab++) {
            cast(wanda, recipe(Spell.PAINT).dab(9).at(stairs.get(0), Direction.UP).size(0).build());
        }
        seen = Seen.of(wanda);
        check(seen.verdicts.size() == 3 && seen.verdicts.stream().allMatch(verdict -> verdict.code() == BuildVerdictPayload.NO_BLOCK)
                && seen.bar.stream().filter(line -> Seen.key(line).equals("message.enhanced.build.no_block")).count() == 1,
                "a refused stroke says why once: " + seen);
        wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.SPRUCE_PLANKS));

        BlockPos last = abs(f, 12, 2, 5);
        level.setBlock(last, north, Block.UPDATE_CLIENTS);
        cast(wanda, recipe(Spell.PAINT).dab(8).at(last, Direction.UP).size(0).build());
        finish(wanda);
        int before = steps(wanda).size();
        helper.runAfterDelay(BuildSession.Stroke.CLOSES_AFTER + 2, () -> {
            check(steps(wanda).size() == before + 1 && level.getBlockState(last).is(Blocks.SPRUCE_STAIRS),
                    "a stroke without its release is sealed 40 ticks after its last dab");
            helper.succeed();
        });
    }

    // B23

    /** A 3 x 3 stone wall: Pull +2 east adds 18 stone; undo; Push -1 takes the wall's 9. */
    @EnhancedTest(template = ARENA, batch = "build_magic_pull")
    public static void pullGrowsAFaceAndPushTakesItIn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        BoundingBox wall = new BoundingBox(5, 2, 5, 5, 4, 7);
        BoundingBox front = new BoundingBox(6, 2, 5, 7, 4, 7);
        set(f, wall, Blocks.STONE);
        cast(wanda, recipe(Spell.PULL).at(abs(f, 5, 3, 6), Direction.EAST).size(2).build());
        finish(wanda);
        check(count(f, front, Blocks.STONE) == 18 && count(f, wall, Blocks.STONE) == 9, "pull +2 adds 18: " + count(f, front, Blocks.STONE));
        BuildMagic.undo(wanda);
        finish(wanda);
        check(count(f, front, Blocks.AIR) == 18 && count(f, wall, Blocks.STONE) == 9, "undone");
        cast(wanda, recipe(Spell.PULL).at(abs(f, 5, 3, 6), Direction.EAST).size(-1).build());
        finish(wanda);
        check(count(f, wall, Blocks.AIR) == 9, "push -1 takes the 9 in: " + count(f, wall, Blocks.STONE));
        helper.succeed();
    }

    // B24

    /**
     * Billy aims at the floor: a hollow Dome of radius 3 stands on the cell above it, 57 blocks and no floor, the floor
     * under it untouched, written in order of distance from its base centre (the first tenth all nearer than the last
     * tenth); a planted Tower of radius 2 whose look set its height to 6 has 6 layers.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_shapes", timeoutTicks = 200)
    public static void aShapeSitsOnTheSurfaceAndGrowsFromItsHeart(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer billy = builder(f, Power.BILLY, 0.5, 0.5, Blocks.STONE);
        UUID id = billy.getUUID();
        cast(billy, recipe(Spell.SHAPE).style(ShapeKind.DOME).size(3).hollow(true).at(abs(f, 10, 1, 10), Direction.UP).build());
        BuildJob job = working(billy);
        check(job != null, "the dome is cast");
        BoundingBox dome = abs(f, new BoundingBox(7, 2, 7, 13, 5, 13));
        List<BlockPos> order = new ArrayList<>();
        Set<BlockPos> placed = new HashSet<>();
        Budget one = Budget.of(1, 4096, 1);
        for (int step = 0; step < 2000 && !job.finished(); step++) {
            BuildScheduler.step(List.of(id), one);
            for (BlockPos pos : BlockPos.betweenClosed(dome.minX(), dome.minY(), dome.minZ(), dome.maxX(), dome.maxY(), dome.maxZ())) {
                if (level.getBlockState(pos).is(Blocks.STONE) && placed.add(pos.immutable())) {
                    order.add(pos.immutable());
                }
            }
        }
        check(job.finished() && order.size() == 57, "a hollow dome of 57: " + order.size());
        check(count(f, new BoundingBox(7, 1, 7, 13, 1, 13), Blocks.SMOOTH_STONE) == 49, "the floor under it is untouched");
        BlockPos heart = abs(f, 10, 2, 10);
        check(level.getBlockState(heart).isAir() && order.stream().allMatch(pos -> pos.getY() >= heart.getY()), "it stands on the floor, with no floor");
        int tenth = order.size() / 10;
        double nearLast = order.subList(0, tenth).stream().mapToDouble(pos -> pos.distSqr(heart)).max().orElseThrow();
        double farFirst = order.subList(order.size() - tenth, order.size()).stream().mapToDouble(pos -> pos.distSqr(heart)).min().orElseThrow();
        check(nearLast < farFirst, "it grows from its heart: " + nearLast + " < " + farFirst);

        ModAttachments.BUILD_DATA.set(billy, ModAttachments.BUILD_DATA.get(billy).withPauseUntil(0L));
        cast(billy, recipe(Spell.SHAPE).style(ShapeKind.TOWER).size(2).size2(6).hollow(true).planted(true).at(abs(f, 3, 1, 15), Direction.UP).build());
        finish(billy);
        Set<Integer> layers = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 1, 2, 13), abs(f, 5, 9, 17))) {
            if (level.getBlockState(pos).is(Blocks.STONE)) {
                layers.add(pos.getY());
            }
        }
        check(layers.size() == 6, "the tower has 6 layers: " + layers.size());
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Copy, Move, Repeat (D-B 4.8-4.10)

    // B25

    /**
     * Three oak stairs in an L, facing north, copied: stamped with one turn, they stand turned a quarter clockwise
     * round the base and face east; stamped flipped along x, mirrored; two stamps make two copies and leave the pocket
     * as it was.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_copy")
    public static void copyStampTurnAndFlip(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        BlockState north = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (BlockPos pos : List.of(abs(f, 3, 2, 3), abs(f, 4, 2, 3), abs(f, 3, 2, 4))) {
            level.setBlock(pos, north, Block.UPDATE_CLIENTS);
        }
        Seen.of(wanda);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(abs(f, 3, 2, 3), abs(f, 4, 2, 4)).at(abs(f, 3, 2, 3), Direction.UP).build());
        finish(wanda);
        Seen seen = Seen.of(wanda);
        Pocket pocket = Pocket.held(wanda.getUUID());
        check(seen.code() == BuildVerdictPayload.COPIED && pocket != null && pocket.cells() == 4, "the box is copied: " + seen);
        // The pocket goes to her client (its ghost), and again when the client asks for it: every cell as it is.
        BuildMagic.tick(f.level().getServer());
        checkPocketSent(wanda, pocket);
        BuildMagic.action(wanda, BuildActionPayload.POCKET, BlockPos.ZERO, BlockPos.ZERO, pocket.id());
        BuildMagic.tick(f.level().getServer());
        checkPocketSent(wanda, pocket);

        // A 2 x 1 x 2 copy stands on the looked cell (12, 1, 4): its low corner at (12, 2, 4).
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(abs(f, 12, 1, 4), Direction.UP).turn(1).build());
        finish(wanda);
        for (BlockPos pos : List.of(abs(f, 13, 2, 4), abs(f, 13, 2, 5), abs(f, 12, 2, 4))) {
            BlockState state = level.getBlockState(pos);
            check(state.is(Blocks.OAK_STAIRS) && state.getValue(StairBlock.FACING) == Direction.EAST,
                    "turned a quarter, facing east: " + pos + " " + state);
        }
        check(level.getBlockState(abs(f, 12, 2, 5)).isAir(), "the copy's air where the L is open");

        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(abs(f, 12, 1, 10), Direction.UP).flip(SpellRecipe.FLIP_X).build());
        finish(wanda);
        for (BlockPos pos : List.of(abs(f, 13, 2, 10), abs(f, 12, 2, 10), abs(f, 13, 2, 11))) {
            BlockState state = level.getBlockState(pos);
            check(state.is(Blocks.OAK_STAIRS) && state.getValue(StairBlock.FACING) == Direction.NORTH, "mirrored along x: " + pos + " " + state);
        }
        check(level.getBlockState(abs(f, 12, 2, 11)).isAir(), "mirrored, the open corner too");
        check(Pocket.held(wanda.getUUID()) == pocket && pocket.cells() == 4 && count(f, new BoundingBox(3, 2, 3, 4, 2, 4), Blocks.OAK_STAIRS) == 3,
                "two stamps, two copies, the pocket and the original as they were");
        helper.succeed();
    }

    /** The parts of {@code pocket} sent to {@code player} since the last look put together give every cell of it. */
    private static void checkPocketSent(ServerPlayer player, Pocket pocket) {
        List<BuildPocketPayload> parts = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof BuildPocketPayload part) {
                parts.add(part);
            }
        }
        BuildPocketPayload.Whole whole = BuildPocketPayload.join(parts);
        check(whole != null && whole.pocket() == pocket.id() && whole.sx() == pocket.sizeX() && whole.sy() == pocket.sizeY()
                && whole.sz() == pocket.sizeZ(), "the pocket is sent whole: " + parts.size() + " parts");
        int cells = pocket.sizeX() * pocket.sizeY() * pocket.sizeZ();
        BlockState[] sent = new BlockState[cells];
        boolean read = BuildPocketPayload.readRuns(whole.runs(), cells, whole.palette().size(), (first, length, index) -> {
            for (int i = 0; i < length; i++) {
                sent[(int) first + i] = index == 0 ? null : whole.palette().get(index - 1);
            }
        });
        check(read, "its runs cover it");
        for (int y = 0; y < pocket.sizeY(); y++) {
            for (int z = 0; z < pocket.sizeZ(); z++) {
                for (int x = 0; x < pocket.sizeX(); x++) {
                    check(Objects.equals(sent[(y * pocket.sizeZ() + z) * pocket.sizeX() + x], pocket.state(x, y, z)),
                            "its cell " + x + ", " + y + ", " + z + " as it is");
                }
            }
        }
    }

    /** The cells a pocket's stream gives a client, in its order (null: none). */
    private static BlockState[] streamed(Pocket.Stream stream, int cells) {
        for (int tick = 0; tick < 100 && !stream.step(); tick++) {
            // (a slice a tick)
        }
        List<BuildPocketPayload> parts = new ArrayList<>(stream.next(Integer.MAX_VALUE));
        BuildPocketPayload.Whole whole = BuildPocketPayload.join(parts);
        check(whole != null, "the stream's parts make a pocket");
        BlockState[] sent = new BlockState[cells];
        check(BuildPocketPayload.readRuns(whole.runs(), cells, whole.palette().size(), (first, length, index) -> {
            for (int i = 0; i < length; i++) {
                sent[(int) first + i] = index == 0 ? null : whole.palette().get(index - 1);
            }
        }), "its runs cover it");
        return sent;
    }

    // B26

    /**
     * A box copy of a hollow 3 x 3 x 3 stone room stamped into a dirt mound carves its inside; a magic-selected log
     * with four leaves stamped into a dirt cube changes only those 5 cells.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_copy_air")
    public static void aBoxCopyBringsItsAirAMaskCopyOnlyTheThing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        BoundingBox room = new BoundingBox(3, 2, 3, 5, 4, 5);
        set(f, room, Blocks.STONE);
        level.setBlock(abs(f, 4, 3, 4), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(abs(f, room)).at(abs(f, 3, 2, 3), Direction.UP).build());
        finish(wanda);
        BoundingBox mound = new BoundingBox(12, 2, 12, 14, 4, 14);
        set(f, mound, Blocks.DIRT);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(abs(f, 13, 1, 13), Direction.UP).build());
        finish(wanda);
        check(count(f, mound, Blocks.STONE) == 26 && level.getBlockState(abs(f, 13, 3, 13)).isAir(), "the room is carved into the mound: "
                + count(f, mound, Blocks.STONE));

        // A pocket too big to send whole goes to the client as its outer surface, and bigger still as its size alone
        // (D-B 13.1; here a solid 3 x 3 x 3 cube stands in for one of more than 262,144 cells).
        set(f, room, Blocks.STONE);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(abs(f, room)).at(abs(f, 3, 2, 3), Direction.UP).build());
        finish(wanda);
        Pocket solid = Pocket.held(wanda.getUUID());
        BlockState[] surface = streamed(solid.stream(26), 27);
        check(surface[13] == null && Arrays.stream(surface).filter(state -> state != null && state.is(Blocks.STONE)).count() == 26,
                "its surface: 26 stone, the middle left out");
        BlockState[] size = streamed(solid.stream(20), 27);
        check(Arrays.stream(size).allMatch(Objects::isNull), "a bigger one: its size alone");

        BlockPos log = abs(f, 4, 3, 14);
        level.setBlock(abs(f, 4, 2, 14), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            level.setBlock(log.relative(side), leaves, Block.UPDATE_CLIENTS);
        }
        Seen.of(wanda);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).mask(log).at(log, Direction.UP).build());
        finish(wanda);
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.COPIED && seen.verdict().extra() == 5, "the tree's 5 cells are copied: " + seen);
        BoundingBox cube = new BoundingBox(12, 2, 2, 14, 4, 4);
        set(f, cube, Blocks.DIRT);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(abs(f, 13, 1, 3), Direction.UP).build());
        finish(wanda);
        check(count(f, cube, Blocks.DIRT) == 22 && level.getBlockState(abs(f, 13, 2, 3)).is(Blocks.OAK_LOG)
                && count(f, cube, Blocks.OAK_LEAVES) == 4, "only its 5 cells change: " + count(f, cube, Blocks.DIRT));
        helper.succeed();
    }

    // B27

    /**
     * A chest with 5 diamonds, a pig and an item frame in a 3 x 3 x 3 box moved 5 east: the source is air, the chest
     * there holds the 5 diamonds, the pig and the frame are 5 east, no item lies about, and there are exactly 5
     * diamonds; one undo puts back both ends and the frame.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_move_carry")
    public static void moveCarriesBlocksContentsCreaturesAndFrames(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BlockPos chest = abs(f, 3, 2, 3);
        BlockPos stone = abs(f, 2, 3, 2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 5));
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemFrame frame = new ItemFrame(level, stone.east(), Direction.EAST);
        frame.setItem(new ItemStack(Items.APPLE));
        level.addFreshEntity(frame);
        Pig pig = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(4.5, FLOOR, 4.5));
        Vec3 pigAt = pig.position();
        BoundingBox source = new BoundingBox(2, 2, 2, 4, 4, 4);
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, source)).at(abs(f, 8, 1, 3), Direction.UP).build());
        finish(wanda);
        AABB arena = AABB.of(abs(f, new BoundingBox(0, 1, 0, 20, 10, 20))).inflate(2.0);
        check(count(f, source, Blocks.AIR) == 27, "the source is air: " + count(f, source, Blocks.AIR));
        check(level.getBlockEntity(chest.east(5)) instanceof ChestBlockEntity moved && moved.getItem(0).getCount() == 5,
                "the chest holds its 5 diamonds 5 east");
        check(level.getBlockState(stone.east(5)).is(Blocks.STONE), "the stone moved");
        check(pig.isAlive() && pig.position().distanceTo(pigAt.add(5.0, 0.0, 0.0)) < 1.0E-6, "the pig is 5 east: " + pig.position());
        check(!frame.isRemoved() && frame.getPos().equals(stone.east(6)) && frame.getDirection() == Direction.EAST && frame.survives(),
                "the frame hangs 5 east: " + frame.getPos());
        check(level.getEntitiesOfClass(ItemEntity.class, arena).isEmpty(), "no item lies about");
        check(diamonds(level, arena) == 5, "exactly 5 diamonds: " + diamonds(level, arena));
        BuildMagic.undo(wanda);
        finish(wanda);
        check(level.getBlockEntity(chest) instanceof ChestBlockEntity back && back.getItem(0).getCount() == 5
                && level.getBlockState(stone).is(Blocks.STONE),
                "one undo puts the source back");
        check(count(f, new BoundingBox(7, 2, 2, 9, 4, 4), Blocks.AIR) == 27, "and empties the target");
        check(!frame.isRemoved() && frame.getPos().equals(stone.east()) && frame.getDirection() == Direction.EAST,
                "the frame hangs where it was: " + frame.getPos());
        check(level.getEntitiesOfClass(ItemEntity.class, arena).isEmpty() && diamonds(level, arena) == 5, "still 5 diamonds, nothing dropped");
        helper.succeed();
    }

    /** The diamonds in the chests of {@code box}. */
    private static int diamonds(ServerLevel level, AABB box) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed((int) box.minX, (int) box.minY, (int) box.minZ, (int) box.maxX, (int) box.maxY, (int) box.maxZ)) {
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                n += chest.countItem(Items.DIAMOND);
            }
        }
        return n;
    }

    // B28

    /**
     * A row of 9 stone moved 10 east while a claim refuses the target cells from the sixth on: every refused cell's
     * source keeps its block, every allowed one moved, and there are 9 stone in all.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_move_claim")
    public static void aMoveNeverLosesWhatItCouldNotPlace(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        BoundingBox row = new BoundingBox(2, 2, 2, 2, 2, 10);
        set(f, row, Blocks.STONE);
        int line = abs(f, 0, 0, 6).getZ();
        int targetX = abs(f, 12, 0, 0).getX();
        listen(f, BlockEvent.EntityPlaceEvent.class, event -> {
            if (event.getPos().getX() == targetX && event.getPos().getZ() >= line) {
                event.setCanceled(true);
            }
        });
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, row)).at(abs(f, 12, 1, 6), Direction.UP).build());
        BuildJob job = working(wanda);
        finish(wanda);
        int moved = 0;
        int kept = 0;
        for (int z = 2; z <= 10; z++) {
            boolean there = level.getBlockState(abs(f, 12, 2, z)).is(Blocks.STONE);
            boolean here = level.getBlockState(abs(f, 2, 2, z)).is(Blocks.STONE);
            check(there != here, "the stone of row cell " + z + " is in one place, once");
            if (z >= 6) {
                check(here, "a refused target keeps its source: " + z);
            }
            moved += there ? 1 : 0;
            kept += here ? 1 : 0;
        }
        check(moved > 0 && kept >= 5 && moved + kept == 9 && job.left(Left.PROTECTED) == kept, "moved " + moved + ", kept " + kept);
        helper.succeed();
    }

    // B29

    /**
     * A 3 x 3 x 3 pattern moved one block east comes out exactly the pattern shifted by one. A 3 x 1 x 3 pattern turned
     * a quarter where it stands comes out exactly turned; turned again with someone standing on one of its corners (who
     * may not be buried), the corners stay as they were and the rest turns, every block in one place, once.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_move_overlap")
    public static void aMoveThatOverlapsItselfIsExact(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        Map<BlockPos, BlockState> pattern = new HashMap<>();
        Block[] kinds = {Blocks.STONE, Blocks.OAK_PLANKS, Blocks.GLASS, Blocks.DIRT};
        int i = 0;
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 2, 2, 2), abs(f, 4, 4, 4))) {
            BlockState state = kinds[i++ % kinds.length].defaultBlockState();
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            pattern.put(pos.immutable(), state);
        }
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 2, 2, 2), abs(f, 4, 4, 4)).at(abs(f, 4, 1, 3), Direction.UP).build());
        finish(wanda);
        for (Map.Entry<BlockPos, BlockState> cell : pattern.entrySet()) {
            check(level.getBlockState(cell.getKey().east()) == cell.getValue(), "the pattern moved one east: " + cell.getKey());
        }
        check(count(f, new BoundingBox(2, 2, 2, 2, 4, 4), Blocks.AIR) == 9, "its old west side is empty");

        Map<BlockPos, BlockState> square = woolSquare(f, 2, 12);
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 2, 2, 12), abs(f, 4, 2, 14))
                .at(abs(f, 3, 1, 13), Direction.UP).turn(1).build());
        finish(wanda);
        for (Map.Entry<BlockPos, BlockState> cell : square.entrySet()) {
            BlockPos local = cell.getKey().subtract(abs(f, 2, 2, 12));
            BlockPos turned = abs(f, 2 + 2 - local.getZ(), 2, 12 + local.getX());
            check(level.getBlockState(turned) == cell.getValue(), "turned a quarter where it stands: " + local + " to " + turned);
        }

        Map<BlockPos, BlockState> second = woolSquare(f, 10, 12);
        BlockPos corner = abs(f, 10, 2, 12);
        ServerPlayer friend = f.player(10.5, FLOOR, 12.5);
        cast(wanda, recipe(Spell.MOVE).op(SpellRecipe.Op.PUT_DOWN).box(abs(f, 10, 2, 12), abs(f, 12, 2, 14))
                .at(abs(f, 11, 1, 13), Direction.UP).turn(1).build());
        BuildJob job = working(wanda);
        finish(wanda);
        Map<BlockState, Integer> seen = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 10, 2, 12), abs(f, 12, 2, 14))) {
            seen.merge(level.getBlockState(pos), 1, Integer::sum);
        }
        check(seen.size() == 9 && seen.values().stream().allMatch(n -> n == 1) && second.values().stream().allMatch(seen::containsKey),
                "every block in one place, once: " + seen);
        for (BlockPos pos : List.of(abs(f, 10, 2, 12), abs(f, 12, 2, 12), abs(f, 12, 2, 14), abs(f, 10, 2, 14))) {
            check(level.getBlockState(pos) == second.get(pos), "a corner stays as it was: " + pos);
        }
        BlockPos edge = abs(f, 11, 2, 12);
        check(level.getBlockState(abs(f, 12, 2, 13)) == second.get(edge), "and an edge turned");
        check(friend.blockPosition().equals(corner) && job.left(Left.BODIES) == 1, "the friend stood where he was, left once: " + job.left(Left.BODIES));
        helper.succeed();
    }

    /** Nine kinds of wool in a 3 x 1 x 3 square at the arena's {@code x, 2, z} and on, each cell another. */
    private static Map<BlockPos, BlockState> woolSquare(TestKit.Fixture f, int x, int z) {
        Block[] wool = {Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL,
                Blocks.PINK_WOOL, Blocks.GRAY_WOOL, Blocks.CYAN_WOOL};
        Map<BlockPos, BlockState> square = new HashMap<>();
        int i = 0;
        for (int dz = 0; dz < 3; dz++) {
            for (int dx = 0; dx < 3; dx++) {
                BlockPos pos = abs(f, x + dx, 2, z + dz);
                BlockState state = wool[i++].defaultBlockState();
                f.level().setBlock(pos, state, Block.UPDATE_CLIENTS);
                square.put(pos, state);
            }
        }
        return square;
    }

    // B30

    /** A 1 x 2 x 1 pillar repeated 3 times east with a gap of 1: pillars at +2, +4, +6; one step; undo takes all 6. */
    @EnhancedTest(template = ARENA, batch = "build_magic_repeat")
    public static void repeatPlacesARowWithGaps(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BoundingBox pillar = new BoundingBox(3, 2, 3, 3, 3, 3);
        set(f, pillar, Blocks.STONE);
        cast(wanda, recipe(Spell.REPEAT).box(abs(f, pillar)).at(abs(f, 3, 2, 3), Direction.EAST).size(3).size2(1).build());
        finish(wanda);
        BoundingBox row = new BoundingBox(4, 2, 3, 9, 3, 3);
        for (int dx = 2; dx <= 6; dx += 2) {
            check(count(f, new BoundingBox(3 + dx, 2, 3, 3 + dx, 3, 3), Blocks.STONE) == 2, "a pillar at +" + dx);
        }
        check(count(f, row, Blocks.STONE) == 6 && steps(wanda).size() == 1 && steps(wanda).get(0).changed() == 6, "the row is one step of 6");
        BuildMagic.undo(wanda);
        finish(wanda);
        check(count(f, row, Blocks.AIR) == 12 && count(f, pillar, Blocks.STONE) == 2, "undo takes all 6");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The block (D-B 5)

    // B31

    /**
     * The middle click on an upside-down oak stair facing east: Fill makes upside-down stairs facing east; after a
     * change of hotbar slot Fill uses the hand again; a pick of bedrock, and a spawner from the palette, are {@code
     * BAD_BLOCK}; the hotbar's mix builds the very blocks its seed picks (D-B 5.3).
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_pick")
    public static void aPickTakesTheExactStateAndTheHandComesBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        wanda.getInventory().setItem(1, new ItemStack(Blocks.OAK_PLANKS));
        BlockPos stair = abs(f, 2, 2, 2);
        level.setBlock(stair, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP),
                Block.UPDATE_CLIENTS);
        Seen.of(wanda);
        BuildMagic.action(wanda, BuildActionPayload.PICK, stair, BlockPos.ZERO, 0);
        check(Seen.of(wanda).said("message.enhanced.build.picked"), "she picked it");
        BoundingBox first = new BoundingBox(5, 2, 5, 7, 2, 5);
        cast(wanda, recipe(Spell.FILL).box(abs(f, first)).at(abs(f, 5, 2, 5), Direction.UP).material(SpellRecipe.PICKED).build());
        finish(wanda);
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 5, 2, 5), abs(f, 7, 2, 5))) {
            BlockState state = level.getBlockState(pos);
            check(state.is(Blocks.OAK_STAIRS) && state.getValue(StairBlock.FACING) == Direction.EAST && state.getValue(StairBlock.HALF) == Half.TOP,
                    "upside-down stairs facing east: " + state);
        }
        wanda.getInventory().selected = 1;
        BoundingBox second = new BoundingBox(9, 2, 5, 11, 2, 5);
        cast(wanda, recipe(Spell.FILL).box(abs(f, second)).at(abs(f, 9, 2, 5), Direction.UP).material(SpellRecipe.PICKED).build());
        finish(wanda);
        check(count(f, second, Blocks.OAK_PLANKS) == 3, "another slot: the hand again");

        BlockPos bedrock = abs(f, 2, 2, 8);
        level.setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        Seen.of(wanda);
        BuildMagic.action(wanda, BuildActionPayload.PICK, bedrock, BlockPos.ZERO, 0);
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.BAD_BLOCK && seen.said("message.enhanced.build.bad_block"), "bedrock: BAD_BLOCK: " + seen);
        BuildMagic.action(wanda, BuildActionPayload.PICK_BLOCK, BlockPos.ZERO, BlockPos.ZERO, BuiltInRegistries.BLOCK.getId(Blocks.SPAWNER));
        seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.BAD_BLOCK, "a spawner from the palette: BAD_BLOCK: " + seen);
        BuildMagic.action(wanda, BuildActionPayload.PICK_BLOCK, BlockPos.ZERO, BlockPos.ZERO, BuiltInRegistries.BLOCK.getId(Blocks.GLASS));
        BoundingBox third = new BoundingBox(13, 2, 5, 15, 2, 5);
        cast(wanda, recipe(Spell.FILL).box(abs(f, third)).at(abs(f, 13, 2, 5), Direction.UP).material(SpellRecipe.PICKED).build());
        finish(wanda);
        check(count(f, third, Blocks.GLASS) == 3, "glass from the palette");

        // The hotbar's mix: 3 stone and 1 oak planks, weighted, a block per cell by the seed; a stick and bedrock
        // are left out.
        wanda.getInventory().setItem(0, new ItemStack(Blocks.STONE, 3));
        wanda.getInventory().setItem(2, new ItemStack(Items.STICK));
        wanda.getInventory().setItem(3, new ItemStack(Blocks.BEDROCK));
        BoundingBox mixed = new BoundingBox(5, 2, 10, 8, 2, 13);
        long seed = 1234L;
        cast(wanda, recipe(Spell.FILL).box(abs(f, mixed)).at(abs(f, 5, 2, 10), Direction.UP).mix().seed(seed).build());
        finish(wanda);
        SpellMath.Mix mix = new SpellMath.Mix(List.of(new SpellMath.Mix.Part(Blocks.STONE.defaultBlockState(), 3),
                new SpellMath.Mix.Part(Blocks.OAK_PLANKS.defaultBlockState(), 1)));
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 5, 2, 10), abs(f, 8, 2, 13))) {
            BlockState expected = mix.pick(pos.getX(), pos.getY(), pos.getZ(), seed);
            check(level.getBlockState(pos) == expected, "the mix's block at " + pos + ": " + level.getBlockState(pos) + ", not " + expected);
        }
        check(count(f, mixed, Blocks.STONE) > 0 && count(f, mixed, Blocks.OAK_PLANKS) > 0, "both blocks of the mix");
        helper.succeed();
    }

    // B32

    /** A water bucket fills water sources; the Scarlet Witch's lava bucket fills lava; Billy's is {@code VOLATILE}. */
    @EnhancedTest(template = ARENA, batch = "build_magic_fluids")
    public static void fluids(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, null);
        wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        basin(f, 4, 4);
        BlockPos water = abs(f, 4, 2, 4);
        cast(wanda, recipe(Spell.FILL).box(water, water).at(water, Direction.UP).build());
        finish(wanda);
        check(level.getBlockState(water).is(Blocks.WATER) && level.getFluidState(water).isSource(), "a water source");
        wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        basin(f, 10, 4);
        BlockPos lava = abs(f, 10, 2, 4);
        cast(wanda, recipe(Spell.FILL).box(lava, lava).at(lava, Direction.UP).build());
        finish(wanda);
        check(level.getBlockState(lava).is(Blocks.LAVA) && level.getFluidState(lava).isSource(), "her lava");

        ServerPlayer billy = builder(f, Power.BILLY, 20.5, 0.5, null);
        billy.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        basin(f, 16, 4);
        BlockPos his = abs(f, 16, 2, 4);
        Seen.of(billy);
        cast(billy, recipe(Spell.FILL).box(his, his).at(his, Direction.UP).build());
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.VOLATILE && working(billy) == null && level.getBlockState(his).isAir(), "Billy's: VOLATILE: " + seen);
        helper.succeed();
    }

    // B33

    /**
     * A Vanish over only the lower half of an oak door and the foot of a bed takes both halves and both parts; undo
     * brings them back whole; an oak door is no Fill material ({@code TWO_BLOCK}); a copy holding only a door's lower
     * half stamps no door.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_doors")
    public static void doorsAndBedsAreNeverCutInHalf(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BlockPos lower = abs(f, 3, 2, 3);
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        level.setBlock(lower, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
        level.setBlock(lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        BlockPos foot = abs(f, 6, 2, 3);
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
        level.setBlock(foot.north(), bed.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
        BlockState lowerHalf = level.getBlockState(lower);
        BlockState upperHalf = level.getBlockState(lower.above());
        BlockState footPart = level.getBlockState(foot);
        BlockState headPart = level.getBlockState(foot.north());
        cast(wanda, styled(f, Spell.VANISH, VanishStyle.ALL, new BoundingBox(3, 2, 3, 6, 2, 3)));
        finish(wanda);
        check(level.getBlockState(lower).isAir() && level.getBlockState(lower.above()).isAir(), "both halves of the door are gone");
        check(level.getBlockState(foot).isAir() && level.getBlockState(foot.north()).isAir(), "both parts of the bed are gone");
        BuildMagic.undo(wanda);
        finish(wanda);
        check(level.getBlockState(lower).equals(lowerHalf) && level.getBlockState(lower.above()).equals(upperHalf), "the door comes back whole");
        check(level.getBlockState(foot).equals(footPart) && level.getBlockState(foot.north()).equals(headPart), "and the bed");

        wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_DOOR));
        Seen.of(wanda);
        cast(wanda, styled(f, Spell.FILL, FillStyle.SOLID, new BoundingBox(10, 2, 10, 11, 2, 11)));
        Seen seen = Seen.of(wanda);
        check(seen.code() == BuildVerdictPayload.TWO_BLOCK && seen.said("message.enhanced.build.two_block"), "a door is no Fill material: " + seen);

        BlockPos stone = abs(f, 4, 2, 3);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(lower, stone).at(lower, Direction.UP).build());
        finish(wanda);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(abs(f, 12, 1, 14), Direction.UP).build());
        finish(wanda);
        check(level.getBlockState(abs(f, 13, 2, 14)).is(Blocks.STONE), "the copy's stone is stamped");
        check(count(f, new BoundingBox(10, 2, 12, 15, 4, 16), Blocks.OAK_DOOR) == 0, "but no half door");
        helper.succeed();
    }

    // B34

    /**
     * A row of 5 oak fences filled in one spell: the middle one joins east and west. Vanishing a wall whose outside
     * face holds a wall torch takes the torch with no item dropped; undo puts back the wall and the torch.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_settle")
    public static void theSettleStepJoinsFencesAndTorchesComeBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.OAK_FENCE);
        cast(wanda, styled(f, Spell.FILL, FillStyle.SOLID, new BoundingBox(3, 2, 3, 7, 2, 3)));
        finish(wanda);
        BlockState middle = level.getBlockState(abs(f, 5, 2, 3));
        check(middle.is(Blocks.OAK_FENCE) && middle.getValue(FenceBlock.EAST) && middle.getValue(FenceBlock.WEST),
                "the middle fence joins both sides: " + middle);

        BoundingBox wall = new BoundingBox(10, 2, 10, 12, 4, 10);
        set(f, wall, Blocks.STONE);
        BlockPos torch = abs(f, 11, 3, 11);
        BlockState lit = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH);
        level.setBlock(torch, lit, Block.UPDATE_CLIENTS);
        AABB arena = AABB.of(abs(f, new BoundingBox(0, 1, 0, 20, 10, 20))).inflate(2.0);
        cast(wanda, styled(f, Spell.VANISH, VanishStyle.ALL, wall));
        finish(wanda);
        check(count(f, wall, Blocks.AIR) == 9 && level.getBlockState(torch).isAir(), "the wall and its torch are gone");
        check(level.getEntitiesOfClass(ItemEntity.class, arena).isEmpty(), "no item dropped");
        BuildMagic.undo(wanda);
        finish(wanda);
        check(count(f, wall, Blocks.STONE) == 9 && level.getBlockState(torch).equals(lit), "undo puts back the wall and the torch");
        helper.succeed();
    }

    // B35

    /**
     * A 3 x 1 x 3 sand fill two blocks over the floor: 40 ticks later no sand has fallen; air set beside one with
     * {@code setBlockAndUpdate} makes it fall within 10 ticks (it is ordinary sand).
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_sand", timeoutTicks = 100)
    public static void floatingSandStaysWhereMagicPutIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.SAND);
        BoundingBox roof = new BoundingBox(5, 4, 5, 7, 4, 7);
        cast(wanda, styled(f, Spell.FILL, FillStyle.SOLID, roof));
        finish(wanda);
        check(count(f, roof, Blocks.SAND) == 9, "9 sand in the air");
        AABB around = AABB.of(abs(f, new BoundingBox(3, 1, 3, 9, 6, 9)));
        BlockPos corner = abs(f, 5, 4, 5);
        BlockPos beside = corner.west();
        helper.runAfterDelay(40, () -> {
            check(level.getEntitiesOfClass(FallingBlockEntity.class, around).isEmpty() && count(f, roof, Blocks.SAND) == 9,
                    "40 ticks later it has not fallen");
            level.setBlock(beside, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlockAndUpdate(beside, Blocks.AIR.defaultBlockState());
        });
        helper.runAfterDelay(52, () -> {
            check(!level.getBlockState(corner).is(Blocks.SAND), "the sand touched falls: " + level.getBlockState(corner));
            helper.succeed();
        });
    }

    // B36

    /**
     * A cow in a 3-high fill stands on top of it, alive and unhurt; a Scarlet Witch inside her own fill stands on it.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_lift")
    public static void creaturesAreLiftedOutOfNewBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 15.5, 15.5, Blocks.STONE);
        Cow cow = TestKit.spawnWithNoFreeWill(helper, EntityType.COW, new Vec3(5.5, FLOOR, 5.5));
        float health = cow.getHealth();
        cast(wanda, styled(f, Spell.FILL, FillStyle.SOLID, new BoundingBox(4, 2, 4, 6, 4, 6)));
        finish(wanda);
        double top = f.helper().absoluteVec(new Vec3(0.0, 5.0, 0.0)).y;
        check(cow.isAlive() && cow.getHealth() == health && Math.abs(cow.getY() - top) < 1.0E-6,
                "the cow stands on top, unhurt: " + cow.getY() + " / " + top);
        cast(wanda, styled(f, Spell.FILL, FillStyle.SOLID, new BoundingBox(14, 2, 14, 16, 3, 16)));
        finish(wanda);
        double hers = f.helper().absoluteVec(new Vec3(0.0, 4.0, 0.0)).y;
        check(Math.abs(wanda.getY() - hers) < 1.0E-6, "she stands on her own fill: " + wanda.getY() + " / " + hers);
        helper.succeed();
    }

    // B37

    /**
     * A chest with 3 diamonds and an empty chest in a box: Fill, Vanish and Change each leave the full one with its 3
     * diamonds and replace the empty one; the message counts 1; no item lies about.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_chests")
    public static void chestsWithThingsAreLeftStanding(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, Blocks.STONE);
        BlockPos full = abs(f, 3, 2, 3);
        BlockPos empty = abs(f, 5, 2, 3);
        BoundingBox box = new BoundingBox(3, 2, 3, 5, 2, 3);
        level.setBlock(full, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        ((ChestBlockEntity) level.getBlockEntity(full)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        AABB arena = AABB.of(abs(f, new BoundingBox(0, 1, 0, 20, 10, 20))).inflate(2.0);
        record Pass(Spell spell, Enum<?> style, Block material) {
        }
        for (Pass pass : List.of(new Pass(Spell.FILL, FillStyle.SOLID, Blocks.STONE), new Pass(Spell.VANISH, VanishStyle.ALL, Blocks.STONE),
                new Pass(Spell.CHANGE, ChangeStyle.ALL, Blocks.OAK_PLANKS))) {
            level.setBlock(empty, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(pass.material()));
            Seen.of(wanda);
            cast(wanda, styled(f, pass.spell(), pass.style(), box));
            finish(wanda);
            Seen seen = Seen.of(wanda);
            check(level.getBlockEntity(full) instanceof ChestBlockEntity chest
                    && chest.countItem(Items.DIAMOND) == 3, pass.spell() + ": the full chest stays");
            check(!level.getBlockState(empty).is(Blocks.CHEST), pass.spell() + ": the empty one is replaced");
            check(seen.arg("message.enhanced.build.left.containers", 0).equals("1"), pass.spell() + ": counted 1: " + seen);
            check(level.getEntitiesOfClass(ItemEntity.class, arena).isEmpty(), pass.spell() + ": no item");
        }
        helper.succeed();
    }

    // B38

    /**
     * Bedrock, a spawner, a command block and Mjolnir lying as a block: the Scarlet Witch's Vanish leaves all four; a
     * copy of them stamped over dirt leaves the dirt, and there is still exactly one Mjolnir; TNT in Billy's hand is
     * {@code VOLATILE}; 27 TNT of hers are done.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_untouchable")
    public static void untouchablesStayAndAreNeverCopied(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        List<BlockPos> four = List.of(abs(f, 3, 2, 3), abs(f, 4, 2, 3), abs(f, 5, 2, 3), abs(f, 6, 2, 3));
        level.setBlock(four.get(0), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(four.get(1), Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(four.get(2), Blocks.COMMAND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        check(MjolnirBlock.setDown(level, four.get(3), new ItemStack(ModItems.MJOLNIR.get())), "(Mjolnir lies there)");
        List<BlockState> states = four.stream().map(level::getBlockState).toList();
        BoundingBox row = new BoundingBox(3, 2, 3, 6, 2, 3);
        cast(wanda, styled(f, Spell.VANISH, VanishStyle.ALL, row));
        finish(wanda);
        for (int i = 0; i < four.size(); i++) {
            check(level.getBlockState(four.get(i)).equals(states.get(i)), "Vanish leaves it: " + states.get(i));
        }
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(abs(f, row)).at(four.get(0), Direction.UP).build());
        finish(wanda);
        BoundingBox target = new BoundingBox(11, 2, 12, 14, 2, 12);
        set(f, target, Blocks.DIRT);
        cast(wanda, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(abs(f, 12, 1, 12), Direction.UP).build());
        finish(wanda);
        check(count(f, target, Blocks.DIRT) == 4, "the copy leaves those cells as they were: " + count(f, target, Blocks.DIRT));
        int hammers = 0;
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 0, 1, 0), abs(f, 20, 10, 20))) {
            hammers += level.getBlockEntity(pos) instanceof MjolnirBlockEntity ? 1 : 0;
        }
        check(hammers == 1, "exactly one Mjolnir: " + hammers);

        ServerPlayer billy = builder(f, Power.BILLY, 20.5, 0.5, Blocks.TNT);
        Seen.of(billy);
        cast(billy, styled(f, Spell.FILL, FillStyle.SOLID, new BoundingBox(16, 2, 2, 18, 4, 4)));
        Seen seen = Seen.of(billy);
        check(seen.code() == BuildVerdictPayload.VOLATILE && working(billy) == null, "TNT in Billy's hand: VOLATILE: " + seen);
        wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.TNT));
        BoundingBox tnt = new BoundingBox(2, 2, 8, 4, 4, 10);
        Seen.of(wanda);
        cast(wanda, styled(f, Spell.FILL, FillStyle.SOLID, tnt));
        finish(wanda);
        check(count(f, tnt, Blocks.TNT) == 27 && Seen.of(wanda).code() == BuildVerdictPayload.DONE, "27 TNT of hers are done");
        helper.succeed();
    }

    // B39

    /**
     * A sign reading "HI" stamps reading "HI"; Billy's copy of a chest with 3 diamonds stamps empty, the Scarlet
     * Witch's full; suspicious sand with a loot table stamps without one.
     */
    @EnhancedTest(template = ARENA, batch = "build_magic_data")
    public static void copiesKeepOnlyWhitelistedData(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        ServerPlayer billy = builder(f, Power.BILLY, 2.5, 20.5, null);
        BlockPos sign = abs(f, 3, 2, 3);
        level.setBlock(sign, Blocks.OAK_SIGN.defaultBlockState(), Block.UPDATE_CLIENTS);
        ((SignBlockEntity) level.getBlockEntity(sign)).setText(new SignText().setMessage(0, Component.literal("HI")), true);
        BlockPos chest = abs(f, 3, 2, 6);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        BlockPos sand = abs(f, 3, 2, 9);
        level.setBlock(sand, Blocks.SUSPICIOUS_SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        ((BrushableBlockEntity) level.getBlockEntity(sand)).setLootTable(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, 7L);
        check(level.getBlockEntity(sand).saveWithoutMetadata(level.registryAccess()).contains("LootTable"), "(the sand has its loot table)");

        copyAndStamp(wanda, sign, abs(f, 12, 1, 3));
        check(level.getBlockEntity(abs(f, 12, 2, 3)) instanceof SignBlockEntity stamped
                && stamped.getFrontText().getMessage(0, false).getString().equals("HI"), "the sign reads HI");
        copyAndStamp(billy, chest, abs(f, 12, 1, 6));
        check(level.getBlockEntity(abs(f, 12, 2, 6)) instanceof ChestBlockEntity his && his.isEmpty(), "Billy's chest comes out empty");
        copyAndStamp(wanda, chest, abs(f, 15, 1, 6));
        check(level.getBlockEntity(abs(f, 15, 2, 6)) instanceof ChestBlockEntity hers && hers.countItem(Items.DIAMOND) == 3, "hers full");
        copyAndStamp(wanda, sand, abs(f, 12, 1, 9));
        BlockPos copied = abs(f, 12, 2, 9);
        check(level.getBlockState(copied).is(Blocks.SUSPICIOUS_SAND)
                && !level.getBlockEntity(copied).saveWithoutMetadata(level.registryAccess()).contains("LootTable"), "suspicious sand without its loot");
        helper.succeed();
    }

    /**
     * Copies the cell {@code from} and stamps it on the looked floor cell {@code onto} (it lands on the cell above).
     */
    private static void copyAndStamp(ServerPlayer player, BlockPos from, BlockPos onto) {
        cast(player, recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(from, from).at(from, Direction.UP).build());
        finish(player);
        cast(player, recipe(Spell.COPY).op(SpellRecipe.Op.STAMP).at(onto, Direction.UP).build());
        finish(player);
    }

    // B40

    /** A Witches' Road door stamped by magic is one of the Road's doors; vanished, it is not. */
    @EnhancedTest(template = ARENA, batch = "build_magic_road")
    public static void roadDoorsFollowBuilding(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 20.5, null);
        BlockPos door = abs(f, 3, 2, 3);
        level.setBlockAndUpdate(door, ModBlocks.WITCHES_ROAD_DOOR.get().defaultBlockState());
        f.onClose(() -> level.setBlockAndUpdate(door, Blocks.AIR.defaultBlockState()));
        copyAndStamp(wanda, door, abs(f, 12, 1, 3));
        BlockPos stamped = abs(f, 12, 2, 3);
        check(level.getBlockState(stamped).is(ModBlocks.WITCHES_ROAD_DOOR.get()) && WitchesRoad.doors(level).doors().contains(stamped),
                "the stamped door joins the Road");
        cast(wanda, recipe(Spell.VANISH).box(stamped, stamped).at(stamped, Direction.UP).build());
        finish(wanda);
        check(level.getBlockState(stamped).isAir() && !WitchesRoad.doors(level).doors().contains(stamped), "vanished, it leaves the Road");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------

    /**
     * A box spell of the arena's {@code box} in {@code style}, its anchor the box's low corner looked at from above.
     */
    private static SpellRecipe styled(TestKit.Fixture f, Spell spell, Enum<?> style, BoundingBox box) {
        BoundingBox world = abs(f, box);
        return recipe(spell).style(style).box(world).at(new BlockPos(world.minX(), world.minY(), world.minZ()), Direction.UP).build();
    }

    /** Whether the player {@code id} is in build mode (the server's own knowledge). */
    private static boolean mode(UUID id) {
        BuildSession session = BuildSession.existing(id);
        return session != null && session.mode();
    }

    // ------------------------------------------------------------------------------------------------------------
    // A Move and what changed under it (C12)

    // B86

    /**
     * A Move of a box holding a chest with 5 diamonds and two iron blocks, one cell a tick: after it read its source and
     * before those cells' turn, another player takes the 5 diamonds, takes one iron block, and places a block into a
     * source cell. Afterwards there are exactly 5 diamonds and 2 iron blocks in all (inventories, the world, items on the
     * ground); the emptied chest is where it was; the other iron block moved; the placed block stays; {@code
     * kept_changed} 3 (C12, R49). An overlapping Move by one cell (B29) still comes out exact.
     */
    @EnhancedTest(template = ARENA, batch = "build_move_cas", timeoutTicks = 400)
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
