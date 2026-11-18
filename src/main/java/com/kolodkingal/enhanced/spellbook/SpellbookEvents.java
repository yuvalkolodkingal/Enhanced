package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A spellbook on a lectern (Agatha's room and Rio's in her cottage, the Sanctum's library) is taken by using the lectern:
 * a lectern only opens to read a written book, so the book goes straight into the hand of whoever uses it (or the
 * inventory), as "Take Book" would.
 */
@Subscriber
public final class SpellbookEvents {
    private SpellbookEvents() {
    }

    @Subscribe
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!take(player, level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** The player takes the spellbook from the lectern at {@code pos}; false (nothing done) if it holds none or he may not. */
    public static boolean take(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LecternBlock) || !state.getValue(LecternBlock.HAS_BOOK)
                || !(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)
                || !(lectern.getBook().getItem() instanceof SpellbookItem item) || !player.mayBuild()) {
            return false;
        }
        ItemStack book = lectern.getBook().copy();
        lectern.setBook(ItemStack.EMPTY);
        LecternBlock.resetBookState(player, level, pos, state, false);
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.enhanced.spellbook.taken", item.book().bookName())
                .withStyle(s -> s.withColor(item.book().glow)), true);
        return true;
    }
}
