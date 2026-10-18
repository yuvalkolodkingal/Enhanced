package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.npc.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one fixed plan of the city of Asgard (3.5): pure data, the same in every world. x is east, z south; the palace is
 * to the north (z -172 to -16), the Golden Road runs south to the bridge head (z 200), the Rainbow Bridge on to
 * Heimdall's Observatory over the void (centre (0, 73, 440)). See section 2.2 of the plan.
 *
 * <p>The plan is made of <b>pads</b> (the districts, levelled at fixed heights: {@link #pads}), <b>pieces</b> (the
 * placements of the city's 56 templates, {@code data/enhanced/structure/asgard/*.nbt}: {@link #pieces}), the procedural
 * builders of {@code asgard/build} and the fixed points below, which the tests and every lane use. Lane L1 moved one
 * fixed point of the skeleton: {@link #ARRIVAL} is on the Observatory's floor north of the socket (the socket itself is a
 * block in the middle).
 */
public final class CityPlan {
    /** The version of the plan (kept in {@code AsgardState}): a later version only affects chunks not yet generated. */
    public static final int VERSION = 1;

    // ---- fixed points ----

    /**
     * Where the Bifrost sets a traveller down in Asgard: the floor of the Observatory, between its door (north, to the
     * bridge) and the Hofund socket in its middle. (The skeleton had it on the socket; lane L1 moved it 8 blocks north.)
     */
    public static final Vec3 ARRIVAL = new Vec3(0.5, 74.0, 432.5);
    /** Facing the city (north). */
    public static final float ARRIVAL_YAW = 180.0F;
    /** The Hofund socket, in the middle of the Observatory (the Bifrost mechanism is the floor block under it). */
    public static final BlockPos OBSERVATORY_SOCKET = new BlockPos(0, 74, 440);
    /** The centre of the Observatory's dome (radius {@link #OBSERVATORY_RADIUS}), over the void. */
    public static final BlockPos OBSERVATORY_CENTER = new BlockPos(0, 73, 440);
    public static final int OBSERVATORY_RADIUS = 13;
    /** The Bifrost mechanism (its ring turns round the socket): the floor block under the socket. */
    public static final BlockPos MECHANISM = OBSERVATORY_SOCKET.below();
    /** The radius of the circle of runes (bridge crystal) in the Observatory's floor round the socket. */
    public static final double RUNE_CIRCLE_RADIUS = 5.0;
    /** The seat block of the Throne of Asgard (the dais is z -135 to -122), facing the hall (south). */
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
    /** The door of the vault (its south wall: solid gold), opened when Odin wakes ({@link CityBuild#setVaultDoor}). */
    public static final BoundingBox VAULT_DOOR = new BoundingBox(-1, 71, -106, 1, 73, -106);
    /** Gungnir's stand in the vault. */
    public static final BlockPos GUNGNIR_STAND = new BlockPos(0, 71, -122);
    /** Odin's chamber in the east wing: the bed of the Odinsleep (its foot; the head is one block north). */
    public static final BlockPos ODIN_BED = new BlockPos(34, 83, -110);
    /** The training ring: centre and radius. */
    public static final BlockPos TRAINING_RING = new BlockPos(95, 70, 0);
    public static final int TRAINING_RING_RADIUS = 16;
    /** Frigga's garden (her pavilion's middle). */
    public static final BlockPos FRIGGA_GARDEN = new BlockPos(-100, 74, -60);
    /** The Hall of Feasts. */
    public static final BlockPos FEAST_HALL = new BlockPos(90, 68, 92);
    /** The stables (Valkyrie). */
    public static final BlockPos STABLES = new BlockPos(-170, 66, 170);
    /** The bridge head, where the Golden Road meets the Rainbow Bridge (the gate). */
    public static final BlockPos BRIDGE_HEAD = new BlockPos(0, 72, 200);
    /** The Rainbow Bridge: x -3 to 3 at y 72, z 200 to 428 (it reaches the Observatory's door). */
    public static final int BRIDGE_Y = 72;
    public static final int BRIDGE_START_Z = 200;
    public static final int BRIDGE_END_Z = 428;
    public static final int BRIDGE_HALF_WIDTH = 3;
    /** The land (r ≤ 235, more in two directions: {@code AsgardTerrain.edgeRadius}), the sea (to r 380, water to y 62). */
    public static final int LAND_RADIUS = 235;
    public static final int SEA_RADIUS = 380;
    public static final int SEA_LEVEL = 62;
    /** The Golden Road: paved x -6 to 6 from the courtyard's foot (z -16) to the bridge head; its pads are x -17 to 17. */
    public static final int ROAD_HALF_WIDTH = 6;
    public static final int ROAD_PAD_HALF_WIDTH = 17;
    public static final int ROAD_START_Z = -16;
    public static final int ROAD_END_Z = 206;

    // ---- the pads: the districts, levelled ----

    /** What a pad's ground is made of. */
    public enum Surface {
        /** Polished realm stone in squares of bricks (the palace, the courtyard, the road, the feast hall...). */
        PAVED,
        /** Grass (the gardens, the houses' district). */
        GRASS,
        /** Sand-coloured stone (the training grounds). */
        SAND,
        /** Stone bricks (the docks' quay). */
        QUAY
    }

    /**
     * A district levelled at {@code top} (its top block; one stands at {@code top + 1}): a rectangle x0..x1, z0..z1, or a
     * circle ({@code round}: centre (x0, z0), radius x1). {@code protect}: the city's protection covers it.
     */
    public record Pad(String name, int x0, int z0, int x1, int z1, boolean round, int top, Surface surface, boolean protect) {
        public boolean contains(int x, int z) {
            if (round) {
                double dx = x + 0.5 - x0;
                double dz = z + 0.5 - z0;
                return dx * dx + dz * dz <= (double) x1 * x1;
            }
            return x >= x0 && x <= x1 && z >= z0 && z <= z1;
        }

        /** The box round the pad's columns, from 8 below its top to the sky. */
        public BoundingBox bounds() {
            return round ? new BoundingBox(x0 - x1, top - 8, z0 - x1, x0 + x1, 319, z0 + x1) : new BoundingBox(x0, top - 8, z0, x1, 319, z1);
        }
    }

    private static Pad rect(String name, int x0, int z0, int x1, int z1, int top, Surface surface) {
        return new Pad(name, x0, z0, x1, z1, false, top, surface, true);
    }

    private static Pad circle(String name, int cx, int cz, int r, int top, Surface surface) {
        return new Pad(name, cx, cz, r, 0, true, top, surface, true);
    }

    /** The pads in order: where two overlap, the first wins. */
    private static final List<Pad> PADS = List.of(
            rect("palace", -70, -172, 70, -64, 81, Surface.PAVED),
            rect("courtyard", -46, -63, 46, -17, 79, Surface.PAVED),
            rect("plaza", -26, -16, 26, 39, 77, Surface.PAVED),
            rect("road_1", -ROAD_PAD_HALF_WIDTH, 40, ROAD_PAD_HALF_WIDTH, 79, 76, Surface.PAVED),
            rect("road_2", -ROAD_PAD_HALF_WIDTH, 80, ROAD_PAD_HALF_WIDTH, 119, 75, Surface.PAVED),
            rect("road_3", -ROAD_PAD_HALF_WIDTH, 120, ROAD_PAD_HALF_WIDTH, 159, 74, Surface.PAVED),
            rect("road_4", -ROAD_PAD_HALF_WIDTH, 160, ROAD_PAD_HALF_WIDTH, ROAD_END_Z, 72, Surface.PAVED),
            circle("gardens", -100, -60, 48, 73, Surface.GRASS),
            circle("training", 95, 0, 36, 69, Surface.SAND),
            rect("feast", 52, 56, 128, 128, 67, Surface.PAVED),
            rect("barracks", 118, 130, 180, 190, 67, Surface.PAVED),
            rect("east", 18, 44, 50, 196, 71, Surface.GRASS),
            rect("houses", -150, 44, -18, 196, 70, Surface.GRASS),
            rect("houses_low", -205, 44, -151, 140, 67, Surface.GRASS),
            rect("stables", -198, 144, -146, 198, 65, Surface.GRASS),
            rect("docks", -226, -26, -196, 66, 63, Surface.QUAY));

    /** Every pad (the districts). */
    public static List<Pad> pads() {
        return PADS;
    }

    /** The pad (district) at (x, z), or null for the wild land. */
    @Nullable
    public static Pad padAt(int x, int z) {
        for (Pad pad : PADS) {
            if (pad.contains(x, z)) {
                return pad;
            }
        }
        return null;
    }

    // ---- the templates and the pieces ----

    /** The size of every template of the city (x, y, z, unturned), as its Python script made it. */
    public static final Map<String, Vec3i> TEMPLATE_SIZES;

    static {
        Object[] t = {
                "arch_of_victory", new Vec3i(31, 20, 5), "barracks_hall", new Vec3i(31, 14, 15), "barracks_yard", new Vec3i(21, 4, 21),
                "bathhouse", new Vec3i(15, 9, 15), "bridge_gate", new Vec3i(29, 26, 11), "docks", new Vec3i(13, 20, 61),
                "feast_hall", new Vec3i(41, 20, 31), "feast_kitchen", new Vec3i(15, 10, 13), "forge", new Vec3i(11, 10, 11),
                "garden_bridge", new Vec3i(5, 4, 9), "garden_fountain", new Vec3i(9, 7, 9), "granary", new Vec3i(9, 13, 9),
                "hall_craft", new Vec3i(21, 22, 27), "hall_healing", new Vec3i(21, 22, 27), "hall_scribes", new Vec3i(21, 22, 27),
                "hall_valor", new Vec3i(21, 22, 27), "house_1", new Vec3i(9, 15, 12), "house_10", new Vec3i(11, 15, 12),
                "house_2", new Vec3i(11, 20, 9), "house_3", new Vec3i(9, 15, 11), "house_4", new Vec3i(13, 20, 14),
                "house_5", new Vec3i(11, 20, 13), "house_6", new Vec3i(9, 15, 12), "house_7", new Vec3i(13, 20, 11),
                "house_8", new Vec3i(11, 15, 14), "house_9", new Vec3i(9, 20, 13), "market_stall_1", new Vec3i(7, 6, 5),
                "market_stall_2", new Vec3i(7, 6, 5), "market_stall_3", new Vec3i(7, 6, 5), "observatory", new Vec3i(27, 17, 27),
                "odin_chamber", new Vec3i(21, 18, 21), "palace_library", new Vec3i(21, 18, 21), "palace_steps", new Vec3i(21, 3, 4),
                "palace_wing_east", new Vec3i(35, 29, 27), "palace_wing_west", new Vec3i(35, 29, 27), "pavilion_long", new Vec3i(9, 8, 17),
                "pavilion_round", new Vec3i(15, 14, 15), "plaza_fountain", new Vec3i(13, 9, 13), "scribes_tower", new Vec3i(9, 30, 9),
                "shrine", new Vec3i(7, 10, 7), "skiff_large", new Vec3i(7, 12, 17), "skiff_small", new Vec3i(5, 4, 11),
                "sparring_pavilion", new Vec3i(9, 8, 9), "stables", new Vec3i(25, 13, 31), "statue_1", new Vec3i(5, 15, 5),
                "statue_2", new Vec3i(5, 15, 5), "statue_3", new Vec3i(5, 15, 5), "statue_4", new Vec3i(5, 15, 5),
                "throne_room_north", new Vec3i(33, 28, 36), "throne_room_south", new Vec3i(33, 28, 37), "training_ring", new Vec3i(37, 7, 37),
                "vault", new Vec3i(21, 9, 21), "vault_stair", new Vec3i(18, 11, 56), "watchtower", new Vec3i(7, 24, 7),
                "weapon_rack", new Vec3i(5, 4, 2), "well", new Vec3i(5, 6, 5)};
        Map<String, Vec3i> sizes = new LinkedHashMap<>();
        for (int i = 0; i < t.length; i += 2) {
            sizes.put((String) t[i], (Vec3i) t[i + 1]);
        }
        TEMPLATE_SIZES = Collections.unmodifiableMap(sizes);
    }

    /**
     * One placement of a template ({@code data/enhanced/structure/asgard/<template>.nbt}): its corner {@code origin} (where
     * local (0, 0, 0) goes; local y 0 is the ground layer), turned by {@code rotation} about it, {@code mirror}ed. {@code
     * protectedPiece}: inside the city's protection.
     */
    public record Piece(String template, BlockPos origin, Rotation rotation, Mirror mirror, boolean protectedPiece) {
        public ResourceLocation id() {
            return Enhanced.id("asgard/" + template);
        }

        public Vec3i size() {
            return TEMPLATE_SIZES.get(template);
        }

        /** Where a block of the template (local, unturned) lands in the world. */
        public BlockPos toWorld(BlockPos local) {
            return StructureTemplate.transform(local, mirror, rotation, BlockPos.ZERO).offset(origin);
        }

        /** The box the piece fills. */
        public BoundingBox box() {
            Vec3i s = size();
            return BoundingBox.fromCorners(toWorld(BlockPos.ZERO), toWorld(new BlockPos(s.getX() - 1, s.getY() - 1, s.getZ() - 1)));
        }
    }

    /** The rotation that turns a template's front (south, unturned) toward {@code front}. */
    public static Rotation facing(Direction front) {
        return switch (front) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static final List<Piece> PIECES = new ArrayList<>();

    /** A piece at an exact corner, unturned. */
    private static Piece at(String template, int x, int y, int z) {
        Piece piece = new Piece(template, new BlockPos(x, y, z), Rotation.NONE, Mirror.NONE, true);
        PIECES.add(piece);
        return piece;
    }

    /** A piece whose footprint is centred on (cx, cz), its ground layer at {@code top}, its front toward {@code front}. */
    private static Piece centred(String template, int cx, int top, int cz, Direction front) {
        Piece piece = centredPiece(template, cx, top, cz, front);
        PIECES.add(piece);
        return piece;
    }

    private static Piece centredPiece(String template, int cx, int top, int cz, Direction front) {
        Vec3i s = TEMPLATE_SIZES.get(template);
        Rotation rotation = facing(front);
        BlockPos a = StructureTemplate.transform(BlockPos.ZERO, Mirror.NONE, rotation, BlockPos.ZERO);
        BlockPos b = StructureTemplate.transform(new BlockPos(s.getX() - 1, 0, s.getZ() - 1), Mirror.NONE, rotation, BlockPos.ZERO);
        int minX = Math.min(a.getX(), b.getX());
        int minZ = Math.min(a.getZ(), b.getZ());
        int w = Math.abs(a.getX() - b.getX()) + 1;
        int d = Math.abs(a.getZ() - b.getZ()) + 1;
        BlockPos origin = new BlockPos(cx - (w - 1) / 2 - minX, top, cz - (d - 1) / 2 - minZ);
        return new Piece(template, origin, rotation, Mirror.NONE, true);
    }

    private static boolean free(BoundingBox box) {
        for (Piece p : PIECES) {
            if (p.box().inflatedBy(1).intersects(box)) {
                return false;
            }
        }
        return true;
    }

    /** The streets of the houses' district (east-west), and its one north-south street. */
    public static final int[] STREETS_Z = {60, 100, 140, 180};
    public static final int STREET_X = -84;
    public static final int STREET_HALF_WIDTH = 2;
    /** The houses (the citizens' homes, and most of the Watch's hiding places), in order. */
    private static final List<Piece> HOUSES = new ArrayList<>();

    static {
        // the palace: the throne room in two halves, the wings, Odin's chamber, the library, the steps, the vault below
        at("throne_room_north", -16, 81, -136);
        at("throne_room_south", -16, 81, -100);
        at("palace_wing_east", 20, 81, -96);
        at("palace_wing_west", -54, 81, -96);
        at("odin_chamber", 24, 81, -120);
        at("palace_library", -44, 81, -120);
        at("palace_steps", -10, 79, -63);
        at("vault", -10, 70, -126);
        at("vault_stair", -2, 70, -105);
        // the courtyard and the plaza
        centred("plaza_fountain", 0, 79, -38, Direction.SOUTH);
        centred("shrine", -32, 79, -40, Direction.EAST);
        centred("shrine", 32, 79, -40, Direction.WEST);
        int stall = 0;
        for (int z : new int[]{-6, 8, 22}) {
            centred("market_stall_" + (1 + stall % 3), -20, 77, z, Direction.EAST);
            centred("market_stall_" + (1 + (stall + 1) % 3), 20, 77, z, Direction.WEST);
            stall++;
        }
        centred("well", -14, 77, 34, Direction.SOUTH);
        centred("well", 14, 77, 34, Direction.SOUTH);
        // the Golden Road: the Arch of Victory, the statues, the gate of the bridge head
        at("arch_of_victory", -15, 76, 42);
        int[][] statues = {{52, 76}, {92, 75}, {132, 74}, {172, 72}};
        for (int i = 0; i < statues.length; i++) {
            centred("statue_" + (1 + i % 4), -12, statues[i][1], statues[i][0], Direction.EAST);
            centred("statue_" + (1 + (i + 2) % 4), 12, statues[i][1], statues[i][0], Direction.WEST);
        }
        at("bridge_gate", -14, 72, 196);
        // the Observatory over the void
        at("observatory", -13, 73, 427);
        // Frigga's gardens
        centred("pavilion_round", -100, 73, -60, Direction.SOUTH);
        centred("pavilion_long", -126, 73, -32, Direction.EAST);
        centred("pavilion_long", -74, 73, -88, Direction.WEST);
        centred("garden_fountain", -118, 73, -86, Direction.SOUTH);
        centred("garden_fountain", -82, 73, -34, Direction.SOUTH);
        centred("garden_bridge", -128, 73, -60, Direction.SOUTH);
        centred("garden_bridge", -72, 73, -60, Direction.SOUTH);
        centred("garden_bridge", -100, 73, -90, Direction.EAST);
        centred("garden_bridge", -100, 73, -30, Direction.EAST);
        // the training grounds
        centred("training_ring", 95, 69, 0, Direction.SOUTH);
        for (int x : new int[]{87, 95, 103}) {
            centred("weapon_rack", x, 69, -27, Direction.SOUTH);
        }
        centred("sparring_pavilion", 95, 69, 26, Direction.NORTH);
        // the Hall of Feasts and the barracks
        centred("feast_hall", 90, 67, 92, Direction.WEST);
        centred("feast_kitchen", 118, 67, 92, Direction.WEST);
        centred("barracks_hall", 150, 67, 145, Direction.SOUTH);
        centred("barracks_yard", 150, 67, 172, Direction.SOUTH);
        centred("watchtower", 124, 67, 184, Direction.SOUTH);
        centred("watchtower", 174, 67, 136, Direction.SOUTH);
        // the east district: two halls, the forge, the granary, the bathhouse
        centred("hall_healing", 34, 71, 60, Direction.WEST);
        centred("hall_craft", 34, 71, 104, Direction.WEST);
        centred("forge", 34, 71, 134, Direction.WEST);
        centred("granary", 34, 71, 158, Direction.WEST);
        centred("bathhouse", 34, 71, 182, Direction.WEST);
        // the houses' district: two halls and the scribes' tower by the road, the stables, then the houses on the streets
        centred("hall_scribes", -32, 70, 80, Direction.EAST);
        centred("hall_valor", -32, 70, 160, Direction.EAST);
        centred("scribes_tower", -32, 70, 120, Direction.EAST);
        centred("stables", -170, 65, 170, Direction.EAST);
        for (int street : STREETS_Z) {
            for (int side = 0; side < 2; side++) {
                boolean north = side == 0;
                for (int cx : new int[]{-142, -126, -110, -94, -70, -54, -38, -190, -174, -158}) {
                    boolean low = cx < -150;
                    if (low && (street > 140 || street == 140 && !north)) {
                        continue;
                    }
                    String template = "house_" + (1 + (int) (AsgardTerrain.hash(cx, street * 2 + side) % 10));
                    int depth = TEMPLATE_SIZES.get(template).getZ();
                    int cz = north ? street - STREET_HALF_WIDTH - 1 - depth / 2 : street + STREET_HALF_WIDTH + 1 + depth / 2;
                    Piece house = centredPiece(template, cx, low ? 67 : 70, cz, north ? Direction.SOUTH : Direction.NORTH);
                    if (free(house.box())) {
                        PIECES.add(house);
                        HOUSES.add(house);
                    }
                }
            }
        }
        // the docks: the pier out to the west, two skiffs moored beside it
        centred("docks", -232, 50, 20, Direction.WEST);
        centred("skiff_small", -246, 61, 9, Direction.WEST);
        centred("skiff_large", -250, 61, 32, Direction.WEST);
    }

    /** Every piece of the city, in the order they are placed (a later piece's blocks win where two touch). */
    public static List<Piece> pieces() {
        return Collections.unmodifiableList(PIECES);
    }

    /** The houses (the citizens' homes, and most of the Watch's hiding places). */
    public static List<Piece> houses() {
        return Collections.unmodifiableList(HOUSES);
    }

    // ---- the protected city ----

    /** The boxes of the city's builders (outside the pads) that the protection covers. */
    private static final List<BoundingBox> BUILDER_BOXES = List.of(
            new BoundingBox(-6, 30, 196, 6, 82, 428),              // the Rainbow Bridge
            new BoundingBox(-15, 0, 425, 15, 92, 455),             // the Observatory and the rock under it
            new BoundingBox(-165, 60, -64, -148, 80, -56));         // the gardens' channel out to the chasm

    private static final List<BoundingBox> PROTECTED;
    private static final Map<Long, List<BoundingBox>> PROTECTED_BY_CHUNK = new HashMap<>();

    static {
        List<BoundingBox> boxes = new ArrayList<>();
        for (Piece piece : PIECES) {
            if (piece.protectedPiece()) {
                BoundingBox b = piece.box();
                boxes.add(new BoundingBox(b.minX() - 1, b.minY() - 2, b.minZ() - 1, b.maxX() + 1, b.maxY() + 2, b.maxZ() + 1));
            }
        }
        boxes.addAll(BUILDER_BOXES);
        PROTECTED = Collections.unmodifiableList(boxes);
        for (BoundingBox b : PROTECTED) {
            for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
                for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
                    PROTECTED_BY_CHUNK.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(b);
                }
            }
        }
    }

    /** The protected boxes of the city other than its pads (the pieces, a block round them; the bridge; the Observatory). */
    public static List<BoundingBox> protectedBoxes() {
        return PROTECTED;
    }

    /** Whether the plan protects the block at {@code pos} (a protected pad from 8 below its top up, or a protected box). */
    public static boolean isProtected(BlockPos pos) {
        Pad pad = padAt(pos.getX(), pos.getZ());
        if (pad != null && pad.protect() && pos.getY() >= pad.top() - 8) {
            return true;
        }
        List<BoundingBox> near = PROTECTED_BY_CHUNK.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (near != null) {
            for (BoundingBox b : near) {
                if (b.isInside(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- benches (CityBuild sets them), the city's people and places ----

    /** A bench: where it is and which way its sitter faces. */
    public record Bench(BlockPos pos, Direction facing) {
    }

    private static final List<Bench> BENCHES = List.of(
            new Bench(new BlockPos(-10, 80, -38), Direction.EAST), new Bench(new BlockPos(10, 80, -38), Direction.WEST),
            new Bench(new BlockPos(-10, 80, -34), Direction.EAST), new Bench(new BlockPos(10, 80, -34), Direction.WEST),
            new Bench(new BlockPos(-108, 74, -46), Direction.NORTH), new Bench(new BlockPos(-92, 74, -74), Direction.SOUTH),
            new Bench(new BlockPos(-114, 74, -66), Direction.EAST), new Bench(new BlockPos(-86, 74, -54), Direction.WEST),
            new Bench(new BlockPos(-10, 78, 14), Direction.EAST), new Bench(new BlockPos(10, 78, 14), Direction.WEST),
            new Bench(new BlockPos(70, 68, 80), Direction.EAST), new Bench(new BlockPos(70, 68, 104), Direction.EAST));

    /** The benches of the city (CityBuild sets each one at its place). */
    public static List<Bench> benches() {
        return BENCHES;
    }

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
        homes.add(new Home(Role.HEIMDALL, 0, OBSERVATORY_SOCKET.south(3), 4));
        homes.add(new Home(Role.ODIN, 0, ODIN_BED.east(2), 6));
        homes.add(new Home(Role.FRIGGA, 0, FRIGGA_GARDEN, 12));
        homes.add(new Home(Role.SIF, 0, TRAINING_RING.offset(-4, 0, 0), 10));
        homes.add(new Home(Role.VALKYRIE, 0, STABLES, 10));
        homes.add(new Home(Role.VOLSTAGG, 0, FEAST_HALL, 10));
        homes.add(new Home(Role.FANDRAL, 0, TRAINING_RING.offset(4, 0, 0), 10));
        homes.add(new Home(Role.HOGUN, 0, TRAINING_RING.offset(0, 0, 4), 10));
        BlockPos[] guards = {
                new BlockPos(-8, 73, 192), new BlockPos(8, 73, 192),            // the gate of the bridge head
                new BlockPos(0, 73, 300), new BlockPos(0, 73, 390),             // on the bridge
                new BlockPos(-3, 74, 430),                                      // the Observatory's door
                new BlockPos(-8, 80, -58), new BlockPos(8, 80, -58),            // the courtyard
                new BlockPos(-6, 82, -70), new BlockPos(6, 82, -70),            // inside the throne room's doors
                new BlockPos(150, 68, 158), new BlockPos(142, 68, 172),         // the barracks
                new BlockPos(0, 78, 30)};                                       // the plaza
        for (int i = 0; i < EINHERJAR_COUNT; i++) {
            homes.add(new Home(Role.EINHERJAR, i, guards[i], i == 4 ? 4 : 12));
        }
        for (int i = 0; i < CITIZEN_COUNT; i++) {
            // in front of a house's door, every second house of the district
            Piece house = HOUSES.get(i * 2 % HOUSES.size());
            Vec3i s = house.size();
            homes.add(new Home(Role.CITIZEN, i, house.toWorld(new BlockPos(s.getX() / 2, 1, s.getZ() + 1)), 14));
        }
        HOMES = Collections.unmodifiableList(homes);
        List<List<BlockPos>> patrols = new ArrayList<>();
        patrols.add(List.of(new BlockPos(-9, 77, 45), new BlockPos(-9, 73, 194), new BlockPos(9, 73, 194), new BlockPos(9, 77, 45)));
        patrols.add(List.of(new BlockPos(0, 73, 212), new BlockPos(0, 73, 420)));
        patrols.add(List.of(new BlockPos(-30, 80, -55), new BlockPos(30, 80, -55), new BlockPos(30, 80, -24), new BlockPos(-30, 80, -24)));
        patrols.add(List.of(new BlockPos(-3, 82, -70), new BlockPos(-3, 82, -116), new BlockPos(3, 82, -116), new BlockPos(3, 82, -70)));
        PATROLS = Collections.unmodifiableList(patrols);
        List<BlockPos> seats = new ArrayList<>();
        seats.add(THRONE);
        for (Bench bench : BENCHES) {
            seats.add(bench.pos());
        }
        SEATS = Collections.unmodifiableList(seats);
        List<BlockPos> spots = new ArrayList<>();
        // 18 houses (by the back wall, in the middle), the four halls, the library and the stables
        for (int i = 0; spots.size() < 18 && i < 200; i++) {
            Piece house = HOUSES.get(i * 3 % HOUSES.size());
            BlockPos spot = house.toWorld(new BlockPos(house.size().getX() / 2, 1, 2));
            if (!spots.contains(spot)) {
                spots.add(spot);
            }
        }
        for (Piece p : PIECES) {
            if (p.template().startsWith("hall_")) {
                spots.add(p.toWorld(new BlockPos(10, 1, 16)));
            } else if (p.template().equals("palace_library")) {
                spots.add(p.toWorld(new BlockPos(10, 1, 13)));
            } else if (p.template().equals("stables")) {
                spots.add(p.toWorld(new BlockPos(12, 1, 15)));
            }
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

    /** Where Asgardians (and players) can sit: the throne and the benches. */
    public static List<BlockPos> seats() {
        return SEATS;
    }

    /** The 24 places in the city where the Shadows of Hela of the Guardian's Watch can hide (inside buildings). */
    public static List<BlockPos> hidingSpots() {
        return HIDING_SPOTS;
    }

    private CityPlan() {
    }
}
