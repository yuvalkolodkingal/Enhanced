package com.kolodkingal.enhanced.platform.test;

import com.kolodkingal.enhanced.platform.attach.AttachKey;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * What the GameTests need of the loader they run on (port P0, PLAN2 3.3). One implementation per loader, found with
 * {@link ServiceLoader} in the GameTest source set.
 */
public interface TestHooks {
    static TestHooks get() {
        return Lookup.INSTANCE;
    }

    /** {@code neoforge} or {@code fabric} (the parity report's name). */
    String loaderName();

    /**
     * The classes with {@link EnhancedTest} methods, in the order the loader found them (on NeoForge its scan order,
     * which keeps the tests' order, and so their batches, as they were); empty if the loader cannot list them.
     */
    List<Class<?>> testClasses();

    /**
     * A real player on a fake connection that accepts every message of the mod (synced attachments included), placed on
     * the server with {@code profile}; everything a packet sent to him carries can be read back from the connection's
     * channel ({@code TestKit.sentPackets}).
     */
    ServerPlayer mockPlayerWithConnection(ServerLevel level, GameProfile profile);

    /**
     * Whether {@code packet} (a single packet or a bundle) syncs the attachment {@code key} of the entity
     * {@code entityId}.
     */
    boolean isAttachmentSync(Object packet, AttachKey<?> key, int entityId);

    /**
     * The key of the compound in an entity's saved data that holds its attachments, by id (NeoForge
     * {@code "neoforge:attachments"}; port P0, L8).
     */
    String attachmentsKey();

    /** The version of the mod as the loader read it from the mod's metadata (port P0, L8). */
    String modVersion();

    /**
     * The packets the server sends {@code watcher} about {@code holder}'s synced attachments when he starts seeing it
     * (the first sync), in order; check them with {@link #isAttachmentSync}.
     */
    List<Object> initialSyncPackets(Entity holder, ServerPlayer watcher);

    /**
     * Applies the synced attachments that {@code packet} (a single packet or a bundle) carries to {@code target}, as a
     * client receiving it applies them to its copy of the entity; a packet that carries none changes nothing.
     */
    void receiveSync(Object packet, Entity target);

    /** Finds the loader's hooks once. */
    final class Lookup {
        private static final TestHooks INSTANCE = load();

        private static TestHooks load() {
            Optional<TestHooks> found = ServiceLoader.load(TestHooks.class, TestHooks.class.getClassLoader()).findFirst();
            if (found.isPresent()) {
                return found.get();
            }
            for (String name : new String[]{"com.kolodkingal.enhanced.platform.neoforge.test.NeoTestHooks",
                    "com.kolodkingal.enhanced.platform.fabric.test.FabricTestHooks"}) {
                try {
                    return (TestHooks) Class.forName(name).getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    // not this loader
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot create the test hooks " + name, e);
                }
            }
            throw new IllegalStateException("No Enhanced test hooks for this mod loader");
        }

        private Lookup() {
        }
    }
}
