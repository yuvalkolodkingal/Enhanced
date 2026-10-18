package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Odin's vault under the throne room (3.5, {@code CityPlan.VAULT}): Gungnir's stand ({@code CityPlan.GUNGNIR_STAND}) and the
 * treasures of Asgard. Its door ({@code CityPlan.VAULT_DOOR}, the blocks of lane L1's template) stands shut while Odin
 * sleeps; Hela's first defeat wakes him ({@code AsgardState.odinAwake}) and lane L4's victory calls {@link #open}. The door
 * never shuts again. A door the victory could not reach (its chunk was not loaded) opens the first time a player comes
 * near while Odin is awake ({@link #watch}).
 *
 * <p><b>For later releases:</b> the vault keeps an empty place for another treasure, {@link #RESERVED_PEDESTAL} (the mirror
 * of Gungnir's stand across the vault; lane L1 leaves it and the two blocks above it air). 3.6 puts the Reality Stone on a
 * pedestal there. Code that wants to know when the vault opens adds itself to {@link #OPENED}.
 */
public final class Vault {
    /**
     * The empty place in the vault kept for another treasure (3.6: the Reality Stone's pedestal), on the vault's floor
     * opposite Gungnir's stand; it and the two blocks above it are air in 3.5.
     */
    public static final BlockPos RESERVED_PEDESTAL = new BlockPos(-6, 71, -122);
    /** Told (on the server, in Asgard's level) when the vault's door opens. */
    public static final List<Consumer<ServerLevel>> OPENED = new CopyOnWriteArrayList<>();
    /** How near the vault a player must be for the watch to look at its door, and how often it looks. */
    private static final double WATCH_REACH = 48.0;
    private static final int WATCH_EVERY = 100;

    /** Opens the vault's door in Asgard's level: the door's blocks are gone in a puff of golden dust. */
    public static void open(ServerLevel level) {
        if (isOpen(level)) {
            return;
        }
        boolean opened = false;
        for (BlockPos pos : BlockPos.betweenClosed(CityPlan.VAULT_DOOR.minX(), CityPlan.VAULT_DOOR.minY(), CityPlan.VAULT_DOOR.minZ(),
                CityPlan.VAULT_DOOR.maxX(), CityPlan.VAULT_DOOR.maxY(), CityPlan.VAULT_DOOR.maxZ())) {
            if (!level.isLoaded(pos) || level.getBlockState(pos).isAir()) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            opened = true;
        }
        if (!opened) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(CityPlan.VAULT_DOOR.getCenter());
        level.playSound(null, center.x, center.y, center.z, ModSounds.ODINSLEEP_END.get(), SoundSource.BLOCKS, 1.5F, 0.8F);
        for (Consumer<ServerLevel> listener : OPENED) {
            listener.accept(level);
        }
    }

    /** Whether the door is open: every loaded block of it is air (a door in unloaded chunks counts as shut). */
    public static boolean isOpen(ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(CityPlan.VAULT_DOOR.minX(), CityPlan.VAULT_DOOR.minY(), CityPlan.VAULT_DOOR.minZ(),
                CityPlan.VAULT_DOOR.maxX(), CityPlan.VAULT_DOOR.maxY(), CityPlan.VAULT_DOOR.maxZ())) {
            if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Every player, every tick: near the vault in Asgard with Odin awake, a door still shut is opened. */
    static void watch(ServerPlayer player) {
        if (player.tickCount % WATCH_EVERY != 0 || !AsgardDimension.in(player)) {
            return;
        }
        Vec3 door = Vec3.atCenterOf(CityPlan.VAULT_DOOR.getCenter());
        if (player.position().distanceToSqr(door) > WATCH_REACH * WATCH_REACH) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (AsgardState.get(level.getServer()).odinAwake() && !isOpen(level)) {
            open(level);
        }
    }

    private Vault() {
    }
}
