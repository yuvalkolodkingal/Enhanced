package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.config.StonesConfig.Uniqueness;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The stone pedestal's block entity (3.6): who took its stone ({@code [stones] uniqueness = player}: the pedestal still
 * shows the stone to everyone else) and whether its stone is gone for everyone ({@code world}: someone in the world has
 * it; the server looks at the ledger once a second). Both are sent to the clients, so its renderer shows each player
 * what he may still take.
 */
public class StonePedestalBlockEntity extends BlockEntity {
    /** How often (ticks) the server compares the pedestal with the ledger. */
    private static final int CHECK_EVERY = 20;

    private final Set<UUID> takers = new LinkedHashSet<>();
    private boolean gone;
    /**
     * 3.6.1: the seekers this pedestal's stone is held for (the Soul Stone earned at Vormir by one who may not hold it
     * bare), oldest first, with their names; while it holds one, only he or a Bearer may take it.
     */
    private final Map<UUID, String> heldFor = new LinkedHashMap<>();

    public StonePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STONE_PEDESTAL.get(), pos, state);
    }

    /** Whether this player took the stone from this pedestal. */
    public boolean tookIt(UUID player) {
        return takers.contains(player);
    }

    /** Whether the stone is gone for everyone ({@code world} mode: someone has it). */
    public boolean gone() {
        return gone;
    }

    /** The player took the stone. */
    public void taken(UUID player, boolean forEveryone) {
        takers.add(player);
        if (forEveryone) {
            gone = true;
        }
        changed();
    }

    /** The stone is held for this seeker too (3.6.1). */
    public void holdFor(UUID seeker, String name) {
        heldFor.put(seeker, name);
        gone = false;
        changed();
    }

    /** Whether the stone is held for someone (3.6.1). */
    public boolean isHeld() {
        return !heldFor.isEmpty();
    }

    /** Whether the stone is held for this player (3.6.1). */
    public boolean isHeldFor(UUID player) {
        return heldFor.containsKey(player);
    }

    /** The one the stone has been held for longest, or null (3.6.1). */
    @Nullable
    public UUID firstHeldFor() {
        return heldFor.isEmpty() ? null : heldFor.keySet().iterator().next();
    }

    /** The name of one it is held for (as he was called when he earned it). */
    public String heldName(UUID seeker) {
        return heldFor.getOrDefault(seeker, "?");
    }

    /** His stone has been taken (3.6.1). */
    public void release(UUID seeker) {
        heldFor.remove(seeker);
        changed();
    }

    /** Whether the pedestal shows its stone to this player (on the client: to the one who looks). */
    public boolean shows(@Nullable UUID viewer, StoneKind kind) {
        if (isHeld()) {
            return true;
        }
        if (gone) {
            return false;
        }
        return viewer == null || StoneLedger.mode(kind) != Uniqueness.PLAYER || !takers.contains(viewer);
    }

    /** Once a second on the server: in {@code world} mode the stone is gone while someone holds its kind. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, StonePedestalBlockEntity pedestal) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % CHECK_EVERY != 0) {
            return;
        }
        StoneKind kind = StonePedestalBlock.kind(state);
        boolean now = !pedestal.isHeld() && kind != null && StoneLedger.mode(kind) == Uniqueness.WORLD
                && StoneLedger.holder(server.getServer(), kind) != null;
        if (now != pedestal.gone) {
            pedestal.gone = now;
            pedestal.changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (UUID uuid : takers) {
            list.add(NbtUtils.createUUID(uuid));
        }
        tag.put("takers", list);
        tag.putBoolean("gone", gone);
        ListTag held = new ListTag();
        heldFor.forEach((uuid, name) -> {
            CompoundTag one = new CompoundTag();
            one.putUUID("id", uuid);
            one.putString("name", name);
            held.add(one);
        });
        tag.put("held_for", held);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        takers.clear();
        for (Tag uuid : tag.getList("takers", Tag.TAG_INT_ARRAY)) {
            takers.add(NbtUtils.loadUUID(uuid));
        }
        gone = tag.getBoolean("gone");
        heldFor.clear();
        for (Tag one : tag.getList("held_for", Tag.TAG_COMPOUND)) {
            CompoundTag held = (CompoundTag) one;
            if (held.hasUUID("id")) {
                heldFor.put(held.getUUID("id"), held.getString("name"));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
