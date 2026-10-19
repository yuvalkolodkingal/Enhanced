package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The server side of the Stone Wheel key (3.6, default J): which stone a crafted Gauntlet in the player's hand uses. A
 * tap of J picks the next stone in it (in the order of the sockets, {@link StoneKind}'s); holding J opens the wheel on
 * his screen and the stone he lets go on is picked ({@code StoneActionPayload(SELECT, index)}).
 */
public final class StoneWheel {

    /**
     * The player picked the stone of ordinal {@code index} in the Gauntlet in his hand (-1: the next one, a tap of J).
     */
    public static void select(ServerPlayer player, int index) {
        ItemStack gauntlet = gauntletInHand(player);
        if (gauntlet == null) {
            return;
        }
        GauntletContents contents = InfinityGauntletItem.contents(gauntlet);
        StoneKind kind = index < 0 ? next(contents) : index < StoneKind.values().length
                ? StoneKind.values()[index] : null;
        if (kind == null || !contents.has(kind)) {
            return;
        }
        gauntlet.set(ModDataComponents.GAUNTLET.get(), contents.withSelected(kind));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GAUNTLET_SELECT.get(),
                SoundSource.PLAYERS, 0.7F, 0.9F + 0.05F * kind.ordinal());
        player.displayClientMessage(Component.translatable("hud.enhanced.stones.selected", kind.displayName())
                .withStyle(s -> s.withColor(kind.color())), true);
    }

    /** The player holds J: his client opens the wheel (nothing to do here but check there is a Gauntlet to turn). */
    public static void open(ServerPlayer player) {
        ItemStack gauntlet = gauntletInHand(player);
        if (gauntlet == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.gauntlet.none_in_hand"), true);
        }
    }

    /** The stone after the selected one in the Gauntlet (wrapping round), or null when it holds none. */
    @Nullable
    public static StoneKind next(GauntletContents contents) {
        StoneKind[] kinds = StoneKind.values();
        StoneKind selected = contents.selectedStone();
        int from = selected == null ? -1 : selected.ordinal();
        for (int i = 1; i <= kinds.length; i++) {
            StoneKind kind = kinds[Math.floorMod(from + i, kinds.length)];
            if (contents.has(kind)) {
                return kind;
            }
        }
        return null;
    }

    /** The crafted Gauntlet with at least one stone in the player's main hand, else his off hand, or null. */
    @Nullable
    public static ItemStack gauntletInHand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (InfinityGauntletItem.crafted(stack) && InfinityGauntletItem.contents(stack).count() > 0) {
                return stack;
            }
        }
        return null;
    }

    private StoneWheel() {
    }
}
