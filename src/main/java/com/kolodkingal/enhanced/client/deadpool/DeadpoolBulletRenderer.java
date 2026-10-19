package com.kolodkingal.enhanced.client.deadpool;

import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.deadpool.DeadpoolBulletEntity;
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
 * One of Deadpool's cartoon bullets (3.7): a bright gold tracer streak with a white-hot core, all additive geometry
 * facing the camera, as the Hydra bolts are drawn; the Trick Shot's round is longer and brighter. No bullet is drawn as
 * a real one (PLAN 5.1).
 */
public class DeadpoolBulletRenderer extends EntityRenderer<DeadpoolBulletEntity> {
    private static final int CORE = 0xFFF8E0;
    private static final int MAIN = 0xFFD24A;
    private static final int HALO = 0xFF8A1E;
    private static final ResourceLocation NO_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public DeadpoolBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DeadpoolBulletEntity bullet) {
        return NO_TEXTURE;
    }

    @Override
    public boolean shouldRender(DeadpoolBulletEntity bullet, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(DeadpoolBulletEntity bullet, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float size = bullet.trick() ? 1.2F : 0.8F;
        double trail = bullet.trick() ? 2.6 : 1.8;
        Vec3 camera = entityRenderDispatcher.camera.getPosition();
        Vec3 centre = bullet.getPosition(partialTick).add(0, bullet.getBbHeight() * 0.5, 0).subtract(camera);
        Vec3 motion = bullet.getDeltaMovement();
        Vec3 back = motion.lengthSqr() < 1.0E-6 ? Vec3.ZERO
                : motion.normalize().scale(-Math.min(trail, Math.max(0.8, motion.length() * 0.6)));
        Vec3 tail = centre.add(back);
        Matrix4f pose = new Matrix4f();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        float flicker = 0.9F + 0.1F * Mth.sin((bullet.tickCount + partialTick) * 2.3F + bullet.getId());
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.18F * size, EnergyGeometry.argb(HALO, 0.0F),
                EnergyGeometry.argb(HALO, 0.45F * flicker));
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.10F * size, EnergyGeometry.argb(MAIN, 0.0F),
                EnergyGeometry.argb(MAIN, 0.9F * flicker));
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.045F * size, EnergyGeometry.argb(CORE, 0.0F),
                EnergyGeometry.argb(CORE, 1.0F));
        Vec3 toCamera = centre.scale(-1.0);
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.09F * size, 0.08F * size, 10,
                EnergyGeometry.argb(MAIN, 0.9F));
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.04F * size, 0.05F * size, 8,
                EnergyGeometry.argb(CORE, 1.0F));
    }
}
