package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * What glows on En Sabah Nur, full-bright over the body: his blank white eyes, the engraving of his cap, the glyph on his
 * chest (one mask for the armoured body), and, once the armour has fallen, the gold in the cracks of his cap and the veins
 * of his chest (a mask for the true form). The colour follows the state: dark while he wakes, white-gold, violet in the
 * Mind Crush's tell and the Escape (the eyes of the mind), dim while he is staggered, blazing gold at the end.
 */
public class ApocalypseEyesLayer extends RenderLayer<ApocalypseEntity, ApocalypseModel> {
    public static final ResourceLocation GLOW = Enhanced.id("textures/entity/apocalypse_boss_eyes.png");
    public static final ResourceLocation GLOW_TRUE = Enhanced.id("textures/entity/apocalypse_boss_true_eyes.png");
    /** The violet of the mind (the portal's palette): the eyes of the Mind Crush and of the Escape. */
    public static final int VIOLET = 0xB04FD8;
    /** The gold of his shield and of the cracks of the cap. */
    public static final int GOLD = 0xF5C84A;

    public ApocalypseEyesLayer(RenderLayerParent<ApocalypseEntity, ApocalypseModel> parent) {
        super(parent);
    }

    /** The colour (ARGB, alpha for the brightness) the glow has now, or 0 for none (see {@link ApocalypseFxMath#eyeTint}). */
    public static int tint(ApocalypseState state, float time, float age, boolean armoured) {
        return ApocalypseFxMath.eyeTint(state, time, age, armoured);
    }

    private static int argb(int rgb, float alpha) {
        return ApocalypseFxMath.argb(rgb, alpha);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ApocalypseEntity boss, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (boss.isInvisible()) {
            return;
        }
        ApocalypseState state = boss.state();
        float time = boss.stateTime(partialTicks);
        boolean armoured = ApocalypseModel.armoured(boss, state, time);
        int tint = tint(state, time, ageInTicks, armoured);
        if ((tint >>> 24) == 0) {
            return;
        }
        RenderType type = RenderType.eyes(armoured ? GLOW : GLOW_TRUE);
        getParentModel().renderToBuffer(poseStack, buffer.getBuffer(type), 0xF00000, OverlayTexture.NO_OVERLAY, tint);
        // The cracks of the cap blaze at the end of the armour and at the kneel: a second pass, gold, over the true form.
        float glow = getParentModel().glow;
        if (!armoured && glow > 0.05F) {
            int gold = argb(GOLD, glow * (0.7F + 0.3F * Mth.sin(ageInTicks * 0.4F)));
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW_TRUE)), 0xF00000, OverlayTexture.NO_OVERLAY, gold);
        }
    }
}
