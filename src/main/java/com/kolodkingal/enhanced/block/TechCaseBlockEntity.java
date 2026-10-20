package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * A tech case's block entity (3.8, lane L6): the item it shows and gives (one of it), its rule ({@link Rule#ANYONE},
 * {@link Rule#CAPTAIN_ONLY}) and how many each player has taken; the tower's generator writes {@code {"shown": {id:
 * ..., count: 1}, "rule": "anyone"}}. The item and the rule are sent to the clients for the case's renderer; the
 * takers are not. The client keeps when its lid last opened and closed ({@link #openedAt}, {@link #closedAt}: the
 * drawers of {@code TowerFx}).
 */
public class TechCaseBlockEntity extends BlockEntity {
    /** Who may take from a case. */
    public enum Rule {
        ANYONE,
        CAPTAIN_ONLY;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Rule byId(String id) {
            return "captain_only".equals(id) ? CAPTAIN_ONLY : ANYONE;
        }
    }

    private ItemStack shown = ItemStack.EMPTY;
    private Rule rule = Rule.ANYONE;
    private final Map<UUID, Integer> takers = new HashMap<>();
    /** The client's: the game time its lid last began to open and to close (never: {@link Long#MIN_VALUE}). */
    public long openedAt = Long.MIN_VALUE;
    public long closedAt = Long.MIN_VALUE;

    public TechCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TECH_CASE.get(), pos, state);
    }

    /** The item on show (empty: an empty case). Not to be changed: {@link #setShown} copies. */
    public ItemStack shown() {
        return shown;
    }

    public Rule rule() {
        return rule;
    }

    /** Puts one of {@code stack} on show under {@code rule} (a builder's; the templates write it in the data). */
    public void setShown(ItemStack stack, Rule rule) {
        this.shown = stack.copyWithCount(stack.isEmpty() ? 0 : 1);
        this.rule = rule;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** How many {@code player} has taken from this case. */
    public int taken(UUID player) {
        return takers.getOrDefault(player, 0);
    }

    /** Notes one more taken by {@code player}. */
    public void take(UUID player) {
        takers.merge(player, 1, Integer::sum);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeShown(tag, registries);
        SuitDisplayBlockEntity.Takers.write(tag, takers);
    }

    private void writeShown(CompoundTag tag, HolderLookup.Provider registries) {
        if (!shown.isEmpty()) {
            tag.put("shown", shown.save(registries));
        }
        tag.putString("rule", rule.id());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shown = tag.contains("shown") ? ItemStack.parse(registries, tag.getCompound("shown")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        rule = Rule.byId(tag.getString("rule"));
        SuitDisplayBlockEntity.Takers.read(tag, takers);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeShown(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
