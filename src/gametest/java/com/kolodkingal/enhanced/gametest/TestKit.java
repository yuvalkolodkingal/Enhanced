package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.ability.HeroServers;
import com.kolodkingal.enhanced.ability.MagnetPull;
import com.kolodkingal.enhanced.boss.BossData;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.BossLane;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.platform.test.TestHooks;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/**
 * Shared helpers for the Enhanced GameTests.
 *
 * <p>Every test runs in the {@code enhanced:arena} template: a 21x21 smooth stone floor at relative y = 1, so
 * creatures stand at relative y = 2, with 9 blocks of air above (the test framework adds barrier walls and a
 * barrier ceiling around it).
 */
public final class TestKit {
    /** A test's hold on the chunks of an area ({@link Fixture#loadChunksAround}), as a forced chunk loads them. */
    static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> AREA_TICKET =
            net.minecraft.server.level.TicketType.create("enhanced_test_area",
                    java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));

    public static final String ARENA = "arena";
    /** Relative height of the top of the arena floor. */
    public static final double FLOOR = 2.0;

    private static int playerCounter;

    // ------------------------------------------------------------------------------------------------------------
    // Fixture: everything a test changes outside its own arena, undone when the test ends (pass or fail).

    public static Fixture fixture(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        helper.testInfo.addListener(fixture);
        // 3.2: the Chaos Shield is unlimited by default; the tests of its limits (strength, hunger, time, recharge) and of
        // everything else that reads them run under the old rules. A test of the unlimited shield sets it back to true.
        fixture.config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, false);
        // 3.2: R is Billy's continuous beam by default; the tests of the single Hex Bolt (and of everything that presses R as
        // a shot) run with the old bolt. A test of the beam sets it back to true.
        fixture.config(EnhancedConfig.BILLY_BEAM, false);
        return fixture;
    }

    /**
     * A fixture for a test of a boss fight, or of anything that keeps server-wide state from one tick to the next (the
     * boss registry, the rewards waiting to be handed over, the heroes' static maps, Build Magic's builders and spells, the
     * Magnetic Pulls): when the test ends, pass or fail, it lets every fight go, clears the heroes' statics, Build Magic
     * and the pulls, puts back the boss lanes and the Thanos summoning the mod registered and the rewards that were
     * waiting, so a fight left over from one test never reaches the next. A boss test also has a batch of its own
     * ({@code @EnhancedTest(batch = ...)}), so no other test runs while it does.
     */
    public static Fixture bossFixture(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        Map<BossKind, BossLane> lanes = new EnumMap<>(BossKind.class);
        for (BossKind kind : BossKind.values()) {
            BossLane lane = BossFights.lane(kind);
            if (lane != null) {
                lanes.put(kind, lane);
            }
        }
        BossData data = ModAttachments.BOSS_DATA.get(server.overworld());
        ScepterRituals.Hold summoning = ScepterRituals.thanosHold();
        fixture.onClose(() -> {
            ScepterRituals.registerThanosHold(summoning);
            BossFights.clear();
            for (HeroServer hero : HeroServers.all()) {
                hero.onServerStopped();
            }
            BuildMagic.clear();
            BuildScheduler.clear();
            MagnetPull.clear();
            for (BossKind kind : BossKind.values()) {
                BossLane lane = lanes.get(kind);
                if (lane == null) {
                    BossFights.unregister(kind);
                } else {
                    BossFights.register(kind, lane);
                }
            }
            ModAttachments.BOSS_DATA.set(server.overworld(), data);
        });
        return fixture;
    }

    /**
     * What the server had before the first of the tests that change it (a config value, PvP) changed it, and how many of
     * them still hold a change: it comes back only when the last of them ends. (If each test put back the value it found,
     * two tests that overlap would leave the change in place for good whenever the first one ends first.) A test that
     * changes one still needs a batch of its own, or the tests beside it would run with its value.
     */
    private static final Map<Object, Held<?>> HELD = new HashMap<>();

    private static final class Held<T> {
        private final T original;
        private final Consumer<T> restore;
        private int holders;

        private Held(T original, Consumer<T> restore) {
            this.original = original;
            this.restore = restore;
        }
    }

    /** Notes a test's change of {@code key} (now {@code current}); the returned undo restores it after the last holder. */
    @SuppressWarnings("unchecked")
    private static <T> Runnable hold(Object key, T current, Consumer<T> restore) {
        Held<T> held = (Held<T>) HELD.computeIfAbsent(key, k -> new Held<>(current, restore));
        held.holders++;
        return () -> {
            if (--held.holders == 0) {
                HELD.remove(key);
                held.restore.accept(held.original);
            }
        };
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
            return player(x, y, z, gameType, UUID.randomUUID());
        }

        /**
         * The same, for a given UUID: a player who logs in again after they were removed from the server is the same
         * person with a new body, and this is how a test brings them back.
         */
        public ServerPlayer player(double x, double y, double z, GameType gameType, UUID uuid) {
            ServerLevel level = helper.getLevel();
            GameProfile profile = new GameProfile(uuid, "enh-test-" + (++playerCounter));
            // The fake client "supports" every channel, so synced attachments (powers, Hexes) can be sent to it.
            ServerPlayer player = TestHooks.get().mockPlayerWithConnection(level, profile);
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
            undo.add(hold(value, value.get(), value::set));
            value.set(newValue);
        }

        /** PvP is off on the GameTest server; tests where one hero attacks another switch it on. */
        public void pvp(boolean allowed) {
            MinecraftServer server = helper.getLevel().getServer();
            undo.add(hold(MinecraftServer.class, server.isPvpAllowed(), server::setPvpAllowed));
            server.setPvpAllowed(allowed);
        }

        /** Runs {@code action} when the test ends, pass or fail (before what was registered earlier is undone). */
        public void onClose(Runnable action) {
            undo.add(action);
        }

        /**
         * Keeps the chunks within {@code radius} chunks of {@code center} loaded until the test closes, as a player
         * standing there would (the test server loads only the chunks of the test structures, so an area reaching past
         * an arena at the edge of a big run may never load by itself). A ticket of its own ({@link #AREA_TICKET}), not
         * a forced chunk: letting it go never unloads a chunk another test (or its structure) forced.
         */
        public void loadChunksAround(net.minecraft.core.BlockPos center, int radius) {
            ServerLevel level = helper.getLevel();
            net.minecraft.world.level.ChunkPos middle = new net.minecraft.world.level.ChunkPos(center);
            List<net.minecraft.world.level.ChunkPos> held = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(middle.x + dx,
                            middle.z + dz);
                    level.getChunkSource().addRegionTicket(AREA_TICKET, pos, 2, pos);
                    held.add(pos);
                }
            }
            onClose(() -> held.forEach(c -> level.getChunkSource().removeRegionTicket(AREA_TICKET, c, 2, c)));
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

    private static final Field ATTACK_STRENGTH_TICKER;

    static {
        try {
            ATTACK_STRENGTH_TICKER = LivingEntity.class.getDeclaredField("attackStrengthTicker");
            ATTACK_STRENGTH_TICKER.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Makes the player's next swing a full-strength one: what is in their hand counts (the player ticks once, as a
     * connected client's would) and the swing has had all the time it needs to recover (a new player's has hardly
     * begun).
     */
    public static void chargeAttack(ServerPlayer player) {
        player.doTick();
        try {
            ATTACK_STRENGTH_TICKER.setInt(player, 1000);
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
        return soundsIn(sentPackets(player));
    }

    /** Every packet the server has sent this player since the last call to this or to {@link #heardSounds}, oldest first. */
    public static List<Object> sentPackets(ServerPlayer player) {
        EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
        // Nothing ticks the fake connection, which is what flushes it: a packet sent later would sit there unseen.
        channel.flush();
        List<Object> sent = new ArrayList<>();
        for (Object packet = channel.readOutbound(); packet != null; packet = channel.readOutbound()) {
            sent.add(packet);
        }
        return sent;
    }

    /** The sounds among {@code packets}. */
    public static List<HeardSound> soundsIn(List<Object> packets) {
        List<HeardSound> heard = new ArrayList<>();
        for (Object sent : packets) {
            if (sent instanceof ClientboundSoundPacket packet) {
                heard.add(new HeardSound(packet.getSound().value().getLocation(), new Vec3(packet.getX(), packet.getY(), packet.getZ())));
            }
        }
        return heard;
    }

    /** The particle bursts among {@code packets}. */
    public static List<ClientboundLevelParticlesPacket> particlesIn(List<Object> packets) {
        List<ClientboundLevelParticlesPacket> particles = new ArrayList<>();
        for (Object sent : packets) {
            if (sent instanceof ClientboundLevelParticlesPacket packet) {
                particles.add(packet);
            }
        }
        return particles;
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

    private TestKit() {
    }
}
