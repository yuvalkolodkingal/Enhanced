package com.kolodkingal.enhanced.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cagliostro's Time Loop (3.17), Doctor Strange's bargain: the first cast marks the moment (where he stands, his health);
 * cast it again within twenty seconds, or take a blow that would kill him, and he is back in that moment.
 */
public final class TimeLoop {
    private record Mark(ResourceKey<Level> dim, Vec3 at, float health, long until) {
    }

    private static final Map<UUID, Mark> MARKS = new HashMap<>();
    /** How long a mark holds (20 s). */
    public static final int HOLD_TICKS = 400;

    private TimeLoop() {
    }

    static boolean cast(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Mark mark = MARKS.get(player.getUUID());
        if (mark != null && level.getGameTime() < mark.until() && mark.dim() == level.dimension()) {
            back(player, mark);
            return true;
        }
        MARKS.put(player.getUUID(), new Mark(level.dimension(), player.position(), player.getHealth(), level.getGameTime() + HOLD_TICKS));
        MagicFx.send(level, MagicFx.CIRCLE, player.position(), List.of(), 0x4FD068, 0xC8FFD0, 1.4F, HOLD_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("message.enhanced.spell.time_loop.marked").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    private static void back(ServerPlayer player, Mark mark) {
        ServerLevel level = player.serverLevel();
        MARKS.remove(player.getUUID());
        MagicFx.send(level, MagicFx.VORTEX, player.position(), List.of(), 0x4FD068, 0xC8FFD0, 1.4F, 16);
        player.teleportTo(mark.at().x, mark.at().y, mark.at().z);
        player.setHealth(Math.max(player.getHealth(), mark.health()));
        player.resetFallDistance();
        player.clearFire();
        MagicFx.send(level, MagicFx.STARBURST, mark.at().add(0, 1, 0), List.of(), 0x4FD068, 0xC8FFD0, 3.0F, 14);
        level.playSound(null, mark.at().x, mark.at().y, mark.at().z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("message.enhanced.spell.time_loop.back").withStyle(ChatFormatting.GREEN), true);
    }

    /** A blow that would kill him: the loop takes him back instead (true: the blow is undone). */
    public static boolean saves(ServerPlayer player) {
        Mark mark = MARKS.get(player.getUUID());
        if (mark == null || player.level().getGameTime() >= mark.until() || mark.dim() != player.level().dimension()) {
            return false;
        }
        back(player, mark);
        return true;
    }

    public static void forget(UUID player) {
        MARKS.remove(player);
    }

    public static void clear() {
        MARKS.clear();
    }
}
