package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.item.VibraniumShieldItem;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

/**
 * The rules of the Vibranium Shield (3.8, PLAN 3.3): who it serves as Captain America's, when it is up, and what lies in
 * front of it. Anyone may block with it as with any shield; for Captain America ({@link #isCap}: the hero, or a custom
 * hero with his Shield Throw) it is up the moment he raises it (no 5-tick delay), covers everything from the front
 * {@code capBlockAngle} degrees (explosions and beams too, never the void or a {@code /kill}), bounces projectiles
 * back and is never disabled by an axe ({@link CaptainEvents}). Both sides (the client's guard stance asks it too).
 */
public final class ShieldRules {
    /** Whether the stack is the Vibranium Shield. */
    public static boolean isVibranium(ItemStack stack) {
        return stack.getItem() instanceof VibraniumShieldItem;
    }

    /** Whether the stack is a shield of any kind (the Vibranium Shield, vanilla's, a mod's): what a throw may throw. */
    public static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    /** Whether the player is Captain America as far as the shield goes: the hero, or a custom hero who throws it. */
    public static boolean isCap(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.CAPTAIN_AMERICA || data.has(HeroAbility.CAPTAIN_SHIELD_THROW);
    }

    /** The hand that holds a shield (the main hand first), or null. */
    @Nullable
    public static InteractionHand shieldHand(Player player) {
        if (isShield(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        return isShield(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
    }

    /** Whether a hand holds the Vibranium Shield. */
    public static boolean holdsVibranium(Player player) {
        return isVibranium(player.getMainHandItem()) || isVibranium(player.getOffhandItem());
    }

    /**
     * Whether he carries a Vibranium Shield somewhere but in his hands (it is drawn on his back then). Server side: the
     * client knows only its own player's inventory, and reads the synced flag for the others.
     */
    public static boolean carriesOnBack(Player player) {
        if (holdsVibranium(player)) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            if (isVibranium(inventory.items.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether Captain America's Vibranium Shield is up: raised (the use of it, at once), or before him in the Shield
     * Charge.
     */
    public static boolean guarding(Player player) {
        if (!isCap(player)) {
            return false;
        }
        if (player.isUsingItem() && isVibranium(player.getUseItem())) {
            return true;
        }
        return Powers.get(player).is(Live.CAP_CHARGING) && holdsVibranium(player);
    }

    /** The angle in front of him the shield covers (degrees). */
    public static double angle() {
        return CaptainAbilities.cfg(CaptainConfig.CAP_BLOCK_ANGLE, 180.0);
    }

    /**
     * Whether a point lies in front of him within the shield's angle (flat, by where his head looks, as vanilla's
     * shield does). A point straight above, below or at him is in no direction: never in front.
     */
    public static boolean inFront(Player player, Vec3 point) {
        Vec3 look = player.calculateViewVector(0.0F, player.getYHeadRot());
        Vec3 to = point.subtract(player.position());
        Vec3 flatTo = new Vec3(to.x, 0.0, to.z);
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
        if (flatTo.lengthSqr() < 1.0E-6 || flatLook.lengthSqr() < 1.0E-8) {
            return false;
        }
        double cos = flatTo.normalize().dot(flatLook.normalize());
        double half = Math.min(360.0, angle()) / 2.0;
        // (At the default 180 degrees exactly what vanilla's shield covers: anything strictly in front.)
        return half == 90.0 ? cos > 1.0E-6 : cos >= Math.cos(Math.toRadians(half));
    }

    /** Whether the shield takes this blow whole for Captain America: up, from the front, not past invulnerability. */
    public static boolean blocksAll(Player player, DamageSource source) {
        if (!CaptainAbilities.cfg(CaptainConfig.CAP_BLOCKS_EVERYTHING, true) || !guarding(player)) {
            return false;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        Vec3 from = source.getSourcePosition();
        return from != null && inFront(player, from);
    }

    private ShieldRules() {
    }
}
