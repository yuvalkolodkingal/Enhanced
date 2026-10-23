package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.build.BuildKeys;
import com.kolodkingal.enhanced.custom.HeroKeys;
import com.kolodkingal.enhanced.network.AbilityKeyPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 3.6, the Y-row generalisation on the client: for a player whose Y row is not Build Magic (Doctor Strange's Eye, a
 * custom hero's {@code strange.eye}: {@link HeroKeys#yRow}), the Y key ({@code BuildKeys.MAGIC}) goes to the server as
 * ability key {@link HeroKeys#Y}, down and up, with the Sneak it was pressed with; Build Magic keeps the key for Billy
 * and the Scarlet Witch ({@code BuildInput}). Called by {@code BuildInput} every client tick, with the key's click of
 * that tick.
 */
public final class YRowKey {
    private static boolean sentDown;

    public static void tick(Minecraft mc, LocalPlayer player, boolean clicked) {
        boolean usable = mc.screen == null && !player.isSpectator() && HeroKeys.yRow(Powers.get(player)) != null;
        boolean down = usable && BuildKeys.MAGIC.isDown();
        boolean sneak = mc.options.keyShift.isDown();
        if (usable && (down || clicked) && !sentDown) {
            Net.sendToServer(new AbilityKeyPayload(HeroKeys.Y, true, sneak));
            sentDown = true;
        }
        if (!down && sentDown) {
            Net.sendToServer(new AbilityKeyPayload(HeroKeys.Y, false, sneak));
            sentDown = false;
        }
    }

    private YRowKey() {
    }
}
