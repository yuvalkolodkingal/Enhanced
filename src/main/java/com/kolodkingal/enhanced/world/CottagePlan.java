package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The fixed points of Agatha's Cottage (3.7, the plan's 9.2) in its template {@code agatha_cottage/house} (27 x 18 x
 * 25), template-relative and unturned (x east, z south; the front with the gate is north, z 0, Rio's garden at the
 * back, z 15..23). Layer {@link #FLOOR} lies at ground level (the structure's {@code floor}: the ground floor's floor
 * and the lawn), so one stands on layer 4; the cellar is layers 0..2 (one stands on layer 1), the upstairs floor layer
 * 7 (one stands on {@link #UPSTAIRS}). The template is built by lane L6's script {@code SCRATCH/v37/art/l6/cottage.py};
 * {@code CottageKeeper} maps the points through {@code SitePoints.toWorld}. (Lane L6 moved S0's points onto its
 * template: every standing point one layer up, the Road door to layer 1, the beds against the north wall.)
 */
public final class CottagePlan {
    public static final ResourceLocation TEMPLATE = Enhanced.id("agatha_cottage/house");
    public static final Vec3i SIZE = new Vec3i(27, 18, 25);
    /** The ground floor's layer, at ground level (the cellar is layers 0..2); one stands on the layer above. */
    public static final int FLOOR = 3;
    /** The layer one stands on upstairs (its floor is the layer below). */
    public static final int UPSTAIRS = 8;

    /** Agatha's home area: the kitchen, by the pot. */
    public static final BlockPos AGATHA_HOME = new BlockPos(6, 4, 8);
    /** Nicky's home area: his room upstairs (the yard by day). */
    public static final BlockPos NICKY_HOME = new BlockPos(13, 8, 8);
    /** Rio's home area: the path through her garden. */
    public static final BlockPos RIO_HOME = new BlockPos(13, 4, 20);
    /** Senor Scratchy's hutch in the back yard (inside its fence). */
    public static final BlockPos HUTCH = new BlockPos(22, 4, 18);
    /** The Witches' Road door on the cellar's floor. */
    public static final BlockPos ROAD_DOOR = new BlockPos(13, 1, 9);
    /** The four chairs of the family table (stairs): Agatha's, Nicky's, Rio's, and the one kept for a guest. */
    public static final List<BlockPos> TABLE_SEATS = List.of(new BlockPos(8, 4, 8), new BlockPos(11, 4, 8),
            new BlockPos(8, 4, 9), new BlockPos(11, 4, 9));
    /** The three beds' heads (facing north): Agatha's, Nicky's, Rio's. */
    public static final List<BlockPos> BEDS = List.of(new BlockPos(6, 8, 5), new BlockPos(13, 8, 5),
            new BlockPos(20, 8, 5));
    /** The garden gate in the picket fence. */
    public static final BlockPos GATE = new BlockPos(13, 4, 0);

    // ---- lane L6's points of the residents' day (CottageGoals) ----

    /** The cauldron of soup over its fire (what Agatha stirs). */
    public static final BlockPos POT = new BlockPos(5, 4, 6);
    /** Where Agatha stands to stir the pot. */
    public static final BlockPos STOVE = new BlockPos(6, 4, 6);
    /** Where Agatha stands to water the herbs on the kitchen's shelves (they are on its west wall). */
    public static final BlockPos HERBS = new BlockPos(5, 4, 8);
    /** The two rocking chairs on the porch (stairs facing the gate). */
    public static final List<BlockPos> PORCH_CHAIRS = List.of(new BlockPos(10, 4, 2), new BlockPos(16, 4, 2));
    /** The garden bench (two stairs facing the flower beds). */
    public static final List<BlockPos> BENCH = List.of(new BlockPos(11, 4, 18), new BlockPos(12, 4, 18));
    /** Inside Rio's greenhouse. */
    public static final BlockPos GREENHOUSE = new BlockPos(4, 4, 20);
    /** Where Rio tends the flower beds, pumpkins and paths. */
    public static final List<BlockPos> GARDEN = List.of(new BlockPos(9, 4, 17), new BlockPos(11, 4, 20),
            new BlockPos(15, 4, 17), new BlockPos(18, 4, 21));
    /** The middle of the back yard, where Nicky plays. */
    public static final BlockPos YARD = new BlockPos(17, 4, 15);
    /** Outside the gate of Señor Scratchy's pen, where Nicky feeds him. */
    public static final BlockPos HUTCH_GATE = new BlockPos(22, 4, 15);

    private CottagePlan() {
    }
}
