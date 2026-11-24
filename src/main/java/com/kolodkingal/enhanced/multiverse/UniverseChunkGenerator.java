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

import java.util.ArrayList;
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

    /** The universe's number (10005, 2149; 0 for What If, whose cells each have their own). */
    public int universe() {
        return universe;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
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
