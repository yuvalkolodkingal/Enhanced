package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.network.BeamPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StonePowers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;


/**
 * The held beams of the nanotech suits and the Hulkbuster (3.8, lane L4): the Mark 50's Unibeam (the first creature
 * along his look, as the classic suits'), the Mark 85's giant chest laser (PLAN 6.2: 1.5 wide, it pierces: every
 * creature along it) and the Hulkbuster's heavy Unibeam (PLAN 6.4: 2 wide, it pierces too). While one burns {@link
 * Live#BEAMING} is set and a {@link BeamPayload} of its style goes to everyone near when it starts, every few ticks and
 * when it stops (the client draws the wide ones from the chest: {@code client/ironman/nano/NanoBeamDrawers}). Each
 * costs suit energy every tick ({@link SuitEnergy#drain}) and ends at its longest time, when the energy runs out or the
 * key comes up.
 */
public final class NanoBeams {
    /** Which beam burns ({@link NanoState#beamKind}). */
    public static final int UNIBEAM = 1;
    public static final int CHEST_LASER = 2;
    public static final int HULKBUSTER_UNIBEAM = 3;
    /** Ticks between two hits of a beam (PLAN: "every 10 ticks"). */
    public static final int HIT_GAP = 10;

    /** The numbers of one beam. */
    public record Spec(int kind, BeamStyle style, double range, double width, float damage, double energyPerSecond,
                       int maxTicks, boolean pierces) {
    }

    /** Whether one of these beams burns for him. */
    public static boolean burning(ServerPlayer player) {
        NanoState state = NanoState.peek(player.getUUID());
        return state != null && state.beamSince != 0L;
    }

    /** The key went down: the beam begins (nothing when one burns already, or the energy is gone). */
    public static boolean start(ServerPlayer player, PowerData data, Spec spec) {
        NanoState state = NanoState.of(player.getUUID());
        if (state.beamSince != 0L || !SuitEnergy.drain(player, spec.energyPerSecond())) {
            return false;
        }
        long now = player.level().getGameTime();
        state.beamSince = now;
        state.beamKind = spec.kind();
        state.beamHitAt = Long.MIN_VALUE / 2;
        state.beamSent = now;
        state.beamHit.clear();
        data.set(Live.BEAMING, true);
        if (spec.kind() == UNIBEAM) {
            ContinuousBeam.start(player, spec.style(), spec.range());
        } else {
            payload(player, spec, true);
        }
        ServerLevel level = player.serverLevel();
        NanoTech.sound(level, NanoTech.chest(player), spec.kind() == CHEST_LASER ? ModSounds.CHEST_LASER.get()
                : ModSounds.UNIBEAM_START.get(), 1.0F, spec.kind() == HULKBUSTER_UNIBEAM ? 0.7F : 1.0F);
        return true;
    }

    /** One tick of the beam that burns: its cost, its hits, its end at its longest. */
    public static void tick(ServerPlayer player, PowerData data, Spec spec) {
        NanoState state = NanoState.peek(player.getUUID());
        if (state == null || state.beamSince == 0L || state.beamKind != spec.kind()) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - state.beamSince >= spec.maxTicks() || !SuitEnergy.drain(player, spec.energyPerSecond())) {
            stop(player, data);
            return;
        }
        if (spec.kind() == UNIBEAM) {
            ContinuousBeam.tick(player, new com.kolodkingal.enhanced.beam.BeamSpec(spec.style(), spec.range(),
                    spec.damage(), HIT_GAP, 0.0F, ModDamageTypes.UNIBEAM, null));
            return;
        }
        if (now - state.beamSent >= ContinuousBeam.KEEP_ALIVE_TICKS) {
            state.beamSent = now;
            payload(player, spec, true);
        }
        ServerLevel level = player.serverLevel();
        if (now - state.beamHitAt < HIT_GAP && now >= state.beamHitAt) {
            return;
        }
        state.beamHitAt = now;
        state.beamHit.clear();
        Vec3 from = NanoTech.chest(player);
        Vec3 look = player.getLookAngle();
        // The beam leaves the chest and goes where he looks (towards the crosshair's point at its range).
        Vec3 aim = player.getEyePosition().add(look.scale(spec.range())).subtract(from).normalize();
        StonePowers.BeamHit hit = StonePowers.beam(level, player, from, aim, spec.range(), spec.width(),
                spec.damage(), ModDamageTypes.UNIBEAM, spec.damage(), 0.0, state.beamHit, true);
        for (LivingEntity e : hit.hit()) {
            Vec3 at = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            level.sendParticles(new EnergyParticleOptions(spec.style().main(), 1.2F), at.x, at.y, at.z, 6, 0.2, 0.2,
                    0.2, 0.1);
        }
        Vec3 end = hit.end();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 4, 0.2, 0.2, 0.2, 0.2);
        if (hit.shield()) {
            // (3.8.2) It ran into a Chaos Shield (which paid for it in StonePowers.beam): a flash where it stopped.
            level.sendParticles(new EnergyParticleOptions(spec.style().core(), 1.4F), end.x, end.y, end.z, 10, 0.25,
                    0.25, 0.25, 0.12);
            level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.2F), end.x, end.y, end.z, 8,
                    0.25, 0.25, 0.25, 0.1);
            level.playSound(null, end.x, end.y, end.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F,
                    1.4F);
        }
        if ((now - state.beamSince) % 20 == 0) {
            NanoTech.sound(level, from, spec.kind() == CHEST_LASER ? ModSounds.CHEST_LASER.get()
                    : ModSounds.UNIBEAM.get(), 0.8F, spec.kind() == HULKBUSTER_UNIBEAM ? 0.7F : 1.0F);
        }
    }

    /** The beam ends (the key came up, its time, the energy, the suit off). */
    public static void stop(ServerPlayer player, PowerData data) {
        NanoState state = NanoState.peek(player.getUUID());
        if (state == null || state.beamSince == 0L) {
            return;
        }
        int kind = state.beamKind;
        state.beamSince = 0L;
        state.beamKind = 0;
        if (kind == UNIBEAM) {
            ContinuousBeam.stop(player);
        } else {
            data.set(Live.BEAMING, false);
            BeamStyle style = kind == CHEST_LASER ? AvengersBeams.CHEST_LASER : AvengersBeams.HULKBUSTER_UNIBEAM;
            PacketDistributor.sendToPlayersNear(player.serverLevel(), null, player.getX(), player.getY(),
                    player.getZ(), 96.0, new BeamPayload(player.getId(), style.id(), 0.0F, false));
        }
    }

    /**
     * 3.8.2: where a wide chest beam from {@code from} towards {@code aim} (the point at its range along his look)
     * ends: the first block, or the first wall before it (a complete Chaos Shield, a Hex Barrier...: the beam stops at
     * its surface, as {@link StonePowers#beam} stops it on the server). What the client draws it to ({@code
     * NanoBeamDrawers}). Both sides.
     */
    public static Vec3 reach(Level level, Player player, Vec3 from, Vec3 aim, float partialTick) {
        HitResult block = level.clip(new ClipContext(from, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? aim : block.getLocation();
        WandaAbilities.ShieldStop wall = WandaAbilities.shieldAcross(level, player, from, end, partialTick);
        return wall == null ? end : wall.point();
    }

    private static void payload(ServerPlayer player, Spec spec, boolean on) {
        PacketDistributor.sendToPlayersNear(player.serverLevel(), null, player.getX(), player.getY(), player.getZ(),
                96.0, new BeamPayload(player.getId(), spec.style().id(), (float) spec.range(), on));
    }

    private NanoBeams() {
    }
}
