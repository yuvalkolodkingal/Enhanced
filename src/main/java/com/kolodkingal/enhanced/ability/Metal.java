package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What is metal: the one rule every Magneto power asks (his R, his Armor &amp; Weapon Strip, the Magnetic Pull, the aim of
 * {@link Raycasts#metalBlock}), so they never disagree about what he can take (D-M 2).
 *
 * <p>The skeleton of 3.1 (step S): {@link #isMagnetic} is the 3.0 rule of {@code Telekinesis}, moved here word for word,
 * plus a boss whose {@link BossHooks#isMetal} says so (every hook answers false until lane TK sets the bosses' answers),
 * so nothing behaves differently yet. Lane MAG widens it to everything with metal in it (D-M 2.1 to 2.5): gear in any slot,
 * metal blocks falling or lifted, metal projectiles in flight, the metal bodies, Loki's illusions never.
 */
public final class Metal {

    /**
     * Whether Magneto's telekinesis can take this entity: Iron Golems, minecarts, the Sentries, dropped metal, any creature
     * wearing Iron, Chainmail or Gold armor, an Iron Spider, and a boss whose hook says it is metal.
     */
    public static boolean isMagnetic(Entity e) {
        if (e.getType().is(ModTags.MAGNETIC_ENTITIES) || SpiderServer.isIronSpider(e)) {
            return true;
        }
        if (e instanceof BossHooks boss && boss.isMetal()) {
            return true; // Ultron's vibranium, Thanos's gold armour: the boss says so (lane TK)
        }
        if (e instanceof ItemEntity item) {
            return item.getItem().is(ModTags.MAGNETIC_ITEMS) || item.getItem().is(ModTags.MAGNETIC_ARMOR);
        }
        if (e instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor() && living.getItemBySlot(slot).is(ModTags.MAGNETIC_ARMOR)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether a grip of Magneto's on this entity holds on, asked every tick of the grip (where 3.0 asked {@link
     * #isMagnetic}). Today the same rule; lane MAG keeps a caught metal projectile held once it is no longer in flight
     * (D-M 3.5, R9).
     */
    public static boolean staysMagnetic(Entity e) {
        return isMagnetic(e);
    }

    /** A projectile still flying: not stuck in the ground, not a trident flying home, not caught by Magneto. */
    public static boolean inFlight(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow && (arrow.inGround || arrow.isNoPhysics())) {
            return false;
        }
        return !(projectile instanceof FishingHook) && !MagnetoAbilities.isHeld(projectile);
    }

    /** Whether an item is metal: in {@code enhanced:magnetic} or {@code enhanced:magnetic_armor}. */
    public static boolean isMetalItem(ItemStack stack) {
        return stack.is(ModTags.MAGNETIC_ITEMS) || stack.is(ModTags.MAGNETIC_ARMOR);
    }

    /** Whether a block is metal: in {@code enhanced:magnetic_blocks} (the metal ores among them). */
    public static boolean isMetalBlock(BlockState state) {
        return state.is(ModTags.MAGNETIC_BLOCKS);
    }

    /**
     * Whether the metal of this entity is its body, not only what it wears (an Iron Golem, a Sentry, the Hex's Vision, a
     * boss whose hook says it is metal): the strip and the pull tell it apart from a creature in metal gear. Minecarts are
     * metal but no body. Lane MAG adds the metal heroes (Vision, the Ultron hero, the Iron Spider; D-M 2.3 row 4).
     */
    public static boolean isMetalBody(Entity e) {
        return (e.getType().is(ModTags.MAGNETIC_ENTITIES) && !(e instanceof AbstractMinecart))
                || (e instanceof BossHooks boss && boss.isMetal());
    }

    /**
     * Whether this body is conjured, so that its gear is a copy and it is never metal (Loki's illusions, {@code
     * enhanced:conjured_bodies}). Always false until lane MAG reads the tag (D-M 2.5).
     */
    public static boolean isConjured(Entity e) {
        return false;
    }

    /**
     * Whether what {@code entity} has in {@code slot} is real gear, which taking it gives: false for gear that never drops
     * (a Horseman's, D-M 2.5), which crumbles when it is torn off. Always true until lane MAG reads the drop chance.
     */
    public static boolean isRealGear(LivingEntity entity, EquipmentSlot slot) {
        return true;
    }

    private Metal() {
    }
}
