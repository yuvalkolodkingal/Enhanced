package com.kolodkingal.enhanced.road;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * One player's progress on the Witches' Road, saved with the world ({@link RoadSave}): how many trials he has won, where
 * he is in the one he plays, and where he came from (so he can be sent back whatever happens). Every change marks the
 * save dirty.
 */
public final class RoadProgress {
    /**
     * Where a player entered the Road and in what state, to send him back exactly. {@code door} is the Witches' Road door he opened
     * (null when he was sent in by a command): the next best place to put him back if the place he stood on is gone.
     */
    public record Entry(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch, int gameMode, @Nullable BlockPos door) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Entry::dimension),
                Vec3.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.FLOAT.fieldOf("yaw").forGetter(Entry::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(Entry::pitch),
                Codec.INT.fieldOf("gameMode").forGetter(Entry::gameMode),
                BlockPos.CODEC.optionalFieldOf("door").forGetter(e -> Optional.ofNullable(e.door))).apply(i,
                (dimension, pos, yaw, pitch, gameMode, door) -> new Entry(dimension, pos, yaw, pitch, gameMode, door.orElse(null))));

        public Entry(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch, int gameMode) {
            this(dimension, pos, yaw, pitch, gameMode, null);
        }
    }

    public static final Codec<RoadProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("completed", 0).forGetter(p -> p.completed),
            Codec.LONG.optionalFieldOf("salt", 0L).forGetter(p -> p.salt),
            Codec.INT.optionalFieldOf("checkpoint", 0).forGetter(p -> p.checkpoint),
            Codec.BOOL.optionalFieldOf("inside", false).forGetter(p -> p.inside),
            Entry.CODEC.optionalFieldOf("entry").forGetter(p -> Optional.ofNullable(p.entry)),
            Codec.INT.optionalFieldOf("falls", 0).forGetter(p -> p.falls),
            Codec.INT.optionalFieldOf("started", 0).forGetter(p -> p.started),
            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).optionalFieldOf("data", Map.of()).forGetter(p -> p.data))
            .apply(i, RoadProgress::new));

    private int completed;
    private long salt;
    private int checkpoint;
    private boolean inside;
    @Nullable
    private Entry entry;
    private int falls;
    private int started;
    private final Map<String, CompoundTag> data;
    /** Set by the save that holds this progress. */
    @Nullable
    private Runnable dirty;

    public RoadProgress() {
        this(0, 0L, 0, false, Optional.empty(), 0, 0, Map.of());
    }

    private RoadProgress(int completed, long salt, int checkpoint, boolean inside, Optional<Entry> entry, int falls, int started,
                         Map<String, CompoundTag> data) {
        this.completed = completed;
        this.salt = salt;
        this.checkpoint = checkpoint;
        this.inside = inside;
        this.entry = entry.orElse(null);
        this.falls = falls;
        this.started = started;
        this.data = new HashMap<>(data);
    }

    void attach(Runnable dirty) {
        this.dirty = dirty;
    }

    private void changed() {
        if (dirty != null) {
            dirty.run();
        }
    }

    /** How many trials he has won; the next one to play is the one with this number (0 is the first). */
    public int completed() {
        return completed;
    }

    public void setCompleted(int completed) {
        if (this.completed != completed) {
            this.completed = Math.max(0, completed);
            this.checkpoint = 0;
            changed();
        }
    }

    /** Changes his courses (an operator's reset). */
    public long salt() {
        return salt;
    }

    public void setSalt(long salt) {
        this.salt = salt;
        changed();
    }

    /** The checkpoint he has reached in the trial he plays (0 is its start). */
    public int checkpoint() {
        return checkpoint;
    }

    public void setCheckpoint(int checkpoint) {
        if (this.checkpoint != checkpoint) {
            this.checkpoint = checkpoint;
            changed();
        }
    }

    /** Whether he is in a trial now (from the moment he is taken in to the moment he is sent back). */
    public boolean inside() {
        return inside;
    }

    public void setInside(boolean inside) {
        if (this.inside != inside) {
            this.inside = inside;
            changed();
        }
    }

    @Nullable
    public Entry entry() {
        return entry;
    }

    public void setEntry(@Nullable Entry entry) {
        this.entry = entry;
        changed();
    }

    public int falls() {
        return falls;
    }

    public void addFall() {
        falls++;
        changed();
    }

    public int started() {
        return started;
    }

    public void addStarted() {
        started++;
        changed();
    }

    /** What a trial keeps between attempts (a copy: write it back with {@link #putData}). */
    public CompoundTag data(String challenge) {
        CompoundTag tag = data.get(challenge);
        return tag == null ? new CompoundTag() : tag.copy();
    }

    public void putData(String challenge, CompoundTag tag) {
        data.put(challenge, tag.copy());
        changed();
    }

    /** Forgets what a trial kept. */
    public void clearData(String challenge) {
        if (data.remove(challenge) != null) {
            changed();
        }
    }

    /** Whether he has nothing to remember (a fresh player): the save does not keep it. */
    public boolean isBlank() {
        return isUntouched() && salt == 0L;
    }

    /** Whether he has walked no step of the Road (the course he is dealt does not count). */
    public boolean isUntouched() {
        return completed == 0 && !inside && checkpoint == 0 && falls == 0 && started == 0 && data.isEmpty();
    }
}
