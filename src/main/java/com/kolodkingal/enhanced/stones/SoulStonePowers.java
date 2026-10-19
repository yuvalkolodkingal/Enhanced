package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Soul Stone (3.6). Use (held), the <b>Soul Drain</b>: Thanos's ({@link SoulDrain}, shared) with the stone's
 * numbers ({@code [soul_stone]}): it locks onto the creature he looks at, an orange chain takes a little of its life
 * every half second and heals him with half of it, until he lets go, a hard blow or the range tears the chain or the
 * target dies; then its cooldown. A boss loses half as much. He does not become Thanos: it is the stone. Sneak + use,
 * the <b>Soul Sight</b>: for a while every creature near glows orange through the walls for him ({@link
 * Live#SOUL_SIGHT}, the client's {@code SoulSight}), and the invisible glow for everyone. Server only.
 */
public final class SoulStonePowers {
    /** Hunger of a tick of the drain, and of one Soul Sight. */
    static final float DRAIN_EXHAUSTION = 0.02F;
    static final float SIGHT_EXHAUSTION = 0.8F;
    /** How long an invisible creature the Sight finds glows for everyone (ticks). */
    static final int REVEAL_TICKS = 100;

    private record Hurt(long time, float amount) {
    }

    /** Player -> when his Soul Sight ends. */
    private static final Map<UUID, Long> SIGHT_ENDS = new HashMap<>();
    /** Player -> the damage he took lately (a hard blow tears the chain). */
    private static final Map<UUID, Deque<Hurt>> TAKEN = new HashMap<>();

    /** The stone's drain numbers; a boss loses half. */
    private static SoulDrain.Numbers numbers(boolean boss) {
        return new SoulDrain.Numbers() {
            @Override
            public HeroAbility row() {
                return HeroAbility.THANOS_SOUL;
            }

            @Override
            public double range() {
                return SoulStoneConfig.DRAIN_RANGE.get();
            }

            @Override
            public double damagePerSecond() {
                double dps = SoulStoneConfig.DRAIN_DAMAGE_PER_SECOND.get();
                return boss ? dps * 0.5 : dps;
            }

            @Override
            public float healFraction() {
                return SoulStoneConfig.DRAIN_HEAL_FRACTION.get().floatValue();
            }

            @Override
            public float exhaustion() {
                return DRAIN_EXHAUSTION;
            }

            @Override
            public int cooldown() {
                return SoulStoneConfig.DRAIN_COOLDOWN.get();
            }
        };
    }

    // ---- the drain ----

    static void drainStart(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        if (SoulDrain.draining(player)) {
            return;
        }
        double range = SoulStoneConfig.DRAIN_RANGE.get();
        StoneAim.Aim aim = StoneAim.aim(player, range, 0.3, target -> StoneAim.mayStrike(player, target));
        LivingEntity target = aim.creature();
        if (target == null) {
            StoneAim.Aim any = StoneAim.aim(player, range, 0.3, t -> t instanceof Player);
            boolean spared = any.creature() instanceof Player other && !other.isCreative()
                    && !player.canHarmPlayer(other);
            player.displayClientMessage(Component.translatable(spared ? "message.enhanced.thanos.pvp_off"
                    : "message.enhanced.thanos.no_target"), true);
            return;
        }
        StoneUse.begin(cast);
        SoulDrain.start(player, cast.data(), target, numbers(MindStonePowers.boss(target)));
    }

    static boolean drainTick(StoneUse.Hold hold) {
        ServerPlayer player = hold.cast.player();
        if (!StoneUse.isThanos(player)) {
            // (The hero Thanos's own tick runs every drain of his; anyone else's runs here.)
            SoulDrain.tick(player, hold.cast.data(), false, takenLastSecond(player), player.level().getGameTime());
        }
        return SoulDrain.draining(player);
    }

    static void drainEnd(StoneUse.Hold hold) {
        ServerPlayer player = hold.cast.player();
        SoulDrain.end(player, hold.cast.data());
        hold.cast.startCooldown(SoulStoneConfig.DRAIN_COOLDOWN.get());
    }

    /** The player was hurt: a hard enough blow tears his chain. */
    public static void hurt(ServerPlayer player, float amount) {
        if (!SoulDrain.draining(player) || StoneUse.isThanos(player)) {
            return;
        }
        Deque<Hurt> taken = TAKEN.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        taken.addLast(new Hurt(player.level().getGameTime(), amount));
        while (taken.size() > 32) {
            taken.removeFirst();
        }
    }

    private static float takenLastSecond(ServerPlayer player) {
        Deque<Hurt> taken = TAKEN.get(player.getUUID());
        if (taken == null) {
            return 0.0F;
        }
        long now = player.level().getGameTime();
        taken.removeIf(h -> now - h.time() > 20 || now < h.time());
        float total = 0.0F;
        for (Hurt hurt : taken) {
            total += hurt.amount();
        }
        return total;
    }

    // ---- the sight ----

    static void sight(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = player.serverLevel();
        long now = cast.now();
        double range = SoulStoneConfig.SIGHT_RANGE.get();
        SIGHT_ENDS.put(player.getUUID(), now + SoulStoneConfig.SIGHT_TICKS.get());
        cast.data().set(Live.SOUL_SIGHT, true);
        cast.startCooldown(SoulStoneConfig.SIGHT_COOLDOWN.get());
        player.causeFoodExhaustion(SIGHT_EXHAUSTION);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && e.isInvisible() && e.distanceTo(player) <= range)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, REVEAL_TICKS, 0, false, false), player);
        }
        level.sendParticles(new EnergyParticleOptions(StoneKind.SOUL.color(), 1.4F), player.getX(), player.getY() + 1.0,
                player.getZ(), 20, 0.6, 0.8, 0.6, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SOUL_SIGHT.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        StoneKind.SOUL.voice().use(level, player.position(), 0.6F, 1.2F);
    }

    /** Whether the player's Soul Stone sight is open (the tests ask). */
    public static boolean seeing(Player player) {
        return SIGHT_ENDS.containsKey(player.getUUID());
    }

    static void tick(ServerPlayer player, PowerData data) {
        Long ends = SIGHT_ENDS.get(player.getUUID());
        if (ends != null && player.level().getGameTime() >= ends) {
            SIGHT_ENDS.remove(player.getUUID());
            if (!data.has(HeroAbility.THANOS_SOUL)) {
                data.set(Live.SOUL_SIGHT, false);
            }
        }
    }

    static void forget(Player player, PowerData data) {
        if (SIGHT_ENDS.remove(player.getUUID()) != null && !data.has(HeroAbility.THANOS_SOUL)) {
            data.set(Live.SOUL_SIGHT, false);
        }
        TAKEN.remove(player.getUUID());
    }

    static void clear() {
        SIGHT_ENDS.clear();
        TAKEN.clear();
    }

    private SoulStonePowers() {
    }
}
