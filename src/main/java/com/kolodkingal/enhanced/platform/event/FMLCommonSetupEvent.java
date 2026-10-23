package com.kolodkingal.enhanced.platform.event;


import java.util.concurrent.CompletableFuture;

/** The mod's common setup, once registries are complete (NeoForge's mod bus). */
public class FMLCommonSetupEvent {
    public FMLCommonSetupEvent() {
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
