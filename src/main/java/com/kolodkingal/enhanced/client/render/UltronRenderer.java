package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ultron.UltronBodies;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Ultron the boss: the hand-made model ({@link UltronModel}) at the scale of his body (Prime is almost three and a half
 * blocks tall, each later body smaller), in the skin of his form, with the lights of his eyes, his chest, his fingertips
 * and his palms drawn over it ({@link UltronGlowLayer}), the cyan the lift bathes him in ({@link UltronBathLayer}) and
 * the tracker that tells the beams where his fingers are ({@link UltronTracker}). How he stands is {@link UltronPoses}.
 */
public class UltronRenderer extends MobRenderer<UltronEntity, UltronModel<UltronEntity>> {
    public UltronRenderer(EntityRendererProvider.Context context) {
        super(context, new UltronModel<>(context.bakeLayer(UltronModel.LAYER)), 0.7F);
        addLayer(new UltronBathLayer<>(this));
        addLayer(new UltronGlowLayer<>(this, UltronLights.TORSO, UltronLights::torso));
        addLayer(new UltronGlowLayer<>(this, UltronLights.EYES, UltronLights::eyes));
        addLayer(new UltronGlowLayer<>(this, UltronLights.FINGERS, UltronLights::fingers));
        addLayer(new UltronGlowLayer<>(this, UltronLights.PALMS, UltronLights::palms));
        addLayer(new UltronTracker.Layer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(UltronEntity boss) {
        return UltronLights.BOSS_SKINS[UltronAnimation.form(boss)];
    }

    /** The size of his body, from the hitbox's own: Prime 1.9, the Hybrid 1.7, then smaller. */
    private static float sizeOf(UltronEntity boss, UltronPose pose) {
        return UltronBodies.of(boss.bodyIndex()).scale() * pose.get(UltronPose.SCALE);
    }

    @Override
    protected void scale(UltronEntity boss, PoseStack poseStack, float partialTick) {
        float size = sizeOf(boss, UltronAnimation.pose(boss, partialTick));
        poseStack.scale(size, size, size);
    }

    @Override
    protected float getShadowRadius(UltronEntity boss) {
        return 0.45F * UltronBodies.of(boss.bodyIndex()).scale();
    }

    /** The Dive pitches the whole body forward, about the middle of it. */
    @Override
    protected void setupRotations(UltronEntity boss, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(boss, poseStack, bob, yBodyRot, partialTick, scale);
        float pitch = UltronAnimation.pose(boss, partialTick).get(UltronPose.PITCH);
        if (Math.abs(pitch) > 0.01F) {
            float middle = boss.getBbHeight() * 0.5F / scale;
            poseStack.translate(0.0F, middle, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.translate(0.0F, -middle, 0.0F);
        }
    }

    /** A flash of white over the whole body (the Call, the death). */
    @Override
    protected float getWhiteOverlayProgress(UltronEntity boss, float partialTicks) {
        return Mth.clamp(UltronAnimation.pose(boss, partialTicks).get(UltronPose.FLASH), 0.0F, 1.0F);
    }

    /** No name tag over a boss; the bars say who he is. */
    @Override
    protected boolean shouldShowName(UltronEntity boss) {
        return boss.hasCustomName() && super.shouldShowName(boss);
    }
}
