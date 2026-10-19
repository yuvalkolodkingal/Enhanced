package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.StonePowers;
import com.kolodkingal.enhanced.thanos.ThanosEffects;
import com.kolodkingal.enhanced.thanos.ThanosSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Soul Stone's Soul Drain (3.6): moved out of {@code ThanosAbilities} by the 3.6 skeleton, behaviour-neutral
 * (Thanos's B starts it with his numbers and his tick runs it), so that lane L2 can drain with the stone's numbers
 * ({@code [soul_stone]}) for anyone who may use it, without becoming Thanos. The chain locks onto one creature; every
 * half second it takes a little of its life and heals the drainer with it, until the key is let go, the chain is torn
 * (a hard blow, a Chaos Shield, the range) or the target dies; then the cooldown. Server only; per player.
 */
public final class SoulDrain {
    /** The drain takes its toll this often (ticks). */
    public static final int DRAIN_EVERY = 10;
    /** A drain is torn by this much damage taken within one second. */
    public static final float TEAR_DAMAGE = 60.0F;
    /** The chain holds past the range it was locked at, up to this many blocks more. */
    public static final double DRAIN_SLACK = 4.0;

    /**
     * The numbers of a drain, read when used (Thanos's are {@code ThanosConfig}'s; the stone's {@code [soul_stone]}'s).
     */
    public interface Numbers {
        /** The row whose cooldown and colour it is. */
        HeroAbility row();

        double range();

        double damagePerSecond();

        float healFraction();

        /** Food exhaustion per tick. */
        float exhaustion();

        /** Its cooldown (ticks) once it ends. */
        int cooldown();
    }

    /** A drain: whom it holds, since when, with which numbers. */
    private record Drain(UUID target, long start, Numbers numbers) {
    }

    /** Drainer -> his drain. Server thread only. */
    private static final Map<UUID, Drain> DRAINS = new HashMap<>();

    /** Whether the player's chain holds someone now. */
    public static boolean draining(Player player) {
        return DRAINS.containsKey(player.getUUID());
    }

    /** The chain locks onto {@code target} (the caller checked the cooldown, the hunger and the target). */
    public static void start(ServerPlayer player, PowerData data, LivingEntity target, Numbers numbers) {
        ServerLevel level = player.serverLevel();
        DRAINS.put(player.getUUID(), new Drain(target.getUUID(), level.getGameTime(), numbers));
        data.set(Live.SOUL_DRAINING, true);
        ThanosEffects.chains(level, player, List.of(chestOf(target)), DRAIN_EVERY + 2,
                AbilityColors.color(player, numbers.row(), StoneKind.SOUL.color()));
        ThanosSounds.CLENCH.play(level, player.position(), 0.8F, 1.1F);
        StoneKind.SOUL.voice().use(level, player.position(), 0.6F, 1.0F);
    }

    /**
     * One tick of the drainer's drain.
     *
     * @param stopped         whether his hand cannot hold the chain this tick (held, starving)
     * @param takenLastSecond the damage he took in the last second (a hard enough blow tears the chain)
     */
    public static void tick(ServerPlayer player, PowerData data, boolean stopped, float takenLastSecond, long now) {
        Drain drain = DRAINS.get(player.getUUID());
        if (drain == null) {
            data.set(Live.SOUL_DRAINING, false);
            return;
        }
        ServerLevel level = player.serverLevel();
        Numbers numbers = drain.numbers();
        LivingEntity target = level.getEntity(drain.target()) instanceof LivingEntity living ? living : null;
        // The key was let go of (or the hold was released for him), he is held or starving, or the chain cannot hold.
        if (!data.is(Live.SOUL_DRAINING) || player.isSpectator() || stopped || target == null || !target.isAlive()
                || StonePowers.untouchable(target) || player.distanceTo(target) > numbers.range() + DRAIN_SLACK
                || !player.hasLineOfSight(target)) {
            end(player, data);
            return;
        }
        if (takenLastSecond >= TEAR_DAMAGE) {
            // A hard enough blow tears the chain: a flinch, and the cooldown.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_BREAK,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1, false, false, false));
            end(player, data);
            return;
        }
        player.causeFoodExhaustion(numbers.exhaustion());
        long age = now - drain.start();
        if (age == 0 || age % DRAIN_EVERY != 0) {
            return;
        }
        Vec3 chest = chestOf(target);
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player,
                ThanosAbilities.handPosition(player), chest, 1.0F);
        if (shield != null) {
            // The chain stops at the surface of a Chaos Shield, and costs it.
            if (shield.owner() instanceof ServerPlayer wanda && player.canHarmPlayer(wanda)) {
                WandaAbilities.wear(wanda, 12.0F);
            }
            end(player, data);
            return;
        }
        float before = target.getHealth();
        float damage = (float) (numbers.damagePerSecond() * DRAIN_EVERY / 20.0);
        if (StonePowers.hurt(level, player, target, ModDamageTypes.STONE_SOUL, damage, player)) {
            float taken = Math.max(0.0F, before - Math.max(0.0F, target.getHealth()));
            player.heal(taken * numbers.healFraction());
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 0, false, false), player);
        ThanosEffects.chains(level, player, List.of(chest), DRAIN_EVERY + 2,
                AbilityColors.color(player, numbers.row(), StoneKind.SOUL.color()));
        ThanosSounds.SOUL_DRAIN.play(level, player.position(), 0.8F, 1.0F);
    }

    /** Ends the drain, if there is one, and starts its cooldown. */
    public static void end(ServerPlayer player, PowerData data) {
        data.set(Live.SOUL_DRAINING, false);
        Drain drain = DRAINS.remove(player.getUUID());
        if (drain != null) {
            data.startCooldown(drain.numbers().row(), player.level().getGameTime(), drain.numbers().cooldown());
        }
    }

    /** The middle of a creature's body, where the chain goes. */
    private static Vec3 chestOf(Entity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.6, 0.0);
    }

    /** His drain is gone (his death, session, world or power ended): no cooldown. */
    public static void forget(Player player) {
        DRAINS.remove(player.getUUID());
    }

    /** The server stopped. */
    public static void clear() {
        DRAINS.clear();
    }

    private SoulDrain() {
    }
}
