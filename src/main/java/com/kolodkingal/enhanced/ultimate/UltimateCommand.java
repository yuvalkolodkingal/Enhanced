package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.config.core.ConfigEdits;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * {@code /enhanced ultimate} (3.18, operators): {@code true} / {@code false} turns the Ultimate switch on and off for everyone
 * (it is the setting {@code ultimate.enabled}, saved to the config and sent to every player); {@code grant} / {@code revoke
 * <players>} gives or takes the ultimate versions (Cosmic Spider-Man) from those players alone, while the switch is off;
 * with nothing after it, it says whether the switch is on.
 */
public final class UltimateCommand {
    private UltimateCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("ultimate")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> status(ctx.getSource()))
                .then(Commands.argument("on", BoolArgumentType.bool())
                        .executes(ctx -> toggle(ctx, BoolArgumentType.getBool(ctx, "on"))))
                .then(Commands.literal("grant")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> grant(ctx, true))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> grant(ctx, false))));
    }

    private static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable(Ultimate.on() ? "commands.enhanced.ultimate.is_on" : "commands.enhanced.ultimate.is_off")
                .withStyle(Ultimate.on() ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return Ultimate.on() ? 1 : 0;
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx, boolean on) {
        try {
            ConfigEdits.change("ultimate.enabled", Boolean.toString(on));
        } catch (ConfigEdits.Refused refused) {
            ctx.getSource().sendFailure(refused.reason());
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(on ? "commands.enhanced.ultimate.on" : "commands.enhanced.ultimate.off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int grant(CommandContext<CommandSourceStack> ctx, boolean granted) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer player : players) {
            Ultimate.grant(player, granted);
            player.displayClientMessage(Component.translatable(granted ? "message.enhanced.ultimate.granted" : "message.enhanced.ultimate.revoked")
                    .withStyle(granted ? ChatFormatting.AQUA : ChatFormatting.GRAY), false);
        }
        int n = players.size();
        ctx.getSource().sendSuccess(() -> Component.translatable(granted ? "commands.enhanced.ultimate.granted" : "commands.enhanced.ultimate.revoked", n)
                .withStyle(ChatFormatting.GREEN), true);
        return n;
    }
}
