package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/**
 * Falcon's Exo-Wings, the API the shared seams call (3.8, PLAN 8.2-8.3): {@code PowerTicker.applyPassives} asks {@link
 * #canFly}; the serum asks {@link #carries} (the Falcon card is locked without the wings). The 3.8 skeleton writes the
 * plain reads and a neutral {@link #canFly}; lane L5 fills it (worn, not broken, not shielding) and keeps the
 * signatures.
 */
public final class FalconWings {
    /** Whether he carries the Exo-Wings anywhere: the inventory, the chest slot, the off hand (the serum's rule). */
    public static boolean carries(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.FALCON_EXO_WINGS.get()));
    }

    /** Whether the Exo-Wings are on his chest slot (every wing move needs them there). */
    public static boolean worn(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.FALCON_EXO_WINGS.get());
    }

    /**
     * Whether Falcon may fly now (PLAN 8.3). Skeleton: never (lane L5: wearing the wings, not broken, not shielding).
     */
    public static boolean canFly(ServerPlayer player) {
        return false;
    }

    private FalconWings() {
    }
}
