package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.ultron.SokoviaShape.Spec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Whether the island of Sokovia can be built in the sky above a piece of ground: the rules of the Sky Island, which is
 * the only mode that is shipped. Because the island is a copy and the real ground is never edited, none of the rules is
 * about what is built on the ground (a house beside the fight, or under it, is left alone: its blocks are simply not
 * copied). What is checked is the room the island needs:
 *
 * <ol>
 *     <li><b>It fits:</b> the chunks of the island and of the sky above it are loaded, and the island's highest block
 *     is inside the world's height;</li>
 *     <li><b>Nothing hangs:</b> no item frame, painting, leash knot, armour stand, minecart, boat or end crystal in the
 *     island's cylinder (the ritual only: they hold on to blocks or to each other);</li>
 *     <li><b>The sky is empty:</b> the cells the island will take, and a margin of six blocks round them, are air (no
 *     mountain, no tree canopy, no build floating there);</li>
 *     <li><b>It is allowed:</b> spawn protection and claims, asked at the middle and eight points of the rim in the sky
 *     (the ritual only).</li>
 * </ol>
 *
 * <p>The ground's own rules (level enough, mostly walkable) are the ritual's {@code UltronLane.site}. A refusal is a
 * message for the starter; at the lift's warning it makes the fight go on as the Tremor instead.
 */
public final class SokoviaValidator {
    /** The air that must surround the island in the sky. */
    public static final int SKY_MARGIN = 6;
    /** How far past the rim a thing hanging on the ground still counts as in the island's cylinder. */
    public static final double CYLINDER_MARGIN = 1.5;

    /**
     * The first reason the island cannot be lifted from the ground round {@code centre}, or null.
     *
     * @param starter whoever starts the fight, for the protection rule and the hanging things (null skips both: the
     *                lift's warning, which only asks whether the room is still there)
     */
    @Nullable
    public static Component refusal(ServerLevel level, Spec spec, BlockPos centre, @Nullable ServerPlayer starter) {
        int reach = spec.extent() + SKY_MARGIN;
        for (int x = (centre.getX() - reach) >> 4; x <= (centre.getX() + reach) >> 4; x++) {
            for (int z = (centre.getZ() - reach) >> 4; z <= (centre.getZ() + reach) >> 4; z++) {
                if (!level.hasChunk(x, z)) {
                    return Component.translatable("message.enhanced.ultron.refuse.unloaded");
                }
            }
        }
        if (starter != null) {
            Component hanging = hanging(level, spec, centre);
            if (hanging != null) {
                return hanging;
            }
        }
        if (!skyIsEmpty(level, spec, centre)) {
            return Component.translatable("message.enhanced.ultron.refuse.sky");
        }
        if (cutByHex(level, spec, centre)) {
            // 3.3: the island carries no one through the wall of a Hex (only those Wanda lets in may cross it)
            return Component.translatable("message.enhanced.ultron.refuse.hex");
        }
        if (starter != null) {
            for (BlockPos probe : probes(spec, centre)) {
                BlockPos sky = probe.above(spec.height());
                BlockState state = level.getBlockState(sky);
                if (!level.mayInteract(starter, sky) || !BlockRights.postBreak(starter, level, sky, state)) {
                    return Component.translatable("message.enhanced.ultron.refuse.protected");
                }
            }
        }
        return null;
    }

    /**
     * Whether the wall of a Hex cuts the column the island rises through (from under the ground it takes to the sky it is lifted
     * to, with the margin round it): part of the column in a Hex and part out. A Hex all round the column, or far off, is no
     * matter.
     */
    public static boolean cutByHex(ServerLevel level, Spec spec, BlockPos centre) {
        double radius = spec.extent() + SKY_MARGIN;
        double low = centre.getY() - spec.skin() - spec.cone() - SKY_MARGIN;
        double high = centre.getY() + spec.height() + spec.cover() + SKY_MARGIN;
        for (Hex hex : HexManager.data(level).hexes()) {
            Vec3 c = hex.center();
            double flat = Math.hypot(c.x - (centre.getX() + 0.5), c.z - (centre.getZ() + 0.5));
            double nearFlat = Math.max(0.0, flat - radius);
            double nearUp = c.y < low ? low - c.y : c.y > high ? c.y - high : 0.0;
            double farFlat = flat + radius;
            double farUp = Math.max(Math.abs(c.y - low), Math.abs(c.y - high));
            double r2 = (double) hex.radius() * hex.radius();
            boolean meets = nearFlat * nearFlat + nearUp * nearUp < r2;
            boolean within = farFlat * farFlat + farUp * farUp < r2;
            if (meets && !within) {
                return true;
            }
        }
        return false;
    }

