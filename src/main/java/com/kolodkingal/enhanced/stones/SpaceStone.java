package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The Space Stone's journeys between the realms (3.6): the one exception to "only the Bifrost reaches Asgard". Every
 * teleport of the Realm Picker (lane L2, {@code SpaceRealms}) runs inside {@link #moving}, which marks the traveller
 * for as long as the teleport runs (as {@code Bifrost.isMoving}), so {@code AsgardGate} lets it through, unless {@code
 * [space_stone] reachesAsgard} is off. Server thread only.
 */
public final class SpaceStone {
    /** The entities the Space Stone is moving right now. */
    private static final Set<UUID> MOVING = new HashSet<>();

    /** Whether the Space Stone is moving this entity right now (the gate of Asgard asks). */
    public static boolean isMoving(Entity entity) {
        return MOVING.contains(entity.getUUID());
    }

    /** Runs {@code teleport} with {@code traveller} marked as the Space Stone's, and returns what it returned. */
    public static <T> T moving(Entity traveller, Supplier<T> teleport) {
        UUID id = traveller.getUUID();
        boolean already = !MOVING.add(id);
        try {
            return teleport.get();
        } finally {
            if (!already) {
                MOVING.remove(id);
            }
        }
    }

    /** Whether the Space Stone may reach Asgard at all ({@code [space_stone] reachesAsgard}). */
    public static boolean reachesAsgard() {
        return !EnhancedConfig.SPEC.isLoaded() || StonesConfig.SPACE_REACHES_ASGARD.get();
    }

    private SpaceStone() {
    }
}
