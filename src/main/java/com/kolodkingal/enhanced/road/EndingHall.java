package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The end of the Road: a round hall of dark oak and amethyst in a cell of its own, with a dais of amethyst in the middle and a
 * door lying in the floor behind the player. It is no trial (nothing is won here and it is not counted): the player who has won
 * every trial is brought here, the Road asks "Seekest thou the Road?" again and he chooses Billy or Tommy ({@link RoadEnding}).
 * The hall is built like a trial, so it is held, saved, resumed after a login and taken down like one.
 */
public final class EndingHall implements RoadChallenge {
    /** The id it is saved under in the cell ledger. */
    public static final String ID = "road_end";
    /** The radius of the floor, in blocks. */
    private static final int RADIUS = 9;
    /** The radius of the amethyst dais in the middle. */
    private static final double DAIS = 2.6;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isEnding() {
        return true;
    }

    @Override
    public RoadLayout build(RoadBuild build) {
        RoadCanvas canvas = build.canvas();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > RADIUS) {
                    continue;
                }
                boolean rim = d > RADIUS - 1.5;
                boolean ring = d >= 5.0 && d <= 6.0;
                canvas.set(x, 0, z, rim ? RoadPalette.TRIM : d <= DAIS || ring ? RoadPalette.AMETHYST : RoadPalette.PLANKS);
                canvas.set(x, -1, z, rim ? RoadPalette.LOG : RoadPalette.PLANKS);
                // Roots, lichen and lanterns hang from underneath, a different few for every block.
                if (!rim && (x + z) % 2 == 0) {
                    int h = Math.floorMod(x * 31 + z * 17, 5);
                    canvas.set(x, -2, z, h == 0 ? RoadPalette.HANGING_LANTERN : h < 3 ? RoadPalette.ROOTS : RoadPalette.LICHEN);
                }
            }
        }
        // A sea lantern in the middle of the dais, and six pillars of amethyst, each with a soul lantern on top, round the hall.
        canvas.set(0, 0, 0, Blocks.SEA_LANTERN.defaultBlockState());
        for (int k = 0; k < 6; k++) {
            double angle = Math.toRadians(30.0 + 60.0 * k);
            int px = (int) Math.round(Math.cos(angle) * 7.0);
            int pz = (int) Math.round(Math.sin(angle) * 7.0);
            for (int h = 1; h <= 3; h++) {
                canvas.set(px, h, pz, RoadPalette.AMETHYST);
            }
            canvas.set(px, 4, pz, RoadPalette.LANTERN);
        }
        // The door lies in the floor behind the player.
        BlockPos exit = canvas.at(0, 1, 6);
        canvas.set(0, 1, 6, ModBlocks.WITCHES_ROAD_DOOR.get().defaultBlockState());
        BlockPos origin = canvas.origin();
        Vec3 start = new Vec3(origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 4.5);
        RoadLayout.Checkpoint at = new RoadLayout.Checkpoint(start, 180.0F, null, origin.getY() - WitchesRoadConfig.FALL_DEPTH.get());
        return new RoadLayout(List.of(at), null, null, exit, 0);
    }

    /** The middle of the dais, where the light gathers. */
    static Vec3 dais(RoadTrial trial) {
        BlockPos origin = trial.canvas().origin();
        return new Vec3(origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5);
    }

    @Override
    public void onStart(RoadTrial trial, ServerPlayer player) {
        RoadEnding.arrive(trial, player);
    }

    @Override
    public void tick(RoadTrial trial, ServerPlayer player) {
        RoadEnding.tick(trial, player);
    }

    @Override
    public void onEnd(RoadTrial trial, End reason) {
        RoadEnding.forget(trial.owner());
    }
}
