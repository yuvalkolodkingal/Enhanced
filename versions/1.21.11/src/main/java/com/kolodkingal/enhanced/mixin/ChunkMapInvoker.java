package com.kolodkingal.enhanced.mixin;
//#norenames

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.21.11: the chunks a level holds (Build Magic walks them, BuildJob): {@code ChunkMap.getChunks} is gone, its map of
 * visible chunks is what it gave.
 */
@Mixin(ChunkMap.class)
public interface ChunkMapInvoker {
    @Accessor("visibleChunkMap")
    Long2ObjectLinkedOpenHashMap<ChunkHolder> enhanced$chunkMap();
}
