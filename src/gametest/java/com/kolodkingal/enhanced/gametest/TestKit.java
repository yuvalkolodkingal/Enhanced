package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

import java.lang.reflect.Field;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.IntPredicate;

/**
 * Shared helpers for the Enhanced GameTests.
 *
 * <p>Every test runs in the {@code enhanced:arena} template: a 21x21 smooth stone floor at relative y = 1, so
 * creatures stand at relative y = 2, with 9 blocks of air above (the test framework adds barrier walls and a
 * barrier ceiling around it).
 */
public final class TestKit {
    public static final String ARENA = "arena";
    /** Relative height of the top of the arena floor. */
    public static final double FLOOR = 2.0;

    private static int playerCounter;

    // ------------------------------------------------------------------------------------------------------------
    // Fixture: everything a test changes outside its own arena, undone when the test ends (pass or fail).

    public static Fixture fixture(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        helper.testInfo.addListener(fixture);
        return fixture;
    }

    public static final class Fixture implements GameTestListener {
        private final GameTestHelper helper;
        private final List<ServerPlayer> players = new ArrayList<>();
        private final List<Runnable> undo = new ArrayList<>();
        private boolean closed;

        private Fixture(GameTestHelper helper) {
            this.helper = helper;
        }

        public GameTestHelper helper() {
            return helper;
        }

        public ServerLevel level() {
            return helper.getLevel();
        }

        /** A Survival player standing at the given position in the arena, already past spawn protection. */
        public ServerPlayer player(double x, double y, double z) {
            return player(x, y, z, GameType.SURVIVAL);
        }

        public ServerPlayer player(double x, double y, double z, Power power) {
            ServerPlayer player = player(x, y, z);
            if (power != Power.NONE) {
                Powers.setPower(player, power);
            }
            return player;
        }

        /**
         * A real {@link ServerPlayer} on a fake connection. Unlike {@link GameTestHelper#makeMockServerPlayerInLevel()}
         * it keeps its real game mode (the vanilla mock always claims to be Creative, and Creative players are
         * immune to the Mind Stone, the Hex wall and most damage), and its connection accepts the mod's synced data.
         *
         * <p>Note: nothing ticks a player without a client, so tests call {@code PowerTicker.tick} (or
         * {@link ServerPlayer#doTick()}, which is exactly what a connected client's tick does) themselves.
         */
        public ServerPlayer player(double x, double y, double z, GameType gameType) {
            ServerLevel level = helper.getLevel();
            MinecraftServer server = level.getServer();
            GameProfile profile = new GameProfile(UUID.randomUUID(), "enh-test-" + (++playerCounter));
            CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
            ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            // The fake client "supports" every channel, so synced attachments (powers, Hexes) can be sent to it.
            connection.channel().attr(ChannelAttributes.ADHOC_CHANNELS).set(new AcceptAll());
            ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(gameType);
            clearSpawnInvulnerability(player);
            Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
            player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            player.setYHeadRot(0.0F);
            players.add(player);
            return player;
        }

        /** Temporarily changes a server config value; the old value comes back when the test ends. */
        public <T> void config(ModConfigSpec.ConfigValue<T> value, T newValue) {
            T old = value.get();
            value.set(newValue);
            undo.add(() -> value.set(old));
        }

        /** PvP is off on the GameTest server; tests where one hero attacks another switch it on. */
        public void pvp(boolean allowed) {
            MinecraftServer server = helper.getLevel().getServer();
            boolean old = server.isPvpAllowed();
            server.setPvpAllowed(allowed);
            undo.add(() -> server.setPvpAllowed(old));
        }

        /** Runs {@code step} once per tick for ticks 1..{@code lastTick}. Returning true ends the test successfully. */
        public void everyTick(int lastTick, IntPredicate step) {
            for (int t = 1; t <= lastTick; t++) {
                int tick = t;
                helper.runAfterDelay(t, () -> {
                    if (step.test(tick)) {
                        helper.succeed();
                    } else if (tick == lastTick) {
                        throw new GameTestAssertException("Test did not finish within " + lastTick + " ticks");
                    }
                });
            }
        }

        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            for (int i = undo.size() - 1; i >= 0; i--) {
                undo.get(i).run();
            }
            MinecraftServer server = helper.getLevel().getServer();
            for (ServerPlayer player : players) {
                HexManager.dissolveAllOwnedBy(server, player.getUUID());
                if (server.getPlayerList().getPlayer(player.getUUID()) == player) {
                    server.getPlayerList().remove(player);
                }
            }
        }

        @Override
        public void testStructureLoaded(GameTestInfo testInfo) {
        }

        @Override
        public void testPassed(GameTestInfo test, GameTestRunner runner) {
            close();
        }

        @Override
        public void testFailed(GameTestInfo test, GameTestRunner runner) {
            close();
        }

        @Override
        public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Players

    private static final Field SPAWN_INVULNERABLE_TIME;

    static {
        try {
            SPAWN_INVULNERABLE_TIME = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            SPAWN_INVULNERABLE_TIME.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** New players can't be hurt for 3 seconds; tests don't want to wait for that. */
    public static void clearSpawnInvulnerability(ServerPlayer player) {
        try {
            SPAWN_INVULNERABLE_TIME.setInt(player, 0);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Turns the player's head (and body) so they look at an absolute position. */
    public static void lookAt(ServerPlayer player, Vec3 target) {
        Vec3 d = target.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = Mth.wrapDegrees((float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F);
        float pitch = Mth.wrapDegrees((float) (-(Mth.atan2(d.y, horizontal) * Mth.RAD_TO_DEG)));
        look(player, yaw, pitch);
    }

    public static void lookAt(ServerPlayer player, Entity target) {
        lookAt(player, target.position().add(0, target.getBbHeight() * 0.5, 0));
    }

    public static void look(ServerPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.yHeadRotO = yaw;
        player.xRotO = pitch;
    }

    /** Yaw that looks along +X. */
    public static final float EAST = -90.0F;

    // ------------------------------------------------------------------------------------------------------------
    // Sounds

    /** A sound the server sent a player: which sound event, and where in the world it was played. */
    public record HeardSound(ResourceLocation id, Vec3 at) {
    }

    /**
     * The sounds the server has sent this player since the last call, oldest first. The fake client's connection keeps
     * every packet written to it, so a test can ask what a player would have heard. A sound reaches every player
     * within 16 blocks of where it is played, so tests that count sounds only count the ones played in their own arena.
     */
    public static List<HeardSound> heardSounds(ServerPlayer player) {
        EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
        // Nothing ticks the fake connection, which is what flushes it: a packet sent later would sit there unseen.
        channel.flush();
        List<HeardSound> heard = new ArrayList<>();
        for (Object sent = channel.readOutbound(); sent != null; sent = channel.readOutbound()) {
            if (sent instanceof ClientboundSoundPacket packet) {
                heard.add(new HeardSound(packet.getSound().value().getLocation(), new Vec3(packet.getX(), packet.getY(), packet.getZ())));
            }
        }
        return heard;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Assertions

    public static void assertNear(double actual, double expected, double tolerance, String what) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new GameTestAssertException(what + ": expected " + expected + " but was " + actual);
        }
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** A channel set that contains every channel, so the fake client accepts every payload. */
    private static final class AcceptAll extends AbstractSet<ResourceLocation> {
        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public boolean add(ResourceLocation resourceLocation) {
            return false;
        }

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    private TestKit() {
    }
}
