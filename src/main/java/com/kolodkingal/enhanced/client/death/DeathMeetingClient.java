package com.kolodkingal.enhanced.client.death;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.network.DeathMeetingPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * The client's side of the meeting with Death (3.7, lane L8): the server's {@link DeathMeetingPayload.Offer} arrives
 * just before the death, and the vanilla death screen that opens then (or again, while he stays dead) is swapped for
 * {@link DeathMeetingScreen}. Without an offer (Death off on the server, hardcore, {@code doImmediateRespawn}), in a
 * hardcore world, or with {@code showDeathMeeting} off, vanilla's screen stays. Forgotten when he respawns or leaves.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class DeathMeetingClient {
    @Nullable
    private static DeathMeetingPayload.Offer offer;
    /** The cause of this death (vanilla's screen opened again while dead has none: the first one's is kept). */
    @Nullable
    private static Component cause;
    /** Rio's line for this death (one of {@link DeathMeetingScreen#LINES}), kept while the screen opens again. */
    private static int line = -1;
    @Nullable
    private static final Field CAUSE = causeField();

    private DeathMeetingClient() {
    }

    /** The server offers the meeting (the death follows at once). */
    public static void receive(DeathMeetingPayload.Offer payload) {
        offer = payload;
        cause = null;
        line = -1;
    }

    /** Whether the meeting is offered now (the server's offer, not hardcore, and the client's switch on). */
    static boolean offered(Minecraft minecraft) {
        return offer != null && minecraft.level != null && minecraft.player != null
                && !minecraft.level.getLevelData().isHardcore() && EnhancedClientConfig.showDeathMeeting();
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getNewScreen() instanceof DeathScreen death) || death instanceof DeathMeetingScreen
                || !offered(minecraft)) {
            return;
        }
        Component said = causeOf(death);
        if (said != null) {
            cause = said;
        }
        if (line < 0) {
            // The first time this death opens it: her line, and her sound (leaves and a harp, gentle).
            line = RandomSource.create().nextInt(DeathMeetingScreen.LINES);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.RIO_BLESSING.get(), 1.0F, 0.7F));
        }
        event.setNewScreen(new DeathMeetingScreen(cause, offer.nether(), offer.fireSeconds(), line));
    }

    /** A respawn (the new body): she is gone. */
    @SubscribeEvent
    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        forget();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        forget();
    }

    static void forget() {
        offer = null;
        cause = null;
        line = -1;
    }

    /** The cause vanilla's screen was given (a private field), or null. */
    @Nullable
    private static Component causeOf(DeathScreen screen) {
        if (CAUSE == null) {
            return null;
        }
        try {
            return (Component) CAUSE.get(screen);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    @Nullable
    private static Field causeField() {
        try {
            Field field = DeathScreen.class.getDeclaredField("causeOfDeath");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Enhanced.LOGGER.warn("The death screen's cause is not readable; Death's screen shows none", e);
            return null;
        }
    }
}
