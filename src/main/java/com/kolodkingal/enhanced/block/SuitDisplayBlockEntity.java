package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A suit display's block entity (3.8, lane L6; on the lower half): the suit it shows ({@code "suit": "mark_3"}, as the
 * tower's templates write it; none on a display a builder has just put down) and how many each player has taken
 * (saved with the world: neither a refit nor a second visit gives a second one). The suit is sent to the clients for
 * the display's renderer ({@link #kind}); the takers are not.
 */
public class SuitDisplayBlockEntity extends BlockEntity {
    @Nullable
    private SuitKind kind;
    private final Map<UUID, Integer> takers = new HashMap<>();

    public SuitDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_DISPLAY.get(), pos, state);
    }

    /** The suit on show, or null for an empty display. */
    @Nullable
    public SuitKind kind() {
        return kind;
    }

    /** Puts a suit on show (a builder's; the templates write it in the block entity's data). */
    public void setKind(@Nullable SuitKind kind) {
        this.kind = kind;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** How many suits {@code player} has taken from this display. */
    public int taken(UUID player) {
        return takers.getOrDefault(player, 0);
    }

    /** Notes one more suit taken by {@code player}. */
    public void take(UUID player) {
        takers.merge(player, 1, Integer::sum);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeShown(tag);
        Takers.write(tag, takers);
    }

    private void writeShown(CompoundTag tag) {
        if (kind != null) {
            tag.putString("suit", kind.id());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        kind = tag.contains("suit") ? SuitKind.byId(tag.getString("suit")) : null;
        Takers.read(tag, takers);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeShown(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The takers of a display or case as their block entity saves them: a list of {player, taken}. */
    static final class Takers {
        static void write(CompoundTag tag, Map<UUID, Integer> takers) {
            ListTag list = new ListTag();
            for (Map.Entry<UUID, Integer> e : takers.entrySet()) {
                CompoundTag t = new CompoundTag();
                t.put("player", NbtUtils.createUUID(e.getKey()));
                t.putInt("taken", e.getValue());
                list.add(t);
            }
            tag.put("takers", list);
        }

        static void read(CompoundTag tag, Map<UUID, Integer> takers) {
            takers.clear();
            for (Tag t : tag.getList("takers", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                if (c.contains("player")) {
                    takers.put(NbtUtils.loadUUID(c.get("player")), c.getInt("taken"));
                }
            }
        }

        private Takers() {
        }
    }
}
