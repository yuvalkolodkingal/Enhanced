package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Mark 1's moves (3.8, PLAN 5.7): the cave suit, crude grey iron, tough and slow. R the Flamethrower (held, at
 * most {@code flameMaxTicks}: a cone from both forearms that hurts and sets alight; blocks catch fire only with {@code
 * mark1FlameSetsBlocks} and mobGriefing); G the Rocket Boost (it cannot fly: the boots fire up, then forward, then
 * sputter out and he crash-lands, the suit taking the blow); V the Arm Rocket; B the Brace (held: feet planted, arms
 * crossed, incoming damage cut, no knockback, rooted); N the Heavy Punch.
 */
public final class Mark1Moves implements SuitMoves {
    public static final Mark1Moves INSTANCE = new Mark1Moves();

    private static final ResourceLocation BRACE_ROOT = Enhanced.id("iron_mark1_brace_root");
    private static final ResourceLocation BRACE_KNOCKBACK = Enhanced.id("iron_mark1_brace_knockback");

    /** The Brace's longest hold. */
    public static final int BRACE_MAX_TICKS = 100;
    /** Ticks between two of the Flamethrower's hits. */
    public static final int FLAME_GAP = 10;
    /** The crash's dust ring reaches this far. */
    public static final double CRASH_RADIUS = 2.0;

    private static final Map<UUID, State> STATES = new HashMap<>();

    static final class State {
        long flameStart = -1L;
        long braceStart = -1L;
        /** The Rocket Boost: when it began (-1: none), and whether he is falling to the crash. */
        long boostStart = -1L;
        boolean falling;
        long fallGuardUntil;
    }

    static State state(UUID id) {
        return STATES.computeIfAbsent(id, k -> new State());
    }

    private Mark1Moves() {
    }

