package com.kolodkingal.enhanced.client.nano;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.nano.NanoInfectedEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** A person the nanotech has taken (3.12): grey skin, plain clothes, orange threads. */
public class NanoInfectedRenderer extends MobRenderer<NanoInfectedEntity, PlayerModel<NanoInfectedEntity>> {
    private static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/mind/nano_infected.png");

    public NanoInfectedRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NanoInfectedEntity entity) {
        return TEXTURE;
    }
}
