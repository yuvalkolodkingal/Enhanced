package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.world.SchoolRepair;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /enhanced structure school_lab [radius]}, {@code school_lab force} and {@code school_lab status} (3.7,
 * operators; next to {@code repair_school} in {@link SchoolRepair#node()}): build or finish the nearest school's lab
 * now, put the entrance over a player's blocks, or tell the record. Lane L4 writes what they do; the 3.7 skeleton's
 * answer is "not built yet".
 */
public final class LabCommand {

    /** The {@code school_lab} node. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("school_lab")
                .executes(ctx -> notYet(ctx.getSource()))
                .then(Commands.argument("radius", IntegerArgumentType.integer(16, SchoolRepair.MAX_RADIUS))
                        .executes(ctx -> notYet(ctx.getSource())))
                .then(Commands.literal("force").executes(ctx -> notYet(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> notYet(ctx.getSource())));
    }

    private static int notYet(CommandSourceStack source) {
        source.sendFailure(Component.translatable("commands.enhanced.structure.school_lab.not_yet"));
        return 0;
    }

    private LabCommand() {
    }
}
