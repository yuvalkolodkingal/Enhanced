package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Weapon X rack's block entity (3.7, lane L4): how many Adamantium Injections each player has taken from this rack
 * (saved with the world), so each player gets his own, two brothers one each.
 */
public class AdamantiumRackBlockEntity extends BlockEntity {
    private final Map<UUID, Integer> takers = new HashMap<>();

    public AdamantiumRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADAMANTIUM_RACK.get(), pos, state);
    }

    /** How many injections {@code player} has taken from this rack. */
    public int taken(UUID player) {
        return takers.getOrDefault(player, 0);
    }

    /** Notes one more injection taken by {@code player}. */
    public void take(UUID player) {
        takers.merge(player, 1, Integer::sum);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> e : takers.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.put("player", NbtUtils.createUUID(e.getKey()));
            t.putInt("taken", e.getValue());
            list.add(t);
        }
        tag.put("takers", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        takers.clear();
        for (Tag t : tag.getList("takers", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.contains("player")) {
                takers.put(NbtUtils.loadUUID(c.get("player")), c.getInt("taken"));
            }
        }
    }
}
