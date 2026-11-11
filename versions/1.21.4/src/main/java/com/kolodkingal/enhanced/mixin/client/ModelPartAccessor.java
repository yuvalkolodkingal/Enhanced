package com.kolodkingal.enhanced.mixin.client;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

/** 1.21.4: a model part's cubes and children, to rebuild a 1.21.1 humanoid mesh (compat.Meshes). */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("cubes")
    List<ModelPart.Cube> enhanced$cubes();

    @Accessor("children")
    Map<String, ModelPart> enhanced$children();
}
