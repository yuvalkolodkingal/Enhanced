package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A site set on the floor of a tall Nether cavern (3.6, {@code enhanced:nether_site}): the Vormir Cliff. Its template
 * brings its own rock and its own air; it fits only where the template's top stays under the bedrock roof. Registered
 * by the 3.6 skeleton with a stand-in body (the first floor with room over it); lane L4 owns the class and writes the
 * real scan (the plan's 4.4), keeping the codec's fields (it may add optional ones).
 */
public class NetherSiteStructure extends Structure {
    public static final MapCodec<NetherSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template)
    ).apply(instance, NetherSiteStructure::new));

    private final ResourceLocation template;

    public NetherSiteStructure(StructureSettings settings, ResourceLocation template) {
        super(settings);
        this.template = template;
    }

    public ResourceLocation template() {
        return template;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return PlaceholderSites.onCavernFloor(context, template, 33, 110, 8);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.NETHER_SITE.get();
    }
}
