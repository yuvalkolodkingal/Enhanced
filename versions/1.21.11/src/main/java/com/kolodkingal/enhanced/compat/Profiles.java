package com.kolodkingal.enhanced.compat;
//#norenames

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserNameToIdResolver;

import java.util.Optional;
import java.util.UUID;

/**
 * 1.21.11 only: 1.21.1's {@code server.getProfileCache()}: from 1.21.9 the server knows players' names and ids through
 * {@code services().nameToIdCache()}, which gives a name and an id (made into the profile 1.21.1's cache gave).
 */
public final class Profiles {
    private final UserNameToIdResolver cache;

    private Profiles(UserNameToIdResolver cache) {
        this.cache = cache;
    }

    public static Profiles cache(MinecraftServer server) {
        return new Profiles(server.services().nameToIdCache());
    }

    public Optional<GameProfile> get(UUID id) {
        return cache.get(id).map(n -> new GameProfile(n.id(), n.name()));
    }

    public Optional<GameProfile> get(String name) {
        return cache.get(name).map(n -> new GameProfile(n.id(), n.name()));
    }
}
