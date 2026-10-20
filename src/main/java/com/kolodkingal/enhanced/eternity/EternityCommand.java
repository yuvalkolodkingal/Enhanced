package com.kolodkingal.enhanced.eternity;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /enhanced eternity reset <player>} (his wish given back) and {@code /enhanced eternity send <player>} (to the
 * realm, or home from it) (3.8, operators: the {@code /enhanced} node's level).
 */
public final class EternityCommand {

    /** The {@code eternity} node. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("eternity")
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("send").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> send(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
    }

    /** His wishes given back (his visits and his way home kept). */
    public static int reset(CommandSourceStack source, ServerPlayer player) {
        EternityRealm.setProgress(player, EternityRealm.progress(player).reset());
        source.sendSuccess(() -> Component.translatable("commands.enhanced.eternity.reset", player.getDisplayName()),
                true);
        return 1;
    }

    /** To the realm (as if by the altar where he stands), or home from it. */
    public static int send(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        if (EternityRealm.in(player)) {
            EternityRealm.returnHome(player);
            source.sendSuccess(() -> Component.translatable("commands.enhanced.eternity.home",
                    player.getDisplayName()), true);
            return 1;
        }
        if (!EternityRealm.enter(player, player.blockPosition())) {
            source.sendFailure(Component.translatable("commands.enhanced.eternity.refused", player.getDisplayName()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.enhanced.eternity.sent", player.getDisplayName()),
                true);
        return 1;
    }

    private EternityCommand() {
    }
}
