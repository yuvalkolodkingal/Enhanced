package com.kolodkingal.enhanced.client.deadpool;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

/**
 * Dogpool (3.7) on the vanilla wolf's model in his own skin ({@code textures/entity/dogpool.png}: a red-and-black hood
 * and goggles). The 3.7 skeleton's stand-in with a placeholder skin; lane L3 owns the class and the skin.
 */
public class DogpoolRenderer extends WolfRenderer {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/dogpool.png");

    public DogpoolRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Wolf wolf) {
        return TEXTURE;
    }
}
