package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.road.boat.IceTrack;

/**
 * A second, separate simulation of the vanilla 1.21.1 boat on a course, written out here from the game's code ({@code Boat.tick},
 * {@code floatBoat}, {@code controlBoat}, {@code getGroundFriction}, {@code Entity.collide}) and shared with nothing in the mod: a
 * boat of 1.375 x 0.5625 on a grid of columns ({@link IceTrack.Raster}), one {@link #step} a tick. The tests drive it with a bot over
 * thousands of courses, and check it against the real {@code Boat} entity driving the same inputs.
 */
final class IceBoatSim {
    static final double HALF = 1.375 / 2.0;
    static final double HEIGHT = 0.5625;
    private static final double EPS = 1.0E-7;

    // The game's sine table (Mth.sin / Mth.cos): the same 65536 entries.
    private static final float[] SIN = new float[65536];

    static {
        for (int i = 0; i < SIN.length; i++) {
            SIN[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
        }
    }

    static float sin(float v) {
        return SIN[(int) (v * 10430.378F) & 65535];
    }

    static float cos(float v) {
        return SIN[(int) (v * 10430.378F + 16384.0F) & 65535];
    }

    /** What a column of the course does to the boat. */
    interface World {
        /** The top of the column (negative infinity: nothing there). */
        double top(int x, int z);

        /** The friction of the block at the top of the column (the game's block friction). */
        float friction(int x, int z);
    }

    /** A course as a world: heights from the raster plus the offset of the level 0 surface. */
    static final class TrackWorld implements World {
        final IceTrack.Raster raster;
        /** Cells that are open (a barrier that is gone, or still there): a column with this set is solid up to its value. */
        TrackWorld(IceTrack.Raster raster) {
            this.raster = raster;
        }

        @Override
        public double top(int x, int z) {
            return raster.top(x, z);
        }

        @Override
        public float friction(int x, int z) {
            return switch (raster.kind(x, z)) {
                case IceTrack.BLUE -> 0.989F;
                case IceTrack.SNOW, IceTrack.OBSTACLE -> 0.6F;
                default -> 0.98F;
            };
        }
    }

    // ---- state ----
    double x;
    double y;
    double z;
    double vx;
    double vy;
    double vz;
    float yaw;
    float deltaRotation;
    boolean onGround;
    boolean horizontalCollision;
    /** The friction of the last step (0.9 in the air). */
    float lastFriction;
    final World world;

    IceBoatSim(World world, double x, double y, double z, float yaw) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
    }

    double speed() {
        return Math.hypot(vx, vz);
    }

    /** The friction of the blocks under the boat (the game's {@code getGroundFriction}), or 0 if there is nothing under it. */
    float groundFriction() {
        double minX = x - HALF;
        double maxX = x + HALF;
        double minZ = z - HALF;
        double maxZ = z + HALF;
        float sum = 0.0F;
        int count = 0;
        for (int cx = (int) Math.floor(minX); cx < Math.ceil(maxX); cx++) {
            for (int cz = (int) Math.floor(minZ); cz < Math.ceil(maxZ); cz++) {
                // The block has to overlap the box (not just touch it).
                if (!(cx + 1 > minX + 1.0E-9 && cx < maxX - 1.0E-9 && cz + 1 > minZ + 1.0E-9 && cz < maxZ - 1.0E-9)) {
                    continue;
                }
                double top = world.top(cx, cz);
                // The thin slab under the box is [y - 0.001, y]; the block under it reaches up into it.
                if (top > y - 0.001 && top - 1.0 < y) {
                    sum += world.friction(cx, cz);
                    count++;
                }
            }
        }
        return count == 0 ? 0.0F : sum / (float) count;
    }

    /** One tick with the keys held. */
    void step(boolean left, boolean right, boolean up, boolean down) {
        // Boat.tick: the status, from the blocks under it.
        float ground = groundFriction();
        float invFriction;
        if (ground > 0.0F) {
            invFriction = ground;
        } else {
            invFriction = 0.9F;
        }
        lastFriction = invFriction;
        // floatBoat
        vx = vx * (double) invFriction;
        vy = vy + -0.04;
        vz = vz * (double) invFriction;
        deltaRotation = deltaRotation * invFriction;
        // controlBoat
        float f = 0.0F;
        if (left) {
            deltaRotation--;
        }
        if (right) {
            deltaRotation++;
        }
        if (right != left && !up && !down) {
            f += 0.005F;
        }
        yaw = yaw + deltaRotation;
        if (up) {
            f += 0.04F;
        }
        if (down) {
            f -= 0.005F;
        }
        vx = vx + (double) (sin(-yaw * ((float) Math.PI / 180F)) * f);
        vz = vz + (double) (cos(yaw * ((float) Math.PI / 180F)) * f);
        move(vx, vy, vz);
    }

