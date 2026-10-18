package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The only door of Asgard (3.5): nothing travels into or out of Asgard but by the Bifrost ({@link Bifrost#isMoving}).
 * Portals, the Book of the Vishanti, the Tesseract and other mods' teleports are refused; an operator's {@code /tp} and
 * {@code /execute in} stay free (a player with permission level 2). With {@code asgard.enabled} off the gate refuses
 * every way in, the Bifrost too (operators still may), and lets everyone out. A Nether portal cannot be lit in Asgard.
 * The Book of the Vishanti is refused before anything is spent ({@code PlaceBook.refusesAsgard}).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class AsgardGate {

    /** Whether the gate lets {@code entity} travel from {@code from} to {@code to}. */
    public static boolean allows(Entity entity, ResourceKey<Level> from, ResourceKey<Level> to) {
        boolean in = to == AsgardDimension.KEY;
        if (from == to || !in && from != AsgardDimension.KEY) {
            return true;
        }
        if (entity instanceof ServerPlayer player && player.hasPermissions(2)) {
            return true;
        }
        return Bifrost.isMoving(entity) && (!in || Bifrost.asgardEnabled());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (!allows(entity, entity.level().dimension(), event.getDimension())) {
            event.setCanceled(true);
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.enhanced.asgard.only_bifrost").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    /** No Nether portal is lit in Asgard. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPortal(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof Level level && AsgardDimension.is(level)) {
            event.setCanceled(true);
        }
    }

    private AsgardGate() {
    }
}
