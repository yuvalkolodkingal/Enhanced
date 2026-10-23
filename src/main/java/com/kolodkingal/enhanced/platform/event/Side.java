package com.kolodkingal.enhanced.platform.event;

/**
 * Where a {@link Subscriber} class is registered: on both physical sides, only on the physical client (its events and
 * code may use client classes), or only on a dedicated server. As NeoForge's {@code @EventBusSubscriber(value = ...)}.
 */
public enum Side {
    BOTH,
    CLIENT,
    SERVER
}
