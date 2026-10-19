package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.List;
import java.util.Optional;

/**
 * A tall site of several templates stacked on one another (3.6, {@code enhanced:tower_site}): Avengers Tower ({@code
 * base}, {@code mid}, {@code top}, each at most 200 KB). {@code parts} are the templates from the ground up, {@code
 * part_height} the layers between two of them. Registered by the 3.6 skeleton with a stand-in body (the first part
 * only, on the surface); lane L4 owns the class and writes the real one (the plan's 4.2), keeping the codec's fields
 * (it may add optional ones).
 */
public class TowerSiteStructure extends Structure {
    public static final MapCodec<TowerSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.listOf().fieldOf("parts").forGetter(s -> s.parts),
            Codec.intRange(1, 128).optionalFieldOf("part_height", 32).forGetter(s -> s.partHeight)
    ).apply(instance, TowerSiteStructure::new));

    private final List<ResourceLocation> parts;
    private final int partHeight;

    public TowerSiteStructure(StructureSettings settings, List<ResourceLocation> parts, int partHeight) {
        super(settings);
        this.parts = List.copyOf(parts);
        this.partHeight = partHeight;
    }

    public List<ResourceLocation> parts() {
        return parts;
    }

    public int partHeight() {
        return partHeight;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return parts.isEmpty() ? Optional.empty() : PlaceholderSites.onSurface(context, parts.get(0));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TOWER_SITE.get();
    }
}
