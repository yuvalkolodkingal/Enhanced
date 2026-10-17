package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.block.MjolnirBlockEntity;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.world.CraterPiece;
import com.kolodkingal.enhanced.world.CraterStructure;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import com.kolodkingal.enhanced.world.TemplateSiteStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The world structures of 3.3 (lane WORLD): the Hydra outposts (Loki's Scepter), Mjolnir's landing sites and the Mutant
 * School (the wheelchair and Cyclops' glasses): their data, their templates, their loot, and how they are built. The
 * structures are built far from the arenas (100,000 blocks out), where nothing else is.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class WorldStructureTests {
    static final String BATCH = "world";
    static final ResourceLocation WHEELCHAIR = ResourceLocation.fromNamespaceAndPath(Enhanced.MODID, "professor_x_wheelchair");
    static final ResourceLocation GLASSES = ResourceLocation.fromNamespaceAndPath(Enhanced.MODID, "cyclops_glasses");

    private static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Enhanced.id("chests/" + path));
    }

    private static List<ItemStack> roll(GameTestHelper helper, String table, RandomSource random) {
        ServerLevel level = helper.getLevel();
        LootTable loot = level.getServer().reloadableRegistries().getLootTable(loot(table));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(10.5, 2, 10.5)))
                .create(LootContextParamSets.CHEST);
        return loot.getRandomItems(params, random);
    }

    private static int count(List<ItemStack> stacks, ResourceLocation item) {
        return stacks.stream().filter(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).equals(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** The loot tables of the chests of a template (positions relative to its corner, unturned). */
    private static List<String> chestTables(GameTestHelper helper, String template) {
        StructureTemplate built = helper.getLevel().getStructureManager().getOrCreate(Enhanced.id(template));
        List<String> tables = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : built.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST)) {
            tables.add(info.nbt() == null ? "" : info.nbt().getString("LootTable"));
        }
        return tables;
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theThreeStructuresAndTheirSetsAreData(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Registry<Structure> structures = access.registryOrThrow(Registries.STRUCTURE);
        Registry<StructureSet> sets = access.registryOrThrow(Registries.STRUCTURE_SET);
        check(structures.get(Enhanced.id("hydra_outpost")) instanceof TemplateSiteStructure outpost
                && outpost.template().equals(Enhanced.id("hydra_outpost/base")) && outpost.floor() == 5, "the Hydra outpost is a template site");
        check(structures.get(Enhanced.id("mutant_school")) instanceof TemplateSiteStructure school
                && school.template().equals(Enhanced.id("mutant_school/mansion")) && school.floor() == 6, "the Mutant School is a template site");
        check(structures.get(Enhanced.id("mjolnir_crater")) instanceof CraterStructure crater && crater.core().is(ModBlocks.MJOLNIR.get()),
                "Mjolnir's crater has Mjolnir in its middle");
        for (String set : List.of("hydra_outposts", "mjolnir_craters", "mutant_schools")) {
            check(sets.get(Enhanced.id(set)) != null, "the structure set " + set + " exists (so /locate and new chunks find it)");
        }
        record Biomes3(String structure, List<ResourceKey<Biome>> has, List<ResourceKey<Biome>> hasNot) {
        }
        for (Biomes3 b : List.of(
                new Biomes3("hydra_outpost", List.of(Biomes.TAIGA, Biomes.SNOWY_PLAINS, Biomes.FOREST), List.of(Biomes.DESERT, Biomes.OCEAN, Biomes.NETHER_WASTES)),
                new Biomes3("mjolnir_crater", List.of(Biomes.DESERT, Biomes.BADLANDS, Biomes.SAVANNA), List.of(Biomes.OCEAN, Biomes.TAIGA, Biomes.THE_END)),
                new Biomes3("mutant_school", List.of(Biomes.PLAINS, Biomes.MEADOW), List.of(Biomes.DESERT, Biomes.OCEAN, Biomes.BASALT_DELTAS)))) {
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, Enhanced.id("has_structure/" + b.structure()));
            var biomes = access.registryOrThrow(Registries.BIOME);
            for (var key : b.has()) {
                check(biomes.getHolderOrThrow(key).is(tag), b.structure() + " is built in " + key.location());
            }
            for (var key : b.hasNot()) {
                check(!biomes.getHolderOrThrow(key).is(tag), b.structure() + " is never built in " + key.location());
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theScepterIsInEveryOutpostsVault(GameTestHelper helper) {
        List<String> tables = chestTables(helper, "hydra_outpost/base");
        check(tables.stream().filter("enhanced:chests/hydra_outpost/vault"::equals).count() == 1, "the outpost has one vault chest: " + tables);
        check(tables.stream().allMatch(t -> t.startsWith("enhanced:chests/hydra_outpost/")), "every chest of the outpost has its loot: " + tables);
        RandomSource random = RandomSource.create(1945L);
        ResourceLocation scepter = BuiltInRegistries.ITEM.getKey(ModItems.LOKIS_SCEPTER.get());
        for (int i = 0; i < 200; i++) {
            List<ItemStack> vault = roll(helper, "hydra_outpost/vault", random);
            check(count(vault, scepter) == 1 && vault.size() >= 3, "the vault holds exactly one Scepter and some treasure: " + vault);
            for (String other : List.of("hydra_outpost/lab", "hydra_outpost/barracks", "hydra_outpost/supply")) {
                List<ItemStack> stacks = roll(helper, other, random);
                check(!stacks.isEmpty() && count(stacks, scepter) == 0, other + " has loot but no Scepter");
            }
        }
        helper.succeed();
    }

    /**
     * The wheelchair (lane XAVIER) and the glasses (lane CYCLOPS) are each in a table of their own that exists only when its
     * item does; with the item missing the chest still has its other loot and nothing fails to load.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theSchoolHoldsTheWheelchairAndTheGlassesWhenTheyExist(GameTestHelper helper) {
        List<String> tables = chestTables(helper, "mutant_school/mansion");
        check(tables.stream().filter("enhanced:chests/mutant_school/study"::equals).count() == 1, "one chest in the Professor's study: " + tables);
        check(tables.stream().filter("enhanced:chests/mutant_school/cyclops"::equals).count() == 1, "one chest in Scott's room: " + tables);
        check(tables.stream().allMatch(t -> t.startsWith("enhanced:chests/mutant_school/")), "every chest of the school has its loot: " + tables);
        RandomSource random = RandomSource.create(1963L);
        record Hero(String chest, ResourceLocation item) {
        }
        for (Hero hero : List.of(new Hero("mutant_school/study", WHEELCHAIR), new Hero("mutant_school/cyclops", GLASSES))) {
            boolean exists = BuiltInRegistries.ITEM.containsKey(hero.item());
            for (int i = 0; i < 100; i++) {
                List<ItemStack> stacks = roll(helper, hero.chest(), random);
                check(count(stacks, hero.item()) == (exists ? 1 : 0), hero.chest() + " holds " + (exists ? "exactly one " : "no ") + hero.item() + ": " + stacks);
                check(stacks.size() >= 2, hero.chest() + " has its other loot too");
            }
        }
        for (String other : List.of("dormitory", "library", "kitchen", "cerebro")) {
            check(!roll(helper, "mutant_school/" + other, random).isEmpty(), other + " has loot");
        }
        helper.succeed();
    }

    /** The outpost built for real (turned, with its processors) high in the air: its vault, and the footing under it. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void anOutpostIsBuiltWithItsVaultAndAFooting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate built = level.getStructureManager().getOrCreate(Enhanced.id("hydra_outpost/base"));
        Vec3i size = built.getSize();
        check(size.equals(new Vec3i(33, 22, 29)), "the template is 33 x 22 x 29: " + size);
        Holder<StructureProcessorList> processors = level.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                .getHolderOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, Enhanced.id("hydra_outpost")));
        BlockPos center = new BlockPos(100_008, 60, 100_008);
        TemplateSitePiece piece = new TemplateSitePiece(level.getStructureManager(), Enhanced.id("hydra_outpost/base"), processors,
                Rotation.CLOCKWISE_180, center, size, 5);
        build(level, piece);
        // turned half round: template (8, 2, 12) is at center - (8 - 16, 2 - 5, 12 - 14) turned = center + (8, -3, 2)
        BlockPos vault = center.offset(8, -3, 2);
        check(level.getBlockEntity(vault) instanceof ChestBlockEntity, "the vault chest is where the template puts it: " + level.getBlockState(vault));
        check(level.getBlockEntity(vault).saveWithoutMetadata(level.registryAccess()).getString("LootTable").equals("enhanced:chests/hydra_outpost/vault"),
                "with the vault's loot");
        // the yard floats in the air here: under every column of it the footing goes down 16 blocks, no more; under gravel it is dirt
        int gravel = 0;
        for (int dx = -16; dx <= 16; dx += 4) {
            BlockPos yard = center.offset(dx, 0, 12);
            check(!level.getBlockState(yard).isAir(), "the yard is laid at ground level: " + level.getBlockState(yard));
            for (int down = 1; down <= 16; down++) {
                check(!level.getBlockState(yard.below(down)).isAir(), "a footing under the yard, " + down + " down at " + dx);
            }
            check(level.getBlockState(yard.below(17)).isAir(), "sixteen blocks at most");
            if (level.getBlockState(yard).is(Blocks.GRAVEL)) {
                gravel++;
                check(level.getBlockState(yard.below()).is(Blocks.DIRT), "gravel stands on a footing of dirt");
            }
        }
        check(gravel > 0, "some of the yard is gravel");
        helper.succeed();
    }

    /** A crater built on the flat ground: the bowl, the scorch, the campfires, and a Mjolnir nobody owns in the middle. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aCraterHasAnUnclaimedMjolnirInItsMiddle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        int x = 100_008;
        int z = 100_408;
        level.getChunk(x >> 4, z >> 4);
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        // The real structure finds its place on the flat ground (it is level and dry)
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(Enhanced.id("mjolnir_crater"));
        StructureStart start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
                level.getStructureManager(), level.getSeed(), new ChunkPos(x >> 4, z >> 4), 0, level, biome -> true);
        check(start.isValid() && start.getPieces().get(0) instanceof CraterPiece, "the crater finds a place on level, dry ground");
        // (the flat world is only three blocks deep: this one is shallow)
        CraterPiece crater = new CraterPiece(new BlockPos(x, ground, z), 7, 2, 77L, ModBlocks.MJOLNIR.get().defaultBlockState());
        build(level, crater);
        BlockPos core = crater.corePos();
        check(level.getBlockState(core).is(ModBlocks.MJOLNIR.get()), "Mjolnir lies in the middle: " + level.getBlockState(core));
        check(level.getBlockEntity(core) instanceof MjolnirBlockEntity, "as the block that remembers its hammer");
        ItemStack hammer = MjolnirBlock.hammerAt(level, core);
        check(hammer.is(ModItems.MJOLNIR.get()) && Worthiness.ownerOf(hammer) == null, "a Mjolnir nobody owns");
        check(core.getY() < ground, "down in the bowl, below the ground round it");
        check(level.getBlockState(new BlockPos(x, ground, z)).isAir(), "the ground over the middle is blown away");
        int campfires = 0;
        int scorched = 0;
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                for (int y = core.getY() - 2; y <= ground + 3; y++) {
                    var state = level.getBlockState(new BlockPos(x + dx, y, z + dz));
                    if (state.is(Blocks.CAMPFIRE)) {
                        campfires++;
                    }
                    if (state.is(Blocks.BLACKSTONE) || state.is(Blocks.TUFF) || state.is(Blocks.SMOOTH_BASALT) || state.is(Blocks.GRAVEL)) {
                        scorched++;
                    }
                }
            }
        }
        check(campfires == 2, "two campfires smoke in it: " + campfires);
        check(scorched > 100, "its floor and the ground round it are burnt: " + scorched);
        helper.succeed();
    }

    /** As it lies in a crater (the block with no hammer of its own): the first worthy hand claims it, the unworthy cannot. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theCratersMjolnirIsClaimedByTheFirstWorthy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(10, 2, 10));
        level.setBlock(abs, ModBlocks.MJOLNIR.get().defaultBlockState(), 3);
        ServerPlayer nobody = f.player(9.5, 2, 9.5);
        use(level, nobody, abs);
        check(level.getBlockState(abs).is(ModBlocks.MJOLNIR.get()), "it won't budge for the unworthy");
        ServerPlayer thor = f.player(11.5, 2, 11.5, Power.THOR);
        use(level, thor, abs);
        check(level.getBlockState(abs).isAir(), "the worthy lifts it");
        ItemStack hammer = thor.getMainHandItem();
        MjolnirOwner owner = Worthiness.ownerOf(hammer);
        check(hammer.is(ModItems.MJOLNIR.get()) && owner != null && owner.id().equals(thor.getUUID()), "and it is his now: " + owner);
        helper.succeed();
    }

    private static void use(ServerLevel level, ServerPlayer player, BlockPos abs) {
        level.getBlockState(abs).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    /** Builds a piece chunk by chunk, as the world's generation does. */
    private static void build(ServerLevel level, StructurePiece piece) {
        BoundingBox box = piece.getBoundingBox();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        Set<Long> done = new HashSet<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (!done.add(ChunkPos.asLong(cx, cz))) {
                    continue;
                }
                ChunkPos chunk = new ChunkPos(cx, cz);
                level.getChunk(cx, cz);
                BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(), chunk.getMaxBlockX(),
                        level.getMaxBuildHeight(), chunk.getMaxBlockZ());
                piece.postProcess(level, level.structureManager(), generator, RandomSource.create(cx * 31L + cz), chunkBox, chunk, box.getCenter());
            }
        }
    }

    private WorldStructureTests() {
    }
}
