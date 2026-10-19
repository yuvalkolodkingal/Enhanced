package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.WeaponXPodBlock;
import com.kolodkingal.enhanced.block.WeaponXPodBlockEntity;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Weapon X Pod's treatment (3.7, PLAN 5.5): how Deadpool is made, in the X-Men lower lab (the default of open
 * question 1). A <b>powerless</b> player who stands in the pod (within {@link #INSIDE} of the middle of its lower
 * block, his feet at its floor: the pod's open inside, or pressed to its glass) right-clicks it: the hatch closes (he
 * cannot walk or jump), {@code podTicks} of "treatment" (bubbles, a rising hum, a heartbeat, the action bar counting
 * "Treatment… 5… 4…"), then a red flash: <b>Deadpool</b> (the title, the first joke). Sneaking, stepping out, or
 * anything that takes him out of the pod cancels it; a hero is refused ("this one already has powers"); it works for
 * anyone any number of times ({@code podAwakens} off: it hums and does nothing). MCU: the Workshop's chamber that
 * forced his mutation awake (Deadpool, 2016), as a kid-friendly machine (no pain, no suffocation shown).
 *
 * <p>The pod block (lane L4) hands its right-click here ({@link #use}); the treatment ticks from every player's tick
 * ({@link DeadpoolServer#everyPlayerTick}). The pod's look while it works is the lab's: {@link LabFxPayload#POD_START}
 * and {@link LabFxPayload#POD_DONE} at its lower block.
 */
public final class PodTreatment {
    /** How far (blocks, horizontally) from the middle of the pod's lower block a player counts as inside. */
    public static final double INSIDE = 0.9;
    /** The modifiers that hold him still while the hatch is closed. */
    private static final ResourceLocation HOLD_SPEED = Enhanced.id("weapon_x_pod_hold_speed");
    private static final ResourceLocation HOLD_JUMP = Enhanced.id("weapon_x_pod_hold_jump");

    /** A treatment under way: in which pod (its lower block) and since when. */
    record Treatment(BlockPos pod, long start) {
    }

    private static final Map<UUID, Treatment> UNDER_WAY = new HashMap<>();

    /** A right-click on the pod at {@code pos} by {@code player}. */
    public static InteractionResult use(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockPos pod = lower(level, pos);
        if (pod == null) {
            return InteractionResult.CONSUME;
        }
        if (UNDER_WAY.containsKey(player.getUUID())) {
            return InteractionResult.CONSUME;
        }
        if (!DeadpoolAbilities.cfg(DeadpoolConfig.POD_AWAKENS, true)) {
            tell(player, "message.enhanced.deadpool.pod_idle");
            return InteractionResult.CONSUME;
        }
        if (Powers.get(player).power() != Power.NONE) {
            tell(player, "message.enhanced.deadpool.pod_hero");
            level.playSound(null, pod, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.8F, 0.6F);
            return InteractionResult.CONSUME;
        }
        if (!inside(player, pod)) {
            tell(player, "message.enhanced.deadpool.pod_step_in");
            return InteractionResult.CONSUME;
        }
        for (Treatment other : UNDER_WAY.values()) {
            if (other.pod().equals(pod)) {
                tell(player, "message.enhanced.deadpool.pod_busy");
                return InteractionResult.CONSUME;
            }
        }
        long now = level.getGameTime();
        UNDER_WAY.put(player.getUUID(), new Treatment(pod.immutable(), now));
        hold(player, true);
        PacketDistributor.sendToPlayersNear(level, null, pod.getX() + 0.5, pod.getY(), pod.getZ() + 0.5, 64.0,
                new LabFxPayload(LabFxPayload.POD_START, pod));
        running(level, pod, true);
        tell(player, "message.enhanced.deadpool.pod_start");
        return InteractionResult.CONSUME;
    }

    /** The pod's lower block, or null if there is no pod at {@code pos}. */
    static BlockPos lower(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WeaponXPodBlock)) {
            return null;
        }
        return state.hasProperty(WeaponXPodBlock.HALF) && state.getValue(WeaponXPodBlock.HALF) == DoubleBlockHalf.UPPER
                ? pos.below() : pos;
    }

    /** Whether the player stands in the pod whose lower block is {@code pod}. */
    public static boolean inside(ServerPlayer player, BlockPos pod) {
        double dx = player.getX() - (pod.getX() + 0.5);
        double dz = player.getZ() - (pod.getZ() + 0.5);
        double dy = player.getY() - pod.getY();
        return dx * dx + dz * dz <= INSIDE * INSIDE && dy >= -0.5 && dy <= 1.0;
    }

    /** Whether a treatment of the player's is under way. */
    public static boolean underWay(ServerPlayer player) {
        return UNDER_WAY.containsKey(player.getUUID());
    }

    /** Every player, every tick: the treatment under way goes on, ends in Deadpool, or is cancelled. */
    static void tick(ServerPlayer player) {
        Treatment treatment = UNDER_WAY.get(player.getUUID());
        if (treatment == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pod = treatment.pod();
        if (!player.isAlive() || player.isShiftKeyDown() || lower(level, pod) == null || !pod.equals(lower(level, pod))
                || !inside(player, pod) || Powers.get(player).power() != Power.NONE) {
            cancel(player, true);
            return;
        }
        long now = level.getGameTime();
        int ticks = DeadpoolAbilities.cfg(DeadpoolConfig.POD_TICKS, 100);
        long t = now - treatment.start();
        Vec3 middle = Vec3.atBottomCenterOf(pod);
        if (t % 4 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, middle.x, middle.y + 0.3, middle.z, 3, 0.25, 0.2,
                    0.25, 0.05);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.35F, 1.0F, 0.45F), 0.8F), middle.x,
                    middle.y + 1.0, middle.z, 2, 0.25, 0.6, 0.25, 0.0);
        }
        if (t % 20 == 0 && t < ticks) {
            int left = (int) Math.max(1, (ticks - t + 19) / 20);
            player.displayClientMessage(Component.translatable("hud.enhanced.deadpool.treatment", left)
                    .withStyle(ChatFormatting.GREEN), true);
            level.playSound(null, pod, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 0.5F,
                    1.0F + 0.25F * t / Math.max(1, ticks));
        }
        if (t >= ticks) {
            finish(player, treatment);
        }
    }

    /** The red flash: he is Deadpool. */
    private static void finish(ServerPlayer player, Treatment treatment) {
        UNDER_WAY.remove(player.getUUID());
        hold(player, false);
        ServerLevel level = player.serverLevel();
        BlockPos pod = treatment.pod();
        Powers.awaken(player, Power.DEADPOOL, DeadpoolAbilities.CLASSIC);
        PacketDistributor.sendToPlayersNear(level, null, pod.getX() + 0.5, pod.getY(), pod.getZ() + 0.5, 64.0,
                new LabFxPayload(LabFxPayload.POD_DONE, pod));
        running(level, pod, false);
        Vec3 at = player.position();
        level.sendParticles(new DustParticleOptions(new Vector3f(0.95F, 0.12F, 0.2F), 2.0F), at.x, at.y + 1.0, at.z,
                60, 0.5, 0.8, 0.5, 0.0);
        level.playSound(null, pod, ModSounds.WEAPON_X_POD_DONE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, at.x, at.y, at.z, ModSounds.DEADPOOL_AWAKEN.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        DeadpoolLines.say(player, DeadpoolLines.Group.AWAKEN, true);
    }

    /** The treatment ends early: the hatch opens, nothing changes. */
    static void cancel(ServerPlayer player, boolean tell) {
        Treatment treatment = UNDER_WAY.remove(player.getUUID());
        hold(player, false);
        if (treatment == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        PacketDistributor.sendToPlayersNear(level, null, treatment.pod().getX() + 0.5, treatment.pod().getY(),
                treatment.pod().getZ() + 0.5, 64.0, new LabFxPayload(LabFxPayload.POD_DONE, treatment.pod()));
        running(level, treatment.pod(), false);
        if (tell && player.isAlive()) {
            tell(player, "message.enhanced.deadpool.pod_cancel");
        }
    }

    /**
     * The pod's look (lane L4's block entity, synced): the hatch shut, the light quick, the bubbles and the hum while a
     * treatment runs; open and idle again when it ends.
     */
    private static void running(ServerLevel level, BlockPos pod, boolean on) {
        if (level.getBlockEntity(pod) instanceof WeaponXPodBlockEntity entity) {
            entity.setRunning(on);
        }
    }

    /** The hatch closed: he cannot walk or jump (a transient modifier). */
    private static void hold(ServerPlayer player, boolean on) {
        DeadpoolAbilities.setModifier(player, Attributes.MOVEMENT_SPEED, HOLD_SPEED, -1.0, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        DeadpoolAbilities.setModifier(player, Attributes.JUMP_STRENGTH, HOLD_JUMP, -1.0, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    /** He left the server, died or changed dimension. */
    static void forget(ServerPlayer player) {
        cancel(player, false);
    }

    /** He logged in: a hatch that a crash left closed is open (nothing is under way after a login). */
    static void onLogin(ServerPlayer player) {
        UNDER_WAY.remove(player.getUUID());
        hold(player, false);
    }

    static void clear() {
        UNDER_WAY.clear();
    }

    private PodTreatment() {
    }
}
