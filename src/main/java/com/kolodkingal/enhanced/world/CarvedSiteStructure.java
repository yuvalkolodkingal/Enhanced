package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A site carved into an island of the End (3.6, {@code enhanced:carved_site}): the End tunnel, whose vault holds the
 * Orb. Its template's air carves the tunnel into the island. Registered by the 3.6 skeleton with a stand-in body (the
 * island's surface); lane L4 owns the class and writes the real one (the plan's 4.3: the island's edge, the vault under
 * the surface, the refusals), keeping the codec's fields (it may add optional ones).
 */
public class CarvedSiteStructure extends Structure {
    public static final MapCodec<CarvedSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template)
    ).apply(instance, CarvedSiteStructure::new));

    private final ResourceLocation template;

    public CarvedSiteStructure(StructureSettings settings, ResourceLocation template) {
        super(settings);
        this.template = template;
    }

    public ResourceLocation template() {
        return template;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return PlaceholderSites.onSurface(context, template);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CARVED_SITE.get();
    }
}
