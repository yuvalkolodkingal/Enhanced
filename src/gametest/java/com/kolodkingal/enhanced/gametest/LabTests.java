package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.AdamantiumRackBlock;
import com.kolodkingal.enhanced.block.CureStationBlock;
import com.kolodkingal.enhanced.block.LabDoors;
import com.kolodkingal.enhanced.block.SecretBookshelfBlock;
import com.kolodkingal.enhanced.block.WeaponXPodBlock;
import com.kolodkingal.enhanced.block.XDoorBlock;
import com.kolodkingal.enhanced.block.XDoorRoundBlock;
import com.kolodkingal.enhanced.config.SchoolLabConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.school.LabBuilder;
import com.kolodkingal.enhanced.school.LabEvents;
import com.kolodkingal.enhanced.school.LabSite;
import com.kolodkingal.enhanced.school.LabTemplate;
import com.kolodkingal.enhanced.school.Lift;
import com.kolodkingal.enhanced.school.NoHoles;
import com.kolodkingal.enhanced.school.SchoolLabs;
import com.kolodkingal.enhanced.school.SecretEntrance;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The X-Men lower lab (3.7, lane L4, the plan's 6 and 13): its template (the fixed points, the shell, each door framed,
 * water only in the tank), its blocks (the X-doors and the round door open for a player near and shut behind him, the
 * secret shelves swing open and close when nobody stands in them, the rack gives one injection per player, the Cure
 * Station fills a bottle once per cooldown, the lab's machines are unbreakable in Survival), the entrance (only over
 * the school's own blocks; forced over a player's; the lectern's notes), the lift (down and up after 20 ticks of
 * Sneak, never a mob, to the front door when the study pad is gone) and the builder far from the arenas: in all four
 * turns, under a real school (its cellar untouched, within the time budget), resumed after a crash to the same
 * blocks, its shell shutting out water. Batches {@code school_lab} (blocks and entrance in the arena), {@code
 * school_lab_gen} (the builds and the lift, far away, chunks forced) and {@code school_lab_cfg} (a switch turned).
 */
public final class LabTests {
    static final String BATCH = "school_lab";
    static final String GEN = "school_lab_gen";
    static final String CFG = "school_lab_cfg";
    /**
     * The budget of one tick of building (the server thread's CPU time, ms), of the entrance's own tick (a few blocks in
     * the study and the lectern's book) and of the whole build under ground (ticks).
     */
    static final double TICK_MS = 3.0;
    /**
     * How many ticks of a build may pass {@link #TICK_MS} (never twice it): the builder looks at the clock every few
     * cells, but in a big test run the JVM's own work (the collector's card refinement on the server thread, a page
     * fault) now and then lands inside one block placement and costs a tick a few milliseconds the builder cannot see
     * coming.
     */
    static final int NOISY_TICKS = 2;
    static final double ENTRANCE_MS = 10.0;
    static final int BUILD_TICKS = 60;
    /** Where the far tests put their schools (x; z = a row each), and at what height (a school's floor layer). */
    static final int FAR_X = 520_000;
    static final int FAR_Y = 70;

    // ------------------------------------------------------------------------------------------------ the template

