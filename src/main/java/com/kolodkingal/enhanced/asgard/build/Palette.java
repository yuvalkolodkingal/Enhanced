package com.kolodkingal.enhanced.asgard.build;

import com.kolodkingal.enhanced.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

/** The blocks the builders of Asgard use (3.5): the realm's gold and stone, and a few of the game's. */
public final class Palette {
    public static BlockState gold() {
        return ModBlocks.ASGARD_GOLD_PLATING.get().defaultBlockState();
    }

    public static BlockState trim() {
        return ModBlocks.ASGARD_GOLD_TRIM.get().defaultBlockState();
    }

    public static BlockState goldPillar() {
        return ModBlocks.ASGARD_GOLD_PILLAR.get().defaultBlockState();
    }

    public static BlockState stone() {
        return ModBlocks.REALM_STONE.get().defaultBlockState();
    }

    public static BlockState bricks() {
        return ModBlocks.REALM_STONE_BRICKS.get().defaultBlockState();
    }

    public static BlockState polished() {
        return ModBlocks.POLISHED_REALM_STONE.get().defaultBlockState();
    }

    public static BlockState stonePillar() {
        return ModBlocks.REALM_STONE_PILLAR.get().defaultBlockState();
    }

    public static BlockState wall() {
        return ModBlocks.REALM_STONE_BRICK_WALL.get().defaultBlockState();
    }

    /** A stair whose high side is toward {@code up} (one walks up toward it). */
    public static BlockState stairs(Direction up) {
        return ModBlocks.REALM_STONE_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, up)
                .setValue(StairBlock.HALF, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
    }

    public static BlockState bridge(Direction.Axis axis) {
        return ModBlocks.BIFROST_BRIDGE.get().defaultBlockState().setValue(com.kolodkingal.enhanced.block.asgard.BifrostBridgeBlock.AXIS, axis);
    }

    public static BlockState falls() {
        return ModBlocks.COSMIC_FALLS.get().defaultBlockState();
    }

    public static BlockState lattice() {
        return ModBlocks.ASGARD_GOLD_LATTICE.get().defaultBlockState();
    }

    public static BlockState brazier() {
        return ModBlocks.ASGARD_BRAZIER.get().defaultBlockState();
    }

    public static BlockState glass() {
        return Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    }

    public static BlockState water() {
        return Blocks.WATER.defaultBlockState();
    }

    public static BlockState log(Block log) {
        return log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    /** Leaves that never fall (persistent: no ticks). */
    public static BlockState leaves(Block leaves) {
        return leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private Palette() {
    }
}
