package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.multiverse.Multiverse;
import com.kolodkingal.enhanced.multiverse.MultiverseBridge;
import com.kolodkingal.enhanced.multiverse.UniverseChunkGenerator;
import com.kolodkingal.enhanced.multiverse.UniverseLedger;
import com.kolodkingal.enhanced.multiverse.UniverseStones;
import com.kolodkingal.enhanced.multiverse.WhatIf;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The multiverse (3.19, {@code Multiverse}): the universes' worlds are there with their own land, the What If numbers
 * and cells are one to one, words always name the same universe, a stone of another universe carries its universe and
 * works only there, and the bridge's ring is a whole circle with the core at its foot.
 */
public final class MultiverseTests {
    private static final String BATCH = "multiverse";

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theUniversesAreThereWithTheirOwnLand(GameTestHelper helper) {
        ServerLevel any = helper.getLevel();
        // The GameTest server's world is built from the flat preset, with the game's three dimensions alone: the
        // universes' worlds are checked where they are loaded, as on a real server, and their files are read below.
        if (any.getServer().getLevel(Multiverse.XMEN_KEY) != null) {
            for (int universe : new int[]{Multiverse.MAIN, Multiverse.XMEN, Multiverse.ZOMBIES, 12345}) {
                check(Multiverse.levelOf(any.getServer(), universe) != null, "universe " + universe + " has a world");
            }
            ServerLevel xmen = any.getServer().getLevel(Multiverse.XMEN_KEY);
            ServerLevel zombies = any.getServer().getLevel(Multiverse.ZOMBIES_KEY);
            check(xmen.getChunkSource().getGenerator() instanceof UniverseChunkGenerator g && g.universe() == Multiverse.XMEN,
                    "10005's land is the universe generator's, numbered 10005");
            check(zombies.getChunkSource().getGenerator() instanceof UniverseChunkGenerator g && g.universe() == Multiverse.ZOMBIES,
                    "2149's too");
        }
        check(Multiverse.levelOf(any.getServer(), Multiverse.MAIN) == any.getServer().overworld(), "616 is the Overworld");
        // The universes' dimension files are read as the game reads them for a real world.
        for (int universe : new int[]{Multiverse.XMEN, Multiverse.ZOMBIES, 0}) {
            String file = universe == Multiverse.XMEN ? "universe_10005" : universe == Multiverse.ZOMBIES ? "universe_2149" : "what_if";
            net.minecraft.server.packs.resources.Resource resource = any.getServer().getResourceManager()
                    .getResource(com.kolodkingal.enhanced.Enhanced.id("dimension/" + file + ".json")).orElse(null);
            check(resource != null, "the dimension " + file + " is in the mod's data");
            com.google.gson.JsonElement json;
            try (java.io.Reader reader = resource.openAsReader()) {
                json = com.google.gson.JsonParser.parseReader(reader);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            net.minecraft.world.level.dimension.LevelStem stem = net.minecraft.world.level.dimension.LevelStem.CODEC
                    .parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, any.getServer().registryAccess()), json)
                    .getOrThrow();
            check(stem.generator() instanceof UniverseChunkGenerator g && g.universe() == universe,
                    "the dimension " + file + " reads as the universe generator numbered " + universe);
        }
        check(Multiverse.universeAt(Level.OVERWORLD, 0, 0) == Multiverse.MAIN && Multiverse.universeAt(Level.NETHER, 5, 5) == Multiverse.MAIN,
                "the Overworld and the Nether are 616");
        check(Multiverse.universeAt(Multiverse.XMEN_KEY, 0, 0) == Multiverse.XMEN, "10005 is 10005");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void whatIfNumbersAndCellsAreOneToOne(GameTestHelper helper) {
        Set<Long> cells = new HashSet<>();
        for (int n : new int[]{1, 2, 3, 615, 617, 999, 4242, 48213, 99999, 123456, Multiverse.CELLS - 1, Multiverse.CELLS}) {
            int[] cell = Multiverse.cellOf(n);
            check(Math.abs(cell[0]) <= Multiverse.HALF && Math.abs(cell[1]) <= Multiverse.HALF, "cell of " + n + " inside the border");
            check(Multiverse.numberOfCell(cell[0], cell[1]) == n, "the cell of " + n + " is numbered " + n);
            BlockPos centre = Multiverse.centreOf(n);
            check(Multiverse.universeAt(Multiverse.WHAT_IF_KEY, centre.getX() + 0.5, centre.getZ() + 0.5) == n,
                    "standing in the middle of " + n + " is being in " + n);
            check(Multiverse.universeAt(Multiverse.WHAT_IF_KEY, centre.getX() + Multiverse.CELL / 2.0 - 10, centre.getZ()) == n,
                    "and near its edge too");
            check(cells.add(((long) cell[0] << 32) ^ (cell[1] & 0xFFFFFFFFL)), "every number has a cell of its own");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void wordsAlwaysNameTheSameUniverse(GameTestHelper helper) {
        check(Multiverse.named("616") == Multiverse.MAIN && Multiverse.named("Earth-10005") == Multiverse.XMEN
                && Multiverse.named(" 2149 ") == Multiverse.ZOMBIES, "616, 10005 and 2149 name themselves");
        check(Multiverse.named("48213") == 48213, "a number names its What If universe");
        int carter = Multiverse.named("Captain Carter");
        check(carter == Multiverse.named("captain carter") && Multiverse.isWhatIf(carter), "words name one What If universe, whatever their case");
        check(Multiverse.named("Captain Carter") != Multiverse.named("Party Thor"), "other words, another universe");
        for (long n : new long[]{616, 10005, 2149, Multiverse.CELLS + 616L, -5, Long.MAX_VALUE}) {
            int folded = Multiverse.fold(n);
            check(Multiverse.isWhatIf(folded) && folded >= 1 && folded <= Multiverse.CELLS + 1, "fold " + n + " is a What If universe");
        }
        check(WhatIf.of(616) == WhatIf.NOTHING, "the main universe has no twist");
        Set<WhatIf> seen = new HashSet<>();
        for (int n = 1; n < 200; n++) {
            seen.add(WhatIf.of(n));
        }
        check(seen.size() == WhatIf.values().length, "every twist is somewhere");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aStoneOfAnotherUniverseIsOneAndWorksOnlyThere(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        UniverseLedger ledger = UniverseLedger.get(helper.getLevel().getServer());
        ledger.reset();
        ItemStack first = UniverseStones.make(StoneKind.POWER, Multiverse.ZOMBIES);
        ItemStack second = UniverseStones.make(StoneKind.POWER, Multiverse.ZOMBIES);
        check(UniverseStones.origin(first) == Multiverse.ZOMBIES, "the stone says it is from 2149");
        check(UniverseStones.origin(new ItemStack(first.getItem())) == Multiverse.MAIN, "a stone that says nothing is from 616");
        net.minecraft.server.level.ServerPlayer player = f.player(5.5, TestKit.FLOOR, 5.5, com.kolodkingal.enhanced.power.Power.NONE);
        player.getInventory().setItem(0, first);
        UniverseStones.check(player);
        check(ledger.finder(Multiverse.ZOMBIES, StoneKind.POWER) != null, "the first copy taken is the stone");
        player.getInventory().setItem(1, second);
        UniverseStones.check(player);
        check(player.getInventory().getItem(1).isEmpty() && !player.getInventory().getItem(0).isEmpty(),
                "a second copy fades away, the first stays");
        check(UniverseStones.refusal(player, StoneKind.POWER, Multiverse.ZOMBIES) != null, "2149's stone does nothing here");
        check(UniverseStones.refusal(player, StoneKind.POWER, Multiverse.MAIN) == null, "616's stone works here");
        ledger.reset();
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBridgesRingIsAWholeCircle(GameTestHelper helper) {
        BlockPos core = new BlockPos(100, 70, 100);
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST}) {
            List<BlockPos> ring = MultiverseBridge.ring(core, facing);
            check(ring.get(0).equals(core), "the core is the ring's foot");
            check(ring.contains(core.above(2 * MultiverseBridge.RADIUS)), "the ring reaches up to its top");
            check(new HashSet<>(ring).size() == ring.size() && ring.size() >= 28, "a whole ring of blocks: " + ring.size());
            for (BlockPos pos : ring) {
                check(facing.getAxis() == Direction.Axis.Z ? pos.getZ() == core.getZ() : pos.getX() == core.getX(),
                        "the ring stands across the way it faces");
            }
        }
        helper.succeed();
    }

    /** The universes' land is made as a real world makes it: chunks round the middle generate in each, in good time. */
    @EnhancedTest(template = ARENA, batch = "multiverse_land", timeoutTicks = 2400)
    public static void theUniversesLandGeneratesInGoodTime(GameTestHelper helper) {
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        Object[][] universes = {{Multiverse.XMEN_KEY, "universe_10005"}, {Multiverse.ZOMBIES_KEY, "universe_2149"},
                {Multiverse.WHAT_IF_KEY, "what_if"}};
        for (Object[] u : universes) {
            @SuppressWarnings("unchecked")
            net.minecraft.resources.ResourceKey<Level> key = (net.minecraft.resources.ResourceKey<Level>) u[0];
            ServerLevel level = server.getLevel(key);
            if (level == null) {
                level = AsgardTestSupport.makeLevel(server, key, "/data/enhanced/dimension/" + u[1] + ".json");
            }
            long start = System.nanoTime();
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    level.getChunk(dx, dz);
                }
            }
            long ms = (System.nanoTime() - start) / 1_000_000L;
            com.kolodkingal.enhanced.Enhanced.LOGGER.info("Multiverse land: 81 chunks of {} in {} ms", u[1], ms);
            check(ms < 60_000, "81 chunks of " + u[1] + " generate within a minute: " + ms + " ms");
        }
        // Arriving finds ground without making a great many chunks (the noise says where the land is).
        TestKit.Fixture f = TestKit.fixture(helper);
        net.minecraft.server.level.ServerPlayer player = f.player(5.5, TestKit.FLOOR, 5.5, com.kolodkingal.enhanced.power.Power.NONE);
        for (int universe : new int[]{Multiverse.XMEN, Multiverse.ZOMBIES, 48213, Multiverse.named("Captain Carter"), 777}) {
            long start = System.nanoTime();
            com.kolodkingal.enhanced.stones.SpaceRealms.Arrival arrival = com.kolodkingal.enhanced.multiverse.UniverseTravel.arrival(player, universe);
            long ms = (System.nanoTime() - start) / 1_000_000L;
            com.kolodkingal.enhanced.Enhanced.LOGGER.info("Multiverse arrival in {}: {} ms at {}", universe, ms, arrival == null ? null : arrival.spot());
            check(arrival != null, "there is ground to arrive on in " + universe);
            check(Multiverse.universeAt(arrival.level(), BlockPos.containing(arrival.spot())) == universe, "and it is in " + universe);
        }
        helper.succeed();
    }
}
