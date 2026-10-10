package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Lifting, carrying and launching things with the mind. Shared by Magneto (metal only), Wanda (anything, including
 * blocks) and Billy (anything loose, including projectiles in flight: he stops bullets in WandaVision).
 *
 * <p>While held, the target is moved every tick towards the point the holder is looking at. Launched targets are
 * tracked until they slam into something: they hurt every creature they crash into and take heavy impact damage
 * when they hit a wall or the floor. A launched projectile simply flies again, where the holder looks.
 */
public final class Telekinesis {

    public enum Mode {
        MAGNETO(Power.MAGNETO),
        WANDA(Power.SCARLET_WITCH),
        BILLY(Power.BILLY);

        final Power power;

        Mode(Power power) {
            this.power = power;
        }
    }

    /** Max time a player can be held, so nobody is trapped forever. */
    public static final int MAX_PLAYER_HOLD_TICKS = 600;
    private static final double BREAK_DISTANCE = 48.0;
    private static final double MIN_HOLD_DISTANCE = 3.0;
    private static final double MAX_HOLD_DISTANCE = 12.0;
    /** A held projectile hangs weightless; this remembers whether it was weightless before (to restore it). */
    private static final String HELD_PROJECTILE_TAG = "enhanced:telekinesis_held";

    private static final class Grip {
        final UUID target;
        final Mode mode;
        final double distance;
        int ticks;

        Grip(UUID target, Mode mode, double distance) {
            this.target = target;
            this.mode = mode;
            this.distance = distance;
        }
    }

    private static final class Flight {
        final UUID thrower;
        final ResourceKey<Level> dimension;
        final boolean heavy;
        final int color;
        final IntSet hit = new IntOpenHashSet();
        Vec3 lastPos;
        double lastSpeed;
        int ticks;

        Flight(UUID thrower, ResourceKey<Level> dimension, Vec3 pos, double speed, boolean heavy, int color) {
            this.thrower = thrower;
            this.dimension = dimension;
            this.lastPos = pos;
            this.lastSpeed = speed;
            this.heavy = heavy;
            this.color = color;
        }
    }

    /** Holder UUID → what they hold. */
    private static final Map<UUID, Grip> GRIPS = new HashMap<>();
    /** Thrown entity UUID → its flight. */
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Input

    /** The telekinesis key: grab what you look at, or launch what you hold (Sneak: put it down gently). */
    public static void press(ServerPlayer player, Mode mode) {
        Grip grip = GRIPS.get(player.getUUID());
        if (grip != null) {
            if (player.isShiftKeyDown()) {
                release(player, true);
            } else {
                launch(player);
            }
            return;
        }

        double range = EnhancedConfig.TELEKINESIS_RANGE.get();
        EntityHitResult hit = Raycasts.entity(player, range, 0.3, e -> canLift(player, e, mode), 1.0F);
        if (hit == null && mode == Mode.BILLY) {
            // Arrows are small and fast: aim a little more generously at them.
            hit = Raycasts.entity(player, range, 1.0, e -> e instanceof Projectile && canLift(player, e, mode), 1.0F);
        }
        if (hit != null) {
            hold(player, hit.getEntity(), mode);
            return;
        }
        if (mode == Mode.WANDA && BlockLifting.tryLift(player)) {
            return;
        }
        player.displayClientMessage(Component.translatable(mode == Mode.MAGNETO
                ? "message.enhanced.telekinesis.no_metal" : "message.enhanced.telekinesis.nothing"), true);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Targets

    public static boolean canLift(Player holder, Entity e, Mode mode) {
        if (e == holder || !e.isAlive() || e.isSpectator() || isHeld(e) || e.getType().is(ModTags.TELEKINESIS_IMMUNE)) {
            return false;
        }
        if (e.hasPassenger(holder) || holder.hasPassenger(e)) {
            return false;
        }
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        if (MjolnirPin.isPinned(e)) {
            // Mjolnir lies on them: no power but its owner's can move them, any more than it can lift the hammer.
            return false;
        }
        if (e instanceof ItemEntity item && item.getItem().is(ModTags.CANNOT_BE_SNATCHED)) {
            // Mjolnir: no power can lift it, only its owner's own hands.
            return false;
        }
        return switch (mode) {
            case MAGNETO -> isMagnetic(e);
            case WANDA -> isLoose(e);
            case BILLY -> isLoose(e) || (e instanceof Projectile projectile && inFlight(projectile));
        };
    }

    /** Creatures, dropped items, vehicles, primed TNT, falling (or lifted) blocks: anything not part of the world. */
    private static boolean isLoose(Entity e) {
        return e instanceof LivingEntity || e instanceof ItemEntity || e instanceof TelekineticBlockEntity
                || e instanceof VehicleEntity || e instanceof PrimedTnt || e instanceof FallingBlockEntity;
    }

    /** A projectile still flying: not stuck in the ground, not a trident flying home, not caught by Magneto. */
    private static boolean inFlight(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow && (arrow.inGround || arrow.isNoPhysics())) {
            return false;
        }
        return !(projectile instanceof FishingHook) && !MagnetoAbilities.isHeld(projectile);
    }

