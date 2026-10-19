package com.kolodkingal.enhanced.cottage;

import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.world.CottagePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Every Agatha's Cottage the keeper has found (3.7, the plan's 9.4): SavedData {@code enhanced_cottages} on the
 * Overworld, keyed by the cottage's structure start (its chunk). A cottage remembers where its points are in the world
 * (mapped once from its piece, so turned cottages need no more maths), who its residents are (one UUID per role),
 * Señor Scratchy's UUID, whether its cellar door is on the Road, and his run-offs.
 */
public final class Cottages extends SavedData {
    public static final String NAME = "enhanced_cottages";
    /** No run-off going on (Scratchy is home). */
    public static final long HOME = -1L;
    /** The three residents, in the keeper's order. */
    public static final List<Role> ROLES = List.of(Role.AGATHA, Role.NICKY, Role.RIO);

    // ---- a cottage's flags ----
    /** Its cellar door was added to the Road. */
    public static final int DOOR_REGISTERED = 1;
    /** Its Agatha left because a player is Agatha (she comes back in the same shimmer). */
    public static final int AGATHA_AWAY = 2;
    /** Bit of a role (or of Scratchy) once made: the first of each is made even in sight, the next ones unseen. */
    public static final int MADE_SCRATCHY = 4;

    static int made(Role role) {
        return 8 << ROLES.indexOf(role);
    }

    private final Map<Long, Cottage> cottages = new LinkedHashMap<>();

    public static SavedData.Factory<Cottages> factory() {
        return new SavedData.Factory<>(Cottages::new, Cottages::load, null);
    }

    /** The cottages of the world (kept on the Overworld). */
    public static Cottages get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    /** The cottage of this key (its structure start's chunk), or null. */
    @Nullable
    public Cottage byKey(long key) {
        return cottages.get(key);
    }

    /** Records a cottage (or returns the one already recorded under {@code key}). */
    public Cottage register(long key, Points points) {
        Cottage cottage = cottages.get(key);
        if (cottage == null) {
            cottage = new Cottage(this, key, points);
            cottages.put(key, cottage);
            setDirty();
        }
        return cottage;
    }

    /** Forgets a cottage (the tests' fake ones). */
    public void remove(long key) {
        if (cottages.remove(key) != null) {
            setDirty();
        }
    }

    public Collection<Cottage> all() {
        return List.copyOf(cottages.values());
    }

    /** The cottage a resident belongs to (by his slot, {@code cottage/<key>/<ROLE>}), or null. */
    @Nullable
    public Cottage of(AsgardianEntity npc) {
        long key = keyOf(npc.slot());
        return key == Long.MIN_VALUE ? null : cottages.get(key);
    }

    /** The cottage whose box holds {@code pos} (or is within {@code margin} of it), or null. */
    @Nullable
    public Cottage at(BlockPos pos, int margin) {
        for (Cottage cottage : cottages.values()) {
            if (cottage.points().box().inflatedBy(margin).isInside(pos)) {
                return cottage;
            }
        }
        return null;
    }

    /** The slot a cottage's resident fills: {@code cottage/<key>/<ROLE>}. */
    public static String slot(long key, Role role) {
        return "cottage/" + key + "/" + role.name();
    }

