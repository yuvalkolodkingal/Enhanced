package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The operator's side of Build Magic (D-B 16): the {@code build} branch of {@code /enhanced}, which already needs
 * permission level 2.
 *
 * <pre>
 * /enhanced build undo &lt;player&gt; [steps]       undoes his last steps, as Ctrl + Z would (a parent fixes a child's mistake)
 * /enhanced build stop [&lt;player&gt;|all]        stops working spells where they are (what they did stays, undoable), drops queues
 * /enhanced build list                         the spells working or waiting on the server
 * /enhanced build history &lt;player&gt;            his steps
 * /enhanced build rank &lt;player&gt; [1-4]         Billy's rank and practice; with a number, sets it
 * /enhanced build meter &lt;player&gt; &lt;value&gt;      sets Billy's meter (0 to its top)
 * /enhanced build forget &lt;player&gt;             clears his history and his pocket, in memory and on disk
 * </pre>
 */
public final class BuildCommands {

    /** The {@code build} node, to join the tree of {@code /enhanced}. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("build")
                .then(Commands.literal("undo")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> undo(ctx, 1))
                                .then(Commands.argument("steps", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> undo(ctx, IntegerArgumentType.getInteger(ctx, "steps"))))))
                .then(Commands.literal("stop")
                        .executes(ctx -> stop(ctx.getSource(), List.of()))
                        .then(Commands.literal("all").executes(ctx -> stop(ctx.getSource(), List.of())))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> stop(ctx.getSource(), List.of(EntityArgument.getPlayer(ctx, "player").getUUID())))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("history")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> history(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("rank")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> rank(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), 0))
                                .then(Commands.argument("rank", IntegerArgumentType.integer(BillyRanks.MIN_RANK, BillyRanks.MAX_RANK))
                                        .executes(ctx -> rank(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "rank"))))))
                .then(Commands.literal("meter")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> meter(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "value"))))))
                .then(Commands.literal("forget")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> forget(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
    }

    /**
     * Undoes the last {@code steps} steps of {@code target}, newest first, each as a job queued behind what he is doing, with
     * the safe-undo rule of Ctrl + Z. A step that left memory is read back in the background first (the next waits for it,
     * so the order stays); one whose file cannot be read is skipped.
     */
    private static int undo(CommandContext<CommandSourceStack> ctx, int steps) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        UndoHistory history = UndoHistory.of(target.getUUID());
        List<UndoStep> list = history.toUndo(steps);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.build.undo", list.size(), target.getDisplayName()), true);
        queueUndo(server, target, history, list, 0);
        return list.size();
    }

    private static void queueUndo(MinecraftServer server, ServerPlayer target, UndoHistory history, List<UndoStep> list, int i) {
        for (; i < list.size(); i++) {
            UndoStep step = list.get(i);
            ServerLevel level = server.getLevel(step.dimension());
            if (level == null) {
                continue;
            }
            if (!step.loaded()) {
                int next = i + 1;
                UndoFiles.loadStepLater(server, history, step, ok -> {
                    if (ok) {
                        queueOne(server, target, level, step);
                    }
                    queueUndo(server, target, history, list, next);
                });
                return;
            }
            queueOne(server, target, level, step);
        }
        BuildMagic.sync(server, target.getUUID());
    }

    private static void queueOne(MinecraftServer server, ServerPlayer target, ServerLevel level, UndoStep step) {
        Limits limits = Limits.of(target);
        if (limits == null) {
            limits = Limits.wanda(Limits.world(level)); // (he is no builder any more: the safe-undo rule is all that is asked)
        }
        BuildJob job = BuildJob.replay(target, limits, level, step, false, false, BuildScheduler.loaded(target.getUUID(), level));
        BuildScheduler.queue(job, false);
        job.fx(BuildFxPayload.START);
        BuildMagic.sync(server, target.getUUID());
    }

    /** Stops the spells of {@code players} (all of them when none is named) and drops their queues. */
    private static int stop(CommandSourceStack source, List<UUID> players) {
        Set<UUID> who = new LinkedHashSet<>(players);
        if (who.isEmpty()) {
            for (BuildJob job : BuildScheduler.all()) {
                who.add(job.caster());
            }
        }
        int stopped = 0;
        for (UUID player : who) {
            stopped += BuildScheduler.stop(player);
        }
        int count = stopped;
        source.sendSuccess(() -> Component.translatable("commands.enhanced.build.stop", count), true);
        return stopped;
    }

    private static int list(CommandSourceStack source) {
        List<BuildJob> jobs = BuildScheduler.all();
        if (jobs.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.build.list.none"), false);
            return 0;
        }
        for (BuildJob job : jobs) {
            Component what = job.kind() == BuildJob.Kind.SPELL ? Component.translatable(job.recipe().spell().key())
                    : Component.literal(job.kind().name().toLowerCase(java.util.Locale.ROOT));
            Component line = Component.translatable("commands.enhanced.build.list.entry", job.profile().getName(), what,
                    job.level().dimension().location().getPath(), job.permille() / 10, job.walked() + job.remaining());
            source.sendSuccess(() -> line, false);
        }
        return jobs.size();
    }

    private static int history(CommandSourceStack source, ServerPlayer target) {
        UndoHistory history = UndoHistory.of(target.getUUID());
        List<UndoStep> steps = history.allSteps();
        if (steps.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.build.history.empty", target.getDisplayName()), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.enhanced.build.history.header", target.getDisplayName()), false);
        long now = source.getServer().overworld().getGameTime();
        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            UndoStep step = steps.get(i);
            lines.add(Component.translatable("commands.enhanced.build.history.line", i + 1, Component.translatable(step.spell().key()), step.changed(),
                    Math.max(0L, (now - step.time()) / 1200L), step.dimension().location().getPath()));
        }
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return steps.size();
    }

    /** Billy's rank and practice; with {@code rank} (1 to 4) sets it: the practice that rank's threshold, the meter full at its new top. */
    private static int rank(CommandSourceStack source, ServerPlayer target, int rank) {
        if (Powers.power(target) != Power.BILLY) {
            source.sendFailure(Component.translatable("commands.enhanced.build.not_billy", target.getDisplayName()));
            return 0;
        }
        long time = source.getServer().overworld().getGameTime();
        BuildData data = target.getData(ModAttachments.BUILD_DATA);
        if (rank > 0) {
            data = BillyMagic.setRank(data, time, rank);
            target.setData(ModAttachments.BUILD_DATA, data);
            source.sendSuccess(() -> Component.translatable("commands.enhanced.build.rank_set", target.getDisplayName(), rank), true);
        }
        BuildData now = data;
        source.sendSuccess(() -> Component.translatable("commands.enhanced.build.rank", target.getDisplayName(),
                Component.translatable(BillyRanks.nameKey(BillyRanks.rank(now))), BillyRanks.rank(now), BillyMagic.meter(now, time)), false);
        return BillyRanks.rank(data);
    }

    private static int meter(CommandSourceStack source, ServerPlayer target, int value) {
        if (Powers.power(target) != Power.BILLY) {
            source.sendFailure(Component.translatable("commands.enhanced.build.not_billy", target.getDisplayName()));
            return 0;
        }
        long time = source.getServer().overworld().getGameTime();
        BuildData data = BillyMagic.setMeter(target.getData(ModAttachments.BUILD_DATA), time, value);
        target.setData(ModAttachments.BUILD_DATA, data);
        int meter = BillyMagic.meter(data, time);
        source.sendSuccess(() -> Component.translatable("commands.enhanced.build.meter", target.getDisplayName(), meter), true);
        return meter;
    }

    /** Clears his history and his pocket, in memory and on disk. */
    private static int forget(CommandSourceStack source, ServerPlayer target) {
        UUID id = target.getUUID();
        UndoHistory history = UndoHistory.existing(id);
        if (history != null) {
            history.forget();
        }
        Pocket.drop(id);
        UndoFiles.forgetFolder(id);
        BuildMagic.sync(source.getServer(), id);
        source.sendSuccess(() -> Component.translatable("commands.enhanced.build.forget", target.getDisplayName()), true);
        return 1;
    }

    private BuildCommands() {
    }
}
