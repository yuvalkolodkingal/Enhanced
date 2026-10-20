package com.kolodkingal.enhanced.serum;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /enhanced serum open <player>} (3.8, operators, for QA): opens the serum's screen without an item; the choice
 * is checked as any other (the wings for Falcon, not the same hero) and uses nothing up.
 */
public final class SerumCommand {

    /** The {@code serum} node. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("serum")
                .then(Commands.literal("open").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> open(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int open(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        SerumServer.openFree(player);
        source.sendSuccess(() -> Component.translatable("commands.enhanced.serum.opened", player.getDisplayName()),
                true);
        return 1;
    }

    private SerumCommand() {
    }
}
