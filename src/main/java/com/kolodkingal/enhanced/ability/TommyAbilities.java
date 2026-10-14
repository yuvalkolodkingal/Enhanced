package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tommy Maximoff (Speed): Wanda's son, a speedster like his uncle Pietro. His speed doesn't slow the world down (that is
 * Quicksilver's trick); he leaves a teal-green ghost trail instead, as in the Battle of Westview.
 */
public final class TommyAbilities {
    private static final ResourceLocation SPEED_ID = Enhanced.id("tommy_speed");
    private static final ResourceLocation STEP_ID = Enhanced.id("tommy_step");

    public static final int BLITZ_RADIUS = 12;
    public static final int BLITZ_MAX_TARGETS = 8;
    public static final int BLITZ_COOLDOWN = 160;
    public static final int WHIRLWIND_TICKS = 60;
    public static final double WHIRLWIND_RADIUS = 8.0;
    /** Radius of the ring Tommy runs, and that everything caught spins around. */
    public static final double WHIRLWIND_RING = 2.5;
    public static final int WHIRLWIND_COOLDOWN = 200;
    public static final int VIBRATE_MAX_WALL = 4;
    public static final int VIBRATE_COOLDOWN = 40;
    public static final double ACCELERATE_RANGE = 5.0;
    public static final float ACCELERATE_DAMAGE = 6.0F;
    public static final int ACCELERATE_COOLDOWN = 20;

    /** Ticks of whirlwind left, per Tommy. */
    private static final Map<UUID, Integer> WHIRLWINDS = new HashMap<>();

    // ---- input ----

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        if (!down || player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                data.setToggle(Toggle.SUPER_SPEED, !data.toggle(Toggle.SUPER_SPEED));
                player.displayClientMessage(Component.translatable(data.toggle(Toggle.SUPER_SPEED)
                        ? "message.enhanced.super_speed.on" : "message.enhanced.super_speed.off"), true);
            }
            case 2 -> disarmBlitz(player, data);
            case 3 -> whirlwind(player, data);
            case 4 -> vibrate(player, data);
            case 5 -> accelerate(player, data);
            default -> {
            }
        }
    }

    // ---- Ability 1: Super Speed ----

    public static void tick(ServerPlayer player, PowerData data) {
        boolean canRun = data.toggle(Toggle.SUPER_SPEED) && player.isSprinting() && !player.isPassenger()
                && !player.getAbilities().flying && (player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0);
        boolean was = data.is(Live.SPEEDING);
        data.set(Live.SPEEDING, canRun);
        if (canRun) {
            player.causeFoodExhaustion(EnhancedConfig.TOMMY_EXHAUSTION.get().floatValue());
            player.resetFallDistance();
            if (!was) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.QUICKSILVER_WHOOSH.get(),
                        SoundSource.PLAYERS, 0.9F, 1.25F);
            }
        }
        applySpeed(player, data);
        whirlwindTick(player, data);
    }

    /** Super speed and step-up while Tommy runs; also called when the power changes. */
    public static void applySpeed(ServerPlayer player, PowerData data) {
        boolean running = data.power() == Power.TOMMY && data.is(Live.SPEEDING);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, EnhancedConfig.TOMMY_SPEED_BONUS.get(), running,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PowerTicker.setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, 0.65, running);
    }

    public static boolean isRunning(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.TOMMY && data.is(Live.SPEEDING);
    }

    // ---- Ability 2: Disarm Blitz ----

    /**
     * The Battle of Westview: Tommy circles everyone armed around him and is back before they notice, their weapons in
     * his arms. Creatures lose whatever they hold; players only their weapon.
     */
    private static void disarmBlitz(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(2, now)) {
            return;
        }
        // A conjured body (a Loki illusion) is passed by: what it holds is a copy of Loki's, and snatching it would make one.
        List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(BLITZ_RADIUS), e -> e != player && e.isAlive() && !e.isSpectator()
                        && !Metal.isConjured(e) && e.distanceToSqr(player) <= BLITZ_RADIUS * BLITZ_RADIUS
                        && disarmableHand(e) != null));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enhanced.disarm.nothing"), true);
            return;
        }
        targets.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        data.startCooldown(2, now, BLITZ_COOLDOWN);
        Vec3 from = player.position();
        int taken = 0;
        for (LivingEntity target : targets.subList(0, Math.min(BLITZ_MAX_TARGETS, targets.size()))) {
            InteractionHand hand;
            while ((hand = disarmableHand(target)) != null) {
                ItemStack weapon = target.getItemInHand(hand).copy();
                target.setItemInHand(hand, ItemStack.EMPTY);
                if (target instanceof Mob mob) {
                    // The weapon is gone for good, so the mob must not also drop a copy of it when it dies.
                    mob.setDropChance(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, 0.0F);
                }
                if (!player.getInventory().add(weapon)) {
                    player.drop(weapon, false);
                }
                taken++;
                if (target instanceof Player) {
                    break;
                }
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), player);
            ring(level, target.position(), target.getBbWidth() + 0.6);
            trail(level, from, target.position());
            from = target.position();
        }
        trail(level, from, player.position());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.QUICKSILVER_DASH.get(), SoundSource.PLAYERS, 1.0F, 1.3F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("message.enhanced.disarm.done", taken), true);
    }

    /** The hand Tommy would snatch an item from, or null: anything a mob holds, but only a player's weapon. */
    private static InteractionHand disarmableHand(LivingEntity target) {
        if (target instanceof Player p) {
            if (p.isCreative()) {
                return null;
            }
            ItemStack main = p.getMainHandItem();
            return main.is(ModTags.WEAPONS) && !main.is(ModTags.CANNOT_BE_SNATCHED) ? InteractionHand.MAIN_HAND : null;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = target.getItemInHand(hand);
            if (!stack.isEmpty() && !stack.is(ModTags.CANNOT_BE_SNATCHED)) {
                return hand;
            }
        }
        return null;
    }

    // ---- Ability 3: Whirlwind ----

    private static void whirlwind(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(3, now) || WHIRLWINDS.containsKey(player.getUUID())) {
            return;
        }
        data.startCooldown(3, now, WHIRLWIND_COOLDOWN);
        WHIRLWINDS.put(player.getUUID(), WHIRLWIND_TICKS);
        data.set(Live.WHIRLWIND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.QUICKSILVER_WHOOSH.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    /** Everything caught spins around Tommy on a ring, rising; when the whirlwind ends it is flung away. */
    private static void whirlwindTick(ServerPlayer player, PowerData data) {
        Integer left = WHIRLWINDS.get(player.getUUID());
        if (left == null) {
            data.set(Live.WHIRLWIND, false);
            return;
        }
        boolean last = left <= 1 || !player.isAlive();
        Vec3 center = player.position();
        for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(WHIRLWIND_RADIUS, 4.0, WHIRLWIND_RADIUS),
                TommyAbilities::caughtByWhirlwind)) {
            Vec3 flat = e.position().subtract(center).multiply(1, 0, 1);
            double dist = flat.length();
            if (dist > WHIRLWIND_RADIUS) {
                continue;
            }
            Vec3 out = dist < 1.0E-3 ? new Vec3(1, 0, 0) : flat.scale(1.0 / dist);
            Vec3 tangent = new Vec3(-out.z, 0, out.x);
            Vec3 motion;
            if (last) {
                motion = out.scale(1.4).add(0, 0.7, 0);
            } else {
                double lift = e.getY() - center.y < 3.0 ? 0.16 : 0.0;
                motion = tangent.scale(0.65).add(out.scale((WHIRLWIND_RING - dist) * 0.25)).add(0, lift, 0);
            }
            e.setDeltaMovement(motion);
            e.hurtMarked = true;
            e.resetFallDistance();
            if (e instanceof LivingEntity living && left % 20 == 0) {
                living.hurt(ModDamageTypes.source(player.level(), ModDamageTypes.SPEED_BLITZ, player, player), 1.0F);
            }
        }
        if (last) {
            WHIRLWINDS.remove(player.getUUID());
            data.set(Live.WHIRLWIND, false);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.QUICKSILVER_DASH.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
        } else {
            WHIRLWINDS.put(player.getUUID(), left - 1);
        }
    }

    private static boolean caughtByWhirlwind(Entity e) {
        if (!e.isAlive() || e.isSpectator() || e.isPassenger() || e.noPhysics) {
            return false;
        }
        if (e instanceof Player p) {
            return !p.isCreative() && !p.getAbilities().flying;
        }
        return e instanceof LivingEntity || e instanceof Projectile;
    }

    public static boolean hasWhirlwind(Player player) {
        return WHIRLWINDS.containsKey(player.getUUID());
    }

    // ---- Ability 4: Vibrate ----

    /** Tommy vibrates his molecules and passes straight through the wall in front of him (up to 4 blocks thick). */
    private static void vibrate(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(4, now)) {
            return;
        }
        SandCocoonEntity.freeFrom(player); // his molecules slip out of a cocoon of Apocalypse's sand
        Direction facing = player.getDirection();
        BlockPos feet = player.blockPosition();
        int wallStart = -1;
        for (int i = 1; i <= 2; i++) {
            if (!isOpen(level, feet.relative(facing, i))) {
                wallStart = i;
                break;
            }
        }
        if (wallStart < 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.vibrate.no_wall"), true);
            return;
        }
        for (int i = wallStart; i <= wallStart + VIBRATE_MAX_WALL; i++) {
            BlockPos pos = feet.relative(facing, i);
            if (isOpen(level, pos)) {
                Vec3 target = Vec3.atBottomCenterOf(pos);
                if (!HexManager.onTeleport(level, player, target)) {
                    return;
                }
                data.startCooldown(4, now, VIBRATE_COOLDOWN);
                Vec3 start = player.position();
                player.teleportTo(level, target.x, target.y, target.z, player.getYRot(), player.getXRot());
                player.resetFallDistance();
                trail(level, start, target);
                level.playSound(null, start.x, start.y, start.z, ModSounds.VIBRATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, target.x, target.y, target.z, ModSounds.VIBRATE.get(), SoundSource.PLAYERS, 0.7F, 1.2F);
                return;
            }
            if (unbreakable(level, pos) || unbreakable(level, pos.above())) {
                player.displayClientMessage(Component.translatable("message.enhanced.vibrate.unbreakable"), true);
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.enhanced.vibrate.too_thick"), true);
    }

    /** Room to stand: the block and the one above it don't block movement. */
    private static boolean isOpen(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private static boolean unbreakable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getDestroySpeed(level, pos) < 0;
    }

    // ---- Ability 5: Molecular Acceleration (comics) ----

    /** Tommy vibrates whatever he touches until it shatters: a block breaks apart, a creature is blasted back. */
    private static void accelerate(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(5, now)) {
            return;
        }
        HitResult hit = Raycasts.any(player, ACCELERATE_RANGE,
                e -> e instanceof LivingEntity living && living.isAlive() && e != player && !e.isSpectator(), 1.0F);
        if (hit instanceof EntityHitResult entityHit) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            data.startCooldown(5, now, ACCELERATE_COOLDOWN);
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.MOLECULAR_ACCELERATION, player, player), ACCELERATE_DAMAGE);
            Vec3 push = target.position().subtract(player.position());
            target.knockback(0.8, -push.x, -push.z);
            burst(level, entityHit.getLocation());
            return;
        }
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.enhanced.accelerate.nothing"), true);
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > 50.0F || state.is(ModTags.TELEKINESIS_IMMOVABLE)) {
            player.displayClientMessage(Component.translatable("message.enhanced.accelerate.too_hard"), true);
            return;
        }
        if (BossFights.protects(level, pos)) {
            // The arena of a boss fight stays as it is.
            player.displayClientMessage(Component.translatable("message.enhanced.boss.protected"), true);
            return;
        }
        // Breaking a block is a player action: spawn protection, claims and adventure mode all get their say.
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (!player.mayInteract(level, pos) || player.blockActionRestricted(level, pos, mode)
                || CommonHooks.fireBlockBreak(level, mode, player, pos, state).isCanceled()) {
            return;
        }
        data.startCooldown(5, now, ACCELERATE_COOLDOWN);
        level.destroyBlock(pos, !player.isCreative(), player);
        burst(level, Vec3.atCenterOf(pos));
    }

    // ---- effects ----

    /** A teal-green ghost trail between two points, low along the ground like in the Battle of Westview. */
    private static void trail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(80, delta.length() * 3);
        EnergyParticleOptions deep = new EnergyParticleOptions(Power.TOMMY.color, 1.2F);
        EnergyParticleOptions light = new EnergyParticleOptions(Power.TOMMY.glow, 0.9F);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
            level.sendParticles(i % 2 == 0 ? deep : light, p.x, p.y + 0.6, p.z, 1, 0.1, 0.4, 0.1, 0.0);
        }
    }

    /** A ring of trail around a disarmed creature: Tommy ran around it. */
    private static void ring(ServerLevel level, Vec3 center, double radius) {
        EnergyParticleOptions light = new EnergyParticleOptions(Power.TOMMY.glow, 1.0F);
        for (int i = 0; i < 20; i++) {
            double a = i * Mth.TWO_PI / 20;
            level.sendParticles(light, center.x + Math.cos(a) * radius, center.y + 0.4, center.z + Math.sin(a) * radius,
                    1, 0.02, 0.15, 0.02, 0.0);
        }
    }

    private static void burst(ServerLevel level, Vec3 at) {
        level.sendParticles(new EnergyParticleOptions(Power.TOMMY.glow, 1.4F), at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.12);
        level.sendParticles(new EnergyParticleOptions(0xE6FFF5, 1.0F), at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.05);
        level.playSound(null, at.x, at.y, at.z, ModSounds.MOLECULAR_BURST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
        WHIRLWINDS.remove(player.getUUID());
        data.set(Live.WHIRLWIND, false);
        data.set(Live.SPEEDING, false);
    }

    public static void clear() {
        WHIRLWINDS.clear();
    }

    private TommyAbilities() {
    }
}
