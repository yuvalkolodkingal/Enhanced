package com.kolodkingal.enhanced.client.nano;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.nano.NanoUltronEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Ultron in nanotech (3.12): his copper and orange body; in his nano form, a see-through shimmer of particles. */
public class NanoUltronRenderer extends MobRenderer<NanoUltronEntity, PlayerModel<NanoUltronEntity>> {
    private static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/mind/nano_ultron.png");

    public NanoUltronRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(NanoUltronEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(NanoUltronEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.1F, 1.1F, 1.1F);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(NanoUltronEntity entity, boolean visible, boolean translucent, boolean glowing) {
        return entity.nano() ? RenderType.itemEntityTranslucentCull(TEXTURE) : super.getRenderType(entity, visible, translucent, glowing);
    }

    @Override
    public void render(NanoUltronEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        model.setAllVisible(true);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
