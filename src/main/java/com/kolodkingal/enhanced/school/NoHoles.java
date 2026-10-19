package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.block.XDoorBlock;
import com.kolodkingal.enhanced.block.XDoorRoundBlock;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The no-holes rule of the lower lab (3.7, the plan's 6.6), checked on a built lab (the tests, {@code /enhanced
 * structure school_lab status}): every face of the box is a whole solid block with no fluid; no fluid inside but the
 * tank's; every door framed (both sides of both halves and above solid: the 3.3.1 {@code doors.py} rule, for the
 * X-doors, the round door and any vanilla door). The template script runs the same checks on the template.
 */
public final class NoHoles {

    /** The faces of the box that are not whole solid dry blocks. */
    public static List<BlockPos> shellHoles(ServerLevel level, LabSite site) {
        List<BlockPos> out = new ArrayList<>();
        BoundingBox box = site.box();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    if (x != box.minX() && x != box.maxX() && y != box.minY() && y != box.maxY()
                            && z != box.minZ() && z != box.maxZ()) {
                        continue;
                    }
                    p.set(x, y, z);
                    if (!whole(level, p)) {
                        out.add(p.immutable());
                    }
                }
            }
        }
        return out;
    }

    /** Whether the block at {@code p} is whole, solid and dry. */
    public static boolean whole(BlockGetter level, BlockPos p) {
        BlockState state = level.getBlockState(p);
        return state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(level, p);
    }

    /** The fluids inside the box but the tank's water. */
    public static List<BlockPos> strayFluids(ServerLevel level, LabSite site) {
        List<BlockPos> out = new ArrayList<>();
        BoundingBox tank = SchoolLabPlan.TANK_BOX;
        for (int x = 0; x < SchoolLabPlan.LAB_SIZE.getX(); x++) {
            for (int y = 0; y < SchoolLabPlan.LAB_SIZE.getY(); y++) {
                for (int z = 0; z < SchoolLabPlan.LAB_SIZE.getZ(); z++) {
                    if (x > tank.minX() && x < tank.maxX() && y > tank.minY() && y < tank.maxY() && z > tank.minZ()
                            && z < tank.maxZ()) {
                        continue;
                    }
                    BlockPos p = site.lab(x, y, z);
                    if (!level.getBlockState(p).getFluidState().isEmpty()) {
                        out.add(p);
                    }
                }
            }
        }
        return out;
    }

    /** The doors of the box with a gap beside or above them. */
    public static List<BlockPos> unframedDoors(ServerLevel level, LabSite site) {
        List<BlockPos> out = new ArrayList<>();
        BoundingBox box = site.box();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (!framed(level, p)) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    /** Whether the block at {@code p}, if it is a door (lower half) or a round door's part, is framed. */
    public static boolean framed(BlockGetter level, BlockPos p) {
        BlockState state = level.getBlockState(p);
        if (state.getBlock() instanceof XDoorBlock || state.getBlock() instanceof DoorBlock) {
            Direction facing = state.getValue(XDoorBlock.FACING);
            if (state.getValue(XDoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                return true;
            }
            Direction side = facing.getClockWise();
            for (BlockPos s : List.of(p.relative(side), p.relative(side.getOpposite()))) {
                for (BlockPos q : List.of(s, s.above())) {
                    BlockState there = level.getBlockState(q);
                    if (!whole(level, q) && !(there.getBlock() instanceof XDoorBlock)
                            && !(there.getBlock() instanceof DoorBlock)) {
                        return false;
                    }
                }
            }
            return whole(level, p.above(2));
        }
        if (state.is(ModBlocks.X_DOOR_ROUND.get())) {
            int part = state.getValue(XDoorRoundBlock.PART);
            int col = part % 3;
            int row = part / 3;
            Direction right = state.getValue(XDoorRoundBlock.FACING).getClockWise();
            if (col == 0 && !whole(level, p.relative(right.getOpposite()))) {
                return false;
            }
            if (col == 2 && !whole(level, p.relative(right))) {
                return false;
            }
            return row != 2 || whole(level, p.above());
        }
        return true;
    }

    private NoHoles() {
    }
}
