package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

/**
 * The Hexenbesen (3.17): a broom of light that carries its caster for a while (the {@code broom} blessing of
 * {@link MagicZones}). While it lasts he may fly ({@code PowerTicker} lets him), faster than a hero who flies on his own,
 * and every client draws the broom under him, sitting astride it ({@link Live#BROOM}).
 */
public final class Broom {
    /** How fast a broom flies a rider who cannot fly on his own. */
    public static final float SPEED = 0.09F;

    private Broom() {
    }

    /** Whether he rides a broom now. */
    public static boolean riding(ServerPlayer player) {
        return MagicZones.has(player, "broom") && !MagicZones.silenced(player);
    }

    /** The broom comes: he takes off on it. */
    static void mount(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        if (player.onGround()) {
            player.setDeltaMovement(new Vec3(0, 0.5, 0));
            player.hurtMarked = true;
        }
        player.onUpdateAbilities();
    }

    /** Every tick of every player: the broom is drawn while he flies on it. */
    public static void tick(ServerPlayer player) {
        PowerData data = Powers.get(player);
        boolean on = riding(player) && player.getAbilities().flying;
        if (data.is(Live.BROOM) != on) {
            data.set(Live.BROOM, on);
        }
        if (!riding(player) && MagicZones.has(player, "broom_was")) {
            MagicZones.unbuff(player, "broom_was");
            player.resetFallDistance();
        } else if (riding(player)) {
            MagicZones.mark(player, "broom_was", 40);
        }
    }
}
