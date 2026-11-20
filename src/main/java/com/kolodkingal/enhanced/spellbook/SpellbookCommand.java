package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * {@code /enhanced spellbook <players> <ethereal|eldritch|green>}: the players become the book's casters (a hero's power is
 * taken first); {@code /enhanced spellbook <players> none}: casters give their book up; {@code /enhanced spellbook <players>
 * hat <colour 0-11|off|on>}: the Ethereal witch's hat. 3.18.1, the witches' levels: {@code /enhanced spellbook xp <players>
 * add|set <n>} and {@code /enhanced spellbook level <players> <book|gift> <1-5>}.
 */
public final class SpellbookCommand {
    private static final DynamicCommandExceptionType UNKNOWN =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.enhanced.spellbook.unknown", name));

    private SpellbookCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("spellbook")
                // 3.18.1, the witches' levels: their experience, and a level of a book (or of the Demiurge's gift) at once
                .then(Commands.literal("xp")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.literal("add")
                                        .then(Commands.argument("points", IntegerArgumentType.integer(-100000, 100000))
                                                .executes(ctx -> xp(ctx, true))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("points", IntegerArgumentType.integer(0, 100000))
                                                .executes(ctx -> xp(ctx, false))))))
                .then(Commands.literal("level")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("book", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Stream.concat(Arrays.stream(Spellbook.values()).map(b -> b.id),
                                                        Stream.of(WitchLevels.GIFT_KEY)), builder))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, SpellLevels.MAX))
                                                .executes(SpellbookCommand::level)))))
                .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.literal("hat")
                                .then(Commands.literal("off").executes(ctx -> hat(ctx, -1, false)))
                                .then(Commands.literal("on").executes(ctx -> hat(ctx, -1, true)))
                                .then(Commands.argument("color", IntegerArgumentType.integer(0, Spellbooks.HAT_COLORS.size() - 1))
                                        .executes(ctx -> hat(ctx, IntegerArgumentType.getInteger(ctx, "color"), true))))
                        .then(Commands.argument("book", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Stream.concat(Arrays.stream(Spellbook.values()).map(b -> b.id), Stream.of("none")), builder))
                                .executes(SpellbookCommand::set)));
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        String name = StringArgumentType.getString(ctx, "book");
        if (name.equals("none")) {
            int n = 0;
            for (ServerPlayer player : players) {
                if (Spellbooks.renounce(player)) {
                    n++;
                }
            }
            int count = n;
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.spellbook.none", count), true);
            return n;
        }
        Spellbook book = Spellbook.byId(name);
        if (book == null) {
            throw UNKNOWN.create(name);
        }
        for (ServerPlayer player : players) {
            if (Spellbook.of(player) == null && Powers.power(player) != Power.NONE) {
                Powers.setPower(player, Power.NONE);
            }
            Spellbooks.become(player, book);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.spellbook.set", players.size(), book.title()), true);
        return players.size();
    }

    /** {@code xp <players> add|set <points>}: the experience of each witch's source now (players who are no witch are passed over). */
    private static int xp(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        int points = IntegerArgumentType.getInteger(ctx, "points");
        int n = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            if (WitchLevels.setXp(player, points, add)) {
                n++;
            }
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.translatable(add ? "commands.enhanced.spellbook.xp_add"
                : "commands.enhanced.spellbook.xp_set", points, count), true);
        return n;
    }

    /**
     * {@code level <players> <book|gift> <1-5>}: the players are at that level of the book (made its casters first, a
     * hero's power taken, as {@code /enhanced spellbook <players> <book>} does), or of the Demiurge's gift they have.
     */
    private static int level(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        String name = StringArgumentType.getString(ctx, "book");
        int level = IntegerArgumentType.getInteger(ctx, "level");
        Spellbook book = Spellbook.byId(name);
        if (book == null && !WitchLevels.GIFT_KEY.equals(name)) {
            throw UNKNOWN.create(name);
        }
        int n = 0;
        for (ServerPlayer player : players) {
            if (book == null) {
                WitchLevels.Source s = WitchLevels.source(player);
                if (s == null || s.kind() != WitchLevels.Kind.GIFT) {
                    continue;
                }
                WitchLevels.setLevel(player, WitchLevels.GIFT_KEY, level);
            } else {
                ServerPlayer p = player;
                com.kolodkingal.enhanced.registry.ModAttachments.SPELLBOOK.set(p,
                        com.kolodkingal.enhanced.registry.ModAttachments.SPELLBOOK.get(p).withXp(book.id, SpellLevels.threshold(level)));
                if (Spellbook.of(p) != book) {
                    if (Spellbook.of(p) == null && Powers.power(p) != Power.NONE) {
                        Powers.setPower(p, Power.NONE);
                    }
                    Spellbooks.become(p, book);
                } else {
                    WitchLevels.refresh(p);
                }
            }
            n++;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.spellbook.level", count, level), true);
        return n;
    }

    private static int hat(CommandContext<CommandSourceStack> ctx, int color, boolean on) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
            if (Spellbook.of(player) != Spellbook.ETHEREAL) {
                continue;
            }
            if (color >= 0) {
                Spellbooks.chooseHatColor(player, Spellbooks.HAT_COLORS.get(color));
            } else if (Spellbooks.wearsHat(player) != on) {
                Spellbooks.toggleHat(player);
            }
            n++;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.enhanced.spellbook.hat", count), true);
        return n;
    }
}
