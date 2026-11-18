package com.kolodkingal.enhanced.config;

import com.kolodkingal.enhanced.config.core.ConfigEdits;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.config.core.ConfigValuePayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /enhanced config} (3.17, operators only, under {@code /enhanced}): every setting and limit of the server's
 * config file, read and changed from inside the game.
 * <pre>
 * /enhanced config                      the settings screen, on the operator's client (also: menu)
 * /enhanced config get &lt;key&gt;            its value, its default, what it accepts
 * /enhanced config set &lt;key&gt; &lt;value&gt;    checked by the config's own rules; a bad value changes nothing
 * /enhanced config reset &lt;key&gt;          back to its default
 * /enhanced config list [section]       the sections, or a section's settings and values
 * </pre>
 * A key is the setting's sections and name joined by dots, as in the file ({@code custom_hero.budget}); Tab completes
 * it. A change is put in use at once, written to the config file the server reads and sent to every player
 * ({@link ConfigEdits}).
 */
public final class ConfigCommand {
    private static final String KEY = "commands.enhanced.config.";
    /** The most settings one {@code list} prints. */
    private static final int LIST_LIMIT = 120;

    private ConfigCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("config")
                .executes(ctx -> menu(ctx.getSource()))
                .then(Commands.literal("menu").executes(ctx -> menu(ctx.getSource())))
                .then(Commands.literal("get")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ConfigEdits.keys(), b))
                                .executes(ConfigCommand::get)))
                .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ConfigEdits.keys(), b))
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(choices(
                                                StringArgumentType.getString(ctx, "key")), b))
                                        .executes(ctx -> set(ctx, StringArgumentType.getString(ctx, "value"))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ConfigEdits.keys(), b))
                                .executes(ctx -> set(ctx, null))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource(), ""))
                        .then(Commands.argument("section", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(sections().keySet(), b))
                                .executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, "section")))));
    }

    /** What Tab offers for a setting's value: what it is now, its default, its choices. */
    private static List<String> choices(String key) {
        List<String> out = new ArrayList<>();
        ConfigSpec.ConfigValue<?> value = ConfigEdits.find(key);
        if (value == null || !value.owner().isLoaded()) {
            return out;
        }
        if (value instanceof ConfigSpec.BooleanValue) {
            out.add("true");
            out.add("false");
        } else if (value instanceof ConfigSpec.EnumValue<?>) {
            out.addAll(List.of(ConfigEdits.accepts(value).split(", ")));
        } else {
            out.add(ConfigEdits.text(value.getRaw()));
            out.add(ConfigEdits.text(value.getDefault()));
        }
        return out;
    }

    /** Every section that holds settings (a setting's key without its last name), with how many it holds. */
    private static Map<String, Integer> sections() {
        Map<String, Integer> sections = new LinkedHashMap<>();
        for (String key : ConfigEdits.keys()) {
            int dot = key.lastIndexOf('.');
            sections.merge(dot < 0 ? "" : key.substring(0, dot), 1, Integer::sum);
        }
        return sections;
    }

    private static int menu(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Net.sendToPlayer(player, new ConfigValuePayload("", ""));
        return 1;
    }

    private static ConfigSpec.ConfigValue<?> value(CommandContext<CommandSourceStack> ctx) {
        ConfigSpec.ConfigValue<?> value = ConfigEdits.find(StringArgumentType.getString(ctx, "key"));
        if (value == null || !value.owner().isLoaded()) {
            ctx.getSource().sendFailure(Component.translatableWithFallback(KEY + "unknown", "There is no setting %s.",
                    StringArgumentType.getString(ctx, "key")));
            return null;
        }
        return value;
    }

    private static MutableComponent line(ConfigSpec.ConfigValue<?> value) {
        boolean changed = !java.util.Objects.equals(value.getRaw(), value.getDefault());
        return Component.literal(ConfigEdits.key(value)).withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" = ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(ConfigEdits.text(value.getRaw())).withStyle(changed ? ChatFormatting.GOLD : ChatFormatting.WHITE));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) {
        ConfigSpec.ConfigValue<?> value = value(ctx);
        if (value == null) {
            return 0;
        }
        MutableComponent text = line(value);
        text.append("\n").append(Component.translatableWithFallback(KEY + "default", "Default: %s. Accepts: %s.",
                ConfigEdits.text(value.getDefault()), ConfigEdits.accepts(value)).withStyle(ChatFormatting.GRAY));
        String comment = value.getSpec().getComment();
        if (comment != null) {
            // (the file's comment ends with the default and the range, which the line above has said)
            StringBuilder told = new StringBuilder();
            for (String line : comment.split("\n")) {
                String stripped = line.strip();
                if (!stripped.isEmpty() && !stripped.startsWith("Default: ") && !stripped.startsWith("Range: ")
                        && !stripped.startsWith("Allowed Values: ")) {
                    told.append(told.length() == 0 ? "" : " ").append(stripped);
                }
            }
            if (told.length() > 0) {
                text.append("\n").append(Component.literal(told.toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        ctx.getSource().sendSuccess(() -> text, false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx, String text) {
        ConfigSpec.ConfigValue<?> value = value(ctx);
        if (value == null) {
            return 0;
        }
        String before = ConfigEdits.text(value.getRaw());
        try {
            Object now = ConfigEdits.change(ConfigEdits.key(value), text);
            ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(KEY + (text == null ? "reset" : "set"),
                    text == null ? "%s is back to its default, %s (it was %s)." : "%s is now %s (it was %s).",
                    ConfigEdits.key(value), ConfigEdits.text(now), before).withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (ConfigEdits.Refused refused) {
            ctx.getSource().sendFailure(refused.reason());
            return 0;
        }
    }

    private static int list(CommandSourceStack source, String section) {
        Map<String, Integer> sections = sections();
        if (section.isEmpty()) {
            MutableComponent text = Component.translatableWithFallback(KEY + "sections",
                    "%s settings in %s sections (/enhanced config list <section>):", ConfigEdits.keys().size(), sections.size())
                    .withStyle(ChatFormatting.GOLD);
            for (Map.Entry<String, Integer> e : sections.entrySet()) {
                text.append("\n").append(Component.literal(e.getKey().isEmpty() ? "(top)" : e.getKey()).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" (" + e.getValue() + ")").withStyle(ChatFormatting.GRAY));
            }
            source.sendSuccess(() -> text, false);
            return sections.size();
        }
        List<ConfigSpec.ConfigValue<?>> found = new ArrayList<>();
        for (ConfigSpec.ConfigValue<?> value : ConfigEdits.values()) {
            String key = ConfigEdits.key(value);
            if (value.owner().isLoaded() && (key.startsWith(section + ".") || key.equals(section))) {
                found.add(value);
            }
        }
        if (found.isEmpty()) {
            source.sendFailure(Component.translatableWithFallback(KEY + "no_section", "There is no section %s.", section));
            return 0;
        }
        MutableComponent text = Component.translatableWithFallback(KEY + "section", "%s: %s settings (changed ones in gold)",
                section, found.size()).withStyle(ChatFormatting.GOLD);
        for (int i = 0; i < Math.min(found.size(), LIST_LIMIT); i++) {
            text.append("\n").append(line(found.get(i)));
        }
        if (found.size() > LIST_LIMIT) {
            text.append("\n").append(Component.translatableWithFallback(KEY + "more", "... and %s more: list a section inside it.",
                    found.size() - LIST_LIMIT).withStyle(ChatFormatting.GRAY));
        }
        source.sendSuccess(() -> text, false);
        return found.size();
    }
}
