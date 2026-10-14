package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.boss.BossArena;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
 * island is up ({@link #raiseIsland}) the surface where a fighter who is on it is put back is the island's, {@code
 * offset} blocks higher (a fighter on the ground under it is put back on the ground).
 *
 * <p>While it is up, the island is protected whatever {@code boss.protectArena} says ({@link #isCopyCell}): its blocks are
 * copies, which no ability may take for real ones (a diamond-ore copy mined would give diamonds), so block telekinesis only
 * borrows them. The ground it was copied from is hidden by the clients ({@link #hidesGround}): nothing is taken from it
 * and nothing lands on it until the island is down.
 */
public final class UltronArena extends BossArena {
    /** How deep under the ground a fighter may go before the tractor lifts them out (only while the island is down). */
    public static final double TRACTOR_DEPTH = 12.0;

    private final int islandRadius;
    private double islandOffset;
    private boolean islandUp;
    private double islandFloor;
    /** The island's blocks as they were copied from the ground, while it is up (null if none was copied). */
    @Nullable
    private SokoviaTemplate copiedFrom;

    public UltronArena(ServerLevel level, Vec3 center, int islandRadius) {
        super(level, center, islandRadius + 12.0, Containment.FIELD, true);
        this.islandRadius = islandRadius;
        containAt(islandRadius + 12.0);
        joinWithin(islandRadius + 24.0);
        tickets(islandRadius + 24, 3);
        keepDrops(true);
        tractorDepth(TRACTOR_DEPTH);
    }

    /** The radius of the island (and of the ground fight before it). */
    public int islandRadius() {
        return islandRadius;
    }

    /** How far up the island is now: 0 on the ground. */
    public double islandOffset() {
        return islandOffset;
    }

    /**
     * Called by the Sokovia lift while the island rises or comes down: it is {@code offset} blocks up (0 when it is
     * down).
     */
    public void setIslandOffset(double offset) {
        this.islandOffset = offset;
    }

    /**
     * Called by the Sokovia lift when the island is placed in the sky: the fighters on it are put back on it, not on
     * the ground, and the tractor (which lifts a fighter out of the ground) is off, since under the island there is
     * only air.
     *
     * @param offset how many blocks up it is
     * @param floorY the height of its lowest block (what has fallen below it has fallen off)
     */
    public void raiseIsland(double offset, double floorY) {
        raiseIsland(offset, floorY, null);
    }

    /**
     * The same, with the island's blocks as they were copied from the ground ({@code template}): its columns are the ground
     * the clients hide while it is up ({@link #hidesGround}).
     */
    public void raiseIsland(double offset, double floorY, @Nullable SokoviaTemplate template) {
        this.islandOffset = offset;
        this.islandFloor = floorY;
        this.islandUp = true;
        this.copiedFrom = template;
        tractorDepth(0.0);
    }

    /** Called when the island is down again, or was never up: the arena is the ground fight's again. */
    public void landIsland() {
        this.islandOffset = 0.0;
        this.islandUp = false;
        this.copiedFrom = null;
        tractorDepth(TRACTOR_DEPTH);
    }

    /** Whether the island is placed in the sky. */
    public boolean isIslandUp() {
        return islandUp;
    }

    /** The height of the lowest block of the island in the sky (meaningful while it is up). */
    public double islandFloor() {
        return islandFloor;
    }

    /** Whether the entity is up on the island, as opposed to on the ground under it. */
    public boolean isOnTheIsland(Entity entity) {
        return islandUp && entity.getY() >= islandFloor - 4.0;
    }

    /**
     * Whether the column of {@code (x, z)} is one of the island's, while it is up: inside its wavy rim (with no copy to
     * tell, no farther from the middle than the rim can run).
     */
    public boolean isIslandColumn(double x, double z) {
        if (!islandUp) {
            return false;
        }
        SokoviaTemplate t = copiedFrom;
        if (t == null) {
            return Math.hypot(x - center().x, z - center().z) <= islandRadius + SokoviaShape.RIM_SLACK + 1.0;
        }
        return SokoviaShape.inside(t.spec(), Mth.floor(x) - t.centre().getX(), Mth.floor(z) - t.centre().getZ());
    }

    /**
     * Whether {@code pos} is a cell of the island in the sky (D-T 5.1): the island is up, the cell is inside the cylinder,
     * and it is no lower than one block under the island's lowest block. Its blocks are copies of the ground.
     */
    public boolean isCopyCell(BlockPos pos) {
        return islandUp && contains(pos) && pos.getY() >= islandFloor - 1.0;
    }

    /**
     * The arena's protection, and the island in the sky whatever the config says ({@link #isCopyCell}): every ability
     * that would change a block asks this, so the island's copies are never taken, mined or blown up as real blocks.
     */
    @Override
    public boolean protects(BlockPos pos) {
        return super.protects(pos) || isCopyCell(pos);
    }

    /**
     * Whether the clients hide the cell while the island is up (D-T 5.6): a column of the island's disc, from {@code
     * skinDepth} layers into the ground it was copied from up to the island's own cells (the ground, its cover, and the air
     * of the pit over it). Nothing is taken from there and nothing lands there, or it would show under the island, or be
     * missing when it comes down.
     */
    public boolean hidesGround(BlockPos pos) {
        SokoviaTemplate t = copiedFrom;
        if (!islandUp || t == null || pos.getY() >= islandFloor - 1.0) {
            return false;
        }
        int ground = t.groundAt(pos.getX() - t.centre().getX(), pos.getZ() - t.centre().getZ());
        return ground != SokoviaTemplate.NO_GROUND && pos.getY() > ground - t.spec().skin();
    }

    /**
     * The surface of the column of {@code (x, z)}, as the ritual's ground sees it, plus the height of the island for
     * one who is on it.
     */
    @Nullable
    @Override
    protected Vec3 surfaceSpot(ServerPlayer player, double x, double z) {
        return surfaceSpot(player, x, z, isOnTheIsland(player) ? islandOffset : 0.0);
    }

    @Nullable
    private Vec3 surfaceSpot(ServerPlayer player, double x, double z, double up) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!level().hasChunk(bx >> 4, bz >> 4)) {
            return null;
        }
        double y = UltronMotion.surface(level(), x, center().y + 4.0, z) + up;
        Vec3 spot = new Vec3(x, y, z);
        return Teleports.fits(player, spot) ? spot : new Vec3(x, y + 1.0, z);
    }

    /** Back inside the field, three blocks in from its edge; on the island, three blocks inside the rim. */
    @Override
    protected Vec3 rescueSpot(ServerPlayer player) {
        return isOnTheIsland(player) ? islandSpot(player) : super.rescueSpot(player);
    }

    /**
     * Where a fighter who left the island (it is up) is put back on it: three blocks inside the rim, on its surface, on
     * the line from the middle to where they are.
     */
    public Vec3 islandSpot(ServerPlayer player) {
        double dx = player.getX() - center().x;
        double dz = player.getZ() - center().z;
        double length = Math.hypot(dx, dz);
        double reach = Math.max(1.0, islandRadius - 3.0);
        double x = length <= reach || length < 1.0E-6 ? player.getX() : center().x + dx / length * reach;
        double z = length <= reach || length < 1.0E-6 ? player.getZ() : center().z + dz / length * reach;
        Vec3 spot = surfaceSpot(player, x, z, islandOffset);
        return spot != null ? spot : center().add(0, islandOffset, 0);
    }
}
