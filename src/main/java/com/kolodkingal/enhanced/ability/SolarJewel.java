package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.SolarJewelConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.VisionQuest;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Solar Jewel Vision (3.16): Vision as his Source Code (in the VisionQuest suit) runs on the Solar Jewel in his brow,
 * as in the comics, instead of the Mind Stone. The jewel drinks in sunlight and stores it ({@link PowerData#ironCharge},
 * the hero's synced pool, which a change of power empties); by night he runs on what he stored.
 * <ul>
 *   <li>R, held: the Solar Beam, golden light from the jewel that sets what it strikes burning.</li>
 *   <li>Sneak + R: Jewel Overload: the light gathers in the jewel (gold rings close in on his brow), then everything he
 *   stored pours out at once in one great beam that strikes all in its path; the jewel is drained for a while.</li>
 *   <li>N, held: infrared and microwave beams from his eyes, heat that burns creatures and melts snow and ice.</li>
 *   <li>G, V and B stay as they are (flight, Phase, Density): the synthezoid body's, not the stone's.</li>
 * </ul>
 * The client draws all of it ({@code SolarJewelFx}) from the synced states {@link Live#SOLAR_EYES},
 * {@link Live#SOLAR_GATHERING} and {@link Live#SOLAR_OVERLOAD} and the pool.
 */
@com.kolodkingal.enhanced.platform.event.Subscriber
public final class SolarJewel {
    /** The gold of the jewel's light, for the server's particles. */
    public static final int GOLD = 0xFFC93A;
    public static final int WHITE_GOLD = 0xFFF3C2;
    public static final int HEAT = 0xFF5A1E;

    /** A player's Overload under way: the tick it began, the charge it took, and who it has struck. */
    private static final class Overload {
        final long start;
        final int charge;
        final Set<Integer> struck = new HashSet<>();

        Overload(long start, int charge) {
            this.start = start;
            this.charge = charge;
        }
    }

    private static final Map<UUID, Overload> OVERLOADS = new HashMap<>();
    /** When each player's jewel may overload again. */
    private static final Map<UUID, Long> DRAINED_UNTIL = new HashMap<>();

    private SolarJewel() {
    }

    public static boolean solar(PowerData data) {
        return VisionQuest.solar(data);
    }

    // ---- the jewel's light ----

    public static int charge(PowerData data) {
        return data.ironCharge();
    }

    private static boolean creative(ServerPlayer player) {
        return player.getAbilities().instabuild;
    }

    /** Spends {@code amount} of the stored light; false (and nothing spent) if the jewel has less. */
    static boolean spend(ServerPlayer player, PowerData data, int amount) {
        if (creative(player) || amount <= 0) {
            return true;
        }
        if (data.ironCharge() < amount) {
            return false;
        }
        data.setIronCharge(data.ironCharge() - amount);
        return true;
    }

    /** How much light he drinks in this tick where he stands: open daylight, daylight out of the sun, or none. */
    static int absorbing(ServerPlayer player) {
        return absorbing(player, SolarJewelConfig.ABSORB_SUN.get(), SolarJewelConfig.ABSORB_SHADE.get());
    }

    /** The jewel's rule with the rates of {@code sun} (open sky) and {@code shade}: Jim Hammond's solar charge reads it too (3.17). */
    static int absorbing(ServerPlayer player, int sun, int shade) {
        ServerLevel level = player.serverLevel();
        if (!level.dimensionType().hasSkyLight() || !level.isDay()) {
            return 0;
        }
        BlockPos head = BlockPos.containing(player.getEyePosition());
        boolean open = level.canSeeSky(head) && !level.isRainingAt(head);
        return open ? sun : shade;
    }

    // ---- the keys ----

    /** R (or Sneak + R): the Solar Beam, held; or the Overload. True when it handled the key. */
    static boolean onBeamKey(ServerPlayer player, PowerData data, boolean down, boolean alt) {
        if (alt) {
            if (down) {
                startOverload(player, data);
            }
            return true;
        }
        boolean start = down && !player.isSpectator() && !data.visionPhased() && !overloading(player);
        if (start && !data.beamActive()) {
            if (!creative(player) && data.ironCharge() < SolarJewelConfig.BEAM_COST.get()) {
                player.displayClientMessage(Component.translatable("message.enhanced.solar.empty"), true);
                return true;
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.9F, 1.7F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
        }
        data.setBeamActive(start);
        return true;
    }

    /** N, held: the eye beams. */
    static void onEyesKey(ServerPlayer player, PowerData data, boolean down) {
        boolean start = down && !player.isSpectator() && !data.visionPhased() && !overloading(player);
        if (start && !data.is(Live.SOLAR_EYES)) {
            if (!creative(player) && data.ironCharge() < SolarJewelConfig.EYE_BEAM_COST.get()) {
                player.displayClientMessage(Component.translatable("message.enhanced.solar.empty"), true);
                return;
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT,
                    SoundSource.PLAYERS, 0.5F, 1.8F);
        }
        data.set(Live.SOLAR_EYES, start);
    }

    // ---- every tick ----

    /** His jewel, every tick: it drinks in the sun, and his beams and the Overload go on. */
    static void tick(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        int max = SolarJewelConfig.MAX_CHARGE.get();
        if (creative(player)) {
            data.setIronCharge(max);
        } else {
            int gain = absorbing(player);
            if (data.ironCharge() > max) {
                data.setIronCharge(max);
            } else if (gain > 0 && data.ironCharge() < max) {
                data.setIronCharge(Math.min(max, data.ironCharge() + gain));
                // (a full jewel chimes once)
                if (data.ironCharge() == max) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                            SoundSource.PLAYERS, 0.7F, 1.6F);
                }
            }
        }
        if (data.visionPhased()) {
            data.set(Live.SOLAR_EYES, false);
        }
        if (data.is(Live.SOLAR_EYES)) {
            eyesTick(player, data);
        }
        overloadTick(player, data);
    }

    // ---- R: the Solar Beam ----

    /** One tick of the Solar Beam (the Mind Stone beam's path, its walls and shields, in the jewel's gold). */
    static void beamTick(ServerPlayer player, PowerData data) {
        if (!spend(player, data, SolarJewelConfig.BEAM_COST.get())) {
            data.setBeamActive(false);
            player.displayClientMessage(Component.translatable("message.enhanced.solar.empty"), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.8F, 1.5F);
            return;
        }
        ServerLevel level = player.serverLevel();
        HitResult hit = Raycasts.any(player, com.kolodkingal.enhanced.EnhancedConfig.CHAOS_BEAM_RANGE.get(),
                WandaAbilities.BEAM_TARGET, 1.0F);
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, player.getEyePosition(), hit.getLocation(), 1.0F);
        if (shield != null) {
            if (player.tickCount % 10 == 0) {
                WandaAbilities.pay(shield, player, SolarJewelConfig.BEAM_DAMAGE.get().floatValue());
            }
            Vec3 at = shield.point();
            if (player.tickCount % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(shield.color(), 1.4F), at.x, at.y, at.z, 4, 0.12, 0.12, 0.12, 0.12);
            }
            return;
        }
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player),
                    SolarJewelConfig.BEAM_DAMAGE.get().floatValue());
            BeamFire.ignite(target, SolarJewelConfig.BEAM_BURN_SECONDS.get());
        }
        Vec3 end = hit.getLocation();
        // (3.17) the ground burns where the light lands
        if (SolarJewelConfig.BEAM_FIRE.get() && player.tickCount % BeamFire.GROUND_INTERVAL == 0) {
            BeamFire.strike(player, level, hit, 0, 2);
        }
        if (player.tickCount % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(GOLD, 1.6F), end.x, end.y, end.z, 5, 0.15, 0.15, 0.15, 0.14);
            Embers.burst(level, end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    // ---- N: the eye beams ----

    private static void eyesTick(ServerPlayer player, PowerData data) {
        if (!spend(player, data, SolarJewelConfig.EYE_BEAM_COST.get())) {
            data.set(Live.SOLAR_EYES, false);
            player.displayClientMessage(Component.translatable("message.enhanced.solar.empty"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        HitResult hit = Raycasts.any(player, SolarJewelConfig.EYE_BEAM_RANGE.get(), WandaAbilities.BEAM_TARGET, 1.0F);
        Vec3 end = hit.getLocation();
        if (player.tickCount % SolarJewelConfig.EYE_BEAM_INTERVAL.get() == 0) {
            if (hit instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();
                if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player),
                        SolarJewelConfig.EYE_BEAM_DAMAGE.get().floatValue()) && SolarJewelConfig.EYE_BEAM_BURN_SECONDS.get() > 0) {
                    target.igniteForSeconds(SolarJewelConfig.EYE_BEAM_BURN_SECONDS.get());
                }
            } else if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                melt(player, level, blockHit.getBlockPos());
                melt(player, level, blockHit.getBlockPos().relative(blockHit.getDirection()));
            }
        }
        if (player.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.08, 0.08, 0.08, 0.01);
            level.sendParticles(new EnergyParticleOptions(HEAT, 0.9F), end.x, end.y, end.z, 3, 0.08, 0.08, 0.08, 0.06);
        }
    }

    /** The heat melts snow and ice where the eye beams fall (if he may change the world there). */
    private static void melt(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.mayInteract(player, pos) || !player.mayBuild()) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        BlockState into = null;
        if (state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW)) {
            into = Blocks.AIR.defaultBlockState();
        } else if (state.is(Blocks.SNOW_BLOCK)) {
            into = Blocks.AIR.defaultBlockState();
        } else if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            into = level.dimension() == net.minecraft.world.level.Level.NETHER ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
        }
        if (into != null) {
            level.setBlockAndUpdate(pos, into);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.01);
        }
    }

    // ---- Sneak + R: the Overload ----

    public static boolean overloading(ServerPlayer player) {
        return OVERLOADS.containsKey(player.getUUID());
    }

    private static void startOverload(ServerPlayer player, PowerData data) {
        if (overloading(player) || data.visionPhased() || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        Long drained = DRAINED_UNTIL.get(player.getUUID());
        if (drained != null && now < drained && !creative(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.solar.drained",
                    (drained - now + 19) / 20), true);
            return;
        }
        int charge = data.ironCharge();
        if (!creative(player) && charge < SolarJewelConfig.OVERLOAD_MIN_CHARGE.get()) {
            player.displayClientMessage(Component.translatable("message.enhanced.solar.overload_low",
                    charge * 100 / Math.max(1, SolarJewelConfig.MAX_CHARGE.get()),
                    SolarJewelConfig.OVERLOAD_MIN_CHARGE.get() * 100 / Math.max(1, SolarJewelConfig.MAX_CHARGE.get())), true);
            return;
        }
        data.setBeamActive(false);
        data.set(Live.SOLAR_EYES, false);
        data.set(Live.SOLAR_GATHERING, true);
        OVERLOADS.put(player.getUUID(), new Overload(now, creative(player) ? SolarJewelConfig.MAX_CHARGE.get() : charge));
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void overloadTick(ServerPlayer player, PowerData data) {
        Overload o = OVERLOADS.get(player.getUUID());
        if (o == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long age = level.getGameTime() - o.start;
        int gather = SolarJewelConfig.OVERLOAD_CHARGE_TICKS.get();
        int fire = SolarJewelConfig.OVERLOAD_FIRE_TICKS.get();
        if (data.visionPhased() || !player.isAlive() || !solar(data)) {
            stopOverload(player, data);
            return;
        }
        if (age < gather) {
            // the light gathers: a rising hum, and he slows as he braces
            if (age % 8 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT,
                        SoundSource.PLAYERS, 1.2F, 0.8F + 1.2F * age / gather);
            }
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            player.hurtMarked = true;
            return;
        }
        if (age == gather) {
            // it fires: all the light at once
            data.set(Live.SOLAR_GATHERING, false);
            data.set(Live.SOLAR_OVERLOAD, true);
            if (!creative(player)) {
                data.setIronCharge(0);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.2F, 1.6F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.5F, 0.5F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(),
                    SoundSource.PLAYERS, 0.6F, 1.8F);
        }
        if (age < gather + fire) {
            overloadBeam(player, o);
            // the recoil pushes him back a little
            Vec3 back = player.getLookAngle().scale(-0.04);
            player.push(back.x, back.y * 0.3, back.z);
            player.hurtMarked = true;
            return;
        }
        stopOverload(player, data);
        DRAINED_UNTIL.put(player.getUUID(), level.getGameTime() + SolarJewelConfig.OVERLOAD_COOLDOWN.get());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    /** One tick of the great beam: everything within its width along it is struck, once. */
    private static void overloadBeam(ServerPlayer player, Overload o) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = SolarJewelConfig.OVERLOAD_RANGE.get();
        // the beam stops at the first solid block it meets
        HitResult blocks = level.clip(new net.minecraft.world.level.ClipContext(start, start.add(look.scale(range)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        Vec3 end = blocks.getLocation();
        double width = SolarJewelConfig.OVERLOAD_WIDTH.get();
        float share = Math.min(1.0F, o.charge / (float) Math.max(1, SolarJewelConfig.MAX_CHARGE.get()));
        float damage = SolarJewelConfig.OVERLOAD_DAMAGE.get().floatValue() * (0.4F + 0.6F * share);
        AABB box = new AABB(start, end).inflate(width);
        for (Entity e : level.getEntities(player, box, WandaAbilities.BEAM_TARGET)) {
            if (o.struck.contains(e.getId()) || distanceToSegment(e.getBoundingBox().getCenter(), start, end)
                    > width + e.getBbWidth() / 2) {
                continue;
            }
            if (e instanceof LivingEntity living && !AreaTargets.mayHurt(player, living, true)) {
                continue;
            }
            o.struck.add(e.getId());
            if (e.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player), damage)) {
                e.igniteForSeconds(Math.max(4, SolarJewelConfig.BEAM_BURN_SECONDS.get() * 2));
                Vec3 push = look.scale(1.4);
                e.push(push.x, 0.3, push.z);
                e.hurtMarked = true;
            }
        }
        if (player.tickCount % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(WHITE_GOLD, 2.4F), end.x, end.y, end.z, 10, 0.4, 0.4, 0.4, 0.3);
            level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
            Embers.burst(level, end.x, end.y, end.z, 3, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        double t = len < 1.0E-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    private static void stopOverload(ServerPlayer player, PowerData data) {
        OVERLOADS.remove(player.getUUID());
        data.set(Live.SOLAR_GATHERING, false);
        data.set(Live.SOLAR_OVERLOAD, false);
    }

    /** He is no longer Solar Jewel Vision (another look, another power, logged out): his solar powers end. */
    static void end(ServerPlayer player, PowerData data) {
        if (data.is(Live.SOLAR_EYES) || overloading(player)) {
            data.set(Live.SOLAR_EYES, false);
            stopOverload(player, data);
        }
    }

    @com.kolodkingal.enhanced.platform.event.Subscribe
    public static void onLogout(com.kolodkingal.enhanced.platform.event.PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.getEntity().getUUID());
    }

    /** The server forgets a player who left. */
    public static void forget(UUID player) {
        OVERLOADS.remove(player);
        DRAINED_UNTIL.remove(player);
    }
}