    /** The first thing that hangs on to a block or a vehicle inside the island's cylinder, as a refusal, or null. */
    @Nullable
    private static Component hanging(ServerLevel level, Spec spec, BlockPos centre) {
        int extent = spec.extent();
        AABB box = new AABB(centre.getX() - extent - 2, level.getMinBuildHeight(), centre.getZ() - extent - 2,
                centre.getX() + extent + 3, level.getMaxBuildHeight(), centre.getZ() + extent + 3);
        for (Entity entity : level.getEntities((Entity) null, box, SokoviaValidator::hangs)) {
            int dx = entity.getBlockX() - centre.getX();
            int dz = entity.getBlockZ() - centre.getZ();
            double rim = SokoviaShape.rim(spec, Math.atan2(dz, dx)) + CYLINDER_MARGIN;
            if (dx * dx + dz * dz <= rim * rim) {
                return Component.translatable("message.enhanced.ultron.refuse.entity", entity.getName(), entity.blockPosition().toShortString());
            }
        }
        return null;
    }

    private static boolean hangs(Entity entity) {
        return entity instanceof HangingEntity || entity instanceof ArmorStand || entity instanceof VehicleEntity || entity instanceof EndCrystal;
    }

    /**
     * Whether the air the island will take in the sky, with {@link #SKY_MARGIN} blocks round it, is empty; false too if
     * it would not fit under the top of the world.
     */
    public static boolean skyIsEmpty(ServerLevel level, Spec spec, BlockPos centre) {
        int extent = spec.extent();
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int dz = -extent; dz <= extent; dz++) {
            for (int dx = -extent; dx <= extent; dx++) {
                if (!SokoviaShape.inside(spec, dx, dz)) {
                    continue;
                }
                int g = SokoviaTemplate.groundHeight(level, centre.getX() + dx, centre.getZ() + dz, centre.getY(), spec);
                if (g == SokoviaTemplate.NO_GROUND) {
                    g = centre.getY() - 1;
                }
                low = Math.min(low, g - spec.skin() - SokoviaShape.coneDepth(spec, dx, dz) + 1);
                high = Math.max(high, g + spec.cover());
            }
        }
        low += spec.height() - SKY_MARGIN;
        high += spec.height() + SKY_MARGIN;
        if (high >= level.getMaxBuildHeight() || low < level.getMinBuildHeight()) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int wide = extent + SKY_MARGIN;
        for (int dz = -wide; dz <= wide; dz++) {
            for (int dx = -wide; dx <= wide; dx++) {
                double rim = SokoviaShape.rim(spec, Math.atan2(dz, dx)) + SKY_MARGIN;
                if (dx * dx + dz * dz > rim * rim) {
                    continue;
                }
                for (int y = low; y <= high; y++) {
                    if (!level.getBlockState(pos.set(centre.getX() + dx, y, centre.getZ() + dz)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The middle and eight points of the rim, on the ground: where the protection is asked. */
    private static BlockPos[] probes(Spec spec, BlockPos centre) {
        BlockPos[] points = new BlockPos[9];
        points[0] = centre;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            double rim = SokoviaShape.rim(spec, angle);
            points[i + 1] = BlockPos.containing(Vec3.atCenterOf(centre).add(Math.cos(angle) * rim, 0.0, Math.sin(angle) * rim));
        }
        return points;
    }

    private SokoviaValidator() {
    }
}
