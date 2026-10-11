package com.kolodkingal.enhanced.boss;

/** Why boss fights are being ended from outside (a fight ends by itself when it is won, lost or abandoned). */
public enum StopReason {
    /** An operator ended it with {@code /enhanced boss stop}. */
    COMMAND,
    /** The server is stopping: no fight survives a restart. */
    SERVER_STOPPING
}
