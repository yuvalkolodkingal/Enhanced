package com.kolodkingal.enhanced.road.copy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** What one player's room of the Copy the Structure trial remembers while it stands (nothing of it is saved). */
final class CopySession {
    /** A cell the last Check marked: 0 missing, 1 wrong, 2 extra. */
    record Mark(StructureCompare.Cell cell, int kind) {
    }

    final StructureModel model;
    final CopyGeometry geo;
    final BlockPos origin;
    final List<Item> items;

    long lastCheck = Long.MIN_VALUE / 2;
    long lastHint = Long.MIN_VALUE / 2;
    final List<Mark> marks = new ArrayList<>();
    long marksUntil;
    final List<Entity> ghosts = new ArrayList<>();
    long ghostsUntil;
    Display.TextDisplay plotText;

    CopySession(StructureModel model, BlockPos origin) {
        this.model = model;
        this.geo = CopyGeometry.of(model);
        this.origin = origin;
        this.items = new ArrayList<>(model.itemsNeeded().keySet());
    }

    BlockPos plotCorner() {
        return origin.offset(geo.plotX0(), 1, 0);
    }

    BlockPos modelCorner() {
        return origin.offset(geo.modelX0(), 1, 0);
    }

    BlockPos checkButton() {
        return origin.offset(-1, 1, geo.consoleZ() + 1);
    }

    BlockPos hintButton() {
        return origin.offset(1, 1, geo.consoleZ() + 1);
    }

    BlockPos chest() {
        return origin.offset(0, 1, geo.consoleZ());
    }

    /** Compares the plot with the model as it is now. */
    StructureCompare.Result compare(ServerLevel level) {
        BlockPos corner = plotCorner();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        return StructureCompare.compare(model, (x, y, z) -> level.getBlockState(pos.set(corner.getX() + x, corner.getY() + y, corner.getZ() + z)));
    }

    /** Remembers the cells to outline: the lowest ones first, at most {@code limit} of them, until the game time {@code until}. */
    void mark(StructureCompare.Result result, int limit, long until) {
        marks.clear();
        List<Mark> all = new ArrayList<>();
        result.missing().forEach(c -> all.add(new Mark(c, 0)));
        result.wrong().forEach(c -> all.add(new Mark(c, 1)));
        result.extra().forEach(c -> all.add(new Mark(c, 2)));
        all.sort(java.util.Comparator.comparingInt((Mark m) -> m.cell().y()).thenComparingInt(m -> m.cell().z()).thenComparingInt(m -> m.cell().x()));
        marks.addAll(all.subList(0, Math.min(limit, all.size())));
        marksUntil = until;
    }

    void clearGhosts() {
        for (Entity ghost : ghosts) {
            if (!ghost.isRemoved()) {
                ghost.discard();
            }
        }
        ghosts.clear();
    }
}
