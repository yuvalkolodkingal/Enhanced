package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.MagnetPull;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.network.MagnetPullPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Magneto's Magnetic Pull (3.1, D-M 4; spec 18.3), Sneak + R with nothing held: what the ores, the metal blocks and the
 * containers give, what stays (out of sight, out of the cone he faces, holding things, a working piston, a villager's
 * work, someone's claim, an arena), how the search and the takes are sliced and bounded, what it costs, and when nothing
 * happens at all. Batches {@code magnet_pull*}.
 *
 * <p>Magneto stands in the middle of the floor so a pull of radius 8 stays inside the arena. Built metal stands on stone
 * pillars, one block above the floor, where his eyes see each piece over everything lower; ores and raw-metal blocks are
 * set into the floor, where only magnetism reaches them.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class MagnetPullTests {
    private static final String PULL = "magnet_pull";

    /**
     * Eleven places east of the middle (x, z), each seen from Magneto's eyes past all the others when what stands there is
     * one block above the floor, and all inside the 120 degree cone he faces looking east.
     */
    private static final int[][] FAN = {{13, 6}, {15, 6}, {14, 8}, {17, 8}, {16, 9}, {14, 10}, {16, 11}, {17, 12}, {14, 12}, {15, 14}, {14, 15}};

    // ------------------------------------------------------------------------------------------------------------
    // What each thing gives (M17, M18, M19, M20, M25)

    /**
     * M17. The metal comes out of the ores, through the floor: each turns into its ground and gives what mining gives
     * without Fortune (gilded blackstone its nuggets, ancient debris its scrap); nothing drops and no experience appears;
     * the pull rests, costs one hunger point and was heard.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void thePullTakesTheMetalOutOfTheOres(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = magneto(f);
        List<BlockPos> cells = List.of(new BlockPos(5, 1, 10), new BlockPos(15, 1, 10), new BlockPos(10, 1, 5), new BlockPos(10, 1, 15),
                new BlockPos(6, 1, 6), new BlockPos(14, 1, 14), new BlockPos(14, 1, 6));
        List<Block> ores = List.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.COPPER_ORE, Blocks.NETHER_GOLD_ORE,
                Blocks.GILDED_BLACKSTONE, Blocks.ANCIENT_DEBRIS);
        List<Block> grounds = List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.STONE, Blocks.STONE, Blocks.NETHERRACK, Blocks.BLACKSTONE,
                Blocks.NETHERRACK);
        for (int i = 0; i < cells.size(); i++) {
            helper.setBlock(cells.get(i), ores.get(i));
        }
        double hunger = hunger(magneto);
        TestKit.sentPackets(magneto);

        sneakR(magneto);
        check(TestKit.heardSounds(magneto).stream().anyMatch(sound -> sound.id().equals(Enhanced.id("magnet_pull"))), "he hears the pull begin");
        afterThePull(f, magneto, 20, () -> {
            for (int i = 0; i < cells.size(); i++) {
                check(helper.getBlockState(cells.get(i)).is(grounds.get(i)), BuiltInRegistries.BLOCK.getKey(ores.get(i)) + " became "
                        + BuiltInRegistries.BLOCK.getKey(grounds.get(i)) + ", not " + helper.getBlockState(cells.get(i)));
            }
            check(count(magneto, Items.RAW_IRON) == 2, "two raw iron, not " + count(magneto, Items.RAW_IRON));
            check(count(magneto, Items.RAW_GOLD) == 1, "one raw gold, not " + count(magneto, Items.RAW_GOLD));
            check(between(count(magneto, Items.RAW_COPPER), 2, 5), "2 to 5 raw copper, not " + count(magneto, Items.RAW_COPPER));
            check(between(count(magneto, Items.GOLD_NUGGET), 4, 11), "4 to 11 gold nuggets, not " + count(magneto, Items.GOLD_NUGGET));
            check(count(magneto, Items.NETHERITE_SCRAP) == 1, "one netherite scrap, not " + count(magneto, Items.NETHERITE_SCRAP));
            check(helper.getEntities(EntityType.ITEM).isEmpty() && helper.getEntities(EntityType.EXPERIENCE_ORB).isEmpty(),
                    "nothing drops and no experience appears: it all went into his inventory");
            check(!Powers.get(magneto).isReady(1, helper.getLevel().getGameTime()), "the pull rests");
            assertNear(hunger - hunger(magneto), EnhancedConfig.PULL_EXHAUSTION.get(), 0.001, "the hunger the pull cost");
        });
    }

    /**
     * M18. What an ore gives is mining's yield without Fortune, rolled 200 times: one raw iron, 2 to 5 raw copper and never
     * the ore, 2 to 6 nuggets from nether gold ore, always 2 to 5 nuggets from gilded blackstone and never the block, always
     * one scrap from ancient debris. A block whose only loot is itself (a mod's ore, as a raw iron block stands in) gives
     * nothing and is not taken. Each ore's ground, and the ground by height and dimension for a block in no ground tag.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void oreYieldsAreMiningsWithoutFortune(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(10, 1, 10));
        for (int roll = 0; roll < 200; roll++) {
            check(only(MagnetPull.oreYield(level, pos, Blocks.IRON_ORE.defaultBlockState()), Items.RAW_IRON, 1, 1), "iron ore: one raw iron");
            check(only(MagnetPull.oreYield(level, pos, Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState()), Items.RAW_GOLD, 1, 1), "deepslate gold ore: one raw gold");
            check(only(MagnetPull.oreYield(level, pos, Blocks.COPPER_ORE.defaultBlockState()), Items.RAW_COPPER, 2, 5), "copper ore: 2 to 5 raw copper");
            check(only(MagnetPull.oreYield(level, pos, Blocks.NETHER_GOLD_ORE.defaultBlockState()), Items.GOLD_NUGGET, 2, 6), "nether gold ore: 2 to 6 nuggets");
            check(only(MagnetPull.oreYield(level, pos, Blocks.GILDED_BLACKSTONE.defaultBlockState()), Items.GOLD_NUGGET, 2, 5),
                    "gilded blackstone: 2 to 5 nuggets, every time, never the block");
            check(only(MagnetPull.oreYield(level, pos, Blocks.ANCIENT_DEBRIS.defaultBlockState()), Items.NETHERITE_SCRAP, 1, 1), "ancient debris: one scrap");
        }
        BlockState itself = Blocks.RAW_IRON_BLOCK.defaultBlockState();
        check(MagnetPull.oreYield(level, pos, itself).isEmpty(), "a block whose only loot is itself gives no metal");
        check(!MagnetPull.wouldTakeOre(level, pos, itself), "so such an ore is left as it is");
        for (Block ore : List.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.NETHER_GOLD_ORE, Blocks.GILDED_BLACKSTONE, Blocks.ANCIENT_DEBRIS)) {
            check(MagnetPull.wouldTakeOre(level, pos, ore.defaultBlockState()), BuiltInRegistries.BLOCK.getKey(ore) + " is taken");
        }

        List<Block> ores = List.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.COPPER_ORE,
                Blocks.DEEPSLATE_COPPER_ORE, Blocks.NETHER_GOLD_ORE, Blocks.GILDED_BLACKSTONE, Blocks.ANCIENT_DEBRIS);
        List<Block> grounds = List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.STONE, Blocks.DEEPSLATE, Blocks.STONE, Blocks.DEEPSLATE,
                Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.NETHERRACK);
        for (int i = 0; i < ores.size(); i++) {
            check(MagnetPull.groundFor(level, pos, ores.get(i).defaultBlockState()).is(grounds.get(i)),
                    BuiltInRegistries.BLOCK.getKey(ores.get(i)) + " becomes " + BuiltInRegistries.BLOCK.getKey(grounds.get(i)));
        }
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        ServerLevel end = level.getServer().getLevel(Level.END);
        check(MagnetPull.groundFor(level, new BlockPos(0, 10, 0), itself).is(Blocks.STONE), "in no ground tag: stone above y 0");
        check(MagnetPull.groundFor(level, new BlockPos(0, -10, 0), itself).is(Blocks.DEEPSLATE), "deepslate below it");
        check(nether != null && MagnetPull.groundFor(nether, new BlockPos(0, 40, 0), itself).is(Blocks.NETHERRACK), "netherrack in the Nether");
        check(end != null && MagnetPull.groundFor(end, new BlockPos(0, 40, 0), itself).is(Blocks.END_STONE), "end stone in the End");
        helper.succeed();
    }

    /**
     * M19. Built metal in the cone he faces and in sight comes whole into his inventory, as Silk Touch mining gives it: an
     * iron block, a chipped anvil, a water cauldron (as a cauldron), a rail, a hanging lantern, a chain, iron bars, a double
     * slab (two slabs), a lightning rod, and exactly one of each door; every cell is air, both halves of each door, and no
     * item lies anywhere.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void thePullTakesMetalBlocksWhole(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = magneto(f);
        onPillar(helper, FAN[0], Blocks.IRON_BLOCK.defaultBlockState());
        door(helper, FAN[1], Blocks.IRON_DOOR);
        onPillar(helper, FAN[2], Blocks.CHIPPED_ANVIL.defaultBlockState());
        onPillar(helper, FAN[3], Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        onPillar(helper, FAN[4], Blocks.RAIL.defaultBlockState());
        helper.setBlock(new BlockPos(FAN[5][0], 4, FAN[5][1]), Blocks.STONE); // what the lantern hangs from
        helper.setBlock(new BlockPos(FAN[5][0], 3, FAN[5][1]), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        onPillar(helper, FAN[6], Blocks.CHAIN.defaultBlockState());
        onPillar(helper, FAN[7], Blocks.IRON_BARS.defaultBlockState());
        onPillar(helper, FAN[8], Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
        onPillar(helper, FAN[9], Blocks.LIGHTNING_ROD.defaultBlockState());
        door(helper, FAN[10], Blocks.WAXED_COPPER_DOOR);
        for (int[] at : FAN) {
            check(seen(helper, magneto, at[0], 3, at[1]), "the block at " + at[0] + ", " + at[1] + " is in sight");
        }

        sneakR(magneto);
        afterThePull(f, magneto, 20, () -> {
            for (Item item : List.of(Items.IRON_BLOCK, Items.CHIPPED_ANVIL, Items.CAULDRON, Items.RAIL, Items.LANTERN, Items.CHAIN,
                    Items.IRON_BARS, Items.LIGHTNING_ROD, Items.IRON_DOOR, Items.WAXED_COPPER_DOOR)) {
                check(count(magneto, item) == 1, "exactly one " + BuiltInRegistries.ITEM.getKey(item) + ", not " + count(magneto, item));
            }
            check(count(magneto, Items.WAXED_OXIDIZED_CUT_COPPER_SLAB) == 2, "a double slab gives two slabs");
            for (int[] at : FAN) {
                check(helper.getBlockState(new BlockPos(at[0], 3, at[1])).isAir(), "nothing is left at " + at[0] + ", " + at[1]);
            }
            check(helper.getBlockState(new BlockPos(FAN[1][0], 4, FAN[1][1])).isAir()
                    && helper.getBlockState(new BlockPos(FAN[10][0], 4, FAN[10][1])).isAir(), "nor of the doors' upper halves");
            check(helper.getEntities(EntityType.ITEM).isEmpty(), "and no item lies anywhere (a door taken by its upper half would drop one)");
        });
    }

    /**
     * M20 (R36). A block that holds things stays whole with what it holds: a hopper with cobblestone, a lit blast furnace
     * with raw iron; empty, they come whole and spill nothing. A chest minecart with an apple stays, an empty one comes as
     * its item. An extended piston and a powered one stay; a plain one in view comes.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void aHopperComesOnlyWhenEmpty(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        onPillar(helper, FAN[0], Blocks.PISTON.defaultBlockState());
        onPillar(helper, FAN[2], Blocks.HOPPER.defaultBlockState());
        onPillar(helper, FAN[4], Blocks.BLAST_FURNACE.defaultBlockState());
        onPillar(helper, FAN[8], Blocks.HOPPER.defaultBlockState());
        container(helper, FAN[8]).setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        onPillar(helper, FAN[9], Blocks.BLAST_FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.LIT, true));
        container(helper, FAN[9]).setItem(0, new ItemStack(Items.RAW_IRON, 2));
        // A piston pushed out by a redstone block under it, and one powered under an obsidian block it cannot push.
        helper.setBlock(new BlockPos(FAN[3][0], 2, FAN[3][1]), Blocks.REDSTONE_BLOCK);
        helper.setBlock(new BlockPos(FAN[3][0], 3, FAN[3][1]), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(FAN[7][0], 4, FAN[7][1]), Blocks.OBSIDIAN);
        helper.setBlock(new BlockPos(FAN[7][0], 2, FAN[7][1]), Blocks.REDSTONE_BLOCK);
        helper.setBlock(new BlockPos(FAN[7][0], 3, FAN[7][1]), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP));
        MinecartChest full = cart(helper, EntityType.CHEST_MINECART, new Vec3(12.5, FLOOR, 16.5));
        full.setItem(0, new ItemStack(Items.APPLE));
        MinecartChest empty = cart(helper, EntityType.CHEST_MINECART, new Vec3(7.5, FLOOR, 13.5));
        BlockPos extended = new BlockPos(FAN[3][0], 3, FAN[3][1]);
        BlockPos powered = new BlockPos(FAN[7][0], 3, FAN[7][1]);

        f.everyTick(40, tick -> {
            if (tick < 5) {
                return false; // the piston pushes its head out
            }
            if (tick == 5) {
                check(helper.getBlockState(extended).getValue(PistonBaseBlock.EXTENDED), "the first piston stands extended");
                check(!helper.getBlockState(powered).getValue(PistonBaseBlock.EXTENDED)
                        && level.hasNeighborSignal(helper.absolutePos(powered)), "the second is powered and cannot push");
                for (int[] at : List.of(FAN[0], FAN[2], FAN[3], FAN[4], FAN[7], FAN[8], FAN[9])) {
                    check(seen(helper, magneto, at[0], 3, at[1]), "the block at " + at[0] + ", " + at[1] + " is in sight");
                }
                sneakR(magneto);
                return false;
            }
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(helper.getBlockState(new BlockPos(FAN[8][0], 3, FAN[8][1])).is(Blocks.HOPPER)
                    && container(helper, FAN[8]).getItem(0).is(Items.COBBLESTONE) && container(helper, FAN[8]).getItem(0).getCount() == 3,
                    "the hopper with cobblestone stays, and keeps it");
            check(helper.getBlockState(new BlockPos(FAN[9][0], 3, FAN[9][1])).is(Blocks.BLAST_FURNACE)
                    && container(helper, FAN[9]).getItem(0).is(Items.RAW_IRON), "the blast furnace with ore in it stays, and keeps it");
            check(helper.getBlockState(new BlockPos(FAN[2][0], 3, FAN[2][1])).isAir() && count(magneto, Items.HOPPER) == 1, "the empty hopper is his");
            check(helper.getBlockState(new BlockPos(FAN[4][0], 3, FAN[4][1])).isAir() && count(magneto, Items.BLAST_FURNACE) == 1,
                    "the empty blast furnace is his");
            check(helper.getEntities(EntityType.ITEM).isEmpty(), "nothing spilled");
            check(full.isAlive() && full.getItem(0).is(Items.APPLE), "the chest minecart with an apple stays, apple and all");
            check(!empty.isAlive() && count(magneto, Items.CHEST_MINECART) == 1, "the empty one is his, as its item");
            check(helper.getBlockState(extended).is(Blocks.PISTON) && helper.getBlockState(powered).is(Blocks.PISTON),
                    "the extended piston and the powered one stay");
            check(helper.getBlockState(new BlockPos(FAN[0][0], 3, FAN[0][1])).isAir() && count(magneto, Items.PISTON) == 1,
                    "the plain piston is his");
            return true;
        });
    }

    /** M25. The block under his feet never comes: he does not pull his own floor away. Another iron block does. */
    @GameTest(template = ARENA, batch = PULL)
    public static void thePullSparesHisFloor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = magneto(f);
        BlockPos floor = new BlockPos(10, 1, 10);
        helper.setBlock(floor, Blocks.IRON_BLOCK);
        onPillar(helper, new int[]{13, 10}, Blocks.IRON_BLOCK.defaultBlockState());
        sneakR(magneto);
        afterThePull(f, magneto, 20, () -> {
            check(helper.getBlockState(floor).is(Blocks.IRON_BLOCK), "the iron block he stands on stays");
            check(helper.getBlockState(new BlockPos(13, 3, 10)).isAir() && count(magneto, Items.IRON_BLOCK) == 1, "the other one is his");
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Cost, recharge, and when nothing happens (M26, M27, M28, M49, M56)

    /**
     * M26. Hungry: an empty hunger bar says so and nothing comes. In Creative a pull costs no hunger. A pull rests: at once
     * Sneak + R says it is recharging and nothing comes; 100 ticks after it began resting, the next pull comes.
     */
    @GameTest(template = ARENA, batch = PULL, timeoutTicks = 200)
    public static void thePullHasACostAndARecharge(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos first = new BlockPos(6, 1, 10);
        BlockPos second = new BlockPos(14, 1, 10);
        helper.setBlock(first, Blocks.IRON_ORE);

        ServerPlayer hungry = magneto(f);
        hungry.getFoodData().setFoodLevel(0);
        hungry.getFoodData().setSaturation(0.0F);
        BossTestSupport.lastActionBarKey(hungry);
        sneakR(hungry);
        check("message.enhanced.pull.too_hungry".equals(BossTestSupport.lastActionBarKey(hungry)), "with an empty hunger bar: too hungry");
        check(!MagnetPull.isPulling(hungry) && helper.getBlockState(first).is(Blocks.IRON_ORE), "and nothing comes");
        check(Powers.get(hungry).isReady(1, level.getGameTime()), "nor does it rest");

        ServerPlayer creative = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        Powers.setPower(creative, Power.MAGNETO);
        FoodData food = creative.getFoodData();
        double creativeHunger = hunger(creative);
        sneakR(creative);
        check(MagnetPull.isPulling(creative), "a Creative Magneto pulls");

        ServerPlayer magneto = magneto(f);
        PowerData data = Powers.get(magneto);
        long[] readyAt = {Long.MAX_VALUE};
        f.everyTick(180, tick -> {
            PowerTicker.tick(creative);
            PowerTicker.tick(magneto);
            if (tick == 3) {
                check(!MagnetPull.isPulling(creative) && helper.getBlockState(first).is(Blocks.STONE) && count(creative, Items.RAW_IRON) == 1,
                        "the Creative Magneto's pull took the ore");
                assertNear(hunger(creative), creativeHunger, 1.0E-6, "and cost him no hunger (food " + food.getFoodLevel() + ")");
                helper.setBlock(first, Blocks.IRON_ORE);
                sneakR(magneto);
            } else if (tick == 6) {
                check(!MagnetPull.isPulling(magneto) && count(magneto, Items.RAW_IRON) == 1, "his pull took the ore");
                check(!data.isReady(1, level.getGameTime()), "and rests");
                readyAt[0] = data.cooldownUntil(1);
                helper.setBlock(second, Blocks.IRON_ORE);
                BossTestSupport.lastActionBarKey(magneto);
                sneakR(magneto);
                check("message.enhanced.pull.recharging".equals(BossTestSupport.lastActionBarKey(magneto)), "Sneak + R at once: recharging");
                check(!MagnetPull.isPulling(magneto) && helper.getBlockState(second).is(Blocks.IRON_ORE), "and the second ore stays");
            } else if (tick > 6 && level.getGameTime() == readyAt[0] - 1) {
                check(!data.isReady(1, level.getGameTime()), "a tick before, he still rests");
            } else if (tick > 6 && level.getGameTime() == readyAt[0]) {
                check(data.isReady(1, level.getGameTime()), "100 ticks after it began resting he is ready");
                sneakR(magneto);
                check(MagnetPull.isPulling(magneto), "and pulls again");
            } else if (tick > 6 && level.getGameTime() == readyAt[0] + 3) {
                check(helper.getBlockState(second).is(Blocks.STONE) && count(magneto, Items.RAW_IRON) == 2, "the second ore comes");
                return true;
            }
            return false;
        });
    }

    /**
     * M27. A pull that finds no metal says so and costs nothing: no rest, no hunger. Sneak + R again 5 ticks later, and 9,
     * does nothing at all (no line, no new search); {@code magneto.pullRetryTicks} (10) ticks after the first it searches
     * again.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void nothingToPullCostsNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        double hunger = hunger(magneto);
        BossTestSupport.lastActionBarKey(magneto);
        sneakR(magneto);
        check("message.enhanced.pull.nothing".equals(BossTestSupport.lastActionBarKey(magneto)), "no metal around him");
        check(!MagnetPull.isPulling(magneto) && Powers.get(magneto).isReady(1, level.getGameTime()), "the pull is over and does not rest");
        assertNear(hunger(magneto), hunger, 1.0E-6, "it cost no hunger");
        long searched = MagnetPull.searchEndedAt(magneto);
        check(searched == level.getGameTime(), "it searched at the press");

        int retry = EnhancedConfig.PULL_RETRY_TICKS.get();
        f.everyTick(retry + 1, tick -> {
            if (tick == 5 || tick == retry - 1) {
                sneakR(magneto);
                check(level.getGameTime() - searched == tick, "pressed " + tick + " ticks after the first");
                check(actionBar(TestKit.sentPackets(magneto)).isEmpty(), tick + " ticks later Sneak + R says nothing");
                check(MagnetPull.searchEndedAt(magneto) == searched && !MagnetPull.isPulling(magneto), "and does not search");
            } else if (tick == retry) {
                sneakR(magneto);
                check("message.enhanced.pull.nothing".equals(BossTestSupport.lastActionBarKey(magneto)), "after " + retry + " ticks it searches again");
                check(MagnetPull.searchEndedAt(magneto) == level.getGameTime(), "a new search");
                return true;
            }
            return false;
        });
    }

    /** M28. Holding something, Sneak + R still sets it down gently: no pull, no rest. */
    @GameTest(template = ARENA, batch = PULL)
    public static void sneakRWhileHoldingStillSetsDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = magneto(f);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(14.5, FLOOR, 10.5));
        TestKit.lookAt(magneto, golem);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto), "R holds the golem");
        sneakR(magneto);
        check(!Telekinesis.isHolding(magneto) && !Telekinesis.isHeld(golem), "Sneak + R sets it down");
        check(!MagnetPull.isPulling(magneto) && Powers.get(magneto).isReady(1, helper.getLevel().getGameTime()), "and pulls nothing");
        helper.succeed();
    }

    /**
     * M49. Right after a grip of his ended (the golem he held died), Sneak + R does nothing at all: no pull, no rest, no
     * line, the ore unchanged; 20 ticks after the grip ended it pulls. Riding a minecart, Sneak + R does nothing (Sneak is
     * how he gets off); off it, he pulls.
     */
    @GameTest(template = ARENA, batch = PULL, timeoutTicks = 100)
    public static void noPullRightAfterAGrip(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        PowerData data = Powers.get(magneto);
        BlockPos ore = new BlockPos(6, 1, 10);
        helper.setBlock(ore, Blocks.IRON_ORE);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(14.5, FLOOR, 10.5));
        TestKit.lookAt(magneto, golem);
        AbilityHandler.onKey(magneto, 1, true);
        check(Telekinesis.isHolding(magneto), "R holds the golem");
        long[] gripEnded = {0};

        f.everyTick(80, tick -> {
            if (tick == 1) {
                golem.kill();
                return false;
            }
            PowerTicker.tick(magneto);
            if (tick == 2) {
                check(!Telekinesis.isHolding(magneto), "the grip is lost when the golem dies");
                gripEnded[0] = level.getGameTime();
                TestKit.sentPackets(magneto);
                sneakR(magneto);
                check(!MagnetPull.isPulling(magneto) && data.isReady(1, level.getGameTime()), "Sneak + R right after: no pull, no rest");
                check(helper.getBlockState(ore).is(Blocks.IRON_ORE) && actionBar(TestKit.sentPackets(magneto)).isEmpty(),
                        "the ore unchanged, and not a word");
            } else if (level.getGameTime() == gripEnded[0] + EnhancedConfig.TELEKINESIS_SNEAK_GRACE_TICKS.get()) {
                sneakR(magneto);
                check(MagnetPull.isPulling(magneto), "a second after the grip ended he pulls");
            } else if (level.getGameTime() == gripEnded[0] + EnhancedConfig.TELEKINESIS_SNEAK_GRACE_TICKS.get() + 4) {
                check(helper.getBlockState(ore).is(Blocks.STONE) && count(magneto, Items.RAW_IRON) == 1, "the ore gave its iron");
                data.startCooldown(1, level.getGameTime(), 0);
                helper.setBlock(ore, Blocks.IRON_ORE);
                Minecart cart = cart(helper, EntityType.MINECART, new Vec3(10.5, FLOOR, 7.5));
                magneto.startRiding(cart, true);
                check(magneto.isPassenger(), "he rides the minecart");
                sneakR(magneto);
                check(!MagnetPull.isPulling(magneto) && helper.getBlockState(ore).is(Blocks.IRON_ORE), "riding, Sneak + R does nothing");
                magneto.stopRiding();
                sneakR(magneto);
                check(MagnetPull.isPulling(magneto), "off it, he pulls");
            } else if (level.getGameTime() == gripEnded[0] + EnhancedConfig.TELEKINESIS_SNEAK_GRACE_TICKS.get() + 10) {
                check(helper.getBlockState(ore).is(Blocks.STONE) && count(magneto, Items.RAW_IRON) == 2, "and the ore gave its iron");
                return true;
            }
            return false;
        });
    }

    /**
     * M56 (R54). A Spectator's press, however it reaches the server, does nothing: no pull, no line, the ore as it was. A
     * pull whose Magneto becomes a Spectator, or dies, ends that tick, and nothing more comes.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void aSpectatorNeverPulls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        List<BlockPos> ores = List.of(new BlockPos(6, 1, 10), new BlockPos(14, 1, 10), new BlockPos(10, 1, 6), new BlockPos(10, 1, 14));
        for (BlockPos ore : ores) {
            helper.setBlock(ore, Blocks.IRON_ORE);
        }
        ServerPlayer spectator = f.player(10.5, FLOOR, 10.5, GameType.SPECTATOR);
        Powers.setPower(spectator, Power.MAGNETO);
        TestKit.sentPackets(spectator);
        spectator.setShiftKeyDown(true);
        Telekinesis.press(spectator, Telekinesis.Mode.MAGNETO);
        spectator.setShiftKeyDown(false);
        check(!MagnetPull.isPulling(spectator) && actionBar(TestKit.sentPackets(spectator)).isEmpty(), "a Spectator's press: no pull, not a word");

        ServerPlayer watching = magneto(f);
        sneakR(watching);
        check(MagnetPull.isPulling(watching), "a pull begins");
        watching.setGameMode(GameType.SPECTATOR);
        ServerPlayer dying = magneto(f);
        f.everyTick(10, tick -> {
            PowerTicker.tick(watching);
            if (tick == 1) {
                check(!MagnetPull.isPulling(watching), "it ends the tick he becomes a Spectator");
                check(ores.stream().allMatch(ore -> helper.getBlockState(ore).is(Blocks.IRON_ORE)), "and took nothing");
                sneakR(dying);
                check(MagnetPull.isPulling(dying), "another pull begins");
                dying.kill();
                check(!MagnetPull.isPulling(dying), "and ends when its Magneto dies");
            }
            if (tick == 5) {
                PowerTicker.tick(dying);
                check(ores.stream().allMatch(ore -> helper.getBlockState(ore).is(Blocks.IRON_ORE)), "nothing more came");
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Slicing and limits (M29, M30, M31, M50)

    /**
     * M29. With 2 a tick and at most 10 a pull: after the first pull tick at most 2 cells changed; then exactly the 10
     * nearest of 30 raw iron blocks, and 20 stay; his payloads carry 10 streams in all, never more than 2 at once, and the
     * first says the field began, with its radius.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_budget")
    public static void thePullIsSlicedAndBounded(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_PER_TICK, 2);
        f.config(EnhancedConfig.PULL_MAX_TARGETS, 10);
        ServerPlayer magneto = magneto(f);
        List<BlockPos> near = List.of(new BlockPos(11, 1, 10), new BlockPos(9, 1, 10), new BlockPos(10, 1, 11), new BlockPos(10, 1, 9),
                new BlockPos(12, 1, 10), new BlockPos(8, 1, 10), new BlockPos(10, 1, 12), new BlockPos(10, 1, 8), new BlockPos(11, 1, 11),
                new BlockPos(9, 1, 9));
        List<BlockPos> far = List.of(new BlockPos(16, 1, 10), new BlockPos(5, 1, 10), new BlockPos(10, 1, 16), new BlockPos(10, 1, 5),
                new BlockPos(16, 1, 12), new BlockPos(5, 1, 8), new BlockPos(12, 1, 16), new BlockPos(8, 1, 5), new BlockPos(15, 1, 14),
                new BlockPos(6, 1, 7), new BlockPos(14, 1, 15), new BlockPos(7, 1, 6), new BlockPos(16, 1, 8), new BlockPos(5, 1, 12),
                new BlockPos(8, 1, 16), new BlockPos(12, 1, 5), new BlockPos(15, 1, 7), new BlockPos(6, 1, 14), new BlockPos(7, 1, 15),
                new BlockPos(14, 1, 6));
        List<BlockPos> all = new ArrayList<>(near);
        all.addAll(far);
        for (BlockPos cell : all) {
            helper.setBlock(cell, Blocks.RAW_IRON_BLOCK);
        }
        List<Object> packets = new ArrayList<>();
        TestKit.sentPackets(magneto);
        sneakR(magneto);
        packets.addAll(TestKit.sentPackets(magneto));

        f.everyTick(12, tick -> {
            PowerTicker.tick(magneto);
            packets.addAll(TestKit.sentPackets(magneto));
            long taken = all.stream().filter(cell -> helper.getBlockState(cell).isAir()).count();
            if (tick == 1) {
                check(taken <= 2, "the first pull tick took at most 2, not " + taken);
            }
            if (tick < 8) {
                return false;
            }
            check(taken == 10, "exactly 10 were taken, not " + taken);
            check(near.stream().allMatch(cell -> helper.getBlockState(cell).isAir()), "the 10 nearest");
            check(far.stream().allMatch(cell -> helper.getBlockState(cell).is(Blocks.RAW_IRON_BLOCK)), "and the 20 others stay");
            List<MagnetPullPayload> sent = payloads(packets);
            check(!sent.isEmpty() && sent.get(0).first() && sent.get(0).radius() == 8.0F, "the first payload starts the field, radius 8");
            check(sent.stream().skip(1).noneMatch(MagnetPullPayload::first), "and only the first");
            check(sent.stream().mapToInt(p -> p.streams().size()).sum() == 10, "10 streams in all");
            check(sent.stream().allMatch(p -> p.streams().size() <= 2), "never more than 2 in one tick");
            check(sent.stream().allMatch(p -> p.magneto() == magneto.getId()), "each names Magneto");
            return true;
        });
    }

    /**
     * M30. At radius 9 and 512 cells a tick, the search spreads over several ticks, and still finds the ores at the far
     * edge of the sphere.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_scan")
    public static void theSearchSpreadsOverTicksAtABigRadius(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_RADIUS, 9);
        f.config(EnhancedConfig.PULL_SCAN_PER_TICK, 512);
        ServerPlayer magneto = magneto(f);
        // Ores in every part of the floor, so the search reads every section the sphere meets there; four at its edge.
        List<BlockPos> ores = List.of(new BlockPos(2, 1, 10), new BlockPos(18, 1, 10), new BlockPos(10, 1, 2), new BlockPos(10, 1, 18),
                new BlockPos(5, 1, 5), new BlockPos(16, 1, 5), new BlockPos(5, 1, 16), new BlockPos(16, 1, 16), new BlockPos(12, 1, 12));
        for (BlockPos ore : ores) {
            helper.setBlock(ore, Blocks.IRON_ORE);
        }
        long pressed = helper.getLevel().getGameTime();
        sneakR(magneto);
        afterThePull(f, magneto, 40, () -> {
            check(ores.stream().allMatch(ore -> helper.getBlockState(ore).is(Blocks.STONE)), "every ore gave its iron, the far ones too");
            check(count(magneto, Items.RAW_IRON) == ores.size(), "all of it his");
            check(MagnetPull.searchEndedAt(magneto) > pressed, "the search took more than the tick of the press");
        });
    }

    /**
     * M31. The pull stops at once when Magneto dies: 3 taken (one a tick), and 20 ticks later still 3. The same when he
     * loses the power of Magneto.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_stop", timeoutTicks = 100)
    public static void thePullStopsWhenMagnetoDies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_PER_TICK, 1);
        List<BlockPos> blocks = ring(20);
        for (BlockPos cell : blocks) {
            helper.setBlock(cell, Blocks.RAW_IRON_BLOCK);
        }
        ServerPlayer dies = magneto(f);
        sneakR(dies);
        ServerPlayer[] changes = new ServerPlayer[1];
        f.everyTick(60, tick -> {
            if (tick <= 3) {
                PowerTicker.tick(dies);
            }
            if (tick == 3) {
                check(taken(helper, blocks) == 3, "3 taken, one a tick: " + taken(helper, blocks));
                dies.getInventory().clearContent(); // what he took stays out of the arena when he drops everything
                dies.kill();
                check(!MagnetPull.isPulling(dies), "his pull ends when he dies");
            } else if (tick > 3 && tick < 23) {
                PowerTicker.tick(dies);
            } else if (tick == 23) {
                check(taken(helper, blocks) == 3, "20 ticks later still 3");
                for (BlockPos cell : blocks) {
                    helper.setBlock(cell, Blocks.RAW_IRON_BLOCK);
                }
                for (ItemEntity item : helper.getEntities(EntityType.ITEM)) {
                    item.discard();
                }
                changes[0] = magneto(f);
                sneakR(changes[0]);
            } else if (tick > 23 && tick <= 26) {
                PowerTicker.tick(changes[0]);
                if (tick == 26) {
                    check(taken(helper, blocks) == 3, "the next Magneto took 3: " + taken(helper, blocks));
                    Powers.setPower(changes[0], Power.SCARLET_WITCH);
                    check(!MagnetPull.isPulling(changes[0]), "his pull ends when he is Magneto no more");
                }
            } else if (tick > 26) {
                PowerTicker.tick(changes[0]);
                if (tick == 46) {
                    check(taken(helper, blocks) == 3, "and 20 ticks later still 3");
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * M50. The pull stays where he pressed: teleported 100 blocks away after 2 pull ticks (one a tick), exactly 2 were
     * taken, 20 ticks later still 2, and the pull is over, with the line for what came so far.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_move")
    public static void theJobStaysWhereHePressed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_PER_TICK, 1);
        List<BlockPos> blocks = ring(20);
        for (BlockPos cell : blocks) {
            helper.setBlock(cell, Blocks.RAW_IRON_BLOCK);
        }
        ServerPlayer magneto = magneto(f);
        sneakR(magneto);
        f.everyTick(30, tick -> {
            PowerTicker.tick(magneto);
            if (tick == 2) {
                check(taken(helper, blocks) == 2, "2 taken: " + taken(helper, blocks));
                magneto.teleportTo(helper.getLevel(), magneto.getX() + 100.0, magneto.getY(), magneto.getZ(), magneto.getYRot(), 0.0F);
                TestKit.sentPackets(magneto);
            }
            if (tick == 3) {
                check(!MagnetPull.isPulling(magneto), "the pull ends once he is far from where he pressed");
                check("message.enhanced.pull.done".equals(BossTestSupport.lastActionBarKey(magneto)), "and he reads that the metal came");
            }
            if (tick == 22) {
                check(taken(helper, blocks) == 2, "and 20 ticks later still exactly 2: " + taken(helper, blocks));
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Protection (M32, M47)

    /**
     * M32. A claim that refuses everything: nothing comes, he reads that the metal here is protected, and the pull does not
     * rest. A claim on the chunks west of a border: the ores and the ingot there stay, those east of it come, an ingot found
     * east of it that drifts onto the claim before its turn stays there, and he reads that the rest is protected. In
     * Adventure the ores stay and a dropped ingot comes. In the cylinder of a running fight, protected or not (R5), the
     * ore stays and the one outside comes.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_rights", timeoutTicks = 100)
    public static void thePullNeverTouchesProtectedPlaces(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        // Claims go by chunks: the border is the chunk border nearest the middle of the floor.
        int border = Math.floorDiv(origin.getX() + 18, 16) * 16;
        int east = border - origin.getX();
        double standX = Math.max(8.5, Math.min(12.5, east));
        List<BlockPos> westOres = List.of(new BlockPos(east - 1, 1, 7), new BlockPos(east - 1, 1, 13));
        List<BlockPos> eastOres = List.of(new BlockPos(east, 1, 7), new BlockPos(east, 1, 13));

        // A claim on everything.
        Consumer<BlockEvent.BreakEvent> all = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, all);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(all));
        List<BlockPos> four = List.of(new BlockPos(6, 1, 10), new BlockPos(14, 1, 10), new BlockPos(10, 1, 6), new BlockPos(10, 1, 14));
        for (BlockPos ore : four) {
            helper.setBlock(ore, Blocks.IRON_ORE);
        }
        ServerPlayer first = magneto(f);
        BossTestSupport.lastActionBarKey(first);
        sneakR(first);
        NeoForge.EVENT_BUS.unregister(all);
        check("message.enhanced.pull.protected".equals(BossTestSupport.lastActionBarKey(first)), "everything protected: he is told so");
        check(four.stream().allMatch(ore -> helper.getBlockState(ore).is(Blocks.IRON_ORE)) && count(first, Items.RAW_IRON) == 0, "nothing came");
        check(Powers.get(first).isReady(1, level.getGameTime()), "and the pull does not rest");
        for (BlockPos ore : four) {
            helper.setBlock(ore, Blocks.STONE);
        }

        // A claim on the chunks west of the border.
        Consumer<BlockEvent.BreakEvent> west = event -> event.setCanceled(event.getPos().getX() < border);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, west);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(west));
        for (BlockPos ore : westOres) {
            helper.setBlock(ore, Blocks.IRON_ORE);
        }
        for (BlockPos ore : eastOres) {
            helper.setBlock(ore, Blocks.IRON_ORE);
        }
        ItemEntity westIngot = ingot(helper, new Vec3(east - 0.5, FLOOR, 10.5));
        ItemEntity eastIngot = ingot(helper, new Vec3(east + 0.5, FLOOR, 10.5));
        // Found east of the border, the farthest of the targets: it drifts onto the claim before its turn comes.
        ItemEntity drifter = ingot(helper, new Vec3(east + 0.5, FLOOR, 15.5));
        ServerPlayer second = f.player(standX, FLOOR, 10.5, Power.MAGNETO);
        List<Object> secondPackets = new ArrayList<>();
        TestKit.sentPackets(second);
        sneakR(second);

        ServerPlayer[] adventurer = new ServerPlayer[1];
        ServerPlayer[] fighters = new ServerPlayer[2];
        BlockPos inside = new BlockPos(4, 1, 10);
        BlockPos outside = new BlockPos(15, 1, 10);
        f.everyTick(80, tick -> {
            if (tick <= 6) {
                PowerTicker.tick(second);
                secondPackets.addAll(TestKit.sentPackets(second));
            }
            if (tick == 1) {
                check(MagnetPull.isPulling(second) && drifter.isAlive(), "the pull has taken the nearest, not yet the drifting ingot");
                drifter.setPos(helper.absoluteVec(new Vec3(east - 0.5, FLOOR, 15.5)));
            }
            if (tick == 6) {
                NeoForge.EVENT_BUS.unregister(west);
                check(!MagnetPull.isPulling(second), "the second pull is over");
                check(westOres.stream().allMatch(ore -> helper.getBlockState(ore).is(Blocks.IRON_ORE)), "the claimed ores stay");
                check(westIngot.isAlive(), "and so does the ingot on the claim");
                check(drifter.isAlive() && drifter.getX() < border, "and the ingot that drifted onto the claim before its turn stays there");
                check(eastOres.stream().allMatch(ore -> helper.getBlockState(ore).is(Blocks.STONE)) && !eastIngot.isAlive(),
                        "the ores and the ingot east of the border come");
                check(count(second, Items.RAW_IRON) == 2 && count(second, Items.IRON_INGOT) == 1, "two raw iron and an ingot are his");
                List<String> lines = actionBar(secondPackets);
                check(!lines.isEmpty() && "message.enhanced.pull.done_protected".equals(lines.get(lines.size() - 1)),
                        "he reads that the rest is protected: " + lines);
                westIngot.discard();
                drifter.discard();
                for (BlockPos ore : westOres) {
                    helper.setBlock(ore, Blocks.STONE);
                }

                // Adventure: the ores are the world's, which he may not break; what lies on the ground is anyone's.
                for (BlockPos ore : four) {
                    helper.setBlock(ore, Blocks.IRON_ORE);
                }
                ingot(helper, new Vec3(12.5, FLOOR, 12.5));
                adventurer[0] = f.player(10.5, FLOOR, 10.5, GameType.ADVENTURE);
                Powers.setPower(adventurer[0], Power.MAGNETO);
                sneakR(adventurer[0]);
            } else if (tick > 6 && tick <= 10) {
                PowerTicker.tick(adventurer[0]);
                if (tick == 10) {
                    check(!MagnetPull.isPulling(adventurer[0]), "the Adventure pull is over");
                    check(four.stream().allMatch(ore -> helper.getBlockState(ore).is(Blocks.IRON_ORE)), "in Adventure the ores stay");
                    check(count(adventurer[0], Items.IRON_INGOT) == 1 && count(adventurer[0], Items.RAW_IRON) == 0, "and the ingot comes");
                    for (BlockPos ore : four) {
                        helper.setBlock(ore, Blocks.STONE);
                    }
                    fighters[0] = fightPull(f, helper, true, inside, outside);
                }
            } else if (tick > 10 && tick <= 14) {
                PowerTicker.tick(fighters[0]);
                if (tick == 14) {
                    check(helper.getBlockState(inside).is(Blocks.IRON_ORE), "the ore in a protected arena stays");
                    check(helper.getBlockState(outside).is(Blocks.STONE) && count(fighters[0], Items.RAW_IRON) == 1, "the ore outside it comes");
                    BossFights.stopAll(level.getServer(), StopReason.SERVER_STOPPING);
                    fighters[1] = fightPull(f, helper, false, inside, outside);
                }
            } else if (tick > 14 && tick <= 18) {
                PowerTicker.tick(fighters[1]);
                if (tick == 18) {
                    check(helper.getBlockState(inside).is(Blocks.IRON_ORE), "the ore in an arena that protects nothing (Thanos's) stays too");
                    check(helper.getBlockState(outside).is(Blocks.STONE) && count(fighters[1], Items.RAW_IRON) == 1, "the ore outside it comes");
                    return true;
                }
            }
            return false;
        });
    }

    /** A fight whose cylinder covers the west of the floor, an ore in it and one outside, and a new Magneto's pull. */
    private static ServerPlayer fightPull(TestKit.Fixture f, GameTestHelper helper, boolean protect, BlockPos inside, BlockPos outside) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(inside, Blocks.IRON_ORE);
        helper.setBlock(outside, Blocks.IRON_ORE);
        ServerPlayer magneto = magneto(f);
        BossTestSupport.TestArena arena = new BossTestSupport.TestArena(level, helper.absoluteVec(new Vec3(2.5, FLOOR, 10.5)), 4.0,
                Containment.NONE, protect);
        arena.containAt(3.0).joinWithin(5.0).tractorDepth(0.0);
        BossFrameworkTests.start(f, BossKind.ULTRON, f.player(2.5, FLOOR, 10.5), new BossTestSupport.TestFight(level, BossKind.ULTRON, arena));
        check(BossFights.inAnyArena(level, helper.absolutePos(inside)) && !BossFights.inAnyArena(level, helper.absolutePos(outside)),
                "the arena holds one ore and not the other");
        sneakR(magneto);
        return magneto;
    }

    /**
     * M47. A blast furnace and a bell that villagers have claimed (a job site, a meeting point) stay; an unclaimed blast
     * furnace comes. And the pull's answer to R names the claimed one as not coming.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_poi")
    public static void aVillagersWorkStays(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        int[] claimedFurnace = FAN[2];
        int[] bell = FAN[4];
        int[] freeFurnace = FAN[8];
        onPillar(helper, claimedFurnace, Blocks.BLAST_FURNACE.defaultBlockState());
        onPillar(helper, bell, Blocks.BELL.defaultBlockState());
        onPillar(helper, freeFurnace, Blocks.BLAST_FURNACE.defaultBlockState());
        f.everyTick(20, tick -> {
            if (tick == 2) {
                // The points of interest are noted a moment after the blocks are set; then villagers take two of them.
                for (int[] at : List.of(claimedFurnace, bell)) {
                    BlockPos pos = helper.absolutePos(new BlockPos(at[0], 3, at[1]));
                    check(level.getPoiManager().getType(pos).isPresent(), "the block at " + at[0] + ", " + at[1] + " is a point of interest");
                    check(level.getPoiManager().take(type -> true, (type, p) -> p.equals(pos), pos, 1).isPresent(), "and a villager claims it");
                }
                check(MagnetPull.claimedByVillager(level, helper.absolutePos(new BlockPos(claimedFurnace[0], 3, claimedFurnace[1])),
                        Blocks.BLAST_FURNACE.defaultBlockState()), "the armorer's blast furnace is claimed");
                check(!MagnetPull.wouldTakeBuilt(level, helper.absolutePos(new BlockPos(claimedFurnace[0], 3, claimedFurnace[1])),
                        Blocks.BLAST_FURNACE.defaultBlockState()), "the pull would not take it (what R tells him)");
                sneakR(magneto);
                return false;
            }
            if (tick < 3) {
                return false;
            }
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(helper.getBlockState(new BlockPos(claimedFurnace[0], 3, claimedFurnace[1])).is(Blocks.BLAST_FURNACE), "the claimed blast furnace stays");
            check(helper.getBlockState(new BlockPos(bell[0], 3, bell[1])).is(Blocks.BELL), "the meeting point's bell stays");
            check(helper.getBlockState(new BlockPos(freeFurnace[0], 3, freeFurnace[1])).isAir() && count(magneto, Items.BLAST_FURNACE) == 1,
                    "the unclaimed blast furnace is his");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sight, the cone, the switches (M34a, M35, M46, M59)

    /** M34a. With built metal off, the ore and the raw iron block come; the iron block, the rail and the hopper stay. */
    @GameTest(template = ARENA, batch = "magnet_pull_built")
    public static void theBuiltMetalSwitch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_TAKES_BUILT_METAL, false);
        ServerPlayer magneto = magneto(f);
        BlockPos ore = new BlockPos(6, 1, 10);
        BlockPos raw = new BlockPos(10, 1, 6);
        helper.setBlock(ore, Blocks.IRON_ORE);
        helper.setBlock(raw, Blocks.RAW_IRON_BLOCK);
        onPillar(helper, FAN[2], Blocks.IRON_BLOCK.defaultBlockState());
        onPillar(helper, FAN[4], Blocks.RAIL.defaultBlockState());
        onPillar(helper, FAN[8], Blocks.HOPPER.defaultBlockState());
        check(!MagnetPull.wouldTakeBuilt(helper.getLevel(), helper.absolutePos(new BlockPos(FAN[8][0], 3, FAN[8][1])),
                Blocks.HOPPER.defaultBlockState()), "the pull would not take the hopper (what R tells him)");
        sneakR(magneto);
        afterThePull(f, magneto, 20, () -> {
            check(helper.getBlockState(ore).is(Blocks.STONE) && count(magneto, Items.RAW_IRON) == 1, "the ore gives its iron");
            check(helper.getBlockState(raw).isAir() && count(magneto, Items.RAW_IRON_BLOCK) == 1, "the raw iron block comes");
            check(helper.getBlockState(new BlockPos(FAN[2][0], 3, FAN[2][1])).is(Blocks.IRON_BLOCK), "the iron block stays");
            check(helper.getBlockState(new BlockPos(FAN[4][0], 3, FAN[4][1])).is(Blocks.RAIL), "the rail stays");
            check(helper.getBlockState(new BlockPos(FAN[8][0], 3, FAN[8][1])).is(Blocks.HOPPER), "the hopper stays");
        });
    }

    /** M35. With the debris switch off, ancient debris stays and an iron ore still comes. */
    @GameTest(template = ARENA, batch = "magnet_pull_debris")
    public static void theDebrisSwitch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_ANCIENT_DEBRIS, false);
        ServerPlayer magneto = magneto(f);
        BlockPos debris = new BlockPos(6, 1, 10);
        BlockPos ore = new BlockPos(14, 1, 10);
        helper.setBlock(debris, Blocks.ANCIENT_DEBRIS);
        helper.setBlock(ore, Blocks.IRON_ORE);
        sneakR(magneto);
        afterThePull(f, magneto, 20, () -> {
            check(helper.getBlockState(debris).is(Blocks.ANCIENT_DEBRIS) && count(magneto, Items.NETHERITE_SCRAP) == 0, "the debris stays");
            check(helper.getBlockState(ore).is(Blocks.STONE) && count(magneto, Items.RAW_IRON) == 1, "the iron ore comes");
        });
    }

    /**
     * M46. Built metal comes only in sight: behind a stone wall the iron block stays while the ore beside it turns to stone
     * and gives its iron (magnetism goes through rock); an iron block in plain sight and a rail seen over a carpet and a
     * bottom slab come.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_sight")
    public static void builtMetalNeedsSight(GameTestHelper helper) {
        sight(helper, true);
    }

    /** M46, with {@code magneto.pullBuiltNeedsSight} off: the iron block behind the wall comes too. */
    @GameTest(template = ARENA, batch = "magnet_pull_sight_off")
    public static void builtMetalNeedsSightUnlessSwitchedOff(GameTestHelper helper) {
        sight(helper, false);
    }

    private static void sight(GameTestHelper helper, boolean needsSight) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_BUILT_NEEDS_SIGHT, needsSight);
        ServerPlayer magneto = magneto(f);
        for (int z = 9; z <= 11; z++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(14, y, z), Blocks.STONE); // a wall 4 blocks east of him
            }
        }
        BlockPos hidden = new BlockPos(15, 2, 10);
        BlockPos hiddenOre = new BlockPos(15, 2, 11);
        helper.setBlock(hidden, Blocks.IRON_BLOCK);
        helper.setBlock(hiddenOre, Blocks.IRON_ORE);
        BlockPos seenBlock = new BlockPos(13, 3, 6);
        helper.setBlock(seenBlock.below(), Blocks.STONE);
        helper.setBlock(seenBlock, Blocks.IRON_BLOCK);
        BlockPos rail = new BlockPos(14, 2, 14);
        helper.setBlock(rail, Blocks.RAIL);
        helper.setBlock(new BlockPos(12, 2, 12), Blocks.WHITE_CARPET);
        helper.setBlock(new BlockPos(13, 2, 13), Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        check(!seen(helper, magneto, hidden.getX(), hidden.getY(), hidden.getZ()), "the wall hides the iron block");
        check(seen(helper, magneto, seenBlock.getX(), seenBlock.getY(), seenBlock.getZ()), "the other iron block is in plain sight");
        check(seen(helper, magneto, rail.getX(), rail.getY(), rail.getZ()), "the rail is seen over the carpet and the slab");

        sneakR(magneto);
        afterThePull(f, magneto, 20, () -> {
            check(helper.getBlockState(hiddenOre).is(Blocks.STONE) && count(magneto, Items.RAW_IRON) == 1,
                    "the hidden ore gives its iron through the rock");
            check(helper.getBlockState(seenBlock).isAir() && helper.getBlockState(rail).isAir() && count(magneto, Items.RAIL) == 1,
                    "the iron block in sight and the rail come");
            if (needsSight) {
                check(helper.getBlockState(hidden).is(Blocks.IRON_BLOCK) && count(magneto, Items.IRON_BLOCK) == 1,
                        "the iron block behind the wall stays");
            } else {
                check(helper.getBlockState(hidden).isAir() && count(magneto, Items.IRON_BLOCK) == 2,
                        "with sight switched off, the iron block behind the wall comes too");
            }
        });
    }

    /**
     * M59 (R36). Built metal comes only from where he looks: looking east, a lantern and a rail east of him come; a
     * lantern and a hopper behind him stay, while the ore behind him gives its iron and a dropped ingot behind him comes.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void builtMetalComesFromWhereHeLooks(GameTestHelper helper) {
        cone(helper, false);
    }

    /** M59 with {@code magneto.pullBuiltCone} 360: the lantern behind him comes too. */
    @GameTest(template = ARENA, batch = "magnet_pull_cone_all")
    public static void builtMetalComesFromAllRoundAt360(GameTestHelper helper) {
        cone(helper, true);
    }

    private static void cone(GameTestHelper helper, boolean allRound) {
        TestKit.Fixture f = TestKit.fixture(helper);
        if (allRound) {
            f.config(EnhancedConfig.PULL_BUILT_CONE, 360);
        }
        ServerPlayer magneto = magneto(f);
        onPillar(helper, FAN[2], Blocks.LANTERN.defaultBlockState());
        onPillar(helper, FAN[8], Blocks.RAIL.defaultBlockState());
        BlockPos lanternBehind = new BlockPos(6, 3, 9);
        BlockPos hopperBehind = new BlockPos(7, 3, 12);
        onPillar(helper, new int[]{lanternBehind.getX(), lanternBehind.getZ()}, Blocks.LANTERN.defaultBlockState());
        onPillar(helper, new int[]{hopperBehind.getX(), hopperBehind.getZ()}, Blocks.HOPPER.defaultBlockState());
        BlockPos ore = new BlockPos(7, 1, 10);
        helper.setBlock(ore, Blocks.IRON_ORE);
        ItemEntity ingot = ingot(helper, new Vec3(6.5, FLOOR, 11.5));
        check(seen(helper, magneto, lanternBehind.getX(), lanternBehind.getY(), lanternBehind.getZ())
                && seen(helper, magneto, hopperBehind.getX(), hopperBehind.getY(), hopperBehind.getZ()), "he could see what is behind him");

        sneakR(magneto);
        afterThePull(f, magneto, 20, () -> {
            check(helper.getBlockState(new BlockPos(FAN[2][0], 3, FAN[2][1])).isAir() && helper.getBlockState(new BlockPos(FAN[8][0], 3, FAN[8][1])).isAir()
                    && count(magneto, Items.RAIL) == 1, "the lantern and the rail he faces come");
            check(helper.getBlockState(ore).is(Blocks.STONE) && count(magneto, Items.RAW_IRON) == 1, "the ore behind him gives its iron");
            check(!ingot.isAlive() && count(magneto, Items.IRON_INGOT) == 1, "the ingot behind him comes");
            if (allRound) {
                check(helper.getBlockState(lanternBehind).isAir() && count(magneto, Items.LANTERN) == 2, "at 360 the lantern behind him comes too");
            } else {
                check(helper.getBlockState(lanternBehind).is(Blocks.LANTERN) && count(magneto, Items.LANTERN) == 1, "the lantern behind him stays");
                check(helper.getBlockState(hopperBehind).is(Blocks.HOPPER), "and so does the hopper behind him");
            }
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Storage and delivery (M23, M48, M57)

    /**
     * M23. With his inventory full, what the ores give drops at his feet (he reads it once), and the 70 ingots he has no
     * room for were never searched: they lie where they were, and did not crowd the ores out of the pull's 64 places.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_full")
    public static void aFullInventoryDropsTheRestAtHisFeet(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_MAX_TARGETS, 64);
        ServerPlayer magneto = magneto(f);
        for (int slot = 0; slot < magneto.getInventory().items.size(); slot++) {
            magneto.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        List<BlockPos> ores = List.of(new BlockPos(4, 1, 10), new BlockPos(16, 1, 10), new BlockPos(10, 1, 4));
        for (BlockPos ore : ores) {
            helper.setBlock(ore, Blocks.IRON_ORE);
        }
        List<ItemEntity> ingots = new ArrayList<>();
        List<Vec3> where = new ArrayList<>();
        for (double x = 6.25; x <= 14.75 && ingots.size() < 70; x += 0.5) {
            for (double z = 6.25; z <= 14.75 && ingots.size() < 70; z += 0.5) {
                double d = Math.hypot(x - 10.5, z - 10.5);
                if (d >= 2.0 && d <= 4.0) {
                    ItemEntity ingot = ingot(helper, new Vec3(x, FLOOR, z));
                    ingot.setUnlimitedLifetime(); // and so it never merges with its neighbours
                    ingots.add(ingot);
                    where.add(ingot.position());
                }
            }
        }
        check(ingots.size() == 70, "70 ingots lie round him, not " + ingots.size());
        List<Object> packets = new ArrayList<>();
        TestKit.sentPackets(magneto);
        sneakR(magneto);
        packets.addAll(TestKit.sentPackets(magneto));
        f.everyTick(20, tick -> {
            PowerTicker.tick(magneto);
            packets.addAll(TestKit.sentPackets(magneto));
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(ores.stream().allMatch(ore -> helper.getBlockState(ore).is(Blocks.STONE)), "the ores gave their iron");
            int atFeet = 0;
            for (ItemEntity item : helper.getEntities(EntityType.ITEM)) {
                if (item.getItem().is(Items.RAW_IRON) && item.position().distanceTo(magneto.position()) <= 1.5) {
                    atFeet += item.getItem().getCount();
                }
            }
            check(atFeet == 3, "the 3 raw iron lie at his feet, not " + atFeet);
            for (int i = 0; i < ingots.size(); i++) {
                check(ingots.get(i).isAlive() && ingots.get(i).position().distanceTo(where.get(i)) < 0.01, "ingot " + i + " lies where it was");
            }
            check(actionBar(packets).stream().filter("message.enhanced.pull.full"::equals).count() == 1, "he read that his inventory is full, once");
            return true;
        });
    }

    /**
     * M23 in Creative (D-M 4.4: nothing is ever lost). For a Creative player the game's {@code Inventory.add} empties a
     * stack that does not fit and calls it done. A Creative Magneto with a full inventory, with PvP and {@code
     * magneto.pullFromStorage} on: the ore's raw iron and the sword torn from A's hand drop at his feet (he reads it once),
     * and the chest keeps its ingots, the item frame its sword and the armour stand its chestplate, as for anyone without
     * room.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_full_creative")
    public static void aFullInventoryInCreativeLosesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(EnhancedConfig.PULL_FROM_STORAGE, true);
        ServerPlayer magneto = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        Powers.setPower(magneto, Power.MAGNETO);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        check(magneto.hasInfiniteMaterials(), "Magneto plays in Creative");
        for (int slot = 0; slot < magneto.getInventory().items.size(); slot++) {
            magneto.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        BlockPos ore = new BlockPos(10, 1, 6);
        helper.setBlock(ore, Blocks.IRON_ORE);
        ServerPlayer a = f.player(14.5, FLOOR, 10.5);
        a.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        BlockPos chest = new BlockPos(13, 2, 8);
        helper.setBlock(chest, Blocks.CHEST);
        container(helper, chest).setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        ItemFrame frame = frame(helper, new BlockPos(14, 3, 13), new ItemStack(Items.IRON_SWORD));
        ArmorStand stand = stand(helper, new Vec3(12.5, FLOOR, 14.5), Items.IRON_CHESTPLATE);
        check(Raycasts.inSight(magneto, a.getEyePosition()) && seen(helper, magneto, chest.getX(), chest.getY(), chest.getZ()),
                "he sees A and the chest");
        List<Object> packets = new ArrayList<>();
        TestKit.sentPackets(magneto);
        sneakR(magneto);
        packets.addAll(TestKit.sentPackets(magneto));
        f.everyTick(20, tick -> {
            PowerTicker.tick(magneto);
            packets.addAll(TestKit.sentPackets(magneto));
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(helper.getBlockState(ore).is(Blocks.STONE) && a.getMainHandItem().isEmpty(), "the ore gave its iron and A lost his sword");
            check(atFeet(helper, magneto, Items.RAW_IRON) == 1, "the raw iron lies at his feet");
            check(atFeet(helper, magneto, Items.IRON_SWORD) == 1, "and so does A's sword: nothing is lost");
            check(container(helper, chest).getItem(0).getCount() == 5, "the chest keeps its ingots");
            check(frame.getItem().is(Items.IRON_SWORD), "the frame keeps its sword");
            check(stand.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "the stand keeps its chestplate");
            check(actionBar(packets).stream().filter("message.enhanced.pull.full"::equals).count() == 1, "he read that his inventory is full, once");
            return true;
        });
    }

    /**
     * M48 (R38). With {@code magneto.pullFromStorage} off: a chest keeps its ingots, a barrel its sword, an item frame its
     * ingot, an armour stand its iron chestplate; the ingot lying beside them comes.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void aChestKeepsItsMetal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = magneto(f);
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(13, 2, 8), Blocks.CHEST);
        container(helper, new BlockPos(13, 2, 8)).setItem(0, new ItemStack(Items.IRON_INGOT, 10));
        helper.setBlock(new BlockPos(13, 2, 12), Blocks.BARREL);
        container(helper, new BlockPos(13, 2, 12)).setItem(0, new ItemStack(Items.IRON_SWORD));
        ItemFrame frame = frame(helper, new BlockPos(14, 3, 10), new ItemStack(Items.IRON_INGOT));
        ArmorStand stand = stand(helper, new Vec3(12.5, FLOOR, 14.5), Items.IRON_CHESTPLATE);
        check(Raycasts.inSight(magneto, stand.position().add(0, stand.getBbHeight() * 0.5, 0)), "he sees the armour stand");
        ItemEntity loose = ingot(helper, new Vec3(12.5, FLOOR, 10.5));
        sneakR(magneto);
        afterThePull(f, magneto, 20, () -> {
            ItemStack chest = container(helper, new BlockPos(13, 2, 8)).getItem(0);
            check(chest.is(Items.IRON_INGOT) && chest.getCount() == 10, "the chest keeps its 10 ingots");
            check(container(helper, new BlockPos(13, 2, 12)).getItem(0).is(Items.IRON_SWORD), "the barrel keeps its sword");
            check(frame.isAlive() && frame.getItem().is(Items.IRON_INGOT), "the frame keeps its ingot");
            check(stand.isAlive() && stand.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                    && count(magneto, Items.IRON_CHESTPLATE) == 0, "the armour stand keeps its iron chestplate");
            check(!loose.isAlive() && count(magneto, Items.IRON_INGOT) == 1 && count(magneto, Items.IRON_SWORD) == 0,
                    "the loose ingot is his, and nothing else");
            check(level.getBlockState(helper.absolutePos(new BlockPos(13, 2, 8))).is(Blocks.CHEST), "the chest stands");
        });
    }

    /**
     * M57 (R38). With {@code magneto.pullFromStorage} on: the iron of a chest in sight is his and its dirt stays; an item
     * frame's sword and an armour stand's chestplate are his, the frame and the stand stay; a fisherman's claimed barrel
     * keeps its iron, and so does a chest behind a wall. A map maker's locks hold as against a hand: a fixed frame
     * keeps its sword, and a stand whose chest slot is locked its chestplate. The claims are asked for a frame or a
     * stand as for a chest: an armour stand on a claimed cell keeps its chestplate while a loose ingot beside it comes;
     * a chest on someone's claim keeps its iron; and a Magneto in Adventure takes a loose ingot but no frame's or
     * stand's metal.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_storage", timeoutTicks = 100)
    public static void storageOnlyWhenAsked(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_FROM_STORAGE, true);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        BlockPos chest = new BlockPos(13, 2, 8);
        helper.setBlock(chest, Blocks.CHEST);
        container(helper, chest).setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        container(helper, chest).setItem(1, new ItemStack(Items.DIRT, 7));
        ItemFrame frame = frame(helper, new BlockPos(14, 3, 13), new ItemStack(Items.IRON_SWORD));
        ArmorStand stand = stand(helper, new Vec3(12.5, FLOOR, 14.5), Items.IRON_CHESTPLATE);
        BlockPos barrel = new BlockPos(16, 2, 10);
        helper.setBlock(barrel, Blocks.BARREL);
        container(helper, barrel).setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        BlockPos hiddenChest = new BlockPos(7, 2, 10);
        helper.setBlock(hiddenChest, Blocks.CHEST);
        container(helper, hiddenChest).setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        for (int z = 9; z <= 11; z++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(8, y, z), Blocks.STONE);
            }
        }
        BlockPos claimedChest = new BlockPos(12, 2, 5);
        ItemFrame fixedFrame = frame(helper, new BlockPos(14, 3, 7), new ItemStack(Items.IRON_SWORD));
        fixedFrame.fixed = true;
        ArmorStand lockedStand = stand(helper, new Vec3(12.5, FLOOR, 6.5), Items.IRON_CHESTPLATE);
        lockedStand.disabledSlots = 1 << EquipmentSlot.CHEST.getFilterFlag() + 8; // its chestplate cannot be taken out
        ServerPlayer[] adventurer = new ServerPlayer[1];
        ItemEntity[] loose = new ItemEntity[1];
        List<Consumer<BlockEvent.BreakEvent>> claims = new ArrayList<>();
        f.onClose(() -> claims.forEach(NeoForge.EVENT_BUS::unregister));
        List<Object> packets = new ArrayList<>();
        int[] stage = {0};
        f.everyTick(90, tick -> {
            if (tick == 2) {
                BlockPos pos = helper.absolutePos(barrel);
                check(level.getPoiManager().take(type -> true, (type, p) -> p.equals(pos), pos, 1).isPresent(), "a fisherman claims the barrel");
                check(seen(helper, magneto, chest.getX(), chest.getY(), chest.getZ()) && seen(helper, magneto, barrel.getX(), barrel.getY(), barrel.getZ()),
                        "he sees the chest and the barrel");
                check(!seen(helper, magneto, hiddenChest.getX(), hiddenChest.getY(), hiddenChest.getZ()), "but not the chest behind the wall");
                check(Raycasts.inSight(magneto, fixedFrame.position().add(0, fixedFrame.getBbHeight() * 0.5, 0))
                        && Raycasts.inSight(magneto, lockedStand.position().add(0, lockedStand.getBbHeight() * 0.5, 0)),
                        "he sees the fixed frame and the locked stand");
                sneakR(magneto);
                return false;
            }
            if (tick < 3) {
                return false;
            }
            PowerTicker.tick(magneto);
            packets.addAll(TestKit.sentPackets(magneto));
            if (adventurer[0] != null) {
                PowerTicker.tick(adventurer[0]);
            }
            if (MagnetPull.isPulling(magneto) || adventurer[0] != null && MagnetPull.isPulling(adventurer[0])) {
                return false;
            }
            switch (stage[0]++) {
                case 0 -> {
                    check(container(helper, chest).getItem(0).isEmpty() && count(magneto, Items.IRON_INGOT) == 5, "the chest's 5 ingots are his");
                    check(container(helper, chest).getItem(1).is(Items.DIRT) && helper.getBlockState(chest).is(Blocks.CHEST), "its dirt and the chest stay");
                    check(frame.isAlive() && frame.getItem().isEmpty() && count(magneto, Items.IRON_SWORD) == 1, "the frame's sword is his; the frame stays");
                    check(stand.isAlive() && stand.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && count(magneto, Items.IRON_CHESTPLATE) == 1,
                            "the stand's chestplate is his; the stand stays");
                    check(container(helper, barrel).getItem(0).getCount() == 4, "the fisherman's barrel keeps its iron");
                    check(container(helper, hiddenChest).getItem(0).getCount() == 3, "the chest behind the wall keeps its iron");
                    check(fixedFrame.getItem().is(Items.IRON_SWORD), "the fixed frame keeps its sword");
                    check(lockedStand.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
                            "the locked stand keeps its chestplate");

                    // A claim on the armour stand's cell only: the section round it is anyone's, so only the stand's own
                    // break check can refuse it.
                    stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                    BlockPos standCell = stand.blockPosition();
                    Consumer<BlockEvent.BreakEvent> standClaim = event -> event.setCanceled(event.getPos().equals(standCell));
                    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, standClaim);
                    claims.add(standClaim);
                    loose[0] = ingot(helper, new Vec3(12.5, FLOOR, 12.5));
                    Powers.get(magneto).startCooldown(1, level.getGameTime(), 0);
                    packets.clear();
                    sneakR(magneto);
                    packets.addAll(TestKit.sentPackets(magneto));
                }
                case 1 -> {
                    check(stand.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE) && count(magneto, Items.IRON_CHESTPLATE) == 1,
                            "the armour stand on someone's claim keeps its chestplate");
                    check(!loose[0].isAlive() && count(magneto, Items.IRON_INGOT) == 6, "while the loose ingot beside it comes");
                    List<String> lines = actionBar(packets);
                    check(!lines.isEmpty() && "message.enhanced.pull.done_protected".equals(lines.get(lines.size() - 1)),
                            "and he reads that the rest is protected: " + lines);
                    stand.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                    claims.forEach(NeoForge.EVENT_BUS::unregister);

                    // A claim on one chest's cell.
                    helper.setBlock(claimedChest, Blocks.CHEST);
                    check(seen(helper, magneto, claimedChest.getX(), claimedChest.getY(), claimedChest.getZ()), "he sees the chest on the claim");
                    container(helper, claimedChest).setItem(0, new ItemStack(Items.IRON_INGOT, 2));
                    BlockPos claimed = helper.absolutePos(claimedChest);
                    Consumer<BlockEvent.BreakEvent> claim = event -> event.setCanceled(event.getPos().equals(claimed));
                    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, claim);
                    claims.add(claim);
                    Powers.get(magneto).startCooldown(1, level.getGameTime(), 0);
                    sneakR(magneto);
                }
                case 2 -> {
                    check(container(helper, claimedChest).getItem(0).getCount() == 2 && count(magneto, Items.IRON_INGOT) == 6,
                            "the chest on someone's claim keeps its iron");

                    // Adventure, no claim anywhere: what lies on the ground is anyone's; a frame or a stand is someone's build,
                    // as a chest is.
                    claims.forEach(NeoForge.EVENT_BUS::unregister);
                    frame.setItem(new ItemStack(Items.IRON_SWORD));
                    stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                    loose[0] = ingot(helper, new Vec3(12.5, FLOOR, 12.5));
                    adventurer[0] = f.player(10.5, FLOOR, 10.5, GameType.ADVENTURE);
                    Powers.setPower(adventurer[0], Power.MAGNETO);
                    TestKit.look(adventurer[0], TestKit.EAST, 0.0F);
                    sneakR(adventurer[0]);
                }
                default -> {
                    check(!loose[0].isAlive() && count(adventurer[0], Items.IRON_INGOT) == 1, "in Adventure the loose ingot comes");
                    check(frame.getItem().is(Items.IRON_SWORD) && count(adventurer[0], Items.IRON_SWORD) == 0, "but the frame keeps its sword");
                    check(stand.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE) && count(adventurer[0], Items.IRON_CHESTPLATE) == 0,
                            "and the stand its chestplate");
                    return true;
                }
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // What lies loose, creatures and players (M21, M22, M24, M33, M36)

    /**
     * M21. Loose metal and creatures' gear come from all round him: an ingot, a golden apple, the stuck arrow anyone may
     * pick up, his own stuck trident, and a zombie's iron helmet and sword are his. The diamond, the skeleton's kind of
     * arrow nobody may pick up, another player's stuck trident (it goes back only to him who threw it), the zombie's
     * leather boots, the iron armour of another player's horse and an armour stand's gold chestplate stay where they
     * are, and the villager is left alone.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void thePullTakesLooseMetalAndGear(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = magneto(f);
        ItemEntity ingot = ingot(helper, new Vec3(12.5, FLOOR, 8.5));
        ItemEntity apple = loose(helper, new Vec3(8.5, FLOOR, 12.5), new ItemStack(Items.GOLDEN_APPLE));
        ItemEntity diamond = loose(helper, new Vec3(12.5, FLOOR, 12.5), new ItemStack(Items.DIAMOND));
        Arrow anyones = stuck(helper, new Vec3(7.5, FLOOR, 8.5), AbstractArrow.Pickup.ALLOWED);
        Arrow nobodys = stuck(helper, new Vec3(13.5, FLOOR, 9.5), AbstractArrow.Pickup.DISALLOWED);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 13.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        ServerPlayer owner = f.player(19.5, FLOOR, 1.5);
        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, new Vec3(5.5, FLOOR, 10.5));
        horse.tameWithName(owner);
        horse.setBodyArmorItem(new ItemStack(Items.IRON_HORSE_ARMOR));
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(8.5, FLOOR, 6.5));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(12.5, FLOOR, 5.5));
        check(owner.distanceTo(magneto) > EnhancedConfig.PULL_RADIUS.get() + 2, "the horse's owner is out of reach");
        ThrownTrident his = stuckTrident(helper, new Vec3(14.5, FLOOR, 7.5), magneto);
        ThrownTrident others = stuckTrident(helper, new Vec3(9.5, FLOOR, 14.5), owner);
        check(his.pickup == AbstractArrow.Pickup.ALLOWED && others.pickup == AbstractArrow.Pickup.ALLOWED,
                "each trident may be picked up, as a player's throw leaves it");

        sneakR(magneto);
        afterThePull(f, magneto, 20, () -> {
            check(!ingot.isAlive() && count(magneto, Items.IRON_INGOT) == 1, "the ingot is his");
            check(!apple.isAlive() && count(magneto, Items.GOLDEN_APPLE) == 1, "the golden apple is his");
            check(!anyones.isAlive() && count(magneto, Items.ARROW) == 1, "one arrow is his: the one anyone may pick up");
            check(nobodys.isAlive(), "the arrow nobody may pick up stays stuck");
            check(!his.isAlive() && count(magneto, Items.TRIDENT) == 1, "his own stuck trident is his again");
            check(others.isAlive() && others.inGround, "another player's stuck trident stays where it is");
            check(count(magneto, Items.IRON_HELMET) == 1 && count(magneto, Items.IRON_SWORD) == 1, "the zombie's helmet and sword are his");
            check(zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && zombie.getMainHandItem().isEmpty()
                    && zombie.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS), "it keeps only its leather boots");
            check(diamond.isAlive() && count(magneto, Items.DIAMOND) == 0, "the diamond stays");
            check(horse.getBodyArmorItem().is(Items.IRON_HORSE_ARMOR) && count(magneto, Items.IRON_HORSE_ARMOR) == 0,
                    "another player's horse keeps its armour");
            check(stand.getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE), "the armour stand keeps its chestplate");
            check(villager.isAlive() && villager.getHealth() == villager.getMaxHealth(), "the villager is left alone");
        });
    }

    /**
     * M22. V at a zombie in an iron chestplate, then Sneak + R the next tick: the chestplate the strip marked for him
     * comes at once, through its pickup delay; an iron ingot another player threw a tick before the strip does not.
     */
    @GameTest(template = ARENA, batch = PULL)
    public static void theStripThenThePullAtOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        ServerPlayer other = f.player(18.5, FLOOR, 18.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        Vec3 at = helper.absoluteVec(new Vec3(8.5, FLOOR, 12.5));
        ItemEntity thrown = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.IRON_INGOT), 0.0, 0.0, 0.0);
        thrown.setPickUpDelay(40);
        thrown.setThrower(other);
        level.addFreshEntity(thrown);

        f.everyTick(20, tick -> {
            if (tick == 1) {
                TestKit.lookAt(magneto, zombie);
                AbilityHandler.onKey(magneto, 3, true);
                check(zombie.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "V tears the chestplate off");
                return false;
            }
            if (tick == 2) {
                sneakR(magneto);
                return false;
            }
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(count(magneto, Items.IRON_CHESTPLATE) == 1, "the chestplate the strip marked for him is his at once");
            check(thrown.isAlive() && thrown.hasPickUpDelay() && count(magneto, Items.IRON_INGOT) == 0,
                    "the ingot another player threw waits out its delay where it lies");
            return true;
        });
    }

    /**
     * M24. An Iron Golem without AI, 7 blocks away and alone in reach: the pull tugs it at least 2 blocks nearer, unhurt and
     * not set against him, and the tug costs (he rests, one hunger point, Sneak + R says recharging). Rested: an empty
     * minecart and a TNT minecart come as their items (the TNT without going off; the minecart with the name it was
     * given); a chest minecart with dirt in it, a minecart a zombie rides and the command block minecart stay; another
     * player's drone, with PvP off, is not tugged.
     */
    @GameTest(template = ARENA, batch = PULL, timeoutTicks = 240)
    public static void thePullTugsGolemsAndTakesEmptyCarts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        PowerData data = Powers.get(magneto);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(3.5, FLOOR, 10.5));
        double before = golem.distanceTo(magneto);
        double hunger = hunger(magneto);
        sneakR(magneto);
        int[] stage = {0};
        AbstractMinecart[] carts = new AbstractMinecart[5];
        UltronSentryEntity[] drone = new UltronSentryEntity[1];
        Vec3[] droneAt = new Vec3[1];

        f.everyTick(230, tick -> {
            PowerTicker.tick(magneto);
            long now = level.getGameTime();
            if (stage[0] == 0 && tick == 12) {
                check(golem.distanceTo(magneto) <= before - 2.0, "the golem slid at least 2 blocks nearer: " + before + " to " + golem.distanceTo(magneto));
                check(golem.getHealth() == golem.getMaxHealth() && golem.getTarget() != magneto, "unhurt, and not set against him");
                check(MagnetPull.lastTugs(magneto) == 1, "one body tugged");
                check(!data.isReady(1, now), "the tug was the pull's work: he rests");
                assertNear(hunger - hunger(magneto), EnhancedConfig.PULL_EXHAUSTION.get(), 0.001, "and it cost a hunger point");
                BossTestSupport.lastActionBarKey(magneto);
                sneakR(magneto);
                check("message.enhanced.pull.recharging".equals(BossTestSupport.lastActionBarKey(magneto)), "Sneak + R at once: recharging");
                golem.discard();
                stage[0] = 1;
            } else if (stage[0] == 1 && data.isReady(1, now)) {
                carts[0] = cart(helper, EntityType.MINECART, new Vec3(13.5, FLOOR, 8.5));
                carts[0].setCustomName(Component.literal("Rusty"));
                carts[1] = cart(helper, EntityType.TNT_MINECART, new Vec3(7.5, FLOOR, 13.5));
                MinecartChest chest = cart(helper, EntityType.CHEST_MINECART, new Vec3(13.5, FLOOR, 12.5));
                chest.setItem(0, new ItemStack(Items.DIRT, 5));
                carts[2] = chest;
                carts[3] = cart(helper, EntityType.MINECART, new Vec3(7.5, FLOOR, 7.5));
                Zombie rider = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(7.5, FLOOR, 7.5));
                check(rider.startRiding(carts[3], true), "a zombie rides a minecart");
                carts[4] = cart(helper, EntityType.COMMAND_BLOCK_MINECART, new Vec3(12.5, FLOOR, 5.5));
                ServerPlayer ultron = f.player(19.5, FLOOR, 1.5, Power.ULTRON);
                drone[0] = UltronSentryEntity.drone(level, helper.absoluteVec(new Vec3(10.5, FLOOR + 1.0, 15.5)), ultron.getUUID(), false, false);
                drone[0].setHeld(true);
                level.addFreshEntity(drone[0]);
                droneAt[0] = drone[0].position();
                sneakR(magneto);
                stage[0] = 2;
            } else if (stage[0] == 2 && !MagnetPull.isPulling(magneto)) {
                check(!carts[0].isAlive() && count(magneto, Items.MINECART) == 1, "the empty minecart is his, as its item");
                ItemStack named = magneto.getInventory().items.stream().filter(stack -> stack.is(Items.MINECART))
                        .findFirst().orElseThrow();
                check(Component.literal("Rusty").equals(named.get(DataComponents.CUSTOM_NAME)),
                        "and it keeps its name: " + named.getHoverName().getString());
                check(!carts[1].isAlive() && count(magneto, Items.TNT_MINECART) == 1, "the TNT minecart is his");
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        check(!helper.getBlockState(new BlockPos(7 + dx, 1, 13 + dz)).isAir(), "and it went off nowhere: the floor is whole");
                    }
                }
                check(carts[2].isAlive() && ((MinecartChest) carts[2]).getItem(0).is(Items.DIRT) && ((MinecartChest) carts[2]).getItem(0).getCount() == 5,
                        "the chest minecart with dirt in it stays, dirt and all");
                check(carts[3].isAlive() && carts[3].isVehicle(), "the minecart the zombie rides stays");
                check(carts[4].isAlive() && count(magneto, Items.COMMAND_BLOCK_MINECART) == 0, "the command block minecart stays");
                check(MagnetPull.lastTugs(magneto) == 0 && drone[0].position().distanceTo(droneAt[0]) < 0.2,
                        "another player's drone is not tugged with PvP off");
                return true;
            }
            return false;
        });
    }

    /**
     * M33 (R35). Players, PvP off: a pull leaves every one of them as he was. PvP on: player A's iron sword flies into
     * Magneto's inventory (a stream from A in the tick's payload) and A keeps his armour; the Vision is tugged toward him;
     * the Iron Spider V already stripped is left as he is; the teammate keeps his chestplate, and player B behind a stone
     * wall keeps everything. Within the grace a second pull leaves A his helmet; after it a third one takes it. V at A tears
     * his chestplate off, unmarked.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_pvp", timeoutTicks = 360)
    public static void thePullAndPlayers(GameTestHelper helper) {
        players(helper, false);
    }

    /**
     * M33 with {@code magneto.pullPlayerGearToFeet}: A's sword drops at his own feet, unmarked, and flies to nobody. Its
     * pickup delay is the 40 ticks of a torn-off piece, counted from the take: 35 ticks after it not even A can pick the
     * sword up; 41 ticks after it A takes it back.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_pvp_feet", timeoutTicks = 160)
    public static void thePullAndPlayersGearToFeet(GameTestHelper helper) {
        players(helper, true);
    }

    private static void players(GameTestHelper helper, boolean toFeet) {
        TestKit.Fixture f = TestKit.fixture(helper);
        if (toFeet) {
            f.config(EnhancedConfig.PULL_PLAYER_GEAR_TO_FEET, true);
        }
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        PowerData data = Powers.get(magneto);
        ServerPlayer a = f.player(14.5, FLOOR, 10.5);
        ironSet(a);
        a.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        ServerPlayer vision = f.player(6.5, FLOOR, 10.5, Power.VISION);
        ServerPlayer spider = f.player(10.5, FLOOR, 6.5, Power.SPIDER_MAN);
        Powers.setVariant(spider, Variants.get(Power.SPIDER_MAN, "iron_spider"));
        PowerTicker.tick(spider);
        check(SpiderServer.onStripped(spider) && SpiderServer.isStripped(spider), "V stripped the Iron Spider before");
        ServerPlayer mate = f.player(13.5, FLOOR, 13.5);
        mate.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        ServerPlayer b = f.player(10.5, FLOOR, 16.5);
        ironSet(b);
        for (int x = 9; x <= 11; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 14), Blocks.STONE);
            }
        }
        Scoreboard board = level.getScoreboard();
        PlayerTeam team = board.addPlayerTeam(toFeet ? "magnet_pull_mates_feet" : "magnet_pull_mates");
        team.setAllowFriendlyFire(false);
        board.addPlayerToTeam(magneto.getScoreboardName(), team);
        board.addPlayerToTeam(mate.getScoreboardName(), team);
        f.onClose(() -> board.removePlayerTeam(team));
        check(!Raycasts.inSight(magneto, b.getEyePosition()), "B stands behind the wall");
        for (ServerPlayer seen : List.of(a, vision, spider, mate)) {
            check(Raycasts.inSight(magneto, seen.getEyePosition()), seen.getScoreboardName() + " is in sight");
        }
        List<ServerPlayer> everyone = List.of(a, vision, spider, mate, b);
        List<List<ItemStack>> worn = new ArrayList<>();
        for (ServerPlayer player : everyone) {
            worn.add(wornBy(player));
        }
        TestKit.sentPackets(spider);
        List<Object> packets = new ArrayList<>();
        int[] stage = {0};
        long[] since = {0};
        ItemEntity[] sword = {null};
        sneakR(magneto);

        f.everyTick(toFeet ? 150 : 350, tick -> {
            PowerTicker.tick(magneto);
            packets.addAll(TestKit.sentPackets(magneto));
            long now = level.getGameTime();
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            switch (stage[0]) {
                case 0 -> {
                    for (int i = 0; i < everyone.size(); i++) {
                        check(sameStacks(wornBy(everyone.get(i)), worn.get(i)), "PvP off: " + everyone.get(i).getScoreboardName() + " keeps everything");
                    }
                    check(vision.getDeltaMovement().lengthSqr() < 1.0E-9, "PvP off: the Vision is not tugged");
                    check(!actionBar(TestKit.sentPackets(spider)).contains("message.enhanced.legs.stripped"), "nor the Spider stripped again");
                    check(data.isReady(1, now), "the pull did nothing, so it does not rest");
                    f.pvp(true);
                    since[0] = now;
                    stage[0] = 1;
                }
                case 1 -> {
                    if (now - since[0] > EnhancedConfig.PULL_RETRY_TICKS.get()) {
                        packets.clear();
                        sneakR(magneto);
                        stage[0] = 2;
                    }
                }
                case 2 -> {
                    check(a.getMainHandItem().isEmpty(), "PvP on: A's sword is torn from his hand");
                    for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                        check(!a.getItemBySlot(slot).isEmpty(), "one piece only: A keeps his " + slot.getName());
                    }
                    boolean streamed = payloads(packets).stream().flatMap(payload -> payload.streams().stream())
                            .anyMatch(stream -> stream.item() == Items.IRON_SWORD && stream.from().distanceTo(a.position().add(0, a.getBbHeight() * 0.5, 0)) < 1.5);
                    if (toFeet) {
                        List<ItemEntity> swords = helper.getEntities(EntityType.ITEM).stream().filter(item -> item.getItem().is(Items.IRON_SWORD)).toList();
                        check(swords.size() == 1 && swords.get(0).position().distanceTo(a.position()) <= 1.5, "the sword lies at A's feet");
                        check(swords.get(0).hasPickUpDelay() && !swords.get(0).getPersistentData().contains(MagnetoAbilities.STRIPPED_BY_TAG),
                                "unmarked, and waiting out its delay so that A can take it back");
                        check(count(magneto, Items.IRON_SWORD) == 0 && !streamed, "it flew to nobody");
                        sword[0] = swords.get(0);
                    } else {
                        check(count(magneto, Items.IRON_SWORD) == 1, "the sword is in Magneto's inventory");
                        check(streamed, "and flew to him from A, in the tick's payload");
                    }
                    Vec3 tug = vision.getDeltaMovement();
                    check(tug.length() > 0.3 && tug.dot(magneto.position().subtract(vision.position())) > 0.0, "the Vision is tugged toward him: " + tug);
                    check(!actionBar(TestKit.sentPackets(spider)).contains("message.enhanced.legs.stripped") && SpiderServer.isStripped(spider),
                            "the Spider, stripped already, is left as he is");
                    check(sameStacks(wornBy(mate), worn.get(3)), "the teammate keeps his chestplate");
                    check(sameStacks(wornBy(b), worn.get(4)), "B, behind the wall, keeps everything");
                    if (toFeet) {
                        // The take is when A's grace began: the pull may have gone on a tick or two after it.
                        since[0] = now - (EnhancedConfig.PULL_PLAYER_GRACE_TICKS.get() - MagnetPull.playerGraceLeft(a));
                        stage[0] = 7;
                        return false;
                    }
                    stage[0] = 3;
                }
                case 3 -> {
                    if (data.isReady(1, now)) {
                        sneakR(magneto);
                        stage[0] = 4;
                    }
                }
                case 4 -> {
                    check(a.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && MagnetPull.playerGraceLeft(a) > 0,
                            "rested, a second pull within the grace leaves A his helmet");
                    since[0] = now;
                    stage[0] = 5;
                }
                case 5 -> {
                    if (MagnetPull.playerGraceLeft(a) == 0 && data.isReady(1, now) && now - since[0] > EnhancedConfig.PULL_RETRY_TICKS.get()) {
                        sneakR(magneto);
                        stage[0] = 6;
                    }
                }
                case 7 -> {
                    if (now - since[0] >= 35) {
                        check(sword[0].hasPickUpDelay(), "35 ticks after the take the sword still waits out its delay");
                        sword[0].playerTouch(a);
                        check(!sword[0].isRemoved() && a.getInventory().countItem(Items.IRON_SWORD) == 0, "so not even A can pick it up yet");
                        stage[0] = 8;
                    }
                }
                case 8 -> {
                    if (now - since[0] >= 41) {
                        check(!sword[0].hasPickUpDelay(), "41 ticks after the take its 40 ticks are over");
                        sword[0].playerTouch(a);
                        check(sword[0].isRemoved() && a.getInventory().countItem(Items.IRON_SWORD) == 1, "and A takes his sword back");
                        return true;
                    }
                }
                default -> {
                    check(a.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && count(magneto, Items.IRON_HELMET) == 1,
                            "after the grace a third pull takes A's helmet into his inventory");
                    data.startCooldown(3, now, 0);
                    TestKit.lookAt(magneto, a);
                    AbilityHandler.onKey(magneto, 3, true);
                    List<ItemEntity> chests = helper.getEntities(EntityType.ITEM).stream().filter(item -> item.getItem().is(Items.IRON_CHESTPLATE)).toList();
                    check(a.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && chests.size() == 1, "V at A: his chestplate falls");
                    check(!chests.get(0).getPersistentData().contains(MagnetoAbilities.STRIPPED_BY_TAG), "unmarked: torn off a player");
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * M36. With {@code magneto.pullTakesGear} off and {@code magneto.pullTug} 0: a zombie keeps its iron helmet, an Iron
     * Golem does not move, and a dropped ingot still comes.
     */
    @GameTest(template = ARENA, batch = "magnet_pull_gear")
    public static void theGearSwitchAndTheTug(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.PULL_TAKES_GEAR, false);
        f.config(EnhancedConfig.PULL_TUG, 0.0);
        ServerPlayer magneto = magneto(f);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(13.5, FLOOR, 12.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(6.5, FLOOR, 13.5));
        ItemEntity ingot = ingot(helper, new Vec3(12.5, FLOOR, 8.5));
        Vec3 golemAt = golem.position();
        sneakR(magneto);
        long[] doneAt = {-1};
        f.everyTick(30, tick -> {
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            if (doneAt[0] < 0) {
                doneAt[0] = tick;
            }
            if (tick < doneAt[0] + 5) {
                return false; // a tugged golem would be on its way by now
            }
            check(zombie.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && count(magneto, Items.IRON_HELMET) == 0, "the zombie keeps its helmet");
            check(golem.position().distanceTo(golemAt) < 0.01 && MagnetPull.lastTugs(magneto) == 0, "the golem does not move");
            check(!ingot.isAlive() && count(magneto, Items.IRON_INGOT) == 1, "the dropped ingot still comes");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Piglins (M52)

    /** M52. A gold block pulled angers the piglins near him, as breaking it by hand does; an iron block does not. */
    @GameTest(template = ARENA, batch = "magnet_pull_piglin")
    public static void piglinsMindTheirGold(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        Piglin first = piglin(helper, new Vec3(10.5, FLOOR, 16.5));
        onPillar(helper, FAN[2], Blocks.GOLD_BLOCK.defaultBlockState());
        sneakR(magneto);
        Piglin[] second = new Piglin[1];
        f.everyTick(20, tick -> {
            PowerTicker.tick(magneto);
            if (tick == 3) {
                check(count(magneto, Items.GOLD_BLOCK) == 1, "the gold block is his");
                check(first.getBrain().getMemory(MemoryModuleType.ANGRY_AT).filter(magneto.getUUID()::equals).isPresent(), "the piglin is angry at him");
                second[0] = piglin(helper, new Vec3(4.5, FLOOR, 10.5));
                onPillar(helper, FAN[8], Blocks.IRON_BLOCK.defaultBlockState());
                Powers.get(magneto).startCooldown(1, level.getGameTime(), 0);
                sneakR(magneto);
            }
            if (tick == 6) {
                check(count(magneto, Items.IRON_BLOCK) == 1, "the iron block is his");
                check(second[0].getBrain().getMemory(MemoryModuleType.ANGRY_AT).isEmpty(), "and no piglin minds it");
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the others see and hear (M43)

    /**
     * M43. Everyone who sees Magneto sees his pull: a player 6 blocks from him is sent its payloads (each naming Magneto,
     * the first starting the field's pulse, and a stream of raw iron from the middle of each of the two ores) and hears
     * the pull begin and the ores break; a player 600 blocks away (farther than anyone is tracked) is sent none of it.
     */
    @GameTest(template = ARENA, batch = PULL, timeoutTicks = 200)
    public static void thePullIsSeenAndHeardNearby(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer magneto = magneto(f);
        ServerPlayer watcher = f.player(16.5, FLOOR, 10.5);
        ServerPlayer far = f.player(4.5, FLOOR, 4.5);
        Vec3 away = far.position().add(600.0, 0.0, 0.0);
        far.setNoGravity(true);
        far.moveTo(away.x, away.y, away.z, 0.0F, 0.0F);
        List<BlockPos> ores = List.of(new BlockPos(7, 1, 13), new BlockPos(13, 1, 7));
        for (BlockPos ore : ores) {
            helper.setBlock(ore, Blocks.IRON_ORE);
        }
        ChunkPos his = magneto.chunkPosition();
        List<Object> near = new ArrayList<>();
        List<Object> farAway = new ArrayList<>();
        boolean[] pressed = {false};
        f.everyTick(150, tick -> {
            if (!pressed[0]) {
                // A tick for the trackers first. A player is shown the entities of a chunk only once the chunk was sent to
                // him, and the server sends more chunks only as a client acknowledges the last ones: the fake one does here.
                watcher.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
                if (tick < 2 || !watcher.getChunkTrackingView().contains(his) || watcher.connection.chunkSender.isPending(his.toLong())) {
                    return false;
                }
                level.getChunkSource().move(watcher);
                TestKit.sentPackets(watcher);
                TestKit.sentPackets(far);
                sneakR(magneto);
                pressed[0] = true;
            } else {
                PowerTicker.tick(magneto);
            }
            near.addAll(TestKit.sentPackets(watcher));
            farAway.addAll(TestKit.sentPackets(far));
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            // (The Magnetos of the arenas next door may be in the watcher's sight too: only this one's payloads count.)
            List<MagnetPullPayload> sent = payloads(near).stream().filter(p -> p.magneto() == magneto.getId()).toList();
            check(!sent.isEmpty(), "the watcher is sent the pull, each payload naming Magneto");
            check(sent.get(0).first() && sent.stream().skip(1).noneMatch(MagnetPullPayload::first), "the first starts the field's pulse, and only the first");
            List<MagnetPullPayload.Stream> streams = sent.stream().flatMap(p -> p.streams().stream()).toList();
            check(streams.size() == 2, "two streams, not " + streams.size());
            for (BlockPos ore : ores) {
                Vec3 middle = Vec3.atCenterOf(helper.absolutePos(ore));
                check(streams.stream().anyMatch(s -> s.from().distanceTo(middle) < 1.0E-6), "a stream leaves the middle of the ore at " + ore);
            }
            check(streams.stream().allMatch(s -> s.item() == Items.RAW_IRON && s.count() == 1), "each one raw iron");
            check(TestKit.soundsIn(near).stream().anyMatch(sound -> sound.id().equals(Enhanced.id("magnet_pull"))), "the watcher hears the pull begin");
            for (BlockPos ore : ores) {
                check(near.stream().anyMatch(p -> p instanceof ClientboundLevelEventPacket event && event.getType() == 2001
                        && event.getPos().equals(helper.absolutePos(ore))), "and sees and hears the ore at " + ore + " break");
            }
            check(count(magneto, Items.RAW_IRON) == 2, "the raw iron is Magneto's");
            check(payloads(farAway).isEmpty(), "the player 600 blocks away is sent no payload");
            check(TestKit.soundsIn(farAway).stream().noneMatch(sound -> sound.id().equals(Enhanced.id("magnet_pull")))
                    && farAway.stream().noneMatch(p -> p instanceof ClientboundLevelEventPacket event && event.getType() == 2001),
                    "and hears nothing of it");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Magneto in the middle of the floor, looking east and level. */
    private static ServerPlayer magneto(TestKit.Fixture f) {
        ServerPlayer magneto = f.player(10.5, FLOOR, 10.5, Power.MAGNETO);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        return magneto;
    }

    /** Sneak + R with nothing held, pressed and let go, as his client sends it. */
    static void sneakR(ServerPlayer magneto) {
        magneto.setShiftKeyDown(true);
        AbilityHandler.onKey(magneto, 1, true);
        AbilityHandler.onKey(magneto, 1, false);
        magneto.setShiftKeyDown(false);
    }

    /** Ticks Magneto until his pull is over, then runs the asserts (within {@code limit} ticks). */
    private static void afterThePull(TestKit.Fixture f, ServerPlayer magneto, int limit, Runnable asserts) {
        f.everyTick(limit, tick -> {
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            asserts.run();
            return true;
        });
    }

    /** A block set on a stone pillar, one above the floor, at (x, z) = {@code at}. */
    private static void onPillar(GameTestHelper helper, int[] at, BlockState state) {
        helper.setBlock(new BlockPos(at[0], 2, at[1]), Blocks.STONE);
        helper.setBlock(new BlockPos(at[0], 3, at[1]), state);
    }

    /** Both halves of a door on a stone pillar, the lower half one above the floor. */
    private static void door(GameTestHelper helper, int[] at, Block door) {
        helper.setBlock(new BlockPos(at[0], 2, at[1]), Blocks.STONE);
        helper.setBlock(new BlockPos(at[0], 3, at[1]), door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(new BlockPos(at[0], 4, at[1]), door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static Container container(GameTestHelper helper, int[] at) {
        return container(helper, new BlockPos(at[0], 3, at[1]));
    }

    private static Container container(GameTestHelper helper, BlockPos relative) {
        return (Container) helper.getBlockEntity(relative);
    }

    /** Whether Magneto sees the middle of the cell (relative): the line from his eyes crosses no other cell's shape. */
    private static boolean seen(GameTestHelper helper, ServerPlayer magneto, int x, int y, int z) {
        return Raycasts.inSight(magneto, helper.absolutePos(new BlockPos(x, y, z)));
    }

    /** An iron ingot lying still on the floor, anyone's to pick up. */
    private static ItemEntity ingot(GameTestHelper helper, Vec3 relative) {
        Vec3 at = helper.absoluteVec(relative);
        ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.IRON_INGOT));
        item.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    /** A stack lying still on the floor, anyone's to pick up. */
    private static ItemEntity loose(GameTestHelper helper, Vec3 relative, ItemStack stack) {
        Vec3 at = helper.absoluteVec(relative);
        ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack, 0.0, 0.0, 0.0);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    /** An arrow stuck in the floor at {@code relative}, with its pickup rule. */
    private static Arrow stuck(GameTestHelper helper, Vec3 relative, AbstractArrow.Pickup pickup) {
        Vec3 at = helper.absoluteVec(relative.subtract(0, 0.05, 0));
        Arrow arrow = new Arrow(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.ARROW), null);
        arrow.setDeltaMovement(Vec3.ZERO);
        arrow.inGround = true;
        arrow.pickup = pickup;
        helper.getLevel().addFreshEntity(arrow);
        return arrow;
    }

    /** A trident stuck in the floor at {@code relative}, thrown by {@code thrower}. */
    private static ThrownTrident stuckTrident(GameTestHelper helper, Vec3 relative, ServerPlayer thrower) {
        Vec3 at = helper.absoluteVec(relative.subtract(0, 0.05, 0));
        ThrownTrident trident = new ThrownTrident(helper.getLevel(), thrower, new ItemStack(Items.TRIDENT));
        trident.moveTo(at.x, at.y, at.z);
        trident.setDeltaMovement(Vec3.ZERO);
        trident.inGround = true;
        helper.getLevel().addFreshEntity(trident);
        return trident;
    }

    /** A full set of iron armour on the player. */
    private static void ironSet(ServerPlayer player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }

    /** What the player holds and wears, slot by slot (copies). */
    private static List<ItemStack> wornBy(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            stacks.add(player.getItemBySlot(slot).copy());
        }
        return stacks;
    }

    private static boolean sameStacks(List<ItemStack> now, List<ItemStack> before) {
        for (int i = 0; i < now.size(); i++) {
            if (!ItemStack.matches(now.get(i), before.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** An item frame holding {@code stack}, facing west from the stone block east of {@code relative}. */
    private static ItemFrame frame(GameTestHelper helper, BlockPos relative, ItemStack stack) {
        helper.setBlock(relative.east(), Blocks.STONE);
        ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(relative), Direction.WEST);
        frame.setItem(stack);
        helper.getLevel().addFreshEntity(frame);
        return frame;
    }

    /** An armour stand at {@code relative} wearing {@code chest} on its chest. */
    private static ArmorStand stand(GameTestHelper helper, Vec3 relative, Item chest) {
        ArmorStand stand = new ArmorStand(helper.getLevel(), 0, 0, 0);
        stand.moveTo(helper.absoluteVec(relative));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        helper.getLevel().addFreshEntity(stand);
        return stand;
    }

    private static <T extends net.minecraft.world.entity.vehicle.AbstractMinecart> T cart(GameTestHelper helper, EntityType<T> type, Vec3 relative) {
        T cart = type.create(helper.getLevel());
        cart.moveTo(helper.absoluteVec(relative));
        helper.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static Piglin piglin(GameTestHelper helper, Vec3 relative) {
        Piglin piglin = helper.spawnWithNoFreeWill(EntityType.PIGLIN, relative);
        piglin.setImmuneToZombification(true);
        return piglin;
    }

    /** {@code n} floor cells round the middle (not under him), set with raw iron by the tests that count takes. */
    private static List<BlockPos> ring(int n) {
        List<BlockPos> cells = new ArrayList<>();
        for (int x = 5; x <= 15 && cells.size() < n; x += 2) {
            for (int z = 5; z <= 15 && cells.size() < n; z += 2) {
                if (x != 11 || z != 11) {
                    cells.add(new BlockPos(x, 1, z));
                }
            }
        }
        return cells;
    }

    /** How many of {@code item} lie on the ground within 1.5 blocks of the player's feet. */
    private static int atFeet(GameTestHelper helper, ServerPlayer player, Item item) {
        int n = 0;
        for (ItemEntity e : helper.getEntities(EntityType.ITEM)) {
            if (e.getItem().is(item) && e.position().distanceTo(player.position()) <= 1.5) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    private static long taken(GameTestHelper helper, List<BlockPos> cells) {
        return cells.stream().filter(cell -> helper.getBlockState(cell).isAir()).count();
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    private static boolean between(int value, int low, int high) {
        return value >= low && value <= high;
    }

    /** Whether the stacks are only {@code item}, low to high of it in all. */
    private static boolean only(List<ItemStack> stacks, Item item, int low, int high) {
        int total = 0;
        for (ItemStack stack : stacks) {
            if (!stack.is(item)) {
                return false;
            }
            total += stack.getCount();
        }
        return between(total, low, high);
    }

    /** What he has left of his hunger: food and saturation, less the exhaustion still to pay. */
    private static double hunger(ServerPlayer player) {
        FoodData food = player.getFoodData();
        return 4.0 * food.getFoodLevel() + 4.0 * food.getSaturationLevel() - food.getExhaustionLevel();
    }

    /** The keys of the action-bar lines among the packets, oldest first. */
    private static List<String> actionBar(List<Object> packets) {
        List<String> keys = new ArrayList<>();
        for (Object packet : packets) {
            if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay() && chat.content().getContents() instanceof TranslatableContents line) {
                keys.add(line.getKey());
            }
        }
        return keys;
    }

    /** The pull's payloads among the packets (bundled ones too), oldest first. */
    private static List<MagnetPullPayload> payloads(List<Object> packets) {
        List<MagnetPullPayload> found = new ArrayList<>();
        for (Object packet : packets) {
            collect(packet, found);
        }
        return found;
    }

    private static void collect(Object packet, List<MagnetPullPayload> found) {
        if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof MagnetPullPayload payload) {
            found.add(payload);
        } else if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> inner : bundle.subPackets()) {
                collect(inner, found);
            }
        }
    }

    private MagnetPullTests() {
    }
}
