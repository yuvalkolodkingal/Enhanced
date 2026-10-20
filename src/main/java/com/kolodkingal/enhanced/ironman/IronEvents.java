package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.item.IronSuitItem;
import com.kolodkingal.enhanced.item.NanoArcReactorItem;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Iron Man's event hooks (3.8, lane L3). Priorities as {@code docs/HEROES-AND-BOSSES.md} lists them:
 * <ul> <li>{@link LivingIncomingDamageEvent}, HIGH: no fall damage from the Mark 1's crash landing; NORMAL: during a
 * suit-up the blow is cut by {@code suitUpDamageFactor} (never to nothing), and the Mark 1's Brace cuts it by {@code
 * braceReduction};</li> <li>{@link LivingFallEvent}: the crash landing takes no fall at all;</li> <li>{@link
 * LivingChangeTargetEvent}, NORMAL: a mob the Mark 3's flares blinded cannot pick him again for a while;</li>
 * <li>{@link LivingEntityUseItemEvent.Start}: no item is used during a suit-up or a take-off;</li> <li>{@link
 * LivingEquipmentChangeEvent}: the Nano Arc Reactor put on the chest makes its wearer Iron Man (PLAN 5.2);</li>
 * <li>{@link ItemAttributeModifierEvent}: a classic suit's armour, toughness and knockback resistance are the server's
 * ({@code [mark_*]}) and stand for the whole set.</li> </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class IronEvents {
    private static final ResourceLocation CHEST_ARMOR = ResourceLocation.withDefaultNamespace("armor.chestplate");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFallDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypeTags.IS_FALL)
                && Mark1Moves.crashing(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Mark1Moves.crashing(player)) {
            event.setDamageMultiplier(0.0F);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || Powers.power(player) != Power.IRON_MAN) {
            return;
        }
        float amount = SuitPhases.protect(player, event.getAmount());
        if (IronSuits.state(player).has(IronManState.BRACING)) {
            amount = Mark1Moves.braced(amount);
        }
        if (amount != event.getAmount()) {
            event.setAmount(amount);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && event.getEntity() instanceof Mob mob && Mark3Moves.flared(mob, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && IronSuits.state(player).inPhase()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEquip(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.CHEST && event.getEntity() instanceof ServerPlayer player
                && event.getTo().getItem() instanceof NanoArcReactorItem
                && !(event.getFrom().getItem() instanceof NanoArcReactorItem)) {
            NanoArcReactorItem.wornBy(player);
        }
    }

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof IronSuitItem suit) || !suit.kind().classic()) {
            return;
        }
        double armor;
        double toughness;
        double knockback;
        switch (suit.kind()) {
            case MARK_1 -> {
                armor = IronManAbilities.cfg(IronManConfig.MARK1_ARMOR, 18.0);
                toughness = IronManAbilities.cfg(IronManConfig.MARK1_TOUGHNESS, 3.0);
                knockback = IronManAbilities.cfg(IronManConfig.MARK1_KNOCKBACK_RESISTANCE, 0.5);
            }
            case MARK_3 -> {
                armor = IronManAbilities.cfg(IronManConfig.MARK3_ARMOR, 16.0);
                toughness = IronManAbilities.cfg(IronManConfig.MARK3_TOUGHNESS, 2.0);
                knockback = IronManAbilities.cfg(IronManConfig.MARK3_KNOCKBACK_RESISTANCE, 0.2);
            }
            case MARK_5 -> {
                armor = IronManAbilities.cfg(IronManConfig.MARK5_ARMOR, 12.0);
                toughness = IronManAbilities.cfg(IronManConfig.MARK5_TOUGHNESS, 1.0);
                knockback = IronManAbilities.cfg(IronManConfig.MARK5_KNOCKBACK_RESISTANCE, 0.1);
            }
            default -> {
                armor = IronManAbilities.cfg(IronManConfig.HULKBUSTER_ARMOR, 20.0);
                toughness = IronManAbilities.cfg(IronManConfig.HULKBUSTER_TOUGHNESS, 12.0);
                knockback = IronManAbilities.cfg(IronManConfig.HULKBUSTER_KNOCKBACK_RESISTANCE, 1.0);
            }
        }
        event.replaceModifier(Attributes.ARMOR, new AttributeModifier(CHEST_ARMOR, armor,
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
        event.replaceModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(CHEST_ARMOR, toughness,
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
        event.replaceModifier(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(CHEST_ARMOR, knockback,
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
    }

    private IronEvents() {
    }
}
