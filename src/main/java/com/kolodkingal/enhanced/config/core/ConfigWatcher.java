package com.kolodkingal.enhanced.config.core;

import com.kolodkingal.enhanced.Enhanced;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Watches the loaded config files (port P0, lane L7): a file changed on disk (by a player's editor, or a server
 * owner) is read again half a second after its last change, on the watcher's own thread, as NeoForge's watcher did.
 * {@code -Denhanced.config.noWatch=true} turns it off.
 */
final class ConfigWatcher {
    private static final long DEBOUNCE_MILLIS = 500;
    private static final Map<Path, Consumer<Path>> FILES = new ConcurrentHashMap<>();
    private static final Map<Path, WatchKey> DIRECTORIES = new HashMap<>();
    private static WatchService service;
    private static ScheduledExecutorService debouncer;
    private static final Map<Path, ScheduledFuture<?>> PENDING = new ConcurrentHashMap<>();

    private ConfigWatcher() {
    }

    /** Calls {@code onChange} (with the file) after {@code file} changes, until {@link #unwatch}. */
    static synchronized void watch(Path file, Consumer<Path> onChange) {
        if (Boolean.getBoolean("enhanced.config.noWatch")) {
            return;
        }
        Path absolute = file.toAbsolutePath().normalize();
        Path dir = absolute.getParent();
        try {
            start();
            if (!DIRECTORIES.containsKey(dir)) {
                DIRECTORIES.put(dir, dir.register(service, StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE));
            }
            FILES.put(absolute, onChange);
        } catch (IOException | RuntimeException e) {
            Enhanced.LOGGER.warn("Cannot watch config file {} for changes", file, e);
        }
    }

    static synchronized void unwatch(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        FILES.remove(absolute);
        ScheduledFuture<?> pending = PENDING.remove(absolute);
        if (pending != null) {
            pending.cancel(false);
        }
        Path dir = absolute.getParent();
        if (FILES.keySet().stream().noneMatch(watched -> watched.getParent().equals(dir))) {
            WatchKey key = DIRECTORIES.remove(dir);
            if (key != null) {
                key.cancel();
            }
        }
    }

    private static void start() throws IOException {
        if (service != null) {
            return;
        }
        service = FileSystems.getDefault().newWatchService();
        debouncer = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Enhanced config reloader");
            thread.setDaemon(true);
            return thread;
        });
        WatchService watching = service;
        Thread thread = new Thread(() -> run(watching), "Enhanced config watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run(WatchService watching) {
        while (true) {
            WatchKey key;
            try {
                key = watching.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }
            if (key.watchable() instanceof Path dir) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context() instanceof Path name) {
                        changed(dir.resolve(name).toAbsolutePath().normalize());
                    }
                }
            }
            key.reset();
        }
    }

    private static void changed(Path file) {
        Consumer<Path> onChange = FILES.get(file);
        if (onChange == null) {
            return;
        }
        PENDING.compute(file, (path, pending) -> {
            if (pending != null) {
                pending.cancel(false);
            }
            return debouncer.schedule(() -> {
                PENDING.remove(path);
                try {
                    onChange.accept(path);
                } catch (RuntimeException e) {
                    Enhanced.LOGGER.error("Failed to reload config file {}", path, e);
                }
            }, DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
        });
    }
}
