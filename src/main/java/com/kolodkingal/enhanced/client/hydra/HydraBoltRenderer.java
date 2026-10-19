package com.kolodkingal.enhanced.client.hydra;

import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.hydra.HydraBoltEntity;
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
 * A Hydra energy bolt (3.6, lane L8): a short Tesseract-blue streak (0x4FB8FF) with a white-blue core (0xDDF4FF), all
 * additive geometry facing the camera, as Ultron's bolts are drawn. The sniper's is longer and brighter, the pistol's
 * smaller.
 */
public class HydraBoltRenderer extends EntityRenderer<HydraBoltEntity> {
    private static final int CORE = 0xDDF4FF;
    private static final int MAIN = 0x4FB8FF;
    private static final int HALO = 0x2A7BD6;
    private static final ResourceLocation NO_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public HydraBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(HydraBoltEntity bolt) {
        return NO_TEXTURE;
    }

    @Override
    public boolean shouldRender(HydraBoltEntity bolt, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(HydraBoltEntity bolt, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float size = switch (bolt.kind()) {
            case HydraBoltEntity.SNIPER -> 1.25F;
            case HydraBoltEntity.PISTOL -> 0.75F;
            default -> 1.0F;
        };
        double trail = bolt.kind() == HydraBoltEntity.SNIPER ? 3.2 : 1.6;
        Vec3 camera = entityRenderDispatcher.camera.getPosition();
        Vec3 centre = bolt.getPosition(partialTick).add(0, bolt.getBbHeight() * 0.5, 0).subtract(camera);
        Vec3 motion = bolt.getDeltaMovement();
        Vec3 back = motion.lengthSqr() < 1.0E-6 ? Vec3.ZERO
                : motion.normalize().scale(-Math.min(trail, Math.max(0.8, motion.length() * 1.2)));
        Vec3 tail = centre.add(back);
        Matrix4f pose = new Matrix4f();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        float flicker = 0.9F + 0.1F * Mth.sin((bolt.tickCount + partialTick) * 1.9F + bolt.getId());
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.24F * size, EnergyGeometry.argb(HALO, 0.0F),
                EnergyGeometry.argb(HALO, 0.5F * flicker));
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.13F * size, EnergyGeometry.argb(MAIN, 0.0F),
                EnergyGeometry.argb(MAIN, 0.85F * flicker));
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.055F * size, EnergyGeometry.argb(CORE, 0.0F),
                EnergyGeometry.argb(CORE, 0.95F));
        Vec3 toCamera = centre.scale(-1.0);
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.17F * size, 0.13F * size, 12,
                EnergyGeometry.argb(HALO, 0.45F * flicker));
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.1F * size, 0.1F * size, 10, EnergyGeometry.argb(MAIN, 0.9F));
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.045F * size, 0.06F * size, 8,
                EnergyGeometry.argb(CORE, 1.0F));
    }
}
