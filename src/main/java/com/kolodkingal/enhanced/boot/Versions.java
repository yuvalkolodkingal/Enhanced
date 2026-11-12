package com.kolodkingal.enhanced.boot;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which copy of the mod runs (docs/MULTIVERSION.md). The jar holds one complete copy of the mod per Minecraft
 * version it supports, each compiled against that version and in a package of its own; only the copy of the running
 * version is ever loaded. This class and the rest of {@code boot} use nothing that differs between those versions.
 */
public final class Versions {
    /** One supported version: the Minecraft versions it covers and the package of its copy. */
    record Copy(List<String> minecraft, String pkg) {
    }

    /**
     * The copies in this jar. The first is the mod as written (its sources are the repository's own); each other one
     * is made by the build from the same sources for its version (versions/&lt;mc&gt;).
     */
    static final List<Copy> COPIES = List.of(
            new Copy(List.of("1.21.1"), "com.kolodkingal.enhanced"),
            new Copy(List.of("1.21.4"), "com.kolodkingal.enhanced.v1_21_4"),
            new Copy(List.of("1.21.11"), "com.kolodkingal.enhanced.v1_21_11"));

    private static String running;

    private Versions() {
    }

    /** The running Minecraft version ("1.21.1"), read from the game's own version.json. */
    public static synchronized String minecraft() {
        if (running == null) {
            running = read();
        }
        return running;
    }

    private static String read() {
        // FML knows it first: FMLLoader.versionInfo().mcVersion() (FML 4), FMLLoader.getCurrent().getVersionInfo()
        // .mcVersion() (FML 10); reflection: not every FML has either
        try {
            Class<?> loader = Class.forName("net.neoforged.fml.loading.FMLLoader");
            Object info;
            try {
                info = loader.getMethod("versionInfo").invoke(null);
            } catch (NoSuchMethodException e) {
                Object current = loader.getMethod("getCurrent").invoke(null);
                info = current.getClass().getMethod("getVersionInfo").invoke(current);
            }
            Object mc = info.getClass().getMethod("mcVersion").invoke(info);
            if (mc instanceof String s && !s.isEmpty()) {
                return s;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // fall back to the game's version.json
        }
        try (InputStream in = Versions.class.getClassLoader().getResourceAsStream("version.json")) {
            if (in != null) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (java.io.IOException ignored) {
            // unknown
        }
        return "unknown";
    }

    /** The package of the copy for the running version, or null if this jar has none. */
    public static String packageForRunning() {
        String mc = minecraft();
        for (Copy copy : COPIES) {
            if (copy.minecraft().contains(mc) && present(copy)) {
                return copy.pkg();
            }
        }
        return null;
    }

    /** Whether the jar holds this copy (a build may leave out a copy that is still being ported). */
    private static boolean present(Copy copy) {
        return Versions.class.getClassLoader().getResource(copy.pkg().replace('.', '/') + "/Enhanced.class") != null;
    }

    /** The versions this jar supports, for the message when the running one is not among them. */
    public static String supported() {
        StringBuilder out = new StringBuilder();
        for (Copy copy : COPIES) {
            if (!present(copy)) {
                continue;
            }
            for (String mc : copy.minecraft()) {
                out.append(out.isEmpty() ? "" : ", ").append(mc);
            }
        }
        return out.toString();
    }
}