    // ---- Entity.move for the boat: no step-up, axis by axis ----

    private void move(double mx, double my, double mz) {
        double x0 = x;
        double y0 = y;
        double z0 = z;
        double d0 = mx;
        double d1 = my;
        double d2 = mz;
        // Entity.collideWithShapes: y first, then the longer of x and z first (the box moves as each axis is settled).
        if (d1 != 0.0) {
            d1 = collideY(d1);
            if (d1 != 0.0) {
                y += d1;
            }
        }
        boolean zFirst = Math.abs(d0) < Math.abs(d2);
        if (zFirst && d2 != 0.0) {
            d2 = collideZ(d2);
            if (d2 != 0.0) {
                z += d2;
            }
        }
        if (d0 != 0.0) {
            d0 = collideX(d0);
            if (!zFirst && d0 != 0.0) {
                x += d0;
            }
        }
        if (!zFirst && d2 != 0.0) {
            d2 = collideZ(d2);
        }
        x = x0 + d0;
        y = y0 + d1;
        z = z0 + d2;
        horizontalCollision = d0 != mx || d2 != mz;
        onGround = d1 != my && my < 0.0;
        if (d0 != mx) {
            vx = 0.0;
        }
        if (d2 != mz) {
            vz = 0.0;
        }
        if (d1 != my) {
            vy = 0.0;
        }
    }

    private double collideY(double movement) {
        if (Math.abs(movement) < EPS) {
            return 0.0;
        }
        double minX = x - HALF;
        double maxX = x + HALF;
        double minZ = z - HALF;
        double maxZ = z + HALF;
        for (int cx = (int) Math.floor(minX + EPS); cx <= (int) Math.floor(maxX - EPS); cx++) {
            for (int cz = (int) Math.floor(minZ + EPS); cz <= (int) Math.floor(maxZ - EPS); cz++) {
                double top = world.top(cx, cz);
                if (top == Double.NEGATIVE_INFINITY) {
                    continue;
                }
                if (movement < 0.0) {
                    double distance = top - y;
                    if (distance <= EPS) {
                        movement = Math.max(movement, distance);
                    }
                }
            }
        }
        return movement;
    }

    private double collideX(double movement) {
        if (Math.abs(movement) < EPS) {
            return 0.0;
        }
        double minX = x - HALF;
        double maxX = x + HALF;
        double minZ = z - HALF;
        double maxZ = z + HALF;
        int from = (int) Math.floor(Math.min(minX, minX + movement)) - 1;
        int to = (int) Math.floor(Math.max(maxX, maxX + movement)) + 1;
        for (int cx = from; cx <= to; cx++) {
            for (int cz = (int) Math.floor(minZ + EPS); cz <= (int) Math.floor(maxZ - EPS); cz++) {
                double top = world.top(cx, cz);
                if (top == Double.NEGATIVE_INFINITY || !(y + EPS < top)) {
                    continue;
                }
                if (movement > 0.0) {
                    double distance = cx - maxX;
                    if (distance >= -EPS) {
                        movement = Math.min(movement, distance);
                    }
                } else {
                    double distance = cx + 1 - minX;
                    if (distance <= EPS) {
                        movement = Math.max(movement, distance);
                    }
                }
            }
        }
        return movement;
    }

    private double collideZ(double movement) {
        if (Math.abs(movement) < EPS) {
            return 0.0;
        }
        double minX = x - HALF;
        double maxX = x + HALF;
        double minZ = z - HALF;
        double maxZ = z + HALF;
        int from = (int) Math.floor(Math.min(minZ, minZ + movement)) - 1;
        int to = (int) Math.floor(Math.max(maxZ, maxZ + movement)) + 1;
        for (int cz = from; cz <= to; cz++) {
            for (int cx = (int) Math.floor(minX + EPS); cx <= (int) Math.floor(maxX - EPS); cx++) {
                double top = world.top(cx, cz);
                if (top == Double.NEGATIVE_INFINITY || !(y + EPS < top)) {
                    continue;
                }
                if (movement > 0.0) {
                    double distance = cz - maxZ;
                    if (distance >= -EPS) {
                        movement = Math.min(movement, distance);
                    }
                } else {
                    double distance = cz + 1 - minZ;
                    if (distance <= EPS) {
                        movement = Math.max(movement, distance);
                    }
                }
            }
        }
        return movement;
    }
}
