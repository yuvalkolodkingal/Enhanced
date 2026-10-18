package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.asgard.AsgardTerrain;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The waterways of Frigga's gardens (3.5): raised channels of still water between gold-edged walls, crossing the
 * gardens north-south and east-west round Frigga's pavilion; the western one runs out over the wild land on a wall to
 * the lip of the chasm, where the cosmic falls pour down into the void. More falls spill over the chasm's rim here and
 * there.
 */
public final class Waterways implements CityBuilder {
    public static final Waterways INSTANCE = new Waterways();
    /** The water's level (the gardens' pad is at 73). */
    public static final int WATER = 74;
    /** The lowest block of the falls. */
    public static final int FALLS_BOTTOM = -40;

    /** A channel along a line (x0..x1 at z, or z0..z1 at x), its walls two blocks off the line. */
    public record Channel(int x0, int z0, int x1, int z1) {
        boolean alongX() {
            return z0 == z1;
        }

        @Nullable
        BlockState at(int x, int y, int z) {
            int along = alongX() ? x : z;
            int across = alongX() ? z - z0 : x - x0;
            int a0 = alongX() ? x0 : z0;
            int a1 = alongX() ? x1 : z1;
            if (along < a0 - 2 || along > a1 + 2 || Math.abs(across) > 2) {
                return null;
            }
            boolean end = along < a0 || along > a1;
            boolean wall = Math.abs(across) == 2 || end;
            if (y == WATER) {
                return wall ? Palette.trim() : Palette.water();
            }
            if (y == WATER - 1) {
                return Palette.polished();
            }
            if (y < WATER - 1) {
                int ground = AsgardTerrain.top(x, z);
                return ground != Integer.MIN_VALUE && y > ground ? Palette.bricks() : null;
            }
            return null;
        }
    }

    public static final List<Channel> CHANNELS = List.of(
            new Channel(-160, -60, -109, -60),
            new Channel(-91, -60, -56, -60),
            new Channel(-100, -107, -100, -68),
            new Channel(-100, -52, -100, -16));

    @Override
    public BoundingBox box() {
        return new BoundingBox(-185, FALLS_BOTTOM, -110, -50, 80, -10);
    }

    /** The falls into the chasm: where the western channel ends, and here and there over the rim. */
    @Nullable
    private static BlockState falls(int x, int y, int z) {
        if (y > WATER || y < FALLS_BOTTOM || AsgardTerrain.column(x, z).kind() != AsgardTerrain.Kind.VOID
                || !AsgardTerrain.inChasm(x + 0.5, z + 0.5)) {
            return null;
        }
        AsgardTerrain.Column east = AsgardTerrain.column(x + 1, z);
        if (Math.abs(z + 60) <= 1 && x >= -166 && x <= -161) {
            return east.kind() == AsgardTerrain.Kind.LAND || x == -161 ? Palette.falls() : null;
        }
        if (east.kind() == AsgardTerrain.Kind.LAND && Math.floorMod(AsgardTerrain.hash(x, z), 7) == 0 && y <= east.top()) {
            return Palette.falls();
        }
        return null;
    }

    @Nullable
    @Override
    public BlockState at(int x, int y, int z) {
        for (Channel c : CHANNELS) {
            BlockState s = c.at(x, y, z);
            if (s != null) {
                return s;
            }
        }
        return falls(x, y, z);
    }

    @Override
    public void place(CityCanvas canvas) {
        BoundingBox b = box();
        if (!b.intersects(canvas.box())) {
            return;
        }
        for (int x = Math.max(b.minX(), canvas.minX()); x <= Math.min(b.maxX(), canvas.minX() + 15); x++) {
            for (int z = Math.max(b.minZ(), canvas.minZ()); z <= Math.min(b.maxZ(), canvas.minZ() + 15); z++) {
                boolean chasm = AsgardTerrain.inChasm(x + 0.5, z + 0.5);
                int y0 = chasm ? FALLS_BOTTOM : 55;
                for (int y = y0; y <= WATER; y++) {
                    BlockState s = at(x, y, z);
                    if (s != null) {
                        canvas.set(x, y, z, s);
                    }
                }
            }
        }
    }

    private Waterways() {
    }
}
