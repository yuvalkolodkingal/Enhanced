package com.kolodkingal.enhanced.asgard.npc;

import java.util.Locale;

/**
 * The roles of the Asgardians (3.5), the one entity {@code enhanced:asgardian}: the eight named gods and warriors (one
 * each in the city, the "fixtures"), the Einherjar guards and the citizens. Written by the 3.5 skeleton (the lanes
 * summon and place them by role); lane L5 owns it. Saved by name: append only.
 */
public enum Role {
    HEIMDALL,
    ODIN,
    FRIGGA,
    SIF,
    VALKYRIE,
    VOLSTAGG,
    FANDRAL,
    HOGUN,
    EINHERJAR,
    CITIZEN;

    /** Whether there is exactly one of this role in the city (the eight named ones). */
    public boolean named() {
        return this != EINHERJAR && this != CITIZEN;
    }

    /** The id in lang keys and slots: {@code heimdall}, {@code einherjar}... */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
