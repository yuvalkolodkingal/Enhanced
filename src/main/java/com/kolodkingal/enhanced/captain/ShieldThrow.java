package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * R: the Shield Throw, and with Sneak the Ricochet Throw (3.8, PLAN 3.4). The shield leaves the hand that holds it
 * (the main hand first) and flies as a {@link ThrownShieldEntity}; while it flies that hand is empty and he cannot
 * throw again, and the cooldown starts when he catches it. Any shield flies and comes back (a custom hero's decision of
 * the plan); only the Vibranium Shield ricochets.
 */
public final class ShieldThrow {
    /** Throws the shield in his hand (a ricochet with {@code alt}). Returns whether it flew. */
    public static boolean start(ServerPlayer player, PowerData data, boolean alt) {
        HeroAbility row = alt ? HeroAbility.CAPTAIN_RICOCHET : HeroAbility.CAPTAIN_SHIELD_THROW;
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.has(HeroAbility.CAPTAIN_SHIELD_THROW) || alt && !data.has(row) || !data.isReady(row, now)) {
            return false;
        }
        if (flying(player) != null) {
            return false; // (one shield in the air at a time)
        }
        InteractionHand hand = ShieldRules.shieldHand(player);
        if (hand == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.captain.needs_shield"), true);
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean ricochet = alt && ShieldRules.isVibranium(stack);
        int bounces = ricochet ? CaptainAbilities.cfg(CaptainConfig.RICOCHETS, 3) : 0;
        int cooldown = ricochet ? CaptainAbilities.cfg(CaptainConfig.RICOCHET_COOLDOWN, 60)
                : CaptainAbilities.cfg(CaptainConfig.THROW_COOLDOWN, 20);
        ThrownShieldEntity thrown = ThrownShieldEntity.thrown(level, player, stack, hand, ricochet, bounces,
                (float) CaptainAbilities.cfg(CaptainConfig.THROW_DAMAGE, 8.0),
                CaptainAbilities.cfg(CaptainConfig.THROW_SPEED, 1.8), CaptainAbilities.cfg(CaptainConfig.THROW_RANGE,
                        24), cooldown);
        if (player.isUsingItem() && player.getUsedItemHand() == hand) {
            player.stopUsingItem();
        }
        player.setItemInHand(hand, ItemStack.EMPTY);
        level.addFreshEntity(thrown);
        CaptainState cs = player.getData(ModAttachments.CAPTAIN);
        player.setData(ModAttachments.CAPTAIN, cs.withShield(thrown.getId())
                .with(CaptainState.OFF_HAND, hand == InteractionHand.OFF_HAND)
                .with(CaptainState.RICOCHET, ricochet).withBouncesLeft(bounces).with(CaptainState.BACK, false));
        player.swing(hand, true);
        CaptainAbilities.sound(level, player.position(), ModSounds.SHIELD_THROW.get(), 1.0F, ricochet ? 1.15F : 1.0F);
        CaptainAbilities.send(level, player, CaptainFxPayload.THROW, List.of(thrown.position()),
                (hand == InteractionHand.OFF_HAND ? 1 : 0) | (ricochet ? 2 : 0));
        return true;
    }

    /** His shield in flight, or null. */
    @Nullable
    public static ThrownShieldEntity flying(ServerPlayer player) {
        int id = player.getData(ModAttachments.CAPTAIN).shieldEntity();
        if (id < 0) {
            return null;
        }
        Entity e = player.serverLevel().getEntity(id);
        return e instanceof ThrownShieldEntity shield && shield.isAlive() ? shield : null;
    }

    /** Every tick: a shield that was caught, dropped or unloaded is no longer in flight. */
    static void tick(ServerPlayer player, PowerData data) {
        CaptainState cs = player.getData(ModAttachments.CAPTAIN);
        if (cs.shieldEntity() >= 0 && flying(player) == null) {
            player.setData(ModAttachments.CAPTAIN, cs.withShield(-1).with(CaptainState.OFF_HAND, false)
                    .with(CaptainState.RICOCHET, false).withBouncesLeft(0));
        }
    }

    /** His shield in flight drops where it is (he lost the power, died or left). */
    static void dropFlying(ServerPlayer player) {
        ThrownShieldEntity shield = flying(player);
        if (shield != null) {
            shield.drop();
        }
    }

    private ShieldThrow() {
    }
}
