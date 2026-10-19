package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.healing.HealingBurst;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * B: the Taunt, and the two tricks of his that make monsters mind him or lose him (3.7).
 *
 * <p>The Taunt: a pose (a wave, the finger guns or a heart with both hands) and a joke over his head (a bubble every
 * client within {@link DeadpoolLines#BUBBLE_RANGE} reads in its own language); every hostile creature within {@code
 * tauntRadius} that is no boss turns on him and keeps at him for {@code tauntTicks} (so his friends are spared). Sneak
 * + B: the Chimichanga Break, the Healing Burst of the healing factor; with a chimichanga in his bag he eats it (a
 * munch, a happy sigh): {@code chimichangaFraction} of his health instead of the burst's share, and Absorption II for
 * 30 seconds. Refused while poisoned (the burst says so): nothing is eaten, nothing waits. Sneak + N: the Smoke Vanish,
 * a grey-red puff, Invisibility for {@code smokeTicks}, and every monster within {@link #SMOKE_RADIUS} loses him for as
 * long. The keeping and the losing are {@link DeadpoolEvents}' (the creatures' change of target).
 */
public final class Taunt {
    /** The poses of the Taunt. */
    public static final int WAVE = 0;
    public static final int FINGER_GUNS = 1;
    public static final int HEART = 2;
    public static final int POSES = 3;
    /** How far the smoke makes monsters lose him. */
    public static final double SMOKE_RADIUS = 16.0;
    /** The chimichanga's Absorption: II for 30 seconds. */
    static final int ABSORPTION_TICKS = 600;
    static final int ABSORPTION_LEVEL = 1;

    /** Each taunted creature: whom it keeps at, until when. */
    record Held(UUID player, long until) {
    }

    private static final Map<UUID, Held> TAUNTED = new HashMap<>();
    private static final Map<UUID, Held> LOST = new HashMap<>();

    /** B: the Taunt. Returns the creatures that turned on him. */
    static int taunt(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_TAUNT)) {
            return 0;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.DEADPOOL_TAUNT, now)) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        int pose = level.random.nextInt(POSES);
        int line = DeadpoolLines.pick(DeadpoolLines.Group.TAUNT, level.random);
        int count = 0;
        double radius = DeadpoolAbilities.cfg(DeadpoolConfig.TAUNT_RADIUS, 16);
        long until = now + DeadpoolAbilities.cfg(DeadpoolConfig.TAUNT_TICKS, 100);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                m -> m.isAlive() && m.distanceTo(player) <= radius && tauntable(m))) {
            LOST.remove(mob.getUUID());
            TAUNTED.remove(mob.getUUID());
            mob.setTarget(player);
            TAUNTED.put(mob.getUUID(), new Held(player.getUUID(), until));
            count++;
        }
        data.startCooldown(HeroAbility.DEADPOOL_TAUNT, now, DeadpoolAbilities.cfg(DeadpoolConfig.TAUNT_COOLDOWN, 200));
        DeadpoolLines.sayLine(player, DeadpoolLines.Group.TAUNT, line, pose);
        DeadpoolAbilities.sound(level, player.position(), ModSounds.DEADPOOL_TAUNT.get(), 1.0F, 1.0F);
        return count;
    }

    /** Whether the Taunt turns this creature: a hostile one that is no boss. */
    public static boolean tauntable(Mob mob) {
        return mob instanceof Enemy && !mob.getType().is(ModTags.BOSSES);
    }

    /** Sneak + B: the Chimichanga Break. */
    static boolean chimichangaBreak(ServerPlayer player, PowerData data) {
        if (!DeadpoolAbilities.hasVariant(data, HeroAbility.DEADPOOL_CHIMICHANGA)) {
            return false;
        }
        long now = player.level().getGameTime();
        DeadpoolAbilities.State state = DeadpoolAbilities.state(player.getUUID());
        if (now < state.breakReadyAt) {
            player.displayClientMessage(Component.translatable("message.enhanced.deadpool.break_wait",
                    (state.breakReadyAt - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ItemStack snack = chimichanga(player);
        boolean eats = !snack.isEmpty();
        double fraction = eats ? DeadpoolAbilities.cfg(DeadpoolConfig.CHIMICHANGA_FRACTION, 1.0)
                : HealingBurst.fraction();
        if (!HealingBurst.burst(player, fraction)) {
            return false; // (refused, poisoned: the burst said so; nothing eaten, nothing waits)
        }
        ServerLevel level = player.serverLevel();
        if (eats) {
            if (!player.getAbilities().instabuild) {
                snack.shrink(1);
            }
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS, ABSORPTION_LEVEL));
            DeadpoolAbilities.send(level, player, DeadpoolFxPayload.EAT, List.of(player.position()), 1);
            DeadpoolAbilities.sound(level, player.position(), ModSounds.CHIMICHANGA_MUNCH.get(), 1.0F, 1.0F);
            DeadpoolLines.say(player, DeadpoolLines.Group.CHIMICHANGA, true);
        } else {
            DeadpoolAbilities.send(level, player, DeadpoolFxPayload.EAT, List.of(player.position()), 0);
        }
        state.breakReadyAt = now + DeadpoolAbilities.scaled(data, HeroAbility.DEADPOOL_CHIMICHANGA,
                HealingBurst.cooldown());
        return true;
    }

    /** The first chimichanga in his hands or bag, or the empty stack. */
    static ItemStack chimichanga(ServerPlayer player) {
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (stack.is(ModItems.CHIMICHANGA.get())) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.CHIMICHANGA.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Sneak + N: the Smoke Vanish. */
    static boolean smoke(ServerPlayer player, PowerData data) {
        if (!DeadpoolAbilities.hasVariant(data, HeroAbility.DEADPOOL_SMOKE)) {
            return false;
        }
        long now = player.level().getGameTime();
        DeadpoolAbilities.State state = DeadpoolAbilities.state(player.getUUID());
        if (now < state.smokeReadyAt) {
            player.displayClientMessage(Component.translatable("message.enhanced.deadpool.smoke_wait",
                    (state.smokeReadyAt - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int ticks = DeadpoolAbilities.cfg(DeadpoolConfig.SMOKE_TICKS, 80);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        ServerLevel level = player.serverLevel();
        long until = now + ticks;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SMOKE_RADIUS),
                m -> m.isAlive() && m.distanceTo(player) <= SMOKE_RADIUS)) {
            TAUNTED.remove(mob.getUUID());
            LOST.put(mob.getUUID(), new Held(player.getUUID(), until));
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
        Vec3 at = player.position();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 40, 1.0, 0.8, 1.0, 0.02);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.6, at.z, 12, 0.8, 0.4, 0.8, 0.01);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.78F, 0.06F, 0.18F), 1.6F), at.x, at.y + 1.0,
                at.z, 24, 1.0, 0.8, 1.0, 0.0);
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.SMOKE, List.of(at), ticks);
        DeadpoolAbilities.sound(level, at, ModSounds.DEADPOOL_SMOKE.get(), 1.0F, 1.0F);
        state.smokeReadyAt = now + DeadpoolAbilities.scaled(data, HeroAbility.DEADPOOL_SMOKE,
                DeadpoolAbilities.cfg(DeadpoolConfig.SMOKE_COOLDOWN, 400));
        return true;
    }

    /**
     * Whether a creature's change of target to {@code next} must not happen: a taunted one keeps at its taunter (no
     * other target while the taunt lasts and he is there), and one the smoke blinded does not find him again.
     */
    static boolean keepsOff(Mob mob, LivingEntity next, long now) {
        Held lost = LOST.get(mob.getUUID());
        if (lost != null) {
            if (now > lost.until()) {
                LOST.remove(mob.getUUID());
            } else if (next != null && next.getUUID().equals(lost.player())) {
                return true;
            }
        }
        Held held = TAUNTED.get(mob.getUUID());
        if (held != null) {
            if (now > held.until()) {
                TAUNTED.remove(mob.getUUID());
                return false;
            }
            if (next == null || !next.getUUID().equals(held.player())) {
                net.minecraft.world.entity.player.Player taunter = mob.level().getPlayerByUUID(held.player());
                if (taunter != null && taunter.isAlive() && !taunter.isSpectator()
                        && taunter.level() == mob.level() && !com.kolodkingal.enhanced.healing.HealingFactor.isDown(
                        taunter)) {
                    return true;
                }
                TAUNTED.remove(mob.getUUID());
            }
        }
        return false;
    }

    /** Whether the creature is held by a taunt right now (for the tests and the HUD). */
    public static boolean taunted(Mob mob) {
        Held held = TAUNTED.get(mob.getUUID());
        return held != null && mob.level().getGameTime() <= held.until();
    }

    static void forget(UUID player) {
        TAUNTED.values().removeIf(h -> h.player().equals(player));
        LOST.values().removeIf(h -> h.player().equals(player));
    }

    static void clear() {
        TAUNTED.clear();
        LOST.clear();
    }

    private Taunt() {
    }
}