    /** The template as a block getter: void is stone, as the builder makes it under a school. */
    static BlockGetter templateGetter(LabTemplate t) {
        return new BlockGetter() {
            @Override
            public BlockEntity getBlockEntity(BlockPos pos) {
                return null;
            }

            @Override
            public BlockState getBlockState(BlockPos p) {
                if (!t.inside(p.getX(), p.getY(), p.getZ())) {
                    return Blocks.STONE.defaultBlockState();
                }
                BlockState s = t.at(p.getX(), p.getY(), p.getZ());
                return s == null ? Blocks.STONE.defaultBlockState() : s;
            }

            @Override
            public FluidState getFluidState(BlockPos pos) {
                return getBlockState(pos).getFluidState();
            }

            @Override
            public int getHeight() {
                return 512;
            }

            @Override
            public int getMinBuildHeight() {
                return -256;
            }
        };
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theTemplateHoldsThePlanAndHasNoHoles(GameTestHelper helper) throws Exception {
        LabTemplate t = LabTemplate.get(helper.getLevel());
        check(t.sx == 45 && t.sy == 28 && t.sz == 42, "45 x 28 x 42");
        Map<BlockPos, Block> want = new java.util.HashMap<>();
        SchoolLabPlan.LAB_PADS.forEach(p -> want.put(p, ModBlocks.ELEVATOR_PAD.get()));
        want.put(SchoolLabPlan.CURE_STATION, ModBlocks.CURE_STATION.get());
        want.put(SchoolLabPlan.ADAMANTIUM_RACK, ModBlocks.ADAMANTIUM_RACK.get());
        want.put(SchoolLabPlan.WEAPON_X_POD, ModBlocks.WEAPON_X_POD.get());
        want.put(SchoolLabPlan.WEAPON_X_POD.above(), ModBlocks.WEAPON_X_POD.get());
        want.put(SchoolLabPlan.CEREBRO_DOOR, ModBlocks.X_DOOR_ROUND.get());
        want.put(SchoolLabPlan.CEREBRO_CONSOLE, ModBlocks.CEREBRO_CONSOLE.get());
        BoundingBox walk = SchoolLabPlan.WALKWAY;
        for (BlockPos p : BlockPos.betweenClosed(walk.minX(), walk.minY(), walk.minZ(), walk.maxX(), walk.maxY(),
                walk.maxZ())) {
            check(t.at(p.getX(), p.getY(), p.getZ()) != null && !t.at(p.getX(), p.getY(), p.getZ()).isAir(),
                    "the walkway is whole at " + p);
        }
        for (Map.Entry<BlockPos, Block> e : want.entrySet()) {
            BlockPos p = e.getKey();
            BlockState s = t.at(p.getX(), p.getY(), p.getZ());
            check(s != null && s.is(e.getValue()), p + " holds " + e.getValue() + ", not " + s);
        }
        BlockPos c = SchoolLabPlan.CEREBRO_CENTER;
        check(t.at(c.getX(), c.getY(), c.getZ()).isAir(), "Cerebro's middle is open");
        BlockGetter getter = templateGetter(t);
        int faces = 0;
        for (int x = 0; x < t.sx; x++) {
            for (int y = 0; y < t.sy; y++) {
                for (int z = 0; z < t.sz; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = t.at(x, y, z);
                    if (t.face(x, y, z)) {
                        faces++;
                        check(s != null && NoHoles.whole(getter, p), "the shell is whole at " + p + ": " + s);
                    }
                    check(NoHoles.framed(getter, p), "the door at " + p + " is framed");
                    if (s != null && !s.getFluidState().isEmpty()) {
                        BoundingBox tank = SchoolLabPlan.TANK_BOX;
                        check(x > tank.minX() && x < tank.maxX() && y > tank.minY() && y < tank.maxY()
                                && z > tank.minZ() && z < tank.maxZ(), "water only inside the tank's glass: " + p);
                    }
                }
            }
        }
        check(faces == t.faceCount(), "every face counted");
        MinecraftServer server = helper.getLevel().getServer();
        try (InputStream in = server.getResourceManager().getResourceOrThrow(ResourceLocation.fromNamespaceAndPath(
                Enhanced.MODID, "structure/mutant_school/lower_lab.nbt")).open()) {
            int bytes = in.readAllBytes().length;
            check(bytes <= 200 * 1024, "the template is within 200 KB: " + bytes);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ the blocks

    static void xDoor(GameTestHelper helper, BlockPos left, Direction facing) {
        BlockState leaf = ModBlocks.X_DOOR.get().defaultBlockState().setValue(XDoorBlock.FACING, facing);
        BlockPos right = left.relative(facing.getClockWise());
        for (BlockPos p : List.of(left, right)) {
            DoorHingeSide hinge = p.equals(left) ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            helper.setBlock(p, leaf.setValue(XDoorBlock.HINGE, hinge));
            helper.setBlock(p.above(), leaf.setValue(XDoorBlock.HINGE, hinge).setValue(XDoorBlock.HALF,
                    DoubleBlockHalf.UPPER));
        }
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void anXDoorOpensForAPlayerNearAndShutsBehindHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos left = new BlockPos(10, 2, 10);
        xDoor(helper, left, Direction.NORTH);
        ServerLevel level = helper.getLevel();
        List<BlockPos> cells = XDoorBlock.cells(level, helper.absolutePos(left.above()));
        check(cells.size() == 4, "a door of two leaves, two high: " + cells.size());
        ServerPlayer far = f.player(10.5, 2, 15.5);
        LabDoors.openNear(far);
        check(!open(level, cells), "nobody near: shut");
        Zombie zombie = TestKit.spawn(helper, EntityType.ZOMBIE, new BlockPos(10, 2, 11));
        zombie.setNoAi(true);
        check(!open(level, cells), "a zombie opens nothing");
        ServerPlayer near = f.player(10.5, 2, 12.0);
        LabDoors.openNear(near);
        check(open(level, cells), "a player within 2: open");
        check(level.getBlockState(cells.get(0)).getCollisionShape(level, cells.get(0)).isEmpty(),
                "an open door lets one through");
        helper.runAfterDelay(30, () -> {
            check(open(level, cells), "still open while he stands near");
            near.teleportTo(far.getX(), far.getY(), far.getZ());
        });
        helper.runAfterDelay(80, () -> {
            check(!open(level, cells), "shut after he went");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theRoundDoorOpensWhole(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockState part = ModBlocks.X_DOOR_ROUND.get().defaultBlockState().setValue(XDoorRoundBlock.FACING,
                Direction.EAST);
        for (int p = 0; p < 9; p++) {
            // facing east: the columns go south (the facing's clockwise side)
            helper.setBlock(new BlockPos(10, 2 + p / 3, 9 + p % 3), part.setValue(XDoorRoundBlock.PART, p));
        }
        List<BlockPos> cells = XDoorRoundBlock.cells(level, helper.absolutePos(new BlockPos(10, 3, 10)));
        check(cells.size() == 9, "nine parts: " + cells.size());
        ServerPlayer near = f.player(8.5, 2, 10.5);
        LabDoors.openNear(near);
        check(open(level, cells), "open, all of it");
        near.teleportTo(near.getX() - 8, near.getY(), near.getZ());
        helper.runAfterDelay(60, () -> {
            check(!open(level, cells), "shut again");
            helper.succeed();
        });
    }

    static boolean open(ServerLevel level, List<BlockPos> cells) {
        return cells.stream().allMatch(p -> level.getBlockState(p).getValue(XDoorBlock.OPEN));
    }

    static void shelves(GameTestHelper helper, BlockPos base) {
        for (int dz = 0; dz < 2; dz++) {
            for (int dy = 0; dy < 2; dy++) {
                helper.setBlock(base.offset(0, dy, dz), ModBlocks.SECRET_BOOKSHELF.get().defaultBlockState()
                        .setValue(SecretBookshelfBlock.FACING, Direction.EAST)
                        .setValue(SecretBookshelfBlock.HINGE, dz == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT));
            }
        }
    }

    static void use(ServerLevel level, ServerPlayer player, BlockPos abs) {
        level.getBlockState(abs).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP,
                abs, false));
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theShelvesSwingOpenAndCloseWhenNobodyStandsInThem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos base = new BlockPos(4, 2, 8);
        shelves(helper, base);
        ServerPlayer player = f.player(6.5, 2, 9.0);
        use(level, player, helper.absolutePos(base.above()));
        List<BlockPos> group = SecretBookshelfBlock.group(level, helper.absolutePos(base));
        check(group.size() == 4, "both columns swing: " + group.size());
        check(group.stream().allMatch(p -> level.getBlockState(p).getValue(SecretBookshelfBlock.OPEN)), "open");
        check(level.getBlockState(group.get(0)).getCollisionShape(level, group.get(0)).isEmpty(), "nothing to bump");
        // someone steps into the nook: they stay open past their time
        Zombie zombie = TestKit.spawn(helper, EntityType.ZOMBIE, base);
        zombie.setNoAi(true);
        helper.runAfterDelay(SecretEntrance.OPEN_TICKS + 30, () -> {
            check(group.stream().allMatch(p -> level.getBlockState(p).getValue(SecretBookshelfBlock.OPEN)),
                    "open while someone stands in them");
            zombie.discard();
        });
        helper.runAfterDelay(SecretEntrance.OPEN_TICKS + 80, () -> {
            check(group.stream().noneMatch(p -> level.getBlockState(p).getValue(SecretBookshelfBlock.OPEN)),
                    "shut by themselves");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRackGivesOneInjectionToEachPlayer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos rack = new BlockPos(10, 2, 10);
        helper.setBlock(rack, ModBlocks.ADAMANTIUM_RACK.get().defaultBlockState());
        BlockPos abs = helper.absolutePos(rack);
        ServerPlayer peter = f.player(10.5, 2, 12.5);
        ServerPlayer eilon = f.player(11.5, 2, 12.5, Power.MAGNETO);
        check(AdamantiumRackBlock.take(level, abs, peter), "Peter gets one");
        check(!AdamantiumRackBlock.take(level, abs, peter), "not twice");
        check(AdamantiumRackBlock.take(level, abs, eilon), "his brother gets his own");
        check(count(peter, ModItems.ADAMANTIUM_INJECTION.get()) == 1
                && count(eilon, ModItems.ADAMANTIUM_INJECTION.get()) == 1, "one each");
        use(level, peter, abs);
        check(count(peter, ModItems.ADAMANTIUM_INJECTION.get()) == 1, "a right-click gives no second");
        helper.succeed();
    }

    static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n + (player.getOffhandItem().is(item) ? player.getOffhandItem().getCount() : 0);
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theCureStationFillsABottleOncePerCooldown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos station = new BlockPos(10, 2, 10);
        helper.setBlock(station, ModBlocks.CURE_STATION.get().defaultBlockState());
        BlockPos abs = helper.absolutePos(station);
        ServerPlayer player = f.player(10.5, 2, 12.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        check(count(player, ModItems.MUTANT_CURE.get()) == 1 && count(player, Items.GLASS_BOTTLE) == 1,
                "one bottle became a Mutant Cure");
        check(!CureStationBlock.fill(level, abs, player, InteractionHand.MAIN_HAND), "not again so soon");
        check(count(player, ModItems.MUTANT_CURE.get()) == 1 && count(player, Items.GLASS_BOTTLE) == 1,
                "nothing used");
        player.getPersistentData().putLong(CureStationBlock.LAST, level.getGameTime() - 1200);
        check(CureStationBlock.fill(level, abs, player, InteractionHand.MAIN_HAND), "after the cooldown, again");
        check(count(player, ModItems.MUTANT_CURE.get()) == 2 && count(player, Items.GLASS_BOTTLE) == 0, "two cures");
        ServerPlayer empty = f.player(11.5, 2, 12.5);
        level.getBlockState(abs).useItemOn(ItemStack.EMPTY, level, empty, InteractionHand.MAIN_HAND, hit);
        check(count(empty, ModItems.MUTANT_CURE.get()) == 0, "an empty hand gets words, not a cure");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theLabsMachinesAreUnbreakableInSurvival(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(10.5, 2, 12.5);
        List<Block> blocks = List.of(ModBlocks.SECRET_BOOKSHELF.get(), ModBlocks.ELEVATOR_PAD.get(),
                ModBlocks.X_DOOR.get(), ModBlocks.X_DOOR_ROUND.get(), ModBlocks.ADAMANTIUM_RACK.get(),
                ModBlocks.WEAPON_X_POD.get(), ModBlocks.CURE_STATION.get(), ModBlocks.CEREBRO_CONSOLE.get());
        BlockPos at = new BlockPos(10, 2, 10);
        for (Block block : blocks) {
            helper.setBlock(at, block.defaultBlockState());
            BlockPos abs = helper.absolutePos(at);
            BlockState state = level.getBlockState(abs);
            check(state.getDestroySpeed(level, abs) < 0 && state.getDestroyProgress(player, level, abs) == 0.0F,
                    block + " cannot be mined in Survival");
            helper.setBlock(at, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyTheOneWhoStandsInsideThePodIsInIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pod = new BlockPos(10, 2, 10);
        BlockState lower = ModBlocks.WEAPON_X_POD.get().defaultBlockState().setValue(WeaponXPodBlock.FACING,
                Direction.WEST);
        helper.setBlock(pod, lower);
        helper.setBlock(pod.above(), lower.setValue(WeaponXPodBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos abs = helper.absolutePos(pod);
        check(level.noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(abs)
                .add(0, 1.0 / 16, 0))), "a player fits inside the pod");
        check(WeaponXPodBlock.lower(level.getBlockState(abs.above()), abs.above()).equals(abs),
                "the upper half leads to the lower");
        check(level.getBlockEntity(abs) != null && level.getBlockEntity(abs.above()) == null,
                "the block entity is on the lower half");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ the entrance

    /** A school whose study lies in the arena: its template's (0, 0, 0) at the arena's (0, -4, -10), unturned. */
    static LabSite arenaSchool(GameTestHelper helper) {
        return new LabSite(helper.absolutePos(new BlockPos(0, -4, -10)), Rotation.NONE, new BlockPos(23, 0, 22));
    }

    static void study(ServerLevel level, LabSite site) {
        for (BlockPos p : SchoolLabPlan.STUDY_SHELVES) {
            level.setBlock(site.school(p), Blocks.BOOKSHELF.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (BlockPos p : SchoolLabPlan.STUDY_PADS) {
            level.setBlock(site.school(p), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(site.school(SchoolLabPlan.LECTERN), Blocks.LECTERN.defaultBlockState(), Block.UPDATE_ALL);
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theEntranceGoesOnlyOverTheSchoolsOwnBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LabSite site = arenaSchool(helper);
        study(level, site);
        BlockPos mine = site.school(SchoolLabPlan.STUDY_SHELVES.get(1));
        level.setBlock(mine, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        check(!SecretEntrance.place(level, site, false), "a player's glass where a shelf goes: no entrance");
        for (BlockPos p : SchoolLabPlan.STUDY_PADS) {
            check(level.getBlockState(site.school(p)).is(Blocks.SPRUCE_PLANKS), "the planks are left alone");
        }
        check(level.getBlockState(mine).is(Blocks.GLASS), "and his glass");
        check(SecretEntrance.place(level, site, true), "forced, it goes");
        for (BlockPos p : SchoolLabPlan.STUDY_SHELVES) {
            BlockState s = level.getBlockState(site.school(p));
            check(s.is(ModBlocks.SECRET_BOOKSHELF.get()) && s.getValue(SecretBookshelfBlock.FACING) == Direction.EAST,
                    "a secret shelf facing into the study at " + p);
        }
        for (BlockPos p : SchoolLabPlan.STUDY_PADS) {
            check(level.getBlockState(site.school(p)).is(ModBlocks.ELEVATOR_PAD.get()), "a pad at " + p);
        }
        BlockState lectern = level.getBlockState(site.school(SchoolLabPlan.LECTERN));
        check(lectern.getValue(LecternBlock.HAS_BOOK), "the Professor's notes on the lectern");
        check(SecretEntrance.place(level, site, false), "a second time: already there, nothing to do");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = CFG)
    public static void withOpenToAllOffOnlyTheXMenOpenTheShelves(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SchoolLabConfig.OPEN_TO_ALL, false);
        ServerLevel level = helper.getLevel();
        BlockPos base = new BlockPos(4, 2, 8);
        shelves(helper, base);
        BlockPos abs = helper.absolutePos(base);
        ServerPlayer thor = f.player(6.5, 2, 9.0, Power.THOR);
        use(level, thor, abs);
        check(!level.getBlockState(abs).getValue(SecretBookshelfBlock.OPEN), "Thor is no X-Man: shut");
        ServerPlayer logan = f.player(6.5, 2, 9.5, Power.WOLVERINE);
        use(level, logan, abs);
        check(level.getBlockState(abs).getValue(SecretBookshelfBlock.OPEN), "Wolverine knows the way");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ far builds

    /** A school site far from the arenas (row {@code row}), its chunks forced until the test ends. */
    static LabSite farSchool(GameTestHelper helper, TestKit.Fixture f, int row, Rotation turn) {
        ServerLevel level = helper.getLevel();
        LabSite site = new LabSite(new BlockPos(FAR_X, FAR_Y - 6, row * 256), turn, new BlockPos(23, 0, 22));
        BoundingBox box = site.box();
        BlockPos a = site.school(BlockPos.ZERO);
        BlockPos b = site.school(new BlockPos(46, 33, 43));
        int x0 = Math.min(Math.min(a.getX(), b.getX()), box.minX()) - 2;
        int x1 = Math.max(Math.max(a.getX(), b.getX()), box.maxX()) + 2;
        int z0 = Math.min(Math.min(a.getZ(), b.getZ()), box.minZ()) - 2;
        int z1 = Math.max(Math.max(a.getZ(), b.getZ()), box.maxZ()) + 2;
        List<ChunkPos> forced = new ArrayList<>();
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                forced.add(new ChunkPos(cx, cz));
            }
        }
        f.onClose(() -> forced.forEach(c -> level.setChunkForced(c.x, c.z, false)));
        return site;
    }

    static SchoolLabs.Lab newLab(LabSite site, long key) {
        return new SchoolLabs.Lab(key, SchoolLabs.State.BUILDING, LabBuilder.CHECK, SchoolLabPlan.VERSION, "",
                site.origin(), site.turn(), site.pivot(), 0);
    }

    /** Checks a built lab: the plan's points hold their blocks, turned as the school, and no holes. */
    static void checkBuilt(ServerLevel level, LabSite site, String what) {
        Map<BlockPos, Block> want = new java.util.HashMap<>();
        SchoolLabPlan.LAB_PADS.forEach(p -> want.put(p, ModBlocks.ELEVATOR_PAD.get()));
        want.put(SchoolLabPlan.CURE_STATION, ModBlocks.CURE_STATION.get());
        want.put(SchoolLabPlan.ADAMANTIUM_RACK, ModBlocks.ADAMANTIUM_RACK.get());
        want.put(SchoolLabPlan.WEAPON_X_POD, ModBlocks.WEAPON_X_POD.get());
        want.put(SchoolLabPlan.WEAPON_X_POD.above(), ModBlocks.WEAPON_X_POD.get());
        want.put(SchoolLabPlan.CEREBRO_DOOR, ModBlocks.X_DOOR_ROUND.get());
        want.put(SchoolLabPlan.CEREBRO_CONSOLE, ModBlocks.CEREBRO_CONSOLE.get());
        want.put(new BlockPos(SchoolLabPlan.WALKWAY.minX(), SchoolLabPlan.WALKWAY.minY(), SchoolLabPlan.WALKWAY.minZ()),
                Blocks.IRON_BLOCK);
        want.put(new BlockPos(SchoolLabPlan.TANK_BOX.minX(), SchoolLabPlan.TANK_BOX.minY(),
                SchoolLabPlan.TANK_BOX.minZ()), Blocks.GLASS);
        for (Map.Entry<BlockPos, Block> e : want.entrySet()) {
            BlockState s = level.getBlockState(site.lab(e.getKey()));
            check(s.is(e.getValue()), what + ": " + e.getKey() + " holds " + e.getValue() + ", not " + s);
        }
        BlockState pad = level.getBlockState(site.lab(SchoolLabPlan.LAB_PADS.get(0)));
        check(pad.getValue(com.kolodkingal.enhanced.block.ElevatorPadBlock.FACING) == site.turn(Direction.EAST),
                what + ": the pad faces east, turned");
        check(level.getBlockState(site.lab(SchoolLabPlan.CEREBRO_CENTER)).isAir(), what + ": Cerebro's middle");
        check(level.getBlockState(site.lab(new BlockPos(4, 14, 32))).is(Blocks.WATER), what + ": the tank's water");
        List<BlockPos> holes = NoHoles.shellHoles(level, site);
        check(holes.isEmpty(), what + ": the shell is whole: " + holes.stream().limit(5).toList());
        List<BlockPos> fluids = NoHoles.strayFluids(level, site);
        check(fluids.isEmpty(), what + ": no stray fluid: " + fluids.stream().limit(5).toList());
        List<BlockPos> doors = NoHoles.unframedDoors(level, site);
        check(doors.isEmpty(), what + ": every door framed: " + doors.stream().limit(5).toList());
    }

    static void builtInTurn(GameTestHelper helper, int row, Rotation turn) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        LabSite site = farSchool(helper, f, row, turn);
        SchoolLabs.Lab lab = LabBuilder.advance(level, newLab(site, -1000 - row), Long.MAX_VALUE, Integer.MAX_VALUE,
                false);
        check(lab.state() == SchoolLabs.State.DONE, "built: " + lab);
        check(LabBuilder.ENTRANCE_BLOCKED.equals(lab.reason()), "no study in the air: the entrance is left out");
        checkBuilt(level, site, turn.name());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = GEN, timeoutTicks = 200)
    public static void aLabIsBuiltUnturned(GameTestHelper helper) {
        builtInTurn(helper, 0, Rotation.NONE);
    }

    @EnhancedTest(template = ARENA, batch = GEN, timeoutTicks = 200)
    public static void aLabIsBuiltTurnedClockwise(GameTestHelper helper) {
        builtInTurn(helper, 1, Rotation.CLOCKWISE_90);
    }

    @EnhancedTest(template = ARENA, batch = GEN, timeoutTicks = 200)
    public static void aLabIsBuiltTurnedRound(GameTestHelper helper) {
        builtInTurn(helper, 2, Rotation.CLOCKWISE_180);
    }

    @EnhancedTest(template = ARENA, batch = GEN, timeoutTicks = 200)
    public static void aLabIsBuiltTurnedCounterclockwise(GameTestHelper helper) {
        builtInTurn(helper, 3, Rotation.COUNTERCLOCKWISE_90);
    }

    /** The hash of every block of the lab's box, by lab-local position. */
    static long hash(ServerLevel level, LabSite site) {
        long h = 17;
        for (int x = 0; x < SchoolLabPlan.LAB_SIZE.getX(); x++) {
            for (int y = 0; y < SchoolLabPlan.LAB_SIZE.getY(); y++) {
                for (int z = 0; z < SchoolLabPlan.LAB_SIZE.getZ(); z++) {
                    h = h * 31 + level.getBlockState(site.lab(x, y, z)).hashCode();
                }
            }
        }
        return h;
    }

    @EnhancedTest(template = ARENA, batch = GEN, timeoutTicks = 400)
    public static void aRealSchoolGetsItsLabWithinTheBudgetAndKeepsItsCellar(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        LabSite site = farSchool(helper, f, 4, Rotation.CLOCKWISE_90);
        // a server's first lab warms the code of every block it sets (a sign's text, a chest's loot, a book): build one
        // first, so the timing is of a lab like the many after it
        LabSite warm = farSchool(helper, f, 10, Rotation.NONE);
        LabBuilder.advance(level, newLab(warm, -2001), Long.MAX_VALUE, Integer.MAX_VALUE, false);
        study(level, warm);
        SecretEntrance.place(level, warm, false);
        StructureTemplate school = level.getStructureManager().getOrCreate(Enhanced.id("mutant_school/mansion"));
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(site.turn()).setMirror(Mirror.NONE)
                .setRotationPivot(site.pivot());
        school.placeInWorld(level, site.origin(), site.origin(), settings, level.getRandom(), Block.UPDATE_CLIENTS);
        // the ground the lab is dug into (a test school stands in the air): stone, deepslate below
        BoundingBox box = site.box();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            level.setBlock(p, p.getY() < box.minY() + 6 ? Blocks.DEEPSLATE.defaultBlockState()
                    : Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        // the old cellar (school y 0..5) and the three layers above the lab, as they are before
        List<BlockState> before = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(1, -3, 1, 45, 5, 42)) {
            before.add(level.getBlockState(site.school(p)));
        }
        long[] worst = {0};
        long[] entrance = {0};
        List<String> slow = new ArrayList<>();
        int[] ticks = {0};
        SchoolLabs.Lab[] lab = {newLab(site, -2000)};
        f.everyTick(BUILD_TICKS + 40, tick -> {
            if (lab[0].state() == SchoolLabs.State.BUILDING) {
                long start = cpuNanos();
                SchoolLabs.Lab was = lab[0];
                lab[0] = LabBuilder.advance(level, lab[0], System.nanoTime() + LabBuilder.BUDGET_NANOS, 2, false);
                long took = cpuNanos() - start;
                // the first tick also reads the template and warms the code
                if (was.step() == LabBuilder.ENTRANCE) {
                    entrance[0] = took;
                } else if (tick > 1) {
                    worst[0] = Math.max(worst[0], took);
                    if (took / 1e6 > TICK_MS) {
                        slow.add("step " + was.step() + " from " + was.cursor() + " to " + lab[0].step() + " "
                                + lab[0].cursor() + ": " + took / 1e6 + " ms");
                    }
                }
                ticks[0]++;
                return false;
            }
            Enhanced.LOGGER.info("[school_lab] a lab built in {} ticks, the worst {} ms, the entrance's {} ms",
                    ticks[0], String.format(java.util.Locale.ROOT, "%.2f", worst[0] / 1e6),
                    String.format(java.util.Locale.ROOT, "%.2f", entrance[0] / 1e6));
            check(lab[0].state() == SchoolLabs.State.DONE && lab[0].reason().isEmpty(), "done, entrance in: " + lab[0]);
            check(ticks[0] <= BUILD_TICKS, "built within " + BUILD_TICKS + " ticks: " + ticks[0]);
            check(slow.size() <= NOISY_TICKS && worst[0] / 1e6 <= 2 * TICK_MS, "each tick within " + TICK_MS
                    + " ms (at most " + NOISY_TICKS + " within " + 2 * TICK_MS + "): " + slow);
            check(entrance[0] / 1e6 <= ENTRANCE_MS, "the entrance within " + ENTRANCE_MS + " ms: " + entrance[0] / 1e6);
            checkBuilt(level, site, "under a real school");
            int i = 0;
            // (3.8.1, the flake of a full run: the school's cellar has a daylight detector, whose power the world
            // changes by itself with the time of day, so a whole-state comparison failed when the run reached this
            // test by day. "Untouched" is about the builder: the same block stands there, whatever its power.)
            for (BlockPos p : BlockPos.betweenClosed(1, -3, 1, 45, 5, 42)) {
                BlockState now = level.getBlockState(site.school(p));
                check(now.getBlock() == before.get(i++).getBlock() || isEntrance(p), "the cellar and the ground "
                        + "above the lab are untouched at " + p.immutable() + ": " + now);
            }
            for (BlockPos p : SchoolLabPlan.STUDY_SHELVES) {
                check(level.getBlockState(site.school(p)).is(ModBlocks.SECRET_BOOKSHELF.get()), "a secret shelf");
            }
            for (BlockPos p : SchoolLabPlan.STUDY_PADS) {
                check(level.getBlockState(site.school(p)).is(ModBlocks.ELEVATOR_PAD.get()), "a study pad");
            }
            check(level.getBlockState(site.school(SchoolLabPlan.LECTERN)).getValue(LecternBlock.HAS_BOOK),
                    "the notes on the lectern");
            return true;
        });
    }

    /** The current thread's CPU time (ns), or the wall clock where the JVM does not measure it. */
    static long cpuNanos() {
        java.lang.management.ThreadMXBean threads = java.lang.management.ManagementFactory.getThreadMXBean();
        return threads.isCurrentThreadCpuTimeSupported() ? threads.getCurrentThreadCpuTime() : System.nanoTime();
    }

    static boolean isEntrance(BlockPos schoolPos) {
        return SchoolLabPlan.STUDY_PADS.contains(schoolPos) || SchoolLabPlan.STUDY_SHELVES.contains(schoolPos)
                || SchoolLabPlan.LECTERN.equals(schoolPos);
    }

    @EnhancedTest(template = ARENA, batch = GEN, timeoutTicks = 300)
    public static void aBuildCutShortResumesToTheSameBlocks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        LabSite whole = farSchool(helper, f, 5, Rotation.CLOCKWISE_180);
        LabSite cut = farSchool(helper, f, 6, Rotation.CLOCKWISE_180);
        LabBuilder.advance(level, newLab(whole, -3000), Long.MAX_VALUE, Integer.MAX_VALUE, false);
        // the cut one: a few layers in, then the server "stops" and starts again from what it last saved
        SchoolLabs.Lab lab = newLab(cut, -3001);
        SchoolLabs.Lab saved = lab;
        for (int i = 0; i < 6 && lab.state() == SchoolLabs.State.BUILDING; i++) {
            lab = LabBuilder.advance(level, lab, System.nanoTime(), 2, false);
            if (i == 3) {
                saved = lab;
            }
        }
        check(saved.state() == SchoolLabs.State.BUILDING && saved.step() >= LabBuilder.SHELL,
                "it was cut mid-build: " + saved);
        SchoolLabs.Lab resumed = LabBuilder.advance(level, saved, Long.MAX_VALUE, Integer.MAX_VALUE, false);
        check(resumed.state() == SchoolLabs.State.DONE, "it ends");
        check(hash(level, whole) == hash(level, cut), "with the same blocks as one built in one go");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = GEN, timeoutTicks = 200)
    public static void theShellShutsOutWaterAcrossItsWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        LabSite site = farSchool(helper, f, 7, Rotation.NONE);
        BoundingBox box = site.box();
        // a pocket of water and lava straddling the west wall, and one in the floor
        for (int dz = 0; dz < 3; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                level.setBlock(new BlockPos(box.minX() + dx, box.minY() + 13, box.minZ() + 10 + dz),
                        Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(new BlockPos(box.minX() + 20 + dz, box.minY() + dx + 1, box.minZ() + 20),
                        Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        LabBuilder.advance(level, newLab(site, -4000), Long.MAX_VALUE, Integer.MAX_VALUE, false);
        check(level.getBlockState(new BlockPos(box.minX() - 1, box.minY() + 13, box.minZ() + 10)).is(Blocks.WATER),
                "the water outside stays outside");
        check(NoHoles.whole(level, new BlockPos(box.minX(), box.minY() + 13, box.minZ() + 10)), "the wall is whole");
        check(NoHoles.strayFluids(level, site).isEmpty(), "nothing got in");
        check(NoHoles.shellHoles(level, site).isEmpty(), "and the shell is whole");
        helper.runAfterDelay(40, () -> {
            check(NoHoles.strayFluids(level, site).isEmpty(), "nothing got in later either");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------ the lift

    @EnhancedTest(template = ARENA, batch = GEN, timeoutTicks = 200)
    public static void theLiftCarriesAPlayerDownAndUpButNoMob(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        LabSite site = farSchool(helper, f, 8, Rotation.COUNTERCLOCKWISE_90);
        long key = -5000;
        SchoolLabs store = SchoolLabs.get(level);
        store.put(new SchoolLabs.Lab(key, SchoolLabs.State.DONE, LabBuilder.ENTRANCE, SchoolLabPlan.VERSION, "",
                site.origin(), site.turn(), site.pivot(), 0));
        f.onClose(() -> store.remove(key));
        BlockState pad = ModBlocks.ELEVATOR_PAD.get().defaultBlockState();
        BlockPos study = site.school(SchoolLabPlan.STUDY_PADS.get(0));
        BlockPos below = site.lab(SchoolLabPlan.LAB_PADS.get(0));
        level.setBlock(study, pad, Block.UPDATE_ALL);
        level.setBlock(below, pad, Block.UPDATE_ALL);
        ServerPlayer player = f.player(10.5, 2, 10.5);
        stand(player, study);
        Zombie zombie = new Zombie(level);
        zombie.moveTo(Vec3.atBottomCenterOf(site.school(SchoolLabPlan.STUDY_PADS.get(1)).above()));
        level.addFreshEntity(zombie);
        player.setShiftKeyDown(true);
        for (int i = 1; i < 20; i++) {
            Lift.tick(player);
        }
        check(player.blockPosition().equals(study.above()), "19 ticks of Sneak: still in the study");
        Lift.tick(player);
        check(player.blockPosition().equals(below.above()), "20: down in the lab, on its pad: "
                + player.blockPosition());
        Lift.tick(player);
        stand(player, below);
        for (int i = 0; i < 30; i++) {
            Lift.tick(player);
        }
        check(player.blockPosition().equals(below.above()), "one ride a press");
        player.setShiftKeyDown(false);
        Lift.tick(player);
        player.setShiftKeyDown(true);
        for (int i = 0; i < 20; i++) {
            Lift.tick(player);
        }
        check(player.blockPosition().equals(study.above()), "and up again");
        check(zombie.blockPosition().equals(site.school(SchoolLabPlan.STUDY_PADS.get(1)).above()),
                "the zombie on the other pad rode nowhere");
        // the study pad broken in Creative: up goes to the front door, outside
        level.setBlock(study, Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
        stand(player, below);
        player.setShiftKeyDown(false);
        Lift.tick(player);
        player.setShiftKeyDown(true);
        for (int i = 0; i < 20; i++) {
            Lift.tick(player);
        }
        check(player.blockPosition().equals(site.school(SchoolLabPlan.FRONT_DOOR_OUTSIDE)),
                "to the front door: " + player.blockPosition());
        zombie.discard();
        helper.succeed();
    }

    static void stand(ServerPlayer player, BlockPos pad) {
        player.teleportTo(pad.getX() + 0.5, pad.getY() + 1.0, pad.getZ() + 0.5);
        player.setOnGround(true);
    }

    // ------------------------------------------------------------------------------------------------ the command

    @EnhancedTest(template = ARENA, batch = GEN)
    public static void theCommandSaysWhenThereIsNoSchool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        List<Component> said = new ArrayList<>();
        CommandSource listener = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                said.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = new CommandSourceStack(listener, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)),
                net.minecraft.world.phys.Vec2.ZERO, level, 4, "lab_test", Component.literal("lab_test"), server,
                null);
        for (String args : List.of("", " 64", " force", " status")) {
            said.clear();
            server.getCommands().performPrefixedCommand(source, "enhanced structure school_lab" + args);
            check(said.size() == 1 && key(said.get(0)).equals("commands.enhanced.structure.school_lab.none"),
                    "'school_lab" + args + "' answers: no school near: " + said);
        }
        helper.succeed();
    }

    /** The translation key of a message (or of its first translated part). */
    static String key(@Nullable Component c) {
        if (c == null) {
            return "null";
        }
        if (c.getContents() instanceof TranslatableContents t) {
            return t.getKey();
        }
        for (Component sibling : c.getSiblings()) {
            String k = key(sibling);
            if (!k.startsWith("?")) {
                return k;
            }
        }
        return "?" + c;
    }

    @EnhancedTest(template = ARENA, batch = GEN)
    public static void aPlayerInsideABuiltLabIsToldOfIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        LabSite site = farSchool(helper, f, 9, Rotation.NONE);
        long key = -6000;
        SchoolLabs store = SchoolLabs.get(level);
        store.put(new SchoolLabs.Lab(key, SchoolLabs.State.DONE, LabBuilder.ENTRANCE, SchoolLabPlan.VERSION, "",
                site.origin(), site.turn(), site.pivot(), 0));
        f.onClose(() -> store.remove(key));
        check(LabEvents.insideLab(level, site.lab(SchoolLabPlan.CEREBRO_CENTER)), "Cerebro is in the lab");
        check(!LabEvents.insideLab(level, site.school(new BlockPos(23, 7, 22))), "the hall upstairs is not");
        helper.succeed();
    }

    private LabTests() {
    }
}
