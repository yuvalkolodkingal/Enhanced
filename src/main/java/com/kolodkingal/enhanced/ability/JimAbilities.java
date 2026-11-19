package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.ChargedBeamConfig;
import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jim Hammond (3.17), the android first Human Torch, as the designer specified him (and nothing more):
 * <ol>
 *   <li>R, the Fire Beam ({@link JimBeam}: a continuous beam while it is held, fed by his solar charge; it burns what it
 *   touches and the ground along it);</li>
 *   <li>total immunity to fire ({@link #FIRE_IMMUNITY});</li>
 *   <li>a body almost as durable as Vision's, healing five times as fast ({@link #BODY});</li>
 *   <li>V, fireballs ({@link JimFireballs});</li>
 *   <li>B, the Nova Flame ({@link ChargedBeam}'s own mechanism: hold to charge, release to fire; stronger the longer; burns
 *   everything along and around its path, explodes where it strikes, makes magma when charged; then a long cooldown);</li>
 *   <li>his whole body burns while he flies, and whoever strikes him then is set alight ({@link JimEvents}).</li>
 * </ol>
 * G is flight (a flame form the client draws), N the human disguise (the look, android and human).
 */
public final class JimAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("jim_body_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("jim_body_armor");

    private static final Map<UUID, Long> LAST_HEAL = new HashMap<>();

    private JimAbilities() {
    }

    /** The Nova Flame: the Charged Beam's own mechanism (hold to charge, release to fire), with Jim's numbers, jewel and fire. */
    public static final ChargedBeam.Profile NOVA = new ChargedBeam.Profile(JimConfig.NOVA_CHARGE_TICKS::get,
            JimConfig.NOVA_TAP_TICKS::get, JimConfig.NOVA_COOLDOWN::get, JimConfig.NOVA_MIN_DAMAGE::get,
            JimConfig.NOVA_DAMAGE::get, JimConfig.NOVA_MIN_RANGE::get, JimConfig.NOVA_RANGE::get, JimConfig.NOVA_WIDTH::get,
            ModDamageTypes.SOLAR_BEAM, c -> Math.round(JimConfig.NOVA_BURN_SECONDS.get() * (1.0F + (float) c)), JimAbilities::novaImpact,
            new ChargedBeam.Gate() {
                @Override
                public boolean start(ServerPlayer player, PowerData data) {
                    return JimSolar.has(player, data, JimConfig.NOVA_MIN_CHARGE.get());
                }

                @Override
                public boolean fire(ServerPlayer player, PowerData data, float charge) {
                    return JimSolar.spend(player, data, JimSolar.novaCost(charge));
                }
            }, Live.JIM_NOVA_CHARGING, Live.JIM_NOVA_BLAST, JimConfig.NOVA_DURATION_TICKS::get);

    // ---- keys ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        switch (ability) {
            case 1 -> JimBeam.onKey(player, data, down);
            case 2 -> {
                if (down) {
                    PowerTicker.toggleFlight(player);
                }
            }
            case 3 -> JimFireballs.onKey(player, data, down);
            case 4 -> {
                ChargedBeam.onKey(player, data, down, NOVA, HeroAbility.JIM_NOVA);
            }
            case 5 -> {
                if (down) {
                    disguise(player, data);
                }
            }
            default -> {
            }
        }
    }

    /** N: the human disguise: android to human and back (the look is drawn by the client from {@link PowerData#disguise}). */
    private static void disguise(ServerPlayer player, PowerData data) {
        data.setDisguise(!data.disguise());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        player.serverLevel().sendParticles(new EnergyParticleOptions(0xFF8A2A, 1.4F), player.getX(), player.getY() + 1, player.getZ(),
                24, 0.3, 0.8, 0.3, 0.02);
        player.displayClientMessage(Component.translatable(data.disguise() ? "message.enhanced.disguise.on"
                : "message.enhanced.disguise.off"), true);
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        JimSolar.tick(player, data);
        if (data.has(HeroAbility.JIM_BEAM)) {
            JimBeam.tick(player, data);
        }
        if (data.has(HeroAbility.JIM_FIREBALL)) {
            JimFireballs.tick(player, data);
        }
        if (data.has(HeroAbility.JIM_NOVA)) {
            ChargedBeam.tick(player, data, NOVA, HeroAbility.JIM_NOVA);
        }
        if (data.has(HeroAbility.JIM_FIRE_IMMUNITY)) {
            fireImmunity(player);
        }
        if (data.has(HeroAbility.JIM_BODY) || data.has(HeroAbility.JIM_FAST_HEALING)) {
            regenerate(player);
        }
        boolean flames = flaming(player, data);
        if (data.is(Live.JIM_FLAMING) != flames) {
            data.set(Live.JIM_FLAMING, flames);
        }
    }

    /** Whether his whole body is burning now: he flies (in a look that burns: not the human disguise). */
    public static boolean flaming(ServerPlayer player, PowerData data) {
        return data.has(HeroAbility.JIM_FLAME_BODY) && JimConfig.FLAMES_WHILE_FLYING.get() && !player.isSpectator()
                && player.getAbilities().flying && !data.disguise() && !"human".equals(data.variant());
    }

    // ---- the body ----

    /** Total immunity to fire: burning, lava, magma, fire-type attacks. */
    private static void fireImmunity(ServerPlayer player) {
        player.clearFire();
        MobEffectInstance effect = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (effect == null || effect.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, false));
        }
    }

    /** Healing five times as fast as a normal player (who heals one health in four seconds). */
    private static void regenerate(ServerPlayer player) {
        if (player.isDeadOrDying() || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        long now = player.level().getGameTime();
        int every = Math.max(1, Math.round(80.0F / JimConfig.REGEN_MULTIPLIER.get().floatValue()));
        Long last = LAST_HEAL.get(player.getUUID());
        if (last == null || now - last >= every) {
            LAST_HEAL.put(player.getUUID(), now);
            player.heal(1.0F);
        }
    }

    static void applyBody(ServerPlayer player, PowerData data) {
        boolean body = data.has(HeroAbility.JIM_BODY);
        float scale = data.bodyScale(HeroAbility.JIM_BODY);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, JimConfig.HEALTH_BONUS.get() * scale, body);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, JimConfig.ARMOR_BONUS.get() * scale, body);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---- the Nova Flame's mark on the world ----

    /**
     * Where the Nova Flame strikes: an explosion scaled by the charge (as Source Code Vision's charged beam has), then everything
     * along and around the beam's line burns: every creature within a radius of it (about 2 blocks for a tap, about 4 at a full
     * charge) is set alight, fire is placed on the open faces of the blocks of that band, and a charge of 80% or more turns
     * the ground along the path into magma. All through the mod's rules: never with {@code mobGriefing} off, never against
     * {@link BlockRights}; never him.
     */
    private static void novaImpact(ServerPlayer player, ServerLevel level, HitResult hit, float charge) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = hit.getLocation();
        Vec3 axis = end.subtract(eye);
        double length = axis.length();
        double radius = Mth.lerp(charge, (float) JimConfig.NOVA_BURN_RADIUS_MIN.get().doubleValue(),
                (float) Math.max(JimConfig.NOVA_BURN_RADIUS_MIN.get(), JimConfig.NOVA_BURN_RADIUS_MAX.get()));
        int seconds = Math.round(JimConfig.NOVA_BURN_SECONDS.get() * (1.0F + charge));
        // the creatures beside the line burn too
        if (seconds > 0 && radius > 0.0) {
            for (net.minecraft.world.entity.Entity e : level.getEntities(player, new AABB(eye, end).inflate(radius + 1.0), WandaAbilities.BEAM_TARGET)) {
                if (e instanceof LivingEntity living && !AreaTargets.mayHurt(player, living, true)) {
                    continue;
                }
                Vec3 c = e.getBoundingBox().getCenter();
                Vec3 ab = end.subtract(eye);
                double len2 = ab.lengthSqr();
                double t = len2 < 1.0E-9 ? 0 : Math.max(0, Math.min(1, c.subtract(eye).dot(ab) / len2));
                if (c.distanceTo(eye.add(ab.scale(t))) <= radius + e.getBbWidth() / 2.0) {
                    BeamFire.ignite(e, seconds);
                }
            }
        }
        if (length > 1.0 && radius > 0.0) {
            boolean magma = JimConfig.NOVA_MAGMA.get() && charge >= JimConfig.NOVA_MAGMA_MIN_CHARGE.get();
            boolean fire = JimConfig.NOVA_FIRE.get();
            if (magma || fire) {
                Vec3 dir = axis.scale(1.0 / length);
                int steps = (int) Math.min(48, length / 2.0);
                int fires = 0;
                for (int i = 0; i <= steps; i++) {
                    Vec3 c = eye.add(dir.scale(Math.min(length, 1.5 + i * 2.0)));
                    if (magma) {
                        BlockPos ground = groundBelow(level, BlockPos.containing(c), 10);
                        if (ground != null) {
                            magma(player, level, ground, JimConfig.NOVA_MAGMA_RADIUS.get());
                        }
                    }
                    for (int k = 0; fire && k < 4 && fires < 80; k++) {
                        Vec3 at = c.add((level.random.nextDouble() - 0.5) * 2.0 * radius, (level.random.nextDouble() - 0.5) * radius,
                                (level.random.nextDouble() - 0.5) * 2.0 * radius);
                        BlockPos ground = groundBelow(level, BlockPos.containing(at), 8);
                        // (never near him: the fire is not for the shooter's own feet)
                        if (ground != null && ground.above().distToCenterSqr(eye) > 9.0 && BeamFire.place(player, level, ground.above())) {
                            fires++;
                        }
                    }
                }
            }
        }
        if (JimConfig.NOVA_EXPLOSION.get() && hit.getType() == HitResult.Type.BLOCK) {
            float r = Mth.lerp(charge, (float) ChargedBeamConfig.EXPLOSION_MIN_RADIUS.get().doubleValue(),
                    (float) Math.max(ChargedBeamConfig.EXPLOSION_MIN_RADIUS.get(), ChargedBeamConfig.EXPLOSION_MAX_RADIUS.get()));
            ChargedBeam.explode(player, level, end, r, charge, (float) ChargedBeamConfig.EXPLOSION_MAX_DAMAGE.get().doubleValue(),
                    ChargedBeamConfig.EXPLOSION_BREAKS_BLOCKS.get());
        }
    }

    /** The first solid block at or below {@code from} within {@code down} blocks (the ground under a point of the beam), or null. */
    private static BlockPos groundBelow(ServerLevel level, BlockPos from, int down) {
        BlockPos pos = from;
        for (int i = 0; i <= down; i++) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced()) {
                return pos;
            }
            pos = pos.below();
        }
        return null;
    }

    /** A charged beam turns the ground it strikes into magma: the struck block and the ones round it on its layer. */
    static int magma(ServerPlayer player, ServerLevel level, BlockPos centre, int r) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return 0;
        }
        int made = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + r / 2) {
                    continue;
                }
                BlockPos pos = centre.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                if (state.isAir() || state.is(Blocks.MAGMA_BLOCK) || hardness < 0.0F || hardness > 8.0F || !state.getFluidState().isEmpty()
                        || level.getBlockEntity(pos) != null || !state.isCollisionShapeFullBlock(level, pos)) {
                    continue;
                }
                if (BlockRights.mayBlast(player, level, pos)) {
                    level.setBlockAndUpdate(pos, Blocks.MAGMA_BLOCK.defaultBlockState());
                    Embers.burst(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.0);
                    made++;
                }
            }
        }
        return made;
    }

    /** Where the Nova starts by look: the classic suit's jewel is on the forehead (the head part); the android's gold diamond and the human disguise are on the chest. */
    public static boolean novaStartsAtHead(PowerData data) {
        return !data.disguise() && "classic".equals(data.variant());
    }

    // ---- ending ----

    static void endAll(ServerPlayer player, PowerData data) {
        LAST_HEAL.remove(player.getUUID());
        JimFireballs.forget(player.getUUID());
        JimBeam.forget(player.getUUID());
        ChargedBeam.forget(player.getUUID());
        data.set(Live.JIM_THROW, false);
        data.set(Live.JIM_FIREBALL_CHARGING, false);
        data.set(Live.JIM_NOVA_CHARGING, false);
        data.set(Live.JIM_NOVA_BLAST, false);
        data.set(Live.JIM_FLAMING, false);
        data.set(Live.BEAM_CHARGING, false);
        data.set(Live.BEAM_FIRING, false);
        // the fire resistance he kept up (quiet, a few seconds) goes with the power
        MobEffectInstance kept = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (kept != null && kept.isAmbient() && !kept.isVisible() && kept.getDuration() <= 100) {
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, 0, false);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, 0, false);
    }

    static void forget(UUID player) {
        LAST_HEAL.remove(player);
        JimFireballs.forget(player);
        JimBeam.forget(player);
        ChargedBeam.forget(player);
    }
}
