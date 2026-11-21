package com.kolodkingal.enhanced.compat;
//#norenames

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 1.21.11 (3.14.1): the armour layer's models. Up to 1.21.8 the armour layer copied the player model's pose onto them
 * ({@code copyPropertiesTo}), so the armour wore the hero's pose; from 1.21.9 they are posed by vanilla when drawn, from
 * the render state. HumanoidArmorLayerMixin marks them, and HumanoidModelMixin puts the hero's pose of the frame on a
 * marked model after vanilla posed it.
 */
public final class ArmourPose {

    private ArmourPose() {
    }

    public static void mark(Object model) {
        com.kolodkingal.enhanced.client.render.HeroPoses.markArmour(model);
    }

    public static boolean marked(Object model) {
        return com.kolodkingal.enhanced.client.render.HeroPoses.isArmour(model);
    }
}
