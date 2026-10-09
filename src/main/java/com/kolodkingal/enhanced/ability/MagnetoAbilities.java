package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Magneto, the Master of Magnetism. */
public final class MagnetoAbilities {
    /** Projectiles the shield already caught and dropped are tagged so they are not caught again while falling. */
    public static final String DEMAGNETIZED_TAG = "enhanced:demagnetized";
    private static final double SHIELD_RADIUS = 4.5;

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        switch (ability) {
            case 1 -> {
                if (down) {
                    Telekinesis.press(player, Telekinesis.Mode.MAGNETO);
                }
            }
            case 2 -> data.setShrapnelActive(down && !player.isSpectator());
            case 3 -> {
                if (down) {
                    disarm(player, data);
                }
            }
            case 4 -> {
                if (down) {
                    toggleShield(player, data);
                }
            }
            case 5 -> {
                if (down) {
                    PowerTicker.toggleFlight(player);
                }
            }
            default -> {
            }
        }
    }

    static void tick(ServerPlayer player, PowerData data) {
        if (data.shrapnelActive()) {
            shrapnelTick(player, data);
        }
        if (data.magneticShield() && !player.isSpectator()) {
            shieldTick(player);
        }
    }

    // ---- the helmet (works for whoever wears it) ----

    public static boolean wearsHelmet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MAGNETO_HELMET.get());
    }

    static void helmetTick(ServerPlayer player, PowerData data) {
        if (!wearsHelmet(player)) {
            return;
        }
        if (data.power() == Power.NONE) {
            Powers.awaken(player, Power.MAGNETO);
        }
        // Mental immunity: Nausea never sticks (the effect event stops new Nausea; this cleans up old Nausea).
        if (player.hasEffect(MobEffects.CONFUSION)) {
            player.removeEffect(MobEffects.CONFUSION);
        }
    }

    // ---- Ability 2: Iron Shrapnel Storm ----

    private static void shrapnelTick(ServerPlayer player, PowerData data) {
        int interval = EnhancedConfig.SHRAPNEL_FIRE_INTERVAL.get();
        if (player.tickCount % interval != 0) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            if (data.ironCharge() <= 0) {
                int melted = meltIron(player.getInventory());
                if (melted <= 0) {
                    data.setShrapnelActive(false);
                    player.displayClientMessage(Component.translatable("message.enhanced.shrapnel.no_iron"), true);
                    return;
                }
                data.setIronCharge(melted);
            }
            data.setIronCharge(data.ironCharge() - 1);
        }

        ServerLevel level = player.serverLevel();
        IronShrapnelEntity shard = new IronShrapnelEntity(level, player);
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 origin = player.getEyePosition().add(look.scale(0.7)).add(right.scale(0.25)).add(0, -0.2, 0);
        shard.setPos(origin.x, origin.y, origin.z);
        shard.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.5F, 2.5F);
        shard.configure(EnhancedConfig.SHRAPNEL_DAMAGE.get().floatValue(), EnhancedConfig.SHRAPNEL_PIERCE.get());
        level.addFreshEntity(shard);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.5F,
                1.6F + player.getRandom().nextFloat() * 0.4F);
    }

    /** Shards one iron item is worth, or 0 if it can't be used. */
    public static int shardValue(ItemStack stack) {
        if (stack.isEmpty() || stack.isEnchanted()) {
            return 0;
        }
        if (stack.is(Items.IRON_NUGGET)) {
            return 1;
        }
        if (stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON)) {
            return 9;
        }
        if (stack.is(Items.IRON_BLOCK) || stack.is(Items.RAW_IRON_BLOCK)) {
            return 81;
        }
        if (stack.is(ModTags.SHRAPNEL_AMMO)) {
            return 18;
        }
        return 0;
    }

    /** Priority when melting: plain iron first (smallest first), tools and armor last. */
    private static int meltPriority(ItemStack stack) {
        int value = shardValue(stack);
        if (value == 0) {
            return Integer.MAX_VALUE;
        }
        boolean plainIron = stack.is(Items.IRON_NUGGET) || stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON)
                || stack.is(Items.IRON_BLOCK) || stack.is(Items.RAW_IRON_BLOCK);
        return plainIron ? value : 1000;
    }

    /** Melts the best iron item in the inventory into shards. Returns the shards gained (0 = no iron). */
    static int meltIron(Inventory inventory) {
        int bestSlot = -1;
        int bestPriority = Integer.MAX_VALUE;
        for (int i = 0; i < inventory.items.size(); i++) {
            int p = meltPriority(inventory.items.get(i));
            if (p < bestPriority) {
                bestPriority = p;
                bestSlot = i;
            }
        }
        if (bestSlot < 0) {
            return 0;
        }
        ItemStack stack = inventory.items.get(bestSlot);
        int value = shardValue(stack);
        stack.shrink(1);
        inventory.setChanged();
        return value;
    }

    /** Shards available right now (charge + everything in the inventory), for the HUD. */
    public static int countAmmo(Player player, PowerData data) {
        int total = data.ironCharge();
        for (ItemStack stack : player.getInventory().items) {
            total += shardValue(stack) * stack.getCount();
        }
        return total;
    }

    // ---- Ability 3: Armor & Weapon Strip ----

    private static void disarm(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(3, now)) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, EnhancedConfig.DISARM_RANGE.get(), 0.2,
                e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator() && e != player, 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.disarm.no_target"), true);
            return;
        }
        LivingEntity target = (LivingEntity) hit.getEntity();
        RandomSource random = player.getRandom();
        int ripped = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.is(ModTags.MAGNETIC_ITEMS) || stack.is(ModTags.MAGNETIC_ARMOR))) {
                continue;
            }
            target.setItemSlot(slot, ItemStack.EMPTY);
            ItemEntity item = new ItemEntity(level, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), stack.copy());
            item.setDeltaMovement((random.nextDouble() - 0.5) * 0.4, 0.3 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.4);
            item.setPickUpDelay(40);
            level.addFreshEntity(item);
            ripped++;
        }
        if (ripped == 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.disarm.no_metal"), true);
            return;
        }
        data.startCooldown(3, now, EnhancedConfig.DISARM_COOLDOWN.get());
        beam(level, player.getEyePosition(), target.position().add(0, target.getBbHeight() * 0.6, 0));
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** A line of magnetic sparks between two points. */
    static void beam(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(64, delta.length() * 2);
        EnergyParticleOptions spark = new EnergyParticleOptions(Power.MAGNETO.glow, 0.9F);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
            level.sendParticles(spark, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    // ---- Ability 4: Magnetic Shield ----

    private static void toggleShield(ServerPlayer player, PowerData data) {
        boolean on = !data.magneticShield();
        data.setMagneticShield(on);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.magnetic_shield.on" : "message.enhanced.magnetic_shield.off"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
    }

    public static boolean hasShieldUp(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.MAGNETO && data.magneticShield() && !player.isSpectator();
    }

    public static boolean isMetalProjectile(Entity entity) {
        return entity instanceof Projectile && entity.getType().is(ModTags.METALLIC_PROJECTILES);
    }

    private static void shieldTick(ServerPlayer player) {
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(SHIELD_RADIUS + 3),
                p -> isMetalProjectile(p) && p.getOwner() != player && !p.getPersistentData().getBoolean(DEMAGNETIZED_TAG))) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toPlayer = center.subtract(projectile.position());
            double reach = SHIELD_RADIUS + velocity.length();
            if (velocity.lengthSqr() > 0.09 && velocity.dot(toPlayer) > 0 && toPlayer.lengthSqr() < reach * reach) {
                repel(player, projectile);
            }
        }
    }

    /** The field catches a metal projectile: 50/50 it drops dead or flies back at whoever shot it. */
    public static void repel(ServerPlayer magneto, Projectile projectile) {
        ServerLevel level = magneto.serverLevel();
        Entity shooter = projectile.getOwner();
        boolean canReturn = shooter instanceof LivingEntity living && living.isAlive() && shooter.level() == level
                && shooter.distanceTo(magneto) < 64;
        if (canReturn && magneto.getRandom().nextDouble() < EnhancedConfig.SHIELD_REFLECT_CHANCE.get()) {
            Vec3 target = shooter.position().add(0, shooter.getBbHeight() * 0.6, 0);
            Vec3 dir = target.subtract(projectile.position()).normalize();
            double speed = Math.max(projectile.getDeltaMovement().length(), 1.6);
            projectile.setDeltaMovement(dir.scale(speed));
            if (projectile instanceof AbstractArrow arrow && !(projectile instanceof ThrownTrident)) {
                // Magneto gets the credit, but nobody gains the right to pick up an arrow they couldn't before.
                AbstractArrow.Pickup pickup = arrow.pickup;
                arrow.setOwner(magneto);
                arrow.pickup = pickup;
            }
            // Tridents keep their owner, so Loyalty and pickup still belong to whoever threw them.
            level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.6F);
        } else {
            RandomSource random = magneto.getRandom();
            projectile.setDeltaMovement((random.nextDouble() - 0.5) * 0.05, -0.05, (random.nextDouble() - 0.5) * 0.05);
            projectile.getPersistentData().putBoolean(DEMAGNETIZED_TAG, true);
            level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.CHAIN_FALL, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        level.sendParticles(new EnergyParticleOptions(Power.MAGNETO.glow, 1.0F), projectile.getX(), projectile.getY(), projectile.getZ(),
                8, 0.15, 0.15, 0.15, 0.05);
    }

    private MagnetoAbilities() {
    }
}
