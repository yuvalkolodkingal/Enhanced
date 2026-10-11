package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The single handler of the boss bars, for a boss that wants a bar of its own (only Thanos does: his six stone
 * sockets). The boss names its first bar with a translatable key ({@code boss.enhanced.thanos}), registers a {@link
 * Drawer} for that key here, and the vanilla bar of every event with that name is replaced by the drawer's. Every other
 * bar is untouched, and so are the bars of a boss that is not tracked on the client in time to draw (the drawer is
 * asked only for the bars it is given).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class BossBarOverride {

    /** Draws a boss's own bar in the place of the vanilla one. */
    @FunctionalInterface
    public interface Drawer {
        /**
         * Draws the bar.
         *
         * @param x the left edge of the vanilla bar, and {@code y} its top
         * @return how far down the next bar goes: the height this one took (the vanilla bar takes 19)
         */
        int draw(GuiGraphics graphics, LerpingBossEvent bar, int x, int y);
    }

    private static final Map<String, Drawer> DRAWERS = new HashMap<>();

    /** Draws every bar named by the translation key {@code nameKey} with {@code drawer}. */
    public static void register(String nameKey, Drawer drawer) {
        DRAWERS.put(nameKey, drawer);
    }

    /** The translation key a bar's name is made of, or the empty string for a name that is not a translation. */
    public static String nameKey(Component name) {
        return name.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }

    /** The drawer registered for a bar with this name, or null. */
    @Nullable
    public static Drawer drawerFor(Component name) {
        return DRAWERS.get(nameKey(name));
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Drawer drawer = drawerFor(event.getBossEvent().getName());
        if (drawer != null) {
            event.setCanceled(true);
            event.setIncrement(drawer.draw(event.getGuiGraphics(), event.getBossEvent(), event.getX(), event.getY()));
        }
    }

    private BossBarOverride() {
    }
}
