package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The Mark 5's suitcase at his feet while the suit unfolds (3.8, PLAN 7.2, Iron Man 2's Monaco): it lies on the ground
 * before him, snaps open, and folds away into the boots as the panels climb out; a take-off plays it backwards (the
 * suit folds down into it).
 */
final class Suitcase {
    static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/iron_man/suitcase.png");
    private static ModelPart base;
    private static ModelPart lid;

    static void bake(ModelPart root) {
        base = root.getChild("base");
        lid = root.getChild("lid");
    }

    /** In the skeleton's root space (pixels, y down, the ground at 24). */
    static void drawAtFeet(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, SuitLook look) {
        if (base == null) {
            return;
        }
        float[] c = SuitTimelines.suitcase(look.progress, look.up);
        float there = c[1];
        if (there <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, 24.0F / 16.0F, -3.0F / 16.0F);
        float s = 0.4F + 0.6F * SuitTimelines.smooth(there);
        poseStack.scale(s, s, s);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        base.render(poseStack, vc, light, overlay);
        lid.xRot = -1.9F * SuitTimelines.smooth(c[0]);
        lid.render(poseStack, vc, light, overlay);
        lid.resetPose();
        poseStack.popPose();
    }

    private Suitcase() {
    }
}
