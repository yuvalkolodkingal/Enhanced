package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Arrays;
import java.util.Optional;

/**
 * One structure template set down on fairly level ground, turned a random way (the Hydra outposts and the Mutant
 * School). Everything about it is data: the template, which of its layers lies at ground level ({@code floor}: the
 * layers below it are cellars, sunk into the ground), how uneven the ground under it may be ({@code max_slope}: the
 * highest and lowest of nine points of its footprint at most this far apart, or it is not built there; all nine on dry land), and the
 * processors its blocks go through (the last one is usually {@code enhanced:foundation}, the footing where the ground
 * falls away). Never on water. 3.6: {@code enabled_by} (optional) names the {@code [stone_sites]} option that stops it
 * generating (the Sanctum's {@code sanctums}, see {@link SiteSwitch}).
 */
public class TemplateSiteStructure extends Structure {
    public static final MapCodec<TemplateSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            Codec.intRange(0, 64).fieldOf("floor").forGetter(s -> s.floor),
            Codec.intRange(0, 32).optionalFieldOf("max_slope", 4).forGetter(s -> s.maxSlope),
            StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(s -> s.processors),
            SiteSwitch.OPTION.optionalFieldOf("enabled_by").forGetter(s -> s.enabledBy)
    ).apply(instance, TemplateSiteStructure::new));

    private final ResourceLocation template;
    private final int floor;
    private final int maxSlope;
    private final Holder<StructureProcessorList> processors;
    private final Optional<String> enabledBy;

    public TemplateSiteStructure(StructureSettings settings, ResourceLocation template, int floor, int maxSlope,
            Holder<StructureProcessorList> processors) {
        this(settings, template, floor, maxSlope, processors, Optional.empty());
    }

    public TemplateSiteStructure(StructureSettings settings, ResourceLocation template, int floor, int maxSlope,
            Holder<StructureProcessorList> processors, Optional<String> enabledBy) {
        super(settings);
        this.template = template;
        this.floor = floor;
        this.maxSlope = maxSlope;
        this.processors = processors;
        this.enabledBy = enabledBy;
    }

    public ResourceLocation template() {
        return template;
    }

    public int floor() {
        return floor;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!SiteSwitch.enabled(enabledBy)) {
            return Optional.empty();
        }
        StructureTemplate built = context.structureTemplateManager().getOrCreate(template);
        Vec3i size = built.getSize();
        if (size.getX() == 0 || size.getZ() == 0) {
            return Optional.empty();
        }
        Rotation rotation = Rotation.getRandom(context.random());
        ChunkPos chunk = context.chunkPos();
        int centerX = chunk.getMiddleBlockX();
        int centerZ = chunk.getMiddleBlockZ();
        boolean turned = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        int halfX = (turned ? size.getZ() : size.getX()) / 2;
        int halfZ = (turned ? size.getX() : size.getZ()) / 2;
        int[] heights = new int[9];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                heights[i++] = SiteGround.surface(context, centerX + dx * halfX, centerZ + dz * halfZ);
            }
        }
        int[] sorted = heights.clone();
        Arrays.sort(sorted);
        if (sorted[8] - sorted[0] > maxSlope) {
            return Optional.empty();
        }
        i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!SiteGround.dryLand(context, centerX + dx * halfX, centerZ + dz * halfZ, heights[i++])) {
                    return Optional.empty();
                }
            }
        }
        int ground = sorted[4];
        BlockPos center = new BlockPos(centerX, ground, centerZ);
        return Optional.of(new GenerationStub(center.above(), builder -> builder.addPiece(
                new TemplateSitePiece(context.structureTemplateManager(), template, processors, rotation, center, size, floor))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TEMPLATE_SITE.get();
    }
}
