package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.asgard.npc.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The one fixed plan of the city of Asgard (3.5): pure data, the same in every world. x is east, z south; the palace is
 * to the north (z -160 to 40), the Golden Road runs south to the bridge head (z 200), the Rainbow Bridge on to
 * Heimdall's Observatory over the void (centre (0, 73, 440)). See section 2.2 of the plan.
 *
 * <p>The 3.5 skeleton writes the <b>fixed points</b> (the constants below, which the tests and every lane use) with the
 * plan's values, and placeholder lists for the data the city's builders fill ({@link #homes}, {@link #patrols}, {@link
 * #seats}, {@link #hidingSpots}): lane L1 owns this class and replaces them; a lane that moves a fixed point notes it
 * for the others.
 */
public final class CityPlan {
    /** The version of the plan (kept in {@code AsgardState}): a later version only affects chunks not yet generated. */
    public static final int VERSION = 1;

    // ---- fixed points ----

    /** Where the Bifrost sets a traveller down in Asgard: the landing platform of the Observatory. */
    public static final Vec3 ARRIVAL = new Vec3(0.5, 74.0, 440.5);
    /** Facing the city (north). */
    public static final float ARRIVAL_YAW = 180.0F;
    /** The Hofund socket, in the middle of the Observatory. */
    public static final BlockPos OBSERVATORY_SOCKET = new BlockPos(0, 74, 440);
    /** The centre of the Observatory's dome (radius {@link #OBSERVATORY_RADIUS}), over the void. */
    public static final BlockPos OBSERVATORY_CENTER = new BlockPos(0, 73, 440);
    public static final int OBSERVATORY_RADIUS = 13;
    /** The seat block of the Throne of Asgard (the dais is z -132 to -122). */
    public static final BlockPos THRONE = new BlockPos(0, 86, -128);
    /** Hela's Seal, behind the throne on the north wall. */
    public static final BlockPos HELA_SEAL = new BlockPos(0, 88, -136);
    /** The middle of the throne room's floor: the centre of Hela's arena. */
    public static final BlockPos THRONE_ROOM_CENTER = new BlockPos(0, 82, -100);
    /** The throne room: x -16 to 16, z -136 to -64, floor 82, ceiling 108. */
    public static final BoundingBox THRONE_ROOM = new BoundingBox(-16, 82, -136, 16, 108, -64);
    /** The floor and the ceiling of the throne room. */
    public static final int THRONE_ROOM_FLOOR = 82;
    public static final int THRONE_ROOM_CEILING = 108;
    /** Odin's vault, below the throne room (floor 70). */
    public static final BoundingBox VAULT = new BoundingBox(-10, 70, -126, 10, 78, -106);
    /** The door of the vault (its south wall), opened when Odin wakes. */
    public static final BoundingBox VAULT_DOOR = new BoundingBox(-1, 71, -106, 1, 73, -106);
    /** Gungnir's stand in the vault. */
    public static final BlockPos GUNGNIR_STAND = new BlockPos(0, 71, -122);
    /** Odin's chamber in the east wing: the bed of the Odinsleep. */
    public static final BlockPos ODIN_BED = new BlockPos(34, 83, -110);
    /** The training ring: centre and radius. */
    public static final BlockPos TRAINING_RING = new BlockPos(95, 70, 0);
    public static final int TRAINING_RING_RADIUS = 16;
    /** Frigga's garden. */
    public static final BlockPos FRIGGA_GARDEN = new BlockPos(-100, 74, -60);
    /** The Hall of Feasts. */
    public static final BlockPos FEAST_HALL = new BlockPos(90, 68, 92);
    /** The stables (Valkyrie). */
    public static final BlockPos STABLES = new BlockPos(-170, 66, 170);
    /** The bridge head, where the Golden Road meets the Rainbow Bridge (the gate). */
    public static final BlockPos BRIDGE_HEAD = new BlockPos(0, 72, 200);
    /** The Rainbow Bridge: x -3 to 3 at y 72, z 200 to 428. */
    public static final int BRIDGE_Y = 72;
    public static final int BRIDGE_START_Z = 200;
    public static final int BRIDGE_END_Z = 428;
    /** The land (r ≤ 235), the sea (to r 380, water to y 62) and the edge of the disk. */
    public static final int LAND_RADIUS = 235;
    public static final int SEA_RADIUS = 380;
    public static final int SEA_LEVEL = 62;

    // ---- the city's people and places (placeholders until lane L1's plan) ----

    /**
     * The home of an Asgardian: the role, its slot number among those of its role (0 for the named ones), where it
     * lives and how far it strolls.
     */
    public record Home(Role role, int index, BlockPos pos, int radius) {
        /** The slot of the population keeper: {@code FRIGGA}, {@code EINHERJAR#3}... */
        public String slot() {
            return role.named() ? role.name() : role.name() + "#" + index;
        }
    }

    /** How many Einherjar and citizens live in the city. */
    public static final int EINHERJAR_COUNT = 12;
    public static final int CITIZEN_COUNT = 14;

    private static final List<Home> HOMES;
    private static final List<List<BlockPos>> PATROLS;
    private static final List<BlockPos> SEATS;
    private static final List<BlockPos> HIDING_SPOTS;

    static {
        List<Home> homes = new ArrayList<>();
        homes.add(new Home(Role.HEIMDALL, 0, OBSERVATORY_SOCKET.south(4), 6));
        homes.add(new Home(Role.ODIN, 0, ODIN_BED, 10));
        homes.add(new Home(Role.FRIGGA, 0, FRIGGA_GARDEN, 10));
        homes.add(new Home(Role.SIF, 0, TRAINING_RING.offset(-4, 0, 0), 10));
        homes.add(new Home(Role.VALKYRIE, 0, STABLES, 10));
        homes.add(new Home(Role.VOLSTAGG, 0, FEAST_HALL, 10));
        homes.add(new Home(Role.FANDRAL, 0, TRAINING_RING.offset(4, 0, 0), 10));
        homes.add(new Home(Role.HOGUN, 0, TRAINING_RING.offset(0, 0, 4), 10));
        for (int i = 0; i < EINHERJAR_COUNT; i++) {
            // Along the Golden Road and at the bridge gate (lane L1 puts them on their patrols).
            homes.add(new Home(Role.EINHERJAR, i, new BlockPos(i % 2 == 0 ? -5 : 5, 72, 40 + 14 * i), 16));
        }
        for (int i = 0; i < CITIZEN_COUNT; i++) {
            // In the houses and halls west of the road and round the Hall of Feasts.
            homes.add(new Home(Role.CITIZEN, i, i < 8 ? new BlockPos(-60 - 18 * i, 70, 60 + 14 * i) : new BlockPos(60 + 9 * i, 68, 70 + 6 * i), 16));
        }
        HOMES = Collections.unmodifiableList(homes);
        List<List<BlockPos>> patrols = new ArrayList<>();
        patrols.add(List.of(new BlockPos(-5, 72, 40), new BlockPos(-5, 72, 196), new BlockPos(5, 72, 196), new BlockPos(5, 72, 40)));
        patrols.add(List.of(new BlockPos(-14, 82, -70), new BlockPos(14, 82, -70)));
        PATROLS = Collections.unmodifiableList(patrols);
        SEATS = List.of(THRONE, FRIGGA_GARDEN.east(3), FEAST_HALL.west(4), FEAST_HALL.east(4));
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            int r = 60 + 50 * (i % 3);
            spots.add(new BlockPos((int) Math.round(Math.cos(angle) * r), 72, (int) Math.round(Math.sin(angle) * r)));
        }
        HIDING_SPOTS = Collections.unmodifiableList(spots);
    }

    /** Every Asgardian's home, the named ones first (one slot each). */
    public static List<Home> homes() {
        return HOMES;
    }

    /** The waypoint loops of the Einherjar on patrol. */
    public static List<List<BlockPos>> patrols() {
        return PATROLS;
    }

    /** Where Asgardians (and players) can sit: benches and the throne. */
    public static List<BlockPos> seats() {
        return SEATS;
    }

    /** The 24 places in the city where the Shadows of Hela of the Guardian's Watch can hide. */
    public static List<BlockPos> hidingSpots() {
        return HIDING_SPOTS;
    }

    private CityPlan() {
    }
}
