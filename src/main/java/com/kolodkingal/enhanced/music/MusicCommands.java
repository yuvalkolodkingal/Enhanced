package com.kolodkingal.enhanced.music;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * {@code /enhanced music reload}: reads the world's music folder again and tells every player (as /reload does, without
 * reloading anything else); {@code /enhanced music list}: what the folder holds, what is played for what, and what was left
 * out and why. Operators only (as all of /enhanced).
 */
public final class MusicCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("music")
                .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())));
    }

    private static int reload(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ServerMusic.Scan scan = ServerMusic.reload(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerMusic.sendManifest(player);
        }
        source.sendSuccess(() -> Component.translatable("commands.enhanced.music.reloaded", scan.songs().size(),
                scan.rejected().size(), ServerMusic.folder(server).toString()), true);
        return scan.songs().size();
    }

    private static int list(CommandSourceStack source) {
        ServerMusic.Scan scan = ServerMusic.current();
        source.sendSuccess(() -> Component.translatable("commands.enhanced.music.folder", ServerMusic.folder(source.getServer()).toString()), false);
        if (scan.songs().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.music.none"), false);
        }
        for (ServerMusic.Song song : scan.songs().values()) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.music.song", song.file(), song.size() / 1024,
                    song.hash().substring(0, 12)), false);
        }
        for (Map.Entry<String, String> bad : scan.rejected().entrySet()) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.music.rejected", bad.getKey(), bad.getValue()), false);
        }
        return scan.songs().size();
    }

    private MusicCommands() {
    }
}
