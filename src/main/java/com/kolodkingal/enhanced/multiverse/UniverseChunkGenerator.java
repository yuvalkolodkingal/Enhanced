package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.Platform;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The land of the other universes (3.19): the game's own noise generator (the Overworld's biomes and land), with the
 * universe's number, which decides which structures it builds ({@link #createState}):
 * <ul>
 *     <li>none of the mod's structures, so no Infinity Stone sites (the stones are in chests: {@link UniverseStones}),
 *     except the Mutant School in 10005, the only universe that has it;</li>
 *     <li>in 2149 every village is an abandoned village ({@code enhanced:abandoned_village_*}, the game's zombie
 *     villages alone).</li>
 * </ul>
 * In the random universes (3.19.1) each cell's {@link UniverseRecipe} turns the biomes into others as they are made
 * ({@link #createBiomes}), swaps the ground and the seas just before the land is decorated, so that the trees and
 * structures stand on the new land as they are, and the stone and the ores just after ({@link #applyBiomeDecoration}).
 * In 2149 and the Gamma Bomb's world the survivors' bunkers are built as the land is ({@link Bunkers}).
 */
public class UniverseChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<UniverseChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings),
            Codec.INT.fieldOf("universe").forGetter(UniverseChunkGenerator::universe)
    ).apply(instance, instance.stable(UniverseChunkGenerator::new)));

    /** The structure set of the school: the one structure of the mod that 10005 keeps. */
    static final String SCHOOLS = "enhanced:mutant_schools";
    /** The game's villages, and their abandoned versions in 2149. */
    static final String VILLAGES = "minecraft:villages";
    static final Map<String, String> ABANDONED = Map.of(
            "minecraft:village_plains", "abandoned_village_plains",
            "minecraft:village_desert", "abandoned_village_desert",
            "minecraft:village_savanna", "abandoned_village_savanna",
            "minecraft:village_snowy", "abandoned_village_snowy",
            "minecraft:village_taiga", "abandoned_village_taiga");

    private final int universe;

    public UniverseChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, int universe) {
        super(biomeSource, settings);
        this.universe = universe;
    }

    /** The universe's number (10005, 2149, Gamma; 0 for the random universes, whose cells each have their own). */
    public int universe() {
        return universe;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** The recipe of the cell a chunk is in, or null outside the random universes. */
    @Nullable
    private UniverseRecipe recipe(ChunkAccess chunk) {
        if (universe != 0) {
            return null;
        }
        int x = chunk.getPos().getMiddleBlockX();
        int z = chunk.getPos().getMiddleBlockZ();
        return UniverseRecipe.of(Multiverse.numberOfCell(Multiverse.cellOf((double) x), Multiverse.cellOf((double) z)));
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender, StructureManager structureManager,
                                                       ChunkAccess chunk) {
        UniverseRecipe recipe = recipe(chunk);
        CompletableFuture<ChunkAccess> made = super.createBiomes(randomState, blender, structureManager, chunk);
        if (recipe == null || !recipe.swapsBiomes()) {
            return made;
        }
        Set<Holder<Biome>> possible = getBiomeSource().possibleBiomes();
        return made.thenApply(done -> {
            // (The chunk's own getNoiseBiome refuses before its status says it has biomes: read the sections.)
            done.fillBiomesFromNoise((qx, qy, qz, sampler) -> recipe.biome(done.getSection(done.getSectionIndex(qy << 2))
                    .getNoiseBiome(qx & 3, qy & 3, qz & 3), possible), randomState.sampler());
            return done;
        });
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        UniverseRecipe recipe = recipe(chunk);
        if (recipe != null && (recipe.ground() != null || recipe.sea() != null)) {
            swap(chunk, recipe, false);
        }
        super.applyBiomeDecoration(level, chunk, structureManager);
        // The stone and the ores after the land is decorated: the ores are in the stone first, then both turn.
        if (recipe != null && (recipe.stone() != null || recipe.ore() != null)) {
            swap(chunk, recipe, true);
        }
        if (universe == Multiverse.ZOMBIES || universe == Multiverse.GAMMA) {
            Bunkers.build(this, level, chunk, universe);
        }
    }

    /** The game's ores, every one of which a universe of other ores turns. */
    static final Set<Block> ORES = Set.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.LAPIS_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.NETHER_QUARTZ_ORE);

    /** Whether the block is what the land is made of (the stone of each shape: stone, deepslate, netherrack, end stone). */
    static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.NETHERRACK) || state.is(Blocks.END_STONE);
    }

    /**
     * The cell's blocks in place of the game's, written straight into the chunk's sections: the ground and the seas
     * ({@code late} false: before the land is decorated), or the stone and the ores ({@code late}: after).
     */
    static void swap(ChunkAccess chunk, UniverseRecipe recipe, boolean late) {
        BlockState stone = late ? recipe.stone() : null;
        BlockState ore = late ? recipe.ore() : null;
        BlockState ground = late ? null : recipe.ground();
        BlockState sea = late ? null : recipe.sea();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (LevelChunkSection section : chunk.getSections()) {
            if (section.hasOnlyAir() || !section.maybeHas(state -> stone != null && isStone(state) || ore != null && ORES.contains(state.getBlock())
                    || ground != null && state.is(Blocks.GRASS_BLOCK) || sea != null && state == water)) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        BlockState to = null;
                        if (stone != null && isStone(state)) {
                            to = stone;
                        } else if (ore != null && ORES.contains(state.getBlock())) {
                            to = ore;
                        } else if (ground != null && state.is(Blocks.GRASS_BLOCK)) {
                            to = ground;
                        } else if (sea != null && state == water) {
                            to = sea;
                        }
                        if (to != null) {
                            section.setBlockState(x, y, z, to, false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> lookup, RandomState randomState, long seed) {
        return ChunkGeneratorStructureState.createForFlat(randomState, seed, getBiomeSource(),
                lookup.listElements().<Holder<StructureSet>>map(set -> keep(set.getRegisteredName(), set)).filter(Objects::nonNull));
    }

    /** The structure set as this universe builds it, or null if it builds none of it. */
    private Holder<StructureSet> keep(String id, Holder<StructureSet> set) {
        if (id.startsWith(Enhanced.MODID + ":")) {
            return universe == Multiverse.XMEN && id.equals(SCHOOLS) ? set : null;
        }
        if (universe == Multiverse.ZOMBIES && id.equals(VILLAGES)) {
            return abandoned(set);
        }
        return set;
    }

    /** The villages of 2149: the same placement, the abandoned villages in place of the living ones. */
    private static Holder<StructureSet> abandoned(Holder<StructureSet> villages) {
        MinecraftServer server = Platform.get().currentServer();
        if (server == null) {
            return villages;
        }
        List<StructureSet.StructureSelectionEntry> entries = new ArrayList<>();
        for (StructureSet.StructureSelectionEntry entry : villages.value().structures()) {
            String from = entry.structure().getRegisteredName();
            String to = ABANDONED.get(from);
            if (to == null) {
                continue;
            }
            try {
                Holder<Structure> structure = server.registryAccess().registryOrThrow(Registries.STRUCTURE)
                        .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, Enhanced.id(to)));
                entries.add(new StructureSet.StructureSelectionEntry(structure, entry.weight()));
            } catch (RuntimeException missing) {
                Enhanced.LOGGER.warn("The abandoned village {} is missing; 2149 builds no village there", to);
            }
        }
        return entries.isEmpty() ? null : Holder.direct(new StructureSet(entries, villages.value().placement()));
    }
}
