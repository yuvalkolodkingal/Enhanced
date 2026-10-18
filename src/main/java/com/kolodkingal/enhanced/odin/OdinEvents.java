package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Odin's own game-bus events (3.5), at the priorities of {@code docs/HEROES-AND-BOSSES.md}: his complete Odinforce Shield
 * takes the blows meant for him before Wanda's branch does (highest), and while he sleeps the Odinsleep his hands do
 * nothing (no blow, no use of an item or a block).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class OdinEvents {

    /** His shield, complete, takes the blow (or what it can of it). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Powers.get(player).has(HeroAbility.ODIN_ODINFORCE)) {
            OdinAbilities.guard(player, event);
        }
    }

    /** Asleep, he strikes nothing. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (asleep(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Asleep, he uses nothing. */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (asleep(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (asleep(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean asleep(Player player) {
        PowerData data = Powers.get(player);
        return data.toggle(Toggle.ODINSLEEP) && data.has(HeroAbility.ODIN_COMMAND);
    }

    private OdinEvents() {
    }
}
