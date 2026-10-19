package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The fixed points of Agatha's Cottage (3.7, the plan's 9.2) in its template {@code agatha_cottage/house} (27 x 18 x
 * 25; the ground floor is layer {@link #FLOOR}, the cellar below it, the upstairs at {@link #UPSTAIRS}),
 * template-relative and unturned (x east, z south; the front with the gate is north, z 0, Rio's garden at the back, z
 * 16..24). Written by the 3.7 skeleton; lane L6 builds the real template on them ({@code CottageKeeper} maps them
 * through {@code SitePoints.toWorld}) and may move a point with a note and the test that checks it.
 */
public final class CottagePlan {
    public static final ResourceLocation TEMPLATE = Enhanced.id("agatha_cottage/house");
    public static final Vec3i SIZE = new Vec3i(27, 18, 25);
    /** The ground floor's layer (the cellar is layers 0..2). */
    public static final int FLOOR = 3;
    /** The upstairs' floor layer. */
    public static final int UPSTAIRS = 8;

    /** Agatha's home area: the kitchen. */
    public static final BlockPos AGATHA_HOME = new BlockPos(8, 3, 8);
    /** Nicky's home area: his room upstairs (the yard by day). */
    public static final BlockPos NICKY_HOME = new BlockPos(14, 8, 8);
    /** Rio's home area: her garden. */
    public static final BlockPos RIO_HOME = new BlockPos(13, 3, 20);
    /** Senor Scratchy's hutch in the back yard. */
    public static final BlockPos HUTCH = new BlockPos(22, 3, 18);
    /** The Witches' Road door in the cellar's floor. */
    public static final BlockPos ROAD_DOOR = new BlockPos(13, 0, 10);
    /** The four chairs of the family table (stairs). */
    public static final List<BlockPos> TABLE_SEATS = List.of(new BlockPos(7, 3, 6), new BlockPos(10, 3, 6),
            new BlockPos(7, 3, 9), new BlockPos(10, 3, 9));
    /** The three beds' heads: Agatha's, Nicky's, Rio's. */
    public static final List<BlockPos> BEDS = List.of(new BlockPos(6, 8, 11), new BlockPos(14, 8, 11),
            new BlockPos(20, 8, 11));
    /** The garden gate in the picket fence. */
    public static final BlockPos GATE = new BlockPos(13, 3, 0);

    private CottagePlan() {
    }
}
