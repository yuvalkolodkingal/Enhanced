package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * N: Redwing, and Sneak + N: Redwing Strike (3.8, PLAN 8.4). Redwing ({@link RedwingEntity}, {@code redwingHealth}
 * 12, a red-and-black drone with a red eye) launches from his pack and flies two blocks over his shoulder: every {@link
 * #MARK_EVERY} ticks it marks every monster within {@code redwingMarkRadius} 32 of him (each glows for him, and only
 * for him, {@link #MARK_TICKS} ticks), and every {@code redwingZapGap} 20 ticks it zaps the nearest marked one within
 * {@code redwingZapRange} 16 of it that it can see ({@code redwing_laser}, {@code redwingZapDamage} 2). It flies home
 * into the pack after {@code redwingTicks} 1200, at N again, when he takes the wings off; shot down it never breaks up
 * messily (sparks, and it drops into the pack): "Redwing needs repairs." for {@code redwingRepairTicks} 600 ({@code
 * Toggle.REDWING_OUT} while it is out). Redwing Strike: Redwing (launched first if it is in the pack) rams the creature
 * he looks at within {@code strikeRange} 32, {@code strikeDamage} 8 and Slowness II for {@link #STRIKE_SLOW_TICKS}
 * ticks, then flies back to his shoulder; {@code strikeCooldown} 200 on N's slot.
 */
public final class Redwing {
    /** How often (ticks) Redwing marks, and for how long a mark lasts. */
    public static final int MARK_EVERY = 40;
    public static final int MARK_TICKS = 100;
    /** Redwing Strike's Slowness II. */
    public static final int STRIKE_SLOW_TICKS = 60;
    static final int STRIKE_SLOW_AMPLIFIER = 1;

    /** N: Redwing out of the pack, or home into it. */
    static boolean toggle(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.FALCON_REDWING)) {
            return false;
        }
        if (data.toggle(Toggle.REDWING_OUT)) {
            recall(player, data, true);
            return true;
        }
        return launch(player, data) != null;
    }

    /** Redwing flies out of his pack (refused while it is being repaired). */
    @Nullable
    static RedwingEntity launch(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        FalconState fs = FalconAbilities.data(player);
        if (fs.redwingDownUntil() > now) {
            long left = (fs.redwingDownUntil() - now + 19) / 20;
            player.displayClientMessage(Component.translatable("message.enhanced.falcon.redwing_repairs")
                    .append(" ").append(Component.translatable("message.enhanced.falcon.redwing_seconds", left)),
                    true);
            return null;
        }
        ServerLevel level = player.serverLevel();
        RedwingEntity drone = ModEntities.REDWING.get().create(level);
        if (drone == null) {
            return null;
        }
        Vec3 from = player.position().add(0, player.getBbHeight() * 0.8, 0)
                .subtract(Vec3.directionFromRotation(0.0F, player.getYRot()).scale(0.4));
        drone.moveTo(from.x, from.y, from.z, player.getYRot(), 0.0F);
        drone.setOwner(player);
        AttributeInstance health = drone.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(FalconAbilities.cfg(FalconConfig.REDWING_HEALTH, 12.0));
        }
        drone.setHealth(drone.getMaxHealth());
        level.addFreshEntity(drone);
        int ticks = FalconAbilities.cfg(FalconConfig.REDWING_TICKS, 1200);
        FalconAbilities.setData(player, fs.withRedwing(Optional.of(drone.getUUID()), now + ticks));
        data.setToggle(Toggle.REDWING_OUT, true);
        FalconAbilities.State state = FalconAbilities.state(player.getUUID());
        state.lastMark = Long.MIN_VALUE / 2;
        state.lastZap = now;
        FalconAbilities.sound(level, from, ModSounds.REDWING_LAUNCH.get(), 1.0F, 1.0F);
        FalconAbilities.send(level, player, FalconFxPayload.REDWING_OUT, List.of(from), drone.getId());
        return drone;
    }

    /** Redwing home into his pack ({@code sound}: with its whirr and the effect; quietly when he is going away). */
    static void recall(ServerPlayer player, PowerData data, boolean sound) {
        RedwingEntity drone = drone(player);
        if (drone != null) {
            if (sound) {
                ServerLevel level = player.serverLevel();
                FalconAbilities.sound(level, drone.position(), ModSounds.REDWING_RETURN.get(), 0.9F, 1.0F);
                FalconAbilities.send(level, player, FalconFxPayload.REDWING_IN, List.of(drone.position(),
                        player.position().add(0, player.getBbHeight() * 0.7, 0)), 0);
            }
            drone.discard();
        }
        forgetDrone(player);
        if (data.toggle(Toggle.REDWING_OUT)) {
            data.setToggle(Toggle.REDWING_OUT, false);
        }
        FalconAbilities.State state = FalconAbilities.peek(player.getUUID());
        if (state != null) {
            state.marks.clear();
        }
    }

    /** The attachment forgets the drone (its repairs are kept). */
    static void forgetDrone(ServerPlayer player) {
        FalconState fs = FalconAbilities.data(player);
        if (fs.redwing().isPresent() || fs.redwingUntil() != 0L) {
            FalconAbilities.setData(player, fs.withRedwing(Optional.empty(), 0L));
        }
    }

    /** His Redwing in his world, or null. */
    @Nullable
    public static RedwingEntity drone(ServerPlayer player) {
        Optional<java.util.UUID> id = FalconAbilities.data(player).redwing();
        if (id.isEmpty()) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(id.get());
        return entity instanceof RedwingEntity drone && drone.isAlive() ? drone : null;
    }

    /** Sneak + N: Redwing rams the creature he looks at (launched first if it is in the pack). */
    static boolean strike(ServerPlayer player, PowerData data) {
        HeroAbility row = HeroAbility.FALCON_REDWING_STRIKE;
        long now = player.level().getGameTime();
        if (!FalconAbilities.hasVariant(data, row)) {
            return false;
        }
        HeroAbility slot = FalconAbilities.slotRow(data, row);
        if (!data.isReady(slot, now)) {
            return false;
        }
        int range = FalconAbilities.cfg(FalconConfig.STRIKE_RANGE, 32);
        LivingEntity target = target(player, range);
        if (target == null) {
            return false;
        }
        RedwingEntity drone = drone(player);
        if (drone == null && data.toggle(Toggle.REDWING_OUT)) {
            recall(player, data, false);
        }
        if (drone == null) {
            drone = launch(player, data);
            if (drone == null) {
                return false;
            }
        }
        data.startCooldown(slot, now, FalconAbilities.cfg(FalconConfig.STRIKE_COOLDOWN, 200));
        drone.strike(target);
        ServerLevel level = player.serverLevel();
        FalconAbilities.sound(level, drone.position(), ModSounds.REDWING_STRIKE.get(), 1.0F, 1.0F);
        FalconAbilities.send(level, drone, FalconFxPayload.REDWING_STRIKE, List.of(drone.position(),
                target.getBoundingBox().getCenter()), target.getId());
        return true;
    }

    /** The creature Redwing Strike would take: the one he looks at within {@code range} that he may hurt. */
    @Nullable
    static LivingEntity target(ServerPlayer player, double range) {
        EntityHitResult hit = com.kolodkingal.enhanced.ability.Raycasts.entity(player, range, 0.8,
                e -> e instanceof LivingEntity living && FalconAbilities.mayHurt(player, living), 1.0F);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /** Redwing reached the creature it rams: the blow and the slowness; then it flies back to his shoulder. */
    static void strikeHit(RedwingEntity drone, LivingEntity target) {
        ServerPlayer owner = drone.ownerPlayer();
        if (owner == null || !(drone.level() instanceof ServerLevel level)) {
            return;
        }
        if (FalconAbilities.mayHurt(owner, target)) {
            FalconAbilities.hurt(owner, target, ModDamageTypes.REDWING_LASER,
                    FalconAbilities.cfg(FalconConfig.STRIKE_DAMAGE, 8.0), drone);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STRIKE_SLOW_TICKS,
                    STRIKE_SLOW_AMPLIFIER), owner);
            FalconAbilities.push(owner, target, drone.position(), 0.5);
        }
        Vec3 at = target.getBoundingBox().getCenter();
        FalconAbilities.sound(level, at, ModSounds.FALCON_DIVE_HIT.get(), 0.8F, 1.4F);
        FalconAbilities.send(level, drone, FalconFxPayload.REDWING_STRIKE, List.of(at), -1);
    }

    /** Redwing was shot down: it drops into the pack in a shower of sparks, and needs repairs. */
    static void shotDown(RedwingEntity drone) {
        ServerPlayer owner = drone.ownerPlayer();
        if (!(drone.level() instanceof ServerLevel level)) {
            return;
        }
        FalconAbilities.send(level, drone, FalconFxPayload.REDWING_IN, List.of(drone.position()), 1);
        FalconAbilities.sound(level, drone.position(), ModSounds.REDWING_ZAP.get(), 1.0F, 0.6F);
        if (owner == null) {
            return;
        }
        PowerData data = Powers.get(owner);
        long now = level.getGameTime();
        FalconState fs = FalconAbilities.data(owner);
        int repair = FalconAbilities.cfg(FalconConfig.REDWING_REPAIR_TICKS, 600);
        FalconAbilities.setData(owner, fs.withRedwing(Optional.empty(), 0L).withRedwingDownUntil(now + repair));
        data.setToggle(Toggle.REDWING_OUT, false);
        Powers.syncIfDirty(owner);
        FalconAbilities.State state = FalconAbilities.peek(owner.getUUID());
        if (state != null) {
            state.marks.clear();
        }
        FalconAbilities.say(owner, "message.enhanced.falcon.redwing_repairs");
    }

    /** Every tick of its owner: its time, the wings, the marks and the zaps. */
    static void ownerTick(ServerPlayer player, PowerData data, FalconAbilities.State state, long now) {
        state.marks.values().removeIf(until -> until < now);
        FalconState fs = FalconAbilities.data(player);
        if (fs.redwingDownUntil() != 0L && fs.redwingDownUntil() <= now) {
            FalconAbilities.setData(player, fs.withRedwingDownUntil(0L));
        }
        if (!data.toggle(Toggle.REDWING_OUT)) {
            return;
        }
        RedwingEntity drone = drone(player);
        if (drone == null) {
            recall(player, data, false);
            return;
        }
        if (now >= FalconAbilities.data(player).redwingUntil() || !FalconWings.hasWings(player, data)) {
            recall(player, data, true);
            return;
        }
        if (now - state.lastMark >= MARK_EVERY) {
            state.lastMark = now;
            mark(player, state, now);
        }
        int gap = FalconAbilities.cfg(FalconConfig.REDWING_ZAP_GAP, 20);
        if (now - state.lastZap >= gap && !drone.striking()) {
            if (zap(player, drone, state)) {
                state.lastZap = now;
            }
        }
    }

    /** Marks every monster near him: each glows for him alone (an effect only he is sent). */
    static void mark(ServerPlayer player, FalconAbilities.State state, long now) {
        double radius = FalconAbilities.cfg(FalconConfig.REDWING_MARK_RADIUS, 32.0);
        AABB round = player.getBoundingBox().inflate(radius);
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, round, e -> e != player
                && hostile(player, e) && e.distanceTo(player) <= radius)) {
            state.marks.put(e.getId(), now + MARK_TICKS);
            Net.sendToPlayer(player, new FalconFxPayload(FalconFxPayload.MARK, e.getId(), List.of(),
                    MARK_TICKS));
        }
    }

    /** Whether Redwing marks this creature: a monster, or one after him, that he may hurt. */
    static boolean hostile(ServerPlayer player, LivingEntity e) {
        if (!FalconAbilities.mayHurt(player, e)) {
            return false;
        }
        return e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == player;
    }

    /** One zap at the nearest marked creature Redwing sees within its range; whether it zapped. */
    static boolean zap(ServerPlayer player, RedwingEntity drone, FalconAbilities.State state) {
        if (state.marks.isEmpty()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        double range = FalconAbilities.cfg(FalconConfig.REDWING_ZAP_RANGE, 16.0);
        Vec3 eye = drone.position().add(0, drone.getBbHeight() * 0.5, 0);
        LivingEntity best = null;
        for (Map.Entry<Integer, Long> mark : state.marks.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey)).toList()) {
            Entity e = level.getEntity(mark.getKey());
            if (!(e instanceof LivingEntity living) || !living.isAlive() || !FalconAbilities.mayHurt(player, living)) {
                continue;
            }
            Vec3 to = living.getBoundingBox().getCenter();
            if (to.distanceTo(eye) > range || level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, drone)).getType() != HitResult.Type.MISS) {
                continue;
            }
            if (best == null || to.distanceToSqr(eye) < best.getBoundingBox().getCenter().distanceToSqr(eye)) {
                best = living;
            }
        }
        if (best == null) {
            return false;
        }
        Vec3 to = best.getBoundingBox().getCenter();
        float damage = (float) FalconAbilities.cfg(FalconConfig.REDWING_ZAP_DAMAGE, 2.0);
        // (3.8.2) A Chaos Shield in the way stops the laser at its surface, and pays for the zap, as every beam.
        WandaAbilities.ShieldStop wall = WandaAbilities.shieldAcross(level, player, eye, to, 1.0F);
        if (wall != null) {
            to = wall.point();
            WandaAbilities.stopBeam(level, player, wall, damage, 0xFF5A5A);
        } else {
            FalconAbilities.hurt(player, best, ModDamageTypes.REDWING_LASER, damage, drone);
        }
        FalconAbilities.sound(level, eye, ModSounds.REDWING_ZAP.get(), 0.8F, 1.0F);
        FalconAbilities.send(level, drone, FalconFxPayload.REDWING_ZAP, List.of(eye, to), best.getId());
        return true;
    }

    /** The creatures Redwing has marked for him now (their ids), for the tests and his HUD. */
    public static Set<Integer> marks(Player player) {
        FalconAbilities.State state = FalconAbilities.peek(player.getUUID());
        return state == null ? Set.of() : new HashSet<>(state.marks.keySet());
    }

    private Redwing() {
    }
}
