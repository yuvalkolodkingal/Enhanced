package com.kolodkingal.enhanced.client.death;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.mixin.client.DeathScreenAccessor;
import com.kolodkingal.enhanced.network.DeathMeetingPayload;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ScreenEvent;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;


/**
 * The client's side of the meeting with Death (3.7, lane L8): the server's {@link DeathMeetingPayload.Offer} arrives
 * just before the death, and the vanilla death screen that opens then (or again, while he stays dead) is swapped for
 * {@link DeathMeetingScreen}. Without an offer (Death off on the server, hardcore, {@code doImmediateRespawn}), in a
 * hardcore world, or with {@code showDeathMeeting} off, vanilla's screen stays. Forgotten when he respawns or leaves.
 */
@Subscriber(side = Side.CLIENT)
public final class DeathMeetingClient {
    @Nullable
    private static DeathMeetingPayload.Offer offer;
    /** The cause of this death (vanilla's screen opened again while dead has none: the first one's is kept). */
    @Nullable
    private static Component cause;
    /** Rio's line for this death (one of {@link DeathMeetingScreen#LINES}), kept while the screen opens again. */
    private static int line = -1;

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

    @Subscribe
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
    @Subscribe
    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        forget();
    }

    @Subscribe
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        forget();
    }

    static void forget() {
        offer = null;
        cause = null;
        line = -1;
    }

    /** The cause vanilla's screen was given (a private field, read through an accessor), or null. */
    @Nullable
    private static Component causeOf(DeathScreen screen) {
        return ((DeathScreenAccessor) screen).enhanced$getCauseOfDeath();
    }
}
