package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * A light on the body of Ultron or a Sentry: a mask the size of the skin (white where the light is) drawn over the model
 * full bright, in a colour and a brightness that come from the pose, so the eyes, the fingertips, the palms and the light
 * in his torso each glow by themselves. Nothing is drawn while the light is out.
 */
public class UltronGlowLayer<T extends LivingEntity> extends RenderLayer<T, UltronModel<T>> {
    /** Says how a light burns in the pose being drawn: its colour as 0xRRGGBB scaled by its brightness, or 0 for out. */
    @FunctionalInterface
    public interface Light {
        int of(UltronPose pose);
    }

    private final RenderType type;
    private final Light light;

    public UltronGlowLayer(RenderLayerParent<T, UltronModel<T>> parent, ResourceLocation mask, Light light) {
        super(parent);
        this.type = RenderType.eyes(mask);
        this.light = light;
    }

    /** A colour {@code rgb} (0 to 1 each) at a brightness, as the packed colour the model draws with (alpha full). */
    public static int color(float r, float g, float b, float brightness) {
        int red = Mth.clamp((int) (r * brightness * 255.0F), 0, 255);
        int green = Mth.clamp((int) (g * brightness * 255.0F), 0, 255);
        int blue = Mth.clamp((int) (b * brightness * 255.0F), 0, 255);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        int color = light.of(UltronAnimation.pose(entity, partialTick));
        if ((color & 0xFFFFFF) == 0) {
            return;
        }
        getParentModel().renderToBuffer(poseStack, buffer.getBuffer(type), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
    }
}
