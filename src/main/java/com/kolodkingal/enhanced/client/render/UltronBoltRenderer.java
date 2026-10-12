package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A bolt of Ultron's plasma, in the colours of whoever fired it (Prime's amber with a red edge, a Sentry's cyan, the
 * human Ultron's red): a white core, a halo, and a trail along where it has been, all additive. The heavy blast of the
 * Overcharge is thicker and wears a crown of short arcs. Nothing is textured: it is geometry facing the camera.
 */
public class UltronBoltRenderer extends EntityRenderer<UltronBoltEntity> {
    /** What a palette is drawn in: the core, the body, the halo and the edge, as 0xRRGGBB. */
    private record Colors(int core, int main, int halo, int edge) {
    }

    private static final Colors PRIME = new Colors(0xFFF3D6, 0xFFA23A, 0xFF7A1A, 0xE5322B);
    private static final Colors SENTRY = new Colors(0xEAF8FF, 0x45B4F5, 0x2F7FC4, 0x2F7FC4);
    private static final Colors HUMAN = new Colors(0xFFEDE8, 0xE23B2E, 0x9A2018, 0x9A2018);
    /** How long the trail is, in blocks, at a bolt's usual speed. */
    private static final double TRAIL = 2.4;
    /** The bolt has nothing to texture, but an entity renderer must name a texture. */
    private static final ResourceLocation NO_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public UltronBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(UltronBoltEntity bolt) {
        return NO_TEXTURE;
    }

    @Override
    public boolean shouldRender(UltronBoltEntity bolt, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(UltronBoltEntity bolt, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Colors colors = switch (bolt.palette()) {
            case SENTRY -> SENTRY;
            case HUMAN -> HUMAN;
            default -> PRIME;
        };
        Vec3 camera = entityRenderDispatcher.camera.getPosition();
        Vec3 centre = bolt.getPosition(partialTick).add(0, bolt.getBbHeight() * 0.5, 0).subtract(camera);
        Vec3 motion = bolt.getDeltaMovement();
        Vec3 back = motion.lengthSqr() < 1.0E-6 ? Vec3.ZERO : motion.normalize().scale(-Math.min(TRAIL * (bolt.isHeavy() ? 1.5 : 1.0), Math.max(1.2, motion.length() * 1.6)));
        float size = bolt.isHeavy() ? 2.4F : 1.0F;
        Matrix4f pose = new Matrix4f();
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        float flicker = 0.9F + 0.1F * Mth.sin((bolt.tickCount + partialTick) * 1.7F + bolt.getId());
        Vec3 tail = centre.add(back);
        // The trail: a halo ribbon, a body ribbon and a hot thin one, all thinning to nothing at the tail.
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.34F * size, EnergyGeometry.argb(colors.halo(), 0.0F), EnergyGeometry.argb(colors.halo(), 0.55F * flicker));
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.20F * size, EnergyGeometry.argb(colors.edge(), 0.0F), EnergyGeometry.argb(colors.main(), 0.8F * flicker));
        EnergyGeometry.ribbon(pose, vc, tail, centre, 0.0F, 0.09F * size, EnergyGeometry.argb(colors.core(), 0.0F), EnergyGeometry.argb(colors.core(), 0.95F));
        // The head: a halo, a body and a core, facing the camera.
        Vec3 toCamera = centre.scale(-1.0);
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.28F * size, 0.2F * size, 12, EnergyGeometry.argb(colors.halo(), 0.5F * flicker));
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.16F * size, 0.16F * size, 12, EnergyGeometry.argb(colors.main(), 0.85F));
        EnergyGeometry.ring(pose, vc, centre, toCamera, 0.07F * size, 0.1F * size, 10, EnergyGeometry.argb(colors.core(), 1.0F));
        if (bolt.isHeavy()) {
            // A crown of short arcs round the blast, changing every other tick.
            RandomSource random = RandomSource.create((long) ((bolt.tickCount + partialTick) / 2.0F) * 7919L + bolt.getId());
            for (int i = 0; i < 4; i++) {
                Vec3 end = centre.add((random.nextDouble() - 0.5) * 1.5, (random.nextDouble() - 0.5) * 1.5, (random.nextDouble() - 0.5) * 1.5);
                EnergyGeometry.bolt(pose, vc, centre, end, 0.05F, 0.12F, EnergyGeometry.argb(colors.main(), 0.9F), random.nextLong());
                EnergyGeometry.bolt(pose, vc, centre, end, 0.02F, 0.12F, EnergyGeometry.argb(colors.core(), 0.95F), random.nextLong());
            }
        }
    }

}
