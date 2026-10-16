package com.kolodkingal.enhanced.vishanti;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A ward: a sphere round a base that no monster can enter (see {@link WardManager}). One base of one sorcerer has one ward,
 * named as the base is; it lives in the store of the dimension where the base is.
 *
 * @param owner  the sorcerer whose base it is
 * @param name   the name of the base place in the owner's book
 * @param center the saved place (where the owner stood)
 * @param radius blocks; 0 means the ward sleeps (the owner is no longer a sorcerer, or wards are turned off)
 * @param color  RGB of the owner's power, for the dome the clients draw
 */
public record Ward(UUID owner, String name, Vec3 center, int radius, int color) {
    public static final Codec<Ward> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Ward::owner),
            Codec.STRING.fieldOf("name").forGetter(Ward::name),
            Vec3.CODEC.fieldOf("center").forGetter(Ward::center),
            Codec.INT.fieldOf("radius").forGetter(Ward::radius),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(Ward::color)
    ).apply(i, Ward::new));

    /** Whether the ward protects: it is not asleep. */
    public boolean awake() {
        return radius > 0;
    }

    /** Whether the point is inside the sphere (the edge included). */
    public boolean contains(Vec3 point) {
        return awake() && center.distanceToSqr(point) <= (double) radius * radius;
    }

    /** The same ward with another radius and colour. */
    public Ward with(int radius, int color) {
        return new Ward(owner, name, center, radius, color);
    }

    /** Whether this is the ward of that owner's base called {@code baseName} (names compare ignoring case, as the book's do). */
    public boolean is(UUID owner, String baseName) {
        return this.owner.equals(owner) && name.equalsIgnoreCase(baseName);
    }
}
