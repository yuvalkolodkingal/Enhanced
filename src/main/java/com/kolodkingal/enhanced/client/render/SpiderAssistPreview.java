package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SwingAssist;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.SpiderMotion;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The preview of Web Assist (3.3): where the local Spider-Man's next web would go, as a small white web facing the
 * camera (a ring, six spokes and a bright centre; pale blue for a point of the sky), for him alone. It eases from one
 * anchor to the next near it, fades out and back in where the next is far, and is drawn between ticks where it really
 * is (nothing snaps or blinks). See {@link SpiderMotion#preview}.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class SpiderAssistPreview {
    private static final int WHITE = 0xFFFFFF;
    private static final int SILVER = 0xDDE8F2;
    private static final int SKY = 0xA9D4FF;
    /** How far (blocks) the shown web may glide to the next anchor; further, it fades out and reappears there. */
    private static final double GLIDE = 3.0;
    /** Its share of the way to the anchor each tick, and how fast it fades in and out. */
    private static final double EASE = 0.5;
    private static final float FADE = 0.25F;

    @Nullable
    private static Vec3 at;
    @Nullable
    private static Vec3 atOld;
    private static float alpha;
    private static float alphaOld;
    private static boolean sky;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            at = null;
            atOld = null;
            alpha = 0.0F;
            alphaOld = 0.0F;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        atOld = at;
        alphaOld = alpha;
        SwingAssist.Anchor target = SpiderMotion.preview();
        if (target == null) {
            alpha = Math.max(0.0F, alpha - FADE);
            return;
        }
        Vec3 point = target.point();
        if (at == null || alpha <= 0.02F) {
            // (Hidden: it appears where the anchor is, and fades in there.)
            at = point;
            atOld = point;
            sky = target.kind() == SwingAssist.Kind.SKY;
            alpha = Math.min(1.0F, alpha + FADE);
        } else if (at.distanceTo(point) > GLIDE) {
            alpha = Math.max(0.0F, alpha - FADE * 1.5F);
        } else {
            at = at.lerp(point, EASE);
            sky = target.kind() == SwingAssist.Kind.SKY;
            alpha = Math.min(1.0F, alpha + FADE);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || at == null || (alpha <= 0.01F && alphaOld <= 0.01F)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 point = atOld == null ? at : atOld.lerp(at, partialTick);
        float shown = Mth.lerp(partialTick, alphaOld, alpha);
        if (shown <= 0.01F) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        Vec3 centre = point.subtract(camera);
        double distance = centre.length();
        if (distance < 0.5) {
            return;
        }
        float time = mc.level.getGameTime() + partialTick;
        float breathe = EnhancedClientConfig.reducedMotion() ? 1.0F : 1.0F + 0.06F * Mth.sin(time * 0.25F);
        float radius = (float) (0.22 + distance * 0.012) * breathe;
        float width = (float) (0.035 + distance * 0.0016);
        int main = sky ? SKY : SILVER;
        Vec3 facing = centre.scale(-1.0 / distance);
        Vec3 u = facing.cross(new Vec3(0, 1, 0));
        u = u.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = facing.cross(u).normalize();

        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        EnergyGeometry.ring(pose, vc, centre, facing, radius, width * 2.2F, 20, EnergyGeometry.argb(main, 0.3F * shown));
        EnergyGeometry.ring(pose, vc, centre, facing, radius, width, 20, EnergyGeometry.argb(WHITE, 0.85F * shown));
        EnergyGeometry.ring(pose, vc, centre, facing, radius * 0.5F, width * 0.7F, 14, EnergyGeometry.argb(main, 0.6F * shown));
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * i / 6.0 + Math.PI / 6.0;
            Vec3 spoke = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            EnergyGeometry.ribbon(pose, vc, centre.add(spoke.scale(radius * 0.15)), centre.add(spoke.scale(radius * 1.3)), width * 0.8F,
                    width * 0.4F, EnergyGeometry.argb(WHITE, 0.8F * shown), EnergyGeometry.argb(main, 0.25F * shown));
        }
        EnergyGeometry.ribbon(pose, vc, centre.subtract(u.scale(radius * 0.12)), centre.add(u.scale(radius * 0.12)), radius * 0.24F,
                EnergyGeometry.argb(WHITE, shown), EnergyGeometry.argb(WHITE, shown));
        buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
    }

    private SpiderAssistPreview() {
    }
}