    @Override
    public boolean ownsG() {
        return true;
    }

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        State s = state(player.getUUID());
        long now = player.level().getGameTime();
        if (!down) {
            switch (ability) {
                case 1 -> stopFlame(player, data, s, now);
                case 4 -> stopBrace(player, s);
                default -> {
                }
            }
            return;
        }
        switch (ability) {
            case 1 -> startFlame(player, data, s, now);
            case 2 -> boost(player, data, s, now);
            case 3 -> armRocket(player, data, now);
            case 4 -> startBrace(player, s, now);
            case 5 -> heavyPunch(player, data, now);
            default -> {
            }
        }
    }

    @Override
    public void tick(ServerPlayer player, PowerData data, long now) {
        State s = state(player.getUUID());
        if (s.flameStart >= 0) {
            flameTick(player, data, s, now);
        }
        if (s.braceStart >= 0) {
            if (now - s.braceStart >= BRACE_MAX_TICKS
                    || !SuitEnergy.drain(player, IronManAbilities.cfg(IronManConfig.MARK1_BRACE_ENERGY_PER_SECOND,
                    2.0))) {
                stopBrace(player, s);
            }
        }
        boostTick(player, data, s, now);
    }

    @Override
    public void end(ServerPlayer player, PowerData data) {
        State s = STATES.get(player.getUUID());
        if (s == null) {
            return;
        }
        long now = player.level().getGameTime();
        stopFlame(player, data, s, now);
        stopBrace(player, s);
        if (s.boostStart >= 0) {
            // (The boost ends where it is; the crash still lands softly.)
            s.boostStart = -1L;
            s.falling = true;
            data.set(Live.IRON_DASHING, false);
        }
    }

    // ---- R: the Flamethrower ----

    private static void startFlame(ServerPlayer player, PowerData data, State s, long now) {
        if (s.flameStart >= 0 || !IronManAbilities.ready(data, 1, now)) {
            return;
        }
        if (SuitEnergy.empty(player) && !player.isCreative()) {
            SuitEnergy.refuse(player);
            return;
        }
        s.flameStart = now;
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.FLAME_ON, List.of(player.position()), 0);
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.MARK1_FLAMETHROWER.get(), 1.0F,
                1.0F);
    }

    private static void flameTick(ServerPlayer player, PowerData data, State s, long now) {
        int max = IronManAbilities.cfg(IronManConfig.MARK1_FLAME_MAX_TICKS, 60);
        long t = now - s.flameStart;
        if (t >= max || !SuitEnergy.drain(player, IronManAbilities.cfg(IronManConfig.MARK1_FLAME_ENERGY_PER_SECOND,
                4.0))) {
            stopFlame(player, data, s, now);
            return;
        }
        ServerLevel level = player.serverLevel();
        double length = IronManAbilities.cfg(IronManConfig.MARK1_FLAME_LENGTH, 5.0);
        double half = IronManAbilities.cfg(IronManConfig.MARK1_FLAME_ANGLE, 30.0) / 2.0;
        Vec3 look = player.getLookAngle();
        for (boolean left : new boolean[]{false, true}) {
            Vec3 from = IronManAbilities.palm(player, left);
            for (int i = 0; i < 3; i++) {
                double reach = 0.5 + level.random.nextDouble() * length;
                Vec3 at = from.add(look.scale(reach)).add((level.random.nextDouble() - 0.5) * reach * 0.4,
                        (level.random.nextDouble() - 0.5) * reach * 0.4, (level.random.nextDouble() - 0.5) * reach
                                * 0.4);
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
        if (t % 8 == 0) {
            IronManAbilities.sound(level, player.position(), ModSounds.MARK1_FLAMETHROWER.get(), 0.7F, 1.0F);
        }
        if (t % FLAME_GAP != 0) {
            return;
        }
        float damage = (float) IronManAbilities.cfg(IronManConfig.MARK1_FLAME_DAMAGE, 2.0);
        int seconds = IronManAbilities.cfg(IronManConfig.MARK1_FLAME_FIRE_SECONDS, 4);
        for (LivingEntity e : IronManAbilities.inCone(player, length, half, 16)) {
            if (IronManAbilities.hurt(player, e, ModDamageTypes.IRON_FLAME, damage, player)) {
                e.igniteForSeconds(seconds);
            }
        }
        if (IronManAbilities.cfg(IronManConfig.MARK1_FLAME_SETS_BLOCKS, false)
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            Vec3 eye = player.getEyePosition();
            BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(length)), ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos fire = hit.getBlockPos().relative(hit.getDirection());
                if (BaseFireBlock.canBePlacedAt(level, fire, hit.getDirection())) {
                    level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
                }
            }
        }
    }

    private static void stopFlame(ServerPlayer player, PowerData data, State s, long now) {
        if (s.flameStart < 0) {
            return;
        }
        s.flameStart = -1L;
        IronManAbilities.cooldown(data, 1, now, IronManAbilities.cfg(IronManConfig.MARK1_FLAME_COOLDOWN, 40));
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.FLAME_OFF, List.of(player.position()), 0);
    }

    /** Whether his flamethrower burns now (for the tests and the HUD). */
    public static boolean flaming(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s != null && s.flameStart >= 0;
    }

    // ---- G: the Rocket Boost and its crash ----

    private static void boost(ServerPlayer player, PowerData data, State s, long now) {
        if (s.boostStart >= 0 || s.falling || !IronManAbilities.ready(data, 2, now)) {
            return;
        }
        if (!SuitEnergy.spend(player, IronManAbilities.cfg(IronManConfig.MARK1_BOOST_ENERGY, 30.0))) {
            return;
        }
        s.boostStart = now;
        s.falling = false;
        data.set(Live.IRON_DASHING, true);
        IronManAbilities.cooldown(data, 2, now, IronManAbilities.cfg(IronManConfig.MARK1_BOOST_COOLDOWN, 200));
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.BOOST, List.of(player.position()), 0);
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.IRON_BOOST.get(), 1.0F, 1.0F);
    }

    private static void boostTick(ServerPlayer player, PowerData data, State s, long now) {
        ServerLevel level = player.serverLevel();
        if (s.boostStart >= 0) {
            long t = now - s.boostStart;
            int up = IronManAbilities.cfg(IronManConfig.MARK1_BOOST_UP_TICKS, 10);
            int forward = IronManAbilities.cfg(IronManConfig.MARK1_BOOST_FORWARD_TICKS, 20);
            if (t < up) {
                player.setDeltaMovement(0.0, 1.0, 0.0);
            } else if (t < up + forward) {
                Vec3 look = player.getLookAngle();
                Vec3 flat = new Vec3(look.x, Math.max(-0.2, Math.min(0.3, look.y)), look.z).normalize();
                player.setDeltaMovement(flat.scale(0.8));
            } else {
                // The boots sputter out: he falls to his crash.
                s.boostStart = -1L;
                s.falling = true;
                data.set(Live.IRON_DASHING, false);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(), player.getZ(), 8, 0.2,
                        0.1, 0.2, 0.02);
                return;
            }
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY(), player.getZ(), 4, 0.15, 0.05, 0.15,
                    0.02);
            return;
        }
        if (s.falling && (player.onGround() || player.isInWater() || player.getAbilities().flying)) {
            s.falling = false;
            if (player.onGround()) {
                crash(player);
            }
        }
    }

    /** The crash landing: he takes nothing (the suit does), a ring of dust hurts what stands near, a thud. */
    private static void crash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        State s = state(player.getUUID());
        s.fallGuardUntil = level.getGameTime() + 5;
        player.fallDistance = 0.0F;
        IronManAbilities.wear(player, IronManAbilities.cfg(IronManConfig.MARK1_CRASH_DURABILITY, 30));
        double damage = IronManAbilities.cfg(IronManConfig.MARK1_CRASH_DAMAGE, 3.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox()
                .inflate(CRASH_RADIUS, 0.5, CRASH_RADIUS), e -> e != player && e.isAlive())) {
            if (e.distanceTo(player) <= CRASH_RADIUS + 0.5
                    && IronManAbilities.hurt(player, e, DamageTypes.PLAYER_ATTACK, damage, player)) {
                IronManAbilities.push(player, e, player.position(), 0.5);
            }
        }
        BlockPos below = player.blockPosition().below();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below).isAir()
                ? Blocks.DIRT.defaultBlockState() : level.getBlockState(below)), player.getX(), player.getY() + 0.1,
                player.getZ(), 30, 1.2, 0.1, 1.2, 0.15);
        IronManAbilities.sound(level, player.position(), ModSounds.IRON_CRASH.get(), 1.0F, 1.0F);
        IronManAbilities.send(level, player, IronFxPayload.CRASH, List.of(player.position()), 0);
    }

    /**
     * Whether the boost's fall is his to take softly (the crash landing does no harm to him): falling from a boost,
     * or landed a moment ago.
     */
    public static boolean crashing(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s != null && (s.boostStart >= 0 || s.falling || player.level().getGameTime() <= s.fallGuardUntil);
    }

    /** Whether the Rocket Boost fires now. */
    public static boolean boosting(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s != null && s.boostStart >= 0;
    }

    // ---- V: the Arm Rocket ----

    private static void armRocket(ServerPlayer player, PowerData data, long now) {
        if (!IronManAbilities.ready(data, 3, now)
                || !SuitEnergy.spend(player, IronManAbilities.cfg(IronManConfig.MARK1_ROCKET_ENERGY, 15.0))) {
            return;
        }
        IronManAbilities.cooldown(data, 3, now, IronManAbilities.cfg(IronManConfig.MARK1_ROCKET_COOLDOWN, 80));
        Vec3 from = IronManAbilities.palm(player, true);
        Vec3 aim = Mark3Moves.aimPoint(player, 48.0);
        IronMissileEntity rocket = IronMissileEntity.create(player.level(), player, IronMissileEntity.ARM_ROCKET, from,
                aim.subtract(from), 1.5, (float) IronManAbilities.cfg(IronManConfig.MARK1_ROCKET_DAMAGE, 8.0),
                (float) IronManAbilities.cfg(IronManConfig.MARK1_ROCKET_POWER, 1.2), 48.0);
        player.level().addFreshEntity(rocket);
        IronManAbilities.sound(player.serverLevel(), from, ModSounds.MARK1_ARM_ROCKET.get(), 1.0F, 1.0F);
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.ARM_ROCKET, List.of(from), 0);
    }

    // ---- B: the Brace ----

    private static void startBrace(ServerPlayer player, State s, long now) {
        if (s.braceStart >= 0) {
            return;
        }
        if (SuitEnergy.empty(player) && !player.isCreative()) {
            SuitEnergy.refuse(player);
            return;
        }
        s.braceStart = now;
        IronSuits.setState(player, IronSuits.state(player).with(IronManState.BRACING, true));
        applyBrace(player, true);
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.BRACE, List.of(player.position()), 1);
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.MARK1_CLANK.get(), 1.0F, 0.8F);
    }

    private static void stopBrace(ServerPlayer player, State s) {
        if (s.braceStart < 0) {
            return;
        }
        s.braceStart = -1L;
        IronSuits.setState(player, IronSuits.state(player).with(IronManState.BRACING, false));
        applyBrace(player, false);
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.BRACE, List.of(player.position()), 0);
    }

    /** The Brace's hold on him: rooted, no knockback (on while {@link IronManState#BRACING}). */
    static void applyBrace(ServerPlayer player, boolean on) {
        IronManAbilities.setTransient(player, Attributes.MOVEMENT_SPEED, BRACE_ROOT, -1.0, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        IronManAbilities.setTransient(player, Attributes.KNOCKBACK_RESISTANCE, BRACE_KNOCKBACK, 1.0, on,
                AttributeModifier.Operation.ADD_VALUE);
    }

    /** The share of a blow the Brace lets through (0.4 by default). */
    public static float braced(float amount) {
        return (float) (amount * (1.0 - IronManAbilities.cfg(IronManConfig.MARK1_BRACE_REDUCTION, 0.6)));
    }

    // ---- N: the Heavy Punch ----

    private static void heavyPunch(ServerPlayer player, PowerData data, long now) {
        if (!IronManAbilities.ready(data, 5, now)) {
            return;
        }
        IronManAbilities.cooldown(data, 5, now, IronManAbilities.cfg(IronManConfig.MARK1_PUNCH_COOLDOWN, 40));
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        List<LivingEntity> hit = IronManAbilities.inCone(player, 3.0, 35.0, 1);
        List<Vec3> points = new ArrayList<>();
        points.add(IronManAbilities.palm(player, false));
        if (!hit.isEmpty()) {
            LivingEntity target = hit.get(0);
            if (IronManAbilities.hurt(player, target, DamageTypes.PLAYER_ATTACK,
                    IronManAbilities.cfg(IronManConfig.MARK1_PUNCH_DAMAGE, 10.0), player)) {
                IronManAbilities.push(player, target, player.position(), 1.5);
            }
            points.add(target.position().add(0, target.getBbHeight() * 0.6, 0));
        }
        IronManAbilities.sound(player.serverLevel(), player.position(), ModSounds.MARK1_CLANK.get(), 1.0F, 0.6F);
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.PUNCH, points, hit.isEmpty() ? 0 : 1);
    }

    static void forget(UUID id) {
        STATES.remove(id);
    }

    static void clear() {
        STATES.clear();
    }

    /** For the tests: the Brace is held (B down). */
    public static boolean bracing(ServerPlayer player) {
        return IronSuits.state(player).has(IronManState.BRACING);
    }
}
