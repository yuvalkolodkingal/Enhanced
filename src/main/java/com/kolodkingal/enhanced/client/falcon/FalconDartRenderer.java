package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.falcon.FalconDartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * One of Falcon's energy darts (3.8): a bright red streak with a white-hot core and a little ring at its head, all
 * additive geometry facing the camera (as Deadpool's tracers are drawn). Energy, not a bullet (PLAN 8.4).
 */
public class FalconDartRenderer extends EntityRenderer<FalconDartEntity> {
    private static final int CORE = 0xFFF0F0;
    private static final int MAIN = 0xFF3A44;
    private static final int HALO = 0xC0101C;
    private static final ResourceLocation NO_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public FalconDartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FalconDartEntity dart) {
        return NO_TEXTURE;
    }

    @Override
    public boolean shouldRender(FalconDartEntity dart, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(FalconDartEntity dart, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 camera = entityRenderDispatcher.camera.getPosition();
        Vec3 centre = dart.getPosition(partialTick).add(0, dart.getBbHeight() * 0.5, 0).subtract(camera);
        Vec3 motion = dart.getDeltaMovement();
        Vec3 back = motion.lengthSqr() < 1.0E-6 ? Vec3.ZERO
                : motion.normalize().scale(-Math.min(2.2, Math.max(0.8, motion.length() * 0.7)));
        Vec3 tail = centre.add(back);
        Matrix4f pose = new Matrix4f();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        float flicker = 0.9F + 0.1F * Mth.sin((dart.tickCount + partialTick) * 2.7F + dart.getId());
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.16F, EnergyGeometry.argb(HALO, 0.0F),
                EnergyGeometry.argb(HALO, 0.5F * flicker));
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.09F, EnergyGeometry.argb(MAIN, 0.0F),
                EnergyGeometry.argb(MAIN, 0.95F * flicker));
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.04F, EnergyGeometry.argb(CORE, 0.0F),
                EnergyGeometry.argb(CORE, 1.0F));
        Vec3 toCamera = centre.scale(-1.0);
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.08F, 0.07F, 10, EnergyGeometry.argb(MAIN, 0.9F));
    }
}
