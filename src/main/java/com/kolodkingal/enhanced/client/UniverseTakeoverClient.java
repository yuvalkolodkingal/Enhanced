package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.network.UniverseTakeoverPayload;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;

/**
 * The sky of a universe Ultimate Wanda has taken (3.19): it turns a deep red over two seconds for everyone in it, and back
 * when she lets it go. The sky dome takes the colour of the fog, made to cover it whole; the fog of the land turns red too.
 * Nothing is drawn: only two colours change, so it costs nothing. The server says when the universe the player stands in
 * changes ({@link UniverseTakeoverPayload}).
 */
@Subscriber(side = Side.CLIENT)
public final class UniverseTakeoverClient {
    /** The red of the sky she brings, and the darker red when her runes burn in it. */
    private static final int SKY = 0xB0102A;
    private static final int RUNE_SKY = 0x7A0618;
    /** How long the sky takes to turn (ms). */
    private static final long TURN_MS = 2000L;

    private static int state = UniverseTakeoverPayload.CLEAR;
    /** The state before the last change, and when it changed (ms), for the turn of the sky. */
    private static int before = UniverseTakeoverPayload.CLEAR;
    private static long changedAt;

    private UniverseTakeoverClient() {
    }

    /** The server's word: what the universe the player stands in is now. */
    public static void accept(int newState) {
        if (newState == state) {
            return;
        }
        before = state;
        state = newState;
        changedAt = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && before == UniverseTakeoverPayload.CLEAR) {
            mc.player.playSound(SoundEvents.AMBIENT_CAVE.value(), 0.6F, 0.5F);
        } else if (mc.player != null && newState == UniverseTakeoverPayload.RUNES) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY() + 20.0, mc.player.getZ(), SoundEvents.EVOKER_PREPARE_WOLOLO,
                    SoundSource.AMBIENT, 0.8F, 0.5F, false);
        }
    }

    /** How red the sky is now (0 to 1). */
    private static float redness() {
        float t = Mth.clamp((System.currentTimeMillis() - changedAt) / (float) TURN_MS, 0.0F, 1.0F);
        float from = before == UniverseTakeoverPayload.CLEAR ? 0.0F : 1.0F;
        float to = state == UniverseTakeoverPayload.CLEAR ? 0.0F : 1.0F;
        return Mth.lerp(t, from, to);
    }

    private static int colour() {
        return state == UniverseTakeoverPayload.RUNES || state == UniverseTakeoverPayload.CLEAR && before == UniverseTakeoverPayload.RUNES
                ? RUNE_SKY : SKY;
    }

    @Subscribe
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        float k = redness();
        if (k <= 0.001F) {
            return;
        }
        int c = colour();
        event.setRed(Mth.lerp(k, event.getRed(), ((c >> 16) & 0xFF) / 255.0F));
        event.setGreen(Mth.lerp(k, event.getGreen(), ((c >> 8) & 0xFF) / 255.0F));
        event.setBlue(Mth.lerp(k, event.getBlue(), (c & 0xFF) / 255.0F));
    }

    /** The sky dome is lost in the red fog (the whole of it is the fog's colour); the land's fog keeps its distance. */
    @Subscribe
    public static void onFog(ViewportEvent.RenderFog event) {
        float k = redness();
        if (k <= 0.001F || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_SKY) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float wanted = Mth.lerp(k, far, 2.0F);
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(wanted);
        event.setCanceled(true);
    }

    @Subscribe
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        state = UniverseTakeoverPayload.CLEAR;
        before = UniverseTakeoverPayload.CLEAR;
        changedAt = 0L;
    }
}
