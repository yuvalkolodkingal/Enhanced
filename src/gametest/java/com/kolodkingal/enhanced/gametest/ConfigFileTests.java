package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.config.core.ConfigFile;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.config.core.ConfigSyncPayload;
import com.kolodkingal.enhanced.config.core.Configs;
import com.kolodkingal.enhanced.config.core.Toml;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.ConfigCompatTests.BATCH;
import static com.kolodkingal.enhanced.gametest.ConfigCompatTests.delete;
import static com.kolodkingal.enhanced.gametest.ConfigCompatTests.read;
import static com.kolodkingal.enhanced.gametest.ConfigCompatTests.tempDir;
import static com.kolodkingal.enhanced.gametest.ConfigCompatTests.write;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The mod's config files (port P0, lane L7): where they are read from (the world's {@code serverconfig/} first), the
 * backups, reading a file again when it changes on disk, and the server's file sent to a joining client.
 */
public final class ConfigFileTests {
    /** A small spec of its own for each test (the mod's are in use by this server; tests of a batch run at once). */
    private record TestSpec(ConfigSpec spec, ConfigSpec.IntValue range, ConfigSpec.BooleanValue on,
                            ConfigSpec.ConfigValue<List<? extends String>> names) {
        static TestSpec make() {
            ConfigSpec.Builder builder = new ConfigSpec.Builder();
            builder.comment("A section.").push("test");
            ConfigSpec.IntValue range = builder.comment("A number.").defineInRange("range", 5, 0, 10);
            ConfigSpec.BooleanValue on = builder.comment("A switch.").define("on", true);
            ConfigSpec.ConfigValue<List<? extends String>> names = builder.comment("Some names.")
                    .defineListAllowEmpty("names", List.of("a"), () -> "b", element -> element instanceof String);
            builder.pop();
            return new TestSpec(builder.build(), range, on, names);
        }
    }

    private ConfigFileTests() {
    }

    private static void writeValue(Path file, String key, String raw) {
        write(file, ConfigCompatCases.set(read(file), "test", key, raw));
    }

