package com.kolodkingal.enhanced.road.parkour;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.road.RoadBuild;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Trial 1, the Parkour: jumps over the void on stepping stones of dark oak, from a start platform through lit checkpoints to a
 * finish platform with the gate. Falling puts the player back at his last checkpoint ({@link com.kolodkingal.enhanced.road.RoadTrial}).
 * The course is made by {@link ParkourGenerator} from the player's seed.
 */
public final class ParkourChallenge implements RoadChallenge {

    @Override
    public String id() {
        return "parkour";
    }

    /** The settings from the server config. */
    public static ParkourGenerator.Params params() {
        return new ParkourGenerator.Params(WitchesRoadConfig.PARKOUR_PADS.get(), WitchesRoadConfig.PARKOUR_DIFFICULTY.get(),
                WitchesRoadConfig.PARKOUR_CHECKPOINT_EVERY.get());
    }

    @Override
    public RoadLayout build(RoadBuild build) {
        ParkourGenerator.Plan plan = ParkourGenerator.generate(build.seed(), params());
        return draw(build.canvas(), plan, WitchesRoadConfig.FALL_DEPTH.get(), WitchesRoadConfig.PARKOUR_TIME_LIMIT_SECONDS.get() * 20);
    }

    /** Draws a plan on the canvas and says where everything is. */
    public static RoadLayout draw(RoadCanvas canvas, ParkourGenerator.Plan plan, int fallDepth, int timeLimitTicks) {
        List<ParkourGenerator.Pad> pads = plan.pads();
        for (ParkourGenerator.Pad pad : pads) {
            switch (pad.kind()) {
                case START -> RoadPalette.startPlatform(canvas, pad.midX(), pad.y(), pad.midZ());
                case CHECKPOINT -> RoadPalette.checkpointPad(canvas, pad.midX(), pad.y(), pad.midZ());
                case FINISH -> RoadPalette.finishPlatform(canvas, pad.midX(), pad.y(), pad.midZ());
                default -> RoadPalette.pad(canvas, pad.minX(), pad.minZ(), pad.maxX(), pad.maxZ(), pad.y());
            }
        }
        ParkourGenerator.Pad finish = plan.finish();
        ParkourGenerator.Pad before = pads.get(pads.size() - 2);
        // The gate stands on the far side of the finish platform, across the way the player came.
        int dirX = (int) Math.signum(finish.centerX() - before.centerX());
        int dirZ = (int) Math.signum(finish.centerZ() - before.centerZ());
        boolean alongX = Math.abs(finish.centerX() - before.centerX()) >= Math.abs(finish.centerZ() - before.centerZ());
        int gx = finish.midX() + (alongX ? dirX * 2 : 0);
        int gz = finish.midZ() + (alongX ? 0 : dirZ * 2);
        Direction.Axis axis = alongX ? Direction.Axis.Z : Direction.Axis.X;
        RoadPalette.gate(canvas, gx, finish.y() + 1, gz, axis);

        // Where the player stands (the start, then each checkpoint), looking at the next pad.
        List<RoadLayout.Checkpoint> checkpoints = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        indices.add(0);
        indices.addAll(plan.checkpoints());
        for (int n = 0; n < indices.size(); n++) {
            ParkourGenerator.Pad pad = pads.get(indices.get(n));
            ParkourGenerator.Pad next = pads.get(Math.min(indices.get(n) + 1, pads.size() - 1));
            int upto = n + 1 < indices.size() ? indices.get(n + 1) : pads.size() - 1;
            int lowest = Integer.MAX_VALUE;
            for (int i = indices.get(n); i <= upto; i++) {
                lowest = Math.min(lowest, pads.get(i).y());
            }
            BlockPos origin = canvas.origin();
            Vec3 at = new Vec3(origin.getX() + pad.centerX(), origin.getY() + pad.y() + 1.0, origin.getZ() + pad.centerZ());
            float yaw = (float) (Mth.atan2(-(next.centerX() - pad.centerX()), next.centerZ() - pad.centerZ()) * Mth.RAD_TO_DEG);
            AABB zone = n == 0 ? null : new AABB(origin.getX() + pad.midX() - 1, origin.getY() + pad.y() + 0.9, origin.getZ() + pad.midZ() - 1,
                    origin.getX() + pad.midX() + 2, origin.getY() + pad.y() + 3.0, origin.getZ() + pad.midZ() + 2);
            checkpoints.add(new RoadLayout.Checkpoint(at, yaw, zone, origin.getY() + lowest - fallDepth + 1.0));
        }
        BlockPos origin = canvas.origin();
        AABB finishZone = new AABB(origin.getX() + finish.minX() + 1, origin.getY() + finish.y() + 0.9, origin.getZ() + finish.minZ() + 1,
                origin.getX() + finish.maxX(), origin.getY() + finish.y() + 3.0, origin.getZ() + finish.maxZ());
        ParkourGenerator.Pad start = plan.start();
        BlockPos exit = RoadPalette.startExitDoor(canvas, start.midX(), start.y(), start.midZ());
        RoadLayout.Gate gate = new RoadLayout.Gate(canvas.at(gx, finish.y() + 1, gz), axis);
        return new RoadLayout(checkpoints, finishZone, gate, exit, timeLimitTicks);
    }
}
