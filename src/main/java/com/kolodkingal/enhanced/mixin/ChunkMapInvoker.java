package com.kolodkingal.enhanced.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code ChunkMap.getChunks}, which is not public: Build Magic walks the chunks a level holds (BuildJob). */
@Mixin(ChunkMap.class)
public interface ChunkMapInvoker {
    @Invoker("getChunks")
    Iterable<ChunkHolder> enhanced$getChunks();
}
