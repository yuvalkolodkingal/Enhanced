package com.kolodkingal.enhanced.platform.attach;

/** Which players a synced attachment of a player (or level) is sent to. */
public enum SyncTo {
    /** Everyone who can see the holder (and the holder himself). */
    ALL,
    /** Only the player who holds it. */
    SELF
}
