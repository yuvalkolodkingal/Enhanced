package com.kolodkingal.enhanced.magic;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;

/**
 * {@code /enhanced witch} (3.17, operators only): the witchcraft of players.
 * <pre>
 * /enhanced witch info [player]
 * /enhanced witch grant &lt;player&gt; &lt;path&gt; [level]
 * /enhanced witch revoke &lt;player&gt;
 * /enhanced witch grimoire &lt;player&gt; &lt;darkhold|vishanti|cagliostro&gt;
 * /enhanced witch cleanse &lt;player&gt;
 * </pre>
 */
public final class WitchCommand {
    private static final String KEY = "commands.enhanced.witch.";

    private WitchCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("witch")
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(WitchPath.values()).map(WitchPath::id), b))
                                        .executes(ctx -> grant(ctx, 1))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, WitchPath.MAX_LEVEL))
                                                .executes(ctx -> grant(ctx, IntegerArgumentType.getInteger(ctx, "level")))))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    Grants.revoke(target);
                                    ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(KEY + "revoked",
                                            "%s has no witchcraft now.", target.getDisplayName()), true);
                                    return 1;
                                })))
                .then(Commands.literal("grimoire")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("book", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"darkhold", "vishanti", "cagliostro"}, b))
                                        .executes(WitchCommand::grimoire))))
                .then(Commands.literal("cleanse")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    Magic.set(target, Magic.data(target).withCorruption(0));
                                    ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(KEY + "cleansed",
                                            "%s's magic is clean of the Darkhold.", target.getDisplayName()), true);
                                    return 1;
                                })));
    }

    private static int grant(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        WitchPath path = WitchPath.byId(StringArgumentType.getString(ctx, "path").toLowerCase(Locale.ROOT));
        if (path == null) {
            ctx.getSource().sendFailure(Component.translatableWithFallback(KEY + "unknown_path",
                    "Unknown path (green, potions, protection, divination, spirit or sorcerer)."));
            return 0;
        }
        Grants.give(target, path, level, true);
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(KEY + "granted", "%s is now %s, level %s.",
                target.getDisplayName(), path.displayName(), level), true);
        return 1;
    }

    private static int grimoire(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String book = StringArgumentType.getString(ctx, "book");
        int flag = switch (book) {
            case "darkhold" -> MagicData.DARKHOLD;
            case "vishanti" -> MagicData.VISHANTI;
            case "cagliostro" -> MagicData.CAGLIOSTRO;
            default -> 0;
        };
        if (flag == 0) {
            return 0;
        }
        Magic.set(target, Magic.data(target).with(flag));
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(KEY + "grimoire", "%s has read %s.",
                target.getDisplayName(), book), true);
        return 1;
    }

    private static int info(CommandSourceStack source, ServerPlayer target) {
        MagicData data = Magic.data(target);
        Component path = data.witchPath() == null ? Component.translatableWithFallback(KEY + "no_craft", "no witchcraft")
                : data.witchPath().displayName();
        source.sendSuccess(() -> Component.translatableWithFallback(KEY + "info",
                "%s: %s (level %s), mana %s/%s, corruption %s, spells known: %s.", target.getDisplayName(), path, data.level(),
                (int) data.mana(), (int) Magic.maxMana(target), data.corruption(), Magic.known(target).size())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }
}
