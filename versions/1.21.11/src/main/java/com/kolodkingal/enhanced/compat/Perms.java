package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;

/** 1.21.11 only: 1.21.1's {@code hasPermissions(level)}; from 1.21.11 a player and a command source have a permission set. */
public final class Perms {
    private Perms() {
    }

    private static Permission level(int level) {
        return new Permission.HasCommandLevel(PermissionLevel.byId(level));
    }

    public static boolean has(Player player, int level) {
        return player.permissions().hasPermission(level(level));
    }

    public static boolean has(CommandSourceStack source, int level) {
        return source.permissions().hasPermission(level(level));
    }
}
