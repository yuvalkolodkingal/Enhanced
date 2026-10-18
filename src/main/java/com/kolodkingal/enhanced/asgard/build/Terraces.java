package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.asgard.AsgardTerrain;
import com.kolodkingal.enhanced.asgard.CityPlan;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * The terraces of Asgard (3.5): where a district's pad meets lower ground, its edge is a retaining wall of realm stone
 * bricks with a band of gold trim and, for a drop of two or more, a balustrade on top; where the wild land stands higher
 * than a pad, its face is walled too. Not where a stairway goes down ({@link Stairways#covers}).
 */
public final class Terraces implements CityBuilder {
    public static final Terraces INSTANCE = new Terraces();
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    @Override
    public BoundingBox box() {
        return new BoundingBox(-290, 30, -290, 290, 100, 290);
    }

    @Nullable
    @Override
    public BlockState at(int x, int y, int z) {
        AsgardTerrain.Column c = AsgardTerrain.column(x, z);
        if (c.kind() != AsgardTerrain.Kind.LAND) {
            return null;
        }
        int low = Integer.MAX_VALUE;
        for (int[] s : SIDES) {
            AsgardTerrain.Column n = AsgardTerrain.column(x + s[0], z + s[1]);
            if (n.pad() != c.pad() && (c.pad() != null || n.pad() != null)) {
                low = Math.min(low, n.solid() ? n.top() : c.top() - 3);
            }
        }
        return wallAt(x, y, z, c, low);
    }

    @Nullable
    private static BlockState wallAt(int x, int y, int z, AsgardTerrain.Column c, int low) {
        if (low == Integer.MAX_VALUE || low >= c.top() || Stairways.covers(x, z)) {
            return null;
        }
        if (y > low && y <= c.top()) {
            return y == c.top() ? Palette.trim() : Palette.bricks();
        }
        if (y == c.top() + 1 && c.pad() != null && c.top() - low >= 2) {
            return Palette.wall();
        }
        return null;
    }

    @Override
    public void place(CityCanvas canvas) {
        for (int x = canvas.minX(); x < canvas.minX() + 16; x++) {
            for (int z = canvas.minZ(); z < canvas.minZ() + 16; z++) {
                AsgardTerrain.Column c = canvas.column(x, z);
                if (c.kind() != AsgardTerrain.Kind.LAND) {
                    continue;
                }
                int low = Integer.MAX_VALUE;
                for (int[] s : SIDES) {
                    AsgardTerrain.Column n = canvas.column(x + s[0], z + s[1]);
                    if (n.pad() != c.pad() && (c.pad() != null || n.pad() != null)) {
                        low = Math.min(low, n.solid() ? n.top() : c.top() - 3);
                    }
                }
                if (low == Integer.MAX_VALUE || low >= c.top()) {
                    continue;
                }
                for (int y = Math.max(low + 1, c.top() - 12); y <= c.top() + 1; y++) {
                    BlockState state = wallAt(x, y, z, c, low);
                    if (state != null) {
                        canvas.set(x, y, z, state);
                    }
                }
            }
        }
    }

    /** Whether a pad is one of those the Golden Road climbs through (their edges are its stairways). */
    static boolean onAxis(@Nullable CityPlan.Pad pad) {
        return pad != null && (pad.name().startsWith("road") || pad.name().equals("plaza") || pad.name().equals("courtyard"));
    }

    private Terraces() {
    }
}
