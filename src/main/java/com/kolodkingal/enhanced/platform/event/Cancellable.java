package com.kolodkingal.enhanced.platform.event;

/**
 * A common event that a listener can cancel, as NeoForge's {@code ICancellableEvent}: once cancelled, the listeners
 * after it are skipped unless they {@link Subscribe#receiveCanceled receive cancelled events}, and whoever posted it
 * reads {@link #isCanceled} to know. On NeoForge both methods read and write the native event.
 */
public interface Cancellable {
    boolean isCanceled();

    void setCanceled(boolean canceled);
}
