package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.strange.SlingPortalEntity;
import com.kolodkingal.enhanced.strange.StrangeAbilities;
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
 * A Sling-Ring portal (3.6): a ring of orange spark streaks spinning round a dark-gold swirl, the far side never rendered
 * (no render-to-texture: the swirl stands for it). It opens from a point over its first ticks and closes into one over
 * its last, both sides seen alike. The streaks are {@code textures/fx/sling_portal.png}, the swirl's arms the Dome's
 * mandala turned dark gold.
 */
public class SlingPortalRenderer extends EntityRenderer<SlingPortalEntity> {
    public static final ResourceLocation RIM = Enhanced.id("textures/fx/sling_portal.png");
    /** The swirl's colours: a deep gold-brown inside, the faint gold of its arms. */
    private static final int SWIRL = 0x2A1806;
    private static final int ARMS = 0x8A5A1C;
    private static final int SEGMENTS = 32;

    public SlingPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(SlingPortalEntity portal, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(portal.getBoundingBox().inflate(0.5));
    }

    /** How open the ring is at {@code age} of its {@code life} (0 shut, 1 open): eased in and out over six ticks. */
    public static float openness(float age, float life) {
        float in = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        float out = Mth.clamp((life - age) / 6.0F, 0.0F, 1.0F);
        float o = Math.min(in, out);
        return o * o * (3.0F - 2.0F * o);
    }

    @Override
    public void render(SlingPortalEntity portal, float yaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        float age = portal.tickCount + partialTicks;
        float open = openness(age, portal.life());
        if (open <= 0.01F) {
            return;
        }
        boolean calm = EnhancedClientConfig.reducedMotion();
        float spin = calm ? 0.0F : age;
        float r = (float) SlingPortalEntity.RADIUS * open;
        Vec3 center = new Vec3(0, SlingPortalEntity.CENTER_UP, 0);
        Vec3 normal = portal.normal();
        Matrix4f pose = poseStack.last().pose();
        // The dark-gold swirl inside (it hides most of what is behind).
        VertexConsumer veil = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        Vec3[] uv = StrangeSpellFx.basis(normal);
        for (int s = 0; s < SEGMENTS; s++) {
            double a0 = Math.PI * 2 * s / SEGMENTS;
            double a1 = Math.PI * 2 * (s + 1) / SEGMENTS;
            Vec3 p0 = center.add(uv[0].scale(Math.cos(a0) * r)).add(uv[1].scale(Math.sin(a0) * r));
            Vec3 p1 = center.add(uv[0].scale(Math.cos(a1) * r)).add(uv[1].scale(Math.sin(a1) * r));
            int mid = EnergyGeometry.argb(SWIRL, 0.82F * open);
            int edge = EnergyGeometry.argb(ARMS, 0.55F * open);
            veil.addVertex(pose, (float) center.x, (float) center.y, (float) center.z).setColor(mid);
            veil.addVertex(pose, (float) center.x, (float) center.y, (float) center.z).setColor(mid);
            veil.addVertex(pose, (float) p0.x, (float) p0.y, (float) p0.z).setColor(edge);
            veil.addVertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).setColor(edge);
        }
        // The swirl's turning arms.
        VertexConsumer arms = buffers.getBuffer(EnhancedRenderTypes.energyField(StrangeSpellFx.DOME_TEX));
        StrangeSpellFx.disc(pose, arms, center, normal, r * 1.0F, -spin * 0.05F, EnergyGeometry.argb(ARMS, 0.6F * open));
        StrangeSpellFx.disc(pose, arms, center, normal, r * 0.6F, spin * 0.09F, EnergyGeometry.argb(ARMS, 0.5F * open));
        // The rim of sparks: two rings of streaks turning against each other, white-gold at the heart of it.
        VertexConsumer rim = buffers.getBuffer(EnhancedRenderTypes.energyField(RIM));
        float rq = r / 0.92F;
        StrangeSpellFx.disc(pose, rim, center, normal, rq, spin * 0.35F, EnergyGeometry.argb(StrangeAbilities.ORANGE,
                1.0F));
        StrangeSpellFx.disc(pose, rim, center, normal, rq * 1.03F, -spin * 0.22F + 1.0F,
                EnergyGeometry.argb(StrangeAbilities.ORANGE, 0.8F));
        StrangeSpellFx.disc(pose, rim, center, normal, rq * 0.98F, spin * 0.5F + 2.0F,
                EnergyGeometry.argb(StrangeAbilities.GOLD, 0.7F));
        super.render(portal, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SlingPortalEntity portal) {
        return RIM;
    }
}
