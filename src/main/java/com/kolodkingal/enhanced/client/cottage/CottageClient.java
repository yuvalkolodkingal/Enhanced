package com.kolodkingal.enhanced.client.cottage;

import com.kolodkingal.enhanced.client.EntityGlow;
import com.kolodkingal.enhanced.cottage.CottageProgress;
import com.kolodkingal.enhanced.cottage.Scratchy;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;

/**
 * The client of Agatha's Cottage (3.7, lane L6): Señor Scratchy glows (soft green) for a player who said he would
 * look for him, within {@link Scratchy#GLOW_RANGE} blocks, until he is home (the flag of the player's own synced
 * {@code COTTAGE} progress). Everything else of the cottage is drawn by the Asgardians' renderer and the world.
 */
public final class CottageClient {
    /** The glow's colour: Rio's garden green. */
    public static final int GLOW = 0x7FE07A;

    public static void setup() {
        EntityGlow.register(CottageClient::reveals, e -> GLOW);
    }

    static boolean reveals(Entity entity) {
        if (!(entity instanceof Rabbit rabbit) || !isScratchy(rabbit.getCustomName())) {
            return false;
        }
        Player me = Minecraft.getInstance().player;
        return me != null && ModAttachments.COTTAGE.get(me).has(CottageProgress.SCRATCHY_QUEST)
                && me.distanceToSqr(entity) <= Scratchy.GLOW_RANGE * Scratchy.GLOW_RANGE;
    }

    static boolean isScratchy(Component name) {
        return name != null && name.getContents() instanceof TranslatableContents t && Scratchy.NAME.equals(t.getKey());
    }

    private CottageClient() {
    }
}
