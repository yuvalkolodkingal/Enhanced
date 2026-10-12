package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The sphere of the Reality Stone: a translucent red globe with a white-pink heart, a halo that breathes and a few
 * motes circling it. Slow enough to see coming and to step out of.
 */
public class ThanosOrbRenderer extends EntityRenderer<ThanosOrbEntity> {
    private static final int RED = StoneKind.REALITY.color();
    private static final int CORE = StoneKind.REALITY.core();

    public ThanosOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ThanosOrbEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
        Matrix4f m = poseStack.last().pose();
        float pulse = 0.9F + 0.1F * Mth.sin(age * 0.8F);
        VertexConsumer veil = buffer.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        EnergyGeometry.veil(m, veil, Vec3.ZERO, 0.5F * pulse, 16, 8, RED, 0.35F, 0.55F);
        VertexConsumer glow = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        EnergyGeometry.veil(m, glow, Vec3.ZERO, 0.28F, 12, 6, CORE, 0.7F, 0.3F);
        EnergyGeometry.veil(m, glow, Vec3.ZERO, 0.75F * pulse, 14, 7, RED, 0.06F, 0.25F);
        // Motes circling it.
        for (int i = 0; i < 4; i++) {
            float a = age * 0.45F + i * Mth.HALF_PI;
            float tilt = i * 0.7F;
            Vec3 at = new Vec3(Mth.cos(a) * 0.7F, Mth.sin(a) * 0.7F * Mth.sin(tilt + 0.5F), Mth.sin(a) * 0.7F * Mth.cos(tilt + 0.5F));
            EnergyGeometry.veil(m, glow, at, 0.07F, 6, 3, CORE, 0.8F, 0.2F);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThanosOrbEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
