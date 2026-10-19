package com.kolodkingal.enhanced.client.hydra;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * The 3.6 skeleton's stand-in look of the Hydra soldiers and officers: a plain humanoid in a black placeholder skin.
 * Lane L8 replaces it with its own model and renderers ({@code HydraSoldierModel}, {@code HydraSoldierRenderer}) and
 * deletes it.
 */
public class HydraPlaceholderRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/hydra/placeholder.png");

    public HydraPlaceholderRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
