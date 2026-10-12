package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * An Ultron Sentry: the model of the boss at 0.95 of Prime's size, in black plates, with blades for antennae. Its eyes are
 * blue while it has nothing to hunt, red while it hunts and white in the ten ticks before it fires; it pitches toward
 * where it flies; assembling, it is built from small to whole. The hero's drones are drawn the same way, with eyes red.
 */
public class UltronSentryRenderer extends MobRenderer<UltronSentryEntity, UltronModel<UltronSentryEntity>> {
    /** A Sentry is drawn at this fraction of the size of Prime's model. */
    public static final float SIZE = 0.95F;

    public UltronSentryRenderer(EntityRendererProvider.Context context) {
        super(context, new UltronModel<>(context.bakeLayer(UltronModel.LAYER)), 0.4F);
        addLayer(new UltronGlowLayer<>(this, UltronLights.SENTRY_EYES, UltronLights::eyes));
        addLayer(new UltronGlowLayer<>(this, UltronLights.TORSO, UltronLights::sentryTorso));
        addLayer(new UltronGlowLayer<>(this, UltronLights.FINGERS, UltronLights::sentryFingers));
        addLayer(new UltronGlowLayer<>(this, UltronLights.PALMS, UltronLights::sentryFingers));
        addLayer(new UltronTracker.Layer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(UltronSentryEntity sentry) {
        return UltronLights.SENTRY_SKIN;
    }

    @Override
    protected void scale(UltronSentryEntity sentry, PoseStack poseStack, float partialTick) {
        float size = SIZE * UltronAnimation.pose(sentry, partialTick).get(UltronPose.SCALE);
        poseStack.scale(size, size, size);
    }

    @Override
    protected float getShadowRadius(UltronSentryEntity sentry) {
        return 0.3F;
    }

    @Override
    protected void setupRotations(UltronSentryEntity sentry, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(sentry, poseStack, bob, yBodyRot, partialTick, scale);
        float pitch = UltronAnimation.pose(sentry, partialTick).get(UltronPose.PITCH);
        if (Math.abs(pitch) > 0.01F) {
            float middle = sentry.getBbHeight() * 0.5F / scale;
            poseStack.translate(0.0F, middle, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.translate(0.0F, -middle, 0.0F);
        }
    }

    /** Flickers white while it assembles. */
    @Override
    protected float getWhiteOverlayProgress(UltronSentryEntity sentry, float partialTicks) {
        return Mth.clamp(UltronAnimation.pose(sentry, partialTicks).get(UltronPose.FLASH), 0.0F, 1.0F);
    }
}
