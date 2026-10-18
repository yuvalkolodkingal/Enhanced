package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.asgard.AsgardTerrain;
import com.kolodkingal.enhanced.asgard.CityPlan;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The stairways of Asgard (3.5): wherever a way goes from one level to a lower one (the Golden Road down its terraces,
 * the streets of the houses down from the road, the paths into the feast hall's, the barracks' and the training
 * grounds' hollows), a flight of stairs as wide as the way, laid on the lower ground, climbing toward the higher. The
 * terraces leave their walls out there.
 */
public final class Stairways implements CityBuilder {
    /**
     * A stairway: the last cell of the higher ground (x, z), the direction toward the lower ground (dx, dz) and its half
     * width across. How many steps it has is the difference of the two grounds' heights.
     */
    public record Way(int x, int z, int dx, int dz, int halfWidth) {
        /** The cells of the higher ground's edge and of the lower ground the stairs may take (up to 12 steps). */
        BoundingBox box() {
            int ax = dz != 0 ? halfWidth : 0;
            int az = dx != 0 ? halfWidth : 0;
            return BoundingBox.fromCorners(new net.minecraft.core.Vec3i(x - ax, 40, z - az),
                    new net.minecraft.core.Vec3i(x + ax + dx * 12, 100, z + az + dz * 12));
        }
    }

    public static final List<Way> WAYS;

    static {
        List<Way> ways = new ArrayList<>();
        int h = CityPlan.ROAD_PAD_HALF_WIDTH;
        for (int z : new int[]{-17, 39, 79, 119, 159}) {
            ways.add(new Way(0, z, 0, 1, h));                         // the Golden Road down its terraces
        }
        for (int street : CityPlan.STREETS_Z) {
            ways.add(new Way(-h, street, -1, 0, CityPlan.STREET_HALF_WIDTH));       // from the road into the houses
            if (street <= 140) {
                ways.add(new Way(-150, street, -1, 0, CityPlan.STREET_HALF_WIDTH)); // down to the lower houses
            }
        }
        for (int z : new int[]{60, 104, 134, 158, 182}) {
            ways.add(new Way(h, z, 1, 0, 2));                         // from the road into the east district
        }
        ways.add(new Way(51, 92, 1, 0, 2));                           // into the feast hall's hollow
        ways.add(new Way(58, 0, 1, 0, 2));                            // into the training grounds
        ways.add(new Way(117, 160, 1, 0, 2));                         // into the barracks
        ways.add(new Way(-170, 140, 0, 1, 2));                        // from the lower houses toward the stables
        ways.add(new Way(-170, 143, 0, 1, 2));
        ways.add(new Way(-195, 20, -1, 0, 2));                        // down to the docks
        ways.add(new Way(-51, -60, -1, 0, 2));                        // up into the gardens
        ways.add(new Way(-52, -60, 1, 0, 2));
        WAYS = List.copyOf(ways);
    }

    public static final Stairways INSTANCE = new Stairways();

    /** Whether a stairway takes the cell (x, z) (the higher edge or its flight): no terrace wall there. */
    public static boolean covers(int x, int z) {
        for (Way w : WAYS) {
            int along = w.dx != 0 ? (x - w.x) * w.dx : (z - w.z) * w.dz;
            int across = w.dx != 0 ? z - w.z : x - w.x;
            if (along < 0 || along > 12 || Math.abs(across) > w.halfWidth) {
                continue;
            }
            int hx = w.dx != 0 ? w.x : x;
            int hz = w.dz != 0 ? w.z : z;
            int k = AsgardTerrain.top(hx, hz) - AsgardTerrain.top(hx + w.dx, hz + w.dz);
            if (along <= Math.max(k, 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BoundingBox box() {
        return new BoundingBox(-260, 40, -60, 200, 100, 220);
    }

    @Nullable
    @Override
    public BlockState at(int x, int y, int z) {
        for (Way w : WAYS) {
            BlockState s = at(w, x, y, z);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    private static BlockState at(Way w, int x, int y, int z) {
        int along = w.dx != 0 ? (x - w.x) * w.dx : (z - w.z) * w.dz;
        int across = w.dx != 0 ? z - w.z : x - w.x;
        if (along < 1 || along > 12 || Math.abs(across) > w.halfWidth) {
            return null;
        }
        int hx = w.dx != 0 ? w.x : x;
        int hz = w.dz != 0 ? w.z : z;
        int high = AsgardTerrain.top(hx, hz);
        int low = AsgardTerrain.top(hx + w.dx, hz + w.dz);
        int k = high - low;
        if (k <= 0 || along > k || high == Integer.MIN_VALUE || low == Integer.MIN_VALUE) {
            return null;
        }
        int i = along - 1;
        int stair = low + k - i;
        if (y == stair) {
            return Palette.stairs(Direction.fromDelta(-w.dx, 0, -w.dz));
        }
        if (y > low && y < stair) {
            return Palette.bricks();
        }
        if (y > stair && y <= stair + 3) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return null;
    }

    @Override
    public void place(CityCanvas canvas) {
        for (Way w : WAYS) {
            BoundingBox b = w.box();
            if (!b.intersects(canvas.box())) {
                continue;
            }
            for (int x = Math.max(b.minX(), canvas.minX()); x <= Math.min(b.maxX(), canvas.minX() + 15); x++) {
                for (int z = Math.max(b.minZ(), canvas.minZ()); z <= Math.min(b.maxZ(), canvas.minZ() + 15); z++) {
                    for (int y = 55; y <= 95; y++) {
                        BlockState s = at(w, x, y, z);
                        if (s != null && !(s.isAir() && canvas.get(x, y, z).isAir())) {
                            canvas.set(x, y, z, s);
                        }
                    }
                }
            }
        }
    }

    private Stairways() {
    }
}
