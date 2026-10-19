package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.world.StoneSitePlans;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

/**
 * One Vormir Cliff in the world (3.6, the plan's 6.1): its places in world coordinates, the plan's fixed points ({@link
 * StoneSitePlans.Vormir}) carried through the template's placement (its corner, rotation and mirror). A site found in
 * the world comes from its structure ({@link VormirSites#at}); the GameTests build small ones of their own with the
 * same parts.
 *
 * @param dimension  the level it stands in
 * @param bounds     the whole site (the template's box)
 * @param edge       the strip at the chasm's lip where a fall must begin ({@code EDGE_BOX}; feet inside it)
 * @param pit        the shaft of the chasm where the fall must end ({@code PIT_BOX})
 * @param pool       the shallow pool where the stone is given ({@code POOL})
 * @param keeperHome where the Stonekeeper stands ({@code STONEKEEPER_HOME})
 */
public record VormirSite(ResourceKey<Level> dimension, BoundingBox bounds, BoundingBox edge, BoundingBox pit,
        BlockPos pool, BlockPos keeperHome) {

    /** The site of a cliff's template placed at {@code corner} with the given turn ({@code pivot}: its pivot). */
    public static VormirSite of(ResourceKey<Level> dimension, BlockPos corner, Mirror mirror, Rotation rotation,
            BlockPos pivot) {
        Placement at = local -> StructureTemplate.transform(local, mirror, rotation, pivot).offset(corner);
        BlockPos far = new BlockPos(StoneSitePlans.Vormir.SIZE.getX() - 1, StoneSitePlans.Vormir.SIZE.getY() - 1,
                StoneSitePlans.Vormir.SIZE.getZ() - 1);
        return new VormirSite(dimension, BoundingBox.fromCorners(at.world(BlockPos.ZERO), at.world(far)),
                box(at, StoneSitePlans.Vormir.EDGE_BOX), box(at, StoneSitePlans.Vormir.PIT_BOX),
                at.world(StoneSitePlans.Vormir.POOL), at.world(StoneSitePlans.Vormir.STONEKEEPER_HOME));
    }

    /** The site of an unturned template at {@code corner}. */
    public static VormirSite of(ResourceKey<Level> dimension, BlockPos corner) {
        return of(dimension, corner, Mirror.NONE, Rotation.NONE, BlockPos.ZERO);
    }

    private static BoundingBox box(Placement at, BoundingBox local) {
        return BoundingBox.fromCorners(at.world(new BlockPos(local.minX(), local.minY(), local.minZ())),
                at.world(new BlockPos(local.maxX(), local.maxY(), local.maxZ())));
    }

    /** Whether the creature stands on the edge: on the ground, its feet inside the strip. */
    public boolean standsOnEdge(LivingEntity entity) {
        return entity.onGround() && entity.level().dimension() == dimension && edge.isInside(entity.blockPosition());
    }

    /** Whether the creature is down in the chasm's shaft. */
    public boolean inPit(LivingEntity entity) {
        return entity.level().dimension() == dimension && pit.isInside(entity.blockPosition());
    }

    /** Whether a place is on the summit: inside the site, no lower than a little under the edge. */
    public boolean onSummit(BlockPos pos) {
        return bounds.isInside(pos) && pos.getY() >= edge.minY() - 3;
    }

    /** The space to look for creatures standing on the edge in. */
    public AABB edgeSpace() {
        return AABB.of(edge).inflate(1.0, 1.0, 1.0);
    }

    /** A point of the template to the world. */
    @FunctionalInterface
    private interface Placement {
        BlockPos world(BlockPos local);
    }
}
