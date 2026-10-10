package com.kolodkingal.enhanced.command;

import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
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
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * /enhanced power get [player]
 * /enhanced power set &lt;players&gt; &lt;power&gt; [look]
 * /enhanced look &lt;players&gt; &lt;look&gt;
 * /enhanced hex list
 * /enhanced hex clear
 */
public final class EnhancedCommand {
    private static final DynamicCommandExceptionType UNKNOWN_POWER =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.enhanced.power.unknown", name));
    private static final DynamicCommandExceptionType UNKNOWN_LOOK =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.enhanced.look.unknown", name));

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
                                                .executes(ctx -> set(ctx, ""))
                                                .then(Commands.argument("look", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(lookIds(
                                                                Power.byName(StringArgumentType.getString(ctx, "power"))), builder))
                                                        .executes(ctx -> set(ctx, StringArgumentType.getString(ctx, "look"))))))))
                .then(Commands.literal("look")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("look", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Power.values()).flatMap(p -> lookIds(p).stream()).distinct(), builder))
                                        .executes(EnhancedCommand::look))))
                .then(Commands.literal("hex")
                        .then(Commands.literal("list").executes(ctx -> listHexes(ctx.getSource())))
                        .then(Commands.literal("clear").executes(ctx -> clearHexes(ctx.getSource())))));
    }

    private static int get(CommandSourceStack source, ServerPlayer player) {
        Power power = Powers.power(player);
        source.sendSuccess(() -> Component.translatable("commands.enhanced.power.get", player.getDisplayName(), power.displayName()), false);
        return power.ordinal();
    }

    private static List<String> lookIds(@Nullable Power power) {
        return power == null ? List.of() : Variants.of(power).stream().map(Variant::id).toList();
    }

    private static int set(CommandContext<CommandSourceStack> ctx, String look) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "power");
        Power power = Power.byName(name);
        if (power == null) {
            throw UNKNOWN_POWER.create(name);
        }
        if (!look.isEmpty() && !lookIds(power).contains(look)) {
            throw UNKNOWN_LOOK.create(look);
        }
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer player : players) {
            if (power == Power.NONE) {
                Powers.setPower(player, Power.NONE);
            } else if (Powers.power(player) == power && !look.isEmpty()) {
                Powers.setVariant(player, Variants.get(power, look));
            } else {
                Powers.awaken(player, power, look);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.power.set", players.size(), power.displayName()), true);
        return players.size();
    }

    /** Changes the look of every given player whose hero has that look. */
    private static int look(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String look = StringArgumentType.getString(ctx, "look");
        int changed = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            Power power = Powers.power(player);
            if (lookIds(power).contains(look)) {
                Powers.setVariant(player, Variants.get(power, look));
                changed++;
            }
        }
        if (changed == 0) {
            throw UNKNOWN_LOOK.create(look);
        }
        int n = changed;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.look.set", n, look), true);
        return changed;
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
