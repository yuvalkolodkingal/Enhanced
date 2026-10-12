package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ultron.VibraniumKeyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * The Vibranium Key, rising out of the ground over {@link VibraniumKeyEntity#EMERGE_TICKS} ticks, its drum turning (slowly
 * when it has just come up, faster while it glows), its cyan seams burning through a mask until it powers down. It is
 * drawn from the two game times in its synced data, so every client sees the same stage of it; its dust and its column of
 * blue motes are {@link UltronEffects}'.
 */
public class VibraniumKeyRenderer extends EntityRenderer<VibraniumKeyEntity> {
    private static final ResourceLocation SKIN = Enhanced.id("textures/entity/vibranium_key.png");
    private static final ResourceLocation GLOW = Enhanced.id("textures/entity/vibranium_key_glow.png");
    /** How far down it sinks when buried, in blocks. */
    private static final float BURIED = 3.2F;
    /** The drum's turn, in degrees a tick, resting and glowing. */
    private static final float SLOW = 0.8F;
    private static final float FAST = 3.0F;

    private final VibraniumKeyModel model;
    private final Map<Integer, float[]> spins = new HashMap<>();

    public VibraniumKeyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new VibraniumKeyModel(context.bakeLayer(VibraniumKeyModel.LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(VibraniumKeyEntity key) {
        return SKIN;
    }

    @Override
    public void render(VibraniumKeyEntity key, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (key.emergedAt() < 0) {
            return; // still buried
        }
        float now = key.level().getGameTime() + partialTick;
        float rise = Mth.clamp((now - key.emergedAt()) / VibraniumKeyEntity.EMERGE_TICKS, 0.0F, 1.0F);
        float power = key.glow((long) now);
        if (key.poweredDownAt() >= 0) {
            power = rise * Math.max(0.0F, 1.0F - (now - key.poweredDownAt()) / VibraniumKeyEntity.POWER_DOWN_TICKS);
        }
        // The drum's angle is the sum of its turns, so a change of speed never makes it jump.
        float[] spin = spins.computeIfAbsent(key.getId(), id -> new float[]{0.0F, now});
        float dt = Mth.clamp(now - spin[1], 0.0F, 5.0F);
        spin[0] += dt * Mth.lerp(power, SLOW, FAST) * rise;
        spin[1] = now;
        model.spin(spin[0] * Mth.DEG_TO_RAD);

        poseStack.pushPose();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // Rising out of the crater: it begins buried and comes up with an ease-out.
        float eased = 1.0F - (1.0F - rise) * (1.0F - rise);
        poseStack.translate(0.0F, (1.0F - eased) * BURIED, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(SKIN)), packedLight, OverlayTexture.NO_OVERLAY);
        if (power > 0.01F) {
            int colour = UltronGlowLayer.color(0.5F, 0.83F, 1.0F, power * (0.85F + 0.15F * Mth.sin(now * 0.3F)));
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
        }
        poseStack.popPose();
        super.render(key, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
