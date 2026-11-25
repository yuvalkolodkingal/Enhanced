package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hydra.HydraBoltEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Characters;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * (3.19) Red Skull's own powers, in place of Captain America's shield moves on three of his keys. What he is, in the
 * comics and the films: Johann Schmidt, given Erskine's first, flawed serum (a super-soldier's body, Cap's, which he keeps,
 * and his burned red face); the Tesseract, the Space Stone in its cube, whose blue power armed Hydra and which carried
 * him across space; the Dust of Death, his red powder that kills and leaves its victims' faces red skulls; his Luger;
 * and Hydra, which he leads (B is "Hail Hydra", {@link Assemble}).
 * <ul>
 *   <li>R: a blast of the Tesseract's blue power (Sneak: a shot of his Luger, quick and light);</li>
 *   <li>G: the Dust of Death, a red cloud blown before him: it withers, poisons and blinds (Sneak: the super-soldier's
 *   leap, Cap's own);</li>
 *   <li>V: the Tesseract carries him where he looks, up to {@link #JUMP_RANGE} blocks (Sneak: Cap's Shield Bash);</li>
 * </ul>
 * N (his fists) and his body are Cap's. Nothing he does harms Hydra, or the players of his side.
 */
public final class RedSkull {
    public static final float TESSERACT_DAMAGE = 12.0F;
    public static final double TESSERACT_RANGE = 40.0;
    public static final int TESSERACT_COOLDOWN = 30;
    public static final float LUGER_DAMAGE = 6.0F;
    public static final int LUGER_EVERY = 8;
    public static final double DUST_RANGE = 6.0;
    public static final int DUST_COOLDOWN = 200;
    public static final double JUMP_RANGE = 32.0;
    public static final int JUMP_COOLDOWN = 80;
    /** The Tesseract's blue, and the dust's red. */
    public static final int TESSERACT_BLUE = 0x3FA9FF;
    public static final int DUST_RED = 0xC81E1E;

    private static final Map<UUID, Long> LUGER_AT = new HashMap<>();

    private RedSkull() {
    }

    public static boolean is(PowerData data) {
        return Characters.is(data, Characters.RED_SKULL);
    }

    /** One of his keys. Returns whether it was his (else Cap's move goes ahead). */
    static boolean onKey(ServerPlayer player, PowerData data, int ability, boolean alt) {
        if (!is(data)) {
            return false;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    luger(player);
                } else {
                    tesseractBlast(player, data);
                }
                return true;
            }
            case 2 -> {
                if (alt) {
                    return false; // (the super-soldier's leap)
                }
                dustOfDeath(player, data);
                return true;
            }
            case 3 -> {
                if (alt) {
                    return false; // (Shield Bash)
                }
                tesseractJump(player, data);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static Vec3 muzzle(ServerPlayer player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.6)).subtract(0, 0.2, 0);
    }

    /** R: the Tesseract's blast. Returns whether it was fired. */
    public static boolean tesseractBlast(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.CAPTAIN_SHIELD_THROW, now)) {
            return false;
        }
        data.startCooldown(HeroAbility.CAPTAIN_SHIELD_THROW, now, TESSERACT_COOLDOWN);
        ServerLevel level = player.serverLevel();
        Vec3 from = muzzle(player);
        level.addFreshEntity(HydraBoltEntity.create(level, player, from, player.getLookAngle(), 2.2, TESSERACT_DAMAGE,
                TESSERACT_RANGE, HydraBoltEntity.SNIPER));
        level.sendParticles(new EnergyParticleOptions(TESSERACT_BLUE, 1.2F), from.x, from.y, from.z, 18, 0.15, 0.15, 0.15, 0.06);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    /** Sneak + R: his Luger. Returns whether it was fired. */
    public static boolean luger(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LUGER_AT.get(player.getUUID());
        if (last != null && now - last < LUGER_EVERY && now >= last) {
            return false;
        }
        LUGER_AT.put(player.getUUID(), now);
        ServerLevel level = player.serverLevel();
        level.addFreshEntity(HydraBoltEntity.create(level, player, muzzle(player), player.getLookAngle(), 3.0, LUGER_DAMAGE,
                32.0, HydraBoltEntity.PISTOL));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.25F, 2.0F);
        return true;
    }

    /** G: the Dust of Death, blown in a cone before him. Returns how many it caught, or -1 when it could not be used. */
    public static int dustOfDeath(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.CAPTAIN_SHIELD_SLAM, now)) {
            return -1;
        }
        data.startCooldown(HeroAbility.CAPTAIN_SHIELD_SLAM, now, DUST_COOLDOWN);
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (int i = 1; i <= 6; i++) {
            Vec3 p = eye.add(look.scale(i));
            level.sendParticles(new EnergyParticleOptions(DUST_RED, 1.0F + i * 0.15F), p.x, p.y - 0.3, p.z, 10 + i * 3,
                    0.2 * i, 0.2 * i, 0.2 * i, 0.01);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ALLAY_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);
        int caught = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(DUST_RANGE),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 to = e.getEyePosition().subtract(eye);
            if (to.length() > DUST_RANGE || to.normalize().dot(look) < 0.6 || Assemble.isAlly(player, e)
                    || e instanceof net.minecraft.world.entity.player.Player victim && (victim.isCreative() || !player.canHarmPlayer(victim))) {
                continue;
            }
            MobEffectInstance poison = new MobEffectInstance(MobEffects.POISON, 120, 1);
            boolean takes = e.canBeAffected(poison);
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1), player);
            e.addEffect(poison, player);
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), player);
            // (the undead take no poison or wither: the dust eats at them harder)
            e.hurt(player.damageSources().indirectMagic(player, player), takes ? 3.0F : 8.0F);
            caught++;
        }
        return caught;
    }

    /** V: the Tesseract carries him where he looks. Returns whether he went. */
    public static boolean tesseractJump(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.CAPTAIN_SHIELD_CHARGE, now)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(JUMP_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 to = hit.getType() == HitResult.Type.MISS ? end
                : hit.getLocation().subtract(player.getLookAngle().scale(0.6));
        Vec3 feet = safeFeet(player, to);
        if (feet == null) {
            return false;
        }
        data.startCooldown(HeroAbility.CAPTAIN_SHIELD_CHARGE, now, JUMP_COOLDOWN);
        Vec3 from = player.position();
        level.sendParticles(new EnergyParticleOptions(TESSERACT_BLUE, 1.4F), from.x, from.y + 1.0, from.z, 40, 0.4, 0.8, 0.4, 0.05);
        player.teleportTo(feet.x, feet.y, feet.z);
        player.resetFallDistance();
        level.sendParticles(new EnergyParticleOptions(TESSERACT_BLUE, 1.4F), feet.x, feet.y + 1.0, feet.z, 40, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);
        level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.8F);
        return true;
    }

    /** Where his feet may stand near {@code at} (room for his body), looking a little up and down; or null. */
    private static Vec3 safeFeet(ServerPlayer player, Vec3 at) {
        for (int dy : new int[] {0, 1, -1, 2, -2}) {
            Vec3 feet = new Vec3(at.x, Math.floor(at.y) + dy, at.z);
            AABB box = player.getDimensions(player.getPose()).makeBoundingBox(feet);
            if (player.level().noCollision(player, box) && player.level().isInWorldBounds(BlockPos.containing(feet))) {
                return feet;
            }
        }
        return null;
    }

    /** Forgets a player who left. */
    public static void forget(UUID player) {
        LUGER_AT.remove(player);
    }
}
