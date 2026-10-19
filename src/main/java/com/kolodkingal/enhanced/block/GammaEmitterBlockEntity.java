package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.hulk.GammaExposure;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Gamma Emitter's block entity (3.6): its warm-up, its beam and its rest. The phase and when it began are sent to
 * the clients (the renderer draws the coils' glow and the beam from them); the beam's work on the players in it is
 * {@link GammaExposure}.
 */
public class GammaEmitterBlockEntity extends BlockEntity {
    /** The phases: resting, warming up, beaming. */
    public static final int IDLE = 0;
    public static final int WARMUP = 1;
    public static final int BEAM = 2;
    /** The beam's height (blocks): a player's height. */
    public static final double BEAM_HEIGHT = 2.0;

    private int phase = IDLE;
    private long phaseStart;
    private long restUntil;

    public GammaEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAMMA_EMITTER.get(), pos, state);
    }

    // ---- the numbers ----

    private static boolean loaded() {
        return com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded();
    }

    public static int warmupTicks() {
        return loaded() ? HulkConfig.WARMUP_TICKS.get() : 60;
    }

    public static int beamTicks() {
        return loaded() ? HulkConfig.BEAM_TICKS.get() : 60;
    }

    public static int beamLength() {
        return loaded() ? HulkConfig.BEAM_LENGTH.get() : 6;
    }

    public static int cooldown() {
        return loaded() ? HulkConfig.EMITTER_COOLDOWN.get() : 200;
    }

    // ---- what it is doing ----

    public int phase() {
        return phase;
    }

    public long phaseStart() {
        return phaseStart;
    }

    /** The way the beam runs. */
    public Direction facing() {
        return getBlockState().getValue(GammaEmitterBlock.FACING);
    }

    /** The beam's space: from the cell in front of its face, {@code beamLength} blocks on, a player's height. */
    public AABB beamBox() {
        Direction d = facing();
        BlockPos first = worldPosition.relative(d);
        AABB box = new AABB(first);
        box = box.expandTowards(d.getStepX() * (beamLength() - 1.0), 0.0, d.getStepZ() * (beamLength() - 1.0));
        return box.setMaxY(box.minY + BEAM_HEIGHT);
    }

    /** Where the beam leaves the lens. */
    public Vec3 lens() {
        Direction d = facing();
        return Vec3.atCenterOf(worldPosition).add(d.getStepX() * 0.5, 0.0, d.getStepZ() * 0.5);
    }

    /**
     * A click ({@code who}) or a redstone signal (null): it warms up, unless it is already working or resting.
     *
     * @return whether it started
     */
    public boolean start(@Nullable Player who) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        long now = server.getGameTime();
        if (phase != IDLE) {
            return false;
        }
        if (now < restUntil) {
            if (who != null) {
                who.displayClientMessage(Component.translatable("message.enhanced.gamma.resting",
                        (int) Math.ceil((restUntil - now) / 20.0)).withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        setPhase(WARMUP, now);
        Vec3 at = Vec3.atCenterOf(worldPosition);
        server.playSound(null, at.x, at.y, at.z, ModSounds.GAMMA_EMITTER_WARMUP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private void setPhase(int phase, long now) {
        this.phase = phase;
        this.phaseStart = now;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            boolean lit = phase != IDLE;
            if (state.getValue(GammaEmitterBlock.LIT) != lit) {
                level.setBlock(worldPosition, state.setValue(GammaEmitterBlock.LIT, lit), Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GammaEmitterBlockEntity emitter) {
        if (!(level instanceof ServerLevel server) || emitter.phase == IDLE) {
            return;
        }
        long now = server.getGameTime();
        long t = now - emitter.phaseStart;
        Vec3 lens = emitter.lens();
        if (emitter.phase == WARMUP) {
            if (t % 4 == 0) {
                server.sendParticles(new EnergyParticleOptions(0x7CFF4A, 0.8F), lens.x, lens.y, lens.z, 3, 0.25, 0.25,
                        0.25, 0.02);
            }
            if (t < warmupTicks()) {
                return;
            }
            // (The beam's first tick is this one: it shines for exactly beamTicks ticks.)
            emitter.setPhase(BEAM, now);
            GammaExposure.beamStarts(server, pos);
            t = 0;
        }
        if (t % 40 == 0) {
            server.playSound(null, lens.x, lens.y, lens.z, ModSounds.GAMMA_EMITTER_BEAM.get(), SoundSource.BLOCKS, 1.0F,
                    1.0F);
        }
        AABB box = emitter.beamBox();
        if (t % 2 == 0) {
            Vec3 c = box.getCenter();
            server.sendParticles(new EnergyParticleOptions(0x9CFF5A, 1.0F), c.x, c.y, c.z, 4,
                    box.getXsize() * 0.35, 0.6, box.getZsize() * 0.35, 0.02);
        }
        GammaExposure.tick(server, pos, box, now);
        if (t + 1 >= beamTicks()) {
            emitter.setPhase(IDLE, now);
            emitter.restUntil = now + cooldown();
            GammaExposure.beamEnds(server, pos);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GammaEmitterBlockEntity emitter) {
    }

    // ---- saving and syncing ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("phase", phase);
        tag.putLong("phase_start", phaseStart);
        tag.putLong("rest_until", restUntil);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        phase = tag.getInt("phase");
        phaseStart = tag.getLong("phase_start");
        restUntil = tag.getLong("rest_until");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("phase", phase);
        tag.putLong("phase_start", phaseStart);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
