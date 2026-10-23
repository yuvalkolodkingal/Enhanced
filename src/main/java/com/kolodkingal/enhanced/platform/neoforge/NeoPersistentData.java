package com.kolodkingal.enhanced.platform.neoforge;

import com.kolodkingal.enhanced.platform.attach.AttachKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code ModAttachments.PERSISTENT_DATA} on NeoForge. It has two modes:
 * <ul>
 * <li><b>view</b> (the default while the P0 lanes move their call sites): the key reads and writes the entity's own
 * {@code getPersistentData()} compound, so code already moved and code not yet moved see the same keys;</li>
 * <li><b>attachment</b> ({@code -Denhanced.persistentData=attachment}; the default once no {@code getPersistentData()}
 * call is left, at the P0 gate): the key is the attachment {@code enhanced:persistent_data}, and the first time an
 * entity's compound is asked for, the mod's keys (every key starting with {@code enhanced}) move out of its old
 * {@code NeoForgeData} into it ({@link #migrate}). It is copied as {@code NeoForgeData} was: whole with the entity's save
 * (a change of dimension), only its {@code PlayerPersisted} part when a player is cloned (a respawn, leaving the End).</li>
 * </ul>
 */
public final class NeoPersistentData implements AttachKey.Handle<CompoundTag> {
    /** Whether the key is the attachment (true) or a view of {@code getPersistentData()} (false). */
    public static final boolean ATTACHMENT_MODE = "attachment".equals(System.getProperty("enhanced.persistentData"));

    private static NeoPersistentData instance;

    private final NeoAttach.Handle<CompoundTag> attachment;

    NeoPersistentData(NeoAttach.Handle<CompoundTag> attachment) {
        this.attachment = attachment;
        instance = this;
    }

    /**
     * Moves the mod's keys of {@code entity}'s old {@code NeoForgeData} into {@code enhanced:persistent_data} now, in
     * either mode (for the migration's checks); returns how many moved.
     */
    public static int migrate(Entity entity) {
        return migrate(entity, instance.attachment);
    }

    /** The attachment's own value (in either mode), made if missing (for the migration's checks). */
    public static CompoundTag attachmentOf(Entity entity) {
        return instance.attachment.get(entity);
    }

    NeoAttach.Handle<CompoundTag> attachment() {
        return attachment;
    }

    /** Whether {@code key} of an entity's old {@code NeoForgeData} belongs to the mod (and moves). */
    public static boolean isModKey(String key) {
        return key.startsWith("enhanced");
    }

    /**
     * Moves the mod's keys of {@code entity}'s old {@code NeoForgeData} into its {@code enhanced:persistent_data}
     * attachment (a key already in the attachment keeps the attachment's value); returns how many moved. Once moved,
     * they are gone from {@code NeoForgeData}, so this happens once per entity.
     */
    public static int migrate(Entity entity, AttachKey.Handle<CompoundTag> attachment) {
        CompoundTag old = entity.getPersistentData();
        if (old.isEmpty()) {
            return 0;
        }
        List<String> moving = new ArrayList<>();
        for (String key : old.getAllKeys()) {
            if (isModKey(key)) {
                moving.add(key);
            }
        }
        if (moving.isEmpty()) {
            return 0;
        }
        CompoundTag data = attachment.get(entity);
        for (String key : moving) {
            if (!data.contains(key)) {
                data.put(key, old.get(key));
            }
            old.remove(key);
        }
        return moving.size();
    }

    /** The attachment's copy when a player is cloned: only its {@code PlayerPersisted} part, as {@code NeoForgeData}. */
    static @Nullable CompoundTag copyOnClone(CompoundTag data, IAttachmentHolder holder, HolderLookup.Provider provider) {
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            return null;
        }
        CompoundTag copy = new CompoundTag();
        copy.put(Player.PERSISTED_NBT_TAG, data.get(Player.PERSISTED_NBT_TAG).copy());
        return copy;
    }

    private static Entity entity(Object holder) {
        if (holder instanceof Entity entity) {
            return entity;
        }
        throw new IllegalArgumentException("Only entities have persistent data: " + holder);
    }

    private Entity migrated(Object holder) {
        Entity entity = entity(holder);
        migrate(entity, attachment);
        return entity;
    }

    @Override
    public CompoundTag get(Object holder) {
        return ATTACHMENT_MODE ? attachment.get(migrated(holder)) : entity(holder).getPersistentData();
    }

    @Override
    public @Nullable CompoundTag set(Object holder, CompoundTag value) {
        if (ATTACHMENT_MODE) {
            return attachment.set(migrated(holder), value);
        }
        CompoundTag data = entity(holder).getPersistentData();
        CompoundTag previous = data.copy();
        for (String key : List.copyOf(data.getAllKeys())) {
            data.remove(key);
        }
        data.merge(value);
        return previous;
    }

    @Override
    public boolean has(Object holder) {
        return ATTACHMENT_MODE ? attachment.has(migrated(holder)) : !entity(holder).getPersistentData().isEmpty();
    }

    @Override
    public @Nullable CompoundTag remove(Object holder) {
        if (ATTACHMENT_MODE) {
            return attachment.remove(migrated(holder));
        }
        CompoundTag data = entity(holder).getPersistentData();
        CompoundTag previous = data.copy();
        for (String key : List.copyOf(data.getAllKeys())) {
            data.remove(key);
        }
        return previous;
    }

    @Override
    public Optional<CompoundTag> getExisting(Object holder) {
        if (ATTACHMENT_MODE) {
            return attachment.getExisting(migrated(holder));
        }
        CompoundTag data = entity(holder).getPersistentData();
        return data.isEmpty() ? Optional.empty() : Optional.of(data);
    }

    @Override
    public void sync(Object holder) {
        // Persistent data is never synced.
    }
}
