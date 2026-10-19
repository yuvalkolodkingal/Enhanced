package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

/**
 * The fixed points of the Hydra outpost (3.6, the plan's 15.6) in its template {@code hydra_outpost/base} of 3.3 (33 x
 * 22 x 29, unchanged): template-relative and unturned (x east, z south, y the template's layer; the ground is layer
 * {@link #FLOOR}). Lane L8 posts the garrison on them, turned as the outpost was ({@link #toWorld}). The skeleton
 * checked each point against {@code SCRATCH/v33/world_tools/hydra_outpost.py} and the shipped {@code base.nbt}; two
 * moved (see {@link #TOWER_POST} and {@link #YARD_ROUTE}).
 */
public final class HydraOutpostPlan {
    public static final ResourceLocation TEMPLATE = Enhanced.id("hydra_outpost/base");
    public static final Vec3i SIZE = new Vec3i(33, 22, 29);
    /** The layer at ground level (the cellar is below it). */
    public static final int FLOOR = 5;
    /** The point the template turns about ({@code TemplateSitePiece}: the middle of its footprint, at layer 0). */
    public static final BlockPos PIVOT = new BlockPos(SIZE.getX() / 2, 0, SIZE.getZ() / 2);

    /** The middle of the compound (the leash is measured from it). */
    public static final BlockPos CENTRE = new BlockPos(16, 6, 14);
    /** The gate in the fence (south). */
    public static final BlockPos GATE = new BlockPos(13, 6, 27);
    /** The gate guards' two posts, inside the gate between the sandbags. */
    public static final List<BlockPos> GATE_POSTS = List.of(new BlockPos(12, 6, 25), new BlockPos(14, 6, 25));
    /** The bunker's iron door (its lower half). */
    public static final BlockPos DOOR = new BlockPos(13, 6, 17);
    /**
     * The sniper's post on the watchtower's platform (on its planks, {@link #TOWER_FLOOR}). The plan's (3, 15, 3) is
     * under the tower's hanging lantern at (3, 16, 3): moved one south.
     */
    public static final BlockPos TOWER_POST = new BlockPos(3, 15, 4);
    /** The watchtower's planks under the sniper. */
    public static final BlockPos TOWER_FLOOR = new BlockPos(3, 14, 4);
    /** The officer's post by day: the command room. */
    public static final BlockPos COMMAND_POST = new BlockPos(17, 6, 10);
    /** The officer's post by night: the radio room on the roof. */
    public static final BlockPos RADIO_POST = new BlockPos(16, 11, 9);
    /** The radio console (an observer) where the alarm is raised and silenced. */
    public static final BlockPos ALARM_CONSOLE = new BlockPos(16, 12, 8);
    /** The redstone lamps the alarm lights: two in the radio room, two in the command room. */
    public static final List<BlockPos> ALARM_LAMPS = List.of(new BlockPos(15, 12, 8), new BlockPos(18, 12, 8),
            new BlockPos(16, 8, 7), new BlockPos(19, 8, 7));
    /** The enforcer's post at the foot of the cellar stairs. */
    public static final BlockPos CELLAR_POST = new BlockPos(13, 1, 8);
    /** Where the reinforcements come up from (out of sight, in the cellar). */
    public static final BlockPos CELLAR_SPAWN = new BlockPos(13, 1, 7);
    /**
     * The yard patrol's loop round the bunker. The plan's last corner (4, 6, 19) is a wheel of the truck: moved one
     * east.
     */
    public static final List<BlockPos> YARD_ROUTE = List.of(new BlockPos(4, 6, 4), new BlockPos(21, 6, 4),
            new BlockPos(21, 6, 19), new BlockPos(5, 6, 19));
    /** The perimeter patrol's loop, 6 outside the fence; y is the ground's at each corner (found when posted). */
    public static final List<BlockPos> PERIMETER_ROUTE = List.of(new BlockPos(-6, FLOOR, -6),
            new BlockPos(38, FLOOR, -6), new BlockPos(38, FLOOR, 33), new BlockPos(-6, FLOOR, 33));
    /**
     * Cover (the blocks themselves; a soldier takes cover beside one): the sandbags by the gate and the helipad, the
     * truck, the bunker's door.
     */
    public static final List<BlockPos> COVER = List.of(
            new BlockPos(8, 6, 25), new BlockPos(9, 6, 25), new BlockPos(10, 6, 25),
            new BlockPos(16, 6, 25), new BlockPos(17, 6, 25), new BlockPos(18, 6, 25),
            new BlockPos(29, 6, 20), new BlockPos(29, 6, 22), new BlockPos(29, 6, 23), new BlockPos(29, 6, 25),
            new BlockPos(29, 6, 26),
            new BlockPos(2, 6, 21), DOOR);

    /**
     * The template's position (its layer 0's north-west corner, unturned) of an outpost whose piece was centred on
     * {@code center} (the piece's ground point, as {@code TemplateSitePiece} places it).
     */
    public static BlockPos templatePosition(BlockPos center) {
        return center.offset(-SIZE.getX() / 2, -FLOOR, -SIZE.getZ() / 2);
    }

    /**
     * Where a template-relative point is in the world, for an outpost at {@code templatePosition} turned {@code
     * rotation}.
     */
    public static BlockPos toWorld(BlockPos local, Rotation rotation, BlockPos templatePosition) {
        return StructureTemplate.transform(local, Mirror.NONE, rotation, PIVOT).offset(templatePosition);
    }

    private HydraOutpostPlan() {
    }
}
