package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.phys.AABB;

/** The paintings of Vision's mind (3.12): the cinema's title card and the workshop's Human Torch. */
public final class MindPaintings {
    public static final ResourceKey<PaintingVariant> CINEMA =
            ResourceKey.create(Registries.PAINTING_VARIANT, Enhanced.id("mind_cinema"));
    public static final ResourceKey<PaintingVariant> HUMAN_TORCH =
            ResourceKey.create(Registries.PAINTING_VARIANT, Enhanced.id("human_torch"));

    private MindPaintings() {
    }

    /** Hangs the painting at {@code pos} (or one block around, wherever it fits). */
    public static Painting place(ServerLevel level, BlockPos pos, Direction facing, ResourceKey<PaintingVariant> key) {
        Holder<PaintingVariant> variant = level.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getHolderOrThrow(key);
        Direction along = facing.getCounterClockWise();
        for (int up = 0; up <= 1; up++) {
            for (int side = 0; side <= 2; side++) {
                BlockPos at = pos.relative(along, side == 2 ? -1 : side).above(up);
                Painting painting = new Painting(level, at, facing, variant);
                if (painting.survives()) {
                    level.addFreshEntity(painting);
                    return painting;
                }
            }
        }
        return null;
    }

    /**
     * Hangs the painting so that it covers the blocks from {@code low} to {@code high} (both corners of the wall's face it
     * hangs on): of the places around their middle where it fits, the one that covers them best (3.15: the cinema's
     * picture, the whole screen). Falls back to {@link #place} if none covers them.
     */
    public static Painting placeCovering(ServerLevel level, BlockPos low, BlockPos high, Direction facing, ResourceKey<PaintingVariant> key) {
        Holder<PaintingVariant> variant = level.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getHolderOrThrow(key);
        AABB want = new AABB(low.getX(), low.getY(), low.getZ(), high.getX() + 1, high.getY() + 1, high.getZ() + 1);
        BlockPos middle = new BlockPos((low.getX() + high.getX()) / 2, (low.getY() + high.getY()) / 2, (low.getZ() + high.getZ()) / 2);
        Painting best = null;
        double bestScore = -1.0;
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Painting painting = new Painting(level, middle.offset(dx, dy, dz), facing, variant);
                    if (!painting.survives()) {
                        continue;
                    }
                    AABB box = painting.getBoundingBox();
                    double score = overlap(box, want);
                    if (score > bestScore) {
                        best = painting;
                        bestScore = score;
                    }
                }
            }
        }
        if (best == null) {
            return place(level, middle, facing, key);
        }
        level.addFreshEntity(best);
        return best;
    }

    /** How much of {@code want} (in the wall's plane) a box covers. */
    private static double overlap(AABB box, AABB want) {
        double y = Math.max(0.0, Math.min(box.maxY, want.maxY) - Math.max(box.minY, want.minY));
        double x = Math.max(0.0, Math.min(box.maxX, want.maxX) - Math.max(box.minX, want.minX));
        double z = Math.max(0.0, Math.min(box.maxZ, want.maxZ) - Math.max(box.minZ, want.minZ));
        return y * (want.getXsize() > want.getZsize() ? x : z);
    }

    /** The painting of {@code key} near {@code pos}, if one hangs there. */
    public static Painting find(ServerLevel level, BlockPos pos, ResourceKey<PaintingVariant> key) {
        for (Painting p : level.getEntitiesOfClass(Painting.class, new AABB(pos).inflate(3))) {
            if (p.getVariant().is(key)) {
                return p;
            }
        }
        return null;
    }
}
