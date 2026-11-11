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
