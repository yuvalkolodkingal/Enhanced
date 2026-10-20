package com.kolodkingal.enhanced.tower;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * {@code /enhanced structure refit_tower [radius] [force]} and {@code refit_tower status} (3.8, operators; next to
 * {@code repair_school} and {@code school_lab}): refits the nearest Avengers Tower now ({@link TowerRefit#refit}),
 * {@code force} over a player's blocks at the armoury's points too (they drop as if broken; a player's armour stand or
 * item frame is never touched), or tells its record. The takers of its displays and cases are never reset.
 */
public final class TowerCommand {
    /** The largest radius (blocks) the command looks in. */
    public static final int MAX_RADIUS = 512;
    /** The radius when none is given. */
    public static final int DEFAULT_RADIUS = 96;
    private static final String KEY = "commands.enhanced.structure.refit_tower.";

    /** The {@code refit_tower} node. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("refit_tower")
                .executes(ctx -> run(ctx.getSource(), DEFAULT_RADIUS, false))
                .then(Commands.argument("radius", IntegerArgumentType.integer(16, MAX_RADIUS))
                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"), false))
                        .then(Commands.literal("force").executes(ctx -> run(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "radius"), true))))
                .then(Commands.literal("force").executes(ctx -> run(ctx.getSource(), DEFAULT_RADIUS, true)))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));
    }

    private static Optional<TowerRefit.Tower> nearest(CommandSourceStack source, int radius) {
        Optional<TowerRefit.Tower> found = TowerRefit.nearest(source.getLevel(), BlockPos.containing(
                source.getPosition()), radius);
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "none", radius));
        }
        return found;
    }

    /** Refits the nearest tower within {@code radius}; returns the blocks set down. */
    static int run(CommandSourceStack source, int radius, boolean force) {
        Optional<TowerRefit.Tower> found = nearest(source, radius);
        if (found.isEmpty()) {
            return 0;
        }
        ServerLevel level = source.getLevel();
        TowerRefit.Tower tower = found.get();
        BlockPos at = tower.at();
        if (!TowerRefit.loaded(level, tower.pieces())) {
            source.sendFailure(Component.translatable(KEY + "not_loaded", at.getX(), at.getY(), at.getZ()));
            return 0;
        }
        TowerRefit.stop(tower.start());
        TowerRefit.Result r = TowerRefit.refit(level, tower.pieces(), force);
        TowerRefits.get(level).record(tower.start(), new TowerRefits.Entry(r.oldDesign() ? TowerRefit.OLD_DESIGN
                : TowerRefit.VERSION, r, level.getGameTime()));
        if (r.oldDesign()) {
            source.sendFailure(Component.translatable(KEY + "old_design", at.getX(), at.getY(), at.getZ()));
            return 0;
        }
        String key = r.changed() > 0 || r.removed() > 0 ? "done" : r.skipped() > 0 || r.kept() > 0 ? "nothing"
                : "already";
        Component message = Component.translatable(KEY + key, at.getX(), at.getY(), at.getZ(), r.changed(),
                r.removed(), r.skipped(), r.kept());
        source.sendSuccess(() -> message, true);
        return r.changed();
    }

    /** Tells the nearest tower's record. */
    static int status(CommandSourceStack source) {
        Optional<TowerRefit.Tower> found = nearest(source, DEFAULT_RADIUS);
        if (found.isEmpty()) {
            return 0;
        }
        TowerRefit.Tower tower = found.get();
        BlockPos at = tower.at();
        TowerRefits.Entry entry = TowerRefits.get(source.getLevel()).get(tower.start());
        Component message;
        if (entry == null) {
            message = Component.translatable(KEY + "status.none", at.getX(), at.getY(), at.getZ());
        } else if (entry.version() == TowerRefit.OLD_DESIGN) {
            message = Component.translatable(KEY + "old_design", at.getX(), at.getY(), at.getZ());
        } else {
            TowerRefit.Result r = entry.result();
            message = Component.translatable(KEY + "status", at.getX(), at.getY(), at.getZ(), entry.version(),
                    r.changed(), r.removed(), r.skipped(), r.kept());
        }
        source.sendSuccess(() -> message, false);
        return entry == null ? 0 : 1;
    }

    private TowerCommand() {
    }
}
