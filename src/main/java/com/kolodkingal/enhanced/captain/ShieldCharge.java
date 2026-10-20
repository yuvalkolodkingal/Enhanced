package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * V: the Shield Charge, and with Sneak the Shield Bash (3.8, PLAN 3.4). The charge carries him {@code chargeDistance}
 * blocks in {@link #CHARGE_TICKS} ticks behind his raised shield (it blocks from the front all the way, {@link
 * ShieldRules#guarding}); the first creatures in his path are struck and thrown; a wall stops him, unhurt. The bash is a
 * point-blank blow of the shield that stuns ({@code bashStunTicks}: Slowness IV, and no jump); a boss takes the damage
 * only.
 */
public final class ShieldCharge {
    /** The charge's length in ticks (its distance is the config's). */
    public static final int CHARGE_TICKS = 12;
    /** It ends a little after its ticks at the latest, whatever happened. */
    static final int CHARGE_GRACE = 6;
    /** How far the charge throws them. */
    static final double CHARGE_THROW = 1.5;
    /** The bash's reach and cone. */
    static final double BASH_REACH = 3.0;
    static final double BASH_CONE = 60.0;
    /** The stun's Slowness (IV) and its jump modifier (none: the jump is taken away). */
    static final int STUN_AMPLIFIER = 3;
    static final ResourceLocation STUN_JUMP = Enhanced.id("cap_bash_stun");

    /** The creatures his bash has stunned: their jump comes back when the stun ends. */
    private static final Map<UUID, Stun> STUNS = new HashMap<>();

    private record Stun(ResourceKey<Level> level, long until) {
    }

    /** The charge begins. Returns whether it began. */
    public static boolean charge(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        CaptainAbilities.State state = CaptainAbilities.state(player.getUUID());
        if (!data.has(HeroAbility.CAPTAIN_SHIELD_CHARGE) || !data.isReady(HeroAbility.CAPTAIN_SHIELD_CHARGE, now)
                || state.chargeStart >= 0) {
            return false;
        }
        state.chargeStart = now;
        state.chargeFrom = player.position();
        state.chargeDir = CaptainAbilities.flat(player.getLookAngle());
        state.chargeHit.clear();
        data.startCooldown(HeroAbility.CAPTAIN_SHIELD_CHARGE, now, CaptainAbilities.cfg(
                CaptainConfig.CHARGE_COOLDOWN, 100));
        data.set(Live.CAP_CHARGING, true);
        drive(player, state);
        CaptainAbilities.sound(player.serverLevel(), player.position(), ModSounds.SHIELD_CHARGE.get(), 1.0F, 1.0F);
        CaptainAbilities.send(player.serverLevel(), player, CaptainFxPayload.CHARGE, List.of(player.position()), 0);
        return true;
    }

    /** How far the charge has carried him (flat). */
    public static double travelled(ServerPlayer player) {
        CaptainAbilities.State state = CaptainAbilities.peek(player.getUUID());
        if (state == null) {
            return 0.0;
        }
        return Math.hypot(player.getX() - state.chargeFrom.x, player.getZ() - state.chargeFrom.z);
    }

    /** Whether he is charging. */
    public static boolean charging(ServerPlayer player) {
        CaptainAbilities.State state = CaptainAbilities.peek(player.getUUID());
        return state != null && state.chargeStart >= 0;
    }

    static void tick(ServerPlayer player, PowerData data, CaptainAbilities.State state, long now) {
        if (state.chargeStart < 0) {
            return;
        }
        long t = now - state.chargeStart;
        double distance = CaptainAbilities.cfg(CaptainConfig.CHARGE_DISTANCE, 8.0);
        double done = travelled(player);
        strike(player, state);
        boolean wall = t > 1 && player.horizontalCollision;
        if (done >= distance - 0.05 || wall || t >= CHARGE_TICKS + CHARGE_GRACE) {
            end(player, data, state, wall);
            return;
        }
        drive(player, state);
    }

    /** Pushes him on along the charge (the rest of the distance, at most a tick's share). */
    private static void drive(ServerPlayer player, CaptainAbilities.State state) {
        double distance = CaptainAbilities.cfg(CaptainConfig.CHARGE_DISTANCE, 8.0);
        double step = Math.min(distance / CHARGE_TICKS, Math.max(0.0, distance - travelled(player)));
        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(state.chargeDir.x * step, Math.min(m.y, 0.0), state.chargeDir.z * step);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
    }

    /** The creatures in his path: struck once each, thrown on along his way. */
    private static void strike(ServerPlayer player, CaptainAbilities.State state) {
        ServerLevel level = player.serverLevel();
        AABB ahead = player.getBoundingBox().move(state.chargeDir.scale(0.6)).inflate(0.5, 0.2, 0.5);
        float damage = (float) CaptainAbilities.cfg(CaptainConfig.CHARGE_DAMAGE, 6.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, ahead,
                e -> !state.chargeHit.contains(e.getId()) && CaptainAbilities.mayHurt(player, e))) {
            state.chargeHit.add(e.getId());
            if (CaptainAbilities.hurt(player, e, ModDamageTypes.CAP_SLAM, damage, null)) {
                Vec3 behind = e.position().subtract(state.chargeDir);
                CaptainAbilities.push(e, behind, CHARGE_THROW, 0.3, player);
                CaptainAbilities.sound(level, e.position(), ModSounds.SHIELD_BASH.get(), 1.0F, 0.9F);
                CaptainAbilities.send(level, player, CaptainFxPayload.BASH, List.of(e.getBoundingBox().getCenter()),
                        1);
            }
        }
    }

    /** The charge ends (its distance run, a wall, or its time): he stops where he is. */
    static void end(ServerPlayer player, PowerData data, CaptainAbilities.State state, boolean wall) {
        state.chargeStart = -1;
        state.chargeHit.clear();
        data.set(Live.CAP_CHARGING, false);
        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(0.0, Math.min(m.y, 0.0), 0.0);
        player.hurtMarked = true;
        if (wall) {
            CaptainAbilities.sound(player.serverLevel(), player.position(), ModSounds.SHIELD_BLOCK.get(), 0.8F,
                    0.8F);
            CaptainAbilities.send(player.serverLevel(), player, CaptainFxPayload.CHARGE, List.of(player.position()),
                    1);
        }
    }

    /** The bash: the creature in front struck and stunned. Returns whether one was struck. */
    public static boolean bash(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.CAPTAIN_SHIELD_BASH) || !data.isReady(HeroAbility.CAPTAIN_SHIELD_BASH, now)) {
            return false;
        }
        data.startCooldown(HeroAbility.CAPTAIN_SHIELD_BASH, now, CaptainAbilities.cfg(CaptainConfig.BASH_COOLDOWN,
                60));
        ServerLevel level = player.serverLevel();
        LivingEntity target = CaptainAbilities.inFront(player, BASH_REACH, BASH_CONE);
        player.swing(ShieldRules.shieldHand(player) != null ? ShieldRules.shieldHand(player)
                : net.minecraft.world.InteractionHand.MAIN_HAND, true);
        if (target == null) {
            CaptainAbilities.send(level, player, CaptainFxPayload.BASH, List.of(), 0);
            return false;
        }
        if (CaptainAbilities.hurt(player, target, ModDamageTypes.CAP_SLAM, (float) CaptainAbilities.cfg(
                CaptainConfig.BASH_DAMAGE, 5.0), null)) {
            if (!CaptainAbilities.isBoss(target)) {
                stun(target, CaptainAbilities.cfg(CaptainConfig.BASH_STUN_TICKS, 30), player);
            }
            CaptainAbilities.push(target, player.position(), 0.4, 0.0, player);
        }
        CaptainAbilities.sound(level, target.position(), ModSounds.SHIELD_BASH.get(), 1.1F, 1.0F);
        CaptainAbilities.send(level, player, CaptainFxPayload.BASH, List.of(target.getBoundingBox().getCenter()), 2);
        return true;
    }

    /** Stuns a creature for {@code ticks}: Slowness IV and its jump taken away (both come back when it ends). */
    static void stun(LivingEntity target, int ticks, Entity by) {
        if (ticks <= 0) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, STUN_AMPLIFIER), by);
        AttributeInstance jump = target.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.removeModifier(STUN_JUMP);
            jump.addTransientModifier(new AttributeModifier(STUN_JUMP, -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        STUNS.put(target.getUUID(), new Stun(target.level().dimension(), target.level().getGameTime() + ticks));
    }

    /** Whether the creature is stunned by a bash now. */
    public static boolean stunned(LivingEntity target) {
        Stun stun = STUNS.get(target.getUUID());
        return stun != null && target.level().getGameTime() < stun.until();
    }

    /** Every server tick: the stuns that ran out give the jump back. */
    static void tickStuns(MinecraftServer server) {
        if (STUNS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Stun>> it = STUNS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Stun> entry = it.next();
            ServerLevel level = server.getLevel(entry.getValue().level());
            if (level == null) {
                it.remove();
                continue;
            }
            if (level.getGameTime() < entry.getValue().until()) {
                continue;
            }
            it.remove();
            if (level.getEntity(entry.getKey()) instanceof LivingEntity living) {
                AttributeInstance jump = living.getAttribute(Attributes.JUMP_STRENGTH);
                if (jump != null) {
                    jump.removeModifier(STUN_JUMP);
                }
            }
        }
    }

    static void clearStuns() {
        STUNS.clear();
    }

    private ShieldCharge() {
    }
}
