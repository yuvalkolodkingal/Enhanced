package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.parrot.Parrot;

/** 26.3 only: 1.21.1's {@code FlyingAnimal} (the bee and the parrot), which 26.3 does not have. */
public final class Fliers {
    private Fliers() {
    }

    /** Whether the entity is one of the animals that fly. */
    public static boolean is(Entity e) {
        return e instanceof Bee || e instanceof Parrot;
    }

    /** Whether it is such an animal, flying now. */
    public static boolean flying(Entity e) {
        return e instanceof Bee bee ? bee.isFlying() : e instanceof Parrot parrot && parrot.isFlying();
    }
}
