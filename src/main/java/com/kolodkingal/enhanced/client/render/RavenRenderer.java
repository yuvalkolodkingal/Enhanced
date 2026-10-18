package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.odin.RavenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Huginn and Muninn in flight (3.5): {@link RavenModel}, with a small shadow far below. */
public class RavenRenderer extends MobRenderer<RavenEntity, RavenModel> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/raven.png");

    public RavenRenderer(EntityRendererProvider.Context context) {
        super(context, new RavenModel(context.bakeLayer(RavenModel.LAYER)), 0.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(RavenEntity entity) {
        return TEXTURE;
    }
}
