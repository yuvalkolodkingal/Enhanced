package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Magneto's R at a block (3.1, D-M 3.3, 3.4): he tears out metal blocks only, up to {@code magneto.blockLiftMaxHardness},
 * aims past grass, carpets and open trapdoors at a rail or a pressure plate, takes a door out whole and a piston without
 * its head, sets a hanging lantern down standing, angers the piglins whose gold he takes, tells the truth about a fixed
 * block his Magnetic Pull would take, and asks the same rights as a hand.
 */
public final class MagnetoBlockTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static final String NO_METAL = "message.enhanced.telekinesis.no_metal";
    private static final String TOO_STRONG = "message.enhanced.telekinesis.too_strong";
    private static final String PROTECTED = "message.enhanced.telekinesis.protected";
    private static final String FIXED_METAL = "message.enhanced.telekinesis.fixed_metal";
    private static final String FIXED_METAL_FULL = "message.enhanced.telekinesis.fixed_metal_full";

    /** Presses Magneto's R (his metal telekinesis), with Sneak held or not. */
    private static void pressR(ServerPlayer player, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, 1, true);
        AbilityHandler.onKey(player, 1, false);
        player.setShiftKeyDown(false);
    }

    /** Looks at the middle of a block of the arena (relative position). */
    private static void lookAtBlock(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        lookAt(player, Vec3.atCenterOf(helper.absolutePos(pos)));
    }

    /** Looks at a point of the arena (relative position). */
    private static void lookAtPoint(GameTestHelper helper, ServerPlayer player, double x, double y, double z) {
        lookAt(player, helper.absoluteVec(new Vec3(x, y, z)));
    }

    /** The block the player holds now, or null. */
    private static TelekineticBlockEntity heldBlock(GameTestHelper helper, ServerPlayer player) {
        int held = Powers.get(player).heldEntityId();
        for (TelekineticBlockEntity block : helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get())) {
            if (block.getId() == held && Telekinesis.isHolding(player)) {
                return block;
            }
        }
        return null;
    }

    /** Lets go of what the player holds and takes the lifted block away, so nothing of it falls into the next check. */
    private static void putAway(ServerPlayer player, TelekineticBlockEntity block) {
        Telekinesis.release(player, false);
        block.discard();
    }

    /**
     * The cell of the floor's layer (relative y 2) under a floating block, as a relative position. (Worked out from {@link
     * GameTestHelper#relativeVec}: {@code relativePos} turns an unrotated test's x and z round.)
     */
    private static BlockPos cellUnder(GameTestHelper helper, TelekineticBlockEntity block) {
        Vec3 at = helper.relativeVec(block.position());
        return new BlockPos(Mth.floor(at.x), 2, Mth.floor(at.z));
    }

    /** How many items of the kind lie in the arena (or just outside its floor). */
    private static int itemsInTheArena(GameTestHelper helper, Item item) {
        AABB box = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(21, 12, 21).inflate(2.0);
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item))) {
            count += entity.getItem().getCount();
        }
        return count;
    }

    /** Every item lying in the arena. */
    private static int allItems(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM).size();
    }

    /** No lifted block is left in the arena: what was set down has landed. */
    private static boolean allLanded(GameTestHelper helper) {
        return helper.getEntities(ModEntities.TELEKINETIC_BLOCK.get()).isEmpty();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Metal blocks, and only metal

    /**
     * M11 (D-M 3.3): an iron block 6 blocks away: R holds a lifted block of it, in his grey, and its cell is air; carried
     * and set down with Sneak + R, it stands as an iron block under where it floated and nothing drops. R at a stone block:
     * {@code no_metal}, the stone unchanged. R at a netherite block (hardness 50): lifted.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks", timeoutTicks = 100)
    public static void magnetoTearsOutMetalBlocksOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        BlockPos iron = new BlockPos(9, 2, 10);
        BlockPos stone = new BlockPos(8, 2, 6);
        BlockPos netherite = new BlockPos(8, 2, 14);
        helper.setBlock(iron, Blocks.IRON_BLOCK);
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(netherite, Blocks.NETHERITE_BLOCK);

        lookAtBlock(helper, magneto, iron);
        pressR(magneto, false);
        TelekineticBlockEntity block = heldBlock(helper, magneto);
        check(block != null && block.getBlockState().is(Blocks.IRON_BLOCK), "R at an iron block: he holds it");
        check(block.getColor() == Power.MAGNETO.color, "in his iron grey");
        check(helper.getBlockState(iron).isAir(), "its cell is air");
        check(allItems(helper) == 0, "nothing dropped");

        BlockPos[] landing = {null};
        long[] letGo = {0};
        f.everyTick(90, tick -> {
            if (tick <= 15) {
                look(magneto, 0.0F, 0.0F); // he turns south: the block floats round in front of him
                PowerTicker.tick(magneto);
                if (tick == 15) {
                    landing[0] = cellUnder(helper, block);
                    pressR(magneto, true); // Sneak + R: set it down
                    check(!Telekinesis.isHolding(magneto), "set down, he holds nothing");
                    letGo[0] = helper.getLevel().getGameTime();
                }
                return false;
            }
            if (!allLanded(helper)) {
                check(helper.getLevel().getGameTime() - letGo[0] < 40, "it lands within 40 ticks");
                return false;
            }
            check(!landing[0].equals(iron), "it was carried away from its hole");
            check(helper.getBlockState(landing[0]).is(Blocks.IRON_BLOCK), "an iron block stands under where it floated");
            check(allItems(helper) == 0, "and no item lies anywhere");

            lastActionBarKey(magneto);
            lookAtBlock(helper, magneto, stone);
            pressR(magneto, false);
            check(NO_METAL.equals(lastActionBarKey(magneto)), "R at stone: no metal");
            check(!Telekinesis.isHolding(magneto) && helper.getBlockState(stone).is(Blocks.STONE), "the stone is unchanged");

            lookAtBlock(helper, magneto, netherite);
            pressR(magneto, false);
            TelekineticBlockEntity heavy = heldBlock(helper, magneto);
            check(heavy != null && heavy.getBlockState().is(Blocks.NETHERITE_BLOCK) && helper.getBlockState(netherite).isAir(),
                    "R at a netherite block (50): lifted");
            putAway(magneto, heavy);
            return true;
        });
    }

    /** M12 (D-M 3.3): with {@code magneto.blockLiftMaxHardness} 5, a netherite block is too strong; an iron block (5) lifts. */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks_hardness")
    public static void magnetosHardnessLimit(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(EnhancedConfig.MAGNETO_BLOCK_LIFT_MAX_HARDNESS, 5.0);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        BlockPos netherite = new BlockPos(8, 2, 8);
        BlockPos iron = new BlockPos(8, 2, 12);
        helper.setBlock(netherite, Blocks.NETHERITE_BLOCK);
        helper.setBlock(iron, Blocks.IRON_BLOCK);

        lastActionBarKey(magneto);
        lookAtBlock(helper, magneto, netherite);
        pressR(magneto, false);
        check(TOO_STRONG.equals(lastActionBarKey(magneto)), "a netherite block (50) is too strong at a limit of 5");
        check(!Telekinesis.isHolding(magneto) && helper.getBlockState(netherite).is(Blocks.NETHERITE_BLOCK), "it stays");

        lookAtBlock(helper, magneto, iron);
        pressR(magneto, false);
        TelekineticBlockEntity block = heldBlock(helper, magneto);
        check(block != null && block.getBlockState().is(Blocks.IRON_BLOCK) && helper.getBlockState(iron).isAir(),
                "an iron block (5) is lifted");
        putAway(magneto, block);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Aim

    /**
     * M13 (D-M 3.3, {@code Raycasts.metalBlock}): his aim passes what has no collision shape, or whose shape it passes
     * over, and stops at the outline of a metal block: a rail behind tall grass is lifted, and set down on the floor it is
     * a rail again; a heavy weighted pressure plate behind a bottom slab and a white carpet (the ray passes above them) is
     * lifted; an iron block seen past an open oak trapdoor is lifted. A stone block in front of a rail stops the ray: the
     * rail is not lifted, and he is told there is no metal.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks", timeoutTicks = 100)
    public static void magnetoAimsAtRailsAndPlatesPastGrass(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);

        // A pressure plate past a slab and a carpet.
        ServerPlayer plates = f.player(2.5, FLOOR, 10.5, Power.MAGNETO);
        BlockPos plate = new BlockPos(8, 2, 10);
        helper.setBlock(new BlockPos(4, 2, 10), Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        helper.setBlock(new BlockPos(6, 2, 10), Blocks.WHITE_CARPET);
        helper.setBlock(plate, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
        lookAtPoint(helper, plates, 8.5, 2.03, 10.5);
        pressR(plates, false);
        TelekineticBlockEntity held = heldBlock(helper, plates);
        check(held != null && held.getBlockState().is(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE) && helper.getBlockState(plate).isAir(),
                "a heavy weighted pressure plate behind a slab and a carpet is lifted");
        putAway(plates, held);

        // An iron block past an open oak trapdoor (its panel is to one side of the ray).
        ServerPlayer trapdoors = f.player(2.5, FLOOR, 16.5, Power.MAGNETO);
        BlockPos iron = new BlockPos(8, 2, 16);
        helper.setBlock(new BlockPos(5, 2, 16), Blocks.OAK_TRAPDOOR.defaultBlockState()
                .setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.OPEN, true));
        helper.setBlock(iron, Blocks.IRON_BLOCK);
        lookAtBlock(helper, trapdoors, iron);
        pressR(trapdoors, false);
        held = heldBlock(helper, trapdoors);
        check(held != null && held.getBlockState().is(Blocks.IRON_BLOCK) && helper.getBlockState(iron).isAir(),
                "an iron block seen past an open trapdoor is lifted");
        putAway(trapdoors, held);

        // A rail behind a stone block: the stone is what he looks at.
        ServerPlayer walls = f.player(11.5, FLOOR, 7.5, Power.MAGNETO);
        BlockPos hiddenRail = new BlockPos(15, 2, 7);
        helper.setBlock(new BlockPos(14, 2, 7), Blocks.STONE);
        helper.setBlock(hiddenRail, Blocks.RAIL);
        lastActionBarKey(walls);
        lookAtPoint(helper, walls, 15.5, 2.06, 7.5);
        pressR(walls, false);
        check(NO_METAL.equals(lastActionBarKey(walls)), "a stone in front of a rail: no metal");
        check(!Telekinesis.isHolding(walls) && helper.getBlockState(hiddenRail).is(Blocks.RAIL), "the rail stays");

        // A rail behind tall grass, lifted and set down on the floor.
        ServerPlayer rails = f.player(2.5, FLOOR, 4.5, Power.MAGNETO);
        BlockPos rail = new BlockPos(6, 2, 4);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.GRASS_BLOCK); // grass grows on earth, not on the stone floor
        DoublePlantBlock.placeAt(helper.getLevel(), Blocks.TALL_GRASS.defaultBlockState(), helper.absolutePos(new BlockPos(4, 2, 4)), 3);
        helper.setBlock(rail, Blocks.RAIL);
        check(helper.getBlockState(new BlockPos(4, 3, 4)).is(Blocks.TALL_GRASS), "the tall grass stands two blocks high");
        lookAtPoint(helper, rails, 6.5, 2.06, 4.5);
        pressR(rails, false);
        TelekineticBlockEntity railBlock = heldBlock(helper, rails);
        check(railBlock != null && railBlock.getBlockState().is(Blocks.RAIL) && helper.getBlockState(rail).isAir(),
                "a rail behind tall grass is lifted");

        BlockPos[] landing = {null};
        f.everyTick(90, tick -> {
            if (tick <= 10) {
                look(rails, -45.0F, 0.0F); // south-east: it floats round, away from its cell
                PowerTicker.tick(rails);
                if (tick == 10) {
                    landing[0] = cellUnder(helper, railBlock);
                    pressR(rails, true);
                    check(!Telekinesis.isHolding(rails), "set down");
                }
                return false;
            }
            if (!allLanded(helper)) {
                return false;
            }
            check(!landing[0].equals(rail), "it was carried away from its cell");
            check(helper.getBlockState(landing[0]).is(Blocks.RAIL), "set down on the floor, a rail stands");
            check(itemsInTheArena(helper, Items.RAIL) == 0, "and nothing dropped");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Doors, pistons, lanterns

    /**
     * M14 (D-M 3.3, 3.4): R at the upper half of an iron door tears the whole door out: both cells are air, nothing drops,
     * and he holds its lower half. Sneak + R over the floor: a door stands, lower and upper halves. Lifted again and thrown
     * at the barrier wall, it shatters into exactly one iron door. An extended piston (a redstone block under it) is held
     * retracted: its head's cell is air and nothing drops; set down beside a redstone block, it stands and pushes out a new
     * head within 3 ticks.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks", timeoutTicks = 200)
    public static void magnetoCarriesADoorWhole(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);

        // The piston first: everything happens in this tick.
        ServerPlayer pistons = f.player(3.5, FLOOR, 16.5, Power.MAGNETO);
        BlockPos base = new BlockPos(8, 2, 16);
        BlockPos head = base.above();
        BlockPos power = new BlockPos(12, 2, 16);
        BlockPos setDown = new BlockPos(11, 2, 16);
        helper.setBlock(base.below(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(base, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP)
                .setValue(PistonBaseBlock.EXTENDED, true));
        helper.setBlock(head, Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, Direction.UP));
        helper.setBlock(power, Blocks.REDSTONE_BLOCK);
        check(helper.getBlockState(base).getValue(PistonBaseBlock.EXTENDED) && helper.getBlockState(head).is(Blocks.PISTON_HEAD),
                "the piston stands extended");
        lookAtBlock(helper, pistons, base);
        pressR(pistons, false);
        TelekineticBlockEntity piston = heldBlock(helper, pistons);
        check(piston != null && piston.getBlockState().is(Blocks.PISTON), "R at the extended piston: he holds it");
        check(!piston.getBlockState().getValue(PistonBaseBlock.EXTENDED), "held retracted");
        check(helper.getBlockState(base).isAir() && helper.getBlockState(head).isAir(), "its cell and its head's are air");
        check(allItems(helper) == 0, "nothing dropped, not even the head");
        Vec3 over = helper.absoluteVec(new Vec3(setDown.getX() + 0.5, setDown.getY() + 0.01, setDown.getZ() + 0.5));
        piston.setPos(over.x, over.y, over.z);
        pressR(pistons, true); // set it down beside the redstone block

        // The door.
        ServerPlayer magneto = f.player(3.5, FLOOR, 6.5, Power.MAGNETO);
        BlockPos lower = new BlockPos(8, 2, 6);
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, false);
        helper.setBlock(lower, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        lookAtBlock(helper, magneto, lower.above());
        pressR(magneto, false);
        TelekineticBlockEntity held = heldBlock(helper, magneto);
        check(held != null && held.getBlockState().is(Blocks.IRON_DOOR)
                && held.getBlockState().getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER,
                "R at its upper half: he holds the door's lower half");
        check(helper.getBlockState(lower).isAir() && helper.getBlockState(lower.above()).isAir(), "both its cells are air");
        check(allItems(helper) == 0, "nothing dropped");

        int[] phase = {0};
        BlockPos[] landing = {null};
        TelekineticBlockEntity[] thrown = {null};
        long[] pistonLanded = {-1};
        boolean[] pistonDone = {false};
        f.everyTick(190, tick -> {
            if (!pistonDone[0] && !piston.isAlive()) {
                // Set down where a redstone block powers it: placed retracted, it pushes out a new head at once.
                long now = helper.getLevel().getGameTime();
                if (pistonLanded[0] < 0) {
                    pistonLanded[0] = now;
                    check(helper.getBlockState(setDown).is(Blocks.PISTON), "the piston was set down beside the redstone block");
                }
                BlockState stands = helper.getBlockState(setDown);
                pistonDone[0] = stands.is(Blocks.PISTON) && stands.getValue(PistonBaseBlock.EXTENDED)
                        && helper.getBlockState(setDown.above()).is(Blocks.PISTON_HEAD);
                check(pistonDone[0] || now - pistonLanded[0] < 3, "within 3 ticks of landing it stands extended with a new head");
            }
            PowerTicker.tick(magneto);
            switch (phase[0]) {
                case 0 -> { // carried away to the south-east, then set down over the floor
                    look(magneto, -60.0F, 0.0F);
                    if (tick == 8) {
                        landing[0] = cellUnder(helper, held);
                        pressR(magneto, true);
                        check(!Telekinesis.isHolding(magneto), "set down");
                        phase[0] = 1;
                    }
                }
                case 1 -> { // it stands as a door
                    if (held.isAlive()) {
                        return false;
                    }
                    check(!landing[0].equals(lower), "it was carried away from where it stood");
                    BlockState bottom = helper.getBlockState(landing[0]);
                    BlockState top = helper.getBlockState(landing[0].above());
                    check(bottom.is(Blocks.IRON_DOOR) && bottom.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER,
                            "a door's lower half stands");
                    check(top.is(Blocks.IRON_DOOR) && top.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
                            "and its upper half above it");
                    check(allItems(helper) == 0, "nothing dropped");
                    lookAtBlock(helper, magneto, landing[0].above());
                    pressR(magneto, false);
                    thrown[0] = heldBlock(helper, magneto);
                    check(thrown[0] != null && helper.getBlockState(landing[0]).isAir()
                                    && helper.getBlockState(landing[0].above()).isAir(),
                            "lifted again, whole");
                    phase[0] = 2;
                }
                case 2 -> { // thrown at the barrier wall
                    look(magneto, EAST, 0.0F);
                    pressR(magneto, false);
                    check(!Telekinesis.isHolding(magneto), "thrown");
                    phase[0] = 3;
                }
                default -> {
                    if (thrown[0].isAlive() || !pistonDone[0]) {
                        return false;
                    }
                    check(itemsInTheArena(helper, Items.IRON_DOOR) == 1, "it shattered into exactly one iron door, found "
                            + itemsInTheArena(helper, Items.IRON_DOOR));
                    check(allItems(helper) == 1, "and nothing else");
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * D-M 3.3 (an extra test of step 8): in a boss's protected arena an iron door is never borrowed, whichever half he
     * aims at (two cells cannot be one loan): {@code protected}, and both halves stay; an iron block beside it is.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks_arena")
    public static void aDoorIsNeverBorrowed(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer magneto = f.player(5.5, FLOOR, 10.5, Power.MAGNETO);
        BlockPos lower = new BlockPos(9, 2, 8);
        BlockPos iron = new BlockPos(9, 2, 12);
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST);
        helper.setBlock(lower, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.setBlock(iron, Blocks.IRON_BLOCK);
        BossFrameworkTests.start(f, BossKind.ULTRON, magneto, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, true));
        check(BossFights.protects(helper.getLevel(), helper.absolutePos(lower)), "the door stands in a protected arena");

        for (BlockPos half : new BlockPos[]{lower.above(), lower}) {
            lastActionBarKey(magneto);
            lookAtBlock(helper, magneto, half);
            pressR(magneto, false);
            check(PROTECTED.equals(lastActionBarKey(magneto)),
                    "R at the door's " + (half == lower ? "lower" : "upper") + " half: protected");
            check(!Telekinesis.isHolding(magneto) && helper.getBlockState(lower).is(Blocks.IRON_DOOR)
                    && helper.getBlockState(lower.above()).is(Blocks.IRON_DOOR), "both halves stay");
        }

        lookAtBlock(helper, magneto, iron);
        pressR(magneto, false);
        TelekineticBlockEntity held = heldBlock(helper, magneto);
        check(held != null && held.isBorrowed() && helper.getBlockState(iron).isAir(), "the iron block beside it is borrowed");
        Telekinesis.release(magneto, false);
        held.crumble();
        helper.succeed();
    }

    /**
     * D-M 3.4 (an extra test of step 8): a lantern hanging under a block is held as it hung; set down on the floor, where
     * it has nothing to hang from, it stands on the floor.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks", timeoutTicks = 80)
    public static void aHangingLanternStandsWhereItCannotHang(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        BlockPos lantern = new BlockPos(8, 3, 10);
        helper.setBlock(lantern.above(), Blocks.STONE);
        helper.setBlock(lantern, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        lookAtBlock(helper, magneto, lantern);
        pressR(magneto, false);
        TelekineticBlockEntity held = heldBlock(helper, magneto);
        check(held != null && held.getBlockState().is(Blocks.LANTERN) && held.getBlockState().getValue(LanternBlock.HANGING),
                "he holds the lantern as it hung");

        BlockPos[] landing = {null};
        f.everyTick(70, tick -> {
            if (tick <= 8) {
                look(magneto, 0.0F, 0.0F); // south, away from the stone it hung from
                PowerTicker.tick(magneto);
                if (tick == 8) {
                    landing[0] = cellUnder(helper, held);
                    pressR(magneto, true);
                }
                return false;
            }
            if (!allLanded(helper)) {
                return false;
            }
            check(landing[0].getX() != lantern.getX() || landing[0].getZ() != lantern.getZ(), "it was carried away");
            BlockState stands = helper.getBlockState(landing[0]);
            check(stands.is(Blocks.LANTERN) && !stands.getValue(LanternBlock.HANGING), "set down on the floor, it stands");
            check(itemsInTheArena(helper, Items.LANTERN) == 0, "and nothing dropped");
            return true;
        });
    }

    /**
     * D-M 3.3 (an extra test of step 8): a gold block torn out angers the piglins near him, as breaking their gold by hand
     * does; an iron block does not.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks")
    public static void piglinsMindTheGoldHeTearsOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        Piglin piglin = TestKit.spawnWithNoFreeWill(helper, EntityType.PIGLIN, new Vec3(12.5, FLOOR, 16.5));
        BlockPos iron = new BlockPos(8, 2, 8);
        BlockPos gold = new BlockPos(8, 2, 12);
        helper.setBlock(iron, Blocks.IRON_BLOCK);
        helper.setBlock(gold, Blocks.GOLD_BLOCK);

        lookAtBlock(helper, magneto, iron);
        pressR(magneto, false);
        TelekineticBlockEntity held = heldBlock(helper, magneto);
        check(held != null, "an iron block is lifted");
        check(piglin.getBrain().getMemory(MemoryModuleType.ANGRY_AT).isEmpty(), "iron is no piglin's: it is not angry");
        putAway(magneto, held);

        lookAtBlock(helper, magneto, gold);
        pressR(magneto, false);
        held = heldBlock(helper, magneto);
        check(held != null && held.getBlockState().is(Blocks.GOLD_BLOCK), "a gold block is lifted");
        check(piglin.getBrain().getMemory(MemoryModuleType.ANGRY_AT).filter(magneto.getUUID()::equals).isPresent(),
                "the piglin is angry at him");
        putAway(magneto, held);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Fixed blocks, and the rights

    /**
     * M15a (D-M 3.3, R34, R36): a block that holds things is never lifted: a hopper with 3 cobblestone says {@code
     * fixed_metal_full} and still holds 3; an empty hopper and an unclaimed bell (the pull would take them) say {@code
     * fixed_metal}. (A villager's bell is integration test X3.)
     */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks")
    public static void blocksThatHoldThingsStayForR(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        BlockPos full = new BlockPos(8, 2, 6);
        BlockPos empty = new BlockPos(8, 2, 10);
        BlockPos bell = new BlockPos(8, 2, 14);
        helper.setBlock(full, Blocks.HOPPER);
        helper.setBlock(empty, Blocks.HOPPER);
        helper.setBlock(bell, Blocks.BELL);
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getBlockEntity(full);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 3));

        lastActionBarKey(magneto);
        lookAtBlock(helper, magneto, full);
        pressR(magneto, false);
        check(FIXED_METAL_FULL.equals(lastActionBarKey(magneto)), "a hopper with cobblestone in it: it holds things");
        check(!Telekinesis.isHolding(magneto) && helper.getBlockState(full).is(Blocks.HOPPER), "the hopper stays");
        check(hopper.getItem(0).is(Items.COBBLESTONE) && hopper.getItem(0).getCount() == 3, "and still holds 3 cobblestone");

        lookAtBlock(helper, magneto, empty);
        pressR(magneto, false);
        check(FIXED_METAL.equals(lastActionBarKey(magneto)), "an empty hopper: fixed in place, the pull would take it");
        check(!Telekinesis.isHolding(magneto) && helper.getBlockState(empty).is(Blocks.HOPPER), "it stays");

        lookAtBlock(helper, magneto, bell);
        pressR(magneto, false);
        check(FIXED_METAL.equals(lastActionBarKey(magneto)), "an unclaimed bell: the same");
        check(!Telekinesis.isHolding(magneto) && helper.getBlockState(bell).is(Blocks.BELL), "it stays");
        check(allItems(helper) == 0, "nothing spilled");
        helper.succeed();
    }

    /**
     * M16 (D-M 3.3, 7): R asks the rights a hand would need. A claim (a NORMAL-priority listener that cancels his break
     * events): {@code protected}, the iron block stays; without it, lifted. An Adventure Magneto: {@code protected}.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_blocks_rights")
    public static void magnetosLiftAsksTheRights(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer magneto = f.player(3.5, FLOOR, 6.5, Power.MAGNETO);
        BlockPos iron = new BlockPos(8, 2, 6);
        BlockPos adventureIron = new BlockPos(8, 2, 14);
        helper.setBlock(iron, Blocks.IRON_BLOCK);
        helper.setBlock(adventureIron, Blocks.IRON_BLOCK);
        int[] asked = {0};
        Consumer<BlockEvent.BreakEvent> claim = event -> {
            if (event.getPlayer() == magneto) {
                asked[0]++;
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, claim);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(claim));

        lastActionBarKey(magneto);
        lookAtBlock(helper, magneto, iron);
        pressR(magneto, false);
        check(asked[0] == 1, "the claim was asked");
        check(PROTECTED.equals(lastActionBarKey(magneto)), "a claim: this place is protected");
        check(!Telekinesis.isHolding(magneto) && helper.getBlockState(iron).is(Blocks.IRON_BLOCK), "the iron block stays");

        NeoForge.EVENT_BUS.unregister(claim);
        pressR(magneto, false);
        TelekineticBlockEntity held = heldBlock(helper, magneto);
        check(held != null && helper.getBlockState(iron).isAir(), "without the claim, lifted");
        putAway(magneto, held);

        ServerPlayer adventurer = f.player(3.5, FLOOR, 14.5, GameType.ADVENTURE);
        Powers.setPower(adventurer, Power.MAGNETO);
        lastActionBarKey(adventurer);
        lookAtBlock(helper, adventurer, adventureIron);
        pressR(adventurer, false);
        check(PROTECTED.equals(lastActionBarKey(adventurer)), "in Adventure: this place is protected");
        check(!Telekinesis.isHolding(adventurer) && helper.getBlockState(adventureIron).is(Blocks.IRON_BLOCK),
                "the iron block stays");
        helper.succeed();
    }

    private MagnetoBlockTests() {
    }
}
