package com.kolodkingal.enhanced.platform.event;

/**
 * When a listener runs, relative to the other listeners of the same event: NeoForge's five priorities, in the order
 * they run. {@link EventHub#post} runs them from {@link #HIGHEST} to {@link #LOWEST}; on NeoForge the bridge runs each
 * one from a native listener of the same priority, so the mod's listeners keep their place among other mods' listeners.
 */
public enum Priority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST
}
