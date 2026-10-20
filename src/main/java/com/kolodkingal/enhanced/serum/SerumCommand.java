package com.kolodkingal.enhanced.serum;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/**
 * {@code /enhanced serum open <player>} (3.8, operators, for QA): opens the serum's screen without an item. Lane L1
 * writes what it does; the 3.8 skeleton's answer is "not built yet".
 */
public final class SerumCommand {

    /** The {@code serum} node. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("serum")
                .then(Commands.literal("open").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> notYet(ctx.getSource()))));
    }

    private static int notYet(CommandSourceStack source) {
        source.sendFailure(Component.translatable("commands.enhanced.serum.not_yet"));
        return 0;
    }

    private SerumCommand() {
    }
}
