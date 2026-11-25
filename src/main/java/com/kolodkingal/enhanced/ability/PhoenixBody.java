package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/**
 * (3.19.3) Jean Grey takes over bodies, for as long as she likes, on B (Mind Control): B at a player puts her mind in
 * their body; B at a creature takes it as her host as before, and B at a host of hers puts her mind in it. She steers
 * the body as Professor X does ({@link MindLink}), with no time limit and no distance limit in her world, and no telepath
 * keeps her out, except three minds that are closed to her: the Scarlet Witch's, Billy's and Spider-Man's. Magneto's
 * helmet still shuts a mind. B again brings her back. Her own body, left behind, is guarded by her force field meanwhile
 * ({@link PhoenixShield#guarded}), and a blow to it does not break the link.
 */
public final class PhoenixBody {
    /** How far she reaches for a body. */
    public static final double RANGE = 30.0;
    /** Her key for it: B (Mind Control). */
    public static final int KEY = 4;

    private PhoenixBody() {
    }

    /** Whether this player is a Phoenix whose links have no end of their own (Jean Grey herself). */
    public static boolean is(Player player) {
        return player != null && is(Powers.get(player));
    }

    public static boolean is(PowerData data) {
        return data.power() == Power.PHOENIX;
    }

    /** Whether this mind is one she can never enter: the Scarlet Witch's, Billy's or Spider-Man's. */
    public static boolean immune(Entity target) {
        if (!(target instanceof Player player)) {
            return false;
        }
        Power power = Powers.get(player).power();
        return power == Power.SCARLET_WITCH || power == Power.BILLY || power == Power.SPIDER_MAN;
    }

    /** B while she is in a body, or at a creature she already holds: back, or in. */
    static void press(ServerPlayer player) {
        if (MindLink.isLinked(player)) {
            MindLink.end(player.server, player.getUUID(), MindLink.End.LET_GO);
            return;
        }
        LivingEntity target = aimed(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.xavier.nothing"), true);
            return;
        }
        if (target instanceof net.minecraft.world.entity.Mob mob && PhoenixAbilities.isHostOf(player, mob)) {
            PhoenixAbilities.release(mob, false); // her host stops being one: her mind goes in instead
        }
        MindLink.take(player, target);
    }

    /** The body she looks at within {@link #RANGE} (before any block in the way), or null. */
    @org.jetbrains.annotations.Nullable
    public static LivingEntity aimed(ServerPlayer player) {
        EntityHitResult hit = Raycasts.entity(player, RANGE, 0.3, e -> e instanceof LivingEntity l && l != player
                && l.isAlive() && !l.isSpectator() && !(l instanceof ArmorStand) && !MindLink.isHost(l), 1.0F);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }
}
