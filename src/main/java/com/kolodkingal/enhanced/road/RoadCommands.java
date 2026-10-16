package com.kolodkingal.enhanced.road;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * The Road's commands.
 * <pre>
 * /enhanced road status [player]
 * /enhanced road enter &lt;players&gt; [trial]     start a trial (the one reached, or the given one; the number after the last trial is the end), no Gene needed
 * /enhanced road leave &lt;players&gt;
 * /enhanced road reset &lt;players&gt; [newcourse]  forget their progress (and deal them other courses)
 * /enhanced road skip &lt;players&gt;               win the trial they are in (or count the next one won)
 * /enhanced road set &lt;players&gt; &lt;won&gt;          set how many trials they have won
 * /enhanced road layout [player]              where the checkpoints, the finish and the gate of his trial are
 * /enhanced road cells                         the cells in use
 * /road leave                                  (anyone) leave the Road, keeping the Gene
 * /road choose                                 (anyone) at the end of the Road: the choice of Billy or Tommy again
 * /road status                                 (anyone) how far on the Road
 * </pre>
 */
public final class RoadCommands {

    /** The {@code road} branch of {@code /enhanced}. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("road")
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("enter")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> enter(ctx, -1))
                                .then(Commands.argument("trial", IntegerArgumentType.integer(1, 99))
                                        .executes(ctx -> enter(ctx, IntegerArgumentType.getInteger(ctx, "trial") - 1)))))
                .then(Commands.literal("leave")
                        .then(Commands.argument("players", EntityArgument.players()).executes(RoadCommands::leave)))
                .then(Commands.literal("reset")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> reset(ctx, false))
                                .then(Commands.literal("newcourse").executes(ctx -> reset(ctx, true)))))
                .then(Commands.literal("skip")
                        .then(Commands.argument("players", EntityArgument.players()).executes(RoadCommands::skip)))
                .then(Commands.literal("set")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("won", IntegerArgumentType.integer(0, 99)).executes(RoadCommands::set))))
                .then(Commands.literal("layout")
                        .executes(ctx -> layout(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> layout(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("cells").executes(ctx -> cells(ctx.getSource())));
    }

    /** The commands anyone may use: {@code /road leave} and {@code /road status}. */
    public static void registerPlayerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("road")
                .then(Commands.literal("leave").executes(ctx -> leaveSelf(ctx.getSource())))
                .then(Commands.literal("choose").executes(ctx -> chooseAgain(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))));
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        RoadProgress progress = RoadSave.get(source.getServer()).existing(player.getUUID());
        int completed = progress == null ? 0 : progress.completed();
        boolean inside = RoadTrials.isInside(player);
        source.sendSuccess(() -> Component.translatable("commands.enhanced.road.status", player.getDisplayName(), Math.min(completed, RoadChallenges.count()),
                RoadChallenges.count(), Component.translatable(inside ? "commands.enhanced.road.inside" : "commands.enhanced.road.outside"),
                progress == null ? 0 : progress.falls()), false);
        return completed;
    }

    private static int enter(CommandContext<CommandSourceStack> ctx, int trial) throws CommandSyntaxException {
        int started = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            if (RoadTrials.enter(player, trial)) {
                started++;
            }
        }
        int n = started;
        if (started == 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.enhanced.road.enter.none"));
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.road.enter", n), true);
        }
        return started;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int left = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            if (RoadTrials.isInside(player) || RoadDimension.in(player)) {
                RoadTrials.leave(player, RoadChallenge.End.LEFT);
                left++;
            }
        }
        int n = left;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.road.leave", n), true);
        return left;
    }

    private static int leaveSelf(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!RoadTrials.isInside(player) && !RoadDimension.in(player)) {
            source.sendFailure(Component.translatable("commands.enhanced.road.not_in"));
            return 0;
        }
        RoadTrials.leave(player, RoadChallenge.End.LEFT);
        return 1;
    }

    /** {@code /road choose}: at the end of the Road, the question is asked again (the screen opens). */
    private static int chooseAgain(CommandSourceStack source) throws CommandSyntaxException {
        return RoadEnding.reopen(source.getPlayerOrException(), true) ? 1 : 0;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, boolean newCourse) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer player : players) {
            RoadTrials.reset(ctx.getSource().getServer(), player.getUUID(), newCourse);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.road.reset", players.size()), true);
        return players.size();
    }

    private static int skip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int skipped = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            if (RoadTrials.skip(player)) {
                skipped++;
            }
        }
        int n = skipped;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.road.skip", n), true);
        return skipped;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int won = IntegerArgumentType.getInteger(ctx, "won");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer player : players) {
            RoadTrials.setCompleted(ctx.getSource().getServer(), player.getUUID(), won);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.road.set", players.size(), Math.min(won, RoadChallenges.count())), true);
        return players.size();
    }

    private static String at(Vec3 pos) {
        return String.format("%.1f %.1f %.1f", pos.x, pos.y, pos.z);
    }

    private static String at(@Nullable AABB zone) {
        return zone == null ? "-" : at(zone.getCenter());
    }

    private static int layout(CommandSourceStack source, ServerPlayer player) {
        RoadTrial trial = RoadTrials.trialOf(player.getUUID());
        if (trial == null) {
            source.sendFailure(Component.translatable("commands.enhanced.road.layout.none", player.getDisplayName()));
            return 0;
        }
        RoadLayout layout = trial.layout();
        source.sendSuccess(() -> Component.translatable("commands.enhanced.road.layout", trial.challenge().title(), at(layout.start()), at(layout.finish()),
                layout.gate() == null ? "-" : at(layout.gate().zone()), layout.exitDoor() == null ? "-" : layout.exitDoor().toShortString()), false);
        for (int i = 1; i < layout.checkpoints().size(); i++) {
            int n = i;
            source.sendSuccess(() -> Component.translatable("commands.enhanced.road.layout.checkpoint", n, at(layout.checkpoint(n).respawn())), false);
        }
        return layout.checkpoints().size();
    }

    private static int cells(CommandSourceStack source) {
        RoadSave save = RoadSave.get(source.getServer());
        int count = save.cellCount();
        source.sendSuccess(() -> Component.translatable("commands.enhanced.road.cells", count, RoadSweeper.pending(), RoadTrials.playing()), false);
        return count;
    }

    private RoadCommands() {
    }
}
