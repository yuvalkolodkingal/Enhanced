package com.kolodkingal.enhanced.command;

import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.build.BuildCommands;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.vishanti.WardCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
 * /enhanced boss start &lt;ultron|thanos|apocalypse&gt; [force]
 * /enhanced boss stop
 * /enhanced boss list
 * /enhanced boss cleanup &lt;center&gt; &lt;radius&gt;
 * /enhanced build undo &lt;player&gt; [steps]
 * /enhanced build stop [&lt;player&gt;|all]
 * /enhanced build list
 * /enhanced build history &lt;player&gt;
 * /enhanced build rank &lt;player&gt; [1-4]
 * /enhanced build meter &lt;player&gt; &lt;value&gt;
 * /enhanced build forget &lt;player&gt;
 * /enhanced ward list
 * /enhanced ward remove &lt;player&gt;
 *
 * <p>The {@code build} branch is {@link BuildCommands#node()} (Build Magic's operators' tools, D-B 16).
 */
public final class EnhancedCommand {
    private static final DynamicCommandExceptionType UNKNOWN_POWER =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.enhanced.power.unknown", name));
    private static final DynamicCommandExceptionType UNKNOWN_BOSS =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.enhanced.boss.unknown", name));
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
                        .then(Commands.literal("clear").executes(ctx -> clearHexes(ctx.getSource()))))
                .then(Commands.literal("boss")
                        .then(Commands.literal("start")
                                .then(Commands.argument("boss", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(BossKind.values()).map(BossKind::id), builder))
                                        .executes(ctx -> startBoss(ctx, false))
                                        .then(Commands.literal("force").executes(ctx -> startBoss(ctx, true)))))
                        .then(Commands.literal("stop").executes(ctx -> stopBosses(ctx.getSource())))
                        .then(Commands.literal("list").executes(ctx -> listBosses(ctx.getSource())))
                        .then(Commands.literal("cleanup")
                                .then(Commands.argument("center", Vec3Argument.vec3())
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 512))
                                                .executes(ctx -> cleanupBosses(ctx.getSource(), Vec3Argument.getVec3(ctx, "center"),
                                                        IntegerArgumentType.getInteger(ctx, "radius")))))))
                .then(BuildCommands.node())
                .then(WardCommands.node()));
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

    /** Wakes a boss at the player who runs the command; "force" skips the ritual's own checks. */
    private static int startBoss(CommandContext<CommandSourceStack> ctx, boolean force) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "boss");
        BossKind kind = BossKind.byId(name);
        if (kind == null) {
            throw UNKNOWN_BOSS.create(name);
        }
        Component boss = Component.translatable("entity.enhanced." + kind.id());
        if (!BossFights.start(kind, ctx.getSource().getPlayerOrException(), force)) {
            ctx.getSource().sendFailure(Component.translatable("commands.enhanced.boss.start.failed", boss));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.boss.start", boss), true);
        return 1;
    }

    private static int stopBosses(CommandSourceStack source) {
        int running = BossFights.all(source.getServer()).size();
        BossFights.stopAll(source.getServer(), StopReason.COMMAND);
        source.sendSuccess(() -> Component.translatable(running == 0 ? "commands.enhanced.boss.stop.none" : "commands.enhanced.boss.stop"), true);
        return 1;
    }

    private static int listBosses(CommandSourceStack source) {
        List<BossFight> fights = BossFights.all(source.getServer());
        if (fights.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.boss.list.none"), false);
            return 0;
        }
        for (BossFight fight : fights) {
            source.sendSuccess(() -> Component.literal(fight.describe()), false);
        }
        return fights.size();
    }

    /** Removes the boss entities left in a ball round a point: what a fight that went wrong leaves behind. */
    private static int cleanupBosses(CommandSourceStack source, Vec3 center, int radius) {
        List<Entity> strays = source.getLevel().getEntities((Entity) null, AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0),
                entity -> BossFights.isFightEntity(entity) && entity.position().distanceToSqr(center) <= (double) radius * radius);
        strays.forEach(Entity::discard);
        source.sendSuccess(() -> Component.translatable("commands.enhanced.boss.cleanup", strays.size()), true);
        return strays.size();
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
