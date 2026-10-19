package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * The fixed points of the X-Men lower lab (3.7, the plan's 6.7), written by the 3.7 skeleton. Two frames, both unturned
 * (x east, z south): the school's template ({@code mutant_school/mansion}, 47 x 34 x 44, floor layer 6) for the study's
 * entrance and where the lab's template is put, and the lab's own template ({@code mutant_school/lower_lab}, {@link
 * #LAB_SIZE}) for what is inside it. A lab point in the world is {@code SitePoints.toWorld(piece,
 * LAB_ORIGIN.offset(local))}, turned as the school was. Lane L4 may move a point with a note and the test that checks
 * it ({@code MutantSkeletonTests} checks every lab point's block in the template).
 */
public final class SchoolLabPlan {
    /** The version of the lab's template a lab is built from (a later one may rebuild). */
    public static final int VERSION = 1;
    public static final ResourceLocation TEMPLATE = Enhanced.id("mutant_school/lower_lab");

    // ---- the school's template ----
    /**
     * Where the lab's template is put, in the school's template: under its footprint, three layers below the cellar.
     */
    public static final BlockPos LAB_ORIGIN = new BlockPos(1, -31, 1);
    /** The lab's size: it spans school-template x 1..45, y -31..-4, z 1..42. */
    public static final Vec3i LAB_SIZE = new Vec3i(45, 28, 42);
    /** The four bookshelves of the study's west wall (north of the fireplace) that swing open. */
    public static final List<BlockPos> STUDY_SHELVES = List.of(new BlockPos(4, 7, 16), new BlockPos(4, 8, 16),
            new BlockPos(4, 7, 17), new BlockPos(4, 8, 17));
    /** The study's two elevator pads, in the nook behind the shelves (they replace the floor's planks). */
    public static final List<BlockPos> STUDY_PADS = List.of(new BlockPos(4, 6, 16), new BlockPos(4, 6, 17));
    /** The study's empty lectern: the Professor's notes. */
    public static final BlockPos LECTERN = new BlockPos(5, 7, 15);
    /** Outside the school's front door: where a lab pad sends a rider whose study pad is gone. */
    public static final BlockPos FRONT_DOOR_OUTSIDE = new BlockPos(23, 7, 28);

    // ---- the lab's template ----
    /** The lab lobby's two elevator pads (under the study's). */
    public static final List<BlockPos> LAB_PADS = List.of(new BlockPos(3, 22, 15), new BlockPos(3, 22, 16));
    /** The upper level's floor and the lower level's. */
    public static final int UPPER_FLOOR = 22;
    public static final int LOWER_FLOOR = 12;
    /** Hank's Cure Station in the medical bay. */
    public static final BlockPos CURE_STATION = new BlockPos(16, 22, 6);
    /** The Weapon X rack of the Adamantium Injections. */
    public static final BlockPos ADAMANTIUM_RACK = new BlockPos(6, 12, 26);
    /** The Weapon X Pod (its lower half; the upper is one above). */
    public static final BlockPos WEAPON_X_POD = new BlockPos(10, 12, 38);
    /** The adamantium tank: a glass vat of water in an iron frame. */
    public static final BoundingBox TANK_BOX = new BoundingBox(2, 12, 30, 6, 17, 34);
    /** The round X-door of Cerebro: its bottom middle (the door is 3 x 3: z 20..22, y 12..14). */
    public static final BlockPos CEREBRO_DOOR = new BlockPos(19, 12, 21);
    /** The middle of Cerebro's sphere. */
    public static final BlockPos CEREBRO_CENTER = new BlockPos(31, 12, 21);
    /** The sphere's inner radius. */
    public static final int CEREBRO_RADIUS = 11;
    /** The Cerebro console on the central platform. */
    public static final BlockPos CEREBRO_CONSOLE = new BlockPos(31, 12, 23);
    /** The walkway from the door to the platform (2 wide, iron). */
    public static final BoundingBox WALKWAY = new BoundingBox(20, 11, 20, 31, 11, 21);

    /** A lab point in the school's template (unturned): {@link #LAB_ORIGIN} plus the point. */
    public static BlockPos inSchool(BlockPos local) {
        return LAB_ORIGIN.offset(local);
    }

    private SchoolLabPlan() {
    }
}
