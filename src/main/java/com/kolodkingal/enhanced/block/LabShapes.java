package com.kolodkingal.enhanced.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shapes of the X-Men lower lab's blocks (3.7): boxes drawn for a block facing north (in sixteenths, as {@code
 * Block.box}) and turned for the other facings the way their block models are turned (a quarter clockwise seen from
 * above for each step north → east → south → west).
 */
public final class LabShapes {

    /** One shape per horizontal facing, from boxes {x0, y0, z0, x1, y1, z1} drawn facing north. */
    public static Map<Direction, VoxelShape> turned(double[]... boxes) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                shape = Shapes.or(shape, turn(b, facing));
            }
            out.put(facing, shape.optimize());
        }
        return out;
    }

    /** A box drawn facing north, turned to {@code facing}. */
    public static VoxelShape turn(double[] b, Direction facing) {
        double x0 = b[0];
        double z0 = b[2];
        double x1 = b[3];
        double z1 = b[5];
        double ax;
        double az;
        double bx;
        double bz;
        switch (facing) {
            case EAST -> {
                ax = 16 - z0;
                az = x0;
                bx = 16 - z1;
                bz = x1;
            }
            case SOUTH -> {
                ax = 16 - x0;
                az = 16 - z0;
                bx = 16 - x1;
                bz = 16 - z1;
            }
            case WEST -> {
                ax = z0;
                az = 16 - x0;
                bx = z1;
                bz = 16 - x1;
            }
            default -> {
                ax = x0;
                az = z0;
                bx = x1;
                bz = z1;
            }
        }
        return Block.box(Math.min(ax, bx), b[1], Math.min(az, bz), Math.max(ax, bx), b[4], Math.max(az, bz));
    }

    private LabShapes() {
    }
}
