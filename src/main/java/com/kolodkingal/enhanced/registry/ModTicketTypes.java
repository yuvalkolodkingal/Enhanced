package com.kolodkingal.enhanced.registry;

import net.minecraft.server.level.TicketType;

import java.util.UUID;

/**
 * The chunk tickets the boss fights hold on their arenas while they run, one type each, keyed by the fight's id: the
 * chunks stay loaded and ticking, and the ticket is removed when the fight ends. (They have no expiry of their own.)
 *
 * <p>And the tickets of a Build Magic spell at work ({@link #ENHANCED_BUILD}), keyed by the spell's number.
 */
public final class ModTicketTypes {
    public static final TicketType<UUID> ENHANCED_ULTRON = TicketType.create("enhanced_ultron", UUID::compareTo);
    public static final TicketType<UUID> ENHANCED_THANOS = TicketType.create("enhanced_thanos", UUID::compareTo);
    public static final TicketType<UUID> ENHANCED_APOCALYPSE = TicketType.create("enhanced_apocalypse", UUID::compareTo);
    /**
     * A Build Magic spell at work (D-B 11.7): distance 1, on the next chunks of its work order (at most
     * {@code build_magic.maxTicketChunks} at once), so it finishes when its caster flies away or logs out. Only ever on
     * chunks that were loaded when it was cast, so nothing is ever generated for it.
     */
    public static final TicketType<Long> ENHANCED_BUILD = TicketType.create("enhanced_build", Long::compareTo);

    /**
     * The chunks of a Witches' Road trial (3.2), keyed by the player it is built for: held from the moment the cell is planned
     * until it has been cleared, so the trial is loaded and ticking exactly while it exists.
     */
    public static final TicketType<UUID> ENHANCED_ROAD = TicketType.create("enhanced_road", UUID::compareTo);

    // ===== asgard (3.5) =====
    /** The throne room of Asgard while Hela's fight runs (as the other bosses' arenas). */
    public static final TicketType<UUID> ENHANCED_HELA = TicketType.create("enhanced_hela", UUID::compareTo);
    /**
     * Chunks of Asgard held for a while by its own code, keyed by whom they are for: a player's Guardian's Watch (lane L2),
     * the population keeper's homes (lane L5) and the tests that generate Asgard's chunks. Released explicitly.
     */
    public static final TicketType<UUID> ENHANCED_ASGARD = TicketType.create("enhanced_asgard", UUID::compareTo);

    private ModTicketTypes() {
    }
}
