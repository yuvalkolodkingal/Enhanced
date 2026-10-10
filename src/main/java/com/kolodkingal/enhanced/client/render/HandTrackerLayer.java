package com.kolodkingal.enhanced.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.phys.Vec3;

/**
 * Draws nothing: the last layer of a player, it records into {@link HandTracker} where the hands, feet, shoulders and
 * forehead of a hero are in the world this frame, from the pose stack the body was just drawn with.
 */
public class HandTrackerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The pixel in the arm's frame where the fist's middle is: the lowest three pixels of a 12 pixel arm. */
    private static final float FIST_Y = 8.5F;

    public HandTrackerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        HeroAnimState state = HeroAnimator.peek(player);
        // Only a body drawn in the world says where its hands are: not the inventory's preview, whose pose stack is the screen's.
        if (state == null || state.wVanilla > 0.0F || !state.posed) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        float fist = slim ? 0.5F : 1.0F;
        record(player, poseStack, model, ageInTicks, fist);
    }

    private static void record(AbstractClientPlayer player, PoseStack ps, PlayerModel<AbstractClientPlayer> model, float age, float fist) {
        Vec3 handRight = at(ps, model.rightArm, -fist, FIST_Y, 0.0F);
        Vec3 handLeft = at(ps, model.leftArm, fist, FIST_Y, 0.0F);
        Vec3 footRight = at(ps, model.rightLeg, 0.0F, 12.0F, 0.0F);
        Vec3 footLeft = at(ps, model.leftLeg, 0.0F, 12.0F, 0.0F);
        Vec3 shoulderRight = at(ps, model.rightArm, 0.0F, 0.0F, 0.0F);
        Vec3 shoulderLeft = at(ps, model.leftArm, 0.0F, 0.0F, 0.0F);
        Vec3 forehead = at(ps, model.head, 0.0F, -6.0F, -4.15F);
        HandTracker.record(player, age, handRight, handLeft, footRight, footLeft, shoulderRight, shoulderLeft, forehead);
    }

    /** A point (pixels) of a model part's own frame, in the world. */
    private static Vec3 at(PoseStack ps, ModelPart part, float x, float y, float z) {
        ps.pushPose();
        part.translateAndRotate(ps);
        Vec3 world = HandTracker.world(ps, x / 16.0F, y / 16.0F, z / 16.0F);
        ps.popPose();
        return world;
    }
}
