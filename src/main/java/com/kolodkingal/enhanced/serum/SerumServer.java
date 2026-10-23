package com.kolodkingal.enhanced.serum;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SerumConfig;
import com.kolodkingal.enhanced.custom.HeroItems;
import com.kolodkingal.enhanced.falcon.FalconWings;
import com.kolodkingal.enhanced.network.SerumScreenPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Super Soldier Serum's server side (3.8, PLAN 2.1): {@link #open} remembers a pending choice and sends the choice
 * screen; {@link #choose} takes the answer and checks everything again, never trusting the client: a pending choice
 * that is fresh ({@code pendingTicks}), the same hand and slot still holding a serum, one of the serum's three heroes,
 * Falcon only while he carries the Exo-Wings now, alive, not a spectator, not that hero already. Then the serum is
 * used up (not in Creative) and he becomes the hero by {@link Powers#awaken}, the very path of {@code /enhanced power
 * set} (the old hero fully cleared); Captain America gets a Vibranium Shield if he has none. A refused choice tells why
 * and keeps the serum; Escape sends nothing (the serum is kept, the pending choice runs out).
 */
public final class SerumServer {
    /** The heroes of the serum. */
    public static final Set<Power> CHOICES = EnumSet.of(Power.CAPTAIN_AMERICA, Power.RED_HULK, Power.FALCON);
    /** The inventory slot that stands for the off hand. */
    static final int OFF_HAND_SLOT = 40;

    /**
     * A screen sent and not yet answered: the hand and slot the serum was used from, when, and whether it is the
     * operators' command's (which uses up nothing).
     */
    public record Pending(InteractionHand hand, int slot, long at, boolean free) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** Whether the serum works ({@code [super_soldier_serum] enabled}). */
    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || SerumConfig.ENABLED.get();
    }

    /** How long an open screen stays good for a choice (ticks). */
    public static int pendingTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? SerumConfig.PENDING_TICKS.get() : 600;
    }

    /** The player used a serum in {@code hand}: the screen opens (Falcon locked unless he carries the Exo-Wings). */
    public static void open(ServerPlayer player, InteractionHand hand) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        if (!enabled()) {
            player.displayClientMessage(Component.translatable("message.enhanced.serum.disabled"), true);
            return;
        }
        int slot = hand == InteractionHand.OFF_HAND ? OFF_HAND_SLOT : player.getInventory().selected;
        send(player, new Pending(hand, slot, player.level().getGameTime(), false));
    }

    /** The operators' {@code /enhanced serum open}: the screen without a serum (the choice uses nothing up). */
    public static void openFree(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        send(player, new Pending(InteractionHand.MAIN_HAND, -1, player.level().getGameTime(), true));
    }

    private static void send(ServerPlayer player, Pending pending) {
        PENDING.put(player.getUUID(), pending);
        sound(player, ModSounds.SERUM_OPEN.get(), 0.8F, 1.0F);
        Net.sendToPlayer(player, new SerumScreenPayload(pending.hand(), FalconWings.carries(player)));
    }

    /** The choice he has open, or null. */
    @Nullable
    public static Pending pending(ServerPlayer player) {
        return PENDING.get(player.getUUID());
    }

    /** The screen's answer ({@code SerumChoicePayload}). Returns whether he became the hero. */
    public static boolean choose(ServerPlayer player, Power choice) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null) {
            refuse(player, "message.enhanced.serum.no_choice");
            return false;
        }
        if (player.level().getGameTime() - pending.at() >= pendingTicks()) {
            refuse(player, "message.enhanced.serum.expired");
            return false;
        }
        if (!player.isAlive() || player.isSpectator() || !CHOICES.contains(choice)) {
            return false;
        }
        if (!pending.free() && !holdsSerum(player, pending)) {
            refuse(player, "message.enhanced.serum.moved");
            return false;
        }
        if (choice == Power.FALCON && !FalconWings.carries(player)) {
            sound(player, ModSounds.SERUM_LOCKED.get(), 0.8F, 1.0F);
            refuse(player, "message.enhanced.serum.needs_wings");
            return false;
        }
        if (Powers.power(player) == choice) {
            player.displayClientMessage(Component.translatable("message.enhanced.serum.already", choice.displayName())
                    .withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        if (!pending.free() && !player.getAbilities().instabuild) {
            player.getItemInHand(pending.hand()).shrink(1);
        }
        Powers.awaken(player, choice);
        sound(player, ModSounds.SERUM_CHOOSE.get(), 1.0F, 1.0F);
        if (choice == Power.CAPTAIN_AMERICA) {
            sound(player, ModSounds.CAPTAIN_AWAKEN.get(), 1.0F, 1.0F);
            giveShield(player, pending.hand());
        }
        return true;
    }

    /** Whether the hand (and slot) the serum was used from still holds one. */
    static boolean holdsSerum(ServerPlayer player, Pending pending) {
        if (pending.hand() == InteractionHand.MAIN_HAND && player.getInventory().selected != pending.slot()) {
            return false;
        }
        return player.getItemInHand(pending.hand()).is(ModItems.SPECIAL_SOLDIER_SERUM.get());
    }

    /** Captain America's shield, if he has none: into the hand the serum left empty, or his inventory. */
    static void giveShield(ServerPlayer player, InteractionHand hand) {
        if (HeroItems.has(player, ModItems.VIBRANIUM_SHIELD.get())) {
            return;
        }
        ItemStack shield = new ItemStack(ModItems.VIBRANIUM_SHIELD.get());
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, shield);
        } else if (!player.getInventory().add(shield)) {
            player.drop(shield, false);
        }
    }

    private static void refuse(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD), true);
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume,
                pitch);
    }

    /** He left, died or changed dimension: his open choice is forgotten. */
    public static void forget(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void clear() {
        PENDING.clear();
    }

    private SerumServer() {
    }
}
