package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.block.TesseractCradleBlock;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.finders.LocatorRoutes;
import com.kolodkingal.enhanced.finders.LocatorState;
import com.kolodkingal.enhanced.finders.SiteSearch;
import com.kolodkingal.enhanced.finders.SpentSites;
import com.kolodkingal.enhanced.finders.StoneLocator;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Stone Locator (3.7, lane L5; PLAN 8 and 13): the recipe gives the core back byte for byte and tunes the Locator,
 * a vessel is a core, a Titan's or empty Gauntlet is not; what counts as "got" (carried, the ledger in player and world
 * modes); the filter's cycle; the search (stand-in structure starts in the flat test world: found within the budget
 * of 2 loads a tick, a spent site skipped, "too far"); the three spent hooks; the answers realm by realm (the Soul
 * Stone spins in the Overworld, the Power Stone points to a stronghold, the Reality Stone to a Bifrost site and, in
 * Asgard, to the vault's pedestal); the component written only on change; the spark trail's cooldown.
 */
public final class LocatorTests {
    static final String BATCH = "locator";
    /** The searches (server-wide queue and probe source: alone). */
    static final String SEARCH = "locator_search";
    /** The ledger's world mode (a config change: alone). */
    static final String WORLD = "locator_world";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** The Locator recipe's grid with {@code core} in the centre. */
    static CraftingInput grid(ItemStack core) {
        ItemStack n = new ItemStack(Items.NETHERITE_BLOCK);
        ItemStack d = new ItemStack(Items.DIAMOND_BLOCK);
        ItemStack k = new ItemStack(Items.WITHER_SKELETON_SKULL);
        return CraftingInput.of(3, 3, List.of(n.copy(), k, n.copy(), d.copy(), core, d.copy(), n.copy(), d.copy(),
                n.copy()));
    }

    static CraftingRecipe recipe(ServerLevel level) {
        Optional<RecipeHolder<?>> holder = level.getServer().getRecipeManager().byKey(Enhanced.id("stone_locator"));
        check(holder.isPresent() && holder.get().value() instanceof CraftingRecipe, "the Locator's recipe loads");
        return (CraftingRecipe) holder.get().value();
    }

    /** The same stack, byte for byte (the full saved form). */
    static boolean same(ServerLevel level, ItemStack a, ItemStack b) {
        Tag ta = a.save(level.registryAccess());
        Tag tb = b.save(level.registryAccess());
        return ta.equals(tb);
    }

    static ItemStack locator(ServerPlayer player) {
        ItemStack stack = new ItemStack(ModItems.STONE_LOCATOR.get());
        stack.set(ModDataComponents.LOCATOR.get(), LocatorState.tunedTo(StoneKind.TIME));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player.getMainHandItem();
    }

    static ItemStack withFilter(ItemStack stack, StoneKind kind) {
        stack.set(ModDataComponents.LOCATOR.get(), StoneLocator.state(stack).withFilter(Optional.of(kind)));
        return stack;
    }

    static List<String> actionBarKeys(List<Object> packets) {
        List<String> keys = new ArrayList<>();
        for (Object packet : packets) {
            if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay()
                    && chat.content().getContents() instanceof TranslatableContents t) {
                keys.add(t.getKey());
            }
        }
        return keys;
    }

    static <T> List<T> payloads(List<Object> packets, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Object packet : packets) {
            List<Object> inner = new ArrayList<>();
            if (packet instanceof ClientboundBundlePacket bundle) {
                for (Packet<?> p : bundle.subPackets()) {
                    inner.add(p);
                }
            } else {
                inner.add(packet);
            }
            for (Object p : inner) {
                if (p instanceof ClientboundCustomPayloadPacket custom && type.isInstance(custom.payload())) {
                    found.add(type.cast(custom.payload()));
                }
            }
        }
        return found;
    }

    /**
     * A stand-in placement grid: cells of {@code spacing} chunks, each cell's potential chunk at (3, 3) in it; a start
     * where {@code starts} says; every check needs a load (counted per game tick).
     */
    static final class FakeGrid implements SiteSearch.Probe {
        final int spacing;
        final Set<Long> starts = new HashSet<>();
        final Map<Long, Integer> loadsPerTick = new HashMap<>();
        final ServerLevel level;
        int loads;

        FakeGrid(ServerLevel level, int spacing) {
            this.level = level;
            this.spacing = spacing;
        }

        @Override
        public int spacing() {
            return spacing;
        }

        @Override
        public ChunkPos potential(int cellX, int cellZ) {
            return new ChunkPos(cellX * spacing + 3, cellZ * spacing + 3);
        }

        @Override
        public SiteSearch.Check check(ChunkPos chunk) {
            return SiteSearch.Check.LOAD;
        }

        @Override
        public boolean load(ChunkPos chunk) {
            loads++;
            loadsPerTick.merge(level.getGameTime(), 1, Integer::sum);
            return starts.contains(chunk.toLong());
        }

        @Override
        public BlockPos locate(ChunkPos chunk) {
            return chunk.getMiddleBlockPosition(64);
        }
    }

    /** A stand-in list placement (strongholds): every listed chunk has a start, known without a load. */
    record FakeRings(List<ChunkPos> candidates) implements SiteSearch.Probe {
        @Override
        public int spacing() {
            return 0;
        }

        @Override
        public ChunkPos potential(int cellX, int cellZ) {
            return new ChunkPos(cellX, cellZ);
        }

        @Override
        public SiteSearch.Check check(ChunkPos chunk) {
            return SiteSearch.Check.PRESENT;
        }

        @Override
        public boolean load(ChunkPos chunk) {
            return true;
        }

        @Override
        public BlockPos locate(ChunkPos chunk) {
            return chunk.getMiddleBlockPosition(64);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The recipe

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRecipeGivesTheCoreBackAndTunesTheLocator(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingRecipe recipe = recipe(level);
        ItemStack stone = new ItemStack(ModItems.stone(StoneKind.TIME));
        StoneLedger.stamp(stone, UUID.fromString("00000000-0000-0000-0000-00000000e110"));
        ItemStack scepter = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        ItemStack gauntlet = InfinityGauntletItem.empty();
        gauntlet.set(ModDataComponents.GAUNTLET.get(), GauntletContents.EMPTY.with(StoneKind.REALITY)
                .with(StoneKind.SPACE).withSelected(StoneKind.SPACE));
        Object[][] cases = {{stone, StoneKind.TIME}, {scepter, StoneKind.MIND}, {gauntlet, StoneKind.SPACE},
                {new ItemStack(ModItems.THE_ORB.get()), StoneKind.POWER},
                {new ItemStack(ModItems.EYE_OF_AGAMOTTO.get()), StoneKind.TIME}};
        for (Object[] c : cases) {
            ItemStack core = (ItemStack) c[0];
            StoneKind kind = (StoneKind) c[1];
            CraftingInput input = grid(core.copy());
            check(recipe.matches(input, level), "the recipe takes " + core.getItem());
            ItemStack made = recipe.assemble(input, level.registryAccess());
            check(made.is(ModItems.STONE_LOCATOR.get()) && StoneLocator.state(made).tuned().equals(Optional.of(kind)),
                    "a Locator tuned to the " + kind + " from " + core.getItem());
            NonNullList<ItemStack> left = recipe.getRemainingItems(input);
            check(same(level, left.get(4), core), "the core comes back byte for byte: " + core.getItem());
            for (int i = 0; i < left.size(); i++) {
                check(i == 4 || left.get(i).isEmpty(), "the netherite, diamonds and skull are used up (" + i + ")");
            }
        }
        check(StoneLedger.taker(recipe.getRemainingItems(grid(stone.copy())).get(4)) != null,
                "the stone's taker is still written on it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aTitansGauntletOrAnEmptyOneIsNoCore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingRecipe recipe = recipe(level);
        check(!recipe.matches(grid(new ItemStack(ModItems.INFINITY_GAUNTLET.get())), level),
                "not the Titan's Gauntlet");
        check(!recipe.matches(grid(InfinityGauntletItem.empty()), level), "not a Gauntlet with no stone");
        check(!recipe.matches(grid(new ItemStack(Items.DIAMOND)), level), "not a diamond");
        check(new ItemStack(ModItems.TESSERACT.get()).is(ModTags.LOCATOR_CORES)
                && recipe.matches(grid(new ItemStack(ModItems.TESSERACT.get())), level), "the Tesseract is a core");
        check(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                grid(new ItemStack(ModItems.TESSERACT.get())), level).isPresent(), "the crafting table finds it");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What he has

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void whatCountsAsGot(GameTestHelper helper) {
        StonesConfig.Uniqueness player = StonesConfig.Uniqueness.PLAYER;
        StonesConfig.Uniqueness world = StonesConfig.Uniqueness.WORLD;
        check(StoneLocator.need(true, true, true, player, false) == StoneLocator.Need.GOT, "carried: got");
        check(StoneLocator.need(false, true, true, player, false) == StoneLocator.Need.LACKS, "not taken: lacks");
        check(StoneLocator.need(false, true, false, player, false) == StoneLocator.Need.GOT, "taken before: got");
        check(StoneLocator.need(false, true, false, world, true) == StoneLocator.Need.GOT, "his in world mode: got");
        check(StoneLocator.need(false, true, false, world, false) == StoneLocator.Need.SOMEONE_ELSE,
                "another's in world mode: skipped");
        check(StoneLocator.need(false, false, true, StonesConfig.Uniqueness.UNLIMITED, false)
                == StoneLocator.Need.LACKS, "Mind and Space are not counted: lacking until carried");
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer eilon = f.player(5.5, FLOOR, 5.5);
        check(StoneLocator.lacking(eilon).size() == 6, "a new player lacks all six");
        eilon.getInventory().setItem(10, new ItemStack(ModItems.EYE_OF_AGAMOTTO.get()));
        eilon.getInventory().setItem(11, new ItemStack(ModItems.THE_ORB.get()));
        ItemStack gauntlet = InfinityGauntletItem.empty();
        gauntlet.set(ModDataComponents.GAUNTLET.get(), GauntletContents.EMPTY.with(StoneKind.SOUL));
        eilon.getInventory().setItem(12, gauntlet);
        eilon.getInventory().setItem(13, new ItemStack(ModItems.INFINITY_GAUNTLET.get())); // the Titan's: nothing
        eilon.getInventory().offhand.set(0, new ItemStack(ModItems.stone(StoneKind.MIND)));
        check(StoneLocator.need(eilon, StoneKind.TIME) == StoneLocator.Need.GOT, "the Eye holds the Time Stone");
        check(StoneLocator.need(eilon, StoneKind.POWER) == StoneLocator.Need.GOT, "the Orb holds the Power Stone");
        check(StoneLocator.need(eilon, StoneKind.SOUL) == StoneLocator.Need.GOT, "a crafted Gauntlet's stone");
        check(StoneLocator.need(eilon, StoneKind.MIND) == StoneLocator.Need.GOT, "a bare stone in the other hand");
        check(StoneLocator.need(eilon, StoneKind.SPACE) == StoneLocator.Need.LACKS, "the Titan's Gauntlet gives none");
        check(StoneLocator.need(eilon, StoneKind.REALITY) == StoneLocator.Need.LACKS, "nor the Reality Stone");
        ServerPlayer peter = f.player(7.5, FLOOR, 5.5);
        StoneLedger.taken(peter, StoneKind.REALITY);
        check(StoneLocator.need(peter, StoneKind.REALITY) == StoneLocator.Need.GOT,
                "player mode: one he took before (and lost) is not searched for again");
        check(StoneLocator.lacking(peter).equals(List.of(StoneKind.POWER, StoneKind.SPACE, StoneKind.SOUL,
                StoneKind.TIME, StoneKind.MIND)), "the rest in the stones' order: " + StoneLocator.lacking(peter));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WORLD)
    public static void inWorldModeAnotherHoldersStoneIsSkipped(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        f.config(StonesConfig.UNIQUENESS, StonesConfig.Uniqueness.WORLD);
        f.onClose(() -> StoneLedger.reset(server));
        ServerPlayer wanda = f.player(5.5, FLOOR, 5.5);
        ServerPlayer eilon = f.player(8.5, FLOOR, 5.5);
        StoneLedger.taken(wanda, StoneKind.TIME);
        check(StoneLocator.need(wanda, StoneKind.TIME) == StoneLocator.Need.GOT, "his own: got");
        check(StoneLocator.need(eilon, StoneKind.TIME) == StoneLocator.Need.SOMEONE_ELSE, "another's: skipped");
        ItemStack stack = withFilter(locator(eilon), StoneKind.TIME);
        StoneLocator.update(eilon, stack);
        LocatorState state = StoneLocator.state(stack);
        check(state.status() == LocatorState.SOMEONE_ELSE && state.pointing().equals(Optional.of(StoneKind.TIME))
                && state.target().isEmpty(), "\"Someone else holds the Time Stone.\": " + state);
        check(!StoneLocator.lacking(eilon).contains(StoneKind.TIME), "and the filter does not offer it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void sneakUseCyclesTheFilter(GameTestHelper helper) {
        List<StoneKind> lacking = List.of(StoneKind.POWER, StoneKind.SOUL, StoneKind.MIND);
        check(StoneLocator.nextFilter(Optional.empty(), lacking).equals(Optional.of(StoneKind.POWER)), "All → Power");
        check(StoneLocator.nextFilter(Optional.of(StoneKind.POWER), lacking).equals(Optional.of(StoneKind.SOUL)),
                "Power → Soul");
        check(StoneLocator.nextFilter(Optional.of(StoneKind.MIND), lacking).isEmpty(), "Mind → All");
        check(StoneLocator.nextFilter(Optional.of(StoneKind.SPACE), lacking).equals(Optional.of(StoneKind.SOUL)),
                "a kind he got meanwhile → the next he lacks");
        check(StoneLocator.nextFilter(Optional.empty(), List.of()).isEmpty(), "all six: All");
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer eilon = f.player(5.5, FLOOR, 5.5);
        eilon.getInventory().setItem(9, new ItemStack(ModItems.EYE_OF_AGAMOTTO.get()));
        ItemStack stack = locator(eilon);
        List<Optional<StoneKind>> seen = new ArrayList<>();
        TestKit.sentPackets(eilon);
        for (int i = 0; i < 7; i++) {
            eilon.setShiftKeyDown(true);
            stack.use(helper.getLevel(), eilon, InteractionHand.MAIN_HAND);
            seen.add(StoneLocator.state(stack).filter());
        }
        check(seen.equals(List.of(Optional.of(StoneKind.POWER), Optional.of(StoneKind.SPACE),
                Optional.of(StoneKind.REALITY), Optional.of(StoneKind.SOUL), Optional.of(StoneKind.MIND),
                Optional.empty(), Optional.of(StoneKind.POWER))), "every kind he lacks, Time skipped: " + seen);
        List<String> keys = actionBarKeys(TestKit.sentPackets(eilon));
        check(keys.contains("message.enhanced.locator.filter") && keys.contains("message.enhanced.locator.filter.all"),
                "he is told what it looks for: " + keys);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The stack and the spark trail

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theComponentIsWrittenOnlyWhenItChanges(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer eilon = f.player(5.5, FLOOR, 5.5);
        ItemStack stack = withFilter(locator(eilon), StoneKind.SOUL);
        check(StoneLocator.update(eilon, stack), "the first reading is written");
        LocatorState first = stack.get(ModDataComponents.LOCATOR.get());
        check(first.status() == LocatorState.OTHER_REALM && first.target().isEmpty()
                && first.pointing().equals(Optional.of(StoneKind.SOUL))
                && first.tuned().equals(Optional.of(StoneKind.TIME)),
                "the Soul Stone in the Overworld: it spins, tuning kept: " + first);
        check(!StoneLocator.update(eilon, stack), "nothing changed: nothing written");
        check(stack.get(ModDataComponents.LOCATOR.get()) == first, "the very same component stays on the stack");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theSparkTrailFliesAndRests(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer eilon = f.player(5.5, FLOOR, 5.5);
        ItemStack stack = locator(eilon);
        TestKit.sentPackets(eilon);
        eilon.setShiftKeyDown(false);
        check(StoneLocator.pulse(eilon, stack), "use: the trail flies");
        List<Object> sent = TestKit.sentPackets(eilon);
        List<StoneFxPayload> trails = payloads(sent, StoneFxPayload.class).stream()
                .filter(p -> p.kind() == StoneFxPayload.LOCATOR_TRAIL).toList();
        check(trails.size() == 1 && trails.get(0).points().size() == StoneLocator.TRAIL_POINTS
                && trails.get(0).points().size() <= StoneFxPayload.MAX_POINTS, "one trail of 12 points");
        check(TestKit.soundsIn(sent).stream().anyMatch(s -> s.id().equals(ModSounds.LOCATOR_PULSE.getId())),
                "with its hum");
        check(!StoneLocator.pulse(eilon, stack), "it rests (pulseCooldown 100)");
        List<Vec3> line = StoneLocator.trail(new Vec3(0, 64, 0), new BlockPos(100, 64, 0));
        check(Math.abs(line.get(line.size() - 1).x - StoneLocator.TRAIL_LENGTH) < 0.01
                && Math.abs(line.get(line.size() - 1).z) < 0.6,
                "12 blocks toward the site: " + line.get(line.size() - 1));
        f.everyTick(101, t -> t == 101 && StoneLocator.pulse(eilon, stack));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Spent sites

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theThreeSpentHooksMarkTheirSites(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        long space = ChunkPos.asLong(90001, 1);
        long mind = ChunkPos.asLong(90001, 2);
        long power = ChunkPos.asLong(90001, 3);
        BlockPos cradle = helper.absolutePos(new BlockPos(5, 2, 5));
        BlockPos hydra = helper.absolutePos(new BlockPos(7, 2, 5));
        BlockPos end = helper.absolutePos(new BlockPos(9, 2, 5));
        SpentSites.finder((l, at) -> at.sites() == ModTags.STONE_SITE_SPACE && at.where().equals(cradle)
                ? OptionalLong.of(space) : at.sites() == ModTags.STONE_SITE_MIND && at.where().equals(hydra)
                ? OptionalLong.of(mind) : at.sites() == ModTags.STONE_SITE_POWER && at.where().equals(end)
                ? OptionalLong.of(power) : OptionalLong.empty());
        f.onClose(() -> {
            SpentSites.finder(null);
            SpentSites.forget(level, StoneKind.SPACE, space);
            SpentSites.forget(level, StoneKind.MIND, mind);
            SpentSites.forget(level, StoneKind.POWER, power);
        });
        ServerPlayer tony = f.player(5.5, FLOOR, 7.5);
        helper.setBlock(new BlockPos(5, 2, 5), ModBlocks.TESSERACT_CRADLE.get().defaultBlockState()
                .setValue(TesseractCradleBlock.FULL, true));
        check(!SpentSites.isSpent(level, StoneKind.SPACE, space), "a full cradle's tower is not spent");
        check(TesseractCradleBlock.take(level, cradle, level.getBlockState(cradle), tony), "the Tesseract is taken");
        check(SpentSites.isSpent(level, StoneKind.SPACE, space), "the tower is spent");
        roll(level, "chests/hydra_outpost/vault", hydra);
        check(SpentSites.isSpent(level, StoneKind.MIND, mind), "the Hydra vault's loot rolled: the outpost is spent");
        roll(level, "chests/end_tunnel/vault", end);
        check(SpentSites.isSpent(level, StoneKind.POWER, power), "the End vault's loot rolled: the tunnel is spent");
        check(!SpentSites.isSpent(level, StoneKind.TIME, space) && !SpentSites.isSpent(level, StoneKind.MIND, space),
                "only its own kind");
        helper.succeed();
    }

    static void roll(ServerLevel level, String table, BlockPos at) {
        LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Enhanced.id(table)));
        loot.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at))
                .create(LootContextParamSets.CHEST));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The search

    @EnhancedTest(template = ARENA, batch = SEARCH, timeoutTicks = 400)
    public static void theSearchFindsAStartWithinTheBudget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakeGrid grid = new FakeGrid(level, 8);
        ChunkPos far = grid.potential(3, -2); // three rings out
        ChunkPos farther = grid.potential(-5, 0);
        grid.starts.add(far.toLong());
        grid.starts.add(farther.toLong());
        SiteSearch.Job job = SiteSearch.start(new ChunkPos(1, 1), 4096, List.of(grid), c -> false);
        check(!job.done() && job.answer().status() == LocatorState.SEARCHING, "it searches in the background");
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(job::cancel);
        f.everyTick(380, t -> {
            if (!job.done()) {
                return false;
            }
            int most = grid.loadsPerTick.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            check(most <= 2, "at most 2 structure-start loads a tick: " + most);
            check(job.answer().isFound() && job.answer().startChunk() == far.toLong()
                    && job.answer().pos().get().equals(grid.locate(far)), "the nearer start: " + job.answer());
            check(job.loads() == grid.loads && grid.loads <= 81, "the rings to one past the find: " + grid.loads);
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = SEARCH, timeoutTicks = 400)
    public static void theSearchSkipsASpentSiteAndKnowsTooFar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakeGrid grid = new FakeGrid(level, 8);
        ChunkPos spent = grid.potential(1, 0);
        ChunkPos next = grid.potential(2, 2);
        grid.starts.add(spent.toLong());
        grid.starts.add(next.toLong());
        SpentSites.mark(level, StoneKind.TIME, spent.toLong());
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(() -> SpentSites.forget(level, StoneKind.TIME, spent.toLong()));
        SiteSearch.Job job = SiteSearch.start(new ChunkPos(0, 0), 4096, List.of(grid),
                c -> SpentSites.isSpent(level, StoneKind.TIME, c));
        FakeGrid empty = new FakeGrid(level, 8);
        SiteSearch.Job none = SiteSearch.start(new ChunkPos(0, 0), 600, List.of(empty), c -> false);
        check(SiteSearch.start(new ChunkPos(0, 0), 600, List.of(), c -> false).answer().status()
                == LocatorState.TOO_FAR, "nothing placed in this realm: too far at once");
        f.onClose(job::cancel);
        f.onClose(none::cancel);
        f.everyTick(380, t -> {
            if (!job.done() || !none.done()) {
                return false;
            }
            check(job.answer().isFound() && job.answer().startChunk() == next.toLong(),
                    "the spent site is skipped: " + job.answer());
            check(none.answer().status() == LocatorState.TOO_FAR, "no start within the radius: too far");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = SEARCH, timeoutTicks = 200)
    public static void eachRealmHasItsAnswer(GameTestHelper helper) {
        LocatorRoutes.Realm over = LocatorRoutes.Realm.OVERWORLD;
        check(LocatorRoutes.route(over, StoneKind.SOUL, true).type() == LocatorRoutes.Type.SPIN
                && LocatorRoutes.SOUL_NETHER.equals(LocatorRoutes.route(over, StoneKind.SOUL, true).message()),
                "the Soul Stone spins in the Overworld");
        check(LocatorRoutes.route(LocatorRoutes.Realm.NETHER, StoneKind.SOUL, true).type() == LocatorRoutes.Type.SEARCH
                && LocatorRoutes.route(LocatorRoutes.Realm.NETHER, StoneKind.SOUL, true).sites()
                == ModTags.STONE_SITE_SOUL, "and searches Vormir in the Nether");
        check(LocatorRoutes.route(over, StoneKind.POWER, true).sites() == StructureTags.EYE_OF_ENDER_LOCATED
                && LocatorRoutes.route(over, StoneKind.POWER, true).type() == LocatorRoutes.Type.GATEWAY,
                "Power in the Overworld: the stronghold");
        check(LocatorRoutes.route(LocatorRoutes.Realm.END, StoneKind.POWER, true).sites() == ModTags.STONE_SITE_POWER,
                "Power in the End: the tunnels");
        check(LocatorRoutes.route(over, StoneKind.REALITY, true).sites() == ModTags.BIFROST_SITES
                && LocatorRoutes.route(over, StoneKind.REALITY, true, false).status() == LocatorState.HIDDEN,
                "Reality: the Bifrost site; sealed while Hela stands");
        check(LocatorRoutes.route(over, StoneKind.POWER, false).type() == LocatorRoutes.Type.SPIN,
                "no ways shown with pointsToGateways off");
        check(LocatorRoutes.route(LocatorRoutes.Realm.ASGARD, StoneKind.REALITY, true).type()
                == LocatorRoutes.Type.FIXED && LocatorRoutes.route(LocatorRoutes.Realm.ASGARD, StoneKind.TIME, true)
                .message().equals(LocatorRoutes.NOT_ASGARD), "Asgard: only Reality points");
        for (StoneKind kind : StoneKind.values()) {
            check(LocatorRoutes.route(LocatorRoutes.Realm.ROAD, kind, true).message().equals(LocatorRoutes.ROAD),
                    "the Road hides everything");
        }
        check(LocatorRoutes.realm(Level.NETHER) == LocatorRoutes.Realm.NETHER, "the Nether is the Nether");
        // In game: the ways, with stand-in strongholds and Bifrost sites.
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer eilon = f.player(5.5, FLOOR, 5.5);
        ChunkPos at = eilon.chunkPosition();
        ChunkPos stronghold = new ChunkPos(at.x + 40, at.z);
        FakeGrid bifrost = new FakeGrid(helper.getLevel(), 8);
        ChunkPos bifrostSite = bifrost.potential(Math.floorDiv(at.x, 8), Math.floorDiv(at.z, 8) - 2);
        bifrost.starts.add(bifrostSite.toLong());
        SiteSearch.prober((level, tag) -> tag == StructureTags.EYE_OF_ENDER_LOCATED
                ? List.of(new FakeRings(List.of(new ChunkPos(at.x - 200, at.z), stronghold)))
                : tag == ModTags.BIFROST_SITES ? List.of(bifrost) : SiteSearch.placements(level, tag));
        AsgardState asgardState = AsgardState.get(server);
        boolean awake = asgardState.odinAwake();
        f.onClose(() -> {
            SiteSearch.prober(null);
            asgardState.setOdinAwake(awake);
        });
        asgardState.setOdinAwake(true);
        ItemStack time = withFilter(new ItemStack(ModItems.STONE_LOCATOR.get()), StoneKind.TIME);
        StoneLocator.update(eilon, time);
        check(StoneLocator.state(time).status() == LocatorState.TOO_FAR && StoneLocator.state(time).distance() == 4096,
                "no Sanctum in a flat world: too far (4096): " + StoneLocator.state(time));
        ItemStack power = withFilter(locator(eilon), StoneKind.POWER);
        ItemStack reality = withFilter(new ItemStack(ModItems.STONE_LOCATOR.get()), StoneKind.REALITY);
        f.everyTick(150, t -> {
            StoneLocator.update(eilon, power);
            StoneLocator.update(eilon, reality);
            LocatorState p = StoneLocator.state(power);
            LocatorState r = StoneLocator.state(reality);
            if (r.status() == LocatorState.SEARCHING || p.status() == LocatorState.SEARCHING) {
                return false;
            }
            check(p.status() == LocatorState.OTHER_REALM && p.target().map(g -> g.pos().equals(stronghold
                    .getMiddleBlockPosition(64))).orElse(false), "the Power Stone: the nearest stronghold: " + p);
            check(r.status() == LocatorState.OTHER_REALM && r.target().isPresent()
                    && r.target().get().pos().equals(bifrost.locate(bifrostSite)), "Reality: the Bifrost site: " + r);
            asgardState.setOdinAwake(false);
            StoneLocator.update(eilon, reality);
            check(StoneLocator.state(reality).status() == LocatorState.HIDDEN
                    && StoneLocator.state(reality).target().isPresent(), "the vault sealed: still the Bifrost site");
            // In Asgard: the vault's pedestal.
            ServerLevel asgard = AsgardTestSupport.withArrival(server);
            server.getPlayerList().getOps().add(new ServerOpListEntry(eilon.getGameProfile(), 2, false));
            eilon.teleportTo(asgard, CityPlan.ARRIVAL.x, CityPlan.ARRIVAL.y, CityPlan.ARRIVAL.z, 0, 0);
            server.getPlayerList().getOps().remove(eilon.getGameProfile());
            StoneLocator.update(eilon, reality);
            check(StoneLocator.state(reality).status() == LocatorState.HIDDEN
                    && StoneLocator.state(reality).target().isEmpty(), "Asgard, the vault shut: it spins");
            asgardState.setOdinAwake(true);
            StoneLocator.update(eilon, reality);
            LocatorState in = StoneLocator.state(reality);
            check(in.status() == LocatorState.FOUND && in.target().isPresent()
                    && in.target().get().pos().equals(CityPlan.REALITY_PEDESTAL)
                    && in.target().get().dimension() == asgard.dimension(), "Asgard: the vault's pedestal: " + in);
            return true;
        });
    }

    private LocatorTests() {
    }
}
