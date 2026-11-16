package com.kolodkingal.enhanced.balance;

import com.kolodkingal.enhanced.platform.net.Net;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /enhanced balance} (3.16, operators only, under {@code /enhanced}): the server's SMP balance.
 * <pre>
 * /enhanced balance info
 * /enhanced balance full|balanced|hardcore
 * /enhanced balance set &lt;power&gt; &lt;setting&gt; &lt;strength&gt;
 * /enhanced balance reset [power]
 * /enhanced balance reload
 * </pre>
 * A strength is a share of the power's full strength (1: as configured, 0.5: half the range, size, duration or damage,
 * twice the cooldown or cost). Every change is saved with the world and sent to every player at once.
 */
public final class BalanceCommand {
    private static final String KEY = "commands.enhanced.balance.";
    private static final DynamicCommandExceptionType UNKNOWN_POWER = new DynamicCommandExceptionType(
            name -> Component.translatableWithFallback(KEY + "unknown_power", "Unknown power: %s", name));
    private static final DynamicCommandExceptionType UNKNOWN_SETTING = new DynamicCommandExceptionType(
            name -> Component.translatableWithFallback(KEY + "unknown_setting",
                    "Unknown setting: %s (range, size, duration, cooldown, cost or damage)", name));

    private BalanceCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("balance")
                .executes(ctx -> info(ctx.getSource()))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.argument("power", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Balance.POWERS, b))
                                .then(Commands.argument("setting", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Balance.Setting.values()).map(Balance.Setting::id), b))
                                        .then(Commands.argument("strength",
                                                        DoubleArgumentType.doubleArg(0.05, Balance.MAX_STRENGTH))
                                                .executes(BalanceCommand::set)))))
                .then(Commands.literal("reset")
                        .executes(ctx -> reset(ctx.getSource(), null))
                        .then(Commands.argument("power", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Balance.POWERS, b))
                                .executes(ctx -> reset(ctx.getSource(), power(ctx)))));
        for (Balance.Preset preset : Balance.Preset.values()) {
            node.then(Commands.literal(preset.id()).executes(ctx -> preset(ctx.getSource(), preset)));
        }
        return node;
    }

    private static String power(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String power = StringArgumentType.getString(ctx, "power").toLowerCase(Locale.ROOT);
        if (!Balance.POWERS.contains(power)) {
            throw UNKNOWN_POWER.create(power);
        }
        return power;
    }

    private static int preset(CommandSourceStack source, Balance.Preset preset) {
        Balance.apply(preset, Balance.overrides());
        changed();
        source.sendSuccess(() -> Component.translatableWithFallback(KEY + "preset",
                "SMP balance: the %s preset now applies to every player on the server.", name(preset)), true);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String power = power(ctx);
        String settingId = StringArgumentType.getString(ctx, "setting").toLowerCase(Locale.ROOT);
        Balance.Setting setting = Balance.Setting.byId(settingId);
        if (setting == null) {
            throw UNKNOWN_SETTING.create(settingId);
        }
        double strength = DoubleArgumentType.getDouble(ctx, "strength");
        Map<String, Double> o = new LinkedHashMap<>(Balance.overrides());
        o.put(power + "." + setting.id(), strength);
        Balance.apply(Balance.preset(), o);
        changed();
        ctx.getSource().sendSuccess(() -> Component.translatableWithFallback(KEY + "set",
                "SMP balance: %s's %s is at %s of its full strength (on top of the %s preset).", power, setting.id(),
                percent(strength), name(Balance.preset())), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, String power) {
        Map<String, Double> o = new LinkedHashMap<>(Balance.overrides());
        int before = o.size();
        if (power == null) {
            o.clear();
        } else {
            o.keySet().removeIf(k -> k.startsWith(power + "."));
        }
        Balance.apply(Balance.preset(), o);
        changed();
        int removed = before - o.size();
        source.sendSuccess(() -> power == null
                ? Component.translatableWithFallback(KEY + "reset_all",
                        "SMP balance: %s overrides cleared; every power follows the %s preset.", removed,
                        name(Balance.preset()))
                : Component.translatableWithFallback(KEY + "reset",
                        "SMP balance: %s overrides of %s cleared; it follows the %s preset.", removed, power,
                        name(Balance.preset())), true);
        return removed;
    }

    private static int reload(CommandSourceStack source) {
        String problem = Balance.reload();
        if (problem != null) {
            source.sendFailure(Component.translatableWithFallback(KEY + "reload_failed",
                    "The balance file could not be read (%s); nothing changed.", problem));
            return 0;
        }
        Net.sendToAllPlayers(BalanceSyncPayload.now());
        return info(source);
    }

    private static int info(CommandSourceStack source) {
        Balance.Preset preset = Balance.preset();
        MutableComponent text = Component.translatableWithFallback(KEY + "info",
                "SMP balance: the %s preset, for every player.", name(preset)).withStyle(ChatFormatting.GOLD);
        text.append("\n").append(Component.translatableWithFallback(KEY + "info." + preset.id(), switch (preset) {
            case FULL -> "Every power as the server config has it.";
            case BALANCED -> "The strongest powers at 60% (Scarlet Witch, Phoenix, Thanos, Apocalypse, Odin, Hela, "
                    + "Love, the stones), the strong ones at 80%; Wanda's Build Magic on the scale of Billy's at his "
                    + "highest rank.";
            case HARDCORE -> "Every power at half the balanced strength; Wanda's Build Magic on the scale of an "
                    + "Apprentice Billy.";
        }).withStyle(ChatFormatting.GRAY));
        Map<String, Double> o = Balance.overrides();
        if (o.isEmpty()) {
            text.append("\n").append(Component.translatableWithFallback(KEY + "no_overrides", "No overrides.")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            text.append("\n").append(Component.translatableWithFallback(KEY + "overrides", "Overrides:"));
            o.forEach((k, v) -> text.append("\n  " + k + " = " + percent(v)));
        }
        source.sendSuccess(() -> text, false);
        return 1;
    }

    /** Saved, sent to everyone. */
    private static void changed() {
        Balance.save();
        Net.sendToAllPlayers(BalanceSyncPayload.now());
    }

    private static Component name(Balance.Preset preset) {
        return Component.translatableWithFallback(KEY + "preset." + preset.id(), preset.id());
    }

    private static String percent(double strength) {
        return Math.round(strength * 100) + "%";
    }
}
