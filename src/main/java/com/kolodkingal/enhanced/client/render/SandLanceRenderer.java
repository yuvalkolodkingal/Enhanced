package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.kolodkingal.enhanced.client.render.EnergyGeometry.argb;

/**
 * One of the five sand lances: a thin ochre streak along its path, a bright core and a wider haze round it, pointing the
 * way it flies. The grains it sheds are laid by {@link SandFx}, once a tick.
 */
public class SandLanceRenderer extends EntityRenderer<SandLanceEntity> {
    /** How long the streak is, in blocks. */
    private static final double LENGTH = 1.9;

    public SandLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SandLanceEntity lance, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Not on the tick it leaves his hand (a lance right in front of the camera is a wall of ochre).
        if (lance.tickCount < 1 && entityRenderDispatcher.camera.getPosition().distanceToSqr(lance.position()) < 4.0) {
            return;
        }
        Vec3 camera = entityRenderDispatcher.camera.getPosition();
        Vec3 head = lance.getPosition(partialTicks).add(0.0, lance.getBbHeight() * 0.5, 0.0).subtract(camera);
        Vec3 dir = lance.getDeltaMovement();
        if (dir.lengthSqr() < 1.0E-4) {
            dir = Vec3.directionFromRotation(lance.getXRot(), lance.getYRot());
        }
        Vec3 tail = head.subtract(dir.normalize().scale(LENGTH));
        Matrix4f pose = new Matrix4f();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        EnergyGeometry.ribbon(pose, vc, tail, head, 0.0F, 0.20F, argb(SandPalette.MAIN, 0.0F), argb(SandPalette.SHADE, 0.30F));
        EnergyGeometry.ribbon(pose, vc, tail, head, 0.0F, 0.09F, argb(SandPalette.MAIN, 0.0F), argb(SandPalette.MAIN, 0.95F));
        EnergyGeometry.ribbon(pose, vc, tail.lerp(head, 0.35), head, 0.0F, 0.035F, argb(SandPalette.LIGHT, 0.0F), argb(SandPalette.LIGHT, 1.0F));
        super.render(lance, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SandLanceEntity lance) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
