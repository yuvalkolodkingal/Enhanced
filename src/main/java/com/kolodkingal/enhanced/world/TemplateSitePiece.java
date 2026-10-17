package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;

/**
 * The one piece of a {@link TemplateSiteStructure}: its template, turned about the middle of its footprint, with its
 * {@code floor} layer at ground level. It remembers its processors (by name or as written in the structure), so a chunk
 * generated after a restart builds its part the same way.
 */
public class TemplateSitePiece extends TemplateStructurePiece {

    public TemplateSitePiece(StructureTemplateManager manager, ResourceLocation template, Holder<StructureProcessorList> processors,
            Rotation rotation, BlockPos center, Vec3i size, int floor) {
        super(ModStructures.TEMPLATE_SITE_PIECE.get(), 0, manager, template, template.toString(),
                settings(rotation, pivot(size), processors.value()),
                center.offset(-size.getX() / 2, -floor, -size.getZ() / 2));
        this.processors = processors;
    }

    public TemplateSitePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.TEMPLATE_SITE_PIECE.get(), tag, context.structureTemplateManager(),
                id -> settings(Rotation.valueOf(tag.getString("Rot")), BlockPos.of(tag.getLong("Pivot")),
                        readProcessors(context.registryAccess(), tag).value()));
        this.processors = readProcessors(context.registryAccess(), tag);
    }

    private final Holder<StructureProcessorList> processors;

    private static BlockPos pivot(Vec3i size) {
        return new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
    }

    private static StructurePlaceSettings settings(Rotation rotation, BlockPos pivot, StructureProcessorList processors) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE).setRotationPivot(pivot)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        processors.list().forEach(settings::addProcessor);
        return settings;
    }

    private static Holder<StructureProcessorList> readProcessors(RegistryAccess registries, CompoundTag tag) {
        return StructureProcessorType.LIST_CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag.get("Processors"))
                .resultOrPartial(error -> Enhanced.LOGGER.error("A structure piece's processors could not be read: {}", error))
                .orElseGet(() -> Holder.direct(new StructureProcessorList(List.of())));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
        tag.putLong("Pivot", placeSettings.getRotationPivot().asLong());
        Tag written = StructureProcessorType.LIST_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, context.registryAccess()), processors)
                .resultOrPartial(error -> Enhanced.LOGGER.error("A structure piece's processors could not be written: {}", error))
                .orElse(null);
        if (written != null) {
            tag.put("Processors", written);
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }
}
