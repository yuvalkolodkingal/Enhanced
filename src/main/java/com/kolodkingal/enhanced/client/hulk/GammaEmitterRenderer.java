package com.kolodkingal.enhanced.client.hulk;

import com.kolodkingal.enhanced.block.GammaEmitterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * The Gamma Emitter's light (3.6): while it warms up, a gamma-green glow swells at its lens; while it beams, a
 * translucent green column runs {@code beamLength} blocks out of its face (the beacon's beam laid on its side, in gamma
 * green). Its phase and when it began come from the block entity's synced data.
 */
public class GammaEmitterRenderer implements BlockEntityRenderer<GammaEmitterBlockEntity> {
    /** Gamma green, opaque, and the warm-up's paler glow. */
    private static final int BEAM = 0xFF7CFF4A;

    public GammaEmitterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GammaEmitterBlockEntity emitter, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int light,
                       int overlay) {
        if (emitter.getLevel() == null || emitter.phase() == GammaEmitterBlockEntity.IDLE) {
            return;
        }
        long time = emitter.getLevel().getGameTime();
        float t = time - emitter.phaseStart() + partialTick;
        Direction facing = emitter.facing();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(facing.getRotation());
        poseStack.translate(-0.5, 0.45, -0.5);
        if (emitter.phase() == GammaEmitterBlockEntity.WARMUP) {
            float k = Mth.clamp(t / Math.max(1, GammaEmitterBlockEntity.warmupTicks()), 0.0F, 1.0F);
            float pulse = 0.5F + 0.5F * Mth.sin(t * (0.3F + 0.7F * k));
            int alpha = (int) (80 + 150 * k * pulse);
            BeaconRenderer.renderBeaconBeam(poseStack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F, time,
                    0, 1,
                    (alpha << 24) | (BEAM & 0xFFFFFF), 0.04F + 0.12F * k, 0.08F + 0.2F * k * pulse);
        } else {
            BeaconRenderer.renderBeaconBeam(poseStack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F, time,
                    0,
                    GammaEmitterBlockEntity.beamLength(), BEAM, 0.32F, 0.5F);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(GammaEmitterBlockEntity emitter) {
        return true;
    }

    // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
    public AABB getRenderBoundingBox(GammaEmitterBlockEntity emitter) {
        return new AABB(emitter.getBlockPos()).inflate(GammaEmitterBlockEntity.beamLength() + 1.0);
    }
}
