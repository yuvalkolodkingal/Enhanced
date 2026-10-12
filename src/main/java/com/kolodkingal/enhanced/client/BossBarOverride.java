package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
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

    /**
     * Where the boss bars drawn this frame end: the lowest point they reach, and the left edge of the widest of them
     * (its bar or its name). The hero's panel at the top left goes under them when it would run into them.
     */
    private static int barsBottom;
    private static int barsLeft = Integer.MAX_VALUE;

    /** The boss bars are about to be drawn: none is yet. */
    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.BOSS_OVERLAY.equals(event.getName())) {
            barsBottom = 0;
            barsLeft = Integer.MAX_VALUE;
        }
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Drawer drawer = drawerFor(event.getBossEvent().getName());
        if (drawer != null) {
            event.setCanceled(true);
            event.setIncrement(drawer.draw(event.getGuiGraphics(), event.getBossEvent(), event.getX(), event.getY()));
        }
        // (The name is drawn centred over the bar, 9 above it; the next bar's name begins where this one's space ends.)
        int nameWidth = Minecraft.getInstance().font.width(event.getBossEvent().getName());
        int centre = event.getX() + 91;
        barsLeft = Math.min(barsLeft, Math.min(event.getX(), centre - nameWidth / 2));
        barsBottom = Math.max(barsBottom, event.getY() + event.getIncrement() - 9);
    }

    /**
     * How far down a panel at the top left whose right edge is at {@code right} must start so as not to cover the boss
     * bars: 0 when there are none, or when it ends left of them.
     */
    public static int clearance(int right) {
        return barsBottom > 0 && right >= barsLeft - 2 ? barsBottom : 0;
    }

    private BossBarOverride() {
    }
}
