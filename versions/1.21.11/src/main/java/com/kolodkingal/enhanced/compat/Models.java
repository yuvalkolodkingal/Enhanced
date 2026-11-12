package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;

/**
 * 1.21.11 only: 1.21.1's {@code model.copyPropertiesTo(other)} (gone with the render states: a model is posed from a
 * state). The other model takes this one's pose, part by part.
 */
public final class Models {
    private Models() {
    }

    private static void copy(ModelPart from, ModelPart to) {
        to.loadPose(from.storePose());
        to.visible = from.visible;
    }

    public static void copyPose(Object from, Object to) {
        if (from instanceof HumanoidModel<?> a && to instanceof HumanoidModel<?> b) {
            copy(a.head, b.head);
            copy(a.hat, b.hat);
            copy(a.body, b.body);
            copy(a.rightArm, b.rightArm);
            copy(a.leftArm, b.leftArm);
            copy(a.rightLeg, b.rightLeg);
            copy(a.leftLeg, b.leftLeg);
            if (a instanceof PlayerModel pa && b instanceof PlayerModel pb) {
                copy(pa.jacket, pb.jacket);
                copy(pa.leftSleeve, pb.leftSleeve);
                copy(pa.rightSleeve, pb.rightSleeve);
                copy(pa.leftPants, pb.leftPants);
                copy(pa.rightPants, pb.rightPants);
            }
        }
    }
}
