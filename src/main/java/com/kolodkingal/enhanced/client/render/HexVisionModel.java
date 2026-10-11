package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.HexVisionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The body of the Vision of a Hex, with his own simple poses: he hovers with his arms a little away from his sides,
 * flies with them swept back and his legs trailing (the renderer leans the whole body into the flight), stands guard
 * when he fights with one fist cocked and the other arm up, and punches with the vanilla swing on top of the guard.
 * While he fires his Mind Stone beam his arms hang quietly at his sides.
 */
public class HexVisionModel extends PlayerModel<HexVisionEntity> {
    /** How far into flight he is (0 hovering, 1 flying at speed), and what he is doing, for the pose being set up. */
    private float fly;
    private boolean beam;
    private boolean guard;
    private float age;

    public HexVisionModel(ModelPart root) {
        super(root, false);
    }

    /** How fast he moves, from 0 (hovering) to 1 (flying at speed), smoothed between frames. */
    public static float flightAmount(HexVisionEntity entity, float partialTick) {
        return Mth.clamp(entity.walkAnimation.speed(partialTick), 0.0F, 1.0F);
    }

    @Override
    public void setupAnim(HexVisionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        fly = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        beam = entity.isBeaming();
        guard = entity.isAggressive() && !beam;
        age = ageInTicks;
        // No walking in the air: the arms and legs are posed by hand below.
        super.setupAnim(entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);

        float bob = Mth.sin(ageInTicks * 0.09F);
        // Legs hang together, crossed a little at the ankles, and stream out behind him as he flies.
        rightLeg.xRot = Mth.lerp(fly, 0.06F + 0.03F * bob, 0.55F);
        leftLeg.xRot = Mth.lerp(fly, -0.04F - 0.03F * bob, 0.4F);
        rightLeg.zRot = Mth.lerp(fly, 0.02F, 0.06F);
        leftLeg.zRot = Mth.lerp(fly, -0.02F, -0.06F);
        // The flight leans his body forward; his head stays up to look where he is going.
        head.xRot -= fly * 0.35F;
        hat.copyFrom(head);

        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        jacket.copyFrom(body);
    }

    /** The arms are last posed just before the punch's swing is added: this is where their stance is set. */
    @Override
    protected void setupAttackAnimation(HexVisionEntity entity, float ageInTicks) {
        float bob = Mth.sin(age * 0.09F);
        if (guard) {
            // Left arm up and across, right fist cocked by the chest.
            leftArm.xRot = -1.15F;
            leftArm.yRot = 0.35F;
            leftArm.zRot = -0.08F;
            rightArm.xRot = -0.65F;
            rightArm.yRot = -0.3F;
            rightArm.zRot = 0.06F;
        } else if (beam) {
            // Still, arms down by his sides, a little back: the stone does the work.
            rightArm.xRot = 0.12F;
            leftArm.xRot = 0.12F;
            rightArm.zRot = 0.1F;
            leftArm.zRot = -0.1F;
        } else {
            // Hovering: arms a little away from his body, floating. Flying: swept back along it.
            rightArm.xRot = Mth.lerp(fly, 0.04F + 0.04F * bob, 0.7F);
            leftArm.xRot = Mth.lerp(fly, 0.04F - 0.04F * bob, 0.55F);
            rightArm.zRot = Mth.lerp(fly, 0.16F + 0.03F * bob, 0.12F);
            leftArm.zRot = Mth.lerp(fly, -0.16F - 0.03F * bob, -0.12F);
        }
        super.setupAttackAnimation(entity, ageInTicks);
    }
}
