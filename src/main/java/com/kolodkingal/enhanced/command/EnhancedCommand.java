package com.kolodkingal.enhanced.command;

import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;

/**
 * /enhanced power get [player]
 * /enhanced power set &lt;players&gt; &lt;none|magneto|scarlet_witch|photon|quicksilver|vision&gt;
 * /enhanced hex list
 * /enhanced hex clear
 */
public final class EnhancedCommand {
    private static final DynamicCommandExceptionType UNKNOWN_POWER =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.enhanced.power.unknown", name));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enhanced")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("power")
                        .then(Commands.literal("get")
                                .executes(ctx -> get(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("power", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(Power.values()).map(Power::getSerializedName), builder))
                                                .executes(EnhancedCommand::set)))))
                .then(Commands.literal("hex")
                        .then(Commands.literal("list").executes(ctx -> listHexes(ctx.getSource())))
                        .then(Commands.literal("clear").executes(ctx -> clearHexes(ctx.getSource())))));
    }

    private static int get(CommandSourceStack source, ServerPlayer player) {
        Power power = Powers.power(player);
        source.sendSuccess(() -> Component.translatable("commands.enhanced.power.get", player.getDisplayName(), power.displayName()), false);
        return power.ordinal();
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "power");
        Power power = Power.byName(name);
        if (power == null) {
            throw UNKNOWN_POWER.create(name);
        }
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer player : players) {
            if (power == Power.NONE) {
                Powers.setPower(player, Power.NONE);
            } else {
                Powers.awaken(player, power);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.power.set", players.size(), power.displayName()), true);
        return players.size();
    }

    private static int listHexes(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var hexes = HexManager.data(level).hexes();
        if (hexes.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.hex.none"), false);
            return 0;
        }
        for (Hex hex : hexes) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.hex.entry", hex.ownerName(),
                    (int) hex.center().x, (int) hex.center().y, (int) hex.center().z, hex.radius(), hex.allowed().size()), false);
        }
        return hexes.size();
    }

    private static int clearHexes(CommandSourceStack source) {
        int n = HexManager.clear(source.getLevel());
        source.sendSuccess(() -> Component.translatable("commands.enhanced.hex.cleared", n), true);
        return n;
    }

    private EnhancedCommand() {
    }
}
