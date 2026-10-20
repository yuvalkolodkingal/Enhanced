package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Hulkbuster's skeleton (3.8, PLAN 6.4, 7.1): a humanoid with the Mark 44's proportions at human height (the 3.5x
 * scale attribute makes it huge): a small head sunk between huge shoulders set wide apart, long gorilla arms, short
 * thick legs. Its parts carry no cubes: the suit is drawn over them by {@link SuitRender#drawSuit} (the worn suit, by
 * the Hulkbuster lane's renderer swap; the frame Veronica drops; the display). It walks like the player model, heavier.
 */
public class HulkbusterModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("hulkbuster"), "main");
    /** The pivots (pixels): the head, the body, the shoulders (x), the hips (x) and their height. */
    static final float HEAD_Y = -4.0F;
    static final float HEAD_Z = -2.5F;
    static final float BODY_Y = -2.0F;
    static final float ARM_X = 10.5F;
    static final float ARM_Y = -3.0F;
    static final float LEG_X = 4.5F;
    static final float LEG_Y = 12.0F;

    public HulkbusterModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, HEAD_Y, HEAD_Z));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, HEAD_Y, HEAD_Z));
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, BODY_Y, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-ARM_X, ARM_Y, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(ARM_X, ARM_Y, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-LEG_X, LEG_Y, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(LEG_X, LEG_Y, 0.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    /** The player model's animation, moved onto the Hulkbuster's pivots, the arms' and legs' swing a little heavier. */
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        repivot();
    }

    /** Puts the parts back on the Hulkbuster's pivots (after the player model's animation moved them). */
    public void repivot() {
        float crouch = crouching ? 1.0F : 0.0F;
        head.y = HEAD_Y + 3.2F * crouch;
        head.z = HEAD_Z;
        body.y = BODY_Y + 3.2F * crouch;
        float cos = Mth.cos(body.yRot);
        float sin = Mth.sin(body.yRot);
        rightArm.x = -ARM_X * cos;
        rightArm.z = ARM_X * sin;
        leftArm.x = ARM_X * cos;
        leftArm.z = -ARM_X * sin;
        rightArm.y = leftArm.y = ARM_Y + 3.2F * crouch;
        rightLeg.x = -LEG_X;
        leftLeg.x = LEG_X;
        rightLeg.y = leftLeg.y = LEG_Y + 0.2F * crouch;
        rightArm.xRot *= 0.75F;
        leftArm.xRot *= 0.75F;
        rightLeg.xRot *= 0.7F;
        leftLeg.xRot *= 0.7F;
        hat.copyFrom(head);
    }

    /**
     * How far (pixels, in the root's space) a part of the Hulkbuster sits from where the player model's part would be:
     * a caller drawing it on a player's skeleton moves each part by this (its shoulders and hips are wider, its head
     * sunk lower).
     */
    public static float[] fromPlayer(com.kolodkingal.enhanced.ironman.SuitParts part) {
        return switch (part) {
            case HELMET, FACEPLATE -> new float[] {0.0F, HEAD_Y, HEAD_Z};
            case CHEST, BACK -> new float[] {0.0F, BODY_Y, 0.0F};
            case RIGHT_UPPER_ARM, RIGHT_FOREARM, RIGHT_GAUNTLET -> new float[] {-ARM_X + 5.0F, ARM_Y - 2.0F, 0.0F};
            case LEFT_UPPER_ARM, LEFT_FOREARM, LEFT_GAUNTLET -> new float[] {ARM_X - 5.0F, ARM_Y - 2.0F, 0.0F};
            case RIGHT_THIGH, RIGHT_SHIN, RIGHT_BOOT -> new float[] {-LEG_X + 1.9F, LEG_Y - 12.0F, 0.0F};
            case LEFT_THIGH, LEFT_SHIN, LEFT_BOOT -> new float[] {LEG_X - 1.9F, LEG_Y - 12.0F, 0.0F};
        };
    }

    /** Stands it still (the frame, the display), the arms a little out by {@code spread} (0-1). */
    public void rest(float spread) {
        for (ModelPart part : new ModelPart[] {head, hat, body, rightArm, leftArm, rightLeg, leftLeg}) {
            part.resetPose();
        }
        rightArm.zRot = 0.08F + 0.35F * spread;
        leftArm.zRot = -0.08F - 0.35F * spread;
        rightLeg.zRot = 0.04F * spread;
        leftLeg.zRot = -0.04F * spread;
    }
}
