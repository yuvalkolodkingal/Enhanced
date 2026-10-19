package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.world.SchoolLabPlan;
import com.kolodkingal.enhanced.world.SchoolRepair;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Optional;

/**
 * {@code /enhanced structure school_lab [radius]}, {@code school_lab force} and {@code school_lab status} (3.7, lane
 * L4, operators; next to {@code repair_school} in {@link SchoolRepair#node()}): build the nearest school's lab now (or
 * finish one being built), the same and put the entrance over a player's blocks, or tell the school's record (its
 * state, why it is blocked, where the lift's pads are, and whether its shell and doors are whole).
 */
public final class LabCommand {
    static final String KEY = "commands.enhanced.structure.school_lab.";

    /** The {@code school_lab} node. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("school_lab")
                .executes(ctx -> build(ctx.getSource(), SchoolRepair.DEFAULT_RADIUS, false))
                .then(Commands.argument("radius", IntegerArgumentType.integer(16, SchoolRepair.MAX_RADIUS))
                        .executes(ctx -> build(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"),
                                false)))
                .then(Commands.literal("force").executes(ctx -> build(ctx.getSource(), SchoolRepair.DEFAULT_RADIUS,
                        true)))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));
    }

    /** The nearest school's record (made if it is new), or empty with the answer sent. */
    private static Optional<SchoolLabs.Lab> nearest(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.translatable(KEY + "none", radius));
            return Optional.empty();
        }
        Optional<TemplateSitePiece> piece = SchoolRepair.nearest(level, BlockPos.containing(source.getPosition()),
                radius);
        if (piece.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "none", radius));
            return Optional.empty();
        }
        // a school's start is in the chunk of its piece's middle
        BlockPos middle = piece.get().getBoundingBox().getCenter();
        long key = ChunkPos.asLong(middle.getX() >> 4, middle.getZ() >> 4);
        SchoolLabs store = SchoolLabs.get(level);
        for (SchoolLabs.Lab lab : store.all()) {
            if (lab.site().equals(LabSite.of(piece.get()))) {
                return Optional.of(lab);
            }
        }
        return Optional.of(LabBuilder.record(store, key, piece.get()));
    }

    static int build(CommandSourceStack source, int radius, boolean force) {
        if (!LabBuilder.enabled()) {
            source.sendFailure(Component.translatable(KEY + "disabled"));
            return 0;
        }
        Optional<SchoolLabs.Lab> found = nearest(source, radius);
        if (found.isEmpty()) {
            return 0;
        }
        boolean wasDone = found.get().state() == SchoolLabs.State.DONE && found.get().reason().isEmpty();
        SchoolLabs.Lab lab = LabBuilder.buildNow(source.getLevel(), found.get(), force);
        BlockPos pad = lab.site().school(SchoolLabPlan.STUDY_PADS.get(0));
        if (lab.state() == SchoolLabs.State.BLOCKED) {
            source.sendFailure(Component.translatable(KEY + "blocked", lab.reason()));
            return 0;
        }
        if (!lab.reason().isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "entrance_blocked", pad.getX(), pad.getY(), pad.getZ()));
            return 0;
        }
        String answer = wasDone && !force ? "already" : "built";
        source.sendSuccess(() -> Component.translatable(KEY + answer, pad.getX(), pad.getY(), pad.getZ()), true);
        return 1;
    }

    static int status(CommandSourceStack source) {
        Optional<SchoolLabs.Lab> found = nearest(source, SchoolRepair.DEFAULT_RADIUS);
        if (found.isEmpty()) {
            return 0;
        }
        SchoolLabs.Lab lab = found.get();
        ServerLevel level = source.getLevel();
        BlockPos study = lab.site().school(SchoolLabPlan.STUDY_PADS.get(0));
        BlockPos below = lab.site().lab(SchoolLabPlan.LAB_PADS.get(0));
        Component state = Component.translatable(KEY + "state." + lab.state().name().toLowerCase(Locale.ROOT));
        source.sendSuccess(() -> Component.translatable(KEY + "status", state, lab.step(),
                lab.reason().isEmpty() ? "-" : lab.reason(), study.getX(), study.getY(), study.getZ(), below.getX(),
                below.getY(), below.getZ(), lab.turn().name().toLowerCase(Locale.ROOT)), false);
        if (lab.state() == SchoolLabs.State.DONE) {
            int holes = NoHoles.shellHoles(level, lab.site()).size();
            int fluids = NoHoles.strayFluids(level, lab.site()).size();
            int doors = NoHoles.unframedDoors(level, lab.site()).size();
            source.sendSuccess(() -> Component.translatable(KEY + "whole", holes, fluids, doors), false);
        }
        return 1;
    }

    private LabCommand() {
    }
}
