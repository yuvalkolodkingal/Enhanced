package com.kolodkingal.enhanced.multiverse;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /enhanced multiverse} (3.19): with nothing after it, says which universe you are in; {@code go <universe>}
 * (operators) takes you to a universe, named by its number or by any words ({@link Multiverse#named}); {@code
 * reset_stones} (operators) forgets every Infinity Stone found in the other universes.
 */
public final class MultiverseCommand {
    private MultiverseCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("multiverse")
                .executes(ctx -> where(ctx.getSource()))
                .then(Commands.literal("go")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("universe", StringArgumentType.greedyString())
                                .executes(MultiverseCommand::go)))
                .then(Commands.literal("reset_stones")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            UniverseLedger.get(ctx.getSource().getServer()).reset();
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.multiverse.reset_stones")
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }));
    }

    private static int where(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int universe = Multiverse.universeAt(player.level(), player.blockPosition());
        source.sendSuccess(() -> Component.translatable("commands.enhanced.multiverse.where", Multiverse.name(universe))
                .withStyle(ChatFormatting.AQUA), false);
        return universe;
    }

    private static int go(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int universe = Multiverse.named(StringArgumentType.getString(ctx, "universe"));
        if (Multiverse.levelOf(player.server, universe) == null) {
            ctx.getSource().sendFailure(Component.translatable("message.enhanced.multiverse.no_world"));
            return 0;
        }
        return UniverseTravel.send(player, universe, null, UniverseTravel.BRIDGE_BLUE) ? 1 : 0;
    }
}
