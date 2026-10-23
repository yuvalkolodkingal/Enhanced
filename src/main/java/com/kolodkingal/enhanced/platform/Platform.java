package com.kolodkingal.enhanced.platform;

import com.kolodkingal.enhanced.platform.attach.AttachService;
import com.kolodkingal.enhanced.platform.net.NetService;
import com.kolodkingal.enhanced.platform.reg.RegService;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * What the mod asks of the mod loader it runs on (port P0, PLAN2 3.3). Common code sees only this interface and the mod's
 * own types; each loader has one implementation, found with {@link ServiceLoader} ({@code META-INF/services}). On NeoForge
 * it is a thin adapter over the native API ({@code platform.neoforge.NeoPlatform}), so behaviour stays the same by
 * construction.
 */
public interface Platform {
    /** The platform of the loader the game runs on. */
    static Platform get() {
        return Lookup.INSTANCE;
    }

    Loader loader();

    /** Whether this is a game client (single player included), not a dedicated server. */
    boolean isPhysicalClient();

    /** Whether the game runs from a development environment (not a production install). */
    boolean isDevelopment();

    /** The game's {@code config} folder. */
    Path configDir();

    /**
     * A file of the mod's own jar (or of its resource folders in development), by its path inside the jar, such as
     * {@code assets/enhanced/lang/en_us.json}; empty if there is none.
     */
    Optional<Path> modResource(String path);

    /** The server the game runs right now (the integrated one in single player), or null. */
    @Nullable
    MinecraftServer currentServer();

    /** A fake player for actions no real player does (Build Magic's work while its builder is away). */
    ServerPlayer fakePlayer(ServerLevel level, GameProfile profile);

    /** Inventories of blocks that are not vanilla {@code Container}s (other mods' storage). */
    ItemAccess itemAccess();

    /** Multipart entities (the Ender Dragon's parts, other mods' bosses). */
    Parts parts();

    /** The attributes the loader adds (swim speed, creative flight). */
    EntityAttributesCompat attributes();

    /** The loader's own hooks that the mod fires (block place, block break, projectile impact...). */
    LoaderHooks hooks();

    // ---- SPI: used by Net, Attach and Registrars; gameplay code calls those, not these. ----

    NetService net();

    AttachService attachments();

    RegService registries();

    /** The loader's platform, found once. */
    final class Lookup {
        private static final Platform INSTANCE = load();

        private static Platform load() {
            Optional<Platform> found = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            // A loader whose module layer hides the service file still has its class by name.
            for (String name : new String[]{"com.kolodkingal.enhanced.platform.neoforge.NeoPlatform",
                    "com.kolodkingal.enhanced.platform.fabric.FabricPlatform"}) {
                try {
                    return (Platform) Class.forName(name).getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    // not this loader
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot create the platform " + name, e);
                }
            }
            throw new IllegalStateException("No Enhanced platform for this mod loader");
        }

        private Lookup() {
        }
    }
}
