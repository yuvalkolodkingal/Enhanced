package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildPocketPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The builder's own Build Magic on the client (D-B 11.10): build mode, the chosen spell and style, the selection, the
 * ghost, the numbers, the hold timer, the server's last verdict and its marks, and the pocket's copy for the ghost. It
 * sends recipes and actions; the server decides.
 *
 * <p>A stub of the skeleton (3.1, step S): it keeps nothing yet. Build mode is what the server last synced
 * ({@link BuildView#active}, off until lane BS turns it on), and the only thing it sends is Undo and Redo (Ctrl + Z and
 * Ctrl + Y of a Scarlet Witch or Billy, R42), which the server's stub ignores. Lane BC fills it and moves the reading of
 * the keys into its {@code BuildInput}. Only ever loaded on a client.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class BuildClient {
    /** Whether {@code power} builds: the Scarlet Witch and Billy, the two heroes of Build Magic. */
    public static boolean isBuilder(Power power) {
        return power == Power.SCARLET_WITCH || power == Power.BILLY;
    }

    /**
     * Whether the local player is in build mode: what the {@link BuildKeys.Context#BUILD} keys and the panel's sixth line
     * follow. Lane BC: its own mode, which answers at once when Y is tapped.
     */
    public static boolean buildMode() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getExistingData(ModAttachments.BUILD_VIEW).map(BuildView::active).orElse(false);
    }

    /** The server's verdict on a cast, a preflight, an undo or a redo. Lane BC: keep it, show its marks and counts. */
    public static void onVerdict(BuildVerdictPayload payload) {
    }

    /** One part of the copy in the pocket. Lane BC: gather the parts ({@link BuildPocketPayload#join}), mesh the ghost. */
    public static void onPocketPart(BuildPocketPayload payload) {
    }

    /** Forgets everything of the builder (at logout, and when a level unloads). Lane BC: called from {@code BuildInput}. */
    public static void clear() {
    }

    /**
     * Ctrl + Z and Ctrl + Y of a Scarlet Witch or Billy (not a Spectator, no screen open) send UNDO and REDO, in build mode
     * or out of it (R42). A Ctrl + Z that {@link BuildKeys#UNDO} takes never reaches the Vision order on Z.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean undo = clicked(BuildKeys.UNDO);
        boolean redo = clicked(BuildKeys.REDO);
        if (player == null || mc.level == null || mc.screen != null || player.isSpectator() || !isBuilder(Powers.power(player))) {
            return;
        }
        if (undo) {
            PacketDistributor.sendToServer(new BuildActionPayload(BuildActionPayload.UNDO, 0));
        }
        if (redo) {
            PacketDistributor.sendToServer(new BuildActionPayload(BuildActionPayload.REDO, 0));
        }
    }

    /** Whether {@code key} was pressed since the last tick (presses in the same tick count once). */
    private static boolean clicked(KeyMapping key) {
        boolean clicked = false;
        while (key.consumeClick()) {
            clicked = true;
        }
        return clicked;
    }

    private BuildClient() {
    }
}
