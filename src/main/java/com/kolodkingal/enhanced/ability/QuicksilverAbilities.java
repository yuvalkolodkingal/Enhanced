package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Quicksilver (Peter Maximoff): the fastest man alive. */
public final class QuicksilverAbilities {
    private static final ResourceLocation SPEED_ID = Enhanced.id("quicksilver_speed");
    private static final ResourceLocation STEP_ID = Enhanced.id("quicksilver_step");
    public static final int BLITZ_RANGE = 24;
    public static final int BLITZ_HITS = 6;
    public static final float BLITZ_DAMAGE = 2.0F;
    public static final int BLITZ_COOLDOWN = 80;
    public static final int PICKPOCKET_RANGE = 24;
    public static final int PICKPOCKET_COOLDOWN = 100;

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        if (!down || player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                data.setQuicksilverTime(!data.quicksilverTime());
                player.displayClientMessage(Component.translatable(data.quicksilverTime()
                        ? "message.enhanced.quicksilver_time.on" : "message.enhanced.quicksilver_time.off"), true);
            }
            case 2 -> blitz(player, data);
            case 3 -> pickpocket(player, data);
            case 4 -> {
                data.setWallRun(!data.wallRun());
                player.displayClientMessage(Component.translatable(data.wallRun()
                        ? "message.enhanced.wall_run.on" : "message.enhanced.wall_run.off"), true);
            }
            case 5 -> {
                data.setWalkman(!data.walkman());
                player.displayClientMessage(Component.translatable(data.walkman()
                        ? "message.enhanced.walkman.on" : "message.enhanced.walkman.off"), true);
            }
            default -> {
            }
        }
    }

    // ---- the goggles (work for whoever wears them) ----

    public static boolean wearsGoggles(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.QUICKSILVER_GOGGLES.get());
    }

    static void gogglesTick(ServerPlayer player, PowerData data) {
        if (data.power() == Power.NONE && wearsGoggles(player)) {
            Powers.awaken(player, Power.QUICKSILVER);
        }
    }

    // ---- Quicksilver Time ----

    static void tick(ServerPlayer player, PowerData data) {
        boolean canRun = player.isSprinting() && data.quicksilverTime() && !player.isSpectator() && !player.isPassenger()
                && (player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0);
        boolean was = data.slowMo();
        data.setSlowMo(canRun);
        if (canRun) {
            player.causeFoodExhaustion(EnhancedConfig.QUICKSILVER_EXHAUSTION.get().floatValue());
            player.resetFallDistance();
            if (!was) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                        SoundSource.PLAYERS, 0.8F, 1.6F);
            }
        }
        applySpeed(player, data);
    }

    /** Super speed and step-up while Quicksilver Time is running; also called when the power changes. */
    static void applySpeed(ServerPlayer player, PowerData data) {
        boolean running = data.power() == Power.QUICKSILVER && data.slowMo();
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, EnhancedConfig.QUICKSILVER_SPEED_BONUS.get(), running,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PowerTicker.setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, 0.65, running);
    }

    public static boolean isRunning(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.QUICKSILVER && data.slowMo();
    }

    // ---- Ability 2: Speed Blitz ----

    private static void blitz(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(2, now)) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, BLITZ_RANGE, 0.3,
                e -> e instanceof LivingEntity living && living.isAlive() && e != player && !e.isSpectator(), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.blitz.no_target"), true);
            return;
        }
        LivingEntity target = (LivingEntity) hit.getEntity();
        Vec3 start = player.position();
        Vec3 spot = findSpotNextTo(player, target);
        if (spot == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.blitz.blocked"), true);
            return;
        }
        data.startCooldown(2, now, BLITZ_COOLDOWN);

        Vec3 toTarget = target.position().subtract(spot);
        float yaw = (float) (Math.atan2(-toTarget.x, toTarget.z) * (180 / Math.PI));
        player.teleportTo(level, spot.x, spot.y, spot.z, yaw, player.getXRot());
        streak(level, start, spot);

        for (int i = 0; i < BLITZ_HITS && target.isAlive(); i++) {
            target.invulnerableTime = 0;
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.SPEED_BLITZ, player, player), BLITZ_DAMAGE);
        }
        Vec3 push = target.position().subtract(player.position());
        target.knockback(1.0, -push.x, -push.z);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.8F);
    }

    /** A free spot right next to the target, on Quicksilver's side, or null if there is no room. */
    private static Vec3 findSpotNextTo(ServerPlayer player, LivingEntity target) {
        Vec3 away = player.position().subtract(target.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
        double gap = target.getBbWidth() * 0.5 + player.getBbWidth() * 0.5 + 0.4;
        for (double extra : new double[]{0.0, 0.5, 1.0, 1.5}) {
            Vec3 spot = target.position().add(away.scale(gap + extra));
            AABB box = player.getBoundingBox().move(spot.subtract(player.position()));
            if (player.level().noCollision(player, box)) {
                return spot;
            }
        }
        return null;
    }

    // ---- Ability 3: Pickpocket ----

    private static void pickpocket(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(3, now)) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, PICKPOCKET_RANGE, 0.3,
                e -> e instanceof LivingEntity living && living.isAlive() && e != player && !e.isSpectator(), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.pickpocket.no_target"), true);
            return;
        }
        LivingEntity target = (LivingEntity) hit.getEntity();
        InteractionHand hand = !target.getMainHandItem().isEmpty() ? InteractionHand.MAIN_HAND
                : !target.getOffhandItem().isEmpty() ? InteractionHand.OFF_HAND : null;
        if (hand == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.pickpocket.empty"), true);
            return;
        }
        ItemStack stolen = target.getItemInHand(hand).copy();
        target.setItemInHand(hand, ItemStack.EMPTY);
        if (target instanceof Mob mob) {
            mob.setGuaranteedDrop(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        if (!player.getInventory().add(stolen)) {
            player.drop(stolen, false);
        }
        data.startCooldown(3, now, PICKPOCKET_COOLDOWN);
        streak(level, player.getEyePosition(), target.getEyePosition());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.6F);
        player.displayClientMessage(Component.translatable("message.enhanced.pickpocket.stolen", stolen.getHoverName()), true);
    }

    /** A silver-blue streak between two points. */
    private static void streak(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.min(80, delta.length() * 3);
        EnergyParticleOptions silver = new EnergyParticleOptions(Power.QUICKSILVER.glow, 1.2F);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps))).add(0, 1.0, 0);
            level.sendParticles(silver, p.x, p.y, p.z, 1, 0.05, 0.3, 0.05, 0.0);
        }
    }

    private QuicksilverAbilities() {
    }
}
