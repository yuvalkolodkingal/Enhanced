package com.kolodkingal.enhanced.vishanti;

import com.kolodkingal.enhanced.ability.PlaceBook;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.UUID;

/**
 * {@code /enhanced ward list} lists every ward of the server (whose, where, how far it reaches) and {@code /enhanced ward
 * remove <player>} (an online player, or any name or id of one who is offline or gone) takes away every ward of a player: the operator's tools for wards, which persist until their base is
 * forgotten. Part of the operator-only {@code /enhanced} command.
 */
public final class WardCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("ward")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> remove(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player")))));
    }

    private static int list(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            WardStore store = WardStore.peek(level);
            if (store == null) {
                continue;
            }
            for (Ward ward : store.wards()) {
                count++;
                String owner = ownerName(server, ward.owner());
                source.sendSuccess(() -> Component.translatable("commands.enhanced.ward.line", owner, ward.name(),
                        (int) Math.floor(ward.center().x), (int) Math.floor(ward.center().y), (int) Math.floor(ward.center().z),
                        level.dimension().location().toString(), ward.radius()), false);
            }
        }
        if (count == 0) {
            source.sendSuccess(() -> Component.translatable("commands.enhanced.ward.none"), false);
        }
        return count;
    }

    private static int remove(CommandSourceStack source, Collection<GameProfile> profiles) {
        int removed = 0;
        for (GameProfile profile : profiles) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(profile.getId());
            if (online != null) {
                // Their bases stay in their book as places; only the wards are gone, until they save a base again.
                PlaceBook.Kind.VISHANTI.set(online, PlaceBook.Kind.VISHANTI.data(online).withoutBases());
            }
            // (A player who is offline, banned or gone: the wards go from the world by their id alone.)
            int gone = WardManager.removeAll(source.getServer(), profile.getId());
            removed += gone;
            source.sendSuccess(() -> Component.translatable("commands.enhanced.ward.removed", gone, profile.getName()), true);
        }
        return removed;
    }

    private static String ownerName(MinecraftServer server, UUID owner) {
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return server.getProfileCache() == null ? owner.toString()
                : server.getProfileCache().get(owner).map(profile -> profile.getName()).orElse(owner.toString());
    }

    private WardCommands() {
    }
}
