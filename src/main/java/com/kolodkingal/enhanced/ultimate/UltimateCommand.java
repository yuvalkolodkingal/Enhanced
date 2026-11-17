package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.platform.net.Net;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * {@code /enhanced ultimate} (3.17, operators only, under {@code /enhanced}): Ultimate Mode for the whole server.
 * <pre>
 * /enhanced ultimate info
 * /enhanced ultimate on|off
 * /enhanced ultimate default
 * </pre>
 * {@code on} and {@code off} are saved with the world ({@code serverconfig/enhanced-ultimate.json}) and win over
 * {@code [ultimate] enabled}; {@code default} drops the world's own setting. Every change is sent to every player at once.
 */
public final class UltimateCommand {
    private static final String KEY = "commands.enhanced.ultimate.";

    private UltimateCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("ultimate")
                .executes(ctx -> info(ctx.getSource()))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("on").executes(ctx -> set(ctx.getSource(), Boolean.TRUE)))
                .then(Commands.literal("off").executes(ctx -> set(ctx.getSource(), Boolean.FALSE)))
                .then(Commands.literal("default").executes(ctx -> set(ctx.getSource(), null)));
    }

    private static int set(CommandSourceStack source, Boolean on) {
        boolean was = Ultimate.on();
        Ultimate.set(on);
        Ultimate.save();
        Net.sendToAllPlayers(UltimateSyncPayload.now());
        if (was && !Ultimate.on()) {
            UltimateForms.endEveryone(source.getServer());
        }
        source.sendSuccess(() -> Component.translatableWithFallback(KEY + (Ultimate.on() ? "on" : "off"),
                Ultimate.on() ? "Ultimate Mode is on: every hero can awaken an ultimate form (O)."
                        : "Ultimate Mode is off.").withStyle(Ultimate.on() ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
        return Ultimate.on() ? 1 : 0;
    }

    private static int info(CommandSourceStack source) {
        Boolean world = Ultimate.worldSetting();
        MutableComponent text = Component.translatableWithFallback(KEY + "info", "Ultimate Mode: %s (%s).",
                Component.translatableWithFallback(KEY + (Ultimate.on() ? "state.on" : "state.off"), Ultimate.on() ? "on" : "off"),
                Component.translatableWithFallback(KEY + (world == null ? "from_config" : "from_world"),
                        world == null ? "from the server config, [ultimate] enabled" : "the world's own setting, /enhanced ultimate"))
                .withStyle(ChatFormatting.GOLD);
        text.append("\n").append(Component.translatableWithFallback(KEY + "explain",
                "When on, O awakens every hero's ultimate form; Source Code Vision runs on the Solar Jewel; Billy is the Demiurge.")
                .withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> text, false);
        return Ultimate.on() ? 1 : 0;
    }
}
