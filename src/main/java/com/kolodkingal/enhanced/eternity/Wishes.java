package com.kolodkingal.enhanced.eternity;

import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.custom.HeroItems;
import com.kolodkingal.enhanced.item.InfinityStoneItem;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.network.EternityScreenPayload;
import com.kolodkingal.enhanced.odin.GungnirStand;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What Eternity grants (3.8, PLAN 10.5): the rules of the wish, checked by the server on every wish (the picker's locks
 * are only a view of them).
 *
 * <ol>
 * <li><b>Never</b>: {@code #enhanced:wish_forbidden} (the operators' blocks, the unbreakable and unobtainable ones),
 * any {@link GameMasterBlockItem}, any item of a feature this world has not enabled, air. The picker hides them.</li>
 * <li><b>The Infinity Stones</b>: only for one who may hold the bare stone ({@link StoneRules#mayHold}, the pedestal's
 * rule: a stone that burns its holder is no wish); the four site stones through the stone ledger ({@link
 * StoneLedger#mayTake}, recorded with {@link StoneLedger#taken} and stamped as a site's take). Mind and Space are not
 * counted (as their vessels are found freely).</li>
 * <li><b>Thanos's Gauntlet</b> comes empty: the stones are wishes of their own.</li>
 * <li><b>Gungnir</b>: not while the world's one bearer is someone else ({@link GungnirStand#owner}); given bound to him
 * and recorded through {@link GungnirStand#giveTo}.</li>
 * <li><b>Mjolnir</b>: only for the worthy, claimed by him.</li>
 * <li><b>Everything else</b>: a full stack ({@code [eternity] wishFullStack}; one when false) into the inventory, the
 * rest at his feet.</li> </ol> Or he becomes <b>Love</b> ({@link #becomeLove}: the power-set path, and a Stormbreaker
 * if he carries none).
 */
public final class Wishes {
    /** The lang key of the refusal of a forbidden item. */
    public static final String FORBIDDEN = "screen.enhanced.wish.lock.forbidden";
    public static final String GUNGNIR = "screen.enhanced.wish.lock.gungnir";
    public static final String MJOLNIR = "screen.enhanced.wish.lock.mjolnir";
    /** A stone that would burn its holder (the pedestal's line). */
    public static final String STONE_HOLD = "message.enhanced.stones.pedestal_refuse";

    /** Whether the player has a wish left ({@code [eternity] wishesPerPlayer}; 0 is unlimited). */
    public static boolean mayWish(ServerPlayer player) {
        int per = EternityRealm.cfg(EternityConfig.WISHES_PER_PLAYER, 1);
        return per == 0 || EternityRealm.progress(player).wishes() < per;
    }

    /** Whether "Become Love" is offered ({@code [eternity] loveWish}). */
    public static boolean loveOffered() {
        return EternityRealm.cfg(EternityConfig.LOVE_WISH, true);
    }

    /**
     * Whether the item is one Eternity never grants anyone (hidden from the picker): the deny tag, the operators'
     * blocks, a feature this world has not enabled, air. Both sides (the client hides with the same rule).
     */
    public static boolean forbidden(Item item, FeatureFlagSet enabled) {
        return item == Items.AIR || new ItemStack(item).is(ModTags.WISH_FORBIDDEN)
                || item instanceof GameMasterBlockItem
                || !item.isEnabled(enabled);
    }

    /**
     * Why the player may not wish for this item (a lang key; the item's name is its argument), or null when he may.
     */
    @Nullable
    public static String refusal(ServerPlayer player, Item item) {
        if (forbidden(item, player.serverLevel().enabledFeatures())) {
            return FORBIDDEN;
        }
        if (item instanceof InfinityStoneItem stone) {
            StoneKind kind = stone.kind;
            if (!StoneRules.mayHold(player, kind)) {
                return STONE_HOLD;
            }
            return StoneLedger.isSiteStone(kind) ? StoneLedger.refusal(player, kind) : null;
        }
        if (item == ModItems.GUNGNIR.get()) {
            UUID owner = GungnirStand.owner(player.server);
            return owner != null && !owner.equals(player.getUUID()) && !player.isCreative() ? GUNGNIR : null;
        }
        if (item == ModItems.MJOLNIR.get()) {
            return Worthiness.isWorthy(player) ? null : MJOLNIR;
        }
        return null;
    }

    /**
     * The items the picker shows locked for this player, and why (only the ones whose answer depends on him: the
     * stones, Gungnir, Mjolnir; the forbidden ones are hidden by the client itself).
     */
    public static List<EternityScreenPayload.Lock> locks(ServerPlayer player) {
        List<EternityScreenPayload.Lock> locks = new ArrayList<>();
        List<Item> asked = new ArrayList<>();
        for (StoneKind kind : StoneKind.values()) {
            asked.add(ModItems.stone(kind));
        }
        asked.add(ModItems.GUNGNIR.get());
        asked.add(ModItems.MJOLNIR.get());
        for (Item item : asked) {
            String why = refusal(player, item);
            if (why != null && locks.size() < EternityScreenPayload.MAX_LOCKS) {
                locks.add(new EternityScreenPayload.Lock(BuiltInRegistries.ITEM.getKey(item), why));
            }
        }
        return locks;
    }

    /**
     * Grants the item by id if the rules allow (the reason told to him otherwise).
     *
     * @return whether it was granted
     */
    public static boolean grant(ServerPlayer player, ResourceLocation id) {
        Optional<Item> found = id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
        if (found.isEmpty()) {
            return false;
        }
        Item item = found.get();
        String why = refusal(player, item);
        if (why != null) {
            player.displayClientMessage(Component.translatable(why, new ItemStack(item).getHoverName())
                    .withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        if (item == ModItems.GUNGNIR.get()) {
            GungnirStand.giveTo(player);
            return true;
        }
        ItemStack stack;
        if (item instanceof InfinityStoneItem stone) {
            stack = new ItemStack(item);
            if (StoneLedger.isSiteStone(stone.kind)) {
                StoneLedger.stamp(stack, player.getUUID());
                StoneLedger.taken(player, stone.kind);
            }
        } else if (item == ModItems.MJOLNIR.get()) {
            stack = new ItemStack(item);
            Worthiness.claim(player, stack);
        } else if (item == ModItems.INFINITY_GAUNTLET.get()) {
            stack = new ItemStack(item); // (empty: the stones are wishes of their own)
        } else {
            int count = EternityRealm.cfg(EternityConfig.WISH_FULL_STACK, true) ? item.getDefaultMaxStackSize() : 1;
            stack = new ItemStack(item, Math.max(1, count));
        }
        give(player, stack);
        return true;
    }

    /** Into his hand if it is empty, else the inventory; what does not fit lies at his feet. */
    static void give(ServerPlayer player, ItemStack stack) {
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack.split(stack.getMaxStackSize()));
        }
        if (!stack.isEmpty()) {
            player.getInventory().add(stack);
        }
        if (!stack.isEmpty()) {
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.2, player.getZ(), stack);
            drop.setNoPickUpDelay();
            drop.setDeltaMovement(0, 0.1, 0);
            player.level().addFreshEntity(drop);
        }
        player.containerMenu.broadcastChanges();
    }

    /** He becomes Love, Thor's daughter (the old hero cleared), with a Stormbreaker if he carries none. */
    public static void becomeLove(ServerPlayer player) {
        Powers.awaken(player, Power.LOVE);
        if (!HeroItems.has(player, ModItems.STORMBREAKER.get())) {
            give(player, new ItemStack(ModItems.STORMBREAKER.get()));
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOVE_AWAKEN.get(),
                SoundSource.PLAYERS, 1.2F, 1.0F);
        Net.sendToPlayersNear(player.serverLevel(), null, player.getX(), player.getY(), player.getZ(), 64,
                new EternityFxPayload(EternityFxPayload.LOVE_AWAKEN, player.getId(), player.blockPosition(), 0));
    }

    private Wishes() {
    }
}
