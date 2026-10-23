package com.kolodkingal.enhanced.platform.ext;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The loader's item hooks the mod's items answer (port P0, PLAN2 3.3): an item implements the interface of each hook
 * it overrides. Each method has the exact signature of NeoForge's {@code IItemExtension} method of the same name, so
 * on NeoForge the item's method overrides that one too and NeoForge calls it as before; another loader calls it
 * itself (Fabric: mixins, P1). Only vanilla types appear. One interface per hook (or pair of hooks), so an item
 * implements just the ones it answers: an interface whose method the item does not write is a compile error.
 */
public final class ItemExt {
    private ItemExt() {
    }

    /** Each tick of the item lying in the world; true stops the vanilla tick of the item entity. */
    public interface EntityItemUpdate {
        boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity);
    }

    /** The item's use stops (released, or interrupted) after {@code count} ticks left. */
    public interface StopUsing {
        void onStopUsing(ItemStack stack, LivingEntity entity, int count);
    }

    /** Chest gear that flies like an elytra. */
    public interface ElytraFlight {
        boolean canElytraFly(ItemStack stack, LivingEntity entity);

        /** Each tick of a flight; false ends it. */
        boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks);
    }

    /** Whether the item may go into {@code armorType}. */
    public interface Equip {
        boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity);
    }

    /** The wear the item really takes of {@code amount}. */
    public interface DamageItem {
        <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity,
                                               Consumer<Item> onBroken);
    }

    /** Whether the hand redraws (the re-equip bob) when the held stack changes. Client side. */
    public interface ReequipAnimation {
        boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged);
    }

    /** The stack's durability (for an item whose durability is a config option). */
    public interface MaxDamage {
        int getMaxDamage(ItemStack stack);
    }

    /** The armour layer's texture for {@code entity} (null: the material's own). Client side. */
    public interface ArmorTexture {
        @Nullable
        ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer,
                                         boolean innerModel);
    }
}
