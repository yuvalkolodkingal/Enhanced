package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * Quicksilver (Peter Maximoff): the fastest man alive. He has two ways to be fast, and only one is on at a time:
 * <ul>
 *     <li><b>Quicksilver Time</b> (R): everything around him is slowed, whether he runs or not.</li>
 *     <li><b>Super Speed</b> (Sneak + R): only he is fast, the world runs at its normal pace.</li>
 * </ul>
 * In either mode he does everything faster (see {@link #actionSpeed()}), and he runs at super speed while he sprints.
 */
public final class QuicksilverAbilities {
    private static final ResourceLocation SPEED_ID = Enhanced.id("quicksilver_speed");
    private static final ResourceLocation STEP_ID = Enhanced.id("quicksilver_step");
    private static final ResourceLocation MINING_ID = Enhanced.id("quicksilver_mining");
    private static final ResourceLocation ATTACK_ID = Enhanced.id("quicksilver_attack");
    private static final ResourceLocation SWIM_ID = Enhanced.id("quicksilver_swim");
    /** Quicksilver Time while he is not running costs this part of the hunger that running in it costs. */
    private static final float IDLE_EXHAUSTION = 0.5F;
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
                if (player.isShiftKeyDown()) {
                    toggleSuperSpeed(player, data);
                } else {
                    toggleTime(player, data);
                }
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

    // ---- Ability 1: Quicksilver Time (R) and Super Speed (Sneak + R) ----

    /** R: Quicksilver Time on or off. Turning it on turns Super Speed off. */
    private static void toggleTime(ServerPlayer player, PowerData data) {
        boolean on = !data.quicksilverTime();
        if (on && !isFed(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.quicksilver_time.starving"), true);
            return;
        }
        data.setQuicksilverTime(on);
        data.setSuperSpeed(false);
        player.displayClientMessage(Component.translatable(on
                ? "message.enhanced.quicksilver_time.on" : "message.enhanced.quicksilver_time.off"), true);
        if (on) {
            whoosh(player);
        }
    }

    /** Sneak + R: Super Speed on or off. Turning it on turns Quicksilver Time off. */
    private static void toggleSuperSpeed(ServerPlayer player, PowerData data) {
        boolean on = !data.superSpeed();
        data.setSuperSpeed(on);
        data.setQuicksilverTime(false);
        player.displayClientMessage(Component.translatable(on
                ? "message.enhanced.quicksilver_speed.on" : "message.enhanced.quicksilver_speed.off"), true);
    }

    private static void whoosh(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.QUICKSILVER_WHOOSH.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Whether he has the strength to run: a full stomach, or no hunger at all (Creative). */
    private static boolean isFed(ServerPlayer player) {
        return player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0;
    }

    static void tick(ServerPlayer player, PowerData data) {
        boolean fed = isFed(player);
        if (data.quicksilverTime() && !fed) {
            // Starving: Quicksilver Time runs out of strength and stops by itself.
            data.setQuicksilverTime(false);
            player.displayClientMessage(Component.translatable("message.enhanced.quicksilver_time.starving"), true);
        }
        // Quicksilver Time slows the world whether he runs or not; Super Speed never does.
        boolean slowing = data.quicksilverTime() && !player.isSpectator();
        data.setSlowMo(slowing);

        boolean running = (data.quicksilverTime() || data.superSpeed()) && player.isSprinting() && !player.isSpectator()
                && !player.isPassenger() && fed;
        boolean wasRunning = data.is(Live.SPEEDING);
        data.set(Live.SPEEDING, running);
        float exhaustion = EnhancedConfig.QUICKSILVER_EXHAUSTION.get().floatValue();
        if (running) {
            player.causeFoodExhaustion(exhaustion);
            player.resetFallDistance();
            if (!wasRunning && data.superSpeed()) {
                whoosh(player);
            }
        } else if (slowing) {
            player.causeFoodExhaustion(exhaustion * IDLE_EXHAUSTION);
        }
        applySpeed(player, data);
    }

    /**
     * Super speed and step-up while he runs, and the faster mining, attacking and swimming while a mode is on. Also
     * called when the power changes, to take it all away again.
     */
    static void applySpeed(ServerPlayer player, PowerData data) {
        boolean quicksilver = data.power() == Power.QUICKSILVER;
        boolean running = quicksilver && data.is(Live.SPEEDING);
        boolean quick = quicksilver && isQuick(data);
        double faster = actionSpeed() - 1.0;
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, EnhancedConfig.QUICKSILVER_SPEED_BONUS.get(), running,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PowerTicker.setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, 0.65, running);
        PowerTicker.setModifier(player, Attributes.BLOCK_BREAK_SPEED, MINING_ID, faster, quick,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PowerTicker.setModifier(player, Attributes.ATTACK_SPEED, ATTACK_ID, faster, quick,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PowerTicker.setModifier(player, NeoForgeMod.SWIM_SPEED, SWIM_ID, faster, quick,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /**
     * Whether he is running at super speed right now: a mode is on and he is sprinting. The server decides (on a
     * client it is the synced state), and it is what fall damage, the field of view, Wall &amp; Water Run, the
     * running pose, the afterimages and the trails follow.
     */
    public static boolean isRunning(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.QUICKSILVER && data.is(Live.SPEEDING);
    }

    /** Whether a mode (Quicksilver Time or Super Speed) is on: he does everything faster. */
    public static boolean isQuick(Player player) {
        return isQuick(Powers.get(player));
    }

    private static boolean isQuick(PowerData data) {
        return data.power() == Power.QUICKSILVER && (data.quicksilverTime() || data.superSpeed());
    }

    /** How many times as fast he does everything while a mode is on (the config's {@code actionSpeed}). */
    public static double actionSpeed() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.QUICKSILVER_ACTION_SPEED.get() : 4.0;
    }

    /**
     * Using an item (eating, drinking, drawing a bow, loading a crossbow...) goes {@link #actionSpeed()} times as fast:
     * each tick takes the extra ticks off what is left of the use. It never goes below 1, so vanilla still ends the
     * use itself, and eaten food is eaten, a bow released, a crossbow loaded as ever. Fractions carry over from tick
     * to tick.
     *
     * @param remaining ticks left of the use, before this tick
     * @return ticks left after the extra ticks are taken off
     */
    public static int fasterUse(Player player, int remaining) {
        if (remaining <= 1 || !isQuick(player)) {
            return remaining;
        }
        double extra = actionSpeed() - 1.0;
        int skipped = (int) (Math.floor((player.tickCount + 1) * extra) - Math.floor(player.tickCount * extra));
        return Math.max(1, remaining - skipped);
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
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.QUICKSILVER_DASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
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
        InteractionHand hand = snatchable(target.getMainHandItem()) ? InteractionHand.MAIN_HAND
                : snatchable(target.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
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

    private static boolean snatchable(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(ModTags.CANNOT_BE_SNATCHED);
    }

    /** A grey speed blur between two points. */
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
