package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.boss.BossArena;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where Ultron is fought: the ground of the ritual, a disc of {@code R + 12} blocks round the clicked block (the island's
 * radius {@code R} and a margin), and, while the Sokovia lift has the island up, the island itself. It is the framework's
 * arena with Ultron's numbers: nothing in it can be broken, placed, flooded, burned or blown up, nothing spawns in it,
 * the field brings back a fighter who strays past the edge (2 damage that cannot kill, a few seconds of Slowness),
 * the tractor lifts one who ends up 12 blocks under the ground, no teleport takes a fighter out of it, the drops of one
 * who dies stay until the fight is over, and players within {@code R + 24} of the middle join the fight.
 *
 * <p>The ground is found from the height of the middle, not from the sky, so that it works under a roof too; and when the
 * island is up ({@link #setIslandOffset}) the surface where a fighter is put back is the island's, {@code offset} blocks
 * higher.
 */
public final class UltronArena extends BossArena {
    private final int islandRadius;
    private double islandOffset;

    public UltronArena(ServerLevel level, Vec3 center, int islandRadius) {
        super(level, center, islandRadius + 12.0, Containment.FIELD, true);
        this.islandRadius = islandRadius;
        containAt(islandRadius + 12.0);
        joinWithin(islandRadius + 24.0);
        tickets(islandRadius + 24, 3);
        keepDrops(true);
        tractorDepth(12.0);
    }

    /** The radius of the island (and of the ground fight before it). */
    public int islandRadius() {
        return islandRadius;
    }

    /** How far up the island is now: 0 on the ground. */
    public double islandOffset() {
        return islandOffset;
    }

    /** Called by the Sokovia lift: the island is {@code offset} blocks up (0 when it is down). */
    public void setIslandOffset(double offset) {
        this.islandOffset = offset;
    }

    /** The surface of the column of {@code (x, z)}, as the ritual's ground sees it, plus the height of the island. */
    @Nullable
    @Override
    protected Vec3 surfaceSpot(ServerPlayer player, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!level().hasChunk(bx >> 4, bz >> 4)) {
            return null;
        }
        double y = UltronMotion.surface(level(), x, center().y + 4.0, z) + islandOffset;
        Vec3 spot = new Vec3(x, y, z);
        return Teleports.fits(player, spot) ? spot : new Vec3(x, y + 1.0, z);
    }

    /** Back inside the field, three blocks in from its edge; on the island, three blocks inside the rim. */
    @Override
    protected Vec3 rescueSpot(ServerPlayer player) {
        if (islandOffset <= 0.0) {
            return super.rescueSpot(player);
        }
        double dx = player.getX() - center().x;
        double dz = player.getZ() - center().z;
        double length = Math.hypot(dx, dz);
        double reach = Math.max(1.0, islandRadius - 3.0);
        double x = length <= reach || length < 1.0E-6 ? player.getX() : center().x + dx / length * reach;
        double z = length <= reach || length < 1.0E-6 ? player.getZ() : center().z + dz / length * reach;
        Vec3 spot = surfaceSpot(player, x, z);
        return spot != null ? spot : center().add(0, islandOffset, 0);
    }
}
