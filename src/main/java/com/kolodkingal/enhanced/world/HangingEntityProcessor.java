package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Set;

/**
 * Hangs a template's item frames and paintings on the block they are placed at (3.7.1, {@code enhanced:hanging_entities}).
 * Our templates write them without {@code TileX}/{@code TileY}/{@code TileZ}, so Minecraft read each as hung at 0, 0, 0
 * and logged "Block-attached entity at invalid position" (13 times for the Avengers Tower) before moving it to where it
 * belongs; with its block written in, it is read where it is put. Every {@link TemplateSitePiece} places with it.
 */
public final class HangingEntityProcessor extends StructureProcessor {
    public static final HangingEntityProcessor INSTANCE = new HangingEntityProcessor();
    public static final MapCodec<HangingEntityProcessor> CODEC = MapCodec.unit(() -> INSTANCE);
    /** The entities that hang on a block. */
    private static final Set<String> HANGING = Set.of("minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:painting");

    private HangingEntityProcessor() {
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.HANGING_ENTITIES.get();
    }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader level, BlockPos seedPos,
            StructureTemplate.StructureEntityInfo rawEntityInfo, StructureTemplate.StructureEntityInfo entityInfo,
            StructurePlaceSettings settings, StructureTemplate template) {
        if (!HANGING.contains(entityInfo.nbt.getString("id"))) {
            return entityInfo;
        }
        // The block the entity is put in (as it takes it when it is moved there), not the template's own corner.
        BlockPos block = BlockPos.containing(entityInfo.pos);
        CompoundTag nbt = entityInfo.nbt.copy();
        nbt.putInt("TileX", block.getX());
        nbt.putInt("TileY", block.getY());
        nbt.putInt("TileZ", block.getZ());
        return new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, nbt);
    }
}
