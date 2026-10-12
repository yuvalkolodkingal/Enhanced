package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.thanos.ThanosPhantomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * An illusion of Thanos: drawn exactly as he is (his model, his texture, his plates and stones as the real one has
 * them), so that only the shadow he casts and the dust at his feet when he walks unseen tell which is which: an
 * illusion has no shadow.
 */
public class ThanosPhantomRenderer extends MobRenderer<ThanosPhantomEntity, ThanosModel<ThanosPhantomEntity>> {
    public ThanosPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new ThanosModel<>(context.bakeLayer(ThanosModel.LAYER)), 0.0F);
        addLayer(new ThanosSwordLayer<>(this, new ThanosSwordModel(context.bakeLayer(ThanosSwordModel.LAYER))));
        addLayer(new ThanosGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ThanosPhantomEntity entity) {
        return ThanosRenderer.TEXTURE;
    }

    @Override
    protected void scale(ThanosPhantomEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(ThanosRenderer.SCALE, ThanosRenderer.SCALE, ThanosRenderer.SCALE);
    }

    @Override
    protected boolean shouldShowName(ThanosPhantomEntity entity) {
        return false;
    }
}
