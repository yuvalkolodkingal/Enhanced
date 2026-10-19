package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.block.StonePedestalBlock;
import com.kolodkingal.enhanced.block.StonePedestalBlockEntity;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The Soul Stone earned at Vormir by one who may not hold it bare (3.6.1, the user's call: anyone may earn it). After
 * the sacrifice the stone appears on a stone pedestal by the pool ({@code StoneSitePlans.Vormir.PEDESTAL}), held for
 * him: the ledger names him its taker at once (his progress, the world's holder), and the stone is stamped for him when
 * it is taken. Only he or a Bearer (who may hold it bare) may take it; everyone else is told whose it is. He takes it
 * straight into an Infinity Gauntlet he carries (a crafted one with an empty Soul socket), else into a shulker box he
 * carries (where it is safe), else into his hand, where it will not stay long (the stones' grace). When the last stone
 * it held is taken, the pedestal goes.
 */
public final class HeldSoulStone {
    /** How far round the plan's place a free place is looked for (another block stands there). */
    private static final int SEARCH = 2;

    /**
     * The stone appears on the pedestal by the pool of {@code site}, held for {@code seeker}.
     *
     * @return the pedestal's place, or null when there is no room for one (then he is given the stone as anyone)
     */
    @Nullable
    public static BlockPos place(ServerLevel level, VormirSite site, ServerPlayer seeker) {
        BlockPos at = free(level, site.pedestal());
        if (at == null) {
            return null;
        }
        if (!isSoulPedestal(level.getBlockState(at))) {
            level.setBlock(at, ModBlocks.STONE_PEDESTAL.get().defaultBlockState()
                    .setValue(StonePedestalBlock.STONE, StonePedestalBlock.Stone.SOUL)
                    .setValue(StonePedestalBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        }
        if (!(level.getBlockEntity(at) instanceof StonePedestalBlockEntity pedestal)) {
            return null;
        }
        pedestal.holdFor(seeker.getUUID(), seeker.getGameProfile().getName());
        level.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 1.4, at.getZ() + 0.5, 20, 0.3, 0.4, 0.3,
                0.02);
        return at;
    }

    /** The plan's place if it is free (air, or a Soul pedestal already there), else the nearest free place round it. */
    @Nullable
    private static BlockPos free(ServerLevel level, BlockPos plan) {
        if (fits(level, plan)) {
            return plan;
        }
        for (int r = 1; r <= SEARCH; r++) {
            for (BlockPos pos : BlockPos.betweenClosed(plan.offset(-r, 0, -r), plan.offset(r, 0, r))) {
                if (fits(level, pos)) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }

    private static boolean fits(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isSoulPedestal(state) || state.isAir() && level.getBlockState(pos.below()).isSolid();
    }

    private static boolean isSoulPedestal(BlockState state) {
        return state.is(ModBlocks.STONE_PEDESTAL.get()) && StonePedestalBlock.kind(state) == StoneKind.SOUL;
    }

    /**
     * {@code player} asks a pedestal that holds a stone for someone: he gets it if it is held for him, or (the oldest
     * one held) if he may hold it bare; anyone else is told whose it is.
     *
     * @return whether he got it
     */
    public static boolean take(ServerPlayer player, ServerLevel level, BlockPos pos, StonePedestalBlockEntity pedestal,
            StoneKind kind) {
        UUID owner = pedestal.isHeldFor(player.getUUID()) ? player.getUUID()
                : StoneRules.mayHold(player, kind) ? pedestal.firstHeldFor() : null;
        if (owner == null) {
            UUID first = pedestal.firstHeldFor();
            player.displayClientMessage(Component.translatable("message.enhanced.vormir.held_for",
                    first == null ? "?" : pedestal.heldName(first)).withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        pedestal.release(owner);
        ItemStack stone = new ItemStack(ModItems.stone(kind));
        StoneLedger.stamp(stone, owner);
        StonePedestalBlock.takenEffects(player, level, pos, kind);
        if (StoneRules.mayHold(player, kind)) {
            give(player, stone);
        } else {
            stow(player, stone);
        }
        if (!pedestal.isHeld()) {
            level.removeBlock(pos, false);
        }
        return true;
    }

    /** Into his hand if it is free, else his inventory, else at his feet. */
    private static void give(ServerPlayer player, ItemStack stone) {
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        } else if (!player.getInventory().add(stone)) {
            player.drop(stone, false);
        }
    }

    /**
     * One who may not hold the stone bare: into a crafted Infinity Gauntlet he carries, else a shulker box he carries
     * (both safe), else his hand, with a warning.
     */
    public static void stow(ServerPlayer player, ItemStack stone) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack gauntlet = inventory.getItem(i);
            if (InfinityGauntletItem.crafted(gauntlet)
                    && InfinityGauntletItem.insert(gauntlet, stone, player) != null) {
                player.sendSystemMessage(Component.translatable("message.enhanced.vormir.stowed_gauntlet")
                        .withColor(Sacrifice.WHY_COLOR));
                return;
            }
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack box = inventory.getItem(i);
            if (box.getCount() == 1 && box.getItem() instanceof BlockItem item
                    && item.getBlock() instanceof ShulkerBoxBlock && putIn(box, stone)) {
                player.sendSystemMessage(Component.translatable("message.enhanced.vormir.stowed_box")
                        .withColor(Sacrifice.WHY_COLOR));
                return;
            }
        }
        give(player, stone);
        player.sendSystemMessage(Component.translatable("message.enhanced.vormir.carry_quick")
                .withColor(Sacrifice.WHY_COLOR));
    }

    /** Puts the stone into the first free slot of a shulker box item; false when it is full. */
    private static boolean putIn(ItemStack box, ItemStack stone) {
        NonNullList<ItemStack> slots = NonNullList.withSize(ShulkerBoxBlockEntity.CONTAINER_SIZE, ItemStack.EMPTY);
        box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots);
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isEmpty()) {
                slots.set(i, stone);
                box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
                return true;
            }
        }
        return false;
    }

    private HeldSoulStone() {
    }
}
