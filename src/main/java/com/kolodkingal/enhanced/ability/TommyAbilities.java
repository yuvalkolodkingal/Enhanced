package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SpeedConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.Shelters;
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

    /** His own key: the Sneak variant is Sneak held. */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * Key {@code ability} (his home key of the ability) went down or up; {@code alt}: its Sneak variant (3.4: said
     * explicitly, as a custom hero's key router calls it for the binding that went down).
     */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down || player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    boolean off = !data.toggle(Toggle.TOMMY_WALL_RUN_OFF);
                    data.setToggle(Toggle.TOMMY_WALL_RUN_OFF, off);
                    player.displayClientMessage(Component.translatable(off
                            ? "message.enhanced.tommy_wall_run.off" : "message.enhanced.tommy_wall_run.on"), true);
                } else {
                    toggleTime(player, data);
                }
            }
            case 2 -> {
                if (alt) {
                    dash(player, data);
                } else {
                    data.setToggle(Toggle.SUPER_SPEED, !data.toggle(Toggle.SUPER_SPEED));
                    player.displayClientMessage(Component.translatable(data.toggle(Toggle.SUPER_SPEED)
                            ? "message.enhanced.super_speed.on" : "message.enhanced.super_speed.off"), true);
                }
            }
            case 3 -> {
                if (alt) {
                    sonicBoom(player, data);
                } else {
                    whirlwind(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    disarmBlitz(player, data);
                } else {
                    vibrate(player, data);
                }
            }
            case 5 -> accelerate(player, data);
            default -> {
            }
        }
    }

    // ---- Ability 1: Slow Time (3.3) ----

    /** R: Tommy's slow time on or off: the world around him runs slower, as in Quicksilver Time (his Super Speed apart). */
    private static void toggleTime(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.TOMMY_TIME);
        if (on && !fed(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.tommy_time.starving"), true);
            return;
        }
        data.setToggle(Toggle.TOMMY_TIME, on);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.tommy_time.on" : "message.enhanced.tommy_time.off"), true);
        QuicksilverAbilities.timeSound(player, on, 1.0F);
    }

    private static boolean fed(ServerPlayer player) {
        return player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0;
    }

    // ---- Ability 2 (Sneak): Dash Strike; Ability 3 (Sneak): Sonic Boom (3.3) ----

    /** Sneak + G: the Dash Strike, in his teal and green (the cooldown of G). */
    private static void dash(ServerPlayer player, PowerData data) {
        long now = player.serverLevel().getGameTime();
        if (!data.isReady(HeroAbility.TOMMY_SPEED, now)) {
            return;
        }
        if (MjolnirPin.isPinned(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned_cannot"), true);
            return;
        }
        if (!SpeedStrikes.configuredDash(player, SpeedStrikes.Style.of(player, Power.TOMMY)).moved()) {
            player.displayClientMessage(Component.translatable("message.enhanced.dash.blocked"), true);
            return;
        }
        data.startCooldown(HeroAbility.TOMMY_SPEED, now, SpeedStrikes.value(SpeedConfig.DASH_COOLDOWN, 100));
    }

    /** Sneak + V: the Sonic Boom (on the Whirlwind's cooldown: both are his storms). */
    private static void sonicBoom(ServerPlayer player, PowerData data) {
        long now = player.serverLevel().getGameTime();
        if (!data.isReady(HeroAbility.TOMMY_WHIRLWIND, now) || WHIRLWINDS.containsKey(player.getUUID())) {
            return;
        }
        SpeedStrikes.sonicBoom(player, data, SpeedStrikes.Style.of(player, Power.TOMMY), SpeedStrikes.value(SpeedConfig.BOOM_RADIUS, 6.0),
                (float) SpeedStrikes.value(SpeedConfig.BOOM_DAMAGE, 4.0));
        data.startCooldown(HeroAbility.TOMMY_WHIRLWIND, now, SpeedStrikes.value(SpeedConfig.BOOM_COOLDOWN, 160));
    }

    // ---- Ability 1: Super Speed ----

    /**
     * His upkeep, every tick; 3.4: also a custom hero's with any of his abilities, each part only for the ability it is
     * (Super Speed's run, the slow time, the Whirlwind).
     */
    public static void tick(ServerPlayer player, PowerData data) {
        boolean fed = fed(player);
        boolean canRun = false;
        boolean was = data.is(Live.SPEEDING);
        if (data.has(HeroAbility.TOMMY_SPEED)) {
            canRun = data.toggle(Toggle.SUPER_SPEED) && player.isSprinting() && !player.isPassenger()
                    && !player.getAbilities().flying && fed;
            data.set(Live.SPEEDING, canRun);
        }
        boolean slowing = false;
        if (data.has(HeroAbility.TOMMY_TIME)) {
            if (data.toggle(Toggle.TOMMY_TIME) && !fed) {
                // Starving: his slow time runs out of strength and stops by itself.
                data.setToggle(Toggle.TOMMY_TIME, false);
                player.displayClientMessage(Component.translatable("message.enhanced.tommy_time.starving"), true);
            }
            slowing = data.toggle(Toggle.TOMMY_TIME) && !player.isSpectator();
            data.setSlowMo(slowing);
        }
        if (canRun) {
            player.causeFoodExhaustion(EnhancedConfig.TOMMY_EXHAUSTION.get().floatValue());
            player.resetFallDistance();
            if (!was) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.QUICKSILVER_WHOOSH.get(),
                        SoundSource.PLAYERS, 0.9F, 1.25F);
            }
        } else if (slowing) {
            player.causeFoodExhaustion((float) SpeedStrikes.value(SpeedConfig.TOMMY_TIME_EXHAUSTION, 0.01));
        }
        if (!data.acts(Power.QUICKSILVER)) {
            // (3.4: a custom hero with abilities of both speedsters has this upkeep once, from Quicksilver's tick.)
            SpeedSurfaces.tick(player, data);
            SpeedStrikes.tick(player, data);
        }
        applySpeed(player, data);
        whirlwindTick(player, data);
    }

    /** Super speed and step-up while Tommy runs; also called when the power changes. */
    public static void applySpeed(ServerPlayer player, PowerData data) {
        boolean running = data.has(HeroAbility.TOMMY_SPEED) && data.is(Live.SPEEDING);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, EnhancedConfig.TOMMY_SPEED_BONUS.get(), running,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PowerTicker.setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, 0.65, running);
    }

    public static boolean isRunning(Player player) {
        PowerData data = Powers.get(player);
        return data.has(HeroAbility.TOMMY_SPEED) && data.is(Live.SPEEDING);
    }

    // ---- Ability 4 (Sneak): Disarm Blitz ----

    /**
     * The Battle of Westview: Tommy circles everyone armed around him and is back before they notice, their weapons in
     * his arms. Creatures lose whatever they hold; players only their weapon.
     */
    private static void disarmBlitz(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.TOMMY_VIBRATE, now)) {
            return;
        }
        // A conjured body (a Loki illusion) is passed by: what it holds is a copy of Loki's, and snatching it would make one.
        List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(BLITZ_RADIUS), e -> e != player && e.isAlive() && !e.isSpectator()
                        && !Metal.isConjured(e) && e.distanceToSqr(player) <= BLITZ_RADIUS * BLITZ_RADIUS
                        && disarmableHand(e) != null && !Shelters.shelters(player, e)));
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enhanced.disarm.nothing"), true);
            return;
        }
        targets.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        data.startCooldown(HeroAbility.TOMMY_VIBRATE, now, BLITZ_COOLDOWN);
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
            ring(level, player, target.position(), target.getBbWidth() + 0.6);
            trail(level, player, from, target.position());
            from = target.position();
        }
        trail(level, player, from, player.position());
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
        if (!data.isReady(HeroAbility.TOMMY_WHIRLWIND, now) || WHIRLWINDS.containsKey(player.getUUID())) {
            return;
        }
        data.startCooldown(HeroAbility.TOMMY_WHIRLWIND, now, WHIRLWIND_COOLDOWN);
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
                e -> caughtByWhirlwind(e) && !Shelters.shelters(player, e))) {
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
        if (!data.isReady(HeroAbility.TOMMY_VIBRATE, now)) {
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
                data.startCooldown(HeroAbility.TOMMY_VIBRATE, now, VIBRATE_COOLDOWN);
                Vec3 start = player.position();
                player.teleportTo(level, target.x, target.y, target.z, player.getYRot(), player.getXRot());
                player.resetFallDistance();
                trail(level, player, start, target);
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
        if (!data.isReady(HeroAbility.TOMMY_ACCELERATE, now)) {
            return;
        }
        HitResult hit = Raycasts.any(player, ACCELERATE_RANGE,
                e -> e instanceof LivingEntity living && living.isAlive() && e != player && !e.isSpectator(), 1.0F);
        if (hit instanceof EntityHitResult entityHit) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            data.startCooldown(HeroAbility.TOMMY_ACCELERATE, now, ACCELERATE_COOLDOWN);
            if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.MOLECULAR_ACCELERATION, player, player), ACCELERATE_DAMAGE)) {
                Vec3 push = target.position().subtract(player.position());
                target.knockback(0.8, -push.x, -push.z); // (a wall that stopped the hit stops the push too)
            }
            burst(level, player, entityHit.getLocation());
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
        if (BlockRights.behindTheWall(player, level, pos)) {
            // 3.3: and a Hex keeps its own from outside
            player.displayClientMessage(Component.translatable("message.enhanced.telekinesis.protected"), true);
            return;
        }
        // Breaking a block is a player action: spawn protection, claims and adventure mode all get their say.
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (!player.mayInteract(level, pos) || player.blockActionRestricted(level, pos, mode)
                || CommonHooks.fireBlockBreak(level, mode, player, pos, state).isCanceled()) {
            return;
        }
        data.startCooldown(HeroAbility.TOMMY_ACCELERATE, now, ACCELERATE_COOLDOWN);
        level.destroyBlock(pos, !player.isCreative(), player);
        burst(level, player, Vec3.atCenterOf(pos));
    }

    // ---- effects ----

    /**
     * A teal-green ghost trail between two points, low along the ground like in the Battle of Westview (3.4: a custom
     * hero's in his own colours, here and in the ring and the burst).
     */
    private static void trail(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(80, delta.length() * 3);
        SpeedStrikes.Style style = SpeedStrikes.Style.of(player, Power.TOMMY);
        EnergyParticleOptions deep = new EnergyParticleOptions(style.color(), 1.2F);
        EnergyParticleOptions light = new EnergyParticleOptions(style.glow(), 0.9F);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
            level.sendParticles(i % 2 == 0 ? deep : light, p.x, p.y + 0.6, p.z, 1, 0.1, 0.4, 0.1, 0.0);
        }
    }

    /** A ring of trail around a disarmed creature: Tommy ran around it. */
    private static void ring(ServerLevel level, ServerPlayer player, Vec3 center, double radius) {
        EnergyParticleOptions light = new EnergyParticleOptions(SpeedStrikes.Style.of(player, Power.TOMMY).glow(), 1.0F);
        for (int i = 0; i < 20; i++) {
            double a = i * Mth.TWO_PI / 20;
            level.sendParticles(light, center.x + Math.cos(a) * radius, center.y + 0.4, center.z + Math.sin(a) * radius,
                    1, 0.02, 0.15, 0.02, 0.0);
        }
    }

    private static void burst(ServerLevel level, ServerPlayer player, Vec3 at) {
        level.sendParticles(new EnergyParticleOptions(SpeedStrikes.Style.of(player, Power.TOMMY).glow(), 1.4F), at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.12);
        level.sendParticles(new EnergyParticleOptions(0xE6FFF5, 1.0F), at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.05);
        level.playSound(null, at.x, at.y, at.z, ModSounds.MOLECULAR_BURST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
        WHIRLWINDS.remove(player.getUUID());
        data.set(Live.WHIRLWIND, false);
        data.set(Live.SPEEDING, false);
        data.setToggle(Toggle.TOMMY_TIME, false);
        data.setSlowMo(false);
        SpeedSurfaces.end(player, data);
        SpeedStrikes.end(player, data);
    }

    public static void clear() {
        WHIRLWINDS.clear();
    }

    private TommyAbilities() {
    }
}