    /** The server's file is the world's own when there is one, else config/; the client's is not read here. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theServerReadsItsFilesAsNeoForgeDid(GameTestHelper helper) {
        check(EnhancedConfig.SPEC.isLoaded(), "the server's config is loaded while the server runs");
        check(!EnhancedClientConfig.SPEC.isLoaded(), "a dedicated server never reads the client's config");
        List<ConfigFile> server = Configs.of(ConfigFile.Type.SERVER);
        check(server.size() == 1 && server.get(0).spec() == EnhancedConfig.SPEC, "one server file: " + server);
        Path path = server.get(0).path();
        check(path != null && path.getFileName().toString().equals("enhanced-server.toml"), "read from " + path);
        Path world = Configs.serverConfigDir(helper.getLevel().getServer());
        check(Files.isRegularFile(world.resolve("readme.txt")), "the world's serverconfig folder has its readme");
        check(Configs.of(ConfigFile.Type.CLIENT).get(0).fileName().equals("enhanced-client.toml"), "the client's");

        TestSpec t = TestSpec.make();
        Path dir = tempDir();
        try {
            Path config = dir.resolve("config");
            Path override = dir.resolve("serverconfig");
            Files.createDirectories(override);
            ConfigFile file = new ConfigFile(ConfigFile.Type.SERVER, t.spec(), "test-server.toml");
            file.open(config, override);
            check(Files.isRegularFile(config.resolve("test-server.toml")), "with no world file, config/ is made");
            check(t.range().get() == 5, "the default is read");
            file.unload();
            check(!t.spec().isLoaded(), "unloaded");
            Files.copy(config.resolve("test-server.toml"), override.resolve("test-server.toml"));
            writeValue(override.resolve("test-server.toml"), "range", "7");
            file.open(config, override);
            check(override.resolve("test-server.toml").equals(file.path()), "the world's file wins: " + file.path());
            check(t.range().get() == 7, "its value is read: " + t.range().get());
            t.range().set(8);
            t.range().save();
            check(read(override.resolve("test-server.toml")).contains("\trange = 8"), "a save writes the world's file");
            check(read(config.resolve("test-server.toml")).contains("\trange = 5"), "and not config/");
            file.unload();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            delete(dir);
        }
        helper.succeed();
    }

    /** A wrong file is backed up before it is corrected; five backups at most, the newest is -1. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aWrongFileIsBackedUpFiveTimesAtMost(GameTestHelper helper) {
        TestSpec t = TestSpec.make();
        Path dir = tempDir();
        try {
            Path file = dir.resolve("test-server.toml");
            ConfigFile.read(file, t.spec(), "test-server.toml");
            String good = read(file);
            for (int i = 1; i <= 7; i++) {
                write(file, i % 2 == 0 ? "this is [ not toml " + i : good.replace("range = 5", "range = " + (10 + i)));
                Toml.Table table = ConfigFile.read(file, t.spec(), "test-server.toml");
                check(t.range().read(table) == (i % 2 == 0 ? 5 : 10), "corrected " + i + ": " + t.range().read(table));
            }
            for (int i = 1; i <= 5; i++) {
                check(Files.isRegularFile(dir.resolve("test-server-" + i + ".toml.bak")), "backup " + i);
            }
            check(!Files.exists(dir.resolve("test-server-6.toml.bak")), "no sixth backup");
            check(read(dir.resolve("test-server-1.toml.bak")).contains("range = 17"), "the newest is -1");
            check(read(dir.resolve("test-server-2.toml.bak")).contains("not toml 6"), "the one before is -2");
            check(read(file).equals(good.replace("range = 5", "range = 10")), "the file is corrected: clamped");
        } finally {
            delete(dir);
        }
        helper.succeed();
    }

    /** A file changed on disk is read again (half a second after the change), its values with it. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aFileChangedOnDiskIsReadAgain(GameTestHelper helper) {
        TestSpec t = TestSpec.make();
        Path dir = tempDir();
        ConfigFile file = new ConfigFile(ConfigFile.Type.CLIENT, t.spec(), "test-client.toml");
        file.open(dir, null);
        Path path = dir.resolve("test-client.toml");
        check(t.range().get() == 5 && t.on().get(), "the defaults");
        helper.runAfterDelay(5, () -> {
            writeValue(path, "range", "3");
            writeValue(path, "on", "false");
        });
        helper.succeedWhen(() -> {
            check(t.range().get() == 3 && !t.on().get(), "the new values are read: " + t.range().get() + ", "
                    + t.on().get());
            check(read(path).contains("\trange = 3"), "the file is left as written");
            file.unload();
            delete(dir);
        });
    }

    /** The server's file goes to a joining client whole; the client uses its values; its own world's are shared. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aClientUsesTheServersFile(GameTestHelper helper) {
        List<ConfigSyncPayload> payloads = Configs.syncPayloads();
        check(payloads.size() == 1 && payloads.get(0).fileName().equals("enhanced-server.toml"), "one file is sent");
        ConfigSyncPayload payload = payloads.get(0);
        Path path = Configs.of(ConfigFile.Type.SERVER).get(0).path();
        check(path != null && payload.contents().equals(read(path)), "as it is on the server's disk");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ConfigSyncPayload.STREAM_CODEC.encode(buffer, payload);
        ConfigSyncPayload received = ConfigSyncPayload.STREAM_CODEC.decode(buffer);
        buffer.release();
        check(received.equals(payload), "it crosses the wire unchanged (" + payload.contents().length() + " chars)");
        // this server's own client shares its values: the message changes nothing
        Configs.receiveSynced(received, true);
        check(path.equals(Configs.of(ConfigFile.Type.SERVER).get(0).path()), "the server's file stays in use");
        // a client of another server: the values are the server's, with no file
        TestSpec t = TestSpec.make();
        Path dir = tempDir();
        try {
            Path serverFile = dir.resolve("test-server.toml");
            ConfigFile.read(serverFile, t.spec(), "test-server.toml");
            writeValue(serverFile, "range", "9");
            writeValue(serverFile, "names", "[\"x\", \"y\"]");
            ConfigFile client = new ConfigFile(ConfigFile.Type.SERVER, t.spec(), "test-server.toml");
            client.acceptSynced(read(serverFile));
            check(t.spec().isLoaded() && client.path() == null, "loaded, with no file");
            check(t.range().get() == 9 && t.names().get().equals(List.of("x", "y")), "the server's values: "
                    + t.range().get() + " " + t.names().get());
            t.range().set(1);
            t.spec().save();
            check(read(serverFile).contains("\trange = 9"), "a client never writes the server's file");
            client.unload();
        } catch (Toml.ParseException e) {
            throw new IllegalStateException(e.getMessage());
        } finally {
            delete(dir);
        }
        helper.succeed();
    }
}
