package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.SurfaceRun;
import com.kolodkingal.enhanced.client.SpeedsterMotion;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

/**
 * How a speedster's body is turned onto the wall or the ceiling he runs on (3.3): feet on the surface, still in his run
 * cycle. The body is pitched about a pivot so that the feet land on the surface: back 90 degrees running up a wall
 * (facing it), 180 on a ceiling (upside down, facing back the way he came, which is the same as rolled over facing the
 * way he runs), and forward 90 running down a wall (back to it). It is one angle, eased, so the turn from the ground up a
 * wall, onto the ceiling, down the far wall and back to the ground is one smooth backward roll with no snapping, and a
 * drop off a ceiling flips him back over. The body's yaw is eased to face the surface whatever the camera looks at (the
 * head still looks where the camera does).
 *
 * <p>The local hero's own run ({@link SpeedsterMotion}) says what he runs on at once; everyone else's comes from the
 * synced flags ({@code Live.SURFACE_WALL}, {@code Live.SURFACE_CEILING}), the side of him a block is on, and his motion.
 */
final class SpeedSurfacePoses {
    /** The time constant (ticks) of the turn onto and off the surface, and of the yaw. */
    private static final float TAU = 1.0F;
    /** The pivot of the turn up a wall (the feet then touch the wall: half the box's width) and on a ceiling (the middle). */
    private static final float WALL_PIVOT = 0.3F;
    private static final float CEILING_PIVOT = 0.9F;
    /** How high the body is lifted on a wall, so that it sticks out of the wall at the middle of his box, not at his feet. */
    private static final float WALL_LIFT = 0.55F;

    // =================================================================================================================
    // Once per tick
    // =================================================================================================================

    /** What the tick sees: the surface, its normal or the run's heading, and the speed along it. */
    static void sample(HeroAnimState s, AbstractClientPlayer p, PowerData data) {
        Vec3 d = new Vec3(p.getX() - p.xOld, p.getY() - p.yOld, p.getZ() - p.zOld);
        if (d.lengthSqr() > 36.0) {
            d = Vec3.ZERO;
        }
        SurfaceRun.Surface surface;
        int vertical;
        Vec3 heading;
        if (p == Minecraft.getInstance().player) {
            SurfaceRun.Runner runner = SpeedsterMotion.runner();
            surface = runner.surface;
            vertical = runner.vertical;
            heading = runner.heading;
        } else {
            boolean wall = data.is(Live.SURFACE_WALL);
            boolean ceiling = data.is(Live.SURFACE_CEILING);
            // He faces the wall he runs up or down (his own client turns his camera to it).
            float yaw = p.getYRot() * Mth.DEG_TO_RAD;
            Vec3 facing = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
            surface = wall || ceiling ? SurfaceRun.touched(p.level(), p, ceiling, facing) : SurfaceRun.Surface.NONE;
            if (!surface.touches() && (wall || ceiling)) {
                // The flag comes a moment before his position (which is eased in on this client) reaches the surface: keep
                // what he ran on, or take the wall he faces, rather than drop the pose for a tick or two.
                if (ceiling) {
                    surface = SurfaceRun.Surface.CEILING;
                } else if (s.surfaceKind == 1 || s.surfaceKind == 3) {
                    surface = SurfaceRun.Surface.wall(net.minecraft.core.Direction.getNearest(s.surfaceNormal.x, 0.0, s.surfaceNormal.z));
                } else {
                    surface = SurfaceRun.Surface.wall(net.minecraft.core.Direction.getNearest(-facing.x, 0.0, -facing.z));
                }
            }
            vertical = d.y > 0.02 ? 1 : d.y < -0.02 ? -1 : s.surfaceKind == 3 ? -1 : 1;
            double flat = Math.hypot(d.x, d.z);
            heading = flat > 0.05 ? new Vec3(d.x / flat, 0.0, d.z / flat) : s.surfaceHeading;
        }
        switch (surface.kind()) {
            case WALL -> {
                s.surfaceKind = vertical >= 0 ? 1 : 3;
                s.surfaceNormal = surface.normalVector();
            }
            case CEILING -> {
                s.surfaceKind = 2;
                if (heading.lengthSqr() > 0.5) {
                    s.surfaceHeading = heading;
                }
            }
            case NONE -> s.surfaceKind = 0;
        }
        s.surfaceSpeed = s.surfaceKind != 0 ? (float) d.length() : 0.0F;
    }

    // =================================================================================================================
    // Once per frame
    // =================================================================================================================

    /**
     * Eases the body onto (or off) the surface, and gives the run cycle the speed along it. Called before the run cycle
     * is advanced.
     */
    static void advance(HeroAnimState s, AbstractClientPlayer p, float dt, boolean snap) {
        float target;
        Vec3 face = null;
        switch (s.surfaceKind) {
            case 1 -> {
                target = -90.0F;
                face = s.surfaceNormal.scale(-1);
            }
            case 2 -> {
                target = -180.0F;
                face = s.surfaceHeading.scale(-1);
            }
            case 3 -> {
                target = 90.0F;
                face = s.surfaceNormal;
            }
            default -> target = 0.0F;
        }
        float k = snap ? 1.0F : 1.0F - (float) Math.exp(-dt / TAU);
        s.surfacePitch = Mth.wrapDegrees(s.surfacePitch + Mth.wrapDegrees(target - s.surfacePitch) * k);
        if (face != null && face.lengthSqr() > 0.5) {
            float yaw = (float) (Math.atan2(-face.x, face.z) * Mth.RAD_TO_DEG);
            s.surfaceYaw = s.surfaceFace < 0.01F || snap ? yaw : s.surfaceYaw + Mth.wrapDegrees(yaw - s.surfaceYaw) * k;
        }
        float faceTarget = face != null ? 1.0F : 0.0F;
        s.surfaceFace = snap ? faceTarget : s.surfaceFace + (faceTarget - s.surfaceFace) * k;
        if (s.surfaceFace > 0.001F) {
            float side = p.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            s.stanceYaw = Mth.wrapDegrees(s.surfaceYaw - s.bodyYaw) * s.surfaceFace * side;
        } else {
            s.stanceYaw = 0.0F;
        }
        // The pivot and the lift that keep the feet on the surface as the body turns.
        float a = Math.abs(s.surfacePitch);
        if (a <= 90.0F) {
            float u = a / 90.0F;
            s.surfacePivot = WALL_PIVOT;
            s.surfaceLift = WALL_LIFT * u;
        } else {
            float u = (a - 90.0F) / 90.0F;
            s.surfacePivot = AnimMath.lerp(u, WALL_PIVOT, CEILING_PIVOT);
            s.surfaceLift = WALL_LIFT * (1.0F - u);
        }
        if (s.surfaceKind != 0) {
            // His run cycle goes on at the speed along the surface.
            s.speed = Math.max(s.speed, s.surfaceSpeed);
        }
    }

    /** How far the body is turned onto a surface: 0 upright, 1 at 90 degrees or more. */
    static float weight(HeroAnimState s) {
        return Math.min(1.0F, Math.abs(s.surfacePitch) / 90.0F);
    }

    private SpeedSurfacePoses() {
    }
}
