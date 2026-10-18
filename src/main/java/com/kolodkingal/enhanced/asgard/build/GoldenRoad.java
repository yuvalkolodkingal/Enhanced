package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.asgard.AsgardTerrain;
import com.kolodkingal.enhanced.asgard.CityPlan;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * The Golden Road (3.5): from the foot of the palace's courtyard (z -16) south to the bridge head (z 206), paved in
 * polished stone with gold, edged with trim, braziers on both sides every 16 blocks; and the streets of the houses'
 * district (four east-west, one north-south), paved and lit by lamps. On the pads' own level (the stairways join them).
 */
public final class GoldenRoad implements CityBuilder {
    public static final GoldenRoad INSTANCE = new GoldenRoad();

    @Override
    public BoundingBox box() {
        return new BoundingBox(-206, 60, CityPlan.ROAD_START_Z, CityPlan.ROAD_PAD_HALF_WIDTH, 90, CityPlan.ROAD_END_Z);
    }

    /** Whether (x, z) is on a street of the houses' district. */
    public static boolean onStreet(int x, int z) {
        if (x > -18 || x < -205 || z < 44 || z > 196) {
            return false;
        }
        if (Math.abs(x - CityPlan.STREET_X) <= CityPlan.STREET_HALF_WIDTH && x >= -150) {
            return true;
        }
        for (int s : CityPlan.STREETS_Z) {
            if (Math.abs(z - s) <= CityPlan.STREET_HALF_WIDTH && (x >= -150 || s <= 140)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public BlockState at(int x, int y, int z) {
        AsgardTerrain.Column c = AsgardTerrain.column(x, z);
        if (c.pad() == null) {
            return null;
        }
        int top = c.top();
        if (x >= -CityPlan.ROAD_PAD_HALF_WIDTH && Terraces.onAxis(c.pad()) && z >= CityPlan.ROAD_START_Z) {
            int ax = Math.abs(x);
            if (y == top && ax <= CityPlan.ROAD_HALF_WIDTH) {
                if (ax == CityPlan.ROAD_HALF_WIDTH || Math.floorMod(z, 16) == 0) {
                    return Palette.trim();
                }
                return (ax + Math.floorMod(z, 6)) % 6 == 0 ? Palette.gold() : Palette.polished();
            }
            if (y == top + 1 && ax == 8 && Math.floorMod(z, 16) == 8) {
                return Palette.brazier();
            }
            return null;
        }
        if (onStreet(x, z)) {
            boolean edge = !onStreet(x + 1, z) || !onStreet(x - 1, z) || !onStreet(x, z + 1) || !onStreet(x, z - 1);
            if (y == top) {
                return edge ? Palette.bricks() : Palette.polished();
            }
            if (edge && Math.floorMod(x - 10, 16) == 0 && nearRow(z, 2) && x < -20) {
                if (y == top + 1 || y == top + 2) {
                    return Palette.wall();
                }
                if (y == top + 3) {
                    return net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState();
                }
            }
        }
        return null;
    }

    private static boolean nearRow(int z, int d) {
        for (int s : CityPlan.STREETS_Z) {
            if (Math.abs(z - s) == d) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void place(CityCanvas canvas) {
        BoundingBox b = box();
        if (!b.intersects(canvas.box())) {
            return;
        }
        for (int x = Math.max(b.minX(), canvas.minX()); x <= Math.min(b.maxX(), canvas.minX() + 15); x++) {
            for (int z = Math.max(b.minZ(), canvas.minZ()); z <= Math.min(b.maxZ(), canvas.minZ() + 15); z++) {
                AsgardTerrain.Column c = canvas.column(x, z);
                if (c.pad() == null) {
                    continue;
                }
                for (int y = c.top(); y <= c.top() + 3; y++) {
                    BlockState s = at(x, y, z);
                    if (s != null) {
                        canvas.set(x, y, z, s);
                    }
                }
            }
        }
    }

    private GoldenRoad() {
    }
}