    /** The key in a resident's slot, or {@link Long#MIN_VALUE} for any other slot. */
    public static long keyOf(String slot) {
        if (!slot.startsWith("cottage/")) {
            return Long.MIN_VALUE;
        }
        int end = slot.indexOf('/', 8);
        try {
            return Long.parseLong(end < 0 ? slot.substring(8) : slot.substring(8, end));
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }

    // ---- saving ----

    private static Cottages load(CompoundTag tag, HolderLookup.Provider registries) {
        Cottages store = new Cottages();
        for (Tag t : tag.getList("Cottages", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Points points = Points.load(c.getCompound("Points"));
            if (points == null) {
                continue;
            }
            Cottage cottage = new Cottage(store, c.getLong("Key"), points);
            CompoundTag residents = c.getCompound("Residents");
            for (Role role : ROLES) {
                if (residents.hasUUID(role.name())) {
                    cottage.residents.put(role, residents.getUUID(role.name()));
                }
            }
            if (c.hasUUID("Scratchy")) {
                cottage.scratchy = c.getUUID("Scratchy");
            }
            cottage.flags = c.getInt("Flags");
            cottage.gone = c.contains("Gone") ? c.getLong("Gone") : HOME;
            cottage.rollDay = c.contains("RollDay") ? c.getLong("RollDay") : -1L;
            store.cottages.put(cottage.key, cottage);
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Cottage cottage : cottages.values()) {
            CompoundTag c = new CompoundTag();
            c.putLong("Key", cottage.key);
            c.put("Points", cottage.points.save());
            CompoundTag residents = new CompoundTag();
            cottage.residents.forEach((role, id) -> residents.putUUID(role.name(), id));
            c.put("Residents", residents);
            if (cottage.scratchy != null) {
                c.putUUID("Scratchy", cottage.scratchy);
            }
            c.putInt("Flags", cottage.flags);
            c.putLong("Gone", cottage.gone);
            c.putLong("RollDay", cottage.rollDay);
            list.add(c);
        }
        tag.put("Cottages", list);
        return tag;
    }

    // ------------------------------------------------------------------------------------------------------------

    /**
     * Where a cottage's points are in the world ({@link CottagePlan}'s, through its piece), and its box. The tests make
     * their own (a cottage drawn small inside the arena).
     */
    public record Points(BlockPos agathaHome, BlockPos nickyHome, BlockPos rioHome, BlockPos hutch,
                         BlockPos roadDoor, BlockPos gate, BlockPos pot, BlockPos stove, BlockPos herbs,
                         BlockPos greenhouse, BlockPos yard, BlockPos hutchGate, List<BlockPos> seats,
                         List<BlockPos> beds, List<BlockPos> porch, List<BlockPos> bench, List<BlockPos> garden,
                         BoundingBox box) {

        /** The plan's points mapped by {@code toWorld} (a piece's {@code SitePoints.toWorld}), in {@code box}. */
        public static Points of(Function<BlockPos, BlockPos> toWorld, BoundingBox box) {
            return new Points(toWorld.apply(CottagePlan.AGATHA_HOME), toWorld.apply(CottagePlan.NICKY_HOME),
                    toWorld.apply(CottagePlan.RIO_HOME), toWorld.apply(CottagePlan.HUTCH),
                    toWorld.apply(CottagePlan.ROAD_DOOR), toWorld.apply(CottagePlan.GATE),
                    toWorld.apply(CottagePlan.POT),
                    toWorld.apply(CottagePlan.STOVE), toWorld.apply(CottagePlan.HERBS),
                    toWorld.apply(CottagePlan.GREENHOUSE), toWorld.apply(CottagePlan.YARD),
                    toWorld.apply(CottagePlan.HUTCH_GATE), map(CottagePlan.TABLE_SEATS, toWorld),
                    map(CottagePlan.BEDS, toWorld), map(CottagePlan.PORCH_CHAIRS, toWorld),
                    map(CottagePlan.BENCH, toWorld), map(CottagePlan.GARDEN, toWorld), box);
        }

        private static List<BlockPos> map(List<BlockPos> local, Function<BlockPos, BlockPos> toWorld) {
            List<BlockPos> out = new ArrayList<>();
            for (BlockPos p : local) {
                out.add(toWorld.apply(p).immutable());
            }
            return List.copyOf(out);
        }

        /** A role's home: Agatha's kitchen, Nicky's room, Rio's garden. */
        public BlockPos home(Role role) {
            return switch (role) {
                case NICKY -> nickyHome;
                case RIO -> rioHome;
                default -> agathaHome;
            };
        }

        /** A role's chair at the family table (Agatha 0, Nicky 1, Rio 2; the fourth is for a guest). */
        public BlockPos seat(Role role) {
            return seats.get(Math.min(seats.size() - 1, ROLES.indexOf(role)));
        }

        /** A role's bed (its head). */
        public BlockPos bed(Role role) {
            return beds.get(Math.min(beds.size() - 1, ROLES.indexOf(role)));
        }

        /** The middle of the cottage (its box's centre, at the home's height). */
        public Vec3 center() {
            return new Vec3((box.minX() + box.maxX() + 1) / 2.0, agathaHome.getY(),
                    (box.minZ() + box.maxZ() + 1) / 2.0);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            List<BlockPos> singles = List.of(agathaHome, nickyHome, rioHome, hutch, roadDoor, gate, pot, stove, herbs,
                    greenhouse, yard, hutchGate);
            tag.put("Singles", longs(singles));
            tag.put("Seats", longs(seats));
            tag.put("Beds", longs(beds));
            tag.put("Porch", longs(porch));
            tag.put("Bench", longs(bench));
            tag.put("Garden", longs(garden));
            tag.putIntArray("Box", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
            return tag;
        }

        private static LongArrayTag longs(List<BlockPos> list) {
            long[] out = new long[list.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = list.get(i).asLong();
            }
            return new LongArrayTag(out);
        }

        private static List<BlockPos> positions(long[] longs) {
            List<BlockPos> out = new ArrayList<>();
            for (long l : longs) {
                out.add(BlockPos.of(l));
            }
            return List.copyOf(out);
        }

        @Nullable
        static Points load(CompoundTag tag) {
            List<BlockPos> s = positions(tag.getLongArray("Singles"));
            int[] b = tag.getIntArray("Box");
            if (s.size() < 12 || b.length < 6) {
                return null;
            }
            return new Points(s.get(0), s.get(1), s.get(2), s.get(3), s.get(4), s.get(5), s.get(6), s.get(7), s.get(8),
                    s.get(9), s.get(10), s.get(11), positions(tag.getLongArray("Seats")),
                    positions(tag.getLongArray("Beds")), positions(tag.getLongArray("Porch")),
                    positions(tag.getLongArray("Bench")), positions(tag.getLongArray("Garden")),
                    new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
    }

    /** One cottage: its points, its residents, Scratchy, its flags. Changes mark the store to be saved. */
    public static final class Cottage {
        private final Cottages store;
        private final long key;
        private final Points points;
        private final Map<Role, UUID> residents = new EnumMap<>(Role.class);
        @Nullable
        private UUID scratchy;
        private int flags;
        private long gone = HOME;
        private long rollDay = -1L;
        /** When the keeper first missed a Scratchy who ran off (not saved: he is waited for again after a restart). */
        long missingSince = -1L;
        /** This morning's answer was "he runs off", and he has not gone yet (not saved). */
        boolean pendingRun;

        Cottage(Cottages store, long key, Points points) {
            this.store = store;
            this.key = key;
            this.points = points;
        }

        public long key() {
            return key;
        }

        public Points points() {
            return points;
        }

        @Nullable
        public UUID resident(Role role) {
            return residents.get(role);
        }

        public void setResident(Role role, @Nullable UUID id) {
            UUID before = id == null ? residents.remove(role) : residents.put(role, id);
            if (!java.util.Objects.equals(before, id)) {
                store.setDirty();
            }
        }

        @Nullable
        public UUID scratchy() {
            return scratchy;
        }

        public void setScratchy(@Nullable UUID id) {
            scratchy = id;
            store.setDirty();
        }

        public boolean has(int flag) {
            return (flags & flag) == flag;
        }

        public void set(int flag, boolean on) {
            int before = flags;
            flags = on ? flags | flag : flags & ~flag;
            if (before != flags) {
                store.setDirty();
            }
        }

        /** The run-off going on (the game time it began: its number), or {@link #HOME}. */
        public long gone() {
            return gone;
        }

        public boolean scratchyGone() {
            return gone != HOME;
        }

        public void setGone(long run) {
            gone = run;
            missingSince = -1L;
            store.setDirty();
        }

        /** The day of the last morning the keeper asked whether Scratchy runs off. */
        public long rollDay() {
            return rollDay;
        }

        public void setRollDay(long day) {
            rollDay = day;
            store.setDirty();
        }

        /** The horizontal distance from {@code pos} to the cottage's box (0 inside it). */
        public double distance(Vec3 pos) {
            BoundingBox box = points.box();
            double dx = Math.max(0.0, Math.max(box.minX() - pos.x, pos.x - (box.maxX() + 1)));
            double dz = Math.max(0.0, Math.max(box.minZ() - pos.z, pos.z - (box.maxZ() + 1)));
            return Math.sqrt(dx * dx + dz * dz);
        }
    }
}
