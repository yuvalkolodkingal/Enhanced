package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.world.HydraOutpostPlan;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The posts of a Hydra outpost's garrison (3.6, the plan's 15.4), in the order they are manned: {@code [hydra]
 * garrisonSize} (9 by default, up to 14) takes the first ones. The first nine are the plan's: two gate guards, the
 * tower's sniper, the yard patrol (a rifleman and an enforcer), the perimeter patrol (two riflemen, a loop 6 outside
 * the fence), the enforcer at the foot of the cellar stairs and the officer. The five more a server may add stand at
 * the bunker's door, on the helipad, in the command room, in the yard patrol and in the radio room.
 *
 * <p>Points are template-relative and unturned ({@link HydraOutpostPlan}); a patrol's post is a route, which he
 * starts at its corner {@link #start()}.
 */
public enum Post {
    GATE_WEST(Rank.RIFLEMAN, HydraOutpostPlan.GATE_POSTS.get(0), null, 0),
    GATE_EAST(Rank.RIFLEMAN, HydraOutpostPlan.GATE_POSTS.get(1), null, 0),
    TOWER(Rank.SNIPER, HydraOutpostPlan.TOWER_POST, null, 0),
    YARD_RIFLEMAN(Rank.RIFLEMAN, null, Route.YARD, 0),
    YARD_ENFORCER(Rank.ENFORCER, null, Route.YARD, 2),
    PERIMETER_NORTH(Rank.RIFLEMAN, null, Route.PERIMETER, 0),
    PERIMETER_SOUTH(Rank.RIFLEMAN, null, Route.PERIMETER, 2),
    CELLAR(Rank.ENFORCER, HydraOutpostPlan.CELLAR_POST, null, 0),
    /** The officer: the command room by day, the radio room by night. */
    OFFICER(null, HydraOutpostPlan.COMMAND_POST, null, 0),
    DOOR(Rank.RIFLEMAN, new BlockPos(13, 6, 19), null, 0),
    HELIPAD(Rank.RIFLEMAN, new BlockPos(26, 6, 14), null, 0),
    COMMAND_ROOM(Rank.ENFORCER, new BlockPos(17, 6, 13), null, 0),
    YARD_REAR(Rank.RIFLEMAN, null, Route.YARD, 1),
    RADIO_ROOM(Rank.RIFLEMAN, new BlockPos(17, 11, 9), null, 0);

    /** The most posts an outpost has ({@code [hydra] garrisonSize} is at most this). */
    public static final int COUNT = values().length;

    /**
     * The radio consoles a soldier may run to to raise the alarm (observers): the radio room's on the roof and the
     * command room's two. A soldier needs only reach one of them.
     */
    public static final List<BlockPos> CONSOLES = List.of(HydraOutpostPlan.ALARM_CONSOLE, new BlockPos(17, 12, 8),
            new BlockPos(16, 7, 7), new BlockPos(18, 7, 7));
    /** The radio room's two consoles, where a player switches the alarm off (hold use 3 s). */
    public static final List<BlockPos> RADIO_CONSOLES = CONSOLES.subList(0, 2);
    /** Where a soldier stands to use each of {@link #CONSOLES} (the block in front of it). */
    public static final List<BlockPos> CONSOLE_SPOTS = List.of(new BlockPos(16, 11, 9), new BlockPos(17, 11, 9),
            new BlockPos(16, 6, 8), new BlockPos(18, 6, 8));

    /** The patrols' loops. */
    public enum Route {
        YARD(HydraOutpostPlan.YARD_ROUTE, false),
        PERIMETER(HydraOutpostPlan.PERIMETER_ROUTE, true);

        private final List<BlockPos> corners;
        private final boolean onSurface;

        Route(List<BlockPos> corners, boolean onSurface) {
            this.corners = corners;
            this.onSurface = onSurface;
        }

        public List<BlockPos> corners() {
            return corners;
        }

        /** Whether its y is the ground's at each corner (outside the compound) rather than the template's. */
        public boolean onSurface() {
            return onSurface;
        }
    }

    @Nullable
    private final Rank rank;
    @Nullable
    private final BlockPos point;
    @Nullable
    private final Route route;
    private final int start;

    Post(@Nullable Rank rank, @Nullable BlockPos point, @Nullable Route route, int start) {
        this.rank = rank;
        this.point = point;
        this.route = route;
        this.start = start;
    }

    /** The soldier's rank, or null: the officer's post. */
    @Nullable
    public Rank rank() {
        return rank;
    }

    public boolean isOfficer() {
        return rank == null;
    }

    /** The post's point (template-relative), or null for a patrol. */
    @Nullable
    public BlockPos point() {
        return point;
    }

    /** The patrol's route, or null for a fixed post. */
    @Nullable
    public Route route() {
        return route;
    }

    /** The corner of the route the patrol starts at (and is posted at when he appears). */
    public int start() {
        return start;
    }

    /**
     * Where the soldier of this post appears, template-relative: his point, or his route's starting corner (whose y
     * is only the template's floor for the perimeter: the garrison puts him on the ground).
     */
    public BlockPos spawnPoint() {
        return point != null ? point : route.corners().get(start);
    }

    /** The post at {@code index}, or null. */
    @Nullable
    public static Post byIndex(int index) {
        Post[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }
}
