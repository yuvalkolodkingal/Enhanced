package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Eternity's altar's block entity (3.8, PLAN 10.3): the traveller while the altar charges, and a copy of his key that
 * floats and spins over it (drawn by the client's {@code AltarRenderer}; the key itself stays in his hand). The charge
 * lasts {@code [eternity] altarChargeTicks}; Sneak, walking more than {@link #REACH} from the altar, letting the key
 * out of his hands or leaving the world cancels it; at its end {@link EternityRealm#enter} takes him alone. One
 * traveller at a time. The charge is not saved (a reload ends it); the key's copy and the start are synced to the
 * clients.
 */
public class EternityAltarBlockEntity extends BlockEntity {
    /** How far (blocks, from the altar's middle, across) the traveller may stand while it charges. */
    public static final double REACH = 3.0;

    @Nullable
    private UUID traveller;
    private ItemStack key = ItemStack.EMPTY;
    /** The game time the charge began. */
    private long start;

    public EternityAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ETERNITY_ALTAR.get(), pos, state);
    }

    /** The key floating over the altar (empty when it is quiet). */
    public ItemStack key() {
        return key;
    }

    /** The game time the charge began. */
    public long start() {
        return start;
    }

    public boolean charging() {
        return traveller != null || !key.isEmpty();
    }

    @Nullable
    public UUID traveller() {
        return traveller;
    }

    /**
     * The player starts the charge with this key (the altar's checks done).
     *
     * @return false if someone else is already being taken
     */
    public boolean begin(ServerPlayer player, ItemStack held) {
        if (traveller != null && !traveller.equals(player.getUUID())) {
            return false;
        }
        if (traveller != null) {
            return true;
        }
        ServerLevel level = (ServerLevel) this.level;
        traveller = player.getUUID();
        key = held.copyWithCount(1);
        start = level.getGameTime();
        changed();
        Vec3 at = Vec3.atCenterOf(worldPosition);
        level.playSound(null, at.x, at.y, at.z, ModSounds.ALTAR_PLACE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, at.x, at.y, at.z, ModSounds.ALTAR_RISE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        Net.sendToPlayersNear(level, null, at.x, at.y, at.z, 64,
                new EternityFxPayload(EternityFxPayload.ALTAR_CHARGE, player.getId(), worldPosition, chargeTicks()));
        return true;
    }

    /** The charge ends without a journey (cancelled, or he went). */
    public void end() {
        traveller = null;
        key = ItemStack.EMPTY;
        changed();
    }

    /** The charge's length, read when used ({@code [eternity] altarChargeTicks}). */
    public static int chargeTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? EternityConfig.ALTAR_CHARGE_TICKS.get() : 60;
    }

    /** One server tick: the column of stars rises; the traveller is checked; at the end he goes. */
    public static void serverTick(net.minecraft.world.level.Level world, BlockPos pos, BlockState state,
                                  EternityAltarBlockEntity altar) {
        if (altar.traveller == null || !(world instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(altar.traveller);
        Vec3 middle = Vec3.atBottomCenterOf(pos);
        String cancel = null;
        if (player == null || player.level() != level || !player.isAlive() || player.isSpectator()) {
            cancel = "";
        } else if (player.isShiftKeyDown()) {
            cancel = "message.enhanced.eternity.cancelled";
        } else if (Math.hypot(player.getX() - middle.x, player.getZ() - middle.z) > REACH + 0.5) {
            cancel = "message.enhanced.eternity.too_far";
        } else if (EternityAltarBlock.keyHand(player) == null) {
            cancel = "message.enhanced.eternity.needs_key";
        }
        if (cancel != null) {
            altar.end();
            if (player != null && !cancel.isEmpty()) {
                player.displayClientMessage(Component.translatable(cancel).withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        long elapsed = level.getGameTime() - altar.start;
        int charge = chargeTicks();
        float progress = Mth.clamp(elapsed / (float) charge, 0.0F, 1.0F);
        if (elapsed % 2 == 0) {
            // The column of stars rises from the altar, higher as the charge grows.
            double top = 1.2 + progress * 6.0;
            for (int i = 0; i < 3; i++) {
                double y = 1.1 + level.random.nextDouble() * top;
                double a = level.random.nextDouble() * Math.PI * 2;
                double r = 0.25 + level.random.nextDouble() * 0.35;
                level.sendParticles(ParticleTypes.END_ROD, middle.x + Math.cos(a) * r, pos.getY() + y,
                        middle.z + Math.sin(a) * r, 0, 0.0, 0.08, 0.0, 1.0);
            }
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 3,
                    0.4, 0.8, 0.4, 0.02);
        }
        if (elapsed >= charge) {
            altar.end();
            EternityRealm.enter(player, pos);
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ---- what the clients see: the floating key and the start ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!key.isEmpty()) {
            tag.put("Key", key.save(registries));
        }
        tag.putLong("Start", start);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        key = tag.contains("Key") ? ItemStack.parseOptional(registries, tag.getCompound("Key")) : ItemStack.EMPTY;
        start = tag.getLong("Start");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
