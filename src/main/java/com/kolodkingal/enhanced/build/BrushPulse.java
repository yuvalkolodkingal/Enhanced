package com.kolodkingal.enhanced.build;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One pulse of a brush stroke (D-B 4.11: Land's {@link LandBrush.Pulse}, Grow's {@link GrowBrush.Pulse}): planned against
 * the world as it is, a few reads at a time, into {@link LandBrush.Column}s the job writes whole or not at all.
 */
public interface BrushPulse {
    /** Plans on with about {@code reads} block reads; true once the whole pulse is planned. */
    boolean step(int reads);

    /** Whether the whole pulse is planned. */
    boolean done();

    /** The block reads made so far. */
    long reads();

    /** The columns that change, in their order; complete once {@link #done}. */
    List<LandBrush.Column> columns();

    /** How many cells its columns change. */
    long changes();

    /** How many cells its columns span. */
    long cells();

    /** The box of the cells its columns span, or null when none changes. */
    @Nullable BoundingBox bounds();
}
