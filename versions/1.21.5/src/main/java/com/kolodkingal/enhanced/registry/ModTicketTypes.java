package com.kolodkingal.enhanced.registry;

import net.minecraft.server.level.TicketType;

/**
 * 1.21.5's tickets (the 1.21.1 file says what each is for): a ticket type is a record with no key, so the tickets of one
 * type on one chunk are counted rather than told apart; each kind loads and ticks its chunks and never times out.
 */
public final class ModTicketTypes {
    public static final TicketType ENHANCED_ULTRON = holding();
    public static final TicketType ENHANCED_THANOS = holding();
    public static final TicketType ENHANCED_APOCALYPSE = holding();
    public static final TicketType ENHANCED_BUILD = holding();
    public static final TicketType ENHANCED_ROAD = holding();
    public static final TicketType ENHANCED_HELA = holding();
    public static final TicketType ENHANCED_ASGARD = holding();
    public static final TicketType ENHANCED_SLING = holding();

    private static TicketType holding() {
        return new TicketType(TicketType.NO_TIMEOUT, false, TicketType.TicketUse.LOADING_AND_SIMULATION);
    }

    private ModTicketTypes() {
    }
}
