package com.kolodkingal.enhanced.beam;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.network.BeamPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The server side of a continuous beam (3.2): the logic Billy's Hex Beam runs on, shared so that another source (the
 * Darkhold's purple beam) only brings a {@link BeamStyle} and, every tick, a {@link BeamSpec}.
 *
 * <p>The one who fires calls {@link #start} when the key goes down, {@link #tick} every tick while it is held (the
 * caller decides when to stop: key up, hunger, a spectator) and {@link #stop} when it ends. While it lasts {@link
 * Live#BEAMING} is set for the clients (the pose, the sound), and a {@link BeamPayload} is sent to everyone near when it
 * starts, every few ticks and when it stops. A tick: it costs hunger, it looks along the shooter's view for the first
 * creature (the crosshair; blocks stop it), a complete Chaos Shield in the way stops it at its surface and takes the
 * strength the hit would have (see {@link WandaAbilities#shieldAcross}), and otherwise, every {@link BeamSpec#interval()}
 * ticks, the creature takes the damage (the game's own rules for who may hurt whom: PvP, Creative, bosses' caps). Sparks
 * fly where it ends.
 */
public final class ContinuousBeam {
    /** Ticks between the payloads that say the beam goes on. */
    public static final int KEEP_ALIVE_TICKS = 8;

    private static final class State {
        long nextHit;
        long lastSent = Long.MIN_VALUE;
        BeamStyle style;
        float range;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** What a beam can hit: creatures, parts of the Ender Dragon and End Crystals, as Wanda's beam can (alive, pickable). */
    public static Predicate<Entity> targets(Entity shooter) {
        return e -> e != shooter && WandaAbilities.BEAM_TARGET.test(e) && e.isAlive() && !shooter.isPassengerOfSameVehicle(e);
    }

    /** Whether this player's beam is on. */
    public static boolean isFiring(ServerPlayer shooter) {
        return STATES.containsKey(shooter.getUUID());
    }

    /** The key went down: the beam begins (nothing happens if it already burns). */
    public static void start(ServerPlayer shooter, BeamStyle style, double range) {
        if (isFiring(shooter)) {
            return;
        }
        Powers.get(shooter).set(Live.BEAMING, true);
        State state = new State();
        state.style = style;
        state.range = (float) range;
        STATES.put(shooter.getUUID(), state);
        send(shooter, style, state.range, true);
    }

    /** The beam ends (the key came up, hunger, the power was lost): the clients are told. */
    public static void stop(ServerPlayer shooter) {
        State state = STATES.remove(shooter.getUUID());
        Powers.get(shooter).set(Live.BEAMING, false);
        if (state != null) {
            send(shooter, state.style, state.range, false);
        }
    }

    /** Forgets the beam without a word (the player logged out, the server stopped). */
    public static void forget(ServerPlayer shooter) {
        STATES.remove(shooter.getUUID());
    }

    public static void clear() {
        STATES.clear();
    }

    /** One tick of a beam that burns. */
    public static void tick(ServerPlayer shooter, BeamSpec spec) {
        State state = STATES.get(shooter.getUUID());
        if (state == null) {
            return;
        }
        ServerLevel level = shooter.serverLevel();
        long now = level.getGameTime();
        state.style = spec.style();
        state.range = (float) spec.range();
        if (now - state.lastSent >= KEEP_ALIVE_TICKS) {
            send(shooter, spec.style(), (float) spec.range(), true);
            state.lastSent = now;
        }
        shooter.causeFoodExhaustion(spec.exhaustion());

        HitResult hit = Raycasts.any(shooter, spec.range(), targets(shooter), 1.0F);
        Vec3 end = hit.getLocation();
        // A complete Chaos Shield in the way stops the beam at its surface, and pays for it with its strength.
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, shooter, shooter.getEyePosition(), end, 1.0F);
        if (shield != null) {
            if (now >= state.nextHit) {
                state.nextHit = now + spec.interval();
                shielded(level, shooter, shield, spec);
            }
            return;
        }
        if (hit instanceof EntityHitResult entityHit && now >= state.nextHit) {
            state.nextHit = now + spec.interval();
            Entity target = entityHit.getEntity();
            target.hurt(ModDamageTypes.source(level, spec.damageType(), shooter, shooter), spec.damage());
            if (spec.onHit() != null) {
                spec.onHit().accept(shooter, target instanceof PartEntity<?> part ? part.getParent() : target);
            }
            Vec3 at = entityHit.getLocation();
            level.sendParticles(new EnergyParticleOptions(spec.style().main(), 1.0F), at.x, at.y, at.z, 4, 0.12, 0.12, 0.12, 0.1);
            level.sendParticles(new EnergyParticleOptions(spec.style().core(), 0.7F), at.x, at.y, at.z, 2, 0.06, 0.06, 0.06, 0.05);
        }
        // Sparks and a crackle where it lands, on a block or a creature (not in the empty air).
        if (hit.getType() != HitResult.Type.MISS) {
            if (now % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(spec.style().main(), 1.0F), end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.1);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 2, 0.12, 0.12, 0.12, 0.2);
            }
            if (now % 7 == 0) {
                level.playSound(null, end.x, end.y, end.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.2F, 2.0F);
            }
        }
    }

    /** The beam meets a Chaos Shield: it spends itself on the sphere and takes the hit's damage off the shield's strength. */
    private static void shielded(ServerLevel level, ServerPlayer shooter, WandaAbilities.ShieldStop shield, BeamSpec spec) {
        Vec3 at = shield.point();
        level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.2F), at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.12);
        level.sendParticles(new EnergyParticleOptions(spec.style().main(), 0.9F), at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.1);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.25F, 1.9F);
        // (With PvP off the beam would not have hurt her: it costs the shield nothing either.)
        if (shield.owner() instanceof ServerPlayer wanda && shooter.canHarmPlayer(wanda)) {
            WandaAbilities.wear(wanda, spec.damage());
        }
    }

    private static void send(ServerPlayer shooter, BeamStyle style, float range, boolean on) {
        ServerLevel level = shooter.serverLevel();
        PacketDistributor.sendToPlayersNear(level, null, shooter.getX(), shooter.getY(), shooter.getZ(), 96.0,
                new BeamPayload(shooter.getId(), style.id(), range, on));
    }

    private ContinuousBeam() {
    }
}
