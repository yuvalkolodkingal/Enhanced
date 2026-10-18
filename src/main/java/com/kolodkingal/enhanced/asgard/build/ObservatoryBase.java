package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.asgard.CityPlan;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * What holds the Observatory up over the void (3.5): an inverted cone of realm stone banded with gold under its floor
 * (radius 13 at y 72, a point at y 20), and the gold pylon that runs down through it to y 2.
 */
public final class ObservatoryBase implements CityBuilder {
    public static final ObservatoryBase INSTANCE = new ObservatoryBase();
    private static final int TOP = CityPlan.OBSERVATORY_CENTER.getY() - 1;
    private static final int POINT = 20;

    @Override
    public BoundingBox box() {
        int cx = CityPlan.OBSERVATORY_CENTER.getX();
        int cz = CityPlan.OBSERVATORY_CENTER.getZ();
        return new BoundingBox(cx - 14, 2, cz - 14, cx + 14, TOP, cz + 14);
    }

    @Nullable
    @Override
    public BlockState at(int x, int y, int z) {
        double d = Math.hypot(x - CityPlan.OBSERVATORY_CENTER.getX(), z - CityPlan.OBSERVATORY_CENTER.getZ());
        if (d <= 1.0 && y >= 2 && y <= TOP) {
            return Palette.goldPillar();
        }
        if (y < POINT || y > TOP) {
            return null;
        }
        double radius = CityPlan.OBSERVATORY_RADIUS * Math.pow((y - POINT) / (double) (TOP - POINT), 0.6);
        if (d > radius) {
            return null;
        }
        if (d > radius - 1.2 && Math.floorMod(TOP - y, 8) == 0) {
            return Palette.trim();
        }
        return y == TOP ? Palette.polished() : Palette.stone();
    }

    private ObservatoryBase() {
    }
}
