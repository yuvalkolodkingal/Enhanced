package com.kolodkingal.enhanced.eternity;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/**
 * {@code /enhanced eternity reset <player>} (his wish given back) and {@code /enhanced eternity send <player>} (to the
 * realm, or home from it) (3.8, operators). Lane L8 writes what they do; the 3.8 skeleton's answer is "not built yet".
 */
public final class EternityCommand {

    /** The {@code eternity} node. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("eternity")
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> notYet(ctx.getSource()))))
                .then(Commands.literal("send").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> notYet(ctx.getSource()))));
    }

    private static int notYet(CommandSourceStack source) {
        source.sendFailure(Component.translatable("commands.enhanced.eternity.not_yet"));
        return 0;
    }

    private EternityCommand() {
    }
}
