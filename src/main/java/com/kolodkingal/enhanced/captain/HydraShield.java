package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.power.Characters;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * (3.19) The Hydra Shield: the Vibranium Shield as Red Skull and Captain Hydra carry it, black, with Hydra's red skull and
 * tentacles on it ({@code textures/entity/captain/hydra_shield.png}) and its own name. It is the same shield (it blocks,
 * flies and comes back as Cap's does): what it is is written on the stack, so it looks so wherever it is drawn, in a
 * hand, on a back, in flight, in a chest. A Hydra character's shields become Hydra's; Captain America's own become his
 * again.
 */
public final class HydraShield {
    private static final String MARK = "enhanced_hydra_shield";
    /** How often a hero's shields are looked over (ticks). */
    private static final int EVERY = 10;

    private HydraShield() {
    }

    public static boolean is(ItemStack stack) {
        if (!ShieldRules.isVibranium(stack)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(MARK);
    }

    /** Makes the shield Hydra's, or Cap's again. */
    public static void set(ItemStack stack, boolean hydra) {
        if (!ShieldRules.isVibranium(stack) || is(stack) == hydra) {
            return;
        }
        if (hydra) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(MARK, true));
            stack.set(DataComponents.ITEM_NAME, Component.translatable("item.enhanced.hydra_shield"));
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(MARK));
            stack.remove(DataComponents.ITEM_NAME);
        }
    }

    /** What the shields of this hero are: Hydra's for Red Skull and Captain Hydra, Cap's for Captain America; else null. */
    public static Boolean wanted(PowerData data) {
        if (data.power() != Power.CAPTAIN_AMERICA) {
            return null;
        }
        return Characters.hydra(data);
    }

    /** Looks over the shields the player carries, now and then. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % EVERY != 0) {
            return;
        }
        Boolean hydra = wanted(Powers.get(player));
        if (hydra == null) {
            return;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModItems.VIBRANIUM_SHIELD.get())) {
                set(stack, hydra);
            }
        }
    }
}
