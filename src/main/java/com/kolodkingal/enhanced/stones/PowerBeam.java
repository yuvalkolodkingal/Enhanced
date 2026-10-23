package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.StonePowers;
import com.kolodkingal.enhanced.thanos.ThanosEffects;
import com.kolodkingal.enhanced.thanos.ThanosSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Power Stone (3.6). Use (held), the <b>Purple Beam</b> (Eilon's first wish): a thick violet beam from the hand
 * (the shared {@link ContinuousBeam}, half as wide again as the others), range {@code [power_stone] beamRange}: the
 * creature on its line takes {@code beamDamage} every half second, and every {@code explodeEvery} ticks the energy
 * bursts where it ends: {@code explosionDamage} to everyone the holder may hurt within {@code explosionRadius}, thrown
 * away, a purple shock ring. Blocks break only if {@code terrainDamage} is on (off by default) and the gamerule {@code
 * mobGriefing} and every protection allow it ({@link BlockRights#mayBlast}): a small crater, never fire. The stone
 * overheats after {@code beamMaxTicks} (a long cooldown; letting go early, a shorter one); purple veins crawl up the
 * arm while it pours ({@link Live#POWER_VEINS}). Sneak + use, the <b>Power Bolt</b> ("a small beam, like Thanos"):
 * Thanos's Power Blast with the stone's numbers. Server only.
 */
public final class PowerBeam {
    /** Ticks a hit of the beam waits for the next. */
    static final int BEAM_INTERVAL = 10;
    /** Hunger of a tick of the beam, and of one bolt. */
    static final float BEAM_EXHAUSTION = 0.05F;
    static final float BOLT_EXHAUSTION = 1.0F;
    /** How hard the beam pushes the one who fires it back, every few ticks. */
    static final double RECOIL = 0.12;
    /** No block harder than this breaks in a crater (obsidian, bedrock...). */
    static final float CRATER_MAX_HARDNESS = 30.0F;

    static BeamSpec spec() {
        return new BeamSpec(StoneBeams.POWER, StonesConfig.POWER_BEAM_RANGE.get(),
                StonesConfig.POWER_BEAM_DAMAGE.get().floatValue(), BEAM_INTERVAL, BEAM_EXHAUSTION,
                ModDamageTypes.POWER_STONE_BEAM, null);
    }

    // ---- the Purple Beam ----

    static void beamStart(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        if (ContinuousBeam.isFiring(player)) {
            return; // (another beam of his burns: one at a time)
        }
        StoneUse.begin(cast);
        ContinuousBeam.start(player, StoneBeams.POWER, StonesConfig.POWER_BEAM_RANGE.get());
        cast.data().set(Live.POWER_VEINS, true);
        StoneKind.POWER.voice().use(player.serverLevel(), player.position(), 0.9F, 0.9F);
    }

    static boolean beamTick(StoneUse.Hold hold) {
        ServerPlayer player = hold.cast.player();
        if (hold.ticks > StonesConfig.POWER_BEAM_MAX_TICKS.get()) {
            player.displayClientMessage(Component.translatable("message.enhanced.stones.power.overheat"), true);
            return false;
        }
        if (!ContinuousBeam.isFiring(player)) {
            return false;
        }
        ContinuousBeam.tick(player, spec());
        ServerLevel level = player.serverLevel();
        int every = Math.max(1, StonesConfig.POWER_EXPLODE_EVERY.get());
        if (hold.ticks % every == 0) {
            explodeAtEnd(player, level);
        }
        if (hold.ticks % 5 == 0) {
            Vec3 back = player.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize().scale(-RECOIL);
            player.setDeltaMovement(player.getDeltaMovement().add(back));
            player.hurtMarked = true;
        }
        return true;
    }

    /** Where the beam ends now: a creature or a block (null in empty air or at a Chaos Shield, which takes it). */
    private static Vec3 end(ServerPlayer player, ServerLevel level) {
        HitResult hit = Raycasts.any(player, StonesConfig.POWER_BEAM_RANGE.get(), ContinuousBeam.targets(player), 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        Vec3 end = hit.getLocation();
        return WandaAbilities.shieldAcross(level, player, player.getEyePosition(), end, 1.0F) != null ? null : end;
    }

    /** A burst where the beam ends: the splash, the shock ring, and a small crater if the server allows one. */
    static void explodeAtEnd(ServerPlayer player, ServerLevel level) {
        Vec3 end = end(player, level);
        if (end == null) {
            return;
        }
        float radius = StonesConfig.POWER_EXPLOSION_RADIUS.get().floatValue();
        StonePowers.splash(level, player, end, radius, StonesConfig.POWER_EXPLOSION_DAMAGE.get().floatValue(),
                ModDamageTypes.POWER_STONE_EXPLOSION, 0.8, 0.3, player, false);
        ThanosEffects.shockRing(level, end, radius, 6, StoneKind.POWER.color());
        level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        level.sendParticles(new EnergyParticleOptions(StoneKind.POWER.color(), 1.6F), end.x, end.y, end.z, 10, 0.4,
                0.4, 0.4, 0.15);
        level.playSound(null, end.x, end.y, end.z, ModSounds.POWER_EXPLOSION.get(), SoundSource.PLAYERS, 1.2F,
                0.9F + level.random.nextFloat() * 0.2F);
        Net.sendToPlayersNear(level, null, end.x, end.y, end.z, 64,
                new StoneFxPayload(StoneFxPayload.POWER_BLAST, player.getId(), List.of(end), Math.round(radius * 10)));
        crater(player, level, end);
    }

    /**
     * A small crater at {@code at}, only with {@code terrainDamage} on, {@code mobGriefing} on and every block's
     * protection allowing it; never fire, never the hardest blocks.
     *
     * @return how many blocks broke
     */
    public static int crater(ServerPlayer player, ServerLevel level, Vec3 at) {
        if (!StonesConfig.POWER_TERRAIN_DAMAGE.get()) {
            return 0;
        }
        double radius = StonesConfig.POWER_CRATER_RADIUS.get();
        int reach = Mth.ceil(radius);
        BlockPos centre = BlockPos.containing(at);
        int broken = 0;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, -reach, -reach), centre.offset(reach, reach,
                reach))) {
            if (Vec3.atCenterOf(pos).distanceToSqr(at) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness < 0.0F || hardness > CRATER_MAX_HARDNESS
                    || !BlockRights.mayBlast(player, level, pos)) {
                continue;
            }
            if (level.destroyBlock(pos.immutable(), true, player)) {
                broken++;
            }
        }
        return broken;
    }

    static void beamEnd(StoneUse.Hold hold, boolean released) {
        ServerPlayer player = hold.cast.player();
        ContinuousBeam.stop(player);
        hold.cast.data().set(Live.POWER_VEINS, false);
        boolean hot = hold.ticks > StonesConfig.POWER_BEAM_MAX_TICKS.get();
        hold.cast.startCooldown(released && !hot ? StonesConfig.POWER_RELEASE_COOLDOWN.get()
                : StonesConfig.POWER_OVERHEAT_COOLDOWN.get());
    }

    // ---- the Power Bolt ----

    /**
     * A violet bolt along his look, to the first creature or block: the creature takes the bolt and is thrown, and the
     * energy bursts where it ends (everyone else near takes the splash). A complete Chaos Shield stops it and wears.
     */
    static void bolt(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = player.serverLevel();
        double range = StonesConfig.POWER_BOLT_RANGE.get();
        StoneAim.Aim aim = StoneAim.aim(player, range, 0.3, target -> StoneAim.mayStrike(player, target));
        Vec3 hand = StoneAim.hand(player);
        Vec3 end = aim.point();
        cast.startCooldown(StonesConfig.POWER_BOLT_COOLDOWN.get());
        player.causeFoodExhaustion(BOLT_EXHAUSTION);
        ThanosSounds.CLENCH.play(level, player.position());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POWER_BOLT.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        StoneKind.POWER.voice().use(level, player.position(), 0.6F, 1.0F);
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, hand, end, 1.0F);
        if (shield != null) {
            end = shield.point();
            if (shield.owner() instanceof ServerPlayer wanda && player.canHarmPlayer(wanda)) {
                WandaAbilities.wear(wanda, StonesConfig.POWER_BOLT_DAMAGE.get().floatValue());
            }
            level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.2F), end.x, end.y, end.z, 6,
                    0.15, 0.15, 0.15, 0.12);
        } else {
            LivingEntity target = aim.creature();
            if (target != null && StonePowers.hurt(level, player, target, ModDamageTypes.STONE_POWER,
                    StonesConfig.POWER_BOLT_DAMAGE.get().floatValue(), player)) {
                StonePowers.shove(target, hand, 1.2, 0.2, player);
            }
            if (target != null || aim.block()) {
                float splash = StonesConfig.POWER_BOLT_SPLASH.get().floatValue();
                for (LivingEntity e : StonePowers.around(level, player, end, 2.5, e -> e != target)) {
                    if (StonePowers.hurt(level, player, e, ModDamageTypes.STONE_BLAST, splash, player)) {
                        StonePowers.shove(e, end, 0.8, 0.3, player);
                    }
                }
                ThanosEffects.shockRing(level, end, 2.5F, 6, StoneKind.POWER.color());
                level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0, 0, 0, 0);
            }
        }
        ThanosEffects.beam(level, player, hand, end, 0.4F, 6, StoneKind.POWER.color());
    }

    private PowerBeam() {
    }
}
