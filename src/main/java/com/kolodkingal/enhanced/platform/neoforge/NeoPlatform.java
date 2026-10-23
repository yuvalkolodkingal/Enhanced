package com.kolodkingal.enhanced.platform.neoforge;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.EntityAttributesCompat;
import com.kolodkingal.enhanced.platform.ItemAccess;
import com.kolodkingal.enhanced.platform.Loader;
import com.kolodkingal.enhanced.platform.LoaderHooks;
import com.kolodkingal.enhanced.platform.Parts;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.attach.AttachService;
import com.kolodkingal.enhanced.platform.net.NetService;
import com.kolodkingal.enhanced.platform.reg.RegService;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** The platform on NeoForge: each method is the NeoForge call the mod made before the port. */
public final class NeoPlatform implements Platform {
    private final NeoNet net = new NeoNet();
    private final NeoAttach attachments = new NeoAttach();
    private final NeoRegistrars registries = new NeoRegistrars();
    private final NeoItemAccess itemAccess = new NeoItemAccess();
    private final NeoHooks hooks = new NeoHooks();

    private final Parts parts = new Parts() {
        @Override
        public boolean isPart(Entity entity) {
            return entity instanceof PartEntity<?>;
        }

        @Override
        public @Nullable Entity parentOf(Entity entity) {
            return entity instanceof PartEntity<?> part ? part.getParent() : null;
        }
    };

    private final EntityAttributesCompat attributes = new EntityAttributesCompat() {
        @Override
        public Holder<Attribute> swimSpeed() {
            return NeoForgeMod.SWIM_SPEED;
        }

        @Override
        public Holder<Attribute> creativeFlight() {
            return NeoForgeMod.CREATIVE_FLIGHT;
        }
    };

    @Override
    public Loader loader() {
        return Loader.NEOFORGE;
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public boolean isDevelopment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Optional<Path> modResource(String path) {
        IModFileInfo info = ModList.get().getModFileById(Enhanced.MODID);
        if (info == null) {
            return Optional.empty();
        }
        Path found = info.getFile().findResource(path.split("/"));
        return Files.exists(found) ? Optional.of(found) : Optional.empty();
    }

    @Override
    public @Nullable MinecraftServer currentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayerFactory.get(level, profile);
    }

    @Override
    public ItemAccess itemAccess() {
        return itemAccess;
    }

    @Override
    public Parts parts() {
        return parts;
    }

    @Override
    public EntityAttributesCompat attributes() {
        return attributes;
    }

    @Override
    public LoaderHooks hooks() {
        return hooks;
    }

    @Override
    public NetService net() {
        return net;
    }

    @Override
    public AttachService attachments() {
        return attachments;
    }

    @Override
    public RegService registries() {
        return registries;
    }

    /** The NeoForge platform (to reach its glue from the mod's NeoForge entry point). */
    public static NeoPlatform get() {
        return (NeoPlatform) Platform.get();
    }

    NeoNet neoNet() {
        return net;
    }

    NeoAttach neoAttach() {
        return attachments;
    }

    NeoRegistrars neoRegistrars() {
        return registries;
    }
}
