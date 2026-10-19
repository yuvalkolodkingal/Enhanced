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
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntFunction;

/**
 * A site set on the floor of a tall Nether cavern (3.6, {@code enhanced:nether_site}): the Vormir Cliff. The chunk's
 * middle column is scanned from {@code from} (33: over the lava sea) upward for the first solid floor, not lava, with
 * {@code headroom} (8) layers of air over it; the template's layer 0 is set on it, unturned and centred on the column
 * (so its fixed points are its corner plus the plan's offsets). If its top would pass {@code roof} (120, under the
 * bedrock) the chunk is refused: it fits only where the floor is low. The template brings its own rock and its own air
 * (outside the rock it is void: the cavern stays); {@code processors} (its footing, where the floor falls away). {@code
 * enabled_by}: the {@code [stone_sites]} option that stops it ({@link SiteSwitch}).
 */
public class NetherSiteStructure extends Structure {
    private static final Holder<StructureProcessorList> NONE = Holder.direct(new StructureProcessorList(List.of()));

    public static final MapCodec<NetherSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            StructureProcessorType.LIST_CODEC.optionalFieldOf("processors", NONE).forGetter(s -> s.processors),
            Codec.intRange(0, 256).optionalFieldOf("from", 33).forGetter(s -> s.from),
            Codec.intRange(0, 256).optionalFieldOf("roof", 120).forGetter(s -> s.roof),
            Codec.intRange(1, 64).optionalFieldOf("headroom", 8).forGetter(s -> s.headroom),
            SiteSwitch.OPTION.optionalFieldOf("enabled_by").forGetter(s -> s.enabledBy)
    ).apply(instance, NetherSiteStructure::new));

    private final ResourceLocation template;
    private final Holder<StructureProcessorList> processors;
    private final int from;
    private final int roof;
    private final int headroom;
    private final Optional<String> enabledBy;

    public NetherSiteStructure(StructureSettings settings, ResourceLocation template) {
        this(settings, template, NONE, 33, 120, 8, Optional.empty());
    }

    public NetherSiteStructure(StructureSettings settings, ResourceLocation template,
            Holder<StructureProcessorList> processors, int from, int roof, int headroom, Optional<String> enabledBy) {
        super(settings);
        this.template = template;
        this.processors = processors;
        this.from = from;
        this.roof = roof;
        this.headroom = headroom;
        this.enabledBy = enabledBy;
    }

    public ResourceLocation template() {
        return template;
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
        ChunkPos chunk = context.chunkPos();
        NoiseColumn column = context.chunkGenerator().getBaseColumn(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                context.heightAccessor(), context.randomState());
        OptionalInt floor = floor(column::getBlock, from, roof, headroom, size.getY());
        if (floor.isEmpty()) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(chunk.getMiddleBlockX(), floor.getAsInt() + 1, chunk.getMiddleBlockZ());
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(
                piece(context.structureTemplateManager(), center, size))));
    }

    /** The site's one piece: its layer 0 at {@code center} (the middle of its footprint), unturned. */
    public TemplateSitePiece piece(StructureTemplateManager manager, BlockPos center, Vec3i size) {
        return new TemplateSitePiece(manager, template, processors, Rotation.NONE, center, size, 0);
    }

    /**
     * The cavern floor a site of {@code height} layers stands on in a column (block by y): the first solid block from
     * {@code from} up that is not a fluid and has {@code headroom} layers of air (no lava) over it; empty when there is
     * none under the roof, or when the site set on it would pass {@code roof}.
     */
    public static OptionalInt floor(IntFunction<BlockState> column, int from, int roof, int headroom, int height) {
        for (int y = from; y + headroom < roof; y++) {
            BlockState floor = column.apply(y);
            if (floor.isAir() || !floor.getFluidState().isEmpty()) {
                continue;
            }
            boolean room = true;
            for (int up = 1; up <= headroom && room; up++) {
                BlockState above = column.apply(y + up);
                room = above.isAir() && above.getFluidState().isEmpty();
            }
            if (room) {
                return y + 1 + height > roof ? OptionalInt.empty() : OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.NETHER_SITE.get();
    }
}