    /** Iron Golems, minecarts, dropped metal, and any creature wearing Iron, Chainmail or Gold armor. */
    public static boolean isMagnetic(Entity e) {
        if (e.getType().is(ModTags.MAGNETIC_ENTITIES)) {
            return true;
        }
        if (e instanceof ItemEntity item) {
            return item.getItem().is(ModTags.MAGNETIC_ITEMS) || item.getItem().is(ModTags.MAGNETIC_ARMOR);
        }
        if (e instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor() && living.getItemBySlot(slot).is(ModTags.MAGNETIC_ARMOR)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isHeld(Entity e) {
        UUID id = e.getUUID();
        for (Grip g : GRIPS.values()) {
            if (g.target.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHolding(Player player) {
        return GRIPS.containsKey(player.getUUID());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Holding

    public static void hold(ServerPlayer player, Entity target, Mode mode) {
        double dist = player.getEyePosition().distanceTo(target.position().add(0, target.getBbHeight() * 0.5, 0));
        GRIPS.put(player.getUUID(), new Grip(target.getUUID(), mode, Mth.clamp(dist, MIN_HOLD_DISTANCE, MAX_HOLD_DISTANCE)));
        FLIGHTS.remove(target.getUUID());
        Powers.get(player).setHeldEntityId(target.getId());

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            if (mob instanceof IronGolem && mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
        if (target instanceof TelekineticBlockEntity block) {
            block.grab(player);
        }
        if (target instanceof Projectile projectile) {
            // Caught: it stops dead and hangs weightless in front of the holder.
            SlowMotion.unslow(projectile);
            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.getPersistentData().putBoolean(HELD_PROJECTILE_TAG, projectile.isNoGravity());
            projectile.setNoGravity(true);
        }
        target.resetFallDistance();
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                mode == Mode.MAGNETO ? ModSounds.MAGNET_GRAB.get() : SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS, mode == Mode.MAGNETO ? 1.0F : 0.8F, mode == Mode.MAGNETO ? 1.0F : 1.2F);
    }

    /** Called every tick for every player. */
    static void tickHolder(ServerPlayer player, PowerData data) {
        Grip grip = GRIPS.get(player.getUUID());
        if (grip == null) {
            if (data.heldEntityId() != -1) {
                data.setHeldEntityId(-1);
            }
            return;
        }
        grip.ticks++;
        ServerLevel level = player.serverLevel();
        Entity e = level.getEntity(grip.target);
        boolean lost = e == null || !e.isAlive() || e.level() != level || player.isDeadOrDying() || player.isSpectator()
                || data.power() != grip.mode.power
                || e.distanceTo(player) > BREAK_DISTANCE
                || (grip.mode == Mode.MAGNETO && !isMagnetic(e))
                || MjolnirPin.isPinned(e)
                || (e instanceof Player p && (p.isCreative() || p.isSpectator() || grip.ticks > MAX_PLAYER_HOLD_TICKS));
        if (lost) {
            release(player, false);
            return;
        }
        if (data.heldEntityId() != e.getId()) {
            data.setHeldEntityId(e.getId());
        }

        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(grip.distance)).subtract(0, e.getBbHeight() * 0.5, 0);
        Vec3 diff = target.subtract(e.position());
        if (e instanceof ServerPlayer held) {
            // Players move themselves on their own computer, so steer them with velocity updates.
            Vec3 vel = clampLength(diff.scale(0.35), 1.6).add(0, 0.08, 0);
            held.setDeltaMovement(vel);
            held.hurtMarked = true;
            held.resetFallDistance();
            held.connection.aboveGroundTickCount = 0;
        } else {
            e.move(MoverType.SELF, clampLength(diff.scale(0.45), 1.6));
            // A held arrow keeps pointing where the holder looks (an arrow turns to face the way it moves).
            e.setDeltaMovement(e instanceof AbstractArrow ? player.getLookAngle().scale(1.0E-3) : Vec3.ZERO);
            e.hasImpulse = true; // send the new position to clients every tick, so it glides smoothly
            e.resetFallDistance();
            if (e instanceof Mob mob) {
                mob.getNavigation().stop();
                if (mob instanceof IronGolem && mob.getTarget() == player) {
                    mob.setTarget(null);
                }
            }
            if (e instanceof TelekineticBlockEntity block) {
                block.keepHeld();
            }
        }
        if (grip.ticks % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(grip.mode.power.color, 1.2F),
                    e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 3,
                    e.getBbWidth() * 0.6, e.getBbHeight() * 0.5, e.getBbWidth() * 0.6, 0.02);
        }
    }

    /** Lets go of whatever the player holds. */
    public static void release(ServerPlayer player, boolean gently) {
        Grip grip = GRIPS.remove(player.getUUID());
        Powers.get(player).setHeldEntityId(-1);
        if (grip == null) {
            return;
        }
        Entity e = player.serverLevel().getEntity(grip.target);
        if (e == null) {
            return;
        }
        e.resetFallDistance();
        letGo(e);
        if (e instanceof TelekineticBlockEntity block) {
            block.drop();
        } else {
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
        }
        if (gently) {
            player.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    private static void launch(ServerPlayer player) {
        Grip grip = GRIPS.remove(player.getUUID());
        Powers.get(player).setHeldEntityId(-1);
        if (grip == null) {
            return;
        }
        Entity e = player.serverLevel().getEntity(grip.target);
        if (e == null) {
            return;
        }
        double speed = EnhancedConfig.THROW_SPEED.get();
        Vec3 vel = player.getLookAngle().scale(speed);
        letGo(e);
        if (e instanceof TelekineticBlockEntity block) {
            block.launch(player, vel);
        } else if (e instanceof Projectile projectile) {
            launchProjectile(player, projectile, vel);
        } else {
            e.setDeltaMovement(vel);
            e.hurtMarked = true;
            e.hasImpulse = true;
            FLIGHTS.put(e.getUUID(), new Flight(player.getUUID(), player.level().dimension(), e.position(), speed,
                    e instanceof IronGolem, grip.mode.power.color));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.9F, 0.7F);
    }

    /** A held projectile gets its weight back. */
    private static void letGo(Entity e) {
        if (e instanceof Projectile projectile && projectile.getPersistentData().contains(HELD_PROJECTILE_TAG)) {
            projectile.setNoGravity(projectile.getPersistentData().getBoolean(HELD_PROJECTILE_TAG));
            projectile.getPersistentData().remove(HELD_PROJECTILE_TAG);
        }
    }

    /** A caught projectile flies again, where the holder looks, and hits for them. */
    private static void launchProjectile(ServerPlayer player, Projectile projectile, Vec3 velocity) {
        if (projectile instanceof AbstractArrow arrow) {
            arrow.inGround = false;
            if (!(arrow instanceof ThrownTrident)) {
                // The holder gets the credit, but nobody gains the right to pick up an arrow they couldn't before.
                AbstractArrow.Pickup pickup = arrow.pickup;
                arrow.setOwner(player);
                arrow.pickup = pickup;
            }
            // Tridents keep their owner, so Loyalty and pickup still belong to whoever threw them.
        } else {
            projectile.setOwner(player);
        }
        projectile.shoot(velocity.x, velocity.y, velocity.z, (float) velocity.length(), 0.0F);
        projectile.hurtMarked = true;
        projectile.hasImpulse = true;
    }

    /** A projectile saved to disk while held comes back as a normal projectile. */
    public static void onJoin(Entity entity) {
        if (entity instanceof Projectile projectile && projectile.getPersistentData().contains(HELD_PROJECTILE_TAG) && !isHeld(projectile)) {
            letGo(projectile);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Flights

    /** Called once per server tick. */
    public static void tickFlights(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Flight>> it = FLIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Flight> entry = it.next();
            Flight f = entry.getValue();
            ServerLevel level = server.getLevel(f.dimension);
            Entity e = level == null ? null : level.getEntity(entry.getKey());
            if (e == null || !e.isAlive() || ++f.ticks > 80) {
                it.remove();
                continue;
            }
            ServerPlayer thrower = server.getPlayerList().getPlayer(f.thrower);
            if (e instanceof Mob mob && mob.isNoAi()) {
                // Mobs without AI don't run their own physics; fly them ourselves.
                Vec3 v = e.getDeltaMovement();
                e.move(MoverType.SELF, v);
                e.setDeltaMovement(v.x * 0.91, (v.y - 0.08) * 0.98, v.z * 0.91);
                e.hasImpulse = true;
            }
            Vec3 pos = e.position();
            double speed = pos.distanceTo(f.lastPos);
            double hitSpeed = Math.max(speed, f.lastSpeed);

            if (hitSpeed > 0.5) {
                AABB box = e.getBoundingBox().inflate(0.4);
                Vec3 dir = pos.subtract(f.lastPos);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                        v -> v != e && v != thrower && v.isAlive() && !v.isSpectator() && !f.hit.contains(v.getId()))) {
                    f.hit.add(victim.getId());
                    float damage = (f.heavy ? 15F : 6F) * (float) Mth.clamp(hitSpeed / 1.2, 0.6, 1.0);
                    victim.hurt(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, thrower, e), damage);
                    if (dir.horizontalDistanceSqr() > 1.0E-4) {
                        victim.knockback(1.2, -dir.x, -dir.z);
                    }
                    if (f.heavy && e instanceof IronGolem golem && victim instanceof Enemy) {
                        golem.setTarget(victim);
                    }
                }
            }

            if (e instanceof ServerPlayer held) {
                held.connection.aboveGroundTickCount = 0;
            }
            if (e instanceof IronGolem golem && golem.getTarget() == thrower) {
                golem.setTarget(null);
            }

            boolean slammed = f.ticks > 1 && f.lastSpeed > 0.6 && (speed < f.lastSpeed * 0.45 || e.horizontalCollision);
            if (slammed) {
                impact(level, e, f, thrower, f.lastSpeed);
                it.remove();
                continue;
            }
            if (f.ticks > 3 && speed < 0.05 && e.onGround()) {
                it.remove();
                continue;
            }
            if (f.ticks % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(f.color, 1.0F), pos.x, pos.y + e.getBbHeight() * 0.5, pos.z,
                        2, 0.2, 0.2, 0.2, 0.0);
            }
            f.lastSpeed = speed;
            f.lastPos = pos;
        }
    }

    private static void impact(ServerLevel level, Entity e, Flight f, @Nullable ServerPlayer thrower, double impactSpeed) {
        BlockState ground = level.getBlockState(e.blockPosition().below());
        if (ground.isAir()) {
            ground = level.getBlockState(e.blockPosition());
        }
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), e.getX(), e.getY() + 0.2, e.getZ(),
                    30, e.getBbWidth() * 0.5, 0.2, e.getBbWidth() * 0.5, 0.15);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 1, 0, 0, 0, 0);
        if (f.heavy) {
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 1.0F, 0.6F);
        } else if (e instanceof LivingEntity || e instanceof VehicleEntity) {
            // Only bodies and vehicles are hurt by the impact; a thrown item stack must not be destroyed.
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_BIG_FALL, SoundSource.NEUTRAL, 1.0F, 0.8F);
            float damage = (float) Mth.clamp((impactSpeed - 0.6) * 8.0, 2.0, 16.0);
            e.hurt(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, thrower, thrower), damage);
        }
        e.resetFallDistance();
    }

    private static Vec3 clampLength(Vec3 v, double max) {
        double len = v.length();
        return len > max ? v.scale(max / len) : v;
    }

    /** Forget everything (server stopping). */
    public static void clear() {
        GRIPS.clear();
        FLIGHTS.clear();
    }

    /** A player left the game: drop what they hold. */
    public static void onLogout(ServerPlayer player) {
        release(player, false);
    }

    private Telekinesis() {
    }
}
