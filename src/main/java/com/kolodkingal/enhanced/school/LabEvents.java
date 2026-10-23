package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.LabDoors;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The game's side of the X-Men lower lab (3.7, lane L4): each player's tick finds the schools near him ({@link
 * LabBuilder#tick}), rides the lift ({@link Lift#tick}) and opens the lab's doors near him ({@link LabDoors#nearby});
 * the Overworld's tick takes the labs being built on ({@link LabBuilder#tickLevel}); and a player inside a built lab is
 * told so every {@link #AMBIENCE_EVERY} ticks (its soft machinery loop, {@link LabFxPayload#AMBIENCE}).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class LabEvents {
    /** How often a player is told whether he is inside a lab (the client stops the loop after 3 silent times). */
    public static final int AMBIENCE_EVERY = 40;

    private static final Set<UUID> INSIDE = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LabBuilder.tick(player);
        Lift.tick(player);
        LabDoors.nearby(player);
        if (player.tickCount % AMBIENCE_EVERY == 0) {
            ambience(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            LabBuilder.tickLevel(level);
        }
    }

    /** Tells {@code player} whether he is inside a built lab (only when that changes, and while it lasts). */
    static void ambience(ServerPlayer player) {
        boolean inside = !player.isSpectator() && insideLab(player.serverLevel(), player.blockPosition());
        boolean was = INSIDE.contains(player.getUUID());
        if (inside) {
            INSIDE.add(player.getUUID());
        } else {
            INSIDE.remove(player.getUUID());
        }
        if (inside || was) {
            Net.sendToPlayer(player, new LabFxPayload(LabFxPayload.AMBIENCE,
                    inside ? new BlockPos(0, 1, 0) : BlockPos.ZERO));
        }
    }

    /** Whether {@code pos} is inside the box of a built lab. */
    public static boolean insideLab(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }
        for (SchoolLabs.Lab lab : SchoolLabs.get(level).all()) {
            if (lab.state() == SchoolLabs.State.DONE && lab.site().contains(pos)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Lift.forget(player);
            INSIDE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        INSIDE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        LabBuilder.clear();
        Lift.clear();
        INSIDE.clear();
    }

    private LabEvents() {
    }
}
