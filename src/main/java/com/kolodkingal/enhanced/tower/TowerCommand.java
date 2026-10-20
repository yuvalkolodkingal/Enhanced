package com.kolodkingal.enhanced.tower;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /enhanced structure refit_tower [radius] [force]} and {@code refit_tower status} (3.8, operators; next to
 * {@code repair_school} and {@code school_lab}): refit the nearest Avengers Tower now, over a player's blocks too, or
 * tell its record. Lane L6 writes what they do; the 3.8 skeleton's answer is "not built yet".
 */
public final class TowerCommand {
    /** The largest radius (blocks) the command looks in. */
    public static final int MAX_RADIUS = 512;

    /** The {@code refit_tower} node. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("refit_tower")
                .executes(ctx -> notYet(ctx.getSource()))
                .then(Commands.argument("radius", IntegerArgumentType.integer(16, MAX_RADIUS))
                        .executes(ctx -> notYet(ctx.getSource()))
                        .then(Commands.literal("force").executes(ctx -> notYet(ctx.getSource()))))
                .then(Commands.literal("force").executes(ctx -> notYet(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> notYet(ctx.getSource())));
    }

    private static int notYet(CommandSourceStack source) {
        source.sendFailure(Component.translatable("commands.enhanced.structure.refit_tower.not_yet"));
        return 0;
    }

    private TowerCommand() {
    }
}
