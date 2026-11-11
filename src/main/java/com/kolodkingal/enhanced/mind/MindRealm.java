package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Vision's mind (3.12), {@code enhanced:vision_mind}: an island of 110 by 100 blocks for each player who comes in, with
 * Vision's house in the middle ({@link MindBuilder}). Plot {@code n} lies at x = n × {@link #SPACING}. {@link #enter}
 * remembers where the player stood ({@link MindProgress}), builds his island the first time and puts its people in
 * their places; {@link #leave} takes him back. Someone who logs in inside is sent back too.
 */
public final class MindRealm {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Enhanced.id("vision_mind"));
    public static final int SPACING = 512;
    public static final int GROUND = 64;

    private MindRealm() {
    }

    public static boolean in(ServerPlayer player) {
        return player.level().dimension() == LEVEL;
    }

    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(LEVEL);
    }

    /** The middle of plot {@code plot}, at ground level. */
    public static BlockPos origin(int plot) {
        return new BlockPos(plot * SPACING, GROUND, 0);
    }

    /** The plot whose island holds {@code pos}. */
    public static int plotAt(BlockPos pos) {
        return Math.floorDiv(pos.getX() + SPACING / 2, SPACING);
    }

    /** The player's plot, handed out on his first visit. */
    static int plotOf(ServerPlayer player) {
        MindProgress progress = MindProgress.of(player);
        if (progress.plot() < 0) {
            progress.setPlot(Plots.get(player.server).next());
        }
        return progress.plot();
    }

    /** Takes the player into his mind's island, at its gate. */
    public static boolean enter(ServerPlayer player) {
        ServerLevel mind = level(player.server);
        if (mind == null || in(player) || !player.isAlive() || player.isSpectator()) {
            if (mind == null) {
                player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_world"), true);
            }
            return false;
        }
        int plot = plotOf(player);
        MindProgress progress = MindProgress.of(player);
        progress.setReturn(player.level().dimension().location().toString(), player.getX(), player.getY(), player.getZ(),
                player.getYRot());
        BlockPos origin = origin(plot);
        MindBuilder.ensure(mind, origin, progress);
        player.stopRiding();
        Vec3 at = Vec3.atBottomCenterOf(MindBuilder.arrival(origin));
        player.teleportTo(mind, at.x, at.y, at.z, 180.0F, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        keepPeople(mind, plot, progress);
        mind.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
        player.displayClientMessage(Component.translatable("message.enhanced.mind.enter"), true);
        return true;
    }

    /** Takes the player back where he came in from (or, if that is gone, to his respawn point). */
    public static void leave(ServerPlayer player) {
        MindTalk.forget(player);
        MindProgress progress = MindProgress.of(player);
        CompoundTag back = progress.returnPoint();
        progress.clearReturn();
        ServerLevel home = back.contains("Dim")
                ? player.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(back.getString("Dim"))))
                : null;
        if (home != null) {
            Vec3 spot = EternityRealm.safeSpot(home, player, new Vec3(back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z")));
            if (spot != null) {
                player.teleportTo(home, spot.x, spot.y, spot.z, back.getFloat("Yaw"), 0.0F);
                landed(player);
                return;
            }
        }
        DimensionTransition spawn = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        Vec3 at = spawn.pos();
        player.teleportTo(spawn.newLevel(), at.x, at.y, at.z, spawn.yRot(), spawn.xRot());
        landed(player);
    }

    private static void landed(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    /** Everyone of the plot in their place: made if missing, the extra ones sent away. */
    static void keepPeople(ServerLevel mind, int plot, MindProgress progress) {
        BlockPos origin = origin(plot);
        AABB area = new AABB(origin).inflate(70, 80, 70);
        List<MindNpcEntity> present = mind.getEntitiesOfClass(MindNpcEntity.class, area);
        Map<MindNpcEntity.Role, Integer> counts = new EnumMap<>(MindNpcEntity.Role.class);
        for (MindNpcEntity npc : present) {
            counts.merge(npc.role(), 1, Integer::sum);
        }
        for (MindBuilder.Spot spot : MindBuilder.people(origin, progress)) {
            int have = counts.getOrDefault(spot.role(), 0);
            if (have > 0) {
                counts.put(spot.role(), have - 1);
                continue;
            }
            MindNpcEntity npc = ModEntities.MIND_NPC.get().create(mind);
            if (npc == null) {
                continue;
            }
            npc.setRole(spot.role());
            npc.setPlot(plot);
            npc.setAblaze(spot.role() == MindNpcEntity.Role.TORCH && !progress.torchDone());
            Vec3 at = spot.at();
            npc.moveTo(at.x, at.y, at.z, spot.yaw(), 0.0F);
            npc.setYHeadRot(spot.yaw());
            npc.setYBodyRot(spot.yaw());
            mind.addFreshEntity(npc);
        }
        // the ones who should no longer be (the floors below, after the Source Code)
        for (MindNpcEntity npc : present) {
            if (progress.torchDone() && (npc.role() == MindNpcEntity.Role.ULTRON || npc.role() == MindNpcEntity.Role.TORCH
                    || npc.role() == MindNpcEntity.Role.HORTON)) {
                npc.discard();
            }
        }
    }

    /** Ticked for each player inside: no one stays in someone else's mind, and the void sends him home. */
    public static void tick(ServerPlayer player) {
        if (!in(player)) {
            return;
        }
        if (!MindProgress.of(player).hasReturn() || player.getY() < 0) {
            leave(player);
        }
    }

    /** The plots handed out (kept with the mind's own level). */
    public static final class Plots extends SavedData {
        private int next;

        static Plots get(MinecraftServer server) {
            ServerLevel overworld = server.overworld();
            return overworld.getDataStorage().computeIfAbsent(new Factory<>(Plots::new, Plots::load, null),
                    "enhanced_vision_mind");
        }

        private static Plots load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            Plots plots = new Plots();
            plots.next = tag.getInt("Next");
            return plots;
        }

        int next() {
            int plot = next++;
            setDirty();
            return plot;
        }

        @Override
        public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            tag.putInt("Next", next);
            return tag;
        }
    }
}
