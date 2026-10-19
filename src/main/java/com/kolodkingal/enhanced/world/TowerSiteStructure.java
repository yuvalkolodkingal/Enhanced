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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A tall site of several templates stacked on one another (3.6, {@code enhanced:tower_site}): Avengers Tower ({@code
 * base}, {@code mid}, {@code top}, each at most 200 KB). {@code parts} are the templates from the ground up, {@code
 * part_height} the layers between two of them; each part is centred on the one under it and all are turned the same
 * way about the same column. The ground under the base is asked as for a {@link TemplateSiteStructure}: {@code floor}
 * (the base's layer at ground level: the layers under it are a cellar), {@code max_slope} (nine points of the base's
 * footprint at most this far apart, all on dry land), {@code processors} (for the base only: its footing). {@code
 * enabled_by}: the {@code [stone_sites]} option that stops it ({@link SiteSwitch}). One {@link TemplateSitePiece} a
 * part: the game clips each to the chunk being generated.
 */
public class TowerSiteStructure extends Structure {
    private static final Holder<StructureProcessorList> NONE = Holder.direct(new StructureProcessorList(List.of()));

    public static final MapCodec<TowerSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.listOf().fieldOf("parts").forGetter(s -> s.parts),
            Codec.intRange(1, 128).optionalFieldOf("part_height", 32).forGetter(s -> s.partHeight),
            Codec.intRange(0, 64).optionalFieldOf("floor", 0).forGetter(s -> s.floor),
            Codec.intRange(0, 32).optionalFieldOf("max_slope", 4).forGetter(s -> s.maxSlope),
            StructureProcessorType.LIST_CODEC.optionalFieldOf("processors", NONE).forGetter(s -> s.processors),
            SiteSwitch.OPTION.optionalFieldOf("enabled_by").forGetter(s -> s.enabledBy)
    ).apply(instance, TowerSiteStructure::new));

    private final List<ResourceLocation> parts;
    private final int partHeight;
    private final int floor;
    private final int maxSlope;
    private final Holder<StructureProcessorList> processors;
    private final Optional<String> enabledBy;

    public TowerSiteStructure(StructureSettings settings, List<ResourceLocation> parts, int partHeight) {
        this(settings, parts, partHeight, 0, 4, NONE, Optional.empty());
    }

    public TowerSiteStructure(StructureSettings settings, List<ResourceLocation> parts, int partHeight, int floor,
            int maxSlope, Holder<StructureProcessorList> processors, Optional<String> enabledBy) {
        super(settings);
        this.parts = List.copyOf(parts);
        this.partHeight = partHeight;
        this.floor = floor;
        this.maxSlope = maxSlope;
        this.processors = processors;
        this.enabledBy = enabledBy;
    }

    public List<ResourceLocation> parts() {
        return parts;
    }

    public int partHeight() {
        return partHeight;
    }

    public int floor() {
        return floor;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (parts.isEmpty() || !SiteSwitch.enabled(enabledBy)) {
            return Optional.empty();
        }
        StructureTemplateManager manager = context.structureTemplateManager();
        List<Vec3i> sizes = new ArrayList<>();
        for (ResourceLocation part : parts) {
            Vec3i size = manager.getOrCreate(part).getSize();
            if (size.getX() == 0 || size.getZ() == 0) {
                return Optional.empty();
            }
            sizes.add(size);
        }
        Rotation rotation = Rotation.getRandom(context.random());
        ChunkPos chunk = context.chunkPos();
        int centerX = chunk.getMiddleBlockX();
        int centerZ = chunk.getMiddleBlockZ();
        Vec3i base = sizes.get(0);
        boolean turned = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        int halfX = (turned ? base.getZ() : base.getX()) / 2;
        int halfZ = (turned ? base.getX() : base.getZ()) / 2;
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
        int top = sorted[4] - floor + partHeight * (parts.size() - 1) + sizes.get(sizes.size() - 1).getY();
        if (top > context.heightAccessor().getMaxBuildHeight()) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(centerX, sorted[4], centerZ);
        return Optional.of(new GenerationStub(center.above(), builder -> {
            for (TemplateSitePiece piece : pieces(manager, rotation, center)) {
                builder.addPiece(piece);
            }
        }));
    }

    /**
     * The tower's pieces with its base's ground-level layer at {@code center} (the middle of its footprint), turned by
     * {@code rotation}: part {@code i} is set {@code i * part_height} layers over the base, centred on the same column.
     */
    public List<TemplateSitePiece> pieces(StructureTemplateManager manager, Rotation rotation, BlockPos center) {
        List<TemplateSitePiece> out = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            StructureTemplate template = manager.getOrCreate(parts.get(i));
            out.add(new TemplateSitePiece(manager, parts.get(i), i == 0 ? processors : NONE, rotation, center,
                    template.getSize(), floor - i * partHeight));
        }
        return out;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TOWER_SITE.get();
    }
}
