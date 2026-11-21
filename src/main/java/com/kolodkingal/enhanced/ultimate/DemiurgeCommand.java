package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.ability.Demiurge;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import java.util.List;

/**
 * The commands of Billy the Demiurge (3.18.1).
 * <ul>
 * <li>{@code /demiurge give <player> <picks>} and {@code /demiurge take <player>} (anyone: only the Demiurge, with Ultimate,
 * is let through): what his gift screen sends ({@link DemiurgeGifts}; the picks are indexes into its catalogue).</li>
 * <li>{@code /enhanced demiurge <players> on|off}, {@code type <players> <0-4>}, {@code gift <players> <picks>} and
 * {@code take <players>} (operators): the same without the screen. Ultimate is still needed to be the Demiurge or to be
 * given his gift.</li>
 * </ul>
 */
public final class DemiurgeCommand {
    private DemiurgeCommand() {
    }

    /** {@code /demiurge}: for the Demiurge himself (his screen). */
    public static LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal("demiurge")
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("picks", StringArgumentType.greedyString())
                                        .executes(ctx -> give(ctx, false)))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> take(ctx, false))));
    }

    /** {@code /enhanced demiurge}: operators. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("demiurge")
                .then(Commands.literal("on").then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> set(ctx, true))))
                .then(Commands.literal("off").then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> set(ctx, false))))
                .then(Commands.literal("type").then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("type", IntegerArgumentType.integer(0, Demiurge.MAGIC_TYPES.size() - 1))
                                .executes(DemiurgeCommand::type))))
                .then(Commands.literal("gift").then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("picks", StringArgumentType.greedyString())
                                .executes(ctx -> give(ctx, true)))))
                .then(Commands.literal("take").then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> take(ctx, true))));
    }

    private static Collection<ServerPlayer> targets(CommandContext<CommandSourceStack> ctx, boolean operator) throws CommandSyntaxException {
        return operator ? EntityArgument.getPlayers(ctx, "players") : List.of(EntityArgument.getPlayer(ctx, "player"));
    }

    private static int give(CommandContext<CommandSourceStack> ctx, boolean operator) throws CommandSyntaxException {
        ServerPlayer giver = operator ? null : ctx.getSource().getPlayerOrException();
        List<Integer> picks = DemiurgeGifts.parse(StringArgumentType.getString(ctx, "picks"));
        int given = 0;
        for (ServerPlayer target : targets(ctx, operator)) {
            String refused = DemiurgeGifts.give(giver, target, picks);
            if (refused == null) {
                given++;
            } else if (operator) {
                ctx.getSource().sendFailure(Component.translatable(refused, target.getDisplayName()));
            }
        }
        int n = given;
        if (n > 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.demiurge.gift", n)
                    .withStyle(ChatFormatting.AQUA), operator);
        }
        return n;
    }

    private static int take(CommandContext<CommandSourceStack> ctx, boolean operator) throws CommandSyntaxException {
        ServerPlayer giver = operator ? null : ctx.getSource().getPlayerOrException();
        int taken = 0;
        for (ServerPlayer target : targets(ctx, operator)) {
            if (DemiurgeGifts.takeBack(giver, target)) {
                taken++;
            }
        }
        int n = taken;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.demiurge.take", n)
                .withStyle(ChatFormatting.AQUA), operator);
        return n;
    }

    private static int set(CommandContext<CommandSourceStack> ctx, boolean on) throws CommandSyntaxException {
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            PowerData data = Powers.get(player);
            if (data.power() != Power.BILLY) {
                ctx.getSource().sendFailure(Component.translatable("commands.enhanced.demiurge.not_billy", player.getDisplayName()));
                continue;
            }
            if (on && !Demiurge.allowed(player)) {
                ctx.getSource().sendFailure(Component.translatable("commands.enhanced.demiurge.no_ultimate", player.getDisplayName()));
                continue;
            }
            if (on && !Demiurge.is(data)) {
                Demiurge.enter(player, data);
            } else if (!on) {
                Demiurge.revert(player, data, "message.enhanced.demiurge.off");
            }
            done++;
        }
        int n = done;
        ctx.getSource().sendSuccess(() -> Component.translatable(on ? "commands.enhanced.demiurge.on" : "commands.enhanced.demiurge.off", n)
                .withStyle(ChatFormatting.AQUA), true);
        return n;
    }

    private static int type(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int type = IntegerArgumentType.getInteger(ctx, "type");
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            PowerData data = Powers.get(player);
            if (!Demiurge.is(data)) {
                ctx.getSource().sendFailure(Component.translatable("commands.enhanced.demiurge.not_demiurge", player.getDisplayName()));
                continue;
            }
            Demiurge.setType(player, data, type);
            done++;
        }
        int n = done;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.demiurge.type", n, Demiurge.typeName(type))
                .withStyle(ChatFormatting.AQUA), true);
        return n;
    }
}
