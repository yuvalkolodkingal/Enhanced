package com.kolodkingal.enhanced.platform.event.client;


import java.util.concurrent.CompletableFuture;

/** The mod's client setup (NeoForge's mod bus). */
public class FMLClientSetupEvent {
    public FMLClientSetupEvent() {
    }

    /**
     * Runs work on the main thread after the setup (NeoForge runs setup listeners in parallel);
     * without a loader queue it runs now.
     */
    public CompletableFuture<Void> enqueueWork(Runnable work) {
        work.run();
        return CompletableFuture.completedFuture(null);
    }
}
