package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.multiverse.Multiverse;
import com.kolodkingal.enhanced.multiverse.MultiverseBridge;
import com.kolodkingal.enhanced.multiverse.UniverseChunkGenerator;
import com.kolodkingal.enhanced.multiverse.UniverseLedger;
import com.kolodkingal.enhanced.multiverse.UniverseStones;
import com.kolodkingal.enhanced.multiverse.UniverseRecipe;
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
        Object[][] files = {{Multiverse.XMEN, "universe_10005"}, {Multiverse.ZOMBIES, "universe_2149"}, {0, "what_if"},
                {0, "what_if_amplified"}, {0, "what_if_islands"}, {0, "what_if_caves"}, {Multiverse.GAMMA, "what_if_gamma"},
                {-1, "what_if_hydra"}, {-2, "what_if_ultron"}};
        for (Object[] entry : files) {
            int universe = (Integer) entry[0];
            String file = (String) entry[1];
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
            if (universe < 0) {
                String city = universe == -1 ? "hydra" : "ultron";
                check(stem.generator() instanceof com.kolodkingal.enhanced.multiverse.CityChunkGenerator g && g.city().equals(city),
                        "the dimension " + file + " reads as the city generator of " + city);
                continue;
            }
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
        helper.succeed();
    }

    /** (3.19.1) A random universe's number is its seed: the same number, the same world; many numbers, many worlds. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aUniversesNumberIsTheSeedOfItsWorld(GameTestHelper helper) {
        Set<Integer> shapes = new HashSet<>();
        Set<String> stones = new HashSet<>();
        int fogs = 0;
        int kept = 0;
        for (int n = 1; n <= 400; n++) {
            UniverseRecipe recipe = UniverseRecipe.of(n);
            shapes.add(recipe.shape());
            if (recipe.stone() != null) {
                stones.add(recipe.stone().getBlock().getDescriptionId());
            } else {
                kept++;
            }
            fogs += recipe.fog() >= 0 ? 1 : 0;
            check(recipe.title() != null, "every universe has a title");
            check(Multiverse.levelOf(helper.getLevel().getServer(), n) == helper.getLevel().getServer().getLevel(Multiverse.SHAPES.get(recipe.shape())),
                    "a random universe is in the world of its shape");
        }
        check(shapes.size() == 4, "every shape of land is somewhere: " + shapes);
        check(stones.size() >= 15 && kept > 20, "many stones, and some worlds of plain stone: " + stones.size() + ", " + kept);
        check(fogs > 150 && fogs < 400, "most skies have a colour of their own: " + fogs);
        int carter = Multiverse.named("Captain Carter");
        UniverseRecipe first = UniverseRecipe.of(carter);
        check(first.shape() == UniverseRecipe.of(Multiverse.named("captain carter")).shape()
                && first.stone() == UniverseRecipe.of(Multiverse.named("CAPTAIN CARTER")).stone()
                && first.fog() == UniverseRecipe.of(carter).fog(), "the same words make the same world");
        check(UniverseRecipe.of(616).creature(net.minecraft.world.entity.EntityType.ARMOR_STAND) == net.minecraft.world.entity.EntityType.ARMOR_STAND,
                "a kind the recipes do not know stays itself");
        int swapped = 0;
        for (int n = 1; n <= 100; n++) {
            swapped += UniverseRecipe.of(n).creature(net.minecraft.world.entity.EntityType.COW) != net.minecraft.world.entity.EntityType.COW ? 1 : 0;
        }
        check(swapped > 10 && swapped < 90, "in some universes the cows come as something else: " + swapped);
        check(Multiverse.isThemed(Multiverse.HYDRA) && !Multiverse.isWhatIf(Multiverse.ULTRON) && !Multiverse.isWhatIf(Multiverse.GAMMA),
                "the series' three are not random universes");
        check(Multiverse.universeAt(Multiverse.HYDRA_KEY, 5, 5) == Multiverse.HYDRA && Multiverse.universeAt(Multiverse.GAMMA_KEY, 0, 0) == Multiverse.GAMMA
                && Multiverse.universeAt(Multiverse.ULTRON_KEY, 0, 0) == Multiverse.ULTRON, "each of them is its own world");
        check(Multiverse.universeAt(Multiverse.CAVES_KEY, 0.5, 0.5) == Multiverse.universeAt(Multiverse.WHAT_IF_KEY, 0.5, 0.5),
                "the four shapes share their cells' numbers");
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
                {Multiverse.WHAT_IF_KEY, "what_if"}, {Multiverse.AMPLIFIED_KEY, "what_if_amplified"},
                {Multiverse.ISLANDS_KEY, "what_if_islands"}, {Multiverse.CAVES_KEY, "what_if_caves"},
                {Multiverse.GAMMA_KEY, "what_if_gamma"}, {Multiverse.HYDRA_KEY, "what_if_hydra"}, {Multiverse.ULTRON_KEY, "what_if_ultron"}};
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
        for (int universe : new int[]{Multiverse.XMEN, Multiverse.ZOMBIES, 48213, Multiverse.named("Captain Carter"), 777,
                Multiverse.GAMMA, Multiverse.HYDRA, Multiverse.ULTRON, firstOfShape(1), firstOfShape(2), firstOfShape(3)}) {
            long start = System.nanoTime();
            com.kolodkingal.enhanced.stones.SpaceRealms.Arrival arrival = com.kolodkingal.enhanced.multiverse.UniverseTravel.arrival(player, universe);
            long ms = (System.nanoTime() - start) / 1_000_000L;
            com.kolodkingal.enhanced.Enhanced.LOGGER.info("Multiverse arrival in {}: {} ms at {}", universe, ms, arrival == null ? null : arrival.spot());
            check(arrival != null, "there is ground to arrive on in " + universe);
            check(Multiverse.universeAt(arrival.level(), BlockPos.containing(arrival.spot())) == universe, "and it is in " + universe);
        }
        helper.succeed();
    }

    /** The first random universe whose land has the shape (0 the Overworld's ... 3 caves). */
    static int firstOfShape(int shape) {
        for (int n = 1; ; n++) {
            if (UniverseRecipe.of(n).shape() == shape && Multiverse.isWhatIf(n)) {
                return n;
            }
        }
    }

    /** (3.19.1) A random universe's land is made of its own stone, ground and seas, and none of the game's stone is left. */
    @EnhancedTest(template = ARENA, batch = "multiverse_land", timeoutTicks = 2400)
    public static void aRandomUniversesLandIsMadeOfItsOwnStone(GameTestHelper helper) {
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.getLevel(Multiverse.WHAT_IF_KEY);
        if (level == null) {
            level = AsgardTestSupport.makeLevel(server, Multiverse.WHAT_IF_KEY, "/data/enhanced/dimension/what_if.json");
        }
        int n = 1;
        while (UniverseRecipe.of(n).shape() != 0 || UniverseRecipe.of(n).stone() == null || !Multiverse.isWhatIf(n)) {
            n++;
        }
        UniverseRecipe recipe = UniverseRecipe.of(n);
        BlockPos centre = Multiverse.centreOf(n);
        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
        int stone = 0;
        int theirs = 0;
        for (net.minecraft.world.level.chunk.LevelChunkSection section : chunk.getSections()) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        net.minecraft.world.level.block.state.BlockState state = section.getBlockState(x, y, z);
                        stone += state.is(net.minecraft.world.level.block.Blocks.STONE) ? 1 : 0;
                        theirs += state == recipe.stone() ? 1 : 0;
                    }
                }
            }
        }
        check(stone == 0, "no stone is left in universe " + n + ": " + stone);
        check(theirs > 1000, "its land is made of " + recipe.stone() + ": " + theirs);
        helper.succeed();
    }

    /** (3.19.1) Hydra's city: streets, the Citadel at the origin, and the free city once both rulers have fallen. */
    @EnhancedTest(template = ARENA, batch = "multiverse_land", timeoutTicks = 2400)
    public static void hydrasCityFallsWithItsRulers(GameTestHelper helper) {
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        ServerLevel city = server.getLevel(Multiverse.HYDRA_KEY);
        if (city == null) {
            city = AsgardTestSupport.makeLevel(server, Multiverse.HYDRA_KEY, "/data/enhanced/dimension/what_if_hydra.json");
        }
        com.kolodkingal.enhanced.multiverse.HydraCity.State state = com.kolodkingal.enhanced.multiverse.HydraCity.state(server);
        state.reset();
        int ground = com.kolodkingal.enhanced.multiverse.CityChunkGenerator.GROUND;
        net.minecraft.world.level.chunk.LevelChunk citadel = city.getChunk(1, 1);
        check(city.getBlockState(new BlockPos(3, ground, 100)).is(net.minecraft.world.level.block.Blocks.BLACK_CONCRETE)
                || city.getBlockState(new BlockPos(3, ground, 100)).is(net.minecraft.world.level.block.Blocks.YELLOW_CONCRETE), "a street");
        check(city.getBlockState(new BlockPos(27, ground + com.kolodkingal.enhanced.multiverse.CityChunkGenerator.CITADEL_HEIGHT, 27))
                .is(net.minecraft.world.level.block.Blocks.BLACK_CONCRETE), "the Citadel's black roof, high over the city");
        boolean red = false;
        for (int y = ground + 1; y < ground + 40 && !red; y++) {
            for (int x = 16; x < 32 && !red; x++) {
                red = city.getBlockState(new BlockPos(x, y, 11)).is(net.minecraft.world.level.block.Blocks.RED_CONCRETE);
            }
        }
        check(red, "Hydra's red stripes down the Citadel");
        check(city.getBlockState(com.kolodkingal.enhanced.multiverse.CityChunkGenerator.CITADEL_DOOR.above()).isAir(), "the way in is open");
        // Both rulers fall: the city is free, its soldiers are not let in, and Hydra's colours turn.
        for (int kind : new int[]{com.kolodkingal.enhanced.hydra.HydraBossEntity.RED_SKULL, com.kolodkingal.enhanced.hydra.HydraBossEntity.CAPTAIN_HYDRA}) {
            com.kolodkingal.enhanced.hydra.HydraBossEntity boss = com.kolodkingal.enhanced.hydra.HydraBossEntity.make(city, kind);
            check(boss.getMaxHealth() >= 300.0F, "a ruler is a boss: " + boss.getMaxHealth());
            boss.moveTo(27.5, ground + 1, 9.5, 0.0F, 0.0F);
            com.kolodkingal.enhanced.multiverse.HydraCity.bossDown(city, boss);
            check(state.down(kind), "the fall is written down");
        }
        check(state.freed() && com.kolodkingal.enhanced.multiverse.CityChunkGenerator.hydraFreed, "the city is free");
        com.kolodkingal.enhanced.multiverse.HydraCity.turnChunk(city, citadel);
        boolean stillRed = false;
        for (int y = ground; y < ground + 100; y++) {
            for (int x = 16; x < 32; x++) {
                for (int z = 16; z < 32; z++) {
                    stillRed |= citadel.getBlockState(new BlockPos(x, y, z)).is(net.minecraft.world.level.block.Blocks.RED_CONCRETE);
                }
            }
        }
        check(!stillRed, "no Hydra red is left in a turned chunk");
        state.reset();
        helper.succeed();
    }

    /** (3.19.1) The Gamma Bomb's world: every creature a Hulk of its kind, once. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theGammaBombMakesHulksOfEveryone(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Cow cow = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(4, (int) TestKit.FLOOR, 4));
        float health = cow.getMaxHealth();
        com.kolodkingal.enhanced.multiverse.GammaWorld.hulkify(cow);
        check(com.kolodkingal.enhanced.multiverse.GammaWorld.isHulk(cow), "the cow is a Hulk");
        double scale = cow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        check(scale >= 1.45 && scale <= 2.25, "bigger: " + scale);
        check(cow.getMaxHealth() >= health * 3.9F && cow.getHealth() == cow.getMaxHealth(), "four times as tough, and well");
        com.kolodkingal.enhanced.multiverse.GammaWorld.hulkify(cow);
        check(Math.abs(cow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) - scale) < 1.0E-6, "only once");
        helper.succeed();
    }

    /** (3.19.1) What Red Skull and Captain Hydra drop makes their user them: characters of their own. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRulersDropsMakeTheirUserThem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        net.minecraft.server.level.ServerPlayer player = f.player(6.5, TestKit.FLOOR, 6.5, com.kolodkingal.enhanced.power.Power.NONE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(com.kolodkingal.enhanced.registry.ModItems.RED_SKULL_SERUM.get()));
        com.kolodkingal.enhanced.registry.ModItems.RED_SKULL_SERUM.get().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        check(com.kolodkingal.enhanced.power.Powers.is(player, com.kolodkingal.enhanced.power.Power.CAPTAIN_AMERICA)
                && "red_skull".equals(com.kolodkingal.enhanced.power.Powers.get(player).variant()), "the serum makes him Red Skull");
        check(com.kolodkingal.enhanced.power.Characters.is(com.kolodkingal.enhanced.power.Powers.get(player),
                com.kolodkingal.enhanced.power.Characters.RED_SKULL), "a character of his own");
        com.kolodkingal.enhanced.power.Powers.cycleVariant(player, 1);
        check("red_skull".equals(com.kolodkingal.enhanced.power.Powers.get(player).variant()), "Change Look keeps him Red Skull");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(com.kolodkingal.enhanced.registry.ModItems.KOBIK_SHARD.get()));
        com.kolodkingal.enhanced.registry.ModItems.KOBIK_SHARD.get().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        check("captain_hydra".equals(com.kolodkingal.enhanced.power.Powers.get(player).variant()), "the shard of Kobik makes him Captain Hydra");
        helper.succeed();
    }
}
