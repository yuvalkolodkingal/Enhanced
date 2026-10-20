package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.falcon.RedwingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Redwing (3.8): its model ({@link RedwingModel}), its eye and thruster glowing red, and, for a moment after each zap,
 * a thin red beam from its eye to what it zapped ({@link FalconEffects#zap}), additive and facing the camera.
 */
public class RedwingRenderer extends MobRenderer<RedwingEntity, RedwingModel> {
    private static final int BEAM = 0xFF2A36;
    private static final int CORE = 0xFFE8E8;

    public RedwingRenderer(EntityRendererProvider.Context context) {
        super(context, new RedwingModel(context.bakeLayer(RedwingModel.LAYER)), 0.2F);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RedwingEntity drone) {
        return RedwingModel.TEXTURE;
    }

    @Override
    public void render(RedwingEntity drone, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        super.render(drone, entityYaw, partialTick, poseStack, buffer, packedLight);
        FalconEffects.Zap zap = FalconEffects.zap(drone.getId(), drone.level().getGameTime());
        if (zap == null) {
            return;
        }
        Vec3 camera = entityRenderDispatcher.camera.getPosition();
        Vec3 from = drone.getPosition(partialTick).add(0, drone.getBbHeight() * 0.5, 0).subtract(camera);
        Vec3 to = zap.to().subtract(camera);
        float age = drone.level().getGameTime() - zap.at() + partialTick;
        float fade = Math.max(0.0F, 1.0F - age / (FalconEffects.ZAP_TICKS + 1.0F));
        Matrix4f pose = new Matrix4f();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        EnergyGeometry.ribbon(pose, vc, from, to, 0.12F, EnergyGeometry.argb(BEAM, 0.6F * fade),
                EnergyGeometry.argb(BEAM, 0.6F * fade));
        EnergyGeometry.ribbon(pose, vc, from, to, 0.04F, EnergyGeometry.argb(CORE, fade),
                EnergyGeometry.argb(CORE, fade));
    }

    /** The bright pass of the eye and the thruster, drawn by the layer below. */
    static final class Glow extends net.minecraft.client.renderer.entity.layers.RenderLayer<RedwingEntity,
            RedwingModel> {
        Glow(RedwingRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, RedwingEntity drone,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            getParentModel().renderGlow(poseStack, buffer.getBuffer(RenderType.eyes(RedwingModel.TEXTURE)),
                    0xF000F0, OverlayTexture.NO_OVERLAY);
        }
    }
}
