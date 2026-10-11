package com.kolodkingal.enhanced.registry;

import net.minecraft.server.level.TicketType;

import java.util.UUID;

/**
 * The chunk tickets the boss fights hold on their arenas while they run, one type each, keyed by the fight's id: the
 * chunks stay loaded and ticking, and the ticket is removed when the fight ends. (They have no expiry of their own.)
 */
public final class ModTicketTypes {
    public static final TicketType<UUID> ENHANCED_ULTRON = TicketType.create("enhanced_ultron", UUID::compareTo);
    public static final TicketType<UUID> ENHANCED_THANOS = TicketType.create("enhanced_thanos", UUID::compareTo);
    public static final TicketType<UUID> ENHANCED_APOCALYPSE = TicketType.create("enhanced_apocalypse", UUID::compareTo);

    private ModTicketTypes() {
    }
}
