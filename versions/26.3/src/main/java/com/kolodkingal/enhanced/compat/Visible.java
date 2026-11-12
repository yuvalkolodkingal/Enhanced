package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.client.model.HumanoidModel;

/** 26.3 only: 1.21.1's {@code HumanoidModel.setAllVisible}. */
public final class Visible {
    private Visible() {
    }

    public static void all(HumanoidModel<?> model, boolean visible) {
        model.head.visible = visible;
        model.hat.visible = visible;
        model.body.visible = visible;
        model.rightArm.visible = visible;
        model.leftArm.visible = visible;
        model.rightLeg.visible = visible;
        model.leftLeg.visible = visible;
    }
}
