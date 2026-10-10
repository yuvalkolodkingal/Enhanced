package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Where a teleport lands: on top of the block someone looks at, right behind a creature, or on safe ground far away
 * (the Tesseract's escape). Every spot returned leaves room for the traveller's whole body.
 */
public final class Teleports {

    /**
     * The spot for someone teleporting to the block they look at: standing on top of it if there is room, otherwise
     * just in front of the face they hit. Null if neither has room.
     */
    @Nullable
    public static Vec3 onto(Entity traveller, BlockHitResult hit) {
        ServerLevel level = (ServerLevel) traveller.level();
        BlockPos pos = hit.getBlockPos();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        double top = pos.getY() + (shape.isEmpty() ? 0.0 : shape.max(Direction.Axis.Y));
        Vec3 onTop = new Vec3(pos.getX() + 0.5, top, pos.getZ() + 0.5);
        if (fits(traveller, onTop)) {
            return onTop;
        }
        Direction face = hit.getDirection();
        Vec3 out = hit.getLocation().add(face.getStepX() * (traveller.getBbWidth() * 0.5 + 0.05),
                face == Direction.DOWN ? -traveller.getBbHeight() - 0.05 : 0.0,
                face.getStepZ() * (traveller.getBbWidth() * 0.5 + 0.05));
        for (double dy : new double[]{0.0, -0.5, -1.0, -1.5, 0.5}) {
            Vec3 spot = out.add(0, dy, 0);
            if (fits(traveller, spot)) {
                return spot;
            }
        }
        return null;
    }

    /** A spot right behind {@code target} (opposite to where its body faces), or null if there is no room there. */
    @Nullable
    public static Vec3 behind(Entity traveller, LivingEntity target) {
        float yaw = target.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw));
        double gap = target.getBbWidth() * 0.5 + traveller.getBbWidth() * 0.5 + 0.35;
        for (double extra : new double[]{0.0, 0.4, 0.8}) {
            Vec3 spot = target.position().add(back.scale(gap + extra));
            if (fits(traveller, spot)) {
                return spot;
            }
        }
        return null;
    }

    /** Whether the traveller's body fits at {@code feet} without touching a block. */
    public static boolean fits(Entity traveller, Vec3 feet) {
        AABB box = traveller.getBoundingBox().move(feet.subtract(traveller.position()));
        return traveller.level().getWorldBorder().isWithinBounds(box) && traveller.level().noCollision(traveller, box);
    }

    /**
     * A safe spot on the ground between {@code min} and {@code max} blocks away in a random direction, in the same
     * dimension: solid ground underfoot, room for the whole body, no liquid, nothing burning, and no Hex wall in
     * between. Gives up (null) after {@code tries} random spots.
     */
    @Nullable
    public static Vec3 farAway(LivingEntity traveller, RandomSource random, double min, double max, int tries) {
        ServerLevel level = (ServerLevel) traveller.level();
        for (int i = 0; i < tries; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = min + random.nextDouble() * (max - min);
            int x = Mth.floor(traveller.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(traveller.getZ() + Math.sin(angle) * distance);
            if (!level.getWorldBorder().isWithinBounds(x, z, 1.0)) {
                continue;
            }
            level.getChunk(x >> 4, z >> 4);
            Vec3 spot = groundAt(traveller, level, x, z);
            if (spot != null && !crossesHex(level, traveller, spot)) {
                return spot;
            }
        }
        return null;
    }

    /** The highest safe standing spot in this column (below the roof, in a dimension with one). */
    @Nullable
    private static Vec3 groundAt(LivingEntity traveller, ServerLevel level, int x, int z) {
        int bottom = level.getMinBuildHeight();
        int top = level.dimensionType().hasCeiling()
                ? bottom + level.dimensionType().logicalHeight() - 2
                : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos(x, top, z);
        for (; feet.getY() > bottom; feet.move(Direction.DOWN)) {
            if (safeToStand(level, feet)) {
                Vec3 spot = Vec3.atBottomCenterOf(feet);
                if (fits(traveller, spot) && !level.containsAnyLiquid(traveller.getBoundingBox().move(spot.subtract(traveller.position())))) {
                    return spot;
                }
            }
            if (!level.dimensionType().hasCeiling() && !level.getBlockState(feet).isAir()
                    && !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
                // Under the open sky the first solid block from the top is the surface: never land in a cave.
                return null;
            }
        }
        return null;
    }

    /** Solid, harmless ground under {@code feet}, and air (or plants) at the feet and head. */
    private static boolean safeToStand(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP) || !ground.getFluidState().isEmpty() || dangerous(ground)) {
            return false;
        }
        for (BlockPos pos : new BlockPos[]{feet, feet.above()}) {
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty() || dangerous(state)) {
                return false;
            }
        }
        return true;
    }

    private static boolean dangerous(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.LAVA)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.COBWEB);
    }

    /** Whether moving the traveller to {@code target} would take them across a Hex wall (either way). */
    private static boolean crossesHex(ServerLevel level, LivingEntity traveller, Vec3 target) {
        Vec3 from = traveller.position().add(0, traveller.getBbHeight() * 0.5, 0);
        Vec3 to = target.add(0, traveller.getBbHeight() * 0.5, 0);
        for (Hex hex : HexManager.data(level).hexes()) {
            if (hex.contains(from) != hex.contains(to)) {
                return true;
            }
        }
        return false;
    }

    private Teleports() {
    }
}
