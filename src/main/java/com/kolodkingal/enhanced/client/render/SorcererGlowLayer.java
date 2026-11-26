package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * What is made of magic on the Scarlet Witch as the Sorceress Supreme: light, not metal or paint (the design's own note:
 * "tiara and hands/chest sigils are made entirely of magic"). Drawn full-bright, so it shows in the dark:
 * <ul>
 *   <li>the tiara: not metal but red chaos energy, a solid see-through sheet of neon red in the shape of her M, standing
 *   before her brow, its two horns rising over her head ({@code scarlet_witch_sorcerer_tiara.png}, 16 by 16); not
 *   white-hot, and it breathes a little;</li>
 *   <li>the Eye of Agamotto's open eye and its two moons on her chest: the pixels of {@code
 *   scarlet_witch_sorcerer_supreme_glow.png}, drawn over her skin; brighter while her cloak is spread;</li>
 *   <li>the runes of her gloves, which are chaos energy and show only while she uses her magic ({@code SorcererFx.runes}):
 *   one on each hand, on its outside (the back of the hand): an open eye under three moons, floating a hair off the
 *   glove ({@code scarlet_witch_sorcerer_rune.png}, 16 by 16, the top of the picture toward her fingertips). They grow
 *   as they light, and breathe. They are on the arms, so they follow every move of her hands.</li>
 * </ul>
 */
public class SorcererGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TIARA = Enhanced.id("textures/entity/suit/scarlet_witch_sorcerer_tiara.png");
    private static final ResourceLocation GLOW = Enhanced.id("textures/entity/suit/scarlet_witch_sorcerer_supreme_glow.png");
    private static final ResourceLocation RUNE = Enhanced.id("textures/entity/suit/scarlet_witch_sorcerer_rune.png");
    /**
     * The rune on a hand (arm pixels: the slim arm hangs from y -2 to 10, 4 deep; its outer face is 2 from its pivot):
     * how far down the arm its middle is, half its size full grown, and how far off the glove it floats.
     */
    private static final float RUNE_Y = 7.8F;
    private static final float RUNE_HALF = 3.3F;
    private static final float RUNE_OFF = 0.7F;
    /** The tiara's strips across (it bends back a little round her temples). */
    private static final int STRIPS = 4;
    /** Where it stands (head pixels: the head is x -4 to 4, y -8 to 0, its face at z -4): its width and its two ends. */
    private static final float HALF_WIDTH = 4.8F;
    private static final float TOP = -12.6F;
    private static final float BOTTOM = -3.9F;
    private static final float FRONT = -4.75F;

    public SorcererGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (!SorcererFx.worn(player) || player.isInvisible()) {
            return;
        }
        float out = SorcererFx.out(player, partialTicks);
        float breath = EnhancedClientConfig.reducedMotion() ? 0.5F : 0.5F + 0.5F * Mth.sin(ageInTicks * 0.12F);
        // the Eye and its moons: a steady light, fuller while her cloak is spread
        int eye = Mth.clamp((int) (255 * (0.7F + 0.1F * breath + 0.2F * out)), 0, 255);
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (eye << 24) | 0xFFFFFF);
        // the tiara: its picture is see-through already; it breathes a little, and is fullest while her cloak is spread
        int tiara = Mth.clamp((int) (255 * (0.8F + 0.08F * breath + 0.12F * out)), 0, 255);
        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        tiara(buffers.getBuffer(RenderType.entityTranslucentEmissive(TIARA)), poseStack.last(), (tiara << 24) | 0xFFFFFF);
        poseStack.popPose();
        // the runes of her gloves: only while she uses her chaos magic; one a hand, on its outside
        float runes = SorcererFx.runes(player, partialTicks);
        if (runes > 0.02F) {
            int bright = Mth.clamp((int) (255 * runes * (0.82F + 0.18F * breath)), 0, 255);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(RUNE));
            for (int s = -1; s <= 1; s += 2) {
                poseStack.pushPose();
                (s < 0 ? getParentModel().rightArm : getParentModel().leftArm).translateAndRotate(poseStack);
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                // (the back of the hand faces out from her side: s -1 is her right arm)
                rune(vc, poseStack.last(), s * (2.0F + RUNE_OFF), RUNE_HALF * (0.6F + 0.4F * runes), s, (bright << 24) | 0xFFFFFF);
                poseStack.popPose();
            }
        }
    }

    /**
     * One rune: a square standing at {@code x} beside the hand (arm pixels), facing along x ({@code facing} -1 or 1);
     * the picture's top is at her fingertips.
     */
    private static void rune(VertexConsumer vc, PoseStack.Pose pose, float x, float half, int facing, int argb) {
        float tips = RUNE_Y + half;
        float wrist = RUNE_Y - half;
        rune(vc, pose, x, tips, -half, 0.0F, 0.0F, facing, argb);
        rune(vc, pose, x, tips, half, 1.0F, 0.0F, facing, argb);
        rune(vc, pose, x, wrist, half, 1.0F, 1.0F, facing, argb);
        rune(vc, pose, x, wrist, -half, 0.0F, 1.0F, facing, argb);
    }

    private static void rune(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int facing,
                             int argb) {
        vc.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, facing, 0.0F, 0.0F);
    }

    private static void tiara(VertexConsumer vc, PoseStack.Pose pose, int argb) {
        for (int i = 0; i < STRIPS; i++) {
            float x0 = -HALF_WIDTH + 2.0F * HALF_WIDTH * i / STRIPS;
            float x1 = -HALF_WIDTH + 2.0F * HALF_WIDTH * (i + 1) / STRIPS;
            float z0 = depth(x0);
            float z1 = depth(x1);
            float u0 = (float) i / STRIPS;
            float u1 = (float) (i + 1) / STRIPS;
            vertex(vc, pose, x0, TOP, z0, u0, 0.0F, argb);
            vertex(vc, pose, x1, TOP, z1, u1, 0.0F, argb);
            vertex(vc, pose, x1, BOTTOM, z1, u1, 1.0F, argb);
            vertex(vc, pose, x0, BOTTOM, z0, u0, 1.0F, argb);
        }
    }

    /** How far before her face the tiara stands at {@code x}: a hair off her brow, bending back toward her temples. */
    private static float depth(float x) {
        float t = Math.abs(x) / HALF_WIDTH;
        return FRONT + 0.9F * t * t;
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
        vc.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
