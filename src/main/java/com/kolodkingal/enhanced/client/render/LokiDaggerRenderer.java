package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Loki's dagger in flight: its flat item sprite turned to point along its path, like an arrow or a thrown trident,
 * blade first.
 */
public class LokiDaggerRenderer extends EntityRenderer<LokiDaggerEntity> {
    /** The sprite is 16 pixels long: about half a block in flight. */
    private static final float SCALE = 0.55F;

    private final ItemRenderer itemRenderer;

    public LokiDaggerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(LokiDaggerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Not in the thrower's face on the tick it leaves his hand.
        if (entity.tickCount < 2 && entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
        // As for arrows: after these two turns the sprite's +X axis points along the flight.
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        // Two sprites crossed along the blade (like an arrow's), so it is seen from every side. The sprite lies flat,
        // its tip at the right (+X).
        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F * i));
            poseStack.scale(SCALE, SCALE, SCALE);
            itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                    entity.level(), entity.getId());
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LokiDaggerEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
