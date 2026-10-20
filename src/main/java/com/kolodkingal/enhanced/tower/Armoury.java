package com.kolodkingal.enhanced.tower;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.block.SuitDisplayBlockEntity;
import com.kolodkingal.enhanced.block.TechCaseBlock;
import com.kolodkingal.enhanced.block.TechCaseBlockEntity;
import com.kolodkingal.enhanced.config.SerumConfig;
import com.kolodkingal.enhanced.config.TowerConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.network.TowerFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Avengers Tower's armoury (3.8, PLAN 9.3 and 9.4): what a suit display and a tech case give. Every display and
 * case gives <b>one per player</b> ({@code [avengers_tower] displaysPerPlayer}; the serum's case {@code
 * [super_soldier_serum] casePerPlayer}; 0: no limit): its block entity keeps who has taken, and the record is made
 * before the item is handed over. The item goes into the hand if it is empty, else the inventory, else at the feet.
 * A display or case stays full: everyone may take his own. Cap's shield case gives only to Captain America (and a
 * custom hero with his Shield Throw, and a Creative player).
 */
public final class Armoury {
    /** How far the armoury's effects are sent. */
    static final double FX_RANGE = 32.0;
    /** The ticks a case's lid stays up after something is taken (then it closes: the block's scheduled tick). */
    public static final int LID_TICKS = 20;

    /** How many of {@code shown} one player may take from one display or case (0: no limit). */
    public static int perPlayer(ItemStack shown) {
        boolean loaded = EnhancedConfig.SPEC.isLoaded();
        if (shown.is(ModItems.SPECIAL_SOLDIER_SERUM.get())) {
            return loaded ? SerumConfig.CASE_PER_PLAYER.get() : 1;
        }
        return loaded ? TowerConfig.DISPLAYS_PER_PLAYER.get() : 1;
    }

    private static boolean mayTakeMore(int taken, int perPlayer) {
        return perPlayer == 0 || taken < perPlayer;
    }

    /**
     * {@code player} takes the suit of the display whose lower half is at {@code pos}: true if he got one. Refused
     * (a line over the hotbar) when he has his already.
     */
    public static boolean takeSuit(ServerLevel level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof SuitDisplayBlockEntity display) || display.kind() == null) {
            return false;
        }
        SuitKind kind = display.kind();
        ItemStack suit = new ItemStack(IronSuits.item(kind));
        if (!mayTakeMore(display.taken(player.getUUID()), perPlayer(suit))) {
            refuse(level, pos, player, Component.translatable("message.enhanced.tower.already", suit.getHoverName()));
            return false;
        }
        display.take(player.getUUID());
        give(player, suit);
        sound(level, pos, ModSounds.SUIT_DISPLAY_TAKE.get());
        fx(level, pos, TowerFxPayload.DISPLAY_TAKE, player.getId());
        return true;
    }

    /**
     * {@code player} takes from the tech case at {@code pos}: true if he got one. Refused when the case is Cap's and he
     * is not Captain America, or when he has his already. The lid opens; it closes {@link #LID_TICKS} later.
     */
    public static boolean takeFromCase(ServerLevel level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TechCaseBlockEntity box) || box.shown().isEmpty()) {
            return false;
        }
        ItemStack item = box.shown().copyWithCount(1);
        if (box.rule() == TechCaseBlockEntity.Rule.CAPTAIN_ONLY && !isCaptain(player)) {
            refuse(level, pos, player, Component.translatable("message.enhanced.tower.cap_only"));
            return false;
        }
        if (!mayTakeMore(box.taken(player.getUUID()), perPlayer(item))) {
            refuse(level, pos, player, Component.translatable("message.enhanced.tower.already_case",
                    item.getHoverName()));
            return false;
        }
        box.take(player.getUUID());
        give(player, item);
        sound(level, pos, ModSounds.TECH_CASE_OPEN.get());
        sound(level, pos, ModSounds.TECH_CASE_TAKE.get());
        fx(level, pos, TowerFxPayload.CASE_OPEN, player.getId());
        level.scheduleTick(pos, level.getBlockState(pos).getBlock(), LID_TICKS);
        return true;
    }

    /** The lid of the case at {@code pos} closes (its scheduled tick). */
    public static void closeLid(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof TechCaseBlock) {
            fx(level, pos, TowerFxPayload.CASE_CLOSE, 0);
        }
    }

    /** Whether {@code player} may take Cap's shield. */
    public static boolean isCaptain(Player player) {
        PowerData data = Powers.get(player);
        return player.isCreative() || data.power() == Power.CAPTAIN_AMERICA
                || data.has(HeroAbility.CAPTAIN_SHIELD_THROW);
    }

    /** Hands {@code stack} over: into the empty main hand, else the inventory, else dropped at his feet. */
    public static void give(Player player, ItemStack stack) {
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.containerMenu.broadcastChanges();
    }

    private static void refuse(ServerLevel level, BlockPos pos, Player player, Component message) {
        player.displayClientMessage(message, true);
        sound(level, pos, ModSounds.DISPLAY_REFUSE.get());
        fx(level, pos, TowerFxPayload.DISPLAY_REFUSE, player.getId());
    }

    private static void sound(ServerLevel level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    static void fx(ServerLevel level, BlockPos pos, int kind, int arg) {
        PacketDistributor.sendToPlayersNear(level, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                FX_RANGE, new TowerFxPayload(kind, pos, arg));
    }

    private Armoury() {
    }
}
