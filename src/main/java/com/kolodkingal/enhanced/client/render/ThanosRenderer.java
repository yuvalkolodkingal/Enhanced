package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Thanos, the Mad Titan, in the world: the model of {@link ThanosModel} at 1.55 times its size (3.3 blocks tall), the
 * Double-Edged Sword in his hand and the Gauntlet's light on his other. He is invisible while he hides among his
 * illusions (vanilla draws nothing of an invisible mob; his footsteps kick up dust); he never falls over, and has no
 * name tag.
 */
public class ThanosRenderer extends MobRenderer<ThanosEntity, ThanosModel<ThanosEntity>> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/thanos.png");
    /** How much larger than its model he is drawn: 34 pixels become 3.29 blocks. */
    public static final float SCALE = 1.55F;

    public ThanosRenderer(EntityRendererProvider.Context context) {
        super(context, new ThanosModel<>(context.bakeLayer(ThanosModel.LAYER)), 1.2F);
        addLayer(new ThanosSwordLayer<>(this, new ThanosSwordModel(context.bakeLayer(ThanosSwordModel.LAYER))));
        addLayer(new ThanosGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ThanosEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(ThanosEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    /** He kneels and turns to dust; he does not fall on his side. */
    @Override
    protected float getFlipDegrees(ThanosEntity entity) {
        return 0.0F;
    }

    @Override
    protected boolean isShaking(ThanosEntity entity) {
        return false;
    }

    @Override
    protected boolean shouldShowName(ThanosEntity entity) {
        return false;
    }
}
