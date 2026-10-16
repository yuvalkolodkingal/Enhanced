package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.boat.IceBoatChallenge;
import com.kolodkingal.enhanced.road.boat.IceTrack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Consumer;

/** Helpers for the tests of the Ice-Boat Race that need blocks in the Road dimension without walking the Road: a cell of its own, drawn and cleared again. */
final class RoadBoatSupport {

    /** A cell of the Road with something drawn in it; the test's fixture clears it when it ends. */
    static final class Cell {
        final MinecraftServer server;
        final ServerLevel road;
        final int cell;
        final RoadCanvas canvas;
        final BlockPos origin;
        IceTrack track;
        RoadLayout layout;

        Cell(MinecraftServer server, ServerLevel road, int cell, RoadCanvas canvas) {
            this.server = server;
            this.road = road;
            this.cell = cell;
            this.canvas = canvas;
            this.origin = canvas.origin();
        }

        /** Where an offset from the middle of the cell is, in the world. */
        Vec3 at(double dx, double dy, double dz) {
            return new Vec3(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
        }

        BlockState block(int dx, int dy, int dz) {
            return road.getBlockState(origin.offset(dx, dy, dz));
        }
    }

    /** Draws something in a new cell (the drawer gets the canvas) and places all of it at once. */
    static Cell draw(TestKit.Fixture f, Consumer<Cell> drawer) {
        MinecraftServer server = f.level().getServer();
        ServerLevel road = RoadDimension.level(server);
        RoadSave save = RoadSave.get(server);
        UUID owner = UUID.randomUUID();
        int index = save.allocate(owner, "test", 64);
        if (index < 0) {
            throw new IllegalStateException("no cell");
        }
        f.onClose(() -> {
            RoadSweeper.queue(server, index, true);
            RoadSweeper.drain(server);
        });
        Cell cell = new Cell(server, road, index, new RoadCanvas(index, road.getMinBuildHeight(), road.getMaxBuildHeight()));
        drawer.accept(cell);
        cell.canvas.seal();
        save.plan(index, cell.canvas.chunks(), cell.canvas.minY(), cell.canvas.maxY());
        RoadSweeper.hold(road, owner, cell.canvas.chunks());
        while (!cell.canvas.placeSome(road, 1_000_000)) {
            // (a big budget places everything in one call or two)
        }
        return cell;
    }

    /** A whole course, drawn, with the barrier at the start open. */
    static Cell course(TestKit.Fixture f, IceTrack track) {
        Cell cell = draw(f, c -> {
            c.track = track;
            c.layout = IceBoatChallenge.draw(c.canvas, track, 12, 0);
        });
        for (BlockPos pos : IceBoatChallenge.barrierCells(track, cell.origin)) {
            cell.canvas.live(cell.road, pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        return cell;
    }

    private RoadBoatSupport() {
    }
}
