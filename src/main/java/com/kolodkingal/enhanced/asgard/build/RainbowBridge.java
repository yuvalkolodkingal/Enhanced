package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.asgard.AsgardTerrain;
import com.kolodkingal.enhanced.asgard.CityPlan;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * The Rainbow Bridge (3.5): seven blocks of glowing crystal at y 72 from the bridge head's gate (z 197) to the
 * Observatory's door (z 426, then a step up onto its floor), gold beams along its edges, a rail of gold lattice with
 * braziers, and every 24 blocks a pillar down to the ground or the sea floor (none over the void).
 */
public final class RainbowBridge implements CityBuilder {
    public static final RainbowBridge INSTANCE = new RainbowBridge();
    public static final int FIRST_Z = 197;
    public static final int LAST_Z = 426;

    @Override
    public BoundingBox box() {
        return new BoundingBox(-4, 30, FIRST_Z, 4, 74, LAST_Z);
    }

    /** Whether a pillar holds the bridge up at z (over land or sea). */
    static boolean pillarAt(int z) {
        return z >= 216 && Math.floorMod(z - 216, 24) == 0 && z < CityPlan.SEA_RADIUS;
    }

    @Nullable
    @Override
    public BlockState at(int x, int y, int z) {
        int ax = Math.abs(x);
        if (z < FIRST_Z || z > LAST_Z || ax > 4) {
            return null;
        }
        int deck = CityPlan.BRIDGE_Y;
        if (z == LAST_Z && y == deck + 1 && ax <= CityPlan.BRIDGE_HALF_WIDTH) {
            return Palette.stairs(Direction.SOUTH);
        }
        if (y == deck) {
            return ax <= CityPlan.BRIDGE_HALF_WIDTH ? Palette.bridge(Direction.Axis.Z) : Palette.trim();
        }
        if (ax == 4 && z > 206) {
            if (y == deck + 1) {
                return Math.floorMod(z, 24) == 0 ? Palette.brazier() : Palette.lattice();
            }
            if (y == deck - 1) {
                return Palette.trim();
            }
        }
        if (pillarAt(z) && ax <= 1 && y < deck) {
            int ground = AsgardTerrain.top(x, z);
            if (ground != Integer.MIN_VALUE && y > ground) {
                return y == deck - 1 ? Palette.trim() : Palette.stonePillar();
            }
        }
        return null;
    }

    private RainbowBridge() {
    }
}
